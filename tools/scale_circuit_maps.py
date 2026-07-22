#!/usr/bin/env python3
"""Scale circuit geometry while keeping the physical road width unchanged.

The game derives world dimensions from the map aspect ratio, so increasing PNG
resolution alone does not make a circuit larger.  This tool compensates masks
for a larger logical world scale by rebuilding each road at ``1 / scale`` of
its original pixel width.  The loader applies the matching world scale.

The source and output directories must differ.  This is intentional: applying
the operation twice would narrow an already compensated road a second time.
"""

from __future__ import annotations

import argparse
import math
import shutil
from pathlib import Path
from typing import Iterable, Tuple

import numpy as np
from PIL import Image
from scipy.ndimage import (
    binary_closing,
    binary_dilation,
    distance_transform_edt,
    gaussian_filter,
    label,
)


RGB = np.ndarray
MASK_SCALE_EPSILON = 1.0e-6
MARKER_STRUCTURE = np.ones((3, 3), dtype=bool)
MARKER_ROAD_INFERENCE_RADIUS = 8
ROAD_CONTOUR_SMOOTH_SIGMA = 1.0
ROAD_EDGE_ANTIALIAS_SIGMA = 0.75


def color_masks(rgb: RGB) -> dict[str, np.ndarray]:
    values = rgb.astype(np.int16)
    red, green, blue = np.moveaxis(values, -1, 0)
    route_line = (
        (red >= 210)
        & (green >= 95)
        & (green <= 190)
        & (blue <= 95)
        & (red >= green + 45)
        & (green >= blue + 35)
    )
    route_hint = (
        ~route_line
        & (red <= 130)
        & (green >= 35)
        & (blue >= 35)
        & (np.abs(green - blue) <= 24)
        & (green >= red + 28)
        & (blue >= red + 28)
    )
    return {
        "base": (
            ((red >= 145) & (green >= 145) & (blue >= 145))
            | (
                (red >= 70)
                & (red <= 115)
                & (np.abs(red - green) <= 3)
                & (np.abs(green - blue) <= 3)
            )
            | (
                (red >= 245)
                & (blue >= 245)
                & (green >= 1)
                & (green <= 250)
                & (red >= green + 3)
                & (blue >= green + 3)
            )
        ),
        "red": ~route_line & (red >= 150) & (green <= 120) & (blue <= 120),
        "blue": ~route_line & ~route_hint & (blue >= 140) & (red <= 120) & (green <= 170),
        "green": ~route_line & (green >= 145) & (red <= 125) & (blue <= 135),
        "route_hint": route_hint,
        "route_line": route_line,
    }


def crop_to_content(mask: np.ndarray, padding: int = 2) -> Tuple[np.ndarray, Tuple[slice, slice]]:
    ys, xs = np.nonzero(mask)
    if len(xs) == 0:
        raise ValueError("road mask has no playable pixels")
    y0 = max(0, int(ys.min()) - padding)
    y1 = min(mask.shape[0], int(ys.max()) + padding + 1)
    x0 = max(0, int(xs.min()) - padding)
    x1 = min(mask.shape[1], int(xs.max()) + padding + 1)
    slices = (slice(y0, y1), slice(x0, x1))
    return mask[slices].copy(), slices


def thin_zhang_suen(mask: np.ndarray) -> np.ndarray:
    """Return a topology-preserving one-pixel skeleton of a binary mask."""
    image, slices = crop_to_content(mask)
    changed = True
    while changed:
        changed = False
        for first_pass in (True, False):
            padded = np.pad(image, 1, mode="constant")
            p2 = padded[:-2, 1:-1]
            p3 = padded[:-2, 2:]
            p4 = padded[1:-1, 2:]
            p5 = padded[2:, 2:]
            p6 = padded[2:, 1:-1]
            p7 = padded[2:, :-2]
            p8 = padded[1:-1, :-2]
            p9 = padded[:-2, :-2]
            neighbours = (
                p2.astype(np.uint8)
                + p3
                + p4
                + p5
                + p6
                + p7
                + p8
                + p9
            )
            transitions = (
                ((~p2) & p3).astype(np.uint8)
                + ((~p3) & p4)
                + ((~p4) & p5)
                + ((~p5) & p6)
                + ((~p6) & p7)
                + ((~p7) & p8)
                + ((~p8) & p9)
                + ((~p9) & p2)
            )
            if first_pass:
                preserve_a = p2 & p4 & p6
                preserve_b = p4 & p6 & p8
            else:
                preserve_a = p2 & p4 & p8
                preserve_b = p2 & p6 & p8
            remove = (
                image
                & (neighbours >= 2)
                & (neighbours <= 6)
                & (transitions == 1)
                & ~preserve_a
                & ~preserve_b
            )
            if np.any(remove):
                image[remove] = False
                changed = True

    result = np.zeros_like(mask)
    result[slices] = image
    return result


def disk(radius: int) -> np.ndarray:
    y, x = np.ogrid[-radius : radius + 1, -radius : radius + 1]
    return x * x + y * y <= radius * radius


def rebuild_road(source: np.ndarray, scale: float) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    source = binary_closing(source, structure=MARKER_STRUCTURE)
    distances = distance_transform_edt(source)
    skeleton = thin_zhang_suen(source)
    if not np.any(skeleton):
        raise ValueError("road skeleton is empty")

    local_radii = distances[skeleton]
    target_radii = np.maximum(1, np.rint(local_radii / scale).astype(np.int16))
    rebuilt = np.zeros_like(source)
    skeleton_y, skeleton_x = np.nonzero(skeleton)
    for radius in np.unique(target_radii):
        centers = np.zeros_like(source)
        selected = target_radii == radius
        centers[skeleton_y[selected], skeleton_x[selected]] = True
        rebuilt |= binary_dilation(centers, structure=disk(int(radius)))

    # A half-width reconstruction must remain inside the source road.  This
    # also removes isolated skeleton spurs created by one-pixel marker holes.
    rebuilt &= source
    contour = gaussian_filter(
        rebuilt.astype(np.float32),
        sigma=ROAD_CONTOUR_SMOOTH_SIGMA,
        mode="constant",
    )
    rebuilt = binary_closing(contour >= 0.5, structure=disk(1))
    rebuilt &= source
    return rebuilt, skeleton, distances


def scaled_component_mask(mask: np.ndarray, scale: float) -> np.ndarray:
    components, count = label(mask, structure=MARKER_STRUCTURE)
    result = np.zeros_like(mask)
    for component in range(1, count + 1):
        ys, xs = np.nonzero(components == component)
        if len(xs) == 0:
            continue
        center_x = float(xs.mean())
        center_y = float(ys.mean())
        scaled_x = np.rint(center_x + (xs - center_x) / scale).astype(int)
        scaled_y = np.rint(center_y + (ys - center_y) / scale).astype(int)
        valid = (
            (scaled_x >= 0)
            & (scaled_x < mask.shape[1])
            & (scaled_y >= 0)
            & (scaled_y < mask.shape[0])
        )
        scaled_component = np.zeros_like(mask)
        scaled_component[scaled_y[valid], scaled_x[valid]] = True
        # Marker parsers require a meaningful connected area.  Preserve at
        # least a two-pixel radius after scaling tiny source dots.
        if int(scaled_component.sum()) < 8 and len(xs) >= 8:
            radius = 2
            cx = int(round(center_x))
            cy = int(round(center_y))
            y0 = max(0, cy - radius)
            y1 = min(mask.shape[0], cy + radius + 1)
            x0 = max(0, cx - radius)
            x1 = min(mask.shape[1], cx + radius + 1)
            yy, xx = np.ogrid[y0:y1, x0:x1]
            scaled_component[y0:y1, x0:x1] |= (
                (xx - cx) ** 2 + (yy - cy) ** 2 <= radius * radius
            )
        result |= scaled_component
    return result


def scaled_spawn_markers(mask: np.ndarray, scale: float) -> np.ndarray:
    components, count = label(mask, structure=MARKER_STRUCTURE)
    if count == 0:
        return np.zeros_like(mask)
    ys, xs = np.nonzero(mask)
    group_x = float(xs.mean())
    group_y = float(ys.mean())
    markers: list[Tuple[float, float, float]] = []
    for component in range(1, count + 1):
        component_y, component_x = np.nonzero(components == component)
        center_x = float(component_x.mean())
        center_y = float(component_y.mean())
        target_x = group_x + (center_x - group_x) / scale
        target_y = group_y + (center_y - group_y) / scale
        source_radius = math.sqrt(len(component_x) / math.pi)
        markers.append((target_x, target_y, source_radius / scale))

    if len(markers) == 2:
        first_x, first_y, first_radius = markers[0]
        second_x, second_y, second_radius = markers[1]
        dx = second_x - first_x
        dy = second_y - first_y
        separation = max(abs(dx), abs(dy))
        minimum_separation = 4.0
        if separation < minimum_separation and separation > MASK_SCALE_EPSILON:
            expansion = minimum_separation / separation
            dx *= expansion
            dy *= expansion
            markers[0] = (group_x - dx * 0.5, group_y - dy * 0.5, first_radius)
            markers[1] = (group_x + dx * 0.5, group_y + dy * 0.5, second_radius)

    result = np.zeros_like(mask)
    for target_x, target_y, radius_float in markers:
        cx = int(round(target_x))
        cy = int(round(target_y))
        if radius_float < 1.5:
            # A 3x3 marker is the smallest 8-connected shape that satisfies
            # the loader's eight-pixel spawn-marker threshold.
            y0 = max(0, cy - 1)
            y1 = min(mask.shape[0], cy + 2)
            x0 = max(0, cx - 1)
            x1 = min(mask.shape[1], cx + 2)
            result[y0:y1, x0:x1] = True
        else:
            radius = max(2, int(round(radius_float)))
            y0 = max(0, cy - radius)
            y1 = min(mask.shape[0], cy + radius + 1)
            x0 = max(0, cx - radius)
            x1 = min(mask.shape[1], cx + radius + 1)
            yy, xx = np.ogrid[y0:y1, x0:x1]
            result[y0:y1, x0:x1] |= (
                (xx - cx) ** 2 + (yy - cy) ** 2 <= radius * radius
            )
    return result


def dominant_color(rgb: RGB, mask: np.ndarray, fallback: Tuple[int, int, int]) -> np.ndarray:
    if not np.any(mask):
        return np.asarray(fallback, dtype=np.uint8)
    values, counts = np.unique(rgb[mask], axis=0, return_counts=True)
    return values[int(np.argmax(counts))]


def build_scaled_mask(rgb: RGB, scale: float) -> Tuple[RGB, np.ndarray, np.ndarray]:
    colors = color_masks(rgb)
    markers = colors["red"] | colors["blue"] | colors["green"]
    inferred_road = binary_closing(
        colors["base"],
        structure=disk(MARKER_ROAD_INFERENCE_RADIUS),
    )
    # Painted markers replace road pixels, but some finish lines extend past
    # the road edge. Restore only marker pixels inside the inferred envelope
    # so those decorative tips cannot become drivable protrusions.
    source_road = colors["base"] | (markers & inferred_road)
    source_road |= colors["route_line"] & binary_dilation(colors["base"], structure=disk(4))
    rebuilt, _, _ = rebuild_road(source_road, scale)

    output = np.zeros_like(rgb)
    output[rebuilt] = (255, 255, 255)

    red = scaled_spawn_markers(colors["red"], scale)
    blue = scaled_component_mask(colors["blue"], scale)
    green = scaled_component_mask(colors["green"], scale)
    marker_context = binary_dilation(rebuilt, structure=disk(2))
    red &= marker_context
    blue &= marker_context
    green &= marker_context
    output[red] = dominant_color(rgb, colors["red"], (255, 0, 0))
    output[blue] = dominant_color(rgb, colors["blue"], (28, 86, 230))
    output[green] = dominant_color(rgb, colors["green"], (35, 210, 85))

    # Route hints and route-line waypoints describe positions along the map,
    # not road thickness.  Their paths therefore retain their coordinates.
    output[colors["route_hint"]] = rgb[colors["route_hint"]]
    output[colors["route_line"]] = rgb[colors["route_line"]]
    return output, source_road, rebuilt


def nearest_source_pixels(unavailable: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
    _, indices = distance_transform_edt(unavailable, return_indices=True)
    return indices[0], indices[1]


def rebuild_art(
        art: RGB,
        source_road: np.ndarray,
        rebuilt_road: np.ndarray,
        scale: float) -> RGB:
    old_radius = max(3, int(round(7 / max(1.0, scale / 2.0))))
    old_visual_region = binary_dilation(source_road, structure=disk(old_radius))
    source_y, source_x = nearest_source_pixels(old_visual_region)
    background = art.copy()
    background[old_visual_region] = art[source_y[old_visual_region], source_x[old_visual_region]]

    # Soften only the synthesized strip. A wider kernel avoids visible nearest-
    # source sectors around hairpins while all themed pixels outside the old
    # road remain byte-for-byte unchanged.
    blurred = np.empty_like(background)
    for channel in range(3):
        blurred[:, :, channel] = np.clip(
            gaussian_filter(background[:, :, channel].astype(np.float32), sigma=4.5),
            0,
            255,
        ).astype(np.uint8)
    background[old_visual_region] = blurred[old_visual_region]

    road_coverage = gaussian_filter(
        rebuilt_road.astype(np.float32),
        sigma=ROAD_EDGE_ANTIALIAS_SIGMA,
        mode="constant",
    )
    road_alpha = np.clip((road_coverage - 0.08) / 0.84, 0.0, 1.0)[:, :, None]
    result_float = (
        background.astype(np.float32) * (1.0 - road_alpha)
        + art.astype(np.float32) * road_alpha
    )

    old_distance = distance_transform_edt(source_road)
    old_edge = source_road & (old_distance <= 2.25)
    edge_color = dominant_color(art, old_edge, (220, 220, 220)).astype(np.float32)
    edge_gradient_y, edge_gradient_x = np.gradient(road_coverage)
    edge_alpha = np.clip(
        np.hypot(edge_gradient_x, edge_gradient_y) * 2.8,
        0.0,
        1.0,
    )[:, :, None]
    result_float = result_float * (1.0 - edge_alpha) + edge_color * edge_alpha
    result = np.clip(result_float, 0, 255).astype(np.uint8)

    # Recreate the colored halo at the new road boundary using the source
    # border color nearest to each outside pixel.
    outside_distance, new_indices = distance_transform_edt(~rebuilt_road, return_indices=True)
    glow_radius = max(2.0, 6.0 / scale)
    glow = (~rebuilt_road) & (outside_distance <= glow_radius)
    boundary_colors = result[new_indices[0][glow], new_indices[1][glow]].astype(np.float32)
    alpha = (0.34 * np.exp(-outside_distance[glow] / max(1.0, glow_radius * 0.55)))[:, None]
    result[glow] = np.clip(
        result[glow].astype(np.float32) * (1.0 - alpha) + boundary_colors * alpha,
        0,
        255,
    ).astype(np.uint8)
    return result


def estimated_width(mask: np.ndarray) -> float:
    eroded = distance_transform_edt(mask) > 1.0
    perimeter = mask & ~eroded
    if not np.any(perimeter):
        return 0.0
    return 2.0 * float(mask.sum()) / float(perimeter.sum())


def transform_map(mask_path: Path, surface_path: Path, output_dir: Path, scale: float) -> None:
    with Image.open(mask_path).convert("RGB") as image:
        source_mask = np.asarray(image).copy()
    with Image.open(surface_path).convert("RGB") as image:
        art = np.asarray(image).copy()
    if source_mask.shape != art.shape:
        raise ValueError(f"map dimensions differ: {mask_path} and {surface_path}")

    scaled_mask, source_road, rebuilt_road = build_scaled_mask(source_mask, scale)
    scaled_art = rebuild_art(art, source_road, rebuilt_road, scale)

    output_dir.mkdir(parents=True, exist_ok=True)
    Image.fromarray(scaled_mask, mode="RGB").save(output_dir / mask_path.name, optimize=True)
    Image.fromarray(scaled_art, mode="RGB").save(output_dir / surface_path.name, optimize=True)
    old_width = estimated_width(source_road)
    new_width = estimated_width(rebuilt_road)
    source_components = label(source_road, structure=MARKER_STRUCTURE)[1]
    rebuilt_components = label(rebuilt_road, structure=MARKER_STRUCTURE)[1]
    width_ratio = new_width * scale / old_width
    if rebuilt_components != source_components:
        raise ValueError(
            f"road topology changed for {mask_path.stem}: "
            f"{source_components} -> {rebuilt_components} components"
        )
    if not 0.85 <= width_ratio <= 1.15:
        raise ValueError(
            f"road width changed unexpectedly for {mask_path.stem}: ratio={width_ratio:.3f}"
        )
    print(
        f"scaled={mask_path.stem.removesuffix('_mask')} scale={scale:.3f} "
        f"size={source_mask.shape[1]}x{source_mask.shape[0]} "
        f"old_width_px={old_width:.2f} new_width_px={new_width:.2f} "
        f"world_width_ratio={width_ratio:.3f} components={rebuilt_components}"
    )


def selected_masks(source_dir: Path, start: int, end: int) -> Iterable[Path]:
    for mask_path in sorted(source_dir.glob("map[0-9][0-9][0-9]_mask.png")):
        index = int(mask_path.stem[3:6])
        if start <= index <= end:
            yield mask_path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--scale", type=float, default=2.0)
    parser.add_argument("--start", type=int, default=0)
    parser.add_argument("--end", type=int, default=999)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    source_dir = Path(args.source_dir).resolve()
    output_dir = Path(args.output_dir).resolve()
    if args.scale <= 1.0 + MASK_SCALE_EPSILON:
        raise ValueError("--scale must be greater than 1")
    if source_dir == output_dir:
        raise ValueError("--source-dir and --output-dir must differ")

    masks = list(selected_masks(source_dir, args.start, args.end))
    if not masks:
        raise ValueError(f"no circuit masks found in {source_dir}")
    for mask_path in masks:
        surface_path = mask_path.with_name(mask_path.name.replace("_mask.png", ".png"))
        if not surface_path.exists():
            raise FileNotFoundError(surface_path)
        transform_map(mask_path, surface_path, output_dir, args.scale)

    for source in source_dir.iterdir():
        if source.suffixes[-2:] == [".json", ".gz"]:
            continue
        if source.name.endswith("_mask.png") or (
                source.suffix == ".png" and source.with_name(f"{source.stem}_mask.png").exists()):
            continue
        if source.is_file():
            shutil.copy2(source, output_dir / source.name)


if __name__ == "__main__":
    main()

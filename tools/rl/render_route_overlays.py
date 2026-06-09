#!/usr/bin/env python3
"""Render debug overlays of the route stored in generated map caches."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

import jpype
from PIL import Image, ImageDraw, ImageFont


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = Path("desktop") / "target" / "ratass-desktop-1.0.jar"
DEFAULT_OUTPUT_DIR = Path("logs") / "route-overlays"
ROUTE_COLOR = (255, 0, 220, 255)
ROUTE_OUTLINE = (0, 0, 0, 255)
ARROW_COLOR = (0, 215, 255, 255)
BAD_ROUTE_COLOR = (255, 16, 8, 255)
ROUTE_MARKER_TEXT_COLOR = (255, 170, 20, 255)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument(
        "--map-dir",
        action="append",
        default=[],
        help=(
            "map directory to render; may be passed multiple times. "
            "Defaults to assets/maps and tools/rl/trainingMaps."
        ),
    )
    parser.add_argument("--output-dir", default=os.fspath(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--map-scale", type=float, default=8.0)
    parser.add_argument("--sample-step", type=float, default=0.55)
    parser.add_argument("--line-width", type=int, default=5)
    parser.add_argument("--arrow-every", type=float, default=16.0)
    return parser.parse_args()


def start_jvm(jar_path: Path) -> None:
    if jpype.isJVMStarted():
        return
    if not jar_path.exists():
        raise FileNotFoundError(f"{jar_path} does not exist. Run `mvn -pl desktop -am package` first.")
    jpype.startJVM(jpype.getDefaultJVMPath(), "-Xrs", classpath=[str(jar_path)])
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


def load_maps_from_directory(map_dir: Path, map_scale: float):
    loader = jpype.JClass("com.github.jbescos.gameplay.maps.ImageArenaMapLoader")
    string_type = jpype.JClass("java.lang.String")
    float_type = jpype.JClass("java.lang.Float").TYPE
    boolean_type = jpype.JClass("java.lang.Boolean").TYPE
    method = loader.class_.getDeclaredMethod(
        "loadMapsFromDirectory",
        string_type.class_,
        float_type,
        boolean_type,
    )
    method.setAccessible(True)
    return method.invoke(None, repo_relative(map_dir), jpype.JFloat(map_scale), True)


def repo_relative(path: Path) -> str:
    resolved = path.resolve()
    try:
        return os.fspath(resolved.relative_to(REPO_ROOT))
    except ValueError:
        return os.fspath(resolved)


def project_path(path: Path) -> Path:
    return path if path.is_absolute() else REPO_ROOT / path


def world_to_pixel(point, bounds, width: int, height: int) -> tuple[float, float]:
    world_min_x = float(bounds.x)
    world_min_y = float(bounds.y)
    world_width = float(bounds.width)
    world_height = float(bounds.height)
    pixel_x = (float(point.x) - world_min_x) * width / world_width - 0.5
    pixel_y = (world_min_y + world_height - float(point.y)) * height / world_height - 0.5
    return pixel_x, pixel_y


def draw_text(
        draw: ImageDraw.ImageDraw,
        xy: tuple[float, float],
        text: str,
        font,
        fill: tuple[int, int, int, int] = (255, 255, 255, 255)) -> None:
    x, y = xy
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)):
        draw.text((x + dx, y + dy), text, fill=(0, 0, 0, 255), font=font)
    draw.text((x, y), text, fill=fill, font=font)


def draw_arrow(
        draw: ImageDraw.ImageDraw,
        point: tuple[float, float],
        tangent: tuple[float, float],
        size: float) -> None:
    x, y = point
    tx, ty = tangent
    length = max(0.0001, (tx * tx + ty * ty) ** 0.5)
    tx /= length
    ty /= length
    nx = -ty
    ny = tx
    tip = (x + tx * size, y + ty * size)
    left = (x - tx * size * 0.55 + nx * size * 0.42, y - ty * size * 0.55 + ny * size * 0.42)
    right = (x - tx * size * 0.55 - nx * size * 0.42, y - ty * size * 0.55 - ny * size * 0.42)
    draw.polygon([tip, left, right], fill=ARROW_COLOR, outline=ROUTE_OUTLINE)


def route_samples(arena_map, sample_step: float):
    vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
    route_length = float(arena_map.getRouteLength())
    if route_length <= 0.001:
        return []
    step = max(0.05, sample_step)
    count = max(2, int(route_length / step) + 1)
    point = vector2()
    samples = []
    for index in range(count + 1):
        progress = route_length * index / count
        arena_map.findRoutePoint(jpype.JFloat(progress), point)
        samples.append((float(progress), point.cpy()))
    return samples


def route_interval_cuts_offroad(arena_map, start_progress: float, delta: float, sample_step: float) -> bool:
    vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
    point = vector2()
    detection_step = max(0.08, min(0.25, sample_step * 0.5))
    sample_count = max(2, min(2400, int(delta / detection_step) + 1))
    for sample in range(sample_count + 1):
        progress = start_progress + delta * sample / sample_count
        arena_map.findRoutePoint(jpype.JFloat(progress), point)
        if not bool(arena_map.approximateSupports(jpype.JFloat(point.x), jpype.JFloat(point.y))):
            return True
    return False


def draw_route_progress_segment(
        arena_map,
        draw: ImageDraw.ImageDraw,
        bounds,
        width: int,
        height: int,
        start_progress: float,
        delta: float,
        sample_step: float,
        line_width: int,
        fill: tuple[int, int, int, int]) -> None:
    vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
    point = vector2()
    sample_count = max(2, min(1800, int(delta / max(0.08, sample_step)) + 1))
    pixels = []
    for sample in range(sample_count + 1):
        progress = start_progress + delta * sample / sample_count
        arena_map.findRoutePoint(jpype.JFloat(progress), point)
        pixels.append(world_to_pixel(point, bounds, width, height))
    if len(pixels) >= 2:
        draw.line(pixels, fill=fill, width=line_width, joint="curve")


def draw_bad_route_segments(
        arena_map,
        draw: ImageDraw.ImageDraw,
        bounds,
        width: int,
        height: int,
        route_length: float,
        sample_step: float,
        line_width: int) -> None:
    marker_count = int(arena_map.getRouteMarkerPointCount())
    if marker_count < 2 or route_length <= 0.001:
        return
    for marker_index in range(marker_count):
        start_progress = float(arena_map.getRouteMarkerProgress(marker_index))
        end_progress = float(arena_map.getRouteMarkerProgress((marker_index + 1) % marker_count))
        delta = end_progress - start_progress
        if marker_index == marker_count - 1 or delta <= 0.001:
            delta += route_length
        if delta <= 0.001 or delta > route_length + 0.001:
            continue
        if not route_interval_cuts_offroad(arena_map, start_progress, delta, sample_step):
            continue
        draw_route_progress_segment(
            arena_map,
            draw,
            bounds,
            width,
            height,
            start_progress,
            delta,
            max(2.0, sample_step * 0.55),
            line_width + 6,
            ROUTE_OUTLINE,
        )
        draw_route_progress_segment(
            arena_map,
            draw,
            bounds,
            width,
            height,
            start_progress,
            delta,
            max(2.0, sample_step * 0.55),
            line_width + 3,
            BAD_ROUTE_COLOR,
        )


def render_map(arena_map, map_dir: Path, output_dir: Path, args: argparse.Namespace) -> Path:
    map_id = str(arena_map.getId())
    mask_path = map_dir / f"{map_id}_mask.png"
    if not mask_path.exists():
        raise FileNotFoundError(f"missing mask for {map_id}: {mask_path}")

    image = Image.open(mask_path).convert("RGBA")
    draw = ImageDraw.Draw(image)
    rectangle = jpype.JClass("com.badlogic.gdx.math.Rectangle")
    bounds = arena_map.getBounds(rectangle())
    samples = route_samples(arena_map, args.sample_step)
    pixels = [world_to_pixel(point, bounds, image.width, image.height) for _, point in samples]
    if len(pixels) >= 2:
        closed_pixels = pixels + [pixels[0]]
        draw.line(closed_pixels, fill=ROUTE_OUTLINE, width=args.line_width + 4, joint="curve")
        draw.line(closed_pixels, fill=ROUTE_COLOR, width=args.line_width, joint="curve")

    font = ImageFont.load_default()
    route_length = float(arena_map.getRouteLength())
    if route_length > 0.001 and samples:
        label_count = 10
        for label_index in range(label_count):
            progress = route_length * label_index / label_count
            sample_index = min(len(samples) - 1, round(progress / route_length * (len(samples) - 1)))
            x, y = pixels[sample_index]
            draw_text(draw, (x + 6, y + 6), f"{label_index * 10}%", font)

        arrow_step = max(0.1, args.arrow_every)
        vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
        point = vector2()
        tangent = vector2()
        progress = 0.0
        while progress < route_length:
            arena_map.findRoutePoint(jpype.JFloat(progress), point)
            arena_map.findRouteTangent(jpype.JFloat(progress), tangent)
            px, py = world_to_pixel(point, bounds, image.width, image.height)
            tx = float(tangent.x)
            ty = -float(tangent.y)
            draw_arrow(draw, (px, py), (tx, ty), max(7.0, args.line_width * 2.2))
            progress += arrow_step

    draw_bad_route_segments(
        arena_map,
        draw,
        bounds,
        image.width,
        image.height,
        route_length,
        args.sample_step,
        args.line_width,
    )

    vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
    marker_point = vector2()
    for marker_index in range(int(arena_map.getRouteMarkerPointCount())):
        arena_map.getRouteMarkerPoint(marker_index, marker_point)
        x, y = world_to_pixel(marker_point, bounds, image.width, image.height)
        draw_text(
            draw,
            (x + 5, y + 5),
            str(marker_index + 1),
            font,
            fill=ROUTE_MARKER_TEXT_COLOR,
        )

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{map_id}_route_overlay.png"
    image.save(output_path, optimize=True)
    return output_path


def main() -> None:
    args = parse_args()
    start_jvm(project_path(Path(args.jar)).resolve())
    output_dir = project_path(Path(args.output_dir))
    map_dirs = args.map_dir or ["assets/maps", "tools/rl/trainingMaps"]
    written = []
    for map_dir_value in map_dirs:
        map_dir = project_path(Path(map_dir_value))
        maps = load_maps_from_directory(map_dir, args.map_scale)
        for index in range(int(maps.size)):
            written.append(render_map(maps.get(index), map_dir, output_dir, args))
    print(f"route_overlays_ready count={len(written)} output_dir={repo_relative(output_dir)}")
    for path in written:
        print(repo_relative(path))


if __name__ == "__main__":
    main()

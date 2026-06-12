#!/usr/bin/env python3
"""Prepare picture-map masks and annotate checkpoint order."""

from __future__ import annotations

import argparse
import math
import os
from pathlib import Path
from typing import Iterable, List, NamedTuple, Sequence, Tuple

import jpype
from PIL import Image, ImageDraw, ImageFont


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_MAP_DIR = REPO_ROOT / "tools" / "rl" / "trainingMaps"
DEFAULT_GAME_MAP_DIR = REPO_ROOT / "assets" / "maps"

Point = Tuple[float, float]
RED = (225, 28, 34)
BLUE = (28, 86, 230)
GREEN = (35, 210, 85)
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
LABEL_GRAY = (88, 88, 88)


class Component(NamedTuple):
    center_x: float
    center_y: float
    area: int
    min_x: int
    min_y: int
    max_x: int
    max_y: int
    axis_x: float
    axis_y: float


def is_red(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return red >= 150 and green <= 120 and blue <= 120


def is_blue(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return blue >= 140 and red <= 120 and green <= 170


def is_green(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return green >= 145 and red <= 125 and blue <= 135


def is_order_marker(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return red >= 245 and blue >= 245 and 1 <= green <= 250


def is_road(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return (red >= 145 and green >= 145 and blue >= 145) or is_checkpoint_label(color)


def is_checkpoint_label(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return 70 <= red <= 115 and abs(red - green) <= 3 and abs(green - blue) <= 3


def collect_components(
        image: Image.Image,
        predicate,
        min_area: int = 24) -> List[Component]:
    pixels = image.load()
    width, height = image.size
    visited = bytearray(width * height)
    components: List[Component] = []
    for y in range(height):
        for x in range(width):
            start = y * width + x
            if visited[start] or not predicate(pixels[x, y]):
                continue
            stack = [start]
            visited[start] = 1
            area = 0
            sum_x = 0.0
            sum_y = 0.0
            sum_xx = 0.0
            sum_yy = 0.0
            sum_xy = 0.0
            min_x = width
            min_y = height
            max_x = 0
            max_y = 0
            while stack:
                index = stack.pop()
                cx = index % width
                cy = index // width
                area += 1
                sum_x += cx
                sum_y += cy
                sum_xx += cx * cx
                sum_yy += cy * cy
                sum_xy += cx * cy
                min_x = min(min_x, cx)
                min_y = min(min_y, cy)
                max_x = max(max_x, cx)
                max_y = max(max_y, cy)
                for oy in (-1, 0, 1):
                    ny = cy + oy
                    if ny < 0 or ny >= height:
                        continue
                    row = ny * width
                    for ox in (-1, 0, 1):
                        if ox == 0 and oy == 0:
                            continue
                        nx = cx + ox
                        if nx < 0 or nx >= width:
                            continue
                        next_index = row + nx
                        if visited[next_index] or not predicate(pixels[nx, ny]):
                            continue
                        visited[next_index] = 1
                        stack.append(next_index)
            if area < min_area:
                continue
            center_x = sum_x / area
            center_y = sum_y / area
            cov_xx = sum_xx / area - center_x * center_x
            cov_yy = sum_yy / area - center_y * center_y
            cov_xy = sum_xy / area - center_x * center_y
            angle = 0.5 * math.atan2(2.0 * cov_xy, cov_xx - cov_yy)
            components.append(Component(
                center_x,
                center_y,
                area,
                min_x,
                min_y,
                max_x,
                max_y,
                math.cos(angle),
                math.sin(angle),
            ))
    return components


def road_mask(image: Image.Image) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    in_pixels = image.load()
    out_pixels = mask.load()
    width, height = image.size
    for y in range(height):
        for x in range(width):
            if is_road(in_pixels[x, y]):
                out_pixels[x, y] = 255
    return mask


def canonicalize_mask(image: Image.Image) -> Image.Image:
    source = image.convert("RGB")
    output = Image.new("RGB", source.size, BLACK)
    in_pixels = source.load()
    out_pixels = output.load()
    width, height = source.size
    for y in range(height):
        for x in range(width):
            color = in_pixels[x, y]
            if is_red(color):
                out_pixels[x, y] = RED
            elif is_blue(color):
                out_pixels[x, y] = BLUE
            elif is_green(color):
                out_pixels[x, y] = GREEN
            elif is_order_marker(color):
                out_pixels[x, y] = color
            elif is_road(color):
                out_pixels[x, y] = WHITE
    return output


def count_marker_pixels(image: Image.Image, predicate) -> int:
    colors = image.getcolors(maxcolors=4_000_000) or []
    return sum(count for count, color in colors if predicate(color))


def draw_marker(draw: ImageDraw.ImageDraw, point: Point, radius: int, color: Tuple[int, int, int]) -> None:
    x, y = point
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def spawn_grid_points(
        center: Point,
        tangent: Point,
        track_width: float,
        rows: int = 10) -> Iterable[Tuple[Point, Point]]:
    tx, ty = tangent
    normal = (-ty, tx)
    row_spacing = max(26.0, track_width * 0.28)
    column_spacing = max(18.0, track_width * 0.28)
    first_back_offset = max(24.0, track_width * 0.32)
    direction_offset = max(13.0, track_width * 0.18)
    for row in range(rows):
        base_x = center[0] - tx * (first_back_offset + row * row_spacing)
        base_y = center[1] - ty * (first_back_offset + row * row_spacing)
        for column in range(2):
            side = (-0.5 if column == 0 else 0.5) * column_spacing
            spawn = (base_x + normal[0] * side, base_y + normal[1] * side)
            direction = (spawn[0] + tx * direction_offset, spawn[1] + ty * direction_offset)
            yield spawn, direction


def point_on_road(mask: Image.Image, point: Point) -> bool:
    x = int(round(point[0]))
    y = int(round(point[1]))
    return 0 <= x < mask.size[0] and 0 <= y < mask.size[1] and mask.getpixel((x, y)) > 0


def marker_fits_road(mask: Image.Image, point: Point, radius: int) -> bool:
    return point_on_road(mask, point)


def choose_start_gate(green_components: Sequence[Component], image_size: Tuple[int, int]) -> Component:
    width, height = image_size
    return max(
        green_components,
        key=lambda component: (
            component.center_y / height,
            -abs(component.center_x / width - 0.25),
            -component.center_x / width,
        ),
    )


def add_spawn_grid_if_missing(image: Image.Image) -> bool:
    if count_marker_pixels(image, is_red) > 0 and count_marker_pixels(image, is_blue) > 0:
        return False
    green_components = collect_components(image, is_green)
    if not green_components:
        raise ValueError("Cannot add spawn grid: no green checkpoint gates were found.")
    road = road_mask(image)

    best_points: List[Tuple[Point, Point]] = []
    best_track_width = 95.0
    best_score = -1
    best_tie_breaker = -1.0
    preferred_gate = choose_start_gate(green_components, image.size)
    for gate in green_components:
        gate_width = max(gate.max_x - gate.min_x, gate.max_y - gate.min_y)
        track_width = max(40.0, gate_width * 1.20)
        spawn_radius = max(4, int(track_width // 18))
        direction_radius = max(3, int(track_width // 34))
        row_spacing = max(26.0, track_width * 0.28)
        normal_spacing = max(4.0, track_width * 0.08)
        axis = (gate.axis_x, gate.axis_y)
        tangents = [(-axis[1], axis[0]), (axis[1], -axis[0])]
        for tangent in tangents:
            for shift_index in range(13):
                shift = shift_index * row_spacing
                normal = (-tangent[1], tangent[0])
                for normal_index in range(-5, 6):
                    normal_shift = normal_index * normal_spacing
                    anchor = (
                        gate.center_x + tangent[0] * shift + normal[0] * normal_shift,
                        gate.center_y + tangent[1] * shift + normal[1] * normal_shift,
                    )
                    points = list(spawn_grid_points(anchor, tangent, track_width))
                    score = 0
                    for spawn, direction in points:
                        score += 1 if marker_fits_road(road, spawn, spawn_radius) else 0
                        score += 1 if marker_fits_road(road, direction, direction_radius) else 0
                    tie_breaker = (
                        (1.0 if gate is preferred_gate else 0.0)
                        - abs(gate.center_x / image.size[0] - 0.25) * 0.01
                        - gate.center_y / image.size[1] * 0.001
                    )
                    if score > best_score or (score == best_score and tie_breaker > best_tie_breaker):
                        best_score = score
                        best_tie_breaker = tie_breaker
                        best_points = points
                        best_track_width = track_width

    maximum_score = 40
    if best_score < int(maximum_score * 0.75):
        raise ValueError(
            "Cannot add spawn grid: no candidate placement kept enough markers on the road "
            f"({best_score}/{maximum_score}).")

    draw = ImageDraw.Draw(image)
    spawn_radius = max(4, int(best_track_width // 18))
    direction_radius = max(3, int(best_track_width // 34))
    for spawn, direction in best_points:
        draw_marker(draw, spawn, spawn_radius, RED)
        draw_marker(draw, direction, direction_radius, BLUE)
    return True


def font(size: int) -> ImageFont.ImageFont:
    for path in (
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def make_text_mask(label: str, label_font: ImageFont.ImageFont) -> Image.Image:
    measure = Image.new("L", (1, 1), 0)
    measure_draw = ImageDraw.Draw(measure)
    bbox = measure_draw.textbbox((0, 0), label, font=label_font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    text_mask = Image.new("L", (text_width, text_height), 0)
    text_draw = ImageDraw.Draw(text_mask)
    text_draw.text((-bbox[0], -bbox[1]), label, fill=255, font=label_font)
    return text_mask.point(lambda value: 255 if value >= 96 else 0)


def label_offsets(tangent: Point, normal: Point, step: float) -> Iterable[Point]:
    directions = [
        (0.0, 0.0),
        normal,
        (-normal[0], -normal[1]),
        tangent,
        (-tangent[0], -tangent[1]),
        (normal[0] + tangent[0], normal[1] + tangent[1]),
        (normal[0] - tangent[0], normal[1] - tangent[1]),
        (-normal[0] + tangent[0], -normal[1] + tangent[1]),
        (-normal[0] - tangent[0], -normal[1] - tangent[1]),
    ]
    for distance in (0.0, step, step * 1.45, step * 1.9, step * 2.4):
        for dx, dy in directions:
            length = math.hypot(dx, dy)
            if length <= 0.0001:
                yield (0.0, 0.0)
            else:
                yield (dx / length * distance, dy / length * distance)


def label_road_score(
        road: Image.Image,
        text_mask: Image.Image,
        x: int,
        y: int) -> Tuple[float, int]:
    if x < 1 or y < 1 or x + text_mask.size[0] >= road.size[0] - 1 or y + text_mask.size[1] >= road.size[1] - 1:
        return -1.0, 0
    road_pixels = road.load()
    text_pixels = text_mask.load()
    total = 0
    covered = 0
    for text_y in range(text_mask.size[1]):
        road_y = y + text_y
        for text_x in range(text_mask.size[0]):
            if text_pixels[text_x, text_y] == 0:
                continue
            total += 1
            if road_pixels[x + text_x, road_y] > 0:
                covered += 1
    if total == 0:
        return -1.0, 0
    return covered / float(total), covered


def draw_text_masked(
        image: Image.Image,
        road: Image.Image,
        text_mask: Image.Image,
        x: int,
        y: int) -> None:
    image_pixels = image.load()
    road_pixels = road.load()
    text_pixels = text_mask.load()
    for text_y in range(text_mask.size[1]):
        image_y = y + text_y
        for text_x in range(text_mask.size[0]):
            if text_pixels[text_x, text_y] == 0:
                continue
            image_x = x + text_x
            if 0 <= image_x < image.size[0] and 0 <= image_y < image.size[1] and road_pixels[image_x, image_y] > 0:
                image_pixels[image_x, image_y] = LABEL_GRAY


def draw_label_masked(
        image: Image.Image,
        road: Image.Image,
        label: str,
        point: Point,
        tangent: Point,
        normal: Point,
        base_size: int) -> None:
    best = None
    for size in range(base_size, 17, -3):
        text_mask = make_text_mask(label, font(size))
        best_for_size = None
        for offset in label_offsets(tangent, normal, max(18.0, size * 0.85)):
            x = int(round(point[0] + offset[0] - text_mask.size[0] * 0.5))
            y = int(round(point[1] + offset[1] - text_mask.size[1] * 0.5))
            ratio, covered = label_road_score(road, text_mask, x, y)
            distance = math.hypot(offset[0], offset[1])
            score = ratio * 1000.0 + covered * 0.02 - distance * 0.15
            candidate = (score, ratio, covered, size, text_mask, x, y)
            if best_for_size is None or candidate[:4] > best_for_size[:4]:
                best_for_size = candidate
        if best_for_size is not None and best_for_size[1] >= 0.86:
            best = best_for_size
            break
        if best is None or (best_for_size is not None and best_for_size[:4] > best[:4]):
            best = best_for_size
    if best is None:
        return
    _, _, _, _, text_mask, x, y = best
    draw_text_masked(image, road, text_mask, x, y)


def save_mask(image: Image.Image, output: Path) -> None:
    image.save(output, optimize=True, compress_level=9)


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


def remove_caches(map_dir: Path) -> None:
    for pattern in ("*.json.gz", "*.ser"):
        for path in map_dir.glob(pattern):
            path.unlink()


def prepare_masks(map_dir: Path, normalize_names: bool) -> List[Path]:
    source_paths = sorted(
        path for path in map_dir.iterdir()
        if path.is_file() and path.suffix.lower() == ".png"
    )
    if not source_paths:
        raise ValueError(f"No PNG masks found in {map_dir}")

    processed = []
    for index, source in enumerate(source_paths):
        image = canonicalize_mask(Image.open(source))
        added_spawns = add_spawn_grid_if_missing(image)
        output = map_dir / f"train{index:03d}_mask.png" if normalize_names else source
        processed.append((source, output, image, added_spawns))

    if normalize_names:
        for path in source_paths:
            path.unlink()
    for _, output, image, added_spawns in processed:
        save_mask(image, output)
        action = "normalized" if normalize_names else "prepared"
        print(f"{action}={output} added_spawn_grid={1 if added_spawns else 0}")
    return [output for _, output, _, _ in processed]


def normalize_masks(map_dir: Path) -> List[Path]:
    return prepare_masks(map_dir, True)


def load_training_maps():
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    return arena_maps.createHeadlessTrainingSet()


def load_maps_from_directory(map_dir: Path):
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
    return method.invoke(None, os.fspath(map_dir), jpype.JFloat(8.0), True)


def maps_by_id(map_dir: Path, map_source: str):
    if map_source == "training":
        maps = load_training_maps()
    elif map_source == "directory":
        maps = load_maps_from_directory(map_dir)
    else:
        raise ValueError(f"Unknown map source: {map_source}")
    return {str(maps.get(index).getId()): maps.get(index) for index in range(int(maps.size))}


def annotate_checkpoint_numbers(map_dir: Path, jar_path: Path, map_source: str) -> None:
    start_jvm(jar_path)
    rectangle = jpype.JClass("com.badlogic.gdx.math.Rectangle")
    by_id = maps_by_id(map_dir, map_source)

    for mask_path in sorted(map_dir.glob("*_mask.png")):
        map_id = mask_path.name[:-len("_mask.png")]
        arena_map = by_id.get(map_id)
        if arena_map is None:
            print(f"checkpoint_labels_skipped={mask_path} reason=not_loaded")
            continue
        image = Image.open(mask_path).convert("RGB")
        road = road_mask(image)
        bounds = arena_map.getBounds(rectangle())
        world_min_x = float(bounds.x)
        world_min_y = float(bounds.y)
        world_width = float(bounds.width)
        world_height = float(bounds.height)
        label_size = max(24, min(image.size) // 28)
        for checkpoint_index in range(int(arena_map.getCheckpointCount())):
            checkpoint = arena_map.getCheckpoint(checkpoint_index)
            pixel_x = (float(checkpoint.x) - world_min_x) * image.size[0] / world_width - 0.5
            pixel_y = (world_min_y + world_height - float(checkpoint.y)) * image.size[1] / world_height - 0.5
            forward_x = -math.sin(float(checkpoint.angleRad))
            forward_y = math.cos(float(checkpoint.angleRad))
            tangent = (forward_x, -forward_y)
            normal = (-tangent[1], tangent[0])
            draw_label_masked(
                image,
                road,
                str(checkpoint_index + 1),
                (pixel_x, pixel_y),
                tangent,
                normal,
                label_size,
            )
        save_mask(image, mask_path)
        print(f"checkpoint_labels={mask_path} count={int(arena_map.getCheckpointCount())}")


def prebuild_cache(map_dir: Path, jar_path: Path, map_source: str) -> None:
    start_jvm(jar_path)
    maps = maps_by_id(map_dir, map_source)
    print(f"map_cache_ready count={len(maps)} maps={','.join(sorted(maps))}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--map-dir", default=os.fspath(DEFAULT_MAP_DIR))
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument(
        "--mode",
        choices=("training", "game"),
        default="training",
        help="training normalizes PNG names to trainNNN; game preserves existing map names.",
    )
    parser.add_argument(
        "--prebuild-cache",
        action="store_true",
        help="Regenerate .json.gz metadata files after writing the final annotated masks.",
    )
    parser.add_argument(
        "--skip-labels",
        action="store_true",
        help="Do not draw checkpoint numbers. Existing label pixels are folded back into road.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    map_dir = Path(args.map_dir).resolve()
    if args.mode == "game" and args.map_dir == os.fspath(DEFAULT_MAP_DIR):
        map_dir = DEFAULT_GAME_MAP_DIR.resolve()
    jar_path = Path(args.jar).resolve()
    map_source = "training" if args.mode == "training" else "directory"
    remove_caches(map_dir)
    prepare_masks(map_dir, args.mode == "training")
    remove_caches(map_dir)
    if not args.skip_labels:
        annotate_checkpoint_numbers(map_dir, jar_path, map_source)
        remove_caches(map_dir)
    if args.prebuild_cache:
        prebuild_cache(map_dir, jar_path, map_source)


if __name__ == "__main__":
    main()

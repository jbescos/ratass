#!/usr/bin/env python3
"""Draw visible route annotations on map masks without changing gameplay geometry."""

from __future__ import annotations

import argparse
import math
import os
from pathlib import Path
from typing import Tuple

import jpype
from PIL import Image, ImageDraw, ImageFont


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_MAP_DIR = REPO_ROOT / "assets" / "maps"
DEFAULT_TRAINING_MAP_DIR = REPO_ROOT / "tools" / "rl" / "trainingMaps"
DEFAULT_MAP_SCALE = 8.0
BASE_WORLD_HEIGHT = 22.0

Point = Tuple[float, float]
WHITE = (255, 255, 255)
RED = (225, 28, 34)
BLUE = (28, 86, 230)
GREEN = (35, 210, 85)
LABEL_GRAY = (88, 88, 88)
ROUTE_HINT_CYAN = (0, 220, 220)


def start_jvm(jar_path: Path) -> None:
    if jpype.isJVMStarted():
        return
    if not jar_path.exists():
        raise FileNotFoundError(f"{jar_path} does not exist. Run `mvn -DskipTests package` first.")
    jpype.startJVM(jpype.getDefaultJVMPath(), "-Xrs", classpath=[os.fspath(jar_path)])
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


def is_order_marker(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return red >= 245 and blue >= 245 and 1 <= green <= 250 and red >= green + 3 and blue >= green + 3


def is_checkpoint_label(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return 70 <= red <= 115 and abs(red - green) <= 3 and abs(green - blue) <= 3


def is_route_label(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return 40 <= red <= 245 and max(red, green, blue) - min(red, green, blue) <= 12


def is_road(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return red >= 145 and green >= 145 and blue >= 145


def is_red_marker(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return red >= 150 and green <= 120 and blue <= 120


def is_blue_marker(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return blue >= 140 and red <= 120 and green <= 170


def is_green_marker(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return green >= 145 and red <= 125 and blue <= 135


def is_route_hint_marker(color: Tuple[int, int, int]) -> bool:
    red, green, blue = color
    return (
        red <= 130
        and green >= 35
        and blue >= 35
        and abs(green - blue) <= 24
        and green >= red + 28
        and blue >= red + 28)


def protected_marker_color(color: Tuple[int, int, int]) -> Tuple[int, int, int] | None:
    if is_red_marker(color):
        return RED
    if is_blue_marker(color):
        return BLUE
    if is_green_marker(color):
        return GREEN
    return None


def is_existing_annotation(color: Tuple[int, int, int]) -> bool:
    return (
        is_order_marker(color)
        or is_checkpoint_label(color)
        or is_route_label(color)
        or is_route_hint_marker(color))


def nearby_protected_marker_color(
        image: Image.Image,
        x: int,
        y: int,
        radius: int = 4) -> Tuple[int, int, int] | None:
    pixels = image.load()
    for offset_y in range(-radius, radius + 1):
        py = y + offset_y
        if py < 0 or py >= image.size[1]:
            continue
        for offset_x in range(-radius, radius + 1):
            px = x + offset_x
            if px < 0 or px >= image.size[0]:
                continue
            marker_color = protected_marker_color(pixels[px, py])
            if marker_color is not None:
                return marker_color
    return None


def clear_existing_annotations(image: Image.Image) -> None:
    pixels = image.load()
    for y in range(image.size[1]):
        for x in range(image.size[0]):
            color = pixels[x, y]
            if is_route_hint_marker(color):
                pixels[x, y] = ROUTE_HINT_CYAN
            elif is_order_marker(color) or is_checkpoint_label(color) or is_route_label(color):
                pixels[x, y] = WHITE


def load_font(size: int) -> ImageFont.ImageFont:
    for path in (
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"):
        try:
            return ImageFont.truetype(path, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def make_text_mask(label: str, font: ImageFont.ImageFont) -> Image.Image:
    measure = Image.new("L", (1, 1), 0)
    bbox = ImageDraw.Draw(measure).textbbox((0, 0), label, font=font)
    width = bbox[2] - bbox[0]
    height = bbox[3] - bbox[1]
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).text((-bbox[0], -bbox[1]), label, fill=255, font=font)
    return mask.point(lambda value: 255 if value >= 96 else 0)


def text_fits_road(
        image: Image.Image,
        mask: Image.Image,
        x: int,
        y: int,
        min_coverage: float = 0.92,
        avoid_existing_annotations: bool = True) -> bool:
    pixels = image.load()
    mask_pixels = mask.load()
    total = 0
    covered = 0
    for ty in range(mask.size[1]):
        py = y + ty
        if py < 0 or py >= image.size[1]:
            continue
        for tx in range(mask.size[0]):
            if mask_pixels[tx, ty] == 0:
                continue
            total += 1
            px = x + tx
            if not 0 <= px < image.size[0]:
                continue
            color = pixels[px, py]
            if (
                    protected_marker_color(color) is not None
                    or nearby_protected_marker_color(image, px, py, radius=7) is not None
                    or is_route_hint_marker(color)):
                return False
            if avoid_existing_annotations and is_existing_annotation(color):
                return False
            if is_road(color) or is_order_marker(color) or is_checkpoint_label(color) or is_route_label(color):
                covered += 1
    return total > 0 and covered / float(total) >= min_coverage


def draw_route_label(
        image: Image.Image,
        label: str,
        point: Point,
        tangent: Point,
        font: ImageFont.ImageFont,
        offset_step: float) -> bool:
    mask = make_text_mask(label, font)
    tx, ty = tangent
    normal = (-ty, tx)
    base_offsets = (
        (normal[0] * offset_step, normal[1] * offset_step),
        (-normal[0] * offset_step, -normal[1] * offset_step),
        (tx * offset_step, ty * offset_step),
        (-tx * offset_step, -ty * offset_step),
        ((normal[0] + tx) * offset_step, (normal[1] + ty) * offset_step),
        ((normal[0] - tx) * offset_step, (normal[1] - ty) * offset_step),
        ((-normal[0] + tx) * offset_step, (-normal[1] + ty) * offset_step),
        ((-normal[0] - tx) * offset_step, (-normal[1] - ty) * offset_step),
        (0.0, 0.0),
    )
    offsets = []
    for multiplier in (1.0, 1.45, 1.9, 2.35):
        for dx, dy in base_offsets:
            offsets.append((dx * multiplier, dy * multiplier))
    for avoid_existing_annotations, min_coverage in ((True, 0.92), (False, 0.82), (False, 0.68)):
        for dx, dy in offsets:
            x = int(round(point[0] + dx - mask.size[0] * 0.5))
            y = int(round(point[1] + dy - mask.size[1] * 0.5))
            if text_fits_road(
                    image,
                    mask,
                    x,
                    y,
                    min_coverage=min_coverage,
                    avoid_existing_annotations=avoid_existing_annotations):
                image.paste(LABEL_GRAY, (x, y), mask)
                return True
    return False


def draw_route_dot(draw: ImageDraw.ImageDraw, point: Point, order: int, radius: int) -> None:
    x, y = point
    draw.ellipse(
        (x - radius, y - radius, x + radius, y + radius),
        fill=(255, min(250, max(1, order)), 255))


def route_dot_fits(image: Image.Image, point: Point, radius: int) -> bool:
    pixels = image.load()
    center_x, center_y = point
    if nearby_protected_marker_color(image, int(round(center_x)), int(round(center_y)), radius=16) is not None:
        return False
    radius_squared = radius * radius
    for y in range(math.floor(center_y - radius), math.ceil(center_y + radius) + 1):
        if y < 0 or y >= image.size[1]:
            continue
        for x in range(math.floor(center_x - radius), math.ceil(center_x + radius) + 1):
            if x < 0 or x >= image.size[0]:
                continue
            if (x - center_x) * (x - center_x) + (y - center_y) * (y - center_y) > radius_squared:
                continue
            if protected_marker_color(pixels[x, y]) is not None or is_existing_annotation(pixels[x, y]):
                return False
    return True


def world_to_image(
        world_x: float,
        world_y: float,
        image_width: int,
        image_height: int,
        map_scale: float) -> Point:
    world_height = BASE_WORLD_HEIGHT * map_scale
    world_width = world_height * image_width / float(image_height)
    pixel_x = (world_x + world_width * 0.5) * image_width / world_width
    pixel_y = (world_height * 0.5 - world_y) * image_height / world_height
    return pixel_x, pixel_y


def map_marker_count(route_length: float) -> int:
    return max(18, min(48, round(route_length / 20.0)))


def should_skip_route_marker(map_id: str, source_order: int, pixel: Point) -> bool:
    return False


def annotate_map(mask_path: Path, arena_map, map_scale: float) -> None:
    image = Image.open(mask_path).convert("RGB")
    clear_existing_annotations(image)
    draw = ImageDraw.Draw(image)
    vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
    map_id = mask_path.name[:-len("_mask.png")]
    route_length = float(arena_map.getRouteLength())
    marker_count = map_marker_count(route_length)
    step = route_length / marker_count
    marker_radius = max(4, min(image.size) // 180)
    label_font = load_font(max(14, min(image.size) // 64))
    label_offset = max(12.0, marker_radius * 2.4)
    point = vector2()
    next_point = vector2()
    drawn_order = 1
    drawn_labels = 0
    for source_order in range(1, marker_count + 1):
        progress = (source_order - 1) * step + step * 0.35
        arena_map.findRoutePoint(progress, point)
        arena_map.findRoutePoint(progress + max(0.75, step * 0.08), next_point)
        pixel = world_to_image(float(point.x), float(point.y), image.size[0], image.size[1], map_scale)
        if should_skip_route_marker(map_id, source_order, pixel):
            continue
        next_pixel = world_to_image(float(next_point.x), float(next_point.y), image.size[0], image.size[1], map_scale)
        tangent_x = next_pixel[0] - pixel[0]
        tangent_y = next_pixel[1] - pixel[1]
        tangent_len = math.hypot(tangent_x, tangent_y)
        tangent = (1.0, 0.0) if tangent_len <= 0.001 else (tangent_x / tangent_len, tangent_y / tangent_len)
        if route_dot_fits(image, pixel, marker_radius + 1):
            draw_route_dot(draw, pixel, drawn_order, marker_radius)
            if draw_route_label(image, str(drawn_order), pixel, tangent, label_font, label_offset):
                drawn_labels += 1
            draw_route_dot(draw, pixel, drawn_order, marker_radius)
            drawn_order += 1
    image.save(mask_path, optimize=True, compress_level=9)
    print(
        f"route_annotations={mask_path} markers={drawn_order - 1} "
        f"labels={drawn_labels} route_length={route_length:.3f}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--map-set",
        choices=("game", "training"),
        default="game",
        help="game annotates map*_mask.png; training annotates train*_mask.png.")
    parser.add_argument("--map-dir", default="")
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--map-scale", type=float, default=DEFAULT_MAP_SCALE)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    default_dir = DEFAULT_MAP_DIR if args.map_set == "game" else DEFAULT_TRAINING_MAP_DIR
    map_dir = Path(args.map_dir or os.fspath(default_dir)).resolve()
    start_jvm(Path(args.jar).resolve())
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    if args.map_set == "training":
        maps = arena_maps.createHeadlessTrainingSet(float(args.map_scale))
        pattern = "train*_mask.png"
    else:
        maps = arena_maps.createDefaultSet(float(args.map_scale))
        pattern = "map*_mask.png"
    by_id = {str(maps.get(index).getId()): maps.get(index) for index in range(int(maps.size))}
    for mask_path in sorted(map_dir.glob(pattern)):
        map_id = mask_path.name[:-len("_mask.png")]
        arena_map = by_id.get(map_id)
        if arena_map is None or not bool(arena_map.hasRoute()):
            print(f"route_annotations_skipped={mask_path} reason=no_route")
            continue
        annotate_map(mask_path, arena_map, float(args.map_scale))


if __name__ == "__main__":
    main()

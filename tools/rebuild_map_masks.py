#!/usr/bin/env python3
"""Rebuild gameplay masks so they match the visible map PNG dimensions."""

from __future__ import annotations

import argparse
import math
import random
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple

from PIL import Image, ImageDraw, ImageFilter

Point = Tuple[float, float]

BLACK = (0, 0, 0)
BLUE = (28, 86, 230)
RED = (225, 28, 34)
WHITE = (255, 255, 255)


def is_red(pixel: Tuple[int, int, int]) -> bool:
    red, green, blue = pixel
    return red >= 150 and green <= 120 and blue <= 120


def is_blue(pixel: Tuple[int, int, int]) -> bool:
    red, green, blue = pixel
    return blue >= 140 and red <= 120 and green <= 170


def is_playable_mask_pixel(pixel: Tuple[int, int, int]) -> bool:
    red, green, blue = pixel
    return (
        (red >= 145 and green >= 145 and blue >= 145)
        or is_red(pixel)
        or is_blue(pixel)
    )


def load_playable(mask_path: Path, surface: Image.Image) -> Image.Image:
    if not mask_path.exists():
        raise FileNotFoundError(f"Missing source mask for {surface.filename}: {mask_path}")
    with Image.open(mask_path).convert("RGB") as mask:
        if mask.size != surface.size:
            mask = mask.resize(surface.size, Image.Resampling.NEAREST)
        playable = Image.new("L", mask.size, 0)
        source = mask.load()
        target = playable.load()
        for y in range(mask.height):
            for x in range(mask.width):
                target[x, y] = 255 if is_playable_mask_pixel(source[x, y]) else 0
    return playable


def point_safe(playable: Image.Image, x: float, y: float, margin: int) -> bool:
    width, height = playable.size
    if x < margin or y < margin or x >= width - margin or y >= height - margin:
        return False
    pixels = playable.load()
    center_x = int(round(x))
    center_y = int(round(y))
    if pixels[center_x, center_y] < 240:
        return False
    for i in range(20):
        angle = i * math.tau / 20
        px = int(round(x + math.cos(angle) * margin))
        py = int(round(y + math.sin(angle) * margin))
        if px < 0 or py < 0 or px >= width or py >= height or pixels[px, py] < 240:
            return False
    return True


def line_safe(playable: Image.Image, start: Point, end: Point, margin: int) -> bool:
    distance = math.hypot(end[0] - start[0], end[1] - start[1])
    steps = max(2, int(distance / max(2, margin * 0.45)))
    for step in range(1, steps + 1):
        alpha = step / steps
        x = start[0] + (end[0] - start[0]) * alpha
        y = start[1] + (end[1] - start[1]) * alpha
        if not point_safe(playable, x, y, margin):
            return False
    return True


def playable_centroid(playable: Image.Image) -> Point:
    pixels = playable.load()
    step = max(1, min(playable.size) // 180)
    total_x = 0.0
    total_y = 0.0
    count = 0
    for y in range(0, playable.height, step):
        for x in range(0, playable.width, step):
            if pixels[x, y] >= 240:
                total_x += x
                total_y += y
                count += 1
    if count == 0:
        return playable.width * 0.5, playable.height * 0.5
    return total_x / count, total_y / count


def collect_candidates(playable: Image.Image, margin: int, step: int) -> List[Point]:
    width, height = playable.size
    candidates: List[Point] = []
    for y in range(margin, height - margin, step):
        for x in range(margin, width - margin, step):
            if point_safe(playable, x, y, margin):
                candidates.append((float(x), float(y)))
    return candidates


def select_spawns(playable: Image.Image, count: int, seed: int) -> List[Point]:
    width, height = playable.size
    rng = random.Random(seed)
    min_side = min(width, height)
    margin = max(12, min_side // 62)
    step = max(14, min_side // 58)
    candidates = collect_candidates(playable, margin, step)
    while len(candidates) < count and margin > 7:
        margin = max(7, int(margin * 0.78))
        step = max(9, int(step * 0.82))
        candidates = collect_candidates(playable, margin, step)
    if len(candidates) < count:
        raise RuntimeError(f"not enough safe spawn candidates: {len(candidates)} < {count}")

    center_x, center_y = playable_centroid(playable)
    rng.shuffle(candidates)
    chosen: List[Point] = []
    min_distance = max(24.0, min_side * 0.042)

    for index in range(count):
        target_angle = -math.pi / 2 + index * math.tau / count
        for relaxation in range(7):
            required = min_distance * (0.86 ** relaxation)
            best: Point | None = None
            best_score = -1.0e30
            for x, y in candidates:
                if chosen:
                    nearest_squared = min((x - sx) ** 2 + (y - sy) ** 2 for sx, sy in chosen)
                    if nearest_squared < required * required:
                        continue
                    nearest = math.sqrt(nearest_squared)
                else:
                    nearest = min_side
                angle = math.atan2(y - center_y, x - center_x)
                diff = abs(math.atan2(math.sin(angle - target_angle), math.cos(angle - target_angle)))
                radius = math.hypot(x - center_x, y - center_y)
                score = nearest * 1.35 + radius * 0.12 - diff * min_side * 0.18 + rng.random() * 3.0
                if score > best_score:
                    best = (x, y)
                    best_score = score
            if best is not None:
                chosen.append(best)
                break
        else:
            best = max(
                candidates,
                key=lambda point: min((point[0] - sx) ** 2 + (point[1] - sy) ** 2 for sx, sy in chosen),
            )
            chosen.append(best)
    return chosen


def best_direction_target(playable: Image.Image, start: Point, center: Point, offset: int, margin: int) -> Point:
    preferred = math.atan2(center[1] - start[1], center[0] - start[0])
    best: Point | None = None
    best_score = -1.0e30
    for distance_scale in (1.0, 0.78, 0.58):
        distance = offset * distance_scale
        for index in range(32):
            angle = index * math.tau / 32
            x = start[0] + math.cos(angle) * distance
            y = start[1] + math.sin(angle) * distance
            if not line_safe(playable, start, (x, y), margin):
                continue
            diff = abs(math.atan2(math.sin(angle - preferred), math.cos(angle - preferred)))
            safe_bonus = 0
            for extra in (margin + 4, margin + 8, margin + 12):
                if point_safe(playable, x, y, extra):
                    safe_bonus += 1
            score = -diff * 100.0 + safe_bonus * 12.0 + distance_scale * 5.0
            if score > best_score:
                best = (x, y)
                best_score = score
        if best is not None:
            return best
    return start


def marker_components(mask: Image.Image, color_test) -> List[Point]:
    pixels = mask.load()
    width, height = mask.size
    visited = bytearray(width * height)
    out: List[Point] = []
    for y in range(height):
        for x in range(width):
            index = y * width + x
            if visited[index] or not color_test(pixels[x, y]):
                continue
            stack = [(x, y)]
            visited[index] = 1
            total_x = 0.0
            total_y = 0.0
            area = 0
            while stack:
                sx, sy = stack.pop()
                total_x += sx
                total_y += sy
                area += 1
                for ny in range(sy - 1, sy + 2):
                    if ny < 0 or ny >= height:
                        continue
                    for nx in range(sx - 1, sx + 2):
                        if nx < 0 or nx >= width:
                            continue
                        next_index = ny * width + nx
                        if visited[next_index] or not color_test(pixels[nx, ny]):
                            continue
                        visited[next_index] = 1
                        stack.append((nx, ny))
            if area >= 12:
                out.append((total_x / area, total_y / area))
    return out


def draw_markers(playable: Image.Image, spawn_count: int, seed: int) -> Image.Image:
    mask_rgb = Image.new("RGB", playable.size, BLACK)
    mask_rgb.paste(WHITE, mask=playable)
    draw = ImageDraw.Draw(mask_rgb)
    min_side = min(playable.size)
    red_radius = max(5, min_side // 138)
    blue_radius = max(5, min_side // 146)
    direction_offset = max(18, min_side // 42)
    line_margin = max(5, blue_radius + 2)
    center = playable_centroid(playable)
    spawns = select_spawns(playable, spawn_count, seed)

    for x, y in spawns:
        target = best_direction_target(playable, (x, y), center, direction_offset, line_margin)
        draw.ellipse((x - red_radius, y - red_radius, x + red_radius, y + red_radius), fill=RED)
        draw.ellipse(
            (
                target[0] - blue_radius,
                target[1] - blue_radius,
                target[0] + blue_radius,
                target[1] + blue_radius,
            ),
            fill=BLUE,
        )
    return mask_rgb


def rebuild_mask(surface_path: Path, spawn_count: int, seed: int, output_dir: Path | None) -> None:
    mask_path = surface_path.with_name(f"{surface_path.stem}_mask.png")
    with Image.open(surface_path).convert("RGB") as surface:
        playable = load_playable(mask_path, surface)
        playable = playable.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
        mask = draw_markers(playable, spawn_count, seed + int(surface_path.stem[-3:]))
        output_path = (output_dir / mask_path.name) if output_dir else mask_path
        output_path.parent.mkdir(parents=True, exist_ok=True)
        mask.save(output_path)
        red_count = len(marker_components(mask, is_red))
        blue_count = len(marker_components(mask, is_blue))
        print(
            f"rebuilt={output_path} size={mask.width}x{mask.height} "
            f"red={red_count} blue={blue_count}"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--maps-dir", default="assets/maps")
    parser.add_argument("--start", type=int, default=0)
    parser.add_argument("--end", type=int, default=20)
    parser.add_argument("--spawns", type=int, default=50)
    parser.add_argument("--seed", type=int, default=20260522)
    parser.add_argument("--output-dir", default="")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    maps_dir = Path(args.maps_dir)
    output_dir = Path(args.output_dir) if args.output_dir else None
    for surface_path in sorted(maps_dir.glob("map[0-9][0-9][0-9].png")):
        index = int(surface_path.stem[-3:])
        if args.start <= index <= args.end:
            rebuild_mask(surface_path, args.spawns, args.seed, output_dir)


if __name__ == "__main__":
    main()

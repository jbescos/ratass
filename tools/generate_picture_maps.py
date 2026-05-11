#!/usr/bin/env python3
"""Generate procedural picture-map PNGs and gameplay masks.

The generated mask follows assets/maps/README.md:
white = playable, black = void, red = spawn, blue = spawn facing marker.
"""

from __future__ import annotations

import argparse
import math
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable, List, Sequence, Tuple

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageOps

Point = Tuple[float, float]
Builder = Callable[[ImageDraw.ImageDraw, int, int], None]

RED = (225, 28, 34)
BLUE = (28, 86, 230)
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)


@dataclass(frozen=True)
class MapSpec:
    index: int
    size: Tuple[int, int]
    builder: Builder
    floor: Tuple[int, int, int]
    accent: Tuple[int, int, int]
    void: Tuple[int, int, int]
    motif: str
    seed: int


def ellipse(draw: ImageDraw.ImageDraw, cx: float, cy: float, rx: float, ry: float, fill: int) -> None:
    draw.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=fill)


def rect(draw: ImageDraw.ImageDraw, cx: float, cy: float, rw: float, rh: float, fill: int) -> None:
    draw.rectangle((cx - rw, cy - rh, cx + rw, cy + rh), fill=fill)


def capsule(
    draw: ImageDraw.ImageDraw,
    a: Point,
    b: Point,
    width: float,
    fill: int,
) -> None:
    draw.line((a, b), fill=fill, width=max(1, round(width)))
    radius = width * 0.5
    ellipse(draw, a[0], a[1], radius, radius, fill)
    ellipse(draw, b[0], b[1], radius, radius, fill)


def polyline(
    draw: ImageDraw.ImageDraw,
    points: Sequence[Point],
    width: float,
    fill: int,
) -> None:
    draw.line(points, fill=fill, width=max(1, round(width)), joint="curve")
    radius = width * 0.5
    for x, y in points:
        ellipse(draw, x, y, radius, radius, fill)


def star(cx: float, cy: float, outer: float, inner: float, points: int, twist: float = -math.pi / 2) -> List[Point]:
    polygon: List[Point] = []
    for i in range(points * 2):
        radius = outer if i % 2 == 0 else inner
        angle = twist + i * math.pi / points
        polygon.append((cx + math.cos(angle) * radius, cy + math.sin(angle) * radius))
    return polygon


def build_clover(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    cx, cy = w * 0.5, h * 0.5
    r = min(w, h)
    ellipse(draw, cx, cy, r * 0.26, r * 0.24, 255)
    for i in range(5):
        angle = -math.pi / 2 + i * math.tau / 5
        lx = cx + math.cos(angle) * w * 0.22
        ly = cy + math.sin(angle) * h * 0.25
        capsule(draw, (cx, cy), (lx, ly), r * 0.28, 255)
        ellipse(draw, lx, ly, r * 0.24, r * 0.20, 255)
    for x, y, rx, ry in (
        (0.37, 0.38, 0.055, 0.060),
        (0.64, 0.45, 0.045, 0.055),
        (0.48, 0.66, 0.052, 0.050),
    ):
        ellipse(draw, w * x, h * y, w * rx, h * ry, 0)


def build_neon_zigzag(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    points = [
        (w * 0.09, h * 0.56),
        (w * 0.24, h * 0.34),
        (w * 0.39, h * 0.62),
        (w * 0.55, h * 0.38),
        (w * 0.72, h * 0.65),
        (w * 0.91, h * 0.44),
    ]
    polyline(draw, points, h * 0.33, 255)
    for x, y in points:
        ellipse(draw, x, y, h * 0.19, h * 0.17, 255)
    ellipse(draw, w * 0.50, h * 0.50, w * 0.035, h * 0.050, 0)
    ellipse(draw, w * 0.28, h * 0.62, w * 0.040, h * 0.070, 0)
    ellipse(draw, w * 0.74, h * 0.35, w * 0.050, h * 0.060, 0)


def build_glacier(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    cx, cy = w * 0.5, h * 0.5
    ellipse(draw, cx, h * 0.31, w * 0.30, h * 0.24, 255)
    ellipse(draw, cx, h * 0.69, w * 0.30, h * 0.24, 255)
    capsule(draw, (cx, h * 0.30), (cx, h * 0.70), w * 0.24, 255)
    capsule(draw, (w * 0.20, cy), (w * 0.80, cy), h * 0.22, 255)
    ellipse(draw, cx, cy, w * 0.080, h * 0.090, 0)
    ellipse(draw, w * 0.23, h * 0.27, w * 0.045, h * 0.055, 0)
    ellipse(draw, w * 0.78, h * 0.73, w * 0.045, h * 0.055, 0)


def build_spiral(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    cx, cy = w * 0.50, h * 0.51
    points: List[Point] = []
    for i in range(145):
        t = 0.75 + i * 4.9 * math.pi / 144
        radius = (0.055 + i / 144 * 0.42) * min(w, h)
        points.append((cx + math.cos(t) * radius * 1.22, cy + math.sin(t) * radius))
    polyline(draw, points, h * 0.20, 255)
    ellipse(draw, cx, cy, h * 0.18, h * 0.16, 255)
    ellipse(draw, w * 0.17, h * 0.72, h * 0.18, h * 0.15, 255)
    ellipse(draw, w * 0.78, h * 0.24, h * 0.16, h * 0.14, 255)
    ellipse(draw, w * 0.52, h * 0.51, h * 0.060, h * 0.052, 0)


def build_volcano(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    left = (w * 0.31, h * 0.51)
    right = (w * 0.69, h * 0.49)
    ellipse(draw, left[0], left[1], w * 0.23, h * 0.30, 255)
    ellipse(draw, right[0], right[1], w * 0.23, h * 0.30, 255)
    capsule(draw, left, right, h * 0.22, 255)
    capsule(draw, (w * 0.18, h * 0.26), (w * 0.82, h * 0.72), h * 0.16, 255)
    ellipse(draw, left[0], left[1], w * 0.070, h * 0.095, 0)
    ellipse(draw, right[0], right[1], w * 0.070, h * 0.095, 0)
    rect(draw, w * 0.50, h * 0.50, w * 0.040, h * 0.060, 0)


def build_cyber_grid(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    for x in (0.24, 0.50, 0.76):
        rect(draw, w * x, h * 0.50, w * 0.080, h * 0.38, 255)
    for y in (0.26, 0.50, 0.74):
        rect(draw, w * 0.50, h * y, w * 0.36, h * 0.075, 255)
    for x in (0.24, 0.50, 0.76):
        for y in (0.26, 0.50, 0.74):
            ellipse(draw, w * x, h * y, h * 0.095, h * 0.095, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.075, h * 0.070, 0)
    ellipse(draw, w * 0.24, h * 0.50, w * 0.040, h * 0.070, 0)
    ellipse(draw, w * 0.76, h * 0.50, w * 0.040, h * 0.070, 0)


def build_harbor(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    rect(draw, w * 0.50, h * 0.50, w * 0.42, h * 0.105, 255)
    for x in (0.18, 0.36, 0.64, 0.82):
        rect(draw, w * x, h * 0.38, w * 0.055, h * 0.24, 255)
        rect(draw, w * x, h * 0.64, w * 0.055, h * 0.23, 255)
    ellipse(draw, w * 0.50, h * 0.50, w * 0.13, h * 0.15, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.055, h * 0.060, 0)
    ellipse(draw, w * 0.18, h * 0.23, w * 0.040, h * 0.045, 0)
    ellipse(draw, w * 0.82, h * 0.77, w * 0.040, h * 0.045, 0)


def build_lunar(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    cx, cy = w * 0.5, h * 0.5
    ellipse(draw, cx, cy, w * 0.40, h * 0.41, 255)
    capsule(draw, (w * 0.20, h * 0.72), (w * 0.80, h * 0.28), h * 0.18, 255)
    for x, y, rx, ry in (
        (0.38, 0.39, 0.065, 0.070),
        (0.59, 0.57, 0.055, 0.070),
        (0.49, 0.71, 0.052, 0.048),
        (0.66, 0.31, 0.050, 0.046),
    ):
        ellipse(draw, w * x, h * y, w * rx, h * ry, 0)


def build_crystal(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    cx, cy = w * 0.5, h * 0.5
    draw.polygon(star(cx, cy, min(w, h) * 0.44, min(w, h) * 0.25, 8), fill=255)
    ellipse(draw, cx, cy, w * 0.20, h * 0.20, 255)
    capsule(draw, (w * 0.18, h * 0.50), (w * 0.82, h * 0.50), h * 0.12, 255)
    capsule(draw, (w * 0.50, h * 0.18), (w * 0.50, h * 0.82), h * 0.12, 255)
    for polygon in (
        [(w * 0.39, h * 0.31), (w * 0.46, h * 0.42), (w * 0.34, h * 0.43)],
        [(w * 0.61, h * 0.69), (w * 0.53, h * 0.58), (w * 0.67, h * 0.56)],
        [(w * 0.69, h * 0.35), (w * 0.77, h * 0.45), (w * 0.66, h * 0.47)],
    ):
        draw.polygon(polygon, fill=0)


def build_ruins(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    rect(draw, w * 0.50, h * 0.50, w * 0.33, h * 0.33, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.46, h * 0.10, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.10, h * 0.46, 255)
    for x in (0.22, 0.78):
        for y in (0.22, 0.78):
            ellipse(draw, w * x, h * y, h * 0.11, h * 0.11, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.105, h * 0.105, 0)
    ellipse(draw, w * 0.31, h * 0.69, w * 0.045, h * 0.050, 0)
    ellipse(draw, w * 0.69, h * 0.31, w * 0.045, h * 0.050, 0)


def build_factory(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    cx, cy = w * 0.50, h * 0.50
    ellipse(draw, cx, cy, w * 0.25, h * 0.32, 255)
    for i in range(10):
        angle = i * math.tau / 10
        tx = cx + math.cos(angle) * w * 0.24
        ty = cy + math.sin(angle) * h * 0.25
        ellipse(draw, tx, ty, w * 0.085, h * 0.090, 255)
    capsule(draw, (w * 0.15, h * 0.32), (w * 0.85, h * 0.68), h * 0.16, 255)
    capsule(draw, (w * 0.16, h * 0.69), (w * 0.84, h * 0.31), h * 0.13, 255)
    ellipse(draw, cx, cy, w * 0.080, h * 0.100, 0)
    ellipse(draw, w * 0.34, h * 0.24, w * 0.045, h * 0.060, 0)
    ellipse(draw, w * 0.66, h * 0.76, w * 0.045, h * 0.060, 0)


def build_sky_loop(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    centers = [
        (0.22, 0.35),
        (0.41, 0.27),
        (0.61, 0.36),
        (0.78, 0.55),
        (0.56, 0.73),
        (0.31, 0.66),
    ]
    pixel_centers = [(w * x, h * y) for x, y in centers]
    for a, b in zip(pixel_centers, pixel_centers[1:] + pixel_centers[:1]):
        capsule(draw, a, b, h * 0.13, 255)
    for x, y in pixel_centers:
        ellipse(draw, x, y, w * 0.105, h * 0.125, 255)
    ellipse(draw, w * 0.50, h * 0.50, w * 0.14, h * 0.14, 255)
    ellipse(draw, w * 0.50, h * 0.50, w * 0.055, h * 0.060, 0)
    ellipse(draw, w * 0.72, h * 0.33, w * 0.040, h * 0.050, 0)


def build_courtyard(draw: ImageDraw.ImageDraw, w: int, h: int) -> None:
    rect(draw, w * 0.50, h * 0.50, w * 0.26, h * 0.26, 255)
    rect(draw, w * 0.50, h * 0.20, w * 0.18, h * 0.095, 255)
    rect(draw, w * 0.50, h * 0.80, w * 0.18, h * 0.095, 255)
    rect(draw, w * 0.20, h * 0.50, w * 0.095, h * 0.18, 255)
    rect(draw, w * 0.80, h * 0.50, w * 0.095, h * 0.18, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.09, h * 0.42, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.42, h * 0.09, 255)
    for x in (0.25, 0.75):
        for y in (0.25, 0.75):
            ellipse(draw, w * x, h * y, h * 0.10, h * 0.10, 255)
    rect(draw, w * 0.50, h * 0.50, w * 0.070, h * 0.070, 0)
    ellipse(draw, w * 0.35, h * 0.35, w * 0.035, h * 0.045, 0)
    ellipse(draw, w * 0.65, h * 0.65, w * 0.035, h * 0.045, 0)


SPECS = [
    MapSpec(8, (1536, 1024), build_clover, (73, 132, 75), (220, 207, 124), (13, 37, 32), "garden", 8008),
    MapSpec(9, (1672, 941), build_neon_zigzag, (39, 43, 62), (41, 221, 231), (9, 8, 20), "neon", 8009),
    MapSpec(10, (1254, 1254), build_glacier, (117, 184, 205), (239, 249, 255), (12, 46, 66), "ice", 8010),
    MapSpec(11, (1536, 1024), build_spiral, (175, 130, 63), (239, 197, 91), (48, 27, 11), "desert", 8011),
    MapSpec(12, (1672, 941), build_volcano, (79, 63, 58), (255, 119, 37), (37, 8, 5), "volcano", 8012),
    MapSpec(13, (1254, 1254), build_cyber_grid, (40, 50, 56), (69, 242, 181), (4, 10, 15), "circuit", 8013),
    MapSpec(14, (1536, 1024), build_harbor, (128, 91, 55), (88, 183, 184), (6, 43, 55), "harbor", 8014),
    MapSpec(15, (1672, 941), build_lunar, (116, 112, 105), (212, 209, 194), (14, 17, 25), "moon", 8015),
    MapSpec(16, (1254, 1254), build_crystal, (96, 67, 139), (174, 232, 255), (22, 13, 36), "crystal", 8016),
    MapSpec(17, (1536, 1024), build_ruins, (103, 117, 88), (202, 183, 128), (13, 35, 24), "ruins", 8017),
    MapSpec(18, (1672, 941), build_factory, (104, 93, 78), (238, 177, 62), (24, 21, 22), "factory", 8018),
    MapSpec(19, (1254, 1254), build_sky_loop, (126, 153, 170), (233, 238, 218), (23, 53, 78), "sky", 8019),
    MapSpec(20, (1536, 1024), build_courtyard, (139, 128, 99), (214, 191, 118), (31, 38, 40), "courtyard", 8020),
]


def color_shift(color: Tuple[int, int, int], delta: int) -> Tuple[int, int, int]:
    return tuple(max(0, min(255, channel + delta)) for channel in color)


def textured(size: Tuple[int, int], base: Tuple[int, int, int], seed: int, strength: int = 34) -> Image.Image:
    random.seed(seed)
    noise = Image.effect_noise(size, random.randint(24, 56)).convert("L")
    low = Image.new("RGB", size, color_shift(base, -strength))
    high = Image.new("RGB", size, color_shift(base, strength))
    return Image.composite(high, low, noise)


def add_overlay(image: Image.Image, overlay: Image.Image, mask: Image.Image) -> None:
    red, green, blue, alpha = overlay.split()
    overlay.putalpha(ImageChops.multiply(alpha, mask))
    image.alpha_composite(overlay)


def draw_motif(image: Image.Image, mask: Image.Image, spec: MapSpec) -> None:
    w, h = spec.size
    rng = random.Random(spec.seed + 4000)
    overlay = Image.new("RGBA", spec.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    soft = (*color_shift(spec.accent, 10), 76)
    dim = (*color_shift(spec.floor, -48), 84)

    if spec.motif in {"neon", "circuit"}:
        step = max(60, min(w, h) // 9)
        for x in range(-step, w + step, step):
            draw.line((x, 0, x + h // 3, h), fill=soft, width=3)
        for y in range(0, h, step):
            draw.line((0, y, w, y), fill=dim, width=2)
    elif spec.motif in {"harbor", "courtyard", "ruins"}:
        step = max(54, min(w, h) // 11)
        for x in range(0, w, step):
            draw.line((x, 0, x, h), fill=dim, width=3)
        for y in range(0, h, step):
            draw.line((0, y, w, y), fill=dim, width=3)
    elif spec.motif == "factory":
        for y in range(-h, h * 2, 78):
            draw.line((0, y, w, y + w // 4), fill=soft, width=5)
        for x in range(90, w, 180):
            draw.ellipse((x - 42, h * 0.5 - 42, x + 42, h * 0.5 + 42), outline=dim, width=4)
    elif spec.motif == "volcano":
        for _ in range(34):
            x, y = rng.randrange(w), rng.randrange(h)
            points = [(x, y)]
            for _ in range(4):
                x += rng.randint(-55, 55)
                y += rng.randint(-38, 38)
                points.append((x, y))
            draw.line(points, fill=soft, width=rng.randint(2, 5))
    elif spec.motif == "ice":
        for _ in range(44):
            x, y = rng.randrange(w), rng.randrange(h)
            points = [(x, y)]
            for _ in range(3):
                x += rng.randint(-65, 65)
                y += rng.randint(-65, 65)
                points.append((x, y))
            draw.line(points, fill=(245, 255, 255, 92), width=2)
    elif spec.motif == "desert":
        for y in range(-80, h + 120, 84):
            points = [(0, y)]
            for x in range(0, w + 120, 160):
                points.append((x, y + int(math.sin(x * 0.010 + spec.seed) * 26)))
            draw.line(points, fill=dim, width=5)
    elif spec.motif == "crystal":
        for _ in range(42):
            x, y = rng.randrange(w), rng.randrange(h)
            radius = rng.randint(18, 62)
            poly = star(x, y, radius, radius * 0.35, 3, rng.random() * math.tau)
            draw.polygon(poly, outline=soft, fill=(*spec.accent, 20))
    elif spec.motif == "moon":
        for _ in range(45):
            x, y = rng.randrange(w), rng.randrange(h)
            radius = rng.randint(18, 74)
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), outline=dim, width=3)
    else:
        for _ in range(150):
            x, y = rng.randrange(w), rng.randrange(h)
            radius = rng.randint(2, 8)
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=soft)

    add_overlay(image, overlay, mask)


def generate_art(mask: Image.Image, spec: MapSpec) -> Image.Image:
    background = textured(spec.size, spec.void, spec.seed, 18)
    floor = textured(spec.size, spec.floor, spec.seed + 20, 30)
    image = Image.composite(floor, background, mask).convert("RGBA")
    draw_motif(image, mask, spec)

    edge = mask.filter(ImageFilter.FIND_EDGES).filter(ImageFilter.MaxFilter(7))
    edge_overlay = Image.new("RGBA", spec.size, (*spec.accent, 190))
    edge_overlay.putalpha(edge)
    image.alpha_composite(edge_overlay)

    inner_edge = mask.filter(ImageFilter.FIND_EDGES)
    shadow = Image.new("RGBA", spec.size, (0, 0, 0, 95))
    shadow.putalpha(inner_edge.filter(ImageFilter.MaxFilter(11)))
    image.alpha_composite(shadow)
    return image.convert("RGB")


def point_safe(playable: Image.Image, x: float, y: float, margin: int) -> bool:
    w, h = playable.size
    if x < margin or y < margin or x >= w - margin or y >= h - margin:
        return False
    for i in range(16):
        angle = i * math.tau / 16
        px = int(round(x + math.cos(angle) * margin))
        py = int(round(y + math.sin(angle) * margin))
        if playable.getpixel((px, py)) < 240:
            return False
    return playable.getpixel((int(round(x)), int(round(y)))) >= 240


def select_spawns(playable: Image.Image, count: int, seed: int) -> List[Point]:
    w, h = playable.size
    rng = random.Random(seed + 9000)
    margin = max(24, min(w, h) // 32)
    step = max(24, min(w, h) // 28)
    candidates: List[Point] = []
    for y in range(margin, h - margin, step):
        for x in range(margin, w - margin, step):
            if point_safe(playable, x, y, margin):
                candidates.append((x + rng.uniform(-4, 4), y + rng.uniform(-4, 4)))
    if len(candidates) < count:
        raise RuntimeError("not enough safe spawn candidates")

    cx, cy = w * 0.5, h * 0.5
    chosen: List[Point] = []
    min_distance = min(w, h) * 0.095
    for i in range(count):
        target = -math.pi / 2 + i * math.tau / count
        best = None
        best_score = -999999.0
        for x, y in candidates:
            if any((x - sx) ** 2 + (y - sy) ** 2 < min_distance ** 2 for sx, sy in chosen):
                continue
            angle = math.atan2(y - cy, x - cx)
            diff = abs(math.atan2(math.sin(angle - target), math.cos(angle - target)))
            radius = math.hypot(x - cx, y - cy)
            score = -diff * min(w, h) * 0.65 + radius + rng.random() * 10
            if score > best_score:
                best = (x, y)
                best_score = score
        if best is None:
            min_distance *= 0.86
            continue
        chosen.append(best)

    while len(chosen) < count:
        best = max(
            candidates,
            key=lambda point: min((point[0] - sx) ** 2 + (point[1] - sy) ** 2 for sx, sy in chosen),
        )
        chosen.append(best)
    return chosen[:count]


def draw_markers(mask_rgb: Image.Image, playable: Image.Image, spawns: Iterable[Point]) -> None:
    draw = ImageDraw.Draw(mask_rgb)
    w, h = mask_rgb.size
    cx, cy = w * 0.5, h * 0.5
    red_radius = max(7, min(w, h) // 122)
    blue_radius = max(7, min(w, h) // 128)
    offset = max(18, min(w, h) // 46)
    for x, y in spawns:
        draw.ellipse((x - red_radius, y - red_radius, x + red_radius, y + red_radius), fill=RED)
        angle = math.atan2(cy - y, cx - x)
        tx = x + math.cos(angle) * offset
        ty = y + math.sin(angle) * offset
        if not point_safe(playable, tx, ty, max(blue_radius + 3, 12)):
            tx = x + math.cos(angle) * offset * 0.55
            ty = y + math.sin(angle) * offset * 0.55
        draw.ellipse((tx - blue_radius, ty - blue_radius, tx + blue_radius, ty + blue_radius), fill=BLUE)


def build_playable_mask(spec: MapSpec) -> Image.Image:
    playable = Image.new("L", spec.size, 0)
    draw = ImageDraw.Draw(playable)
    spec.builder(draw, spec.size[0], spec.size[1])
    validate_single_playable_component(playable, spec.index)
    return playable


def validate_single_playable_component(playable: Image.Image, map_index: int) -> None:
    w, h = playable.size
    pixels = playable.load()
    visited = bytearray(w * h)
    components: List[List[Point]] = []
    for y in range(h):
        for x in range(w):
            index = y * w + x
            if visited[index] or pixels[x, y] < 240:
                continue
            stack = [(x, y)]
            visited[index] = 1
            component: List[Point] = []
            while stack:
                sx, sy = stack.pop()
                component.append((sx, sy))
                for nx, ny in ((sx + 1, sy), (sx - 1, sy), (sx, sy + 1), (sx, sy - 1)):
                    if nx < 0 or nx >= w or ny < 0 or ny >= h:
                        continue
                    next_index = ny * w + nx
                    if visited[next_index] or pixels[nx, ny] < 240:
                        continue
                    visited[next_index] = 1
                    stack.append((nx, ny))
            components.append(component)

    if len(components) <= 1:
        return

    components.sort(key=len, reverse=True)
    largest = len(components[0])
    island_threshold = max(128, int(w * h * 0.001))
    for component in components[1:]:
        if len(component) > island_threshold:
            raise RuntimeError(
                f"map{map_index:03d} has disconnected playable islands; "
                f"largest_component={largest} island_component={len(component)}"
            )
        for x, y in component:
            pixels[int(x), int(y)] = 0


def write_map(spec: MapSpec, output_dir: Path, spawn_count: int) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    playable = build_playable_mask(spec)
    art = generate_art(playable, spec)
    mask_rgb = ImageOps.colorize(playable, BLACK, WHITE)
    spawns = select_spawns(playable, spawn_count, spec.seed)
    draw_markers(mask_rgb, playable, spawns)

    base_name = f"map{spec.index:03d}"
    art.save(output_dir / f"{base_name}.png")
    mask_rgb.save(output_dir / f"{base_name}_mask.png")
    print(f"generated={base_name} size={spec.size[0]}x{spec.size[1]} spawns={len(spawns)} motif={spec.motif}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="assets/maps")
    parser.add_argument("--start", type=int, default=8)
    parser.add_argument("--end", type=int, default=20)
    parser.add_argument("--spawns", type=int, default=32)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    for spec in SPECS:
        if args.start <= spec.index <= args.end:
            write_map(spec, output_dir, args.spawns)


if __name__ == "__main__":
    main()

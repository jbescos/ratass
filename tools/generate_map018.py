#!/usr/bin/env python3
"""Generate the long-straight map018 circuit and its presentation image."""

from __future__ import annotations

import argparse
import math
import random
from pathlib import Path
from typing import List, Tuple

from PIL import Image, ImageChops, ImageDraw, ImageFilter

Point = Tuple[float, float]

WIDTH = 3600
HEIGHT = 1200
ROAD_WIDTH = 66
SUPERSAMPLE = 2
PATH_SAMPLE_SPACING = 4.0

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
RED = (225, 28, 34)
GREEN = (36, 210, 76)

START = (700.0, 1040.0)
START_LINE_X = 1000
RIGHT_HAIRPIN_X = 3200.0


def distance(a: Point, b: Point) -> float:
    return math.hypot(b[0] - a[0], b[1] - a[1])


class TrackPath:
    def __init__(self, start: Point) -> None:
        self.points: List[Point] = [start]

    @property
    def current(self) -> Point:
        return self.points[-1]

    def line_to(self, end: Point) -> None:
        start = self.current
        samples = max(1, math.ceil(distance(start, end) / PATH_SAMPLE_SPACING))
        for index in range(1, samples + 1):
            alpha = index / samples
            self.points.append(
                (
                    start[0] + (end[0] - start[0]) * alpha,
                    start[1] + (end[1] - start[1]) * alpha,
                )
            )

    def cubic_to(self, control_a: Point, control_b: Point, end: Point) -> None:
        start = self.current
        estimated_length = (
            distance(start, control_a)
            + distance(control_a, control_b)
            + distance(control_b, end)
        )
        samples = max(8, math.ceil(estimated_length / PATH_SAMPLE_SPACING))
        for index in range(1, samples + 1):
            t = index / samples
            inverse = 1.0 - t
            self.points.append(
                (
                    inverse ** 3 * start[0]
                    + 3.0 * inverse ** 2 * t * control_a[0]
                    + 3.0 * inverse * t ** 2 * control_b[0]
                    + t ** 3 * end[0],
                    inverse ** 3 * start[1]
                    + 3.0 * inverse ** 2 * t * control_a[1]
                    + 3.0 * inverse * t ** 2 * control_b[1]
                    + t ** 3 * end[1],
                )
            )

    def arc_to(
        self,
        center: Point,
        radius: float,
        start_angle: float,
        end_angle: float,
    ) -> None:
        expected_start = (
            center[0] + math.cos(start_angle) * radius,
            center[1] + math.sin(start_angle) * radius,
        )
        if distance(self.current, expected_start) > 0.1:
            raise ValueError(
                f"arc does not start at current point: {self.current} != {expected_start}"
            )
        angle_delta = end_angle - start_angle
        samples = max(12, math.ceil(abs(angle_delta) * radius / PATH_SAMPLE_SPACING))
        for index in range(1, samples + 1):
            angle = start_angle + angle_delta * index / samples
            self.points.append(
                (
                    center[0] + math.cos(angle) * radius,
                    center[1] + math.sin(angle) * radius,
                )
            )


def build_centerline() -> List[Point]:
    path = TrackPath(START)

    # Main straight into a tight left hairpin.
    path.line_to((RIGHT_HAIRPIN_X, 1040.0))
    path.arc_to((RIGHT_HAIRPIN_X, 930.0), 110.0, math.pi / 2.0, -math.pi / 2.0)

    # Westbound straight with a fast right-left chicane.
    path.line_to((2420.0, 820.0))
    path.cubic_to((2320.0, 820.0), (2240.0, 735.0), (2110.0, 735.0))
    path.line_to((1940.0, 735.0))
    path.cubic_to((1810.0, 735.0), (1730.0, 820.0), (1600.0, 820.0))
    path.line_to((500.0, 820.0))

    # Tight right hairpin leading onto the longest uninterrupted straight.
    path.arc_to((500.0, 710.0), 110.0, math.pi / 2.0, 3.0 * math.pi / 2.0)
    path.line_to((RIGHT_HAIRPIN_X, 600.0))

    # Second left hairpin and an opposite-direction chicane.
    path.arc_to((RIGHT_HAIRPIN_X, 490.0), 110.0, math.pi / 2.0, -math.pi / 2.0)
    path.line_to((2420.0, 380.0))
    path.cubic_to((2320.0, 380.0), (2240.0, 465.0), (2110.0, 465.0))
    path.line_to((1940.0, 465.0))
    path.cubic_to((1810.0, 465.0), (1730.0, 380.0), (1600.0, 380.0))
    path.line_to((400.0, 380.0))

    # Broad left-hand return corners close the loop without another hairpin.
    path.cubic_to((260.0, 380.0), (180.0, 460.0), (180.0, 600.0))
    path.line_to((180.0, 850.0))
    path.cubic_to((180.0, 970.0), (270.0, 1040.0), (400.0, 1040.0))
    path.line_to(START)
    return path.points


def render_playable(centerline: List[Point]) -> Tuple[Image.Image, Image.Image]:
    scaled_size = (WIDTH * SUPERSAMPLE, HEIGHT * SUPERSAMPLE)
    high_resolution = Image.new("L", scaled_size, 0)
    draw = ImageDraw.Draw(high_resolution)
    scaled_points = [
        (round(x * SUPERSAMPLE), round(y * SUPERSAMPLE)) for x, y in centerline
    ]
    draw.line(
        scaled_points,
        fill=255,
        width=ROAD_WIDTH * SUPERSAMPLE,
        joint="curve",
    )
    soft = high_resolution.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
    hard = soft.point(lambda value: 255 if value >= 128 else 0)
    return soft, hard


def draw_gameplay_markers(mask: Image.Image) -> Image.Image:
    output = Image.new("RGB", mask.size, BLACK)
    output.paste(WHITE, mask=mask)
    draw = ImageDraw.Draw(output)

    road_half_width = ROAD_WIDTH // 2
    draw.line(
        (
            (START_LINE_X, 1040 - road_half_width + 1),
            (START_LINE_X, 1040 + road_half_width - 1),
        ),
        fill=GREEN,
        width=6,
    )

    for y in (1022, 1058):
        draw.ellipse((922, y - 7, 936, y + 7), fill=RED)
    return output


def draw_background_details(image: Image.Image) -> None:
    draw = ImageDraw.Draw(image)
    rng = random.Random(180018)

    panel_width = 150
    panel_height = 120
    for y in range(0, HEIGHT, panel_height):
        for x in range(0, WIDTH, panel_width):
            shade = 22 + ((x // panel_width + y // panel_height) % 3) * 3
            draw.rectangle(
                (x, y, x + panel_width, y + panel_height),
                fill=(shade, shade + 5, shade + 4),
                outline=(38, 47, 44),
                width=2,
            )
            if (x // panel_width + y // panel_height) % 4 == 0:
                draw.line(
                    (x + 18, y + panel_height - 18, x + panel_width - 18, y + 18),
                    fill=(48, 56, 51),
                    width=3,
                )

    for _ in range(110):
        x = rng.randrange(30, WIDTH - 80)
        y = rng.randrange(20, HEIGHT - 50)
        width = rng.randrange(18, 70)
        color = rng.choice(((94, 75, 37), (31, 74, 70), (70, 39, 67)))
        draw.rectangle((x, y, x + width, y + 6), fill=color)

    # Runoff zones make the three hairpins visually distinct from the straights.
    for center_x, center_y, color in (
        (RIGHT_HAIRPIN_X, 930, (91, 72, 36)),
        (500, 710, (30, 72, 68)),
        (RIGHT_HAIRPIN_X, 490, (78, 38, 69)),
    ):
        draw.ellipse(
            (center_x - 165, center_y - 165, center_x + 165, center_y + 165),
            outline=color,
            width=28,
        )


def draw_braking_markers(image: Image.Image) -> None:
    draw = ImageDraw.Draw(image)
    marker_sets = (
        ((2760, 1092), 1),
        ((2760, 548), -1),
        ((760, 872), 1),
    )
    for (start_x, start_y), direction in marker_sets:
        for index, width in enumerate((34, 26, 18)):
            x = start_x + direction * index * 115
            draw.rectangle((x - width, start_y - 5, x + width, start_y + 5), fill=(232, 226, 201))
            draw.rectangle((x - width, start_y - 5, x - width + 7, start_y + 5), fill=(223, 66, 53))


def render_art(soft_mask: Image.Image, hard_mask: Image.Image) -> Image.Image:
    image = Image.new("RGB", (WIDTH, HEIGHT), (19, 25, 24))
    draw_background_details(image)

    glow = soft_mask.filter(ImageFilter.GaussianBlur(14))
    image.paste((18, 108, 104), mask=glow)

    expanded = hard_mask.filter(ImageFilter.MaxFilter(19))
    outer_ring = ImageChops.subtract(expanded, hard_mask)
    image.paste((181, 128, 39), mask=outer_ring)

    road = Image.new("RGB", (WIDTH, HEIGHT), (35, 39, 43))
    road_draw = ImageDraw.Draw(road)
    for x in range(-HEIGHT, WIDTH + HEIGHT, 84):
        road_draw.line((x, 0, x + HEIGHT, HEIGHT), fill=(40, 44, 48), width=3)
    for y in range(45, HEIGHT, 90):
        road_draw.line((0, y, WIDTH, y), fill=(31, 35, 39), width=2)
    image.paste(road, mask=soft_mask)

    inner_edge = ImageChops.subtract(soft_mask, soft_mask.filter(ImageFilter.MinFilter(7)))
    image.paste((79, 226, 214), mask=inner_edge)

    draw_braking_markers(image)
    draw = ImageDraw.Draw(image)

    # Checkered start line and grid boxes are presentation-only.
    top = 1040 - ROAD_WIDTH // 2 + 3
    cell_height = max(5, (ROAD_WIDTH - 6) // 8)
    for row in range(8):
        color = (239, 242, 233) if row % 2 == 0 else (27, 34, 37)
        draw.rectangle(
            (
                START_LINE_X - 5,
                top + row * cell_height,
                START_LINE_X + 5,
                min(1040 + ROAD_WIDTH // 2 - 3, top + (row + 1) * cell_height),
            ),
            fill=color,
        )
    for row, y in enumerate((1022, 1058)):
        for column in range(5):
            x = 900 - column * 88 - row * 30
            draw.rectangle((x - 16, y - 3, x + 16, y + 3), fill=(196, 201, 193))

    return image


def approximate_length(points: List[Point]) -> float:
    return sum(distance(points[index - 1], points[index]) for index in range(1, len(points)))


def validate_geometry(hard_mask: Image.Image, centerline: List[Point]) -> None:
    if distance(centerline[0], centerline[-1]) > 0.01:
        raise RuntimeError("map018 centerline is not closed")
    if hard_mask.getpixel((1800, 710)) != 0:
        raise RuntimeError("parallel straights merged near the center of map018")
    if hard_mask.getpixel((3000, 710)) != 0:
        raise RuntimeError("right-side parallel straights merged in map018")
    if hard_mask.getbbox() is None:
        raise RuntimeError("map018 road mask is empty")


def write_map(output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    centerline = build_centerline()
    soft_mask, hard_mask = render_playable(centerline)
    validate_geometry(hard_mask, centerline)

    gameplay_mask = draw_gameplay_markers(hard_mask)
    art = render_art(soft_mask, hard_mask)

    mask_path = output_dir / "map018_mask.png"
    art_path = output_dir / "map018.png"
    gameplay_mask.save(mask_path, optimize=True, compress_level=9)
    art.save(art_path, optimize=True, compress_level=9)

    world_units_per_pixel = 176.0 / HEIGHT
    route_length = approximate_length(centerline) * world_units_per_pixel
    longest_straight = (RIGHT_HAIRPIN_X - 500.0) * world_units_per_pixel
    print(
        "generated=map018"
        f" size={WIDTH}x{HEIGHT}"
        f" road_width_world={ROAD_WIDTH * world_units_per_pixel:.2f}"
        f" approximate_route_world={route_length:.1f}"
        f" longest_straight_world={longest_straight:.1f}"
        f" mask_bytes={mask_path.stat().st_size}"
        f" art_bytes={art_path.stat().st_size}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="assets/maps")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    write_map(Path(args.output_dir))


if __name__ == "__main__":
    main()

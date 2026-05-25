#!/usr/bin/env python3
"""Generate mask-only race tracks based on recognizable Formula 1 circuit layouts."""

from __future__ import annotations

import argparse
import math
from dataclasses import dataclass
from pathlib import Path
from typing import List, Sequence, Tuple

from PIL import Image, ImageChops, ImageDraw

Point = Tuple[float, float]

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
RED = (225, 28, 34)
BLUE = (28, 86, 230)
GREEN = (36, 210, 76)
START_GRID_ROWS = 10


@dataclass(frozen=True)
class CircuitSpec:
    file_index: int
    name: str
    points: Sequence[Point]


CIRCUITS: Sequence[CircuitSpec] = (
    CircuitSpec(
        0,
        "monza",
        (
            (0.56, 0.88),
            (0.67, 0.76),
            (0.78, 0.62),
            (0.94, 0.55),
            (0.86, 0.39),
            (0.64, 0.36),
            (0.50, 0.24),
            (0.34, 0.32),
            (0.30, 0.51),
            (0.13, 0.63),
            (0.24, 0.80),
            (0.40, 0.86),
        ),
    ),
    CircuitSpec(
        1,
        "bahrain",
        (
            (0.22, 0.82),
            (0.46, 0.86),
            (0.72, 0.79),
            (0.91, 0.62),
            (0.85, 0.42),
            (0.64, 0.33),
            (0.54, 0.18),
            (0.34, 0.24),
            (0.24, 0.43),
            (0.10, 0.48),
            (0.14, 0.66),
        ),
    ),
    CircuitSpec(
        2,
        "silverstone",
        (
            (0.28, 0.82),
            (0.49, 0.75),
            (0.71, 0.86),
            (0.92, 0.67),
            (0.78, 0.50),
            (0.94, 0.34),
            (0.72, 0.16),
            (0.49, 0.25),
            (0.32, 0.13),
            (0.11, 0.31),
            (0.18, 0.52),
            (0.07, 0.70),
        ),
    ),
    CircuitSpec(
        3,
        "spa",
        (
            (0.17, 0.76),
            (0.36, 0.87),
            (0.58, 0.82),
            (0.80, 0.70),
            (0.93, 0.52),
            (0.84, 0.31),
            (0.63, 0.17),
            (0.42, 0.18),
            (0.22, 0.29),
            (0.10, 0.48),
            (0.19, 0.63),
        ),
    ),
    CircuitSpec(
        4,
        "figure_eight",
        (
            (0.53, 0.86),
            (0.76, 0.82),
            (0.93, 0.63),
            (0.82, 0.42),
            (0.58, 0.50),
            (0.37, 0.58),
            (0.10, 0.42),
            (0.23, 0.18),
            (0.49, 0.28),
            (0.64, 0.50),
            (0.86, 0.72),
            (0.70, 0.88),
        ),
    ),
)


def catmull_rom(points: Sequence[Point], samples_per_segment: int) -> List[Point]:
    out: List[Point] = []
    count = len(points)
    for index in range(count):
        p0 = points[(index - 1) % count]
        p1 = points[index]
        p2 = points[(index + 1) % count]
        p3 = points[(index + 2) % count]
        for sample in range(samples_per_segment):
            t = sample / float(samples_per_segment)
            t2 = t * t
            t3 = t2 * t
            x = 0.5 * (
                (2 * p1[0])
                + (-p0[0] + p2[0]) * t
                + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3
            )
            y = 0.5 * (
                (2 * p1[1])
                + (-p0[1] + p2[1]) * t
                + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3
            )
            out.append((x, y))
    return out


def scale_points(points: Sequence[Point], width: int, height: int) -> List[Point]:
    return [(x * width, y * height) for x, y in points]


def path_lengths(points: Sequence[Point]) -> Tuple[List[float], float]:
    cumulative = [0.0]
    total = 0.0
    for index in range(1, len(points) + 1):
        a = points[index - 1]
        b = points[index % len(points)]
        total += math.hypot(b[0] - a[0], b[1] - a[1])
        cumulative.append(total)
    return cumulative, total


def sample_path(points: Sequence[Point], distance: float) -> Tuple[Point, Point]:
    cumulative, total = path_lengths(points)
    distance = distance % total
    for index in range(1, len(cumulative)):
        if cumulative[index] < distance:
            continue
        start = points[index - 1]
        end = points[index % len(points)]
        segment = cumulative[index] - cumulative[index - 1]
        alpha = 0.0 if segment <= 0.0001 else (distance - cumulative[index - 1]) / segment
        x = start[0] + (end[0] - start[0]) * alpha
        y = start[1] + (end[1] - start[1]) * alpha
        tangent_x = end[0] - start[0]
        tangent_y = end[1] - start[1]
        length = math.hypot(tangent_x, tangent_y)
        if length <= 0.0001:
            return (x, y), (1.0, 0.0)
        return (x, y), (tangent_x / length, tangent_y / length)
    return points[0], (1.0, 0.0)


def draw_marker(draw: ImageDraw.ImageDraw, point: Point, radius: int, color: Tuple[int, int, int]) -> None:
    x, y = point
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def draw_road(
        draw: ImageDraw.ImageDraw,
        points: Sequence[Point],
        track_width: int,
        fill=WHITE) -> None:
    draw.line(list(points) + [points[0]], fill=fill, width=track_width, joint="curve")
    radius = track_width * 0.5
    for x, y in points:
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=fill)


def spawn_grid_layout(track_width: int) -> Tuple[int, int, int, int, int]:
    row_spacing = max(64, int(track_width * 0.30))
    column_spacing = max(68, int(track_width * 0.34))
    first_back_offset = max(76, int(track_width * 0.36))
    spawn_radius = max(8, track_width // 28)
    direction_radius = max(5, track_width // 52)
    return row_spacing, column_spacing, first_back_offset, spawn_radius, direction_radius


def draw_checkpoint_gate(
        image: Image.Image,
        road_mask: Image.Image,
        point: Point,
        tangent: Point,
        track_width: int) -> None:
    normal = (-tangent[1], tangent[0])
    line_width = max(5, track_width // 52)

    def find_edge(sign: float) -> float:
        pixels = road_mask.load()
        width, height = road_mask.size
        last = 0.0
        for step in range(int(track_width * 0.40) + 1):
            distance = sign * step
            x = int(round(point[0] + normal[0] * distance))
            y = int(round(point[1] + normal[1] * distance))
            if 0 <= x < width and 0 <= y < height and pixels[x, y] > 0:
                last = distance
            elif step > line_width:
                break
        return last

    start_distance = find_edge(-1.0)
    end_distance = find_edge(1.0)
    if abs(start_distance) + abs(end_distance) < track_width * 0.35:
        start_distance = -track_width * 0.50
        end_distance = track_width * 0.50

    start = (
        point[0] + normal[0] * start_distance,
        point[1] + normal[1] * start_distance,
    )
    end = (
        point[0] + normal[0] * end_distance,
        point[1] + normal[1] * end_distance,
    )
    line_mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(line_mask).line((start, end), fill=255, width=line_width)
    image.paste(GREEN, mask=ImageChops.multiply(line_mask, road_mask))


def draw_spawn_grid(
        draw: ImageDraw.ImageDraw,
        points: Sequence[Point],
        total_length: float,
        track_width: int,
        direction_offset: int,
        rows: int = START_GRID_ROWS) -> None:
    row_spacing, column_spacing, first_back_offset, spawn_radius, direction_radius = (
        spawn_grid_layout(track_width)
    )

    for row in range(rows):
        center, tangent = sample_path(points, total_length - first_back_offset - row * row_spacing)
        normal = (-tangent[1], tangent[0])
        for column in range(2):
            side = (-0.5 if column == 0 else 0.5) * column_spacing
            point = (
                center[0] + normal[0] * side,
                center[1] + normal[1] * side,
            )
            draw_marker(draw, point, spawn_radius, RED)

            blue_point = (
                point[0] + tangent[0] * direction_offset,
                point[1] + tangent[1] * direction_offset,
            )
            draw_marker(draw, blue_point, direction_radius, BLUE)


def render_circuit(
        spec: CircuitSpec,
        output_dir: Path,
        size: Tuple[int, int],
        checkpoints: int,
        track_width: int,
        direction_offset: int) -> None:
    width, height = size
    image = Image.new("RGB", size, BLACK)
    road_mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(image)
    road_draw = ImageDraw.Draw(road_mask)
    smoothed = scale_points(catmull_rom(spec.points, 42), width, height)
    draw_road(draw, smoothed, track_width)
    draw_road(road_draw, smoothed, track_width, 255)

    _, total = path_lengths(smoothed)
    row_spacing, _, first_back_offset, spawn_radius, _ = spawn_grid_layout(track_width)
    reserved_back = first_back_offset + row_spacing * (START_GRID_ROWS - 1) + spawn_radius + track_width * 0.28
    reserved_forward = max(track_width * 0.8, 210)
    available_start = min(reserved_forward, total * 0.18)
    available_end = max(available_start + 1.0, total - reserved_back)
    checkpoint_distances = [0.0]
    if checkpoints > 1:
        for index in range(checkpoints - 1):
            alpha = 0.0 if checkpoints == 2 else index / float(checkpoints - 2)
            checkpoint_distances.append(available_start + (available_end - available_start) * alpha)

    for distance in checkpoint_distances:
        point, tangent = sample_path(smoothed, distance)
        draw_checkpoint_gate(image, road_mask, point, tangent, track_width)
    draw_spawn_grid(draw, smoothed, total, track_width, direction_offset)

    output_dir.mkdir(parents=True, exist_ok=True)
    output = output_dir / f"map{spec.file_index:03d}_mask.png"
    image.save(output)
    print(
        f"generated={output} circuit={spec.name} "
        f"size={width}x{height} track_width={track_width} checkpoints={checkpoints}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="assets/maps")
    parser.add_argument("--width", type=int, default=2400)
    parser.add_argument("--height", type=int, default=1500)
    parser.add_argument("--checkpoints", type=int, default=20)
    parser.add_argument("--track-width", type=int, default=260)
    parser.add_argument("--direction-offset", type=int, default=32)
    parser.add_argument(
        "--all",
        action="store_true",
        help="generate every built-in circuit instead of only masks that already exist",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    for path in output_dir.glob("map*.ser"):
        path.unlink()
    for spec in CIRCUITS:
        output = output_dir / f"map{spec.file_index:03d}_mask.png"
        if not args.all and not output.exists():
            continue
        render_circuit(
            spec,
            output_dir,
            (args.width, args.height),
            args.checkpoints,
            args.track_width,
            args.direction_offset,
        )


if __name__ == "__main__":
    main()

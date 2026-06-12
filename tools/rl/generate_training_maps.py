#!/usr/bin/env python3
"""Generate synthetic race masks used only for RL training."""

from __future__ import annotations

import argparse
import math
from pathlib import Path
from typing import List, NamedTuple, Sequence, Tuple

from PIL import Image, ImageChops, ImageDraw, ImageFont

Point = Tuple[float, float]

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
RED = (225, 28, 34)
BLUE = (28, 86, 230)
GREEN = (36, 210, 76)
LABEL_GRAY = (150, 150, 150)
START_GRID_ROWS = 10


class TrackTemplate(NamedTuple):
    name: str
    points: Sequence[Point]
    track_width: int


TRACK_TEMPLATES: Sequence[TrackTemplate] = (
    TrackTemplate(
        "fast_oval_straight",
        (
            (0.10, 0.74),
            (0.82, 0.74),
            (0.93, 0.60),
            (0.88, 0.39),
            (0.70, 0.25),
            (0.18, 0.25),
            (0.08, 0.42),
        ),
        156,
    ),
    TrackTemplate(
        "backstraight_hairpin",
        (
            (0.11, 0.80),
            (0.86, 0.80),
            (0.93, 0.66),
            (0.84, 0.52),
            (0.58, 0.44),
            (0.28, 0.35),
            (0.13, 0.22),
            (0.30, 0.12),
            (0.70, 0.20),
            (0.84, 0.36),
        ),
        142,
    ),
    TrackTemplate(
        "technical_sweepers",
        (
            (0.15, 0.78),
            (0.35, 0.86),
            (0.56, 0.74),
            (0.77, 0.84),
            (0.91, 0.66),
            (0.72, 0.54),
            (0.84, 0.34),
            (0.60, 0.21),
            (0.39, 0.33),
            (0.17, 0.23),
            (0.08, 0.46),
        ),
        132,
    ),
    TrackTemplate(
        "wide_triangle",
        (
            (0.13, 0.77),
            (0.48, 0.86),
            (0.87, 0.76),
            (0.93, 0.55),
            (0.70, 0.31),
            (0.45, 0.14),
            (0.17, 0.29),
            (0.08, 0.51),
        ),
        150,
    ),
    TrackTemplate(
        "long_box_chicanes",
        (
            (0.10, 0.82),
            (0.88, 0.82),
            (0.91, 0.65),
            (0.74, 0.58),
            (0.88, 0.45),
            (0.76, 0.30),
            (0.48, 0.25),
            (0.20, 0.18),
            (0.09, 0.36),
            (0.18, 0.54),
        ),
        136,
    ),
    TrackTemplate(
        "fast_sweeping_bowl",
        (
            (0.18, 0.72),
            (0.43, 0.86),
            (0.73, 0.79),
            (0.91, 0.58),
            (0.84, 0.33),
            (0.58, 0.17),
            (0.31, 0.24),
            (0.10, 0.44),
        ),
        166,
    ),
    TrackTemplate(
        "stadium_hairpins",
        (
            (0.13, 0.82),
            (0.61, 0.82),
            (0.86, 0.72),
            (0.91, 0.52),
            (0.73, 0.39),
            (0.86, 0.22),
            (0.55, 0.15),
            (0.27, 0.24),
            (0.10, 0.43),
            (0.23, 0.61),
        ),
        130,
    ),
    TrackTemplate(
        "open_kidney",
        (
            (0.18, 0.78),
            (0.44, 0.84),
            (0.80, 0.72),
            (0.92, 0.51),
            (0.75, 0.30),
            (0.46, 0.22),
            (0.23, 0.31),
            (0.09, 0.53),
        ),
        146,
    ),
    TrackTemplate(
        "huge_front_straight",
        (
            (0.08, 0.76),
            (0.90, 0.76),
            (0.94, 0.58),
            (0.80, 0.43),
            (0.55, 0.36),
            (0.36, 0.20),
            (0.15, 0.29),
            (0.10, 0.52),
        ),
        158,
    ),
    TrackTemplate(
        "short_twisty_safe",
        (
            (0.16, 0.75),
            (0.34, 0.84),
            (0.55, 0.76),
            (0.75, 0.82),
            (0.88, 0.64),
            (0.70, 0.50),
            (0.83, 0.30),
            (0.57, 0.19),
            (0.33, 0.27),
            (0.14, 0.41),
        ),
        128,
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


def draw_road(draw: ImageDraw.ImageDraw, points: Sequence[Point], track_width: int, fill) -> None:
    draw.line(list(points) + [points[0]], fill=fill, width=track_width, joint="curve")
    radius = track_width * 0.5
    for x, y in points:
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=fill)


def draw_marker(draw: ImageDraw.ImageDraw, point: Point, radius: int, color: Tuple[int, int, int]) -> None:
    x, y = point
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def draw_checkpoint_gate(
        image: Image.Image,
        road_mask: Image.Image,
        point: Point,
        tangent: Point,
        track_width: int) -> None:
    normal = (-tangent[1], tangent[0])
    line_width = max(5, track_width // 42)
    pixels = road_mask.load()
    width, height = road_mask.size

    def find_edge(sign: float) -> float:
        last = 0.0
        for step in range(int(track_width * 0.75) + 1):
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
    start = (point[0] + normal[0] * start_distance, point[1] + normal[1] * start_distance)
    end = (point[0] + normal[0] * end_distance, point[1] + normal[1] * end_distance)
    line_mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(line_mask).line((start, end), fill=255, width=line_width)
    image.paste(GREEN, mask=ImageChops.multiply(line_mask, road_mask))


def load_label_font(size: int) -> ImageFont.ImageFont:
    for path in (
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"):
        try:
            return ImageFont.truetype(path, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_checkpoint_label(
        draw: ImageDraw.ImageDraw,
        image_size: Tuple[int, int],
        label: int,
        point: Point,
        tangent: Point,
        track_width: int,
        font: ImageFont.ImageFont) -> None:
    normal = (-tangent[1], tangent[0])
    text = str(label)
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x = point[0] + tangent[0] * track_width * 0.34 + normal[0] * track_width * 0.18
    y = point[1] + tangent[1] * track_width * 0.34 + normal[1] * track_width * 0.18
    x = max(2, min(image_size[0] - text_width - 2, int(round(x - text_width * 0.5))))
    y = max(2, min(image_size[1] - text_height - 2, int(round(y - text_height * 0.5))))
    draw.text((x, y), text, fill=LABEL_GRAY, font=font)


def spawn_grid_layout(track_width: int) -> Tuple[int, int, int, int, int]:
    row_spacing = max(44, int(track_width * 0.34))
    column_spacing = max(46, int(track_width * 0.38))
    first_back_offset = max(58, int(track_width * 0.45))
    spawn_radius = max(6, track_width // 18)
    direction_radius = max(4, track_width // 32)
    return row_spacing, column_spacing, first_back_offset, spawn_radius, direction_radius


def draw_spawn_grid(
        draw: ImageDraw.ImageDraw,
        points: Sequence[Point],
        total_length: float,
        track_width: int,
        direction_offset: int) -> None:
    row_spacing, column_spacing, first_back_offset, spawn_radius, direction_radius = (
        spawn_grid_layout(track_width)
    )
    for row in range(START_GRID_ROWS):
        center, tangent = sample_path(points, total_length - first_back_offset - row * row_spacing)
        normal = (-tangent[1], tangent[0])
        for column in range(2):
            side = (-0.5 if column == 0 else 0.5) * column_spacing
            point = (center[0] + normal[0] * side, center[1] + normal[1] * side)
            draw_marker(draw, point, spawn_radius, RED)
            blue_point = (point[0] + tangent[0] * direction_offset, point[1] + tangent[1] * direction_offset)
            draw_marker(draw, blue_point, direction_radius, BLUE)


def clamp_point(point: Point, margin: float = 0.08) -> Point:
    return (min(1.0 - margin, max(margin, point[0])), min(1.0 - margin, max(margin, point[1])))


def scale_points(points: Sequence[Point], width: int, height: int) -> List[Point]:
    return [(x * width, y * height) for x, y in points]


def render_map(
        index: int,
        output_dir: Path,
        width: int,
        height: int,
        checkpoints: int,
        samples_per_segment: int) -> None:
    template = TRACK_TEMPLATES[index % len(TRACK_TEMPLATES)]
    track_width = template.track_width
    direction_offset = max(24, track_width // 4)
    smoothed = scale_points(catmull_rom(template.points, samples_per_segment), width, height)

    image = Image.new("RGB", (width, height), BLACK)
    road_mask = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(image)
    road_draw = ImageDraw.Draw(road_mask)
    draw_road(draw, smoothed, track_width, WHITE)
    draw_road(road_draw, smoothed, track_width, 255)

    _, total = path_lengths(smoothed)
    row_spacing, _, first_back_offset, spawn_radius, _ = spawn_grid_layout(track_width)
    reserved_back = first_back_offset + row_spacing * (START_GRID_ROWS - 1) + spawn_radius + track_width * 0.30
    available_start = min(max(track_width * 0.85, 150), total * 0.18)
    available_end = max(available_start + 1.0, total - reserved_back)
    checkpoint_distances = [0.0]
    for checkpoint_index in range(max(0, checkpoints - 1)):
        alpha = 0.0 if checkpoints <= 2 else checkpoint_index / float(checkpoints - 2)
        checkpoint_distances.append(available_start + (available_end - available_start) * alpha)
    checkpoint_samples = []
    for distance in checkpoint_distances:
        point, tangent = sample_path(smoothed, distance)
        draw_checkpoint_gate(image, road_mask, point, tangent, track_width)
        checkpoint_samples.append((point, tangent))
    draw_spawn_grid(draw, smoothed, total, track_width, direction_offset)
    label_font = load_label_font(max(24, track_width // 4))
    for checkpoint_index, (point, tangent) in enumerate(checkpoint_samples, start=1):
        draw_checkpoint_label(
            draw,
            (width, height),
            checkpoint_index,
            point,
            tangent,
            track_width,
            label_font,
        )

    output = output_dir / f"train{index:03d}_mask.png"
    image.save(output, optimize=True)
    print(
        f"generated={output} template={template.name} "
        f"track_width={track_width} checkpoints={checkpoints}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="tools/rl/trainingMaps")
    parser.add_argument("--count", type=int, default=10)
    parser.add_argument("--width", type=int, default=1400)
    parser.add_argument("--height", type=int, default=1000)
    parser.add_argument("--checkpoints", type=int, default=8)
    parser.add_argument("--samples-per-segment", type=int, default=42)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    for pattern in ("train*.json.gz", "train*.ser"):
        for path in output_dir.glob(pattern):
            path.unlink()
    for path in output_dir.glob("train*_mask.png"):
        path.unlink()
    for index in range(args.count):
        render_map(
            index,
            output_dir,
            args.width,
            args.height,
            args.checkpoints,
            args.samples_per_segment,
        )


if __name__ == "__main__":
    main()

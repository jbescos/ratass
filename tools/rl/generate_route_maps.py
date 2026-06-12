#!/usr/bin/env python3
"""Generate route-only masks for the RL lap-easy stage."""

from __future__ import annotations

import argparse
import math
from pathlib import Path
from typing import Iterable, List, NamedTuple, Sequence, Tuple

from PIL import Image, ImageDraw, ImageFont


Point = Tuple[float, float]

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
RED = (225, 28, 34)
BLUE = (28, 86, 230)
GREEN = (35, 210, 85)
LABEL_GRAY = (88, 88, 88)


class RouteTemplate(NamedTuple):
    name: str
    route: Sequence[Point]
    road_width: int
    marker_count: int
    branches: Sequence[Sequence[Point]] = ()


ROUTE_TEMPLATES: Sequence[RouteTemplate] = (
    RouteTemplate(
        "narrow_oval",
        (
            (0.22, 0.76),
            (0.80, 0.76),
            (0.91, 0.63),
            (0.90, 0.38),
            (0.79, 0.24),
            (0.22, 0.24),
            (0.09, 0.38),
            (0.09, 0.62),
        ),
        38,
        32,
    ),
    RouteTemplate(
        "small_crested_loop",
        (
            (0.21, 0.76),
            (0.43, 0.76),
            (0.56, 0.55),
            (0.69, 0.76),
            (0.85, 0.76),
            (0.92, 0.58),
            (0.84, 0.36),
            (0.60, 0.26),
            (0.30, 0.28),
            (0.11, 0.44),
            (0.11, 0.64),
        ),
        36,
        36,
    ),
    RouteTemplate(
        "long_straight_hairpin",
        (
            (0.21, 0.75),
            (0.86, 0.75),
            (0.93, 0.61),
            (0.89, 0.43),
            (0.72, 0.31),
            (0.30, 0.31),
            (0.13, 0.43),
            (0.13, 0.63),
        ),
        36,
        34,
    ),
    RouteTemplate(
        "angled_technical_loop",
        (
            (0.19, 0.76),
            (0.42, 0.86),
            (0.78, 0.75),
            (0.90, 0.52),
            (0.79, 0.25),
            (0.50, 0.17),
            (0.20, 0.31),
            (0.10, 0.58),
        ),
        36,
        18,
    ),
    RouteTemplate(
        "figure_eight_crossing",
        (
            (0.25, 0.70),
            (0.39, 0.59),
            (0.50, 0.50),
            (0.61, 0.39),
            (0.75, 0.30),
            (0.87, 0.50),
            (0.75, 0.70),
            (0.61, 0.61),
            (0.50, 0.50),
            (0.39, 0.39),
            (0.25, 0.30),
            (0.13, 0.50),
        ),
        34,
        22,
    ),
    RouteTemplate(
        "middle_fork_decoys",
        (
            (0.21, 0.72),
            (0.36, 0.72),
            (0.64, 0.72),
            (0.84, 0.72),
            (0.92, 0.54),
            (0.80, 0.33),
            (0.53, 0.25),
            (0.23, 0.34),
            (0.11, 0.54),
        ),
        34,
        18,
        (
            ((0.36, 0.72), (0.50, 0.47), (0.64, 0.72)),
            ((0.36, 0.72), (0.50, 0.90), (0.64, 0.72)),
        ),
    ),
    RouteTemplate(
        "close_parallel_roads",
        (
            (0.43, 0.69),
            (0.82, 0.69),
            (0.91, 0.60),
            (0.91, 0.43),
            (0.82, 0.34),
            (0.19, 0.34),
            (0.10, 0.43),
            (0.10, 0.60),
            (0.19, 0.69),
        ),
        32,
        34,
    ),
    RouteTemplate(
        "tight_s_before_fork",
        (
            (0.20, 0.76),
            (0.36, 0.74),
            (0.43, 0.56),
            (0.34, 0.42),
            (0.48, 0.28),
            (0.65, 0.41),
            (0.56, 0.60),
            (0.72, 0.74),
            (0.90, 0.61),
            (0.82, 0.38),
            (0.60, 0.22),
            (0.26, 0.24),
            (0.10, 0.47),
        ),
        32,
        44,
        (
            ((0.56, 0.60), (0.73, 0.50), (0.82, 0.38)),
            ((0.56, 0.60), (0.74, 0.87), (0.90, 0.61)),
        ),
    ),
)


def catmull_rom(points: Sequence[Point], samples_per_segment: int, closed: bool = True) -> List[Point]:
    out: List[Point] = []
    if len(points) < 2:
        return list(points)
    count = len(points)
    segment_count = count if closed else count - 1
    for index in range(segment_count):
        p0 = points[(index - 1) % count] if closed or index > 0 else points[index]
        p1 = points[index]
        p2 = points[(index + 1) % count]
        p3 = points[(index + 2) % count] if closed or index + 2 < count else p2
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
    if not closed:
        out.append(points[-1])
    return out


def scale_points(points: Sequence[Point], width: int, height: int) -> List[Point]:
    return [(x * width, y * height) for x, y in points]


def path_lengths(points: Sequence[Point]) -> Tuple[List[float], float]:
    cumulative = [0.0]
    total = 0.0
    for index in range(1, len(points) + 1):
        start = points[index - 1]
        end = points[index % len(points)]
        total += math.hypot(end[0] - start[0], end[1] - start[1])
        cumulative.append(total)
    return cumulative, total


def sample_path(points: Sequence[Point], distance: float) -> Tuple[Point, Point]:
    cumulative, total = path_lengths(points)
    distance %= total
    for index in range(1, len(cumulative)):
        if cumulative[index] < distance:
            continue
        start = points[index - 1]
        end = points[index % len(points)]
        segment = cumulative[index] - cumulative[index - 1]
        alpha = 0.0 if segment <= 0.0001 else (distance - cumulative[index - 1]) / segment
        x = start[0] + (end[0] - start[0]) * alpha
        y = start[1] + (end[1] - start[1]) * alpha
        tx = end[0] - start[0]
        ty = end[1] - start[1]
        length = math.hypot(tx, ty)
        if length <= 0.0001:
            return (x, y), (1.0, 0.0)
        return (x, y), (tx / length, ty / length)
    return points[0], (1.0, 0.0)


def draw_road(
        draw: ImageDraw.ImageDraw,
        points: Sequence[Point],
        road_width: int,
        closed: bool,
        fill) -> None:
    line_points = list(points)
    if closed:
        line_points.append(points[0])
    draw.line(line_points, fill=fill, width=road_width, joint="curve")
    radius = road_width * 0.5
    for x, y in points:
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=fill)


def load_font(size: int) -> ImageFont.ImageFont:
    for path in (
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"):
        try:
            return ImageFont.truetype(path, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def text_fits_road(road: Image.Image, mask: Image.Image, x: int, y: int) -> bool:
    road_pixels = road.load()
    mask_pixels = mask.load()
    width, height = road.size
    total = 0
    covered = 0
    for ty in range(mask.size[1]):
        py = y + ty
        if py < 0 or py >= height:
            continue
        for tx in range(mask.size[0]):
            if mask_pixels[tx, ty] == 0:
                continue
            total += 1
            px = x + tx
            if 0 <= px < width and road_pixels[px, py] > 0:
                covered += 1
    return total > 0 and covered / float(total) >= 0.96


def draw_label_if_fits(
        image: Image.Image,
        road: Image.Image,
        label: str,
        point: Point,
        tangent: Point,
        road_width: int) -> None:
    label_font = load_font(max(8, road_width // 4))
    measure = Image.new("L", (1, 1), 0)
    bbox = ImageDraw.Draw(measure).textbbox((0, 0), label, font=label_font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    mask = Image.new("L", (text_width, text_height), 0)
    ImageDraw.Draw(mask).text((-bbox[0], -bbox[1]), label, fill=255, font=label_font)
    normal = (-tangent[1], tangent[0])
    offsets = (
        (normal[0] * road_width * 0.22, normal[1] * road_width * 0.22),
        (-normal[0] * road_width * 0.22, -normal[1] * road_width * 0.22),
        (tangent[0] * road_width * 0.24, tangent[1] * road_width * 0.24),
        (-tangent[0] * road_width * 0.24, -tangent[1] * road_width * 0.24),
        (0.0, 0.0),
    )
    for dx, dy in offsets:
        x = int(round(point[0] + dx - text_width * 0.5))
        y = int(round(point[1] + dy - text_height * 0.5))
        if text_fits_road(road, mask, x, y):
            image.paste(LABEL_GRAY, (x, y), mask)
            return


def draw_marker(draw: ImageDraw.ImageDraw, point: Point, radius: int, color: Tuple[int, int, int]) -> None:
    x, y = point
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def draw_goal_gate(image: Image.Image, road: Image.Image, point: Point, tangent: Point, road_width: int) -> None:
    normal = (-tangent[1], tangent[0])
    pixels = road.load()
    width, height = road.size

    def find_edge(sign: float) -> float:
        last = 0.0
        for step in range(int(road_width * 0.75) + 1):
            distance = sign * step
            x = int(round(point[0] + normal[0] * distance))
            y = int(round(point[1] + normal[1] * distance))
            if 0 <= x < width and 0 <= y < height and pixels[x, y] > 0:
                last = distance
            elif step > 3:
                break
        return last

    start_distance = find_edge(-1.0)
    end_distance = find_edge(1.0)
    if abs(start_distance) + abs(end_distance) < road_width * 0.35:
        start_distance = -road_width * 0.50
        end_distance = road_width * 0.50
    start = (point[0] + normal[0] * start_distance, point[1] + normal[1] * start_distance)
    end = (point[0] + normal[0] * end_distance, point[1] + normal[1] * end_distance)
    line_mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(line_mask).line((start, end), fill=255, width=max(4, road_width // 8))
    image.paste(GREEN, mask=line_mask)


def draw_spawn_grid(draw: ImageDraw.ImageDraw, route_points: Sequence[Point], road_width: int) -> None:
    _, total = path_lengths(route_points)
    row_spacing = max(18.0, road_width * 0.52)
    column_spacing = max(18.0, road_width * 0.48)
    first_back_offset = max(18.0, road_width * 0.50)
    direction_offset = max(11.0, road_width * 0.32)
    spawn_radius = 3
    direction_radius = max(2, road_width // 18)
    for row in range(10):
        back = first_back_offset + row * row_spacing
        center, tangent = sample_path(route_points, total - back)
        normal = (-tangent[1], tangent[0])
        for column in range(2):
            side = (-0.5 if column == 0 else 0.5) * column_spacing
            spawn = (center[0] + normal[0] * side, center[1] + normal[1] * side)
            direction = (spawn[0] + tangent[0] * direction_offset, spawn[1] + tangent[1] * direction_offset)
            draw_marker(draw, spawn, spawn_radius, RED)
            draw_marker(draw, direction, direction_radius, BLUE)


def marker_points(route_points: Sequence[Point], marker_count: int) -> Iterable[Tuple[int, Point, Point]]:
    _, total = path_lengths(route_points)
    start_offset = total / marker_count * 0.18
    for index in range(marker_count):
        yield index + 1, *sample_path(route_points, start_offset + total * index / marker_count)


def render_route_map(index: int, template: RouteTemplate, output_dir: Path, width: int, height: int) -> None:
    road_width = template.road_width
    image = Image.new("RGB", (width, height), BLACK)
    road = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(image)
    road_draw = ImageDraw.Draw(road)
    route_points = scale_points(catmull_rom(template.route, 18, closed=True), width, height)
    draw_road(draw, route_points, road_width, closed=True, fill=WHITE)
    draw_road(road_draw, route_points, road_width, closed=True, fill=255)
    for branch in template.branches:
        branch_points = scale_points(catmull_rom(branch, 18, closed=False), width, height)
        draw_road(draw, branch_points, road_width, closed=False, fill=WHITE)
        draw_road(road_draw, branch_points, road_width, closed=False, fill=255)

    goal, goal_tangent = sample_path(route_points, 0.0)
    draw_goal_gate(image, road, goal, goal_tangent, road_width)
    for order, point, tangent in marker_points(route_points, template.marker_count):
        draw_marker(draw, point, max(4, road_width // 9), (255, min(250, order), 255))
        draw_label_if_fits(image, road, str(order), point, tangent, road_width)
        draw_marker(draw, point, max(4, road_width // 9), (255, min(250, order), 255))
    draw_spawn_grid(draw, route_points, road_width)

    output = output_dir / f"route{index:03d}_mask.png"
    image.save(output, optimize=True, compress_level=9)
    print(
        f"generated={output} template={template.name} "
        f"road_width={road_width} route_markers={template.marker_count}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="tools/rl/trainingMaps")
    parser.add_argument("--width", type=int, default=720)
    parser.add_argument("--height", type=int, default=420)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    for pattern in ("route*.json.gz", "route*.ser"):
        for path in output_dir.glob(pattern):
            path.unlink()
    for path in output_dir.glob("route*_mask.png"):
        path.unlink()
    for index, template in enumerate(ROUTE_TEMPLATES):
        render_route_map(index, template, output_dir, args.width, args.height)


if __name__ == "__main__":
    main()

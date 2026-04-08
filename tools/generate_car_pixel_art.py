#!/usr/bin/env python3
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, Tuple

from PIL import Image, ImageDraw, ImageFont

BASE_WIDTH = 32
BASE_HEIGHT = 48
BODY_TOP = 3
UPSCALE = 4
PREVIEW_SCALE = 2
PREVIEW_COLUMNS = 5
TRANSPARENT = (0, 0, 0, 0)
TIRE = (28, 31, 37, 255)
TIRE_HIGHLIGHT = (70, 75, 82, 255)
HEADLIGHT = (255, 239, 175, 255)
HEADLIGHT_WARM = (255, 215, 120, 255)
TAILLIGHT = (238, 88, 79, 255)
PREVIEW_BG = (17, 20, 27, 255)
PREVIEW_CARD = (26, 30, 39, 255)
PREVIEW_CARD_OUTLINE = (55, 62, 76, 255)


@dataclass(frozen=True)
class CarSpec:
    filename: str
    label: str
    body: str
    accent: str
    body_profile: Tuple[Tuple[int, int], ...]
    roof_profile: Tuple[Tuple[int, int], ...]
    roof_top: int
    front_axle: int
    rear_axle: int
    accent_mode: str
    extras: Tuple[str, ...] = ()
    front_light: str = "pair"
    rear_light: str = "split"
    wheel_width: int = 2
    wheel_height: int = 5
    roof_style: str = "glass"


CAR_SPECS: Tuple[CarSpec, ...] = (
    CarSpec(
        filename="player",
        label="PLAYER",
        body="#3279df",
        accent="#f6fafc",
        body_profile=((0, 3), (2, 5), (5, 7), (10, 9), (18, 10), (26, 10), (32, 9), (37, 8), (41, 7)),
        roof_profile=((0, 2), (3, 4), (7, 5), (13, 5), (18, 4), (22, 3)),
        roof_top=8,
        front_axle=12,
        rear_axle=31,
        accent_mode="hero",
        extras=("spoiler",),
        front_light="pair",
        rear_light="bar",
    ),
    CarSpec(
        filename="cinder",
        label="CINDER",
        body="#c54730",
        accent="#f4a038",
        body_profile=((0, 4), (2, 6), (6, 9), (12, 11), (19, 11), (28, 11), (35, 10), (41, 8)),
        roof_profile=((0, 3), (4, 5), (10, 6), (17, 5), (22, 3)),
        roof_top=10,
        front_axle=13,
        rear_axle=31,
        accent_mode="double",
        extras=("vents", "spoiler"),
        front_light="slash",
        rear_light="split",
    ),
    CarSpec(
        filename="frost",
        label="FROST",
        body="#7bc6f0",
        accent="#eef9ff",
        body_profile=((0, 4), (4, 6), (10, 8), (18, 8), (27, 8), (36, 7), (41, 5)),
        roof_profile=((0, 3), (4, 4), (10, 5), (16, 5), (22, 4)),
        roof_top=9,
        front_axle=13,
        rear_axle=30,
        accent_mode="cute",
        extras=("spark",),
        front_light="wide",
        rear_light="dots",
    ),
    CarSpec(
        filename="moss",
        label="MOSS",
        body="#5d7d3e",
        accent="#ccb57b",
        body_profile=((0, 4), (2, 6), (6, 8), (12, 10), (21, 10), (30, 10), (37, 9), (41, 8)),
        roof_profile=((0, 4), (4, 5), (10, 6), (18, 6), (24, 5)),
        roof_top=8,
        front_axle=12,
        rear_axle=31,
        accent_mode="nose",
        extras=("rack",),
        front_light="pair",
        rear_light="split",
    ),
    CarSpec(
        filename="volt",
        label="VOLT",
        body="#ead537",
        accent="#19c0e8",
        body_profile=((0, 2), (3, 4), (8, 6), (16, 8), (26, 9), (34, 8), (41, 6)),
        roof_profile=((0, 2), (5, 3), (10, 4), (18, 4), (22, 3)),
        roof_top=11,
        front_axle=14,
        rear_axle=31,
        accent_mode="bolt",
        extras=("skirts", "spoiler"),
        front_light="bar",
        rear_light="bar",
    ),
    CarSpec(
        filename="riot",
        label="RIOT",
        body="#c63b78",
        accent="#b7ef34",
        body_profile=((0, 4), (3, 6), (8, 8), (15, 9), (24, 9), (32, 8), (41, 7)),
        roof_profile=((0, 3), (5, 4), (11, 5), (18, 4), (22, 3)),
        roof_top=9,
        front_axle=13,
        rear_axle=31,
        accent_mode="offset",
        extras=("pipes", "patches"),
        front_light="quad",
        rear_light="split",
    ),
    CarSpec(
        filename="slate",
        label="SLATE",
        body="#6d7684",
        accent="#dfe8f3",
        body_profile=((0, 3), (3, 5), (9, 7), (16, 8), (24, 8), (33, 7), (41, 6)),
        roof_profile=((0, 2), (4, 4), (10, 5), (17, 4), (21, 3)),
        roof_top=10,
        front_axle=13,
        rear_axle=31,
        accent_mode="pin",
        extras=(),
        front_light="slash",
        rear_light="split",
    ),
    CarSpec(
        filename="tango",
        label="TANGO",
        body="#df7b46",
        accent="#fff1c8",
        body_profile=((0, 3), (3, 5), (8, 6), (15, 7), (22, 7), (30, 6), (41, 5)),
        roof_profile=((0, 2), (3, 3), (8, 4), (13, 4), (17, 2)),
        roof_top=11,
        front_axle=14,
        rear_axle=31,
        accent_mode="band",
        extras=("spoiler",),
        front_light="pair",
        rear_light="dots",
    ),
    CarSpec(
        filename="brick",
        label="BRICK",
        body="#a64438",
        accent="#f3d8ae",
        body_profile=((0, 5), (4, 7), (10, 9), (18, 10), (28, 10), (35, 10), (41, 9)),
        roof_profile=((0, 4), (5, 5), (12, 5), (20, 5), (24, 4)),
        roof_top=8,
        front_axle=12,
        rear_axle=31,
        accent_mode="grille",
        extras=("bullbar",),
        front_light="wide",
        rear_light="bar",
        wheel_width=3,
    ),
    CarSpec(
        filename="blitz",
        label="BLITZ",
        body="#14a8d8",
        accent="#f3fbff",
        body_profile=((0, 2), (3, 4), (7, 6), (13, 8), (20, 9), (29, 9), (36, 8), (41, 6)),
        roof_profile=((0, 2), (4, 4), (10, 5), (15, 5), (20, 4)),
        roof_top=10,
        front_axle=14,
        rear_axle=31,
        accent_mode="split",
        extras=("skirts", "fins"),
        front_light="slash",
        rear_light="bar",
    ),
    CarSpec(
        filename="orbit",
        label="ORBIT",
        body="#7f63c8",
        accent="#f2e8ff",
        body_profile=((0, 4), (4, 6), (10, 7), (18, 7), (26, 7), (34, 6), (41, 5)),
        roof_profile=((0, 3), (5, 5), (12, 6), (18, 5), (23, 3)),
        roof_top=9,
        front_axle=13,
        rear_axle=30,
        accent_mode="ring",
        extras=("bubble",),
        front_light="dots",
        rear_light="dots",
        roof_style="bubble",
    ),
    CarSpec(
        filename="knurl",
        label="KNURL",
        body="#6b7078",
        accent="#c67838",
        body_profile=((0, 3), (2, 5), (7, 8), (13, 9), (20, 9), (27, 9), (35, 8), (41, 6)),
        roof_profile=((0, 2), (4, 3), (9, 4), (15, 4), (20, 2)),
        roof_top=11,
        front_axle=14,
        rear_axle=31,
        accent_mode="patch",
        extras=("pipes", "patches", "vents"),
        front_light="pair",
        rear_light="quad",
    ),
    CarSpec(
        filename="viper",
        label="VIPER",
        body="#45a03b",
        accent="#c9ff88",
        body_profile=((0, 2), (4, 4), (10, 6), (18, 8), (26, 9), (34, 9), (41, 7)),
        roof_profile=((0, 2), (4, 3), (10, 4), (18, 4), (23, 3)),
        roof_top=12,
        front_axle=14,
        rear_axle=31,
        accent_mode="snake",
        extras=("fins",),
        front_light="slash",
        rear_light="split",
    ),
    CarSpec(
        filename="torque",
        label="TORQUE",
        body="#cb7034",
        accent="#a5d7f4",
        body_profile=((0, 4), (3, 6), (8, 8), (15, 10), (23, 11), (31, 11), (37, 10), (41, 8)),
        roof_profile=((0, 3), (5, 5), (12, 6), (20, 5), (24, 3)),
        roof_top=9,
        front_axle=12,
        rear_axle=31,
        accent_mode="double",
        extras=("vents", "spoiler"),
        front_light="wide",
        rear_light="bar",
        wheel_width=3,
    ),
    CarSpec(
        filename="crush",
        label="CRUSH",
        body="#a92f32",
        accent="#eab052",
        body_profile=((0, 5), (3, 7), (8, 9), (14, 10), (22, 10), (30, 9), (37, 8), (41, 7)),
        roof_profile=((0, 3), (4, 5), (10, 5), (17, 4), (22, 3)),
        roof_top=10,
        front_axle=13,
        rear_axle=31,
        accent_mode="grille",
        extras=("bullbar", "skirts"),
        front_light="wide",
        rear_light="split",
        wheel_width=3,
    ),
    CarSpec(
        filename="rivet",
        label="RIVET",
        body="#b8892e",
        accent="#f7e6b0",
        body_profile=((0, 4), (3, 6), (8, 7), (15, 8), (24, 8), (33, 8), (41, 7)),
        roof_profile=((0, 3), (4, 4), (10, 5), (17, 5), (22, 4)),
        roof_top=9,
        front_axle=13,
        rear_axle=31,
        accent_mode="stitch",
        extras=("rack", "patches"),
        front_light="pair",
        rear_light="quad",
    ),
    CarSpec(
        filename="dune",
        label="DUNE",
        body="#c7a14d",
        accent="#3db2c8",
        body_profile=((0, 3), (3, 5), (8, 7), (15, 8), (22, 8), (30, 7), (41, 5)),
        roof_profile=((0, 2), (4, 3), (9, 4), (15, 3), (20, 2)),
        roof_top=12,
        front_axle=14,
        rear_axle=31,
        accent_mode="dash",
        extras=("rollcage", "pods"),
        front_light="pair",
        rear_light="dots",
        roof_style="open",
    ),
    CarSpec(
        filename="glitch",
        label="GLITCH",
        body="#5d53d6",
        accent="#ff56ba",
        body_profile=((0, 3), (3, 5), (8, 7), (15, 8), (23, 8), (31, 8), (41, 6)),
        roof_profile=((0, 2), (4, 4), (10, 5), (16, 5), (22, 3)),
        roof_top=9,
        front_axle=13,
        rear_axle=31,
        accent_mode="glitch",
        extras=("patches",),
        front_light="bar",
        rear_light="bar",
    ),
    CarSpec(
        filename="piston",
        label="PISTON",
        body="#bf4336",
        accent="#f4debb",
        body_profile=((0, 4), (3, 6), (8, 8), (15, 9), (23, 9), (31, 8), (41, 6)),
        roof_profile=((0, 3), (4, 4), (10, 5), (16, 4), (21, 3)),
        roof_top=10,
        front_axle=13,
        rear_axle=31,
        accent_mode="classic",
        extras=("vents", "fins"),
        front_light="pair",
        rear_light="fins",
    ),
    CarSpec(
        filename="grit",
        label="GRIT",
        body="#866640",
        accent="#a5d0f0",
        body_profile=((0, 4), (3, 6), (8, 8), (15, 9), (24, 9), (33, 8), (41, 7)),
        roof_profile=((0, 3), (4, 4), (10, 5), (18, 5), (23, 4)),
        roof_top=9,
        front_axle=13,
        rear_axle=31,
        accent_mode="rally",
        extras=("rack", "pods"),
        front_light="quad",
        rear_light="quad",
    ),
)


def rgb(hex_value: str) -> Tuple[int, int, int]:
    hex_value = hex_value.lstrip("#")
    return tuple(int(hex_value[index:index + 2], 16) for index in (0, 2, 4))


def rgba(color: Tuple[int, int, int]) -> Tuple[int, int, int, int]:
    return color[0], color[1], color[2], 255


def mix(left: Tuple[int, int, int], right: Tuple[int, int, int], amount: float) -> Tuple[int, int, int]:
    return (
        int(round(left[0] + (right[0] - left[0]) * amount)),
        int(round(left[1] + (right[1] - left[1]) * amount)),
        int(round(left[2] + (right[2] - left[2]) * amount)),
    )


def darken(color: Tuple[int, int, int], amount: float) -> Tuple[int, int, int]:
    return mix(color, (0, 0, 0), amount)


def lighten(color: Tuple[int, int, int], amount: float) -> Tuple[int, int, int]:
    return mix(color, (255, 255, 255), amount)


def rasterize_profile(profile: Iterable[Tuple[int, int]]) -> Dict[int, int]:
    points = list(profile)
    rows: Dict[int, int] = {}
    for (y0, w0), (y1, w1) in zip(points, points[1:]):
        distance = max(1, y1 - y0)
        for step in range(distance + 1):
            amount = step / distance
            rows[y0 + step] = int(round(w0 + (w1 - w0) * amount))
    rows[points[-1][0]] = points[-1][1]
    return rows


def paint_hline(draw: ImageDraw.ImageDraw, y: int, x0: int, x1: int, color: Tuple[int, int, int, int]) -> None:
    if x0 > x1:
        return
    draw.line((x0, y, x1, y), fill=color)


def draw_profile(
    draw: ImageDraw.ImageDraw,
    rows: Dict[int, int],
    *,
    top: int,
    center_x: int,
    color: Tuple[int, int, int, int],
    inset: int = 0,
    trim_top: int = 0,
    trim_bottom: int = 0,
) -> None:
    row_keys = sorted(rows)
    last_key = row_keys[-1]
    for local_y in row_keys:
        if local_y < trim_top or local_y > last_key - trim_bottom:
            continue
        half_width = rows[local_y] - inset
        if half_width < 0:
            continue
        paint_hline(
            draw,
            top + local_y,
            center_x - half_width,
            center_x + half_width,
            color,
        )


def get_palette(spec: CarSpec) -> Dict[str, Tuple[int, int, int, int]]:
    body = rgb(spec.body)
    accent = rgb(spec.accent)
    return {
        "body": rgba(body),
        "body_shadow": rgba(darken(body, 0.22)),
        "outline": rgba(darken(body, 0.55)),
        "trim": rgba(darken(body, 0.72)),
        "highlight": rgba(lighten(body, 0.28)),
        "accent": rgba(accent),
        "accent_soft": rgba(mix(accent, body, 0.35)),
        "glass": rgba(mix((126, 166, 214), accent, 0.22)),
        "glass_highlight": rgba(mix((214, 240, 255), accent, 0.12)),
    }


def draw_wheels(draw: ImageDraw.ImageDraw, rows: Dict[int, int], spec: CarSpec) -> None:
    center_x = BASE_WIDTH // 2
    radius = spec.wheel_height // 2
    for axle in (spec.front_axle, spec.rear_axle):
        for offset in range(-radius, radius + 1):
            local_y = max(0, min(axle + offset, max(rows)))
            half_width = rows[local_y]
            y = BODY_TOP + local_y
            left_x0 = center_x - half_width - spec.wheel_width
            left_x1 = center_x - half_width - 1
            right_x0 = center_x + half_width + 1
            right_x1 = center_x + half_width + spec.wheel_width
            paint_hline(draw, y, left_x0, left_x1, TIRE)
            paint_hline(draw, y, right_x0, right_x1, TIRE)
            if offset == 0:
                paint_hline(draw, y, left_x0, left_x0, TIRE_HIGHLIGHT)
                paint_hline(draw, y, right_x1, right_x1, TIRE_HIGHLIGHT)


def draw_roof(draw: ImageDraw.ImageDraw, spec: CarSpec, palette: Dict[str, Tuple[int, int, int, int]]) -> None:
    roof_rows = rasterize_profile(spec.roof_profile)
    top = BODY_TOP + spec.roof_top
    center_x = BASE_WIDTH // 2

    if spec.roof_style == "open":
        draw_profile(draw, roof_rows, top=top, center_x=center_x, color=palette["outline"], inset=0)
        draw_profile(draw, roof_rows, top=top, center_x=center_x, color=palette["trim"], inset=1, trim_top=1, trim_bottom=1)
        for local_y in range(2, max(roof_rows) - 1, 5):
            half_width = max(roof_rows[local_y] - 1, 1)
            y = top + local_y
            paint_hline(draw, y, center_x - half_width + 1, center_x + half_width - 1, palette["accent_soft"])
        return

    draw_profile(draw, roof_rows, top=top, center_x=center_x, color=palette["outline"], inset=0)
    draw_profile(draw, roof_rows, top=top, center_x=center_x, color=palette["glass"], inset=1, trim_top=1, trim_bottom=1)
    draw_profile(draw, roof_rows, top=top, center_x=center_x, color=palette["glass_highlight"], inset=3, trim_top=2, trim_bottom=4)

    if spec.roof_style == "bubble":
        last_row = max(roof_rows)
        for local_y in range(2, last_row - 2, 4):
            half_width = max(roof_rows[local_y] - 2, 1)
            paint_hline(
                draw,
                top + local_y,
                center_x - half_width,
                center_x + half_width,
                palette["accent_soft"],
            )


def draw_panel_lines(draw: ImageDraw.ImageDraw, spec: CarSpec, rows: Dict[int, int], palette: Dict[str, Tuple[int, int, int, int]]) -> None:
    center_x = BASE_WIDTH // 2
    roof_bottom = spec.roof_top + max(point[0] for point in spec.roof_profile)
    for local_y in (spec.roof_top - 2, roof_bottom + 2):
        if local_y <= 1 or local_y >= max(rows) - 1:
            continue
        half_width = max(rows[local_y] - 2, 1)
        paint_hline(
            draw,
            BODY_TOP + local_y,
            center_x - half_width,
            center_x + half_width,
            palette["trim"],
        )
    for local_y in range(4, spec.roof_top - 1):
        if local_y % 5 == 0:
            paint_hline(draw, BODY_TOP + local_y, center_x, center_x, palette["highlight"])


def draw_center_stripe(img: Image.Image, rows: Dict[int, int], x_offsets: Tuple[int, ...], color: Tuple[int, int, int, int]) -> None:
    center_x = BASE_WIDTH // 2
    for local_y in range(4, max(rows) - 3):
        half_width = rows[local_y] - 2
        if half_width < 2:
            continue
        y = BODY_TOP + local_y
        for offset in x_offsets:
            x = center_x + offset
            if center_x - half_width <= x <= center_x + half_width:
                img.putpixel((x, y), color)


def draw_accent(draw: ImageDraw.ImageDraw, img: Image.Image, spec: CarSpec, rows: Dict[int, int], palette: Dict[str, Tuple[int, int, int, int]]) -> None:
    center_x = BASE_WIDTH // 2
    accent = palette["accent"]
    soft = palette["accent_soft"]

    if spec.accent_mode == "hero":
        draw_center_stripe(img, rows, (-2, 0, 2), accent)
        draw_center_stripe(img, rows, (-1, 1), palette["highlight"])
    elif spec.accent_mode == "double":
        draw_center_stripe(img, rows, (-3, -1, 1, 3), accent)
    elif spec.accent_mode == "pin":
        draw_center_stripe(img, rows, (0,), accent)
    elif spec.accent_mode == "band":
        for local_y in range(18, 25):
            half_width = max(rows[local_y] - 2, 1)
            paint_hline(draw, BODY_TOP + local_y, center_x - half_width + 2, center_x + half_width - 2, soft)
    elif spec.accent_mode == "bolt":
        points = [(center_x - 1, BODY_TOP + 7), (center_x + 2, BODY_TOP + 13), (center_x - 2, BODY_TOP + 20), (center_x + 1, BODY_TOP + 27), (center_x - 1, BODY_TOP + 35)]
        draw.line(points, fill=accent)
    elif spec.accent_mode == "offset":
        draw_center_stripe(img, rows, (-3,), accent)
        draw_center_stripe(img, rows, (2,), soft)
    elif spec.accent_mode == "split":
        for local_y in range(5, max(rows) - 4):
            half_width = max(rows[local_y] - 2, 1)
            y = BODY_TOP + local_y
            paint_hline(draw, y, center_x - half_width + 1, center_x, soft)
            if local_y % 3 == 0:
                paint_hline(draw, y, center_x + 1, center_x + half_width - 1, accent)
    elif spec.accent_mode == "ring":
        roof_rows = rasterize_profile(spec.roof_profile)
        roof_center_y = BODY_TOP + spec.roof_top + max(roof_rows) // 2
        draw.ellipse((center_x - 4, roof_center_y - 5, center_x + 4, roof_center_y + 5), outline=accent)
    elif spec.accent_mode == "patch":
        for x0, y0, x1, y1 in ((7, 16, 12, 20), (18, 27, 23, 31)):
            draw.rectangle((x0, y0, x1, y1), fill=soft)
    elif spec.accent_mode == "snake":
        points = [(center_x, BODY_TOP + 7), (center_x - 2, BODY_TOP + 13), (center_x + 2, BODY_TOP + 20), (center_x - 1, BODY_TOP + 28), (center_x + 1, BODY_TOP + 36)]
        draw.line(points, fill=accent)
    elif spec.accent_mode == "grille":
        for local_y in range(5, 9):
            half_width = max(rows[local_y] - 3, 1)
            paint_hline(draw, BODY_TOP + local_y, center_x - half_width, center_x + half_width, soft)
    elif spec.accent_mode == "stitch":
        for local_y in range(7, max(rows) - 5, 3):
            for x in (center_x - 3, center_x + 3):
                img.putpixel((x, BODY_TOP + local_y), accent)
    elif spec.accent_mode == "dash":
        for local_y in range(11, 32, 4):
            paint_hline(draw, BODY_TOP + local_y, center_x - 2, center_x + 2, accent)
    elif spec.accent_mode == "glitch":
        for rect in ((8, 11, 14, 17), (16, 17, 21, 24), (10, 29, 15, 35), (18, 33, 23, 38)):
            draw.rectangle(rect, fill=soft)
        draw_center_stripe(img, rows, (0,), accent)
    elif spec.accent_mode == "classic":
        draw_center_stripe(img, rows, (-2, 2), accent)
        for local_y in (14, 29):
            half_width = max(rows[local_y] - 2, 1)
            paint_hline(draw, BODY_TOP + local_y, center_x - half_width + 2, center_x + half_width - 2, soft)
    elif spec.accent_mode == "rally":
        draw_center_stripe(img, rows, (-2, 2), accent)
        draw_center_stripe(img, rows, (0,), palette["highlight"])
    elif spec.accent_mode == "nose":
        for local_y in range(5, 13):
            half_width = rows[local_y]
            y = BODY_TOP + local_y
            paint_hline(draw, y, center_x - half_width + 2, center_x - half_width + 3, accent)
            paint_hline(draw, y, center_x + half_width - 3, center_x + half_width - 2, accent)
    elif spec.accent_mode == "cute":
        for rect in ((12, 13, 14, 16), (17, 13, 19, 16), (14, 20, 17, 22)):
            draw.rectangle(rect, fill=soft)


def draw_lights(draw: ImageDraw.ImageDraw, spec: CarSpec, rows: Dict[int, int]) -> None:
    center_x = BASE_WIDTH // 2
    front_y = BODY_TOP + 2
    rear_y = BODY_TOP + max(rows) - 2
    front_half = rows[2]
    rear_half = rows[max(rows) - 2]

    if spec.front_light == "pair":
        paint_hline(draw, front_y, center_x - front_half + 2, center_x - front_half + 4, HEADLIGHT)
        paint_hline(draw, front_y, center_x + front_half - 4, center_x + front_half - 2, HEADLIGHT)
    elif spec.front_light == "wide":
        paint_hline(draw, front_y, center_x - front_half + 2, center_x + front_half - 2, HEADLIGHT_WARM)
    elif spec.front_light == "quad":
        for x in (center_x - front_half + 2, center_x - front_half + 4, center_x + front_half - 4, center_x + front_half - 2):
            paint_hline(draw, front_y, x, x, HEADLIGHT)
    elif spec.front_light == "slash":
        draw.line((center_x - front_half + 2, front_y + 1, center_x - front_half + 4, front_y - 1), fill=HEADLIGHT)
        draw.line((center_x + front_half - 4, front_y - 1, center_x + front_half - 2, front_y + 1), fill=HEADLIGHT)
    elif spec.front_light == "dots":
        paint_hline(draw, front_y, center_x - front_half + 3, center_x - front_half + 3, HEADLIGHT)
        paint_hline(draw, front_y, center_x + front_half - 3, center_x + front_half - 3, HEADLIGHT)
    elif spec.front_light == "bar":
        paint_hline(draw, front_y, center_x - front_half + 2, center_x + front_half - 2, HEADLIGHT)

    if spec.rear_light == "split":
        paint_hline(draw, rear_y, center_x - rear_half + 2, center_x - rear_half + 4, TAILLIGHT)
        paint_hline(draw, rear_y, center_x + rear_half - 4, center_x + rear_half - 2, TAILLIGHT)
    elif spec.rear_light == "bar":
        paint_hline(draw, rear_y, center_x - rear_half + 2, center_x + rear_half - 2, TAILLIGHT)
    elif spec.rear_light == "quad":
        for x in (center_x - rear_half + 2, center_x - rear_half + 4, center_x + rear_half - 4, center_x + rear_half - 2):
            paint_hline(draw, rear_y, x, x, TAILLIGHT)
    elif spec.rear_light == "dots":
        paint_hline(draw, rear_y, center_x - rear_half + 3, center_x - rear_half + 3, TAILLIGHT)
        paint_hline(draw, rear_y, center_x + rear_half - 3, center_x + rear_half - 3, TAILLIGHT)
    elif spec.rear_light == "fins":
        draw.line((center_x - rear_half + 2, rear_y - 1, center_x - rear_half + 4, rear_y), fill=TAILLIGHT)
        draw.line((center_x + rear_half - 4, rear_y, center_x + rear_half - 2, rear_y - 1), fill=TAILLIGHT)


def draw_extras(draw: ImageDraw.ImageDraw, spec: CarSpec, rows: Dict[int, int], palette: Dict[str, Tuple[int, int, int, int]]) -> None:
    center_x = BASE_WIDTH // 2
    rear_y = BODY_TOP + max(rows) - 1
    front_half = rows[2]
    rear_half = rows[max(rows) - 2]
    roof_rows = rasterize_profile(spec.roof_profile)
    roof_top = BODY_TOP + spec.roof_top
    roof_mid = roof_top + max(roof_rows) // 2

    for extra in spec.extras:
        if extra == "spoiler":
            paint_hline(draw, rear_y + 1, center_x - rear_half + 2, center_x + rear_half - 2, palette["outline"])
            paint_hline(draw, rear_y + 2, center_x - rear_half + 4, center_x + rear_half - 4, palette["accent_soft"])
        elif extra == "rack":
            for local_y in (3, max(roof_rows) - 3):
                half_width = max(roof_rows[local_y] - 1, 1)
                paint_hline(draw, roof_top + local_y, center_x - half_width, center_x + half_width, palette["accent_soft"])
            draw.line((center_x - 5, roof_top + 3, center_x - 5, roof_top + max(roof_rows) - 3), fill=palette["outline"])
            draw.line((center_x + 5, roof_top + 3, center_x + 5, roof_top + max(roof_rows) - 3), fill=palette["outline"])
        elif extra == "vents":
            for x0, x1 in ((center_x - 5, center_x - 3), (center_x + 3, center_x + 5)):
                for y in (BODY_TOP + 9, BODY_TOP + 12):
                    paint_hline(draw, y, x0, x1, palette["trim"])
        elif extra == "bullbar":
            paint_hline(draw, BODY_TOP + 1, center_x - front_half + 1, center_x + front_half - 1, palette["outline"])
            draw.line((center_x - 4, BODY_TOP + 1, center_x - 4, BODY_TOP + 4), fill=palette["trim"])
            draw.line((center_x + 4, BODY_TOP + 1, center_x + 4, BODY_TOP + 4), fill=palette["trim"])
        elif extra == "pipes":
            for y in range(BODY_TOP + 20, BODY_TOP + 30, 3):
                paint_hline(draw, y, center_x - rows[y - BODY_TOP] - 1, center_x - rows[y - BODY_TOP], palette["accent"])
                paint_hline(draw, y, center_x + rows[y - BODY_TOP], center_x + rows[y - BODY_TOP] + 1, palette["accent"])
        elif extra == "bubble":
            draw.ellipse((center_x - 6, roof_mid - 6, center_x + 6, roof_mid + 6), outline=palette["glass_highlight"])
        elif extra == "rollcage":
            draw.line((center_x - 4, roof_top + 2, center_x - 2, roof_top + max(roof_rows) - 2), fill=palette["accent_soft"])
            draw.line((center_x + 4, roof_top + 2, center_x + 2, roof_top + max(roof_rows) - 2), fill=palette["accent_soft"])
            paint_hline(draw, roof_mid, center_x - 4, center_x + 4, palette["accent_soft"])
        elif extra == "rally_lights":
            for x in (center_x - 3, center_x + 3):
                paint_hline(draw, BODY_TOP + 5, x, x + 1, HEADLIGHT)
        elif extra == "fins":
            draw.line((center_x - rear_half + 1, rear_y - 1, center_x - rear_half + 3, rear_y - 3), fill=palette["accent_soft"])
            draw.line((center_x + rear_half - 3, rear_y - 3, center_x + rear_half - 1, rear_y - 1), fill=palette["accent_soft"])
        elif extra == "patches":
            for point in ((10, 18), (21, 23), (13, 33)):
                draw.rectangle((point[0], point[1], point[0] + 1, point[1] + 1), fill=palette["accent"])
        elif extra == "pods":
            paint_hline(draw, BODY_TOP + 6, center_x - front_half + 2, center_x - front_half + 4, HEADLIGHT)
            paint_hline(draw, BODY_TOP + 6, center_x + front_half - 4, center_x + front_half - 2, HEADLIGHT)
        elif extra == "skirts":
            for local_y in range(15, max(rows) - 4):
                half_width = rows[local_y]
                y = BODY_TOP + local_y
                paint_hline(draw, y, center_x - half_width + 1, center_x - half_width + 1, palette["trim"])
                paint_hline(draw, y, center_x + half_width - 1, center_x + half_width - 1, palette["trim"])
        elif extra == "spark":
            draw.line((center_x - 1, BODY_TOP + 8, center_x + 1, BODY_TOP + 6), fill=palette["accent"])
            draw.line((center_x + 1, BODY_TOP + 6, center_x + 2, BODY_TOP + 9), fill=palette["accent"])


def render_car(spec: CarSpec) -> Image.Image:
    image = Image.new("RGBA", (BASE_WIDTH, BASE_HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(image)
    palette = get_palette(spec)
    body_rows = rasterize_profile(spec.body_profile)
    center_x = BASE_WIDTH // 2

    draw_wheels(draw, body_rows, spec)
    draw_profile(draw, body_rows, top=BODY_TOP, center_x=center_x, color=palette["outline"])
    draw_profile(draw, body_rows, top=BODY_TOP, center_x=center_x, color=palette["body"], inset=1, trim_top=1, trim_bottom=1)
    draw_profile(draw, body_rows, top=BODY_TOP + 1, center_x=center_x, color=palette["body_shadow"], inset=2, trim_top=6, trim_bottom=4)
    draw_panel_lines(draw, spec, body_rows, palette)
    draw_roof(draw, spec, palette)
    draw_accent(draw, image, spec, body_rows, palette)
    draw_lights(draw, spec, body_rows)
    draw_extras(draw, spec, body_rows, palette)
    return image


def create_preview_sheet(base_images: Dict[str, Image.Image], output_path: Path) -> None:
    font = ImageFont.load_default()
    tile_width = BASE_WIDTH * PREVIEW_SCALE + 16
    tile_height = BASE_HEIGHT * PREVIEW_SCALE + 24
    rows = (len(CAR_SPECS) + PREVIEW_COLUMNS - 1) // PREVIEW_COLUMNS
    sheet = Image.new(
        "RGBA",
        (PREVIEW_COLUMNS * tile_width + 8, rows * tile_height + 8),
        PREVIEW_BG,
    )
    draw = ImageDraw.Draw(sheet)

    for index, spec in enumerate(CAR_SPECS):
        column = index % PREVIEW_COLUMNS
        row = index // PREVIEW_COLUMNS
        x = 8 + column * tile_width
        y = 8 + row * tile_height
        draw.rectangle((x, y, x + tile_width - 6, y + tile_height - 6), fill=PREVIEW_CARD, outline=PREVIEW_CARD_OUTLINE)
        sprite = base_images[spec.filename].resize(
            (BASE_WIDTH * PREVIEW_SCALE, BASE_HEIGHT * PREVIEW_SCALE),
            Image.Resampling.NEAREST,
        )
        sheet.alpha_composite(sprite, (x + 8, y + 4))
        draw.text((x + 6, y + BASE_HEIGHT * PREVIEW_SCALE + 6), spec.label, font=font, fill=rgba(lighten(rgb(spec.accent), 0.15)))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate local pixel-art car sprites for ratass.")
    parser.add_argument(
        "--out-dir",
        default=str(Path(__file__).resolve().parents[1] / "assets" / "cars"),
        help="Directory where the generated PNG files are written.",
    )
    args = parser.parse_args()

    output_dir = Path(args.out_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    base_images: Dict[str, Image.Image] = {}
    for spec in CAR_SPECS:
        base_image = render_car(spec)
        base_images[spec.filename] = base_image
        final_image = base_image.resize(
            (BASE_WIDTH * UPSCALE, BASE_HEIGHT * UPSCALE),
            Image.Resampling.NEAREST,
        )
        destination = output_dir / f"{spec.filename}.png"
        final_image.save(destination)
        print(f"Wrote {destination}")

    preview_path = output_dir / "preview-sheet.png"
    create_preview_sheet(base_images, preview_path)
    print(f"Wrote {preview_path}")


if __name__ == "__main__":
    main()

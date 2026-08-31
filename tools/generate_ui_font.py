#!/usr/bin/env python3
"""Generate the high-resolution BMFont atlas used by the game UI."""

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


FONT_SIZE = 48
ATLAS_WIDTH = 2048
GLYPH_PADDING = 4


def supported_characters():
    codepoints = list(range(32, 127))
    codepoints.extend(range(160, 384))
    codepoints.extend(
        [
            0x2013,  # en dash
            0x2014,  # em dash
            0x2018,
            0x2019,
            0x201C,
            0x201D,
            0x2022,  # bullet
            0x2026,  # ellipsis
            0x20AC,  # euro
            0x2190,
            0x2191,
            0x2192,
            0x2193,
        ]
    )
    return codepoints


def next_power_of_two(value):
    return 1 << max(0, value - 1).bit_length()


def glyph_metrics(font, codepoint, base):
    character = chr(codepoint)
    left, top, right, bottom = font.getbbox(character, anchor="ls")
    width = max(0, math.ceil(right) - math.floor(left))
    height = max(0, math.ceil(bottom) - math.floor(top))
    return {
        "id": codepoint,
        "character": character,
        "left": math.floor(left),
        "top": math.floor(top),
        "width": width,
        "height": height,
        "xoffset": math.floor(left),
        "yoffset": base + math.floor(top),
        "xadvance": max(1, round(font.getlength(character))),
    }


def pack_glyphs(glyphs):
    cursor_x = GLYPH_PADDING
    cursor_y = GLYPH_PADDING
    row_height = 0
    for glyph in glyphs:
        if glyph["width"] == 0 or glyph["height"] == 0:
            glyph["x"] = 0
            glyph["y"] = 0
            continue
        packed_width = glyph["width"] + GLYPH_PADDING * 2
        packed_height = glyph["height"] + GLYPH_PADDING * 2
        if cursor_x + packed_width > ATLAS_WIDTH:
            cursor_x = GLYPH_PADDING
            cursor_y += row_height
            row_height = 0
        glyph["x"] = cursor_x + GLYPH_PADDING
        glyph["y"] = cursor_y + GLYPH_PADDING
        cursor_x += packed_width
        row_height = max(row_height, packed_height)
    return next_power_of_two(cursor_y + row_height + GLYPH_PADDING)


def render_atlas(font, glyphs, atlas_height, output_path):
    alpha = Image.new("L", (ATLAS_WIDTH, atlas_height), 0)
    draw = ImageDraw.Draw(alpha)
    for glyph in glyphs:
        if glyph["width"] == 0 or glyph["height"] == 0:
            continue
        draw.text(
            (glyph["x"] - glyph["left"], glyph["y"] - glyph["top"]),
            glyph["character"],
            font=font,
            fill=255,
            anchor="ls",
        )
    atlas = Image.new("RGBA", alpha.size, (255, 255, 255, 255))
    atlas.putalpha(alpha)
    atlas.save(output_path, optimize=True)


def kerning_pairs(font, codepoints):
    pairs = []
    advances = {codepoint: font.getlength(chr(codepoint)) for codepoint in codepoints}
    for first in codepoints:
        first_character = chr(first)
        first_advance = advances[first]
        for second in codepoints:
            amount = round(
                font.getlength(first_character + chr(second))
                - first_advance
                - advances[second]
            )
            if amount:
                pairs.append((first, second, amount))
    return pairs


def write_font_definition(
    font, glyphs, codepoints, base, line_height, atlas_height, image_name, output_path
):
    kernings = kerning_pairs(font, codepoints)
    lines = [
        'info face="Liberation Sans UI Semibold" size=48 bold=1 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1',
        "common lineHeight={} base={} scaleW={} scaleH={} pages=1 packed=0 alphaChnl=0 redChnl=4 greenChnl=4 blueChnl=4".format(
            line_height, base, ATLAS_WIDTH, atlas_height
        ),
        'page id=0 file="{}"'.format(image_name),
        "chars count={}".format(len(glyphs)),
    ]
    for glyph in glyphs:
        lines.append(
            "char id={id} x={x} y={y} width={width} height={height} xoffset={xoffset} yoffset={yoffset} xadvance={xadvance} page=0 chnl=15".format(
                **glyph
            )
        )
    lines.append("kernings count={}".format(len(kernings)))
    for first, second, amount in kernings:
        lines.append(
            "kerning first={} second={} amount={}".format(first, second, amount)
        )
    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--font", required=True, type=Path)
    parser.add_argument("--output-dir", default=Path("assets/fonts"), type=Path)
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)
    font = ImageFont.truetype(str(args.font), FONT_SIZE)
    ascent, descent = font.getmetrics()
    line_height = ascent + descent + 2
    base = ascent + 1
    codepoints = supported_characters()
    glyphs = [glyph_metrics(font, codepoint, base) for codepoint in codepoints]
    atlas_height = pack_glyphs(glyphs)

    image_path = args.output_dir / "ui-semibold.png"
    definition_path = args.output_dir / "ui-semibold.fnt"
    render_atlas(font, glyphs, atlas_height, image_path)
    write_font_definition(
        font,
        glyphs,
        codepoints,
        base,
        line_height,
        atlas_height,
        image_path.name,
        definition_path,
    )
    print("generated={} {}x{}".format(image_path, ATLAS_WIDTH, atlas_height))
    print("generated={} glyphs={}".format(definition_path, len(glyphs)))


if __name__ == "__main__":
    main()

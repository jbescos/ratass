#!/usr/bin/env python3
"""Build local trailer slates from current game artwork and captures."""

from __future__ import annotations

from pathlib import Path
from textwrap import wrap

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[2]
TRAILER = ROOT / "marketing" / "trailer"
RAW = TRAILER / "raw" / "current"
OUTPUT = TRAILER / "stills" / "generated"
WIDTH = 1920
HEIGHT = 1080
FONT = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")


def font(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT), size)


def fit_background(source: Image.Image, darkness: float = 0.24) -> Image.Image:
    scale = max(WIDTH / source.width, HEIGHT / source.height)
    size = (round(source.width * scale), round(source.height * scale))
    image = source.resize(size, Image.Resampling.LANCZOS)
    left = (image.width - WIDTH) // 2
    top = (image.height - HEIGHT) // 2
    image = image.crop((left, top, left + WIDTH, top + HEIGHT)).convert("RGB")
    image = image.filter(ImageFilter.GaussianBlur(10))
    return ImageEnhance.Brightness(image).enhance(darkness)


def shadow_text(
    draw: ImageDraw.ImageDraw,
    position: tuple[int, int],
    text: str,
    face: ImageFont.FreeTypeFont,
    color: str = "white",
    anchor: str | None = None,
    spacing: int = 12,
) -> None:
    x, y = position
    draw.multiline_text(
        (x + 4, y + 5), text, font=face, fill=(0, 0, 0, 230),
        anchor=anchor, spacing=spacing, align="center" if anchor == "mm" else "left",
    )
    draw.multiline_text(
        position, text, font=face, fill=color, anchor=anchor, spacing=spacing,
        align="center" if anchor == "mm" else "left",
    )


def wrapped(text: str, width: int) -> str:
    return "\n".join(
        line
        for paragraph in text.splitlines()
        for line in wrap(paragraph, width=width)
    )


def atlas_cell(path: Path, columns: int, rows: int, index: int) -> Image.Image:
    atlas = Image.open(path).convert("RGB")
    column = index % columns
    row = index // columns
    left = round(column * atlas.width / columns)
    top = round(row * atlas.height / rows)
    right = round((column + 1) * atlas.width / columns)
    bottom = round((row + 1) * atlas.height / rows)
    return atlas.crop((left, top, right, bottom))


def marketing_card(
    title: str,
    category: str,
    accent: str,
    artwork: Image.Image,
    effect: str,
    type_icon: Image.Image,
) -> Image.Image:
    width = 570
    height = 820
    card = Image.new("RGBA", (width, height), (2, 5, 8, 255))
    draw = ImageDraw.Draw(card, "RGBA")
    draw.rounded_rectangle(
        (4, 4, width - 5, height - 5), radius=8,
        fill=(3, 7, 11, 255), outline=accent, width=6,
    )
    draw.rounded_rectangle(
        (18, 18, width - 18, 106), radius=6,
        fill=(8, 13, 18, 255), outline=accent, width=3,
    )
    title_size = 38
    while draw.textbbox((0, 0), title, font=font(title_size))[2] > 365:
        title_size -= 2
    title_face = font(title_size)
    icon = ImageOps.contain(type_icon.convert("RGBA"), (66, 66), Image.Resampling.LANCZOS)
    card.alpha_composite(icon, (28 + (66 - icon.width) // 2, 29 + (66 - icon.height) // 2))
    draw.text((292, 62), title, font=title_face, fill="white", anchor="mm")
    draw.rounded_rectangle(
        (width - 100, 29, width - 30, 95), radius=6,
        fill=(12, 16, 22, 255), outline="#f5d35f", width=3,
    )
    draw.text((width - 65, 62), "T3", font=font(30), fill="#f5d35f", anchor="mm")

    art = ImageOps.fit(artwork.convert("RGB"), (530, 420), Image.Resampling.LANCZOS)
    card.paste(art, (20, 120))
    draw.rectangle((20, 120, 550, 540), outline=accent, width=4)
    draw.rounded_rectangle(
        (30, 552, width - 30, 744), radius=6,
        fill=(2, 5, 8, 248), outline=accent, width=3,
    )
    effect_size = 31
    effect_text = wrapped(effect, 34)
    while effect_size > 22:
        box = draw.multiline_textbbox(
            (0, 0), effect_text, font=font(effect_size), spacing=9, align="center"
        )
        if box[2] - box[0] <= 470 and box[3] - box[1] <= 160:
            break
        effect_size -= 1
    draw.multiline_text(
        (width // 2, 648), effect_text, font=font(effect_size), fill="#f5d35f",
        anchor="mm", spacing=9, align="center",
    )
    draw.rounded_rectangle(
        (72, 758, width - 72, 806), radius=6,
        fill=(5, 10, 14, 245), outline=accent, width=3,
    )
    draw.text((width // 2, 782), category, font=font(30), fill=accent, anchor="mm")
    return card


def card_scenes(source: Image.Image) -> None:
    artwork = ROOT / "assets/theme/gt3/roguelite/cards/artwork"
    type_icons = ROOT / "assets/roguelite/cards/card_type_icon_atlas.png"
    icons = {
        "DRIVER": atlas_cell(type_icons, 6, 1, 0),
        "TUNING": atlas_cell(type_icons, 6, 1, 1),
        "TECHNIQUE": atlas_cell(type_icons, 6, 1, 2),
        "POWERUP": atlas_cell(type_icons, 6, 1, 3),
        "REVENGE": atlas_cell(type_icons, 6, 1, 4),
        "SET": atlas_cell(ROOT / "assets/roguelite/cards/set_icon_atlas.png", 3, 3, 2),
    }
    cards = {
        "driver": ("LEON RAYE", "DRIVER", "#e6b83f",
                   atlas_cell(ROOT / "assets/theme/gt3/drivers/driver_art_atlas.png", 5, 3, 8),
                   "AVG LAP 34.76s | MAX SPEED 249 km/h\nOFF ROAD 2.1% | DRIFT 19.8%",
                   "Trust your driver, or take manual control.\nManual driving is much harder."),
        "tuning": ("AERO PROTOTYPE", "TUNING", "#f06445",
                   Image.open(artwork / "020.png"),
                   "Power +28%\nAero +55%",
                   "Permanent changes to power, grip, aero and mass."),
        "technique": ("CORNER MASTER", "TECHNIQUE", "#35c7e6",
                      Image.open(artwork / "086.png"),
                      "Activation: Corner | 4s\nGrip x3\nAero x3",
                      "Multiply your stats when race conditions trigger."),
        "powerup": ("QUANTUM QUARTET", "POWERUP", "#50d96f",
                    Image.open(artwork / "043.png"),
                    "Nearby rival on straight: 4 cars for 5s\nShared cards and Revenge | Cooldown: 10s",
                    "Special abilities with cooldowns.\nAutomatic or activated by you."),
        "revenge": ("HUNTER STORM", "REVENGE", "#f04b9c",
                    Image.open(artwork / "103.png"),
                    "Activation: Rival hit\nOffender: 2 shots/s for 3s",
                    "Somebody hit you?\nMake them pay."),
        "set": ("QUANTUM PACK", "SET", "#ae78f5",
                atlas_cell(ROOT / "assets/theme/gt3/roguelite/cards/set_art_atlas.png", 3, 3, 2),
                "Complete four marked cards\nBonus: Temporal Dominion",
                "Complete a specific build to unlock a sixth bonus card."),
    }

    background = fit_background(source)
    for filename, (card_name, title, accent, artwork_image, effect, description) in cards.items():
        canvas = background.copy()
        draw = ImageDraw.Draw(canvas, "RGBA")
        draw.rectangle((0, 0, WIDTH, HEIGHT), fill=(3, 7, 11, 95))
        draw.rectangle((0, 0, 20, HEIGHT), fill=accent)

        card = marketing_card(card_name, title, accent, artwork_image, effect, icons[title])
        card_x = 145
        card_y = (HEIGHT - card.height) // 2
        canvas.paste(card, (card_x, card_y), card)

        shadow_text(draw, (825, 265), title, font(84), accent)
        draw.rectangle((825, 370, 1730, 376), fill=accent)
        shadow_text(draw, (825, 430), wrapped(description, 36), font(42), "white", spacing=18)
        canvas.convert("RGB").save(OUTPUT / f"card-{filename}.png", optimize=True)


def card_wall() -> None:
    paths = sorted((ROOT / "assets/theme/gt3/roguelite/cards/artwork").glob("*.png"))
    paths += sorted((ROOT / "assets/theme/halloween/roguelite/cards/artwork").glob("*.png"))
    paths = paths[::2]
    tile = 204
    gap = 18
    rows = 4
    columns = (len(paths) + rows - 1) // rows
    wall_width = max(WIDTH + 1000, columns * (tile + gap) + gap)
    wall = Image.new("RGB", (wall_width, HEIGHT), "#04070a")
    draw = ImageDraw.Draw(wall, "RGBA")
    accents = ("#e6b83f", "#f06445", "#35c7e6", "#50d96f", "#f04b9c", "#ae78f5")

    for index, path in enumerate(paths):
        row = index % rows
        column = index // rows
        x = gap + column * (tile + gap) + (row % 2) * (tile // 3)
        y = 98 + row * (tile + gap)
        art = Image.open(path).convert("RGB").resize((tile, tile), Image.Resampling.LANCZOS)
        wall.paste(art, (x, y))
        draw.rectangle((x, y, x + tile, y + tile), outline=accents[index % len(accents)], width=5)

    wall.save(OUTPUT / "card-wall-wide.png", optimize=True)


def synergy_scene(source: Image.Image) -> None:
    canvas = fit_background(source, darkness=0.16)
    draw = ImageDraw.Draw(canvas, "RGBA")
    draw.rectangle((0, 0, WIDTH, HEIGHT), fill=(0, 0, 0, 105))
    shadow_text(draw, (WIDTH // 2, 90), "MAKE CRAZY SYNERGIES", font(72), "white", anchor="mm")

    cards = (
        (116, "TECHNIQUE\nSINGULARITY", "#f06445"),
        (119, "POWERUP\nNEXUS", "#35c7e6"),
        (43, "QUANTUM\nQUARTET", "#50d96f"),
        (103, "HUNTER\nSTORM", "#f04b9c"),
    )
    size = 310
    gap = 110
    total = len(cards) * size + (len(cards) - 1) * gap
    x = (WIDTH - total) // 2
    for index, (art_index, label, accent) in enumerate(cards):
        art_path = ROOT / f"assets/theme/gt3/roguelite/cards/artwork/{art_index:03d}.png"
        art = Image.open(art_path).convert("RGB").resize((size, size), Image.Resampling.LANCZOS)
        canvas.paste(art, (x, 230))
        draw.rectangle((x, 230, x + size, 230 + size), outline=accent, width=7)
        shadow_text(draw, (x + size // 2, 630), label, font(34), accent, anchor="mm", spacing=8)
        if index < len(cards) - 1:
            shadow_text(draw, (x + size + gap // 2, 385), "+", font(70), "white", anchor="mm")
        x += size + gap

    shadow_text(draw, (WIDTH // 2, 910), "147 CARDS. BUILD SOMETHING UNREASONABLE.", font(48), "#f5d35f", anchor="mm")
    canvas.save(OUTPUT / "synergy.png", optimize=True)


def end_slate() -> None:
    menu = Image.open(ROOT / "assets/game_menu.png").convert("RGB")
    canvas = fit_background(menu, darkness=0.55)
    draw = ImageDraw.Draw(canvas, "RGBA")
    draw.rectangle((0, 0, WIDTH, HEIGHT), fill=(0, 0, 0, 90))
    shadow_text(draw, (WIDTH // 2, 400), "ROGUE CIRCUIT", font(116), "#f4d05c", anchor="mm")
    shadow_text(draw, (WIDTH // 2, 535), "BUILD. RACE. REVENGE.", font(50), "white", anchor="mm")
    shadow_text(draw, (WIDTH // 2, 690), "A RACING ROGUELITE", font(38), "#d7e7ef", anchor="mm")
    canvas.save(OUTPUT / "end-slate.png", optimize=True)


def main() -> None:
    source_path = RAW / "card-types-current.png"
    if not source_path.is_file():
        raise SystemExit(f"Missing current card capture: {source_path}")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(source_path).convert("RGBA")
    card_scenes(source)
    card_wall()
    synergy_scene(source)
    end_slate()
    print(f"Generated trailer visuals in {OUTPUT}")


if __name__ == "__main__":
    main()

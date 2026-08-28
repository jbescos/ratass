#!/usr/bin/env python3
"""Build Steam store artwork and screenshots from checked-in game media."""

from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter


REPO_ROOT = Path(__file__).resolve().parents[2]
MENU_ART = REPO_ROOT / "assets" / "game_menu.png"
LIBRARY_HERO_ART = REPO_ROOT / "marketing" / "steam" / "source" / "library-hero-art.png"
TRAILER_ROOT = REPO_ROOT / "marketing" / "trailer" / "raw" / "current"
OUTPUT_ROOT = REPO_ROOT / "marketing" / "steam" / "store-assets"

ASSET_SIZES = {
    "capsule_header.png": (920, 430),
    "capsule_small.png": (462, 174),
    "capsule_main.png": (1232, 706),
    "capsule_vertical.png": (748, 896),
    "library_capsule.png": (600, 900),
    "library_hero.png": (3840, 1240),
    "library_logo.png": (1280, 720),
    "library_header.png": (920, 430),
}

SCREENSHOTS = (
    ("screenshot_01_starting_grid.png", "gt3-current.mp4", 20),
    ("screenshot_02_powerup_battle.png", "gt3-current.mp4", 35),
    ("screenshot_03_pack_racing.png", "gt3-current.mp4", 90),
    ("screenshot_04_card_choice.png", "gt3-current.mp4", 5),
    ("screenshot_05_halloween_pack.png", "halloween-current.mp4", 30),
    ("screenshot_06_halloween_chase.png", "halloween-current.mp4", 100),
)


def resize_cover(
    image: Image.Image,
    size: tuple[int, int],
    focus_x: float = 0.5,
    focus_y: float = 0.5,
) -> Image.Image:
    target_width, target_height = size
    scale = max(target_width / image.width, target_height / image.height)
    resized = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    left = round((resized.width - target_width) * focus_x)
    top = round((resized.height - target_height) * focus_y)
    left = max(0, min(left, resized.width - target_width))
    top = max(0, min(top, resized.height - target_height))
    return resized.crop((left, top, left + target_width, top + target_height))


def extract_logo(menu: Image.Image) -> Image.Image:
    source = menu.crop((45, 65, 890, 490)).convert("RGB")
    alpha = Image.new("L", source.size)
    source_pixels = source.load()
    alpha_pixels = alpha.load()

    for y in range(source.height):
        for x in range(source.width):
            red, green, blue = source_pixels[x, y]
            light = max(red, green, blue)
            neutral_score = max(0, (light - 105) * 3)
            red_score = 0
            if red > 75 and red > green * 1.22 and red > blue * 1.16:
                red_score = max(0, (red - max(green, blue) - 10) * 5)
            alpha_pixels[x, y] = min(255, max(neutral_score, red_score))

    alpha = alpha.filter(ImageFilter.GaussianBlur(0.35))
    logo = source.convert("RGBA")
    logo.putalpha(alpha)
    bounds = logo.getbbox()
    if bounds is None:
        raise RuntimeError("Could not extract the Rogue Circuit logo")
    return logo.crop(bounds)


def darken_left(image: Image.Image, strength: int = 175) -> Image.Image:
    overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
    pixels = overlay.load()
    fade_end = max(1, round(image.width * 0.72))
    for x in range(fade_end):
        alpha = round(strength * (1.0 - x / fade_end) ** 1.5)
        for y in range(image.height):
            pixels[x, y] = (8, 9, 11, alpha)
    return Image.alpha_composite(image.convert("RGBA"), overlay)


def darken_top(image: Image.Image, strength: int = 205) -> Image.Image:
    overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
    pixels = overlay.load()
    fade_end = max(1, round(image.height * 0.6))
    for y in range(fade_end):
        alpha = round(strength * (1.0 - y / fade_end) ** 1.7)
        for x in range(image.width):
            pixels[x, y] = (8, 9, 11, alpha)
    return Image.alpha_composite(image.convert("RGBA"), overlay)


def place_logo(
    image: Image.Image,
    logo: Image.Image,
    max_width: int,
    x: int,
    y: int,
) -> Image.Image:
    rendered = logo.copy()
    rendered.thumbnail((max_width, image.height), Image.Resampling.LANCZOS)
    result = image.convert("RGBA")
    result.alpha_composite(rendered, (x, y))
    return result


def polish(image: Image.Image) -> Image.Image:
    image = ImageEnhance.Contrast(image.convert("RGB")).enhance(1.04)
    return image.filter(ImageFilter.UnsharpMask(radius=1.2, percent=110, threshold=3))


def build_wide_asset(
    menu: Image.Image,
    logo: Image.Image,
    size: tuple[int, int],
    logo_width_ratio: float,
) -> Image.Image:
    racing_art = menu.crop((300, 550, 1536, 1024))
    background = resize_cover(racing_art, size, focus_x=0.67, focus_y=0.56)
    background = darken_left(background)
    max_width = round(size[0] * logo_width_ratio)
    rendered = logo.copy()
    rendered.thumbnail((max_width, round(size[1] * 0.82)), Image.Resampling.LANCZOS)
    x = round(size[0] * 0.035)
    y = (size[1] - rendered.height) // 2
    return place_logo(background, rendered, rendered.width, x, y)


def build_vertical_asset(
    menu: Image.Image,
    logo: Image.Image,
    size: tuple[int, int],
) -> Image.Image:
    racing_art = menu.crop((500, 500, 1536, 1024))
    background = resize_cover(racing_art, size, focus_x=0.58, focus_y=0.52)
    background = darken_top(background)
    rendered = logo.copy()
    rendered.thumbnail(
        (round(size[0] * 0.86), round(size[1] * 0.39)),
        Image.Resampling.LANCZOS,
    )
    x = (size[0] - rendered.width) // 2
    y = round(size[1] * 0.055)
    return place_logo(background, rendered, rendered.width, x, y)


def build_library_logo(logo: Image.Image) -> Image.Image:
    canvas = Image.new("RGBA", ASSET_SIZES["library_logo.png"], (0, 0, 0, 0))
    rendered = logo.copy()
    rendered.thumbnail((1180, 620), Image.Resampling.LANCZOS)
    canvas.alpha_composite(
        rendered,
        ((canvas.width - rendered.width) // 2, (canvas.height - rendered.height) // 2),
    )
    return canvas


def build_artwork() -> None:
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    menu = Image.open(MENU_ART).convert("RGB")
    library_hero_art = Image.open(LIBRARY_HERO_ART).convert("RGB")
    logo = extract_logo(menu)

    assets = {
        "capsule_header.png": build_wide_asset(
            menu, logo, ASSET_SIZES["capsule_header.png"], 0.50
        ),
        "capsule_small.png": build_wide_asset(
            menu, logo, ASSET_SIZES["capsule_small.png"], 0.49
        ),
        "capsule_main.png": build_wide_asset(
            menu, logo, ASSET_SIZES["capsule_main.png"], 0.51
        ),
        "capsule_vertical.png": build_vertical_asset(
            menu, logo, ASSET_SIZES["capsule_vertical.png"]
        ),
        "library_capsule.png": build_vertical_asset(
            menu, logo, ASSET_SIZES["library_capsule.png"]
        ),
        "library_hero.png": resize_cover(
            library_hero_art,
            ASSET_SIZES["library_hero.png"],
            focus_x=0.5,
            focus_y=0.61,
        ),
        "library_logo.png": build_library_logo(logo),
        "library_header.png": build_wide_asset(
            menu, logo, ASSET_SIZES["library_header.png"], 0.50
        ),
    }

    for filename, image in assets.items():
        output = OUTPUT_ROOT / filename
        if filename == "library_logo.png":
            image.save(output, optimize=True)
        else:
            polish(image).save(output, optimize=True)


def build_screenshots() -> None:
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise RuntimeError("ffmpeg is required to extract Steam screenshots")

    screenshot_root = OUTPUT_ROOT / "screenshots"
    screenshot_root.mkdir(parents=True, exist_ok=True)
    for filename, video_name, timestamp in SCREENSHOTS:
        source = TRAILER_ROOT / video_name
        if not source.is_file():
            raise FileNotFoundError(source)
        subprocess.run(
            [
                ffmpeg,
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-ss",
                str(timestamp),
                "-i",
                str(source),
                "-frames:v",
                "1",
                str(screenshot_root / filename),
            ],
            check=True,
        )


def verify_outputs(include_screenshots: bool) -> None:
    for filename, expected_size in ASSET_SIZES.items():
        output = OUTPUT_ROOT / filename
        with Image.open(output) as image:
            if image.size != expected_size:
                raise RuntimeError(f"{output}: expected {expected_size}, got {image.size}")
            if filename == "library_logo.png" and image.mode != "RGBA":
                raise RuntimeError(f"{output}: expected an alpha channel")

    if include_screenshots:
        for filename, _, _ in SCREENSHOTS:
            output = OUTPUT_ROOT / "screenshots" / filename
            with Image.open(output) as image:
                if image.size != (1920, 1080):
                    raise RuntimeError(f"{output}: expected 1920x1080, got {image.size}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--screenshots",
        action="store_true",
        help="rebuild screenshots from local, ignored trailer footage",
    )
    args = parser.parse_args()

    build_artwork()
    if args.screenshots:
        build_screenshots()
    verify_outputs(include_screenshots=args.screenshots)
    print(f"Steam store assets written to {OUTPUT_ROOT}")


if __name__ == "__main__":
    main()

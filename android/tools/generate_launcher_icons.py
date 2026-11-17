#!/usr/bin/env python3
"""Generate Android launcher icons from the exact logo used on the website."""
from io import BytesIO
from pathlib import Path

import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SVG_PATH = ROOT / "assets" / "army-inspired-crest.svg"
RES = ROOT / "android" / "app" / "src" / "main" / "res"

svg = SVG_PATH.read_bytes()

def render_logo(width, height):
    png = cairosvg.svg2png(bytestring=svg, output_width=width, output_height=height)
    return Image.open(BytesIO(png)).convert("RGBA")

# Adaptive icon foreground: keep the complete crest inside Android's safe zone.
foreground = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
logo = render_logo(270, 291)
foreground.alpha_composite(logo, ((432 - logo.width) // 2, (432 - logo.height) // 2))
foreground_dir = RES / "drawable-nodpi"
foreground_dir.mkdir(parents=True, exist_ok=True)
foreground.save(foreground_dir / "app_icon_foreground.png", optimize=True)

# Legacy launcher icons for devices before Android 8.
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for folder, size in sizes.items():
    out_dir = RES / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (size, size), "#172c59")
    side = int(size * 0.78)
    logo = render_logo(side, int(side * 560 / 520))
    # Fit within the square while preserving the original crest proportions.
    logo.thumbnail((int(size * 0.82), int(size * 0.88)), Image.Resampling.LANCZOS)
    canvas.alpha_composite(logo, ((size - logo.width) // 2, (size - logo.height) // 2))
    canvas.convert("RGB").save(out_dir / "ic_launcher.png", optimize=True)
    canvas.convert("RGB").save(out_dir / "ic_launcher_round.png", optimize=True)

print("Generated launcher icons from assets/army-inspired-crest.svg")

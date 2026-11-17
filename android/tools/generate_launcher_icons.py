#!/usr/bin/env python3
"""Generate Android launcher and onboarding artwork from the exact website hero logo."""
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

# This PNG is used in the first-launch screen and is rendered from the same SVG
# that the website displays beside "Welcome to SSC EXAM.IN".
drawable_dir = RES / "drawable-nodpi"
drawable_dir.mkdir(parents=True, exist_ok=True)
render_logo(520, 560).save(drawable_dir / "website_hero_logo.png", optimize=True)

# Adaptive icon foreground: preserve the complete website crest and transparent edges.
foreground = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
logo = render_logo(310, 334)
foreground.alpha_composite(logo, ((432 - logo.width) // 2, (432 - logo.height) // 2))
foreground.save(drawable_dir / "app_icon_foreground.png", optimize=True)

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
    logo = render_logo(520, 560)
    logo.thumbnail((int(size * 0.90), int(size * 0.94)), Image.Resampling.LANCZOS)
    canvas.alpha_composite(logo, ((size - logo.width) // 2, (size - logo.height) // 2))
    canvas.convert("RGB").save(out_dir / "ic_launcher.png", optimize=True)
    canvas.convert("RGB").save(out_dir / "ic_launcher_round.png", optimize=True)

print("Generated launcher and onboarding artwork from the exact website hero logo")

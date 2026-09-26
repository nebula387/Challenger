#!/usr/bin/env python3
"""Turn cut-out companion renders into the assets the app expects.

SDXL paints best at about one megapixel, which is nowhere near the shape of a
phone screen. So generate at a native SDXL resolution, cut the background out,
and let this script do the framing: it trims the transparent margin, scales the
figure and places it on the canvas the chosen placement wants.

    python tools/prepare_companion.py renders/ --mode bottom
    python tools/prepare_companion.py renders/ --mode full

Input files may be .png or .webp and must already have a transparent
background. They are matched by mood name, so name them sad, idle, smile,
happy and dance.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required:  pip install pillow")

MOODS = ("sad", "idle", "smile", "happy", "dance")

# Canvas per placement, and how much of its height the figure should occupy.
# Full screen leaves room at the top because the date and the first card sit
# there — a figure filling the frame ends up with its face behind a card.
LAYOUTS = {
    "bottom": {"size": (1024, 1536), "fill": 1.00},
    "full": {"size": (1080, 2400), "fill": 0.67},
}


def trim(image: Image.Image) -> Image.Image:
    """Drop fully transparent margins so scaling works off the figure itself."""
    box = image.getbbox()
    return image.crop(box) if box else image


def compose(source: Path, canvas: tuple[int, int], fill: float) -> Image.Image:
    figure = trim(Image.open(source).convert("RGBA"))

    width, height = canvas
    target_height = round(height * fill)
    scale = target_height / figure.height
    target_width = round(figure.width * scale)

    # A very wide figure would overflow the canvas, so width wins in that case.
    if target_width > width:
        scale = width / figure.width
        target_width, target_height = width, round(figure.height * scale)

    figure = figure.resize((target_width, target_height), Image.LANCZOS)

    out = Image.new("RGBA", canvas, (0, 0, 0, 0))
    out.paste(figure, ((width - target_width) // 2, height - target_height), figure)
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", type=Path, help="folder with the cut-out renders")
    parser.add_argument("--mode", choices=LAYOUTS, default="bottom",
                        help="placement the images are for (default: bottom)")
    parser.add_argument("--out", type=Path,
                        default=Path("app/src/main/assets/companion/default"),
                        help="pack folder to write into")
    parser.add_argument("--quality", type=int, default=88, help="WebP quality 1-100")
    args = parser.parse_args()

    layout = LAYOUTS[args.mode]
    args.out.mkdir(parents=True, exist_ok=True)

    missing = []
    for mood in MOODS:
        found = next(
            (p for ext in ("png", "webp", "jpg") for p in args.source.glob(f"{mood}.{ext}")),
            None,
        )
        if found is None:
            missing.append(mood)
            continue

        image = compose(found, layout["size"], layout["fill"])
        destination = args.out / f"{mood}.webp"
        image.save(destination, "WEBP", quality=args.quality, method=6)
        print(f"{found.name} -> {destination}  "
              f"{image.width}x{image.height}  {destination.stat().st_size // 1024} KB")

    if missing:
        print(f"\nstill missing: {', '.join(missing)} "
              f"(the app falls back to the nearest mood for these)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

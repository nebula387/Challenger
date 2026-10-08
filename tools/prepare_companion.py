#!/usr/bin/env python3
"""Frame cut-out companion renders for one of the two placements.

Image models paint at their own comfortable resolution, which is never the
shape of a phone screen. This script does the framing instead: it trims the
transparent margin and places the figure on a canvas with the right proportions
for the placement.

It never enlarges the figure. Upscaling a render softens it, and the app scales
whatever it is given to the screen anyway, so growing the pixels here would only
throw quality away twice. The canvas is sized around the source instead.

    python tools/prepare_companion.py renders/ --mode bottom
    python tools/prepare_companion.py renders/ --mode full

Input files need a transparent background and are matched by mood name, so call
them neutral, waiting, sad, happy, celebrating and praise.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required:  pip install pillow")

MOODS = ("neutral", "waiting", "sad", "happy", "celebrating", "praise")

# Proportions per placement, and how much of the canvas height the figure takes.
# Full screen leaves the top third clear because the date and the first card sit
# there — a figure filling the frame ends up with its face behind a card.
LAYOUTS = {
    "bottom": {"aspect": 1024 / 1536, "fill": 1.00},
    "full": {"aspect": 1080 / 2400, "fill": 0.67},
}


def trim(image: Image.Image) -> Image.Image:
    """Drop fully transparent margins so the framing works off the figure."""
    box = image.getbbox()
    return image.crop(box) if box else image


def compose(source: Path, aspect: float, fill: float) -> Image.Image:
    figure = trim(Image.open(source).convert("RGBA"))

    # Grow the canvas around the figure rather than the figure into a canvas.
    height = round(figure.height / fill)
    width = round(height * aspect)

    # Only a figure too wide for its canvas gets resized, and only downwards.
    if figure.width > width:
        scale = width / figure.width
        figure = figure.resize(
            (width, round(figure.height * scale)), Image.LANCZOS
        )

    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    canvas.paste(figure, ((width - figure.width) // 2, height - figure.height), figure)
    return canvas


def main() -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("source", type=Path, help="folder with the cut-out renders")
    parser.add_argument("--mode", choices=LAYOUTS, default="bottom",
                        help="placement the images are for (default: bottom)")
    parser.add_argument("--out", type=Path,
                        default=Path("app/src/main/assets/companion/default/full"),
                        help="folder to write into")
    parser.add_argument("--as-is", action="store_true", dest="as_is",
                        help="only convert to WebP, keep the framing untouched "
                             "(faces are already composed for a circular crop)")
    parser.add_argument("--lossy", type=int, metavar="QUALITY",
                        help="write lossy WebP at this quality instead of lossless")
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

        image = (
            Image.open(found).convert("RGBA") if args.as_is
            else compose(found, layout["aspect"], layout["fill"])
        )
        destination = args.out / f"{mood}.webp"
        if args.lossy:
            image.save(destination, "WEBP", quality=args.lossy, method=6)
        else:
            image.save(destination, "WEBP", lossless=True, method=6)

        print(f"{found.name} -> {destination}  {image.width}x{image.height}  "
              f"{destination.stat().st_size // 1024} KB")

    if missing:
        print(f"\nstill missing: {', '.join(missing)} "
              f"(the app falls back to the nearest mood for these)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

"""
Strips white/near-white backgrounds from team (and optionally league) badge PNGs by
flood-filling from the image border inward — only pixels connected to the edge get
zeroed, so a genuine white element in the middle of a crest (a star, a letter) is left
alone. Re-run safe: already-transparent images are no-ops.

Usage:
    python scripts/strip_badge_white_bg.py            # teams only (default)
    python scripts/strip_badge_white_bg.py --leagues   # leagues only
    python scripts/strip_badge_white_bg.py --all       # both
"""
import argparse
import glob
import os

import numpy as np
from PIL import Image
from scipy import ndimage

WHITE_THRESH = 235  # RGB channel value considered "near white"

BASE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "badges")


def strip_white_background(path: str) -> bool:
    im = Image.open(path).convert("RGBA")
    arr = np.array(im)
    r, g, b, a = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]

    near_white = (r >= WHITE_THRESH) & (g >= WHITE_THRESH) & (b >= WHITE_THRESH) & (a > 0)
    if not near_white.any():
        return False

    labeled, _ = ndimage.label(near_white)
    border_labels = set(labeled[0, :]) | set(labeled[-1, :]) | set(labeled[:, 0]) | set(labeled[:, -1])
    border_labels.discard(0)
    if not border_labels:
        return False

    mask = np.isin(labeled, list(border_labels))
    if not mask.any():
        return False

    arr[..., 3] = np.where(mask, 0, arr[..., 3])
    Image.fromarray(arr, "RGBA").save(path)
    return True


def run(folders: list[str]) -> None:
    changed = []
    for folder in folders:
        for f in glob.glob(os.path.join(BASE, folder, "*.png")):
            if strip_white_background(f):
                changed.append(f)

    print(f"Checked folders: {folders}")
    print(f"Changed {len(changed)} file(s):")
    for c in changed:
        print(" ", os.path.relpath(c, BASE))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--leagues", action="store_true", help="process leagues/ instead of teams/")
    parser.add_argument("--all", action="store_true", help="process both teams/ and leagues/")
    args = parser.parse_args()

    if args.all:
        run(["teams", "leagues"])
    elif args.leagues:
        run(["leagues"])
    else:
        run(["teams"])

#!/usr/bin/env python3
"""SpriteKiln のアトラス（1024x2048、128pxセル）から主人公スプライトを
ゲーム用に切り出すスクリプト。

使い方: python3 tools/slice_atlas_hero.py [アトラスPNG] [アトラスJSON]
既定はレトロピクセルアート調 hero-004 のアトラス。
出力先: android/assets/sprites2d/hero_d<向き>_f<フレーム>.png（歩行4コマ）
        android/assets/sprites2d/hero_d<向き>_idle.png（静止）

変換内容（tools/slice_hero2.py と同じ流儀）:
- アトラスは既に透明背景なので、マゼンタのキーアウトは不要。
- セルを内容の bounding box（alpha > 10）で切り抜く。
- 2px の余白を足す。
- 最大辺 160px に LANCZOS で縮小する（APK の軽量化）。

向きの対応：S=0, SW=1, W=2, NW=3, N=4, NE=5, E=6, SE=7。
歩行フレーム：walk_<dir>_0/2/4/6 → f0/f1/f2/f3
（接触・パッシング・逆接触・パッシング＝ゲームの4コマ構造と一致）。
静止フレーム：idle_<dir>_0 → idle。
"""
import json
import os
import sys

import numpy as np
from PIL import Image

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_ATLAS = os.path.join(
    BASE, "spritekiln-v2-pixel", "art", "specs", "characters", "dopakid",
    "out", "dopakid", "dopakid.png")
DEFAULT_JSON = os.path.join(
    BASE, "spritekiln-v2-pixel", "art", "specs", "characters", "dopakid",
    "out", "dopakid", "dopakid.json")
OUT_DIR = os.path.join(BASE, "android", "assets", "sprites2d")

PAD = 2
MAX_DIM = 160

# atlas の dir 名 → ゲームの向き番号
DIRS = {"s": 0, "sw": 1, "w": 2, "nw": 3, "n": 4, "ne": 5, "e": 6, "se": 7}


def trim(im):
    """内容の bounding box（alpha > 10）で切り抜く。"""
    alpha = np.asarray(im.split()[3])
    mask = alpha > 10
    if not mask.any():
        return im
    ys, xs = np.where(mask)
    return im.crop((int(xs.min()), int(ys.min()),
                    int(xs.max()) + 1, int(ys.max()) + 1))


def convert(cell):
    im = trim(cell)
    padded = Image.new("RGBA", (im.size[0] + 2 * PAD, im.size[1] + 2 * PAD),
                       (0, 0, 0, 0))
    padded.paste(im, (PAD, PAD))
    if max(padded.size) > MAX_DIM:
        scale = MAX_DIM / max(padded.size)
        nw = max(1, round(padded.size[0] * scale))
        nh = max(1, round(padded.size[1] * scale))
        padded = padded.resize((nw, nh), Image.LANCZOS)
    return padded


def main():
    atlas_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_ATLAS
    json_path = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_JSON
    with open(json_path, encoding="utf-8") as f:
        data = json.load(f)
    frames = data["frames"]
    atlas = Image.open(atlas_path).convert("RGBA")
    total = 0
    for dirname, d in sorted(DIRS.items(), key=lambda kv: kv[1]):
        for src_i, f in ((0, 0), (2, 1), (4, 2), (6, 3)):
            key = "walk_%s_%d" % (dirname, src_i)
            fr = frames[key]["frame"]
            cell = atlas.crop((fr["x"], fr["y"],
                               fr["x"] + fr["w"], fr["y"] + fr["h"]))
            out = convert(cell)
            out.save(os.path.join(OUT_DIR, "hero_d%d_f%d.png" % (d, f)),
                     optimize=True)
            print("  hero_d%d_f%d  %dx%d" % (d, f, out.size[0], out.size[1]))
            total += 1
        key = "idle_%s_0" % dirname
        fr = frames[key]["frame"]
        cell = atlas.crop((fr["x"], fr["y"],
                           fr["x"] + fr["w"], fr["y"] + fr["h"]))
        out = convert(cell)
        out.save(os.path.join(OUT_DIR, "hero_d%d_idle.png" % d),
                 optimize=True)
        print("  hero_d%d_idle  %dx%d" % (d, out.size[0], out.size[1]))
        total += 1
    print("converted %d sprites from %s" % (total, atlas_path))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

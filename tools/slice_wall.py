#!/usr/bin/env python3
"""縦長の壁スプライト（1枚の WebP、マゼンタ背景）から wall.png を切り出す。

使い方: python3 tools/slice_wall.py
出力: android/assets/sprites2d/wall.png（透過あり）
      android/assets/sprites2d/manifest.json の wall 項目を更新

壁は幅972×高さ1944（ちょうど2:1）に切り出す。上面（明るいタン色の
レンガ）と前面（暗い赤レンガ）の境がちょうど中央（972px）に来るよう、
上下を少しだけ切り詰める。64px幅で描くと高さ128px＝タイル2枚分に
なり、暗い前面がちょうど1タイル分、下端がタイルの下端に合う。
マゼンタ背景は透過にする（端のピンクのフリンジも含む）。
"""
import json
import os

import numpy as np
from PIL import Image

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(BASE, "samples", "sheets-flat",
                   "media-generation-dopamine-mall-wall-tall-0-ee11bda7-0898-4548-9389-1ae777df268d.webp")
OUT_DIR = os.path.join(BASE, "android", "assets", "sprites2d")
OUT = os.path.join(OUT_DIR, "wall.png")
MANIFEST = os.path.join(OUT_DIR, "manifest.json")

# 内容の bounding box（マゼンタ以外）：x 74〜1045、y 77〜2163。
# 上面と前面の境は y=1130 あたり。境を中央にするために
# x 74〜1046、y 158〜2102 を切り出す（972x1944、境は y=972）。
CROP = (74, 158, 1046, 2102)


def is_magenta(r, g, b):
    """背景（マゼンタ系）かどうか。白とマゼンタの混色によるピンクの
    フリンジ（r・b が高く g が低い）も背景とみなす。"""
    return r > 150 and b > 150 and (r - g) > 60 and (b - g) > 60


def main():
    im = Image.open(SRC).convert("RGB")
    cropped = im.crop(CROP)
    w, h = cropped.size
    print("cropped wall: %dx%d" % (w, h))
    arr = np.asarray(cropped).astype(int)
    # マゼンタ背景を透過にする
    alpha = np.full((h, w), 255, dtype=np.uint8)
    r, g, b = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2]
    magenta = (r > 150) & (b > 150) & ((r - g) > 60) & ((b - g) > 60)
    alpha[magenta] = 0
    out = np.dstack([arr.astype(np.uint8), alpha])
    Image.fromarray(out, "RGBA").save(OUT)
    print("wrote", OUT)
    # manifest を更新
    with open(MANIFEST, encoding="utf-8") as f:
        manifest = json.load(f)
    manifest["wall"] = {"file": "wall.png", "w": w, "h": h}
    with open(MANIFEST, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1, sort_keys=True)
    print("manifest updated: wall %dx%d" % (w, h))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

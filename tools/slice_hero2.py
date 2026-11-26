#!/usr/bin/env python3
"""Spritefy方式で作った主人公v2（samples/sheets-hero-v2/sliced/）を
ゲーム用のスプライトに変換するスクリプト。

使い方: python3 tools/slice_hero2.py
出力先: android/assets/sprites2d/hero_d<向き>_f<フレーム>.png（歩行4コマ）
        android/assets/sprites2d/hero_d<向き>_idle.png（静止）

変換内容（従来のパイプライン tools/slice_sheets.py と同じ）:
- マゼンタ背景（r>150, b>150, r-g>60, b-g>60）を透過にする。
  ※ 厳密な純マゼンタだけを抜くと、縮小時の LANCZOS のリンギングで
    ピンクのフリンジが復活する（1回目の変換で確認）。リサイズ後にも
    純マゼンタ抜きをもう1回行う。紫ブドウはこの条件に当たらない
    （従来の8方向シートと同じ条件で実績あり）。
- 2px の余白を足す。
- 最大辺 160px に LANCZOS で縮小する（APK の軽量化）。

向きの対応：S=0, SW=1, W=2, NW=3, N=4, NE=5, E=6, SE=7。
SW・W・NW は SE・E・NE の左右反転で作ってある（切り出し済み）。
"""
import os

from PIL import Image

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIR = os.path.join(BASE, "samples", "sheets-hero-v2", "sliced")
OUT_DIR = os.path.join(BASE, "android", "assets", "sprites2d")

PAD = 2
MAX_DIM = 160

DIRS = ["S", "SW", "W", "NW", "N", "NE", "E", "SE"]


def is_magenta_pixel(r, g, b):
    """背景（マゼンタ系）かどうか。白とマゼンタの混色によるピンクの
    フリンジ（r・b が高く g が低い）も背景とみなす。ただしピンク系の
    スプライト色（バッグなど）は b-g が小さいため残る。
    （tools/slice_sheets.py の is_magenta_pixel と同じ条件。
    従来の8方向シートでも紫ブドウは残った実績がある。）"""
    return r > 150 and b > 150 and (r - g) > 60 and (b - g) > 60


def key_out_magenta(im):
    """マゼンタ系の背景ピクセルを透過にする（リサイズ前に行う）。"""
    data = im.load()
    w, h = im.size
    for yy in range(h):
        for xx in range(w):
            r, g, b, a = data[xx, yy]
            if a > 10 and is_magenta_pixel(r, g, b):
                data[xx, yy] = (0, 0, 0, 0)
    return im


def key_out_strict(im):
    """リサイズ後に残った純マゼンタ（LANCZOS のリンギングで復活したもの）
    を透過にする。ブドウの紫（r<200）はこの条件に当たらない。"""
    data = im.load()
    w, h = im.size
    for yy in range(h):
        for xx in range(w):
            r, g, b, a = data[xx, yy]
            if a > 10 and r > 200 and g < 120 and b > 200:
                data[xx, yy] = (0, 0, 0, 0)
    return im


def convert(src_name, dst_name):
    src = os.path.join(SRC_DIR, src_name + ".png")
    im = Image.open(src).convert("RGBA")
    im = key_out_magenta(im)
    padded = Image.new("RGBA", (im.size[0] + 2 * PAD, im.size[1] + 2 * PAD),
                       (0, 0, 0, 0))
    padded.paste(im, (PAD, PAD))
    if max(padded.size) > MAX_DIM:
        scale = MAX_DIM / max(padded.size)
        nw = max(1, round(padded.size[0] * scale))
        nh = max(1, round(padded.size[1] * scale))
        padded = padded.resize((nw, nh), Image.LANCZOS)
    padded = key_out_strict(padded)
    out = os.path.join(OUT_DIR, dst_name + ".png")
    padded.save(out, optimize=True)
    return padded.size


def main():
    total = 0
    for d, name in enumerate(DIRS):
        for f in range(4):
            size = convert("hero2_%s_f%d" % (name, f), "hero_d%d_f%d" % (d, f))
            print("  hero_d%d_f%d  %dx%d" % (d, f, size[0], size[1]))
            total += 1
        size = convert("hero2_%s_idle" % name, "hero_d%d_idle" % d)
        print("  hero_d%d_idle  %dx%d" % (d, size[0], size[1]))
        total += 1
    print("converted %d sprites" % total)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

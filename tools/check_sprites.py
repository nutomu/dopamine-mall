#!/usr/bin/env python3
"""切り出したスプライトの品質ゲート用コンタクトシートを作る。
向き・フレームのラベル付きで大きく並べ、目視確認する。
出力先: samples/sheets-flat/check_*.png（ローカルのみ、コミットしない）
"""
import os
from PIL import Image, ImageDraw, ImageFont

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(BASE, "android", "assets", "sprites2d")
CHECK_DIR = os.path.join(BASE, "samples", "sheets-flat")

try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 20)
    FONT_S = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 16)
except Exception:
    FONT = ImageFont.load_default()
    FONT_S = FONT

DIRS = ["S", "SW", "W", "NW", "N", "NE", "E", "SE"]


def contact(names, out, cell_w=190, cell_h=210, label_fn=None):
    cols = len(names[0])
    rows = len(names)
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), (240, 240, 240))
    draw = ImageDraw.Draw(sheet)
    for ri, row in enumerate(names):
        for ci, name in enumerate(row):
            im = Image.open(os.path.join(OUT_DIR, name + ".png")).convert("RGBA")
            im.thumbnail((cell_w - 10, cell_h - 36))
            x = ci * cell_w + (cell_w - im.size[0]) // 2
            y = ri * cell_h + 4
            sheet.paste(im, (x, y), im)
            label = label_fn(ri, ci, name) if label_fn else name
            draw.text((ci * cell_w + 8, ri * cell_h + cell_h - 28), label,
                      fill=(0, 0, 0), font=FONT_S)
    sheet.save(os.path.join(CHECK_DIR, out))
    print("wrote %s" % out)


def main():
    os.makedirs(CHECK_DIR, exist_ok=True)
    # 主人公4コマ版：行＝フレーム、列＝向き
    contact([["hero4_d%d_f%d" % (c, r) for c in range(8)] for r in range(4)],
            "check_hero4.png",
            label_fn=lambda r, c, n: "d%d %s f%d" % (c, DIRS[c], r))
    # 主人公2コマ予備版
    contact([["hero2_d%d_f%d" % (c, r) for c in range(8)] for r in range(2)],
            "check_hero2.png",
            label_fn=lambda r, c, n: "d%d %s f%d" % (c, DIRS[c], r))
    # 平らなチップ15枚
    chips = ["floor_mall", "floor_wood", "floor_carpet", "wall", "bookshelf",
             "shelf", "counter", "table", "arcade", "escalator",
             "stairs", "stage", "bench", "vending", "plant"]
    contact([chips[i:i + 5] for i in range(0, 15, 5)], "check_chips.png")
    # 敵8方向（念のため向きの取り違えを確認）
    kinds = ["customer", "clerk", "guard", "bear", "dog", "angel"]
    contact([["%s_%d" % (k, c) for c in range(8)] for k in kinds],
            "check_enemies.png",
            label_fn=lambda r, c, n: "%s d%d %s" % (kinds[r], c, DIRS[c]))


if __name__ == "__main__":
    main()

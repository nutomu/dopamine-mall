#!/usr/bin/env python3
"""切り出した主人公スプライト（android/assets/sprites2d/hero_d*.png）の
品質ゲート用シートを作る。足シートを先に、フレーム単位で目視確認する。

使い方: python3 tools/check_hero_gate.py
出力先: samples/gate/hero4_contact.png（8方向×待機＋歩行4コマ）
        samples/gate/hero4_feet2x.png（下部45%を2倍に拡大した足シート）

ゲート（docs/ASSET_PIPELINE.md の品質ゲートと同じ）:
- f0/f2 で逆の足、f1/f3 は足を閉じたパッシング
- 上がった足にソール円盤がない（上面／側面のみ）
- ストライド軸が向きと一致（斜めは対角線、横向きの足運びでない）
- 斜めは体も三-quarter、スマホ左手・缶右手が正しい手にある
"""
import os

from PIL import Image, ImageDraw, ImageFont

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(BASE, "android", "assets", "sprites2d")
OUT_DIR = os.path.join(BASE, "samples", "gate")

try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 20)
    FONT_S = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 16)
except Exception:
    FONT = ImageFont.load_default()
    FONT_S = FONT

DIRS = ["S", "SW", "W", "NW", "N", "NE", "E", "SE"]
COLS = ["idle", "f0", "f1", "f2", "f3"]
BG = (30, 30, 44)


def load(name):
    return Image.open(os.path.join(SPRITES, name + ".png")).convert("RGBA")


def contact():
    cell_w, cell_h = 170, 190
    sheet = Image.new("RGB", (len(COLS) * cell_w, len(DIRS) * cell_h), BG)
    d = ImageDraw.Draw(sheet)
    for ri, dirname in enumerate(DIRS):
        d.text((8, ri * cell_h + 8), dirname, fill=(255, 255, 255), font=FONT)
        for ci, col in enumerate(COLS):
            name = "hero_d%d_%s" % (ri, col)
            im = load(name)
            im.thumbnail((cell_w - 12, cell_h - 40))
            x = ci * cell_w + (cell_w - im.size[0]) // 2
            y = ri * cell_h + 30
            sheet.paste(im, (x, y), im)
            d.text((ci * cell_w + 8, ri * cell_h + cell_h - 24), col,
                   fill=(200, 200, 200), font=FONT_S)
    sheet.save(os.path.join(OUT_DIR, "hero4_contact.png"))
    print("wrote hero4_contact.png", sheet.size)


def feet():
    """下部45%を切り出し、2倍に拡大した足シート。"""
    cell_w, cell_h = 340, 260
    sheet = Image.new("RGB", (len(COLS) * cell_w, len(DIRS) * cell_h), BG)
    d = ImageDraw.Draw(sheet)
    for ri, dirname in enumerate(DIRS):
        d.text((8, ri * cell_h + 8), dirname, fill=(255, 255, 255), font=FONT)
        for ci, col in enumerate(COLS):
            name = "hero_d%d_%s" % (ri, col)
            im = load(name)
            w, h = im.size
            crop = im.crop((0, int(h * 0.55), w, h))
            crop = crop.resize((crop.size[0] * 2, crop.size[1] * 2),
                               Image.NEAREST)
            x = ci * cell_w + (cell_w - crop.size[0]) // 2
            y = ri * cell_h + 40
            sheet.paste(crop, (x, y), crop)
            d.text((ci * cell_w + 8, ri * cell_h + cell_h - 24), col,
                   fill=(200, 200, 200), font=FONT_S)
    sheet.save(os.path.join(OUT_DIR, "hero4_feet2x.png"))
    print("wrote hero4_feet2x.png", sheet.size)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    contact()
    feet()
    return 0


if __name__ == "__main__":
    main()

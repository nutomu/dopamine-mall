#!/usr/bin/env python3
"""Q3修正（2026-11-28）の見本を作るスクリプト。

家具の真北の壁も上面を描くようになった新ルール（preview_iso.wall_is_tall）
で描く。修正前は samples/preview25d/repro_furniture_vs_wall.png と
samples/preview25d/furniture_corner_zoom.png（旧ルール）を使うこと。

出力: samples/preview25d/furniture_top_fixed.png
      samples/preview25d/furniture_corner_zoom_fixed.png
      samples/preview25d/wall_north_stairs_fixed.png
      samples/preview25d/doorway_horizontal_fixed.png
      samples/preview25d/doorway_vertical_fixed.png
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from preview_iso import FONT_PATH, TILE, load
from preview_q2_q3 import FONT_CJK, font_cjk, repaint_subtitle
from preview_repro import OUT_DIR, SCENES, render

# 階段・エスカレーターが壁の真北に来るシーン。
# X・E の南の壁は前面だけになり、階段が見えるままになること。
# それ以外の壁は上面を描くこと。
LAYOUT_STAIRS = [
    "..........",
    "..X..E....",
    "..######..",
    "..........",
    "..P..c....",
    "..........",
]


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    # ---- 家具の真北の壁も上面を描く（修正後）----
    # 見出しはサブセットフォントのため漢字（修正後）が出ないので英字にする。
    img = render(SCENES["furniture_vs_wall"], "floor_mall",
                 "furniture_vs_wall (fixed)")
    repaint_subtitle(img, "修正後：北が固体家具の壁も上面を描き、上面が連続する")
    out = os.path.join(OUT_DIR, "furniture_top_fixed.png")
    img.convert("RGB").save(out)
    print("wrote", out)

    # ---- Q3 拡大・注記（修正後。切り抜き範囲は furniture_corner_zoom.png と同じ）----
    full = render(SCENES["furniture_vs_wall"], "floor_mall", "furniture_vs_wall")
    ox, oy = 96, 160
    table_tex = load("table")
    table_w = int(56 * table_tex.width / table_tex.height)
    table_right = ox + 2 * TILE + table_w // 2
    book_tex = load("bookshelf")
    book_w = int(80 * book_tex.width / book_tex.height)
    book_left = ox + 5 * TILE - book_w // 2
    book_right = ox + 5 * TILE + book_w // 2
    covered = [
        (256, 192, table_right, 224, "1"),
        (book_left, 192, 384, 224, "2"),
        (448, 192, book_right, 224, "3"),
    ]
    crop_box = (140, 130, 480, 310)
    crop = full.crop(crop_box).resize(
        ((crop_box[2] - crop_box[0]) * 3, (crop_box[3] - crop_box[1]) * 3),
        Image.NEAREST)
    title_h, caption_h = 56, 136
    canvas = Image.new("RGB", (crop.width, title_h + crop.height + caption_h),
                       (24, 26, 32))
    canvas.paste(crop, (0, title_h))
    d = ImageDraw.Draw(canvas)
    d.text((16, 12), "修正後：家具の真北の壁も上面を描く（上面が連続）",
           font=font_cjk(26), fill=(255, 255, 255))
    # 緑枠：壁の上面の帯（タイル x=2〜7、y=192〜224）が連続している範囲。
    gx0 = (192 - crop_box[0]) * 3
    gy0 = title_h + (192 - crop_box[1]) * 3
    gx1 = (512 - crop_box[0]) * 3
    gy1 = title_h + (224 - crop_box[1]) * 3
    d.rectangle([gx0, gy0, gx1, gy1], outline=(80, 220, 120), width=4)
    for (x0, y0, x1, y1, num) in covered:
        cx0 = (x0 - crop_box[0]) * 3
        cy0 = title_h + (y0 - crop_box[1]) * 3
        cx1 = (x1 - crop_box[0]) * 3
        cy1 = title_h + (y1 - crop_box[1]) * 3
        d.rectangle([cx0, cy0, cx1, cy1], outline=(255, 60, 60), width=4)
        nx, ny = cx0 - 4, cy0 - 4
        d.ellipse([nx - 15, ny - 15, nx + 15, ny + 15],
                  fill=(255, 60, 60), outline=(255, 255, 255), width=2)
        d.text((nx, ny), num, font=font_cjk(22), fill=(255, 255, 255), anchor="mm")
    cy = title_h + crop.height + 12
    d.text((16, cy), "緑枠：壁の上面が x=2〜7 で連続し、家具の下部が均一に覆われる",
           font=font_cjk(20), fill=(120, 220, 140))
    d.text((16, cy + 32), "赤枠1〜3：家具の下隅が上面に覆われるのは正しい隠れ方（壁が手前）",
           font=font_cjk(20), fill=(255, 255, 255))
    d.text((16, cy + 64), "修正前はテーブル・本棚の真北だけ上面が無く、床が見えて壁が薄く見えた",
           font=font_cjk(20), fill=(255, 255, 255))
    d.text((16, cy + 96), "※ 修正前の見本は furniture_corner_zoom.png",
           font=font_cjk(20), fill=(255, 200, 80))
    out = os.path.join(OUT_DIR, "furniture_corner_zoom_fixed.png")
    canvas.save(out)
    print("wrote", out)

    # ---- 階段・エスカレーターが壁の真北（前面だけのまま）----
    img = render(LAYOUT_STAIRS, "floor_mall", "stairs north of wall (fixed)")
    repaint_subtitle(img, "修正後：北が階段・エスカレーターの壁は前面だけ（階段が見える）")
    out = os.path.join(OUT_DIR, "wall_north_stairs_fixed.png")
    img.convert("RGB").save(out)
    print("wrote", out)

    # ---- 出入口が新ルールでも見えること（既存シーンの再描画）----
    img = render(SCENES["horizontal_door"], "floor_mall",
                 "horizontal door (fixed)")
    repaint_subtitle(img, "修正後：出入口の隙間は前面だけのまま（隙間が見える）")
    out = os.path.join(OUT_DIR, "doorway_horizontal_fixed.png")
    img.convert("RGB").save(out)
    print("wrote", out)

    img = render(SCENES["vertical_door"], "floor_mall",
                 "vertical door (fixed)")
    repaint_subtitle(img, "修正後：出入口の隙間は前面だけのまま（隙間が見える）")
    out = os.path.join(OUT_DIR, "doorway_vertical_fixed.png")
    img.convert("RGB").save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()

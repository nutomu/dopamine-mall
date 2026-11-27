#!/usr/bin/env python3
"""描画の不具合の再現シーンを描くスクリプト。

ゲーム本体（MallScreen.java）の描画ループを忠実に再現する：
- ペインター法：行ごとに北（y=0）から南へ。各行でまず全タイルを描き、
  次にその行のキャラクターを描く。
- 壁は 64x128 のチップで、下端をタイルの下端に合わせ、北へ1タイル分
  はみ出す（自分のマス＝前面・赤レンガ、北のマス＝上面・薄茶タイル）。
- 家具・キャラクターは足元中心から北へ高さ分だけ描く。

壁モード：
- current：現状のゲーム通り、北のマスが何であってもはみ出す。
- fixed：北のマスが壁（または範囲外）のときだけはみ出す。
  北のマスが床・家具などのときは前面（64px）だけ描く。

使い方: python3 tools/preview_repro.py
出力: samples/preview25d/repro_<scene>_<mode>.png
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from preview_iso import (ACTORS, FONT_PATH, NORM, SPRITES, TERRAIN, TILE,
                         draw_anchored, draw_shadow, load, tile)

OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "samples", "preview25d")
WALL_H = TILE * 2


def font(size):
    try:
        return ImageFont.truetype(FONT_PATH, size)
    except (OSError, IOError):
        return ImageFont.load_default()


def draw_wall_current(img, px, py):
    """現状のゲーム：北のマスが何でもはみ出す。"""
    wall = load("wall")
    spr = wall.resize((TILE, WALL_H), Image.LANCZOS)
    img.paste(spr, (px - TILE // 2, py - TILE // 2 - TILE), spr)


def draw_wall_fixed(img, px, py, north_is_wall):
    """修正案：北のマスが壁（または範囲外）のときだけはみ出す。
    それ以外は前面（テクスチャ下半分）だけを自分のマスに描く。"""
    wall = load("wall")
    if north_is_wall:
        spr = wall.resize((TILE, WALL_H), Image.LANCZOS)
        img.paste(spr, (px - TILE // 2, py - TILE // 2 - TILE), spr)
    else:
        # 前面だけ（下半分）を 64x64 で自分のマスに描く
        front = wall.crop((0, wall.height // 2, wall.width, wall.height))
        spr = front.resize((TILE, TILE), Image.LANCZOS)
        img.paste(spr, (px - TILE // 2, py - TILE // 2), spr)


def render(layout, floor_tex_name, mode, label):
    """レイアウトをゲーム忠実に描く。mode は current / fixed。"""
    rows = len(layout)
    cols = len(layout[0])
    ox, oy = 96, 160
    W = ox * 2 + cols * TILE
    H = oy + rows * TILE + 80
    img = Image.new("RGBA", (W, H), (24, 26, 32, 255))
    d = ImageDraw.Draw(img)
    f = font(22)
    d.text((16, 16), label, font=f, fill=(255, 255, 255))
    d.text((16, 48), "北が上。壁のはみ出し：%s" % (
        "常に北へ1マス" if mode == "current" else "北が壁のときだけ"),
        font=font(18), fill=(180, 180, 180))
    floor_tex = load(floor_tex_name)
    for y in range(rows):
        for x in range(cols):
            ch = layout[y][x]
            if ch == " ":
                continue
            px, py = tile(x, y, ox, oy)
            if ch == "#":
                north = layout[y - 1][x] if y > 0 else "#"
                if mode == "current":
                    draw_wall_current(img, px, py)
                else:
                    draw_wall_fixed(img, px, py, north == "#")
                continue
            chip = floor_tex.resize((TILE, TILE), Image.LANCZOS)
            img.paste(chip, (px - TILE // 2, py - TILE // 2), chip)
            if ch in TERRAIN:
                name, height = TERRAIN[ch]
                draw_anchored(img, load(name), px, py, height)
            if ch in ACTORS:
                name, height = ACTORS[ch]
                if height <= 40:
                    tex = load(name)
                    w = max(1, int(height * tex.width / tex.height))
                    spr = tex.resize((w, height), Image.LANCZOS)
                    img.paste(spr, (px - w // 2, py - height // 2), spr)
                else:
                    draw_shadow(img, px, py, 40)
                    draw_anchored(img, load(name), px, py, height, name)
    return img


SCENES = {
    # 縦壁の出入口：壁の柱に1マスの隙間（床）。隙間の南に壁がある。
    "vertical_door": [
        "..........",
        "....#.....",
        "....#.....",
        "....#.....",
        "....P.....",
        "....#.....",
        "....#.....",
        "....#.....",
        "..........",
    ],
    # 横壁の出入口：上下の部屋を分ける壁に1マスの隙間。
    "horizontal_door": [
        "##########",
        "#........#",
        "#...P....#",
        "####.#####",
        "#........#",
        "#....c...#",
        "##########",
    ],
    # 本棚が北の壁に接した部屋（にゅーいちのスクリーンショット相当）。
    # 本棚の南に壁はないが、部屋の南壁の北側は床。
    "bookshelf_room": [
        "##########",
        "#BB...C..#",
        "#........#",
        "#...P....#",
        "#..T.....#",
        "#........#",
        "##########",
    ],
    # 家具のすぐ南に壁：テーブル・自販機・本棚の南隣が壁。
    "furniture_vs_wall": [
        "..........",
        "..T..v....",
        "..B......#",
        "..........",
        "..P..c....",
        "..........",
    ],
    # 部屋の角：L字の壁と、その内側の床・本棚。
    "corner": [
        "#####.....",
        "#...#.....",
        "#.B.#.....",
        "#...#.....",
        "#...######",
        "#........#",
        "#####.####",
    ],
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, layout in SCENES.items():
        for mode in ("current", "fixed"):
            label = "%s [%s]" % (name, mode)
            img = render(layout, "floor_mall", mode, label)
            out = os.path.join(OUT_DIR, "repro_%s_%s.png" % (name, mode))
            img.convert("RGB").save(out)
            print("wrote", out)


if __name__ == "__main__":
    main()

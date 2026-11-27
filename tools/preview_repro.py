#!/usr/bin/env python3
"""描画の不具合の再現シーンを描くスクリプト。

ゲーム本体（MallScreen.java）の描画ループを忠実に再現する：
- 2パス構成：床を全部先に描き、壁・家具・キャラ・アイテムを
  足元の画面Yで奥（北）から手前にソートして描く（3/4見下ろし描画）。
  PIL は y が下向きなので、ソートは足元画面Yの昇順。
- 壁は 64x128 のチップで、下端をタイルの下端に合わせ、北へ1タイル分
  はみ出す（自分のマス＝前面・赤レンガ、北のマス＝上面・薄茶タイル）。
- 家具・キャラクターは足元中心から北へ高さ分だけ描く。

壁の上面を描く条件（ゲーム本体の drawWall と同じ。preview_iso.wall_is_tall）：
- 北が壁（y==0 の北端を含む）か、北が開けた床のときは背の高いチップ。
- 北が家具・階段などの地形文字のときは前面だけ（家具・階段を隠さない）。
- 北が通路（出入口の隙間や1マス幅の通路）のときも前面だけ（出入口を隠さない）。
- アクター文字はゲームでは床タイルの上に立つものとして扱う
  （壁の北側に立つキャラの下半身が壁に隠れる、というゲームと同じ前後関係）。

使い方: python3 tools/preview_repro.py
出力: samples/preview25d/repro_<scene>.png
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from preview_iso import (ACTORS, FONT_PATH, NORM, SPRITES, TERRAIN, TILE,
                         draw_anchored, draw_shadow, draw_wall, load, tile,
                         wall_is_tall)

OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "samples", "preview25d")
WALL_H = TILE * 2


def font(size):
    try:
        return ImageFont.truetype(FONT_PATH, size)
    except (OSError, IOError):
        return ImageFont.load_default()


def render(layout, floor_tex_name, label):
    """レイアウトをゲーム忠実に描く。
    描画順はゲーム本体と同じ2パス（床先描き＋足元Yソート）。"""
    rows = len(layout)
    cols = len(layout[0])
    ox, oy = 96, 160
    W = ox * 2 + cols * TILE
    H = oy + rows * TILE + 80
    img = Image.new("RGBA", (W, H), (24, 26, 32, 255))
    d = ImageDraw.Draw(img)
    f = font(22)
    d.text((16, 16), label, font=f, fill=(255, 255, 255))
    d.text((16, 48), "北が上。壁の上面：北が壁か開けた床のときだけ描く",
           font=font(18), fill=(180, 180, 180))
    floor_tex = load(floor_tex_name)
    # (a) 床パス：壁以外の全タイルに床チップを描く
    for y in range(rows):
        for x in range(cols):
            ch = layout[y][x]
            if ch == " " or ch == "#":
                continue
            px, py = tile(x, y, ox, oy)
            chip = floor_tex.resize((TILE, TILE), Image.LANCZOS)
            img.paste(chip, (px - TILE // 2, py - TILE // 2), chip)
    # (b) オブジェクトパス：集めて足元Yでソートして描く
    entries = []
    seq = 0
    for y in range(rows):
        for x in range(cols):
            ch = layout[y][x]
            if ch == " ":
                continue
            px, py = tile(x, y, ox, oy)
            if ch == "#":
                entries.append((py + TILE // 2, 1, px, seq,
                                ("wall", px, py, wall_is_tall(layout, x, y))))
                seq += 1
            elif ch in TERRAIN:
                name, height = TERRAIN[ch]
                entries.append((py, 2, px, seq,
                                ("terrain", name, px, py, height)))
                seq += 1
            elif ch in ACTORS:
                name, height = ACTORS[ch]
                rank = 0 if height <= 40 else 3
                entries.append((py, rank, px, seq,
                                ("actor", name, px, py, height)))
                seq += 1
    entries.sort(key=lambda e: (e[0], e[1], e[2], e[3]))
    for _, _, _, _, draw in entries:
        kind = draw[0]
        if kind == "wall":
            _, px, py, tall = draw
            draw_wall(img, px, py, tall)
        elif kind == "terrain":
            _, name, px, py, height = draw
            draw_anchored(img, load(name), px, py, height)
        else:
            _, name, px, py, height = draw
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
    # 隙間の南の壁は通路判定（y-2が壁）で前面だけになり、隙間が床のまま見える。
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
    # 家具のすぐ南に壁：テーブル・本棚の南隣が壁。
    # 家具の南の壁は前面だけになり、家具が隠れない。
    # 開けた床の南の壁は上面（薄茶）が描かれる。
    "furniture_vs_wall": [
        "..........",
        "..T..B....",
        "..######..",
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
    # 1マス厚の横壁（にゅーいちのスクリーンショット相当。北も南も床）。
    # 壁の全長にわたって上面（薄茶）が赤レンガの上に描かれること。
    "thin_wall": [
        "############",
        "#..........#",
        "#..........#",
        "#..######..#",
        "#..........#",
        "#..P...c...#",
        "#..........#",
        "############",
    ],
    # 南北の通路：横壁の隙間の真下に壁がある。
    # 隙間の真下の壁は通路判定（東西が両方壁）で前面だけになり、
    # 隙間が床のまま見える（通り抜けられる見た目）。
    "ns_passage": [
        "##########",
        "#........#",
        "####.#####",
        "##########",
        "#...P....#",
        "##########",
    ],
    # 東西の通路：南北を壁に挟まれた1マス幅の床（テナントフロアの
    # 最下段の帯に相当）。南の壁は通路判定（y-2が壁）で前面だけになり、
    # 1マス幅の帯が床のまま見える。
    "ew_passage": [
        "##########",
        "#........#",
        "##########",
        "#...P....#",
        "##########",
    ],
    # 壁の北側に立つキャラ：主人公と客が横壁の北に立つ。
    # 壁は上面を描き、北側のキャラの下半身が壁に隠れるのが正しい結果
    # （壁が手前にあるため）。南側の店員は壁の手前にいて全身見える。
    "wall_north_char": [
        "............",
        "............",
        "...P....c...",
        "..########..",
        "............",
        "....s.......",
        "............",
    ],
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, layout in SCENES.items():
        img = render(layout, "floor_mall", name)
        out = os.path.join(OUT_DIR, "repro_%s.png" % name)
        img.convert("RGB").save(out)
        print("wrote", out)


if __name__ == "__main__":
    main()

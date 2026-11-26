#!/usr/bin/env python3
"""シレン風見下ろし描画のプレビュー画像を生成するスクリプト。

ゲーム本体（MallScreen.java）と同じ見下ろし座標系で四角いチップを並べ、
下層（明るいモール）と上層（霧とランタンの神秘）の2枚の PNG（1280x720）を
samples/preview25d/ に作る。ローカル確認用で、リポジトリにはコミットしない。
（ファイル名は旧2.5D時代の preview_iso.py のまま。）

使い方: python3 tools/preview_iso.py
"""
import os

from PIL import Image, ImageDraw, ImageFilter

TILE = 64
W, H = 1280, 720

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(BASE, "android", "assets", "sprites2d")
OUT_DIR = os.path.join(BASE, "samples", "preview25d")


def load(name):
    return Image.open(os.path.join(SPRITES, name + ".png")).convert("RGBA")


def tile(tx, ty, ox, oy):
    """タイル座標を画面座標（タイル中心）に変換する。PIL は y が下向き。"""
    return ox + tx * TILE, oy + ty * TILE


def draw_anchored(img, tex, cx, base_y, height):
    """スプライトを足元中心 (cx, base_y) に、高さ height で描く。"""
    w = max(1, int(height * tex.width / tex.height))
    spr = tex.resize((w, height), Image.LANCZOS)
    img.paste(spr, (int(cx - w / 2), int(base_y - height)), spr)


# タイル文字 → (地形スプライト名, 高さ)（壁 "#" は1マス＝1チップで描く）
TERRAIN = {
    "S": ("shelf", 80),
    "B": ("bookshelf", 80),
    "M": ("arcade", 88),
    "T": ("table", 56),
    "C": ("counter", 64),
    "G": ("stage", 56),
    "E": ("escalator", 72),
    "X": ("stairs", 72),
}
# タイル文字 → キャラクター／アイテム／小物の (スプライト名, 高さ)
# P は主人公の歩行フレームA、Q は歩行フレームB（2フレームの違いを見せるため）
ACTORS = {
    "P": ("hero_d0_f0", 64),
    "Q": ("hero_d0_f1", 64),
    "c": ("customer_0", 64),
    "s": ("clerk_0", 64),
    "g": ("guard_0", 64),
    "a": ("angel_0", 64),
    "l": ("lion", 64),
    "A": ("archangel", 90),
    "d": ("drink", 28),
    "o": ("onigiri", 28),
    "k": ("cake", 28),
    "L": ("lantern", 56),
    "p": ("plant", 56),
    "v": ("vending", 72),
    "b": ("bench", 56),
    "$": ("coins", 40),
}


def draw_floor(layout, floor_tex, ox, oy, img):
    """レイアウトをペインター法（行ごとに上から下へ）で描く。
    壁は1マス＝1チップ（上面と前面を1枚に含む壁チップをそのまま1枚描く）。"""
    rows = len(layout)
    cols = len(layout[0])
    wall = load("wall")
    for y in range(rows):
        for x in range(cols):
            ch = layout[y][x]
            if ch == " ":
                continue
            px, py = tile(x, y, ox, oy)
            # 床の四角いチップ
            chip = floor_tex.resize((TILE, TILE), Image.LANCZOS)
            img.paste(chip, (px - TILE // 2, py - TILE // 2), chip)
            # 壁は1チップで描く
            if ch == "#":
                wchip = wall.resize((TILE, TILE), Image.LANCZOS)
                img.paste(wchip, (px - TILE // 2, py - TILE // 2), wchip)
                continue
            # 地形（家具・階段など）
            if ch in TERRAIN:
                name, height = TERRAIN[ch]
                draw_anchored(img, load(name), px, py, height)
            # キャラクター／アイテム／小物
            if ch in ACTORS:
                name, height = ACTORS[ch]
                if height <= 40:
                    # アイテム・小物はタイル中心に置く
                    tex = load(name)
                    w = max(1, int(height * tex.width / tex.height))
                    spr = tex.resize((w, height), Image.LANCZOS)
                    img.paste(spr, (px - w // 2, py - height // 2), spr)
                else:
                    draw_anchored(img, load(name), px, py, height)


def add_glow(img, cx, cy, radius, color, alpha):
    """ランタンの光：柔らかい円形の光を重ねる。"""
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
              fill=color + (alpha,))
    glow = glow.filter(ImageFilter.GaussianBlur(radius // 2))
    img.alpha_composite(glow)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    # ---- 下層：明るいモール（テナントフロア）----
    lower = [
        "##############",
        "#............#",
        "#.B..C....T..#",
        "#............#",
        "#..P...c.....#",
        "#....s.Q..g..#",
        "#.T......d...#",
        "#..o...v..b..#",
        "#....E....X..#",
        "##############",
    ]
    img = Image.new("RGBA", (W, H), (24, 26, 32, 255))
    ox, oy = 224, 72
    draw_floor(lower, load("floor_mall"), ox, oy, img)
    # 明るいモール：ほんのり暖かい光
    warm = Image.new("RGBA", img.size, (255, 240, 200, 18))
    img.alpha_composite(warm)
    img.convert("RGB").save(os.path.join(OUT_DIR, "preview_lower.png"))
    print("wrote", os.path.join(OUT_DIR, "preview_lower.png"))

    # ---- 上層：霧とランタンの神秘（天界フロア）----
    upper = [
        "##############",
        "#............#",
        "#.L......L...#",
        "#............#",
        "#..P...a.....#",
        "#....l....A..#",
        "#.G......$...#",
        "#..L...p..L..#",
        "#............#",
        "##############",
    ]
    img = Image.new("RGBA", (W, H), (10, 10, 18, 255))
    draw_floor(upper, load("floor_carpet"), ox, oy, img)
    # 暗い神秘的なオーバーレイ
    dark = Image.new("RGBA", img.size, (8, 10, 30, 130))
    img.alpha_composite(dark)
    # ランタンの光（提灯のあるタイル位置に）
    for y, row in enumerate(upper):
        for x, ch in enumerate(row):
            if ch == "L":
                px, py = tile(x, y, ox, oy)
                add_glow(img, px, py - 30, 110, (255, 190, 90), 70)
    # 霧スプライトを薄く漂わせる
    fog = load("fog")
    for (fx, fy, fw) in [(300, 250, 420), (850, 380, 480), (620, 520, 380),
                         (150, 480, 360), (1000, 200, 400)]:
        fh = int(fw * fog.height / fog.width)
        spr = fog.resize((fw, fh), Image.LANCZOS)
        layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
        layer.paste(spr, (fx - fw // 2, fy - fh // 2), spr)
        # 霧はとても薄く
        alpha = layer.split()[3].point(lambda a: int(a * 0.10))
        layer.putalpha(alpha)
        img.alpha_composite(layer)
    img.convert("RGB").save(os.path.join(OUT_DIR, "preview_upper.png"))
    print("wrote", os.path.join(OUT_DIR, "preview_upper.png"))


if __name__ == "__main__":
    main()

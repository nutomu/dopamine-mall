#!/usr/bin/env python3
"""タイルセットを使ってダンジョン1フロア分の見た目を再現した
プレビュー画像（1280x720 PNG）を生成する。出力先は samples/preview/。

2枚作る：
  preview_lower.png  — 下層（明るい現代のショッピングモール）
  preview_upper.png  — 上層（霧とランタンの神秘：暗い通路、暖かい光、霧）
このスクリプトの出力はリポジトリにコミットしない（samples/ は gitignore 済み）。
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(ROOT, "android", "assets", "sprites")
OUT_DIR = os.path.join(ROOT, "samples", "preview")
TILE = 32
COLS, ROWS = 40, 22
W, H = COLS * TILE, ROWS * TILE + 16

_tiles = {}


def tile(name):
    if name not in _tiles:
        _tiles[name] = Image.open(os.path.join(SPRITES, name + ".png")).convert("RGBA")
    return _tiles[name]


def glow_layer(size, cx, cy, radius, color, max_alpha):
    """中心が明るい放射状の光のレイヤーを作る。"""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    steps = 24
    for i in range(steps, 0, -1):
        r = radius * i / steps
        a = int(max_alpha * (1 - i / steps) ** 1.5)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (a,))
    return layer.filter(ImageFilter.GaussianBlur(8))


def build_floor():
    """モックのフロアレイアウト（タイル名の2次元配列）を返す。"""
    grid = [["floor_a" for _ in range(COLS)] for _ in range(ROWS)]
    # 外周の壁
    for x in range(COLS):
        grid[0][x] = "wall"
        grid[ROWS - 1][x] = "wall"
    for y in range(ROWS):
        grid[y][0] = "wall"
        grid[y][COLS - 1] = "wall"
    # テナント区画（左上：書店風、棚）
    for y in range(2, 7):
        for x in range(2, 11):
            grid[y][x] = "floor_b"
    for x in range(3, 10, 3):
        grid[3][x] = "shelf"
        grid[5][x] = "shelf"
    grid[2][6] = "counter"
    # スーパー（右上：棚の列）
    for y in range(2, 7):
        for x in range(28, 38):
            grid[y][x] = "floor_c"
    for x in range(29, 37, 2):
        for y in (3, 5):
            grid[y][x] = "shelf"
    # ゲームセンター（左下：筐体）
    for y in range(13, 19):
        for x in range(2, 11):
            grid[y][x] = "floor_b"
    for x in range(3, 10, 2):
        grid[14][x] = "machine"
        grid[17][x] = "machine"
    # フードコート（右下：テーブル＋カウンター）
    for y in range(13, 19):
        for x in range(28, 38):
            grid[y][x] = "floor_a"
    for (x, y) in [(30, 14), (34, 14), (30, 17), (34, 17)]:
        grid[y][x] = "table"
    for x in range(29, 37):
        grid[13][x] = "counter"
    # イベントフロア（中央下：ステージ＋観客席）
    for y in range(15, 20):
        for x in range(14, 26):
            grid[y][x] = "floor_c"
    for x in range(16, 24):
        grid[15][x] = "stage"
    for x in range(15, 25, 2):
        grid[18][x] = "seat"
    # 通路の床バリエーション
    for y in range(8, 12):
        for x in range(2, 38):
            grid[y][x] = "floor_b" if (x + y) % 3 == 0 else "floor_a"
    # 階段
    grid[10][19] = "escalator"
    grid[10][20] = "escalator"
    grid[11][19] = "stairs"
    grid[11][20] = "stairs"
    return grid


def render_base(grid, actors):
    """タイルと登場人物を並べた基本画像を返す。"""
    img = Image.new("RGBA", (W, H), (10, 10, 14, 255))
    # 上部の HUD 風バー
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W, 16], fill=(0, 0, 0, 255))
    for y in range(ROWS):
        for x in range(COLS):
            img.paste(tile(grid[y][x]), (x * TILE, 16 + y * TILE), tile(grid[y][x]))
    for (name, tx, ty, scale) in actors:
        spr = tile(name)
        if scale != 1:
            spr = spr.resize((int(32 * scale), int(32 * scale)), Image.NEAREST)
        img.paste(spr, (tx * TILE, 16 + ty * TILE), spr)
    return img


def preview_lower():
    grid = build_floor()
    actors = [
        ("player", 19, 11, 1),
        ("enemy_customer", 8, 9, 1), ("enemy_clerk", 24, 9, 1),
        ("enemy_guard", 32, 9, 1), ("enemy_bear_mascot", 6, 16, 1),
        ("item_drink", 12, 10, 1), ("item_onigiri", 26, 10, 1),
        ("item_sweets", 15, 12, 1), ("item_umbrella", 35, 16, 1),
        ("item_frying_pan", 20, 18, 1), ("item_jacket", 22, 18, 1),
    ]
    img = render_base(grid, actors)
    # 明るく暖かいモールの光：全体を少し明るく＋暖色の薄いオーバーレイ
    img = ImageEnhance.Brightness(img.convert("RGB")).enhance(1.12).convert("RGBA")
    warm = Image.new("RGBA", img.size, (255, 225, 170, 18))
    img = Image.alpha_composite(img, warm)
    return img


def preview_upper():
    grid = build_floor()
    actors = [
        ("player", 19, 11, 1),
        ("enemy_angel", 8, 9, 1), ("enemy_lion_god", 30, 9, 1),
        ("enemy_yeti", 24, 16, 1), ("enemy_tsuchinoko", 6, 16, 1),
        ("boss_archangel", 33, 17, 1.5),
        ("item_sweets", 15, 12, 1), ("item_frying_pan", 20, 18, 1),
    ]
    img = render_base(grid, actors)
    # 暗い青のオーバーレイ（サンプルDの雰囲気）
    dark = Image.new("RGBA", img.size, (12, 20, 48, 130))
    img = Image.alpha_composite(img, dark)
    # ランタンの暖かい光（数か所）
    for (tx, ty, r, a) in [(6, 4, 150, 120), (33, 4, 150, 120),
                           (6, 16, 130, 110), (33, 16, 130, 110),
                           (19, 11, 190, 150)]:
        cx, cy = tx * TILE + 16, 16 + ty * TILE + 16
        img = Image.alpha_composite(img, glow_layer(img.size, cx, cy, r, (255, 180, 90), a))
    # 霧（白いぼかしを数か所）
    fog = Image.new("RGBA", img.size, (0, 0, 0, 0))
    fd = ImageDraw.Draw(fog)
    for (x, y, w, h, a) in [(0, 100, 500, 90, 26), (700, 260, 580, 100, 22),
                            (200, 480, 600, 90, 24), (850, 560, 430, 80, 20),
                            (0, 620, 700, 80, 22)]:
        fd.ellipse([x, y, x + w, y + h], fill=(190, 205, 230, a))
    fog = fog.filter(ImageFilter.GaussianBlur(24))
    img = Image.alpha_composite(img, fog)
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    lower = preview_lower()
    lower.save(os.path.join(OUT_DIR, "preview_lower.png"))
    print("wrote preview_lower.png", lower.size)
    upper = preview_upper()
    upper.save(os.path.join(OUT_DIR, "preview_upper.png"))
    print("wrote preview_upper.png", upper.size)


if __name__ == "__main__":
    main()

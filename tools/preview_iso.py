#!/usr/bin/env python3
"""シレン風見下ろし描画のプレビュー画像を生成するスクリプト。

ゲーム本体（MallScreen.java）と同じ見下ろし座標系で四角いチップを並べ、
下層（明るいモール）と上層（霧とランタンの神秘）の2枚の PNG（1280x720）を
samples/preview25d/ に作る。ローカル確認用で、リポジトリにはコミットしない。
（ファイル名は旧2.5D時代の preview_iso.py のまま。）

使い方: python3 tools/preview_iso.py
"""
import json
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

TILE = 64
WALL_H = TILE * 2
W, H = 1280, 720

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(BASE, "android", "assets", "sprites2d")
OUT_DIR = os.path.join(BASE, "samples", "preview25d")
# ゲームに同梱のサブセットフォントを優先（/tmp のフォントはVM置換で消える）。
FONT_PATH = os.path.join(BASE, "android", "assets", "fonts",
                         "NotoSansJP-subset.ttf")

NORM = {}
try:
    with open(os.path.join(SPRITES, "norm.json"), encoding="utf-8") as f:
        NORM = json.load(f)
except FileNotFoundError:
    pass


def load(name):
    return Image.open(os.path.join(SPRITES, name + ".png")).convert("RGBA")


def font(size):
    try:
        return ImageFont.truetype(FONT_PATH, size)
    except (OSError, IOError):
        return ImageFont.load_default()


def tile(tx, ty, ox, oy):
    """タイル座標を画面座標（タイル中心）に変換する。PIL は y が下向き。"""
    return ox + tx * TILE, oy + ty * TILE


def norm_scale(name):
    """方向ごとの表示サイズ正規化の倍率（ゲーム本体と同じ norm.json）。"""
    entry = NORM.get(name)
    return entry["scale"] if entry else 1.0


def draw_shadow(img, cx, base_y, width):
    """キャラクターの足元の柔らかい影（半透明の楕円）。"""
    h = int(width * 0.32)
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - width / 2, base_y - h / 2 + 2, cx + width / 2, base_y + h / 2 + 2],
              fill=(0, 0, 0, 100))
    layer = layer.filter(ImageFilter.GaussianBlur(3))
    img.alpha_composite(layer)


def draw_anchored(img, tex, cx, base_y, height, name=None):
    """スプライトを足元中心 (cx, base_y) に、高さ height で描く。
    キャラクターは norm.json の倍率を掛けて正規化する。"""
    if name is not None:
        height = int(height * norm_scale(name))
    w = max(1, int(height * tex.width / tex.height))
    spr = tex.resize((w, height), Image.LANCZOS)
    img.paste(spr, (int(cx - w / 2), int(base_y - height)), spr)


def draw_wall(img, px, py, tall=True):
    """背の高い壁：下端をタイルの下端に合わせ、上に1タイル分はみ出す。
    ゲーム本体（MallScreen.drawWall）と同じく、はみ出しは北（画面の上）へ。
    PIL は y が下向きなので、壁チップ（64x128）の貼り付け位置は
    (px-32, py-96)：自分のマスに前面（下半分・赤レンガ）、北のマスに
    上面（上半分・薄茶タイル）が来る。
    tall が偽のときは前面だけを自分のマスに描く（北のマスが壁でない場合）。"""
    wall = load("wall")
    if tall:
        spr = wall.resize((TILE, WALL_H), Image.LANCZOS)
        img.paste(spr, (px - TILE // 2, py - TILE // 2 - TILE), spr)
    else:
        front = wall.crop((0, wall.height // 2, wall.width, wall.height))
        spr = front.resize((TILE, TILE), Image.LANCZOS)
        img.paste(spr, (px - TILE // 2, py - TILE // 2), spr)


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
# 地形文字のうち歩行可能なもの（ゲームの Floor.Tile.isWalkable に対応）。
# 北がこれらのとき壁は前面だけを描き、階段・エスカレーターを見えるままにする。
# それ以外の地形文字は固体家具で、北の壁は上面も描く。
WALKABLE_TERRAIN = {"E", "X"}
# タイル文字 → キャラクター／アイテム／小物の (スプライト名, 高さ)
# P・Q・R・S は主人公の歩行フレーム0〜3（4フレームの違いを見せるため）
ACTORS = {
    "P": ("hero_d0_f0", 64),
    "Q": ("hero_d0_f1", 64),
    "R": ("hero_d0_f2", 64),
    "S": ("hero_d0_f3", 64),
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


def wall_is_tall(layout, x, y):
    """壁チップを背の高いもの（上面＋前面）で描くかどうか。
    ゲーム本体（MallScreen.drawWall）と同じ条件。
    - 北が壁（y==0 の北端を含む）→ 背の高いチップ。
    - 北が固体家具（S/B/M/T/C/G）→ 背の高いチップ。壁は家具の手前
      （南）にあるため、上面が家具の下部を覆うのは正しい隠れ方
      （2026-11-28、にゅーいちの指摘）。逆に家具の真北の壁が上面を
      描かないと、その部分だけ床が見えて壁が紙のように薄く見える。
    - 北が階段・エスカレーター（E/X、歩行可能）→ 前面だけ
      （階段を見えるままにする）。
    - 北が開けた床 → 背の高いチップ。
    - 北が通路（出入口の隙間や1マス幅の通路）→ 前面だけ（出入口を隠さない）。
    通路とは、北の床が「東西を壁に挟まれた南北の通路」か
    「南北を壁に挟まれた東西の通路」のこと。
    アクター文字はゲームでは床タイルの上に立つものとして扱う
    （壁の北側に立つキャラの下半身が壁に隠れる、というゲームと同じ
    前後関係をプレビューでも再現するため。地形文字と重なる "S" は
    draw_floor と同じく地形＝本棚として扱う）。
    階段・エスカレーターは歩行可能な地形文字で、前面だけにする。"""
    if y == 0:
        return True
    north = layout[y - 1][x]
    if north == "#":
        return True
    if north in TERRAIN:
        # 固体家具は上面を描く。階段・エスカレーター（歩行可能）は前面だけ。
        return north not in WALKABLE_TERRAIN
    if north != "." and north not in ACTORS:
        return False
    # 北は床（"." か、床の上に立つアクターの足元）
    east_west_walls = (x > 0 and x < len(layout[0]) - 1
                       and layout[y - 1][x - 1] == "#"
                       and layout[y - 1][x + 1] == "#")
    north_south_walls = y > 1 and layout[y - 2][x] == "#"
    return not (east_west_walls or north_south_walls)


def draw_floor(layout, floor_tex, ox, oy, img):
    """レイアウトを3/4見下ろし描画（床先描き＋足元Yソート）で描く。
    ゲーム本体（MallScreen.java）と同じ2パス構成。
    PIL は y が下向きなので、北（奥）ほど画面Yが小さい。ソートは
    足元の画面Yの昇順（北から先に描く）、同値は種別ランクの昇順
    （アイテム0→壁1→地形・小物2→キャラ3）、次に画面Xの昇順。
    壁の足元はタイル南縁（画面Yは py+32）、他はタイル中心 py。
    キャラクターは正規化したサイズで描き、足元に影を付ける。"""
    rows = len(layout)
    cols = len(layout[0])
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
    # エントリ: (足元画面Y, 種別ランク, 画面X, 通し番号, 描画内容)
    entries = []
    seq = 0
    for y in range(rows):
        for x in range(cols):
            ch = layout[y][x]
            if ch == " ":
                continue
            px, py = tile(x, y, ox, oy)
            if ch == "#":
                # 壁は背の高いチップで描く（床チップは敷かない。ゲーム本体と同じ）
                # 上面を描くかどうかは wall_is_tall（ゲーム本体の drawWall と同じ条件）
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
                rank = 0 if height <= 40 else 3  # アイテムは0、キャラは3
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
                # アイテム・小物はタイル中心に置く
                tex = load(name)
                w = max(1, int(height * tex.width / tex.height))
                spr = tex.resize((w, height), Image.LANCZOS)
                img.paste(spr, (px - w // 2, py - height // 2), spr)
            else:
                draw_shadow(img, px, py, 40)
                draw_anchored(img, load(name), px, py, height, name)


def add_glow(img, cx, cy, radius, color, alpha):
    """ランタンの光：柔らかい円形の光を重ねる。"""
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
              fill=color + (alpha,))
    glow = glow.filter(ImageFilter.GaussianBlur(radius // 2))
    img.alpha_composite(glow)


def draw_debug_preview():
    """スプライト確認デバッグモードの画面のプレビュー（主人公）。
    ゲーム本体の drawSpriteDebug と同じ配置で、8方向×停止／歩行／武器使用の
    3段を表示する。各セルの下に実寸（幅×高さ）とフレーム数を出す。"""
    img = Image.new("RGBA", (W, H), (5, 5, 13, 255))
    d = ImageDraw.Draw(img)
    f_title = font(28)
    f_text = font(20)
    f_small = font(16)
    cx = W // 2
    d.text((cx, 40), "スプライト確認", font=f_title, fill=(255, 220, 60), anchor="ma")
    d.text((cx, 80), "主人公  (1 / 15)", font=f_text, fill=(255, 255, 255), anchor="ma")
    scales = "倍率： " + " ".join("%.2f" % norm_scale("hero_d%d_f0" % dd) for dd in range(8))
    d.text((cx, 108), scales, font=f_small, fill=(180, 180, 180), anchor="ma")
    dirs = ["南", "南西", "西", "北西", "北", "北東", "東", "南東"]
    # 停止の行は静止フレーム（idle）、歩行・武器使用の行は歩行フレームを使う
    rows = [("停止", "hero_d%d_idle"), ("歩行", "hero_d%d_f1"), ("武器使用", "hero_d%d_f2")]
    col_w = W // 8
    row_feet = [300, 440, 580]
    row_label_y = [200, 340, 480]
    slash = load("slash")
    hitstar = load("hitstar")
    for dd in range(8):
        col_cx = int(col_w * (dd + 0.5))
        d.text((col_cx, 140), dirs[dd], font=f_text, fill=(255, 255, 255), anchor="ma")
    for row, (label, pattern) in enumerate(rows):
        d.text((16, row_label_y[row]), label, font=f_text, fill=(80, 200, 255))
        for dd in range(8):
            name = pattern % dd
            col_cx = int(col_w * (dd + 0.5))
            feet_y = row_feet[row]
            draw_x, draw_y = col_cx, feet_y
            if row == 2:
                # 武器使用：攻撃方向へ突進（南＝下、北＝上など）
                dxy = [(0, 1), (-1, 1), (-1, 0), (-1, -1),
                       (0, -1), (1, -1), (1, 0), (1, 1)][dd]
                draw_x += int(dxy[0] * 14)
                draw_y += int(dxy[1] * 14)
            draw_shadow(img, draw_x, feet_y, 44)
            draw_anchored(img, load(name), draw_x, draw_y, 72, name)
            if row == 2:
                dxy = [(0, 1), (-1, 1), (-1, 0), (-1, -1),
                       (0, -1), (1, -1), (1, 0), (1, 1)][dd]
                tx = col_cx + dxy[0] * 40
                ty = feet_y + dxy[1] * 40
                spr = slash.resize((64, 64), Image.LANCZOS)
                img.paste(spr, (tx - 32, ty - 32), spr)
                star = hitstar.resize((48, 48), Image.LANCZOS)
                img.paste(star, (tx - 24, ty - 24), star)
            entry = NORM.get(name, {})
            size = "%dx%d  4フレーム" % (entry.get("w", 0), entry.get("h", 0))
            d.text((col_cx, feet_y + 8), size, font=f_small,
                   fill=(180, 180, 180), anchor="ma")
    d.text((cx, 660), "左右キー：キャラ切替  B/Back：タイトルへ", font=f_text,
           fill=(255, 255, 255), anchor="ma")
    img.convert("RGB").save(os.path.join(OUT_DIR, "debug_sprites.png"))
    print("wrote", os.path.join(OUT_DIR, "debug_sprites.png"))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    draw_debug_preview()

    # ---- 下層：明るいモール（テナントフロア）----
    lower = [
        "##############",
        "#............#",
        "#.B..C....T..#",
        "#............#",
        "#..P...c.....#",
        "#....s.Q..g..#",
        "#.T...R..d...#",
        "#..o...v..bS.#",
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

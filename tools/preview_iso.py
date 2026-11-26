#!/usr/bin/env python3
"""2.5Dアイソメトリック描画のプレビュー画像を生成するスクリプト。

ゲーム本体（MallScreen.java）と同じアイソメトリック投影でタイルを並べ、
下層（明るいモール）と上層（霧とランタンの神秘）の2枚の PNG（1280x720）を
samples/preview25d/ に作る。ローカル確認用で、リポジトリにはコミットしない。

使い方: python3 tools/preview_iso.py
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

ISO_TW = 64
ISO_TH = 40
W, H = 1280, 720
# 壁の高さ（タイル中心から壁上面ダイヤまでの垂直距離。旧ピラー88px＝WALL_H＋40 と同じ）
WALL_H = 48
# アイソメ上面の1辺の長さ sqrt(32^2+20^2)
EDGE = 37.7
# バンドスプライト（480x376）の上面ベクトルは (480, 189)、左辺 183px、
# 上面と底面の垂直距離は 170px。上面の傾きは atan(189/480)=21.5°。
BAND_TOP_DX = 480.0
BAND_TOP_DY = 189.0
BAND_SIDE = 183.0
BAND_THICK = 170.0
# バンドスプライトの上面はピクセルアートの階段状で、理想の32°線より
# 最大8pxほど下にずれる箇所がある。その分だけ帯を上にずらし、
# 階段が理想線をまたぐようにする（はみ出しは上面ダイヤが隠す）。
BAND_TOP_LIFT = 4
# 壁面の垂直厚み（WALL_H を32°方向に垂直な距離に直したもの）
FACE_THICK = WALL_H * math.cos(math.radians(32.0))

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(BASE, "android", "assets", "sprites2d")
OUT_DIR = os.path.join(BASE, "samples", "preview25d")


def load(name):
    return Image.open(os.path.join(SPRITES, name + ".png")).convert("RGBA")


def iso(tx, ty, ox, oy):
    """タイル座標を画面座標（タイル中心）に変換する。"""
    return ox + (tx - ty) * ISO_TW // 2, oy + (tx + ty) * ISO_TH // 2


def draw_anchored(img, tex, cx, base_y, height):
    """スプライトを足元中心 (cx, base_y) に、高さ height で描く。"""
    w = max(1, int(height * tex.width / tex.height))
    spr = tex.resize((w, height), Image.LANCZOS)
    img.paste(spr, (int(cx - w / 2), int(base_y - height)), spr)


def band_params(seg_tiles):
    """バンド区画（seg_tiles タイル分）の (sx, sy, theta_deg) を解く。
    theta は画面上で反時計回り（SE面＝上面が右上がり）。上面の長さ・厚み・
    32°の角度をすべて満たす厳密解。1〜2タイルの区画は厳密解がないため、
    角度と長さだけを合わせた緩和解（theta=0、面が少し薄くなる）を返す。"""
    edge = seg_tiles * EDGE
    if seg_tiles >= 3:
        v = 84.06 / edge
        theta = 32.0 - math.degrees(math.asin(max(-1.0, min(1.0, v)))) / 2.0
        sy = FACE_THICK / (BAND_SIDE * math.cos(math.radians(32.0 - theta)))
        sx = edge * math.cos(math.radians(32.0 - theta)) / BAND_TOP_DX
    else:
        theta = 0.0
        sx = edge / 566.0
        sy = 300.0 * sx / BAND_TOP_DY
    return sx, sy, theta


def draw_band(img, band, start, seg_tiles, ascending):
    """壁の前面バンドを1区画描く。
    start: 上面の始点 (x, y)（PIL座標）。ascending=True なら上面が右上がり
    （SE面）、False なら右下がり（SW面）。上面の始点が start に来るよう配置する。"""
    sx, sy, theta = band_params(seg_tiles)
    spr = band
    if not ascending:
        spr = spr.transpose(Image.FLIP_LEFT_RIGHT)
    w0 = max(1, round(spr.width * sx))
    h0 = max(1, round(spr.height * sy))
    spr = spr.resize((w0, h0), Image.LANCZOS)
    # 上面の始点（アンカー）：SE面は画像の (0, 189*sy)、SW面は反転後の (0, 0)。
    ax = 0.0
    ay = (189.0 * sy) if ascending else 0.0
    rot = theta if ascending else -theta
    spr = spr.rotate(rot, expand=True, resample=Image.BICUBIC)
    # 回転中心はリサイズ後画像の中心。アンカーの回転後の位置を求める。
    cx, cy = w0 / 2.0, h0 / 2.0
    rx, ry = ax - cx, ay - cy
    th = math.radians(rot)
    # PIL の rotate は画面上で反時計回り（y 下向き座標ではこの式になる）
    rx2 = rx * math.cos(th) + ry * math.sin(th)
    ry2 = -rx * math.sin(th) + ry * math.cos(th)
    ax2 = rx2 + spr.width / 2.0
    ay2 = ry2 + spr.height / 2.0
    # 上面の階段が理想線をまたぐよう、帯全体を少し上にずらす。
    img.paste(spr, (int(round(start[0] - ax2)),
                    int(round(start[1] - ay2 - BAND_TOP_LIFT))), spr)


def draw_corner(img, corner, px, py):
    """南側の角タイルに角スプライトを描く。上面ダイヤの南頂点に角の上面の
    V 字の谷（画像座標 (240,110)）が来るよう配置する。"""
    w, h = 64, 72
    spr = corner.resize((w, h), Image.LANCZOS)
    # V 字の谷の位置（リサイズ後）：(32, 110*h/369)
    vx, vy = 32.0, 110.0 * h / 369.0
    tx, ty = px, py - WALL_H + 20
    img.paste(spr, (int(round(tx - vx)), int(round(ty - vy))), spr)


def analyze_walls(layout):
    """壁タイルの情報を集める。rows/cols、角タイルの集合、SE/SW方向の
    バンドの走行（run）を返す。走行は最後のタイルをキーにする。
    （バンドは走行の最後のタイルで描く。走行中の壁タイルの床ダイヤは
    バンドより先に描かれるため、バンドがそれを隠す。）
    se_runs: (x, y1) -> 長さ（走行は y1-n+1 .. y1）
    sw_runs: (x1, y) -> 長さ（走行は x1-n+1 .. x1）"""
    rows = len(layout)
    cols = len(layout[0])

    def is_wall(x, y):
        return 0 <= x < cols and 0 <= y < rows and layout[y][x] == "#"

    corners = set()
    se_runs = {}
    sw_runs = {}
    for y in range(rows):
        for x in range(cols):
            if not is_wall(x, y):
                continue
            se = not is_wall(x + 1, y)
            sw = not is_wall(x, y + 1)
            if se and sw:
                corners.add((x, y))
    for y in range(rows):
        for x in range(cols):
            if not is_wall(x, y) or (x, y) in corners:
                continue
            # SE面の走行の開始：1つ上のタイルが壁でない／角／南東面が露出していない場合
            if not is_wall(x + 1, y) and (
                    not is_wall(x, y - 1) or (x, y - 1) in corners
                    or is_wall(x + 1, y - 1)):
                n = 0
                while is_wall(x, y + n) and (x, y + n) not in corners \
                        and not is_wall(x + 1, y + n):
                    n += 1
                if n > 0:
                    se_runs[(x, y + n - 1)] = n
            # SW面の走行の開始：1つ左のタイルが壁でない／角／南西面が露出していない場合
            if not is_wall(x, y + 1) and (
                    not is_wall(x - 1, y) or (x - 1, y) in corners
                    or is_wall(x - 1, y + 1)):
                n = 0
                while is_wall(x + n, y) and (x + n, y) not in corners \
                        and not is_wall(x + n, y + 1):
                    n += 1
                if n > 0:
                    sw_runs[(x + n - 1, y)] = n
    return corners, se_runs, sw_runs


def draw_wall(img, layout, x, y, px, py, corners, se_runs, sw_runs,
              wall_top, band, corner, ox, oy):
    """壁タイル1枚を描く：上面ダイヤ＋（走行の開始タイルなら）前面バンド、
    または角タイルなら角スプライト。"""
    # 上面ダイヤ（継ぎ目なく並ぶ）
    top = wall_top.resize((ISO_TW, ISO_TH), Image.LANCZOS)
    img.paste(top, (px - ISO_TW // 2, py - WALL_H - ISO_TH // 2), top)
    if (x, y) in corners:
        draw_corner(img, corner, px, py)
        return
    # SE面の走行（x 一定、y 方向）：上面は S_top(x,y0) から E_top(x,y1) へ
    # 右上がり。バンドのアンカーは上面の左下端＝区画の最後のタイルの E_top。
    if (x, y) in se_runs:
        n = se_runs[(x, y)]
        # 3タイル区画に分割。余りが1なら最後の2区画を2+2にする（1タイル区画を避ける）
        segs = []
        rest = n
        while rest >= 3:
            segs.append(3)
            rest -= 3
        if rest == 1 and segs:
            segs[-1] = 2
            segs.append(2)
        elif rest > 0:
            segs.append(rest)
        # 区画は走行の末尾（y が大きい方）から配置する（走行は最後のタイルで描く）
        end_y = y
        for seg in reversed(segs):
            sx0, sy0 = iso(x, end_y, ox, oy)
            start = (sx0 + ISO_TW // 2, sy0 - WALL_H)
            draw_band(img, band, start, seg, True)
            end_y -= seg
    # SW面の走行（y 一定、x 方向）：上面は W_top(x0,y) から S_top(x1,y) へ
    if (x, y) in sw_runs:
        n = sw_runs[(x, y)]
        segs = []
        rest = n
        while rest >= 3:
            segs.append(3)
            rest -= 3
        if rest == 1 and segs:
            segs[-1] = 2
            segs.append(2)
        elif rest > 0:
            segs.append(rest)
        cx = x - n + 1
        for seg in segs:
            sx0, sy0 = iso(cx, y, ox, oy)
            start = (sx0 - ISO_TW // 2, sy0 - WALL_H)
            draw_band(img, band, start, seg, False)
            cx += seg


# タイル文字 → (地形スプライト名, 高さ)（壁 "#" は専用の連結描画をする）
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
ACTORS = {
    "P": ("hero_d0_f0", 64),
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
    """レイアウトをペインター法（x+y の昇順）で描く。"""
    rows = len(layout)
    cols = len(layout[0])
    corners, se_runs, sw_runs = analyze_walls(layout)
    wall_top = load("wall_top")
    band = load("wall_band")
    corner = load("wall_corner")
    for s in range(cols + rows - 1):
        for x in range(max(0, s - (rows - 1)), min(cols - 1, s) + 1):
            y = s - x
            ch = layout[y][x]
            if ch == " ":
                continue
            px, py = iso(x, y, ox, oy)
            # 床のひし形タイル
            diamond = floor_tex.resize((ISO_TW, ISO_TH), Image.LANCZOS)
            img.paste(diamond, (px - ISO_TW // 2, py - ISO_TH // 2), diamond)
            # 壁は連結描画（上面ダイヤ＋前面バンド／角）
            if ch == "#":
                draw_wall(img, layout, x, y, px, py, corners, se_runs, sw_runs,
                          wall_top, band, corner, ox, oy)
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
        "#....s....g..#",
        "#.T......d...#",
        "#..o...v..b..#",
        "#....E....X..#",
        "##############",
    ]
    img = Image.new("RGBA", (W, H), (24, 26, 32, 255))
    ox, oy = 640, 130
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
                px, py = iso(x, y, ox, oy)
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

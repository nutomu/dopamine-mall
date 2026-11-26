#!/usr/bin/env python3
"""ドパミン・モールのドット絵タイルセットをプログラム生成する。

32x32 ピクセルのドット絵スプライトを Pillow で描画し、PNG（透過あり）で
android/assets/sprites/ に出力する。出力先はこのスクリプトの場所から
リポジトリルートを辿って決まる。

タイル一覧：
  地形: floor_a / floor_b / floor_c / wall / shelf / machine / table /
        counter / stage / seat / escalator / stairs
  プレイヤー: player
  敵: enemy_customer / enemy_clerk / enemy_guard / enemy_bear_mascot /
      enemy_rabbit_mascot / enemy_dog / enemy_monkey / enemy_tsuchinoko /
      enemy_yeti / enemy_angel / enemy_lion_god /
      boss_manager / boss_giant_mascot / boss_archangel
  アイテム: item_drink / item_onigiri / item_sweets / item_umbrella /
            item_frying_pan / item_apron / item_jacket

敵・アイテムの色は既存の色分け（Enemy.Kind / Item.Kind の r/g/b）を
引き継いでいる。ドット絵らしく、輪郭・陰影・ハイライトを入れる。
"""
import os
import sys

from PIL import Image, ImageDraw

SIZE = 32
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "android", "assets", "sprites")


def canvas():
    """透過背景の 32x32 キャンバスを返す。"""
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def box(d, x0, y0, x1, y1, color):
    """(x0, y0) - (x1, y1) の矩形を描く（両端を含む）。"""
    d.rectangle([x0, y0, x1, y1], fill=color)


def outline(d, x0, y0, x1, y1, color):
    d.rectangle([x0, y0, x1, y1], outline=color)


def px(d, x, y, color):
    d.point((x, y), fill=color)


def dark(color, factor=0.6):
    return tuple(int(c * factor) for c in color[:3]) + (color[3] if len(color) > 3 else 255,)


def light(color, factor=1.3):
    return tuple(min(255, int(c * factor)) for c in color[:3]) + (color[3] if len(color) > 3 else 255,)


# ---------------------------------------------------------------------------
# 地形タイル
# ---------------------------------------------------------------------------
def tile_floor(base, grout, speck):
    """モールのタイル床。2x2 の小タイル＋目地＋斑点。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, base)
    # 16x16 の小タイルを4枚、わずかに色を変える
    box(d, 1, 1, 15, 15, light(base, 1.06))
    box(d, 16, 16, 30, 30, light(base, 1.06))
    box(d, 1, 16, 15, 30, dark(base, 0.94))
    box(d, 16, 1, 30, 15, dark(base, 0.94))
    # 目地
    box(d, 0, 15, 31, 16, grout)
    box(d, 15, 0, 16, 31, grout)
    outline(d, 0, 0, 31, 31, grout)
    # 斑点（タイルの質感）
    for (x, y) in [(5, 6), (10, 12), (21, 8), (26, 24), (8, 26), (24, 20)]:
        px(d, x, y, speck)
    return img


def tile_wall():
    """レンガの壁。暗めの紫茶色。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (84, 66, 92, 255))
    # レンガ（2段ずらし）
    for row in range(4):
        y0 = row * 8
        off = 8 if row % 2 else 0
        for x0 in range(-8 + off, 32, 16):
            box(d, x0 + 1, y0 + 1, x0 + 14, y0 + 6, (110, 88, 108, 255))
            box(d, x0 + 1, y0 + 1, x0 + 14, y0 + 2, (132, 108, 128, 255))
    # 上辺のハイライト
    box(d, 0, 0, 31, 1, (150, 128, 148, 255))
    outline(d, 0, 0, 31, 31, (52, 40, 58, 255))
    return img


def tile_shelf():
    """商品棚。木枠＋2段の棚＋色とりどりの商品。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (96, 64, 32, 255))
    box(d, 2, 2, 29, 29, (150, 104, 52, 255))
    goods = [(200, 60, 60), (60, 160, 90), (60, 110, 200), (220, 180, 60),
             (170, 90, 200), (70, 190, 200)]
    for i, c in enumerate(goods):
        x = 4 + (i % 3) * 9
        y = 5 if i < 3 else 18
        box(d, x, y, x + 6, y + 7, c + (255,))
        box(d, x, y, x + 6, y + 1, light(c + (255,), 1.3))
    box(d, 2, 14, 29, 16, (96, 64, 32, 255))
    box(d, 2, 27, 29, 29, (96, 64, 32, 255))
    outline(d, 0, 0, 31, 31, (60, 38, 18, 255))
    return img


def tile_machine():
    """ゲーム筐体。暗い本体＋明るい画面＋ボタン。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (40, 30, 60, 255))
    box(d, 6, 2, 25, 29, (70, 50, 95, 255))
    box(d, 9, 5, 22, 15, (40, 200, 220, 255))
    # 画面の中のドット絵っぽい模様
    box(d, 11, 7, 14, 10, (255, 220, 60, 255))
    box(d, 17, 10, 20, 13, (255, 90, 140, 255))
    box(d, 9, 18, 22, 20, (140, 240, 120, 255))
    box(d, 11, 23, 14, 26, (255, 80, 80, 255))
    box(d, 18, 23, 21, 26, (90, 140, 255, 255))
    outline(d, 6, 2, 25, 29, (20, 14, 30, 255))
    return img


def tile_table():
    """フードコートのテーブル。木の天板＋脚。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (0, 0, 0, 0))
    box(d, 3, 8, 28, 20, (190, 130, 60, 255))
    box(d, 3, 8, 28, 10, (215, 155, 80, 255))
    box(d, 6, 20, 9, 29, (120, 80, 35, 255))
    box(d, 22, 20, 25, 29, (120, 80, 35, 255))
    # 天板の上のトレイとドリンク
    box(d, 9, 5, 15, 7, (230, 230, 230, 255))
    box(d, 19, 4, 22, 7, (255, 120, 120, 255))
    outline(d, 3, 8, 28, 20, (90, 58, 22, 255))
    return img


def tile_counter():
    """カウンター店舗。緑系の前面＋天板。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (30, 110, 95, 255))
    box(d, 2, 4, 29, 10, (200, 220, 210, 255))
    box(d, 2, 4, 29, 5, (235, 245, 240, 255))
    box(d, 5, 14, 12, 26, (45, 140, 120, 255))
    box(d, 18, 14, 25, 26, (45, 140, 120, 255))
    box(d, 5, 14, 12, 16, (70, 170, 145, 255))
    box(d, 18, 14, 25, 16, (70, 170, 145, 255))
    outline(d, 0, 0, 31, 31, (16, 60, 52, 255))
    return img


def tile_stage():
    """イベントステージ。金色の舞台＋スポットライト。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (120, 90, 25, 255))
    box(d, 2, 10, 29, 29, (205, 165, 50, 255))
    box(d, 2, 10, 29, 13, (235, 200, 80, 255))
    box(d, 6, 2, 10, 8, (255, 245, 180, 255))
    box(d, 21, 2, 25, 8, (255, 245, 180, 255))
    box(d, 13, 18, 18, 24, (255, 240, 160, 255))
    outline(d, 0, 0, 31, 31, (70, 50, 12, 255))
    return img


def tile_seat():
    """観客席の椅子。青い座面＋背もたれ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (0, 0, 0, 0))
    box(d, 7, 4, 24, 16, (70, 95, 150, 255))
    box(d, 7, 4, 24, 7, (95, 125, 180, 255))
    box(d, 5, 16, 26, 22, (85, 115, 170, 255))
    box(d, 8, 22, 11, 29, (50, 65, 100, 255))
    box(d, 20, 22, 23, 29, (50, 65, 100, 255))
    outline(d, 7, 4, 24, 16, (35, 48, 75, 255))
    outline(d, 5, 16, 26, 22, (35, 48, 75, 255))
    return img


def tile_escalator():
    """エスカレーター。緑のステップ（斜めの縞）＋手すり。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (20, 120, 60, 255))
    for i in range(5):
        y = 4 + i * 5
        box(d, 4, y, 27, y + 2, (70, 220, 110, 255))
        box(d, 4, y, 10, y + 2, (140, 255, 180, 255))
    box(d, 0, 2, 3, 29, (180, 190, 190, 255))
    box(d, 28, 2, 31, 29, (180, 190, 190, 255))
    outline(d, 0, 0, 31, 31, (10, 70, 35, 255))
    return img


def tile_stairs():
    """非常階段。青い段差（横縞）。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    box(d, 0, 0, 31, 31, (30, 70, 160, 255))
    for i in range(4):
        y = 4 + i * 6
        box(d, 3, y, 28, y + 3, (80, 140, 235, 255))
        box(d, 3, y, 28, y, (140, 185, 255, 255))
    outline(d, 0, 0, 31, 31, (15, 35, 90, 255))
    return img


# ---------------------------------------------------------------------------
# キャラクター描画の共通ヘルパー
# ---------------------------------------------------------------------------
def body_base(d, body, dark_c, light_c):
    """丸みのある胴体（角を落とした矩形）＋陰影。"""
    box(d, 8, 14, 23, 28, body)
    box(d, 8, 14, 23, 16, light_c)
    box(d, 8, 26, 23, 28, dark_c)
    box(d, 8, 14, 9, 28, dark_c)
    box(d, 22, 14, 23, 28, dark_c)


def face(d, skin=(255, 205, 150, 255)):
    """顔（10x8）＋目。"""
    box(d, 11, 6, 20, 13, skin)
    box(d, 11, 6, 20, 7, light(skin, 1.15))
    box(d, 13, 9, 14, 10, (30, 20, 20, 255))
    box(d, 17, 9, 18, 10, (30, 20, 20, 255))
    box(d, 14, 12, 17, 12, (180, 90, 70, 255))


def shadow(d):
    """足元の影（楕円）。"""
    d.ellipse([8, 29, 23, 31], fill=(0, 0, 0, 90))


def outline_sprite(img, color=(20, 16, 20, 255)):
    """スプライト全体の輪郭を、透明ピクセルに接する不透明ピクセルに描く。"""
    pxs = img.load()
    w, h = img.size
    marked = []
    for y in range(h):
        for x in range(w):
            if pxs[x, y][3] == 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and pxs[nx, ny][3] == 0:
                    marked.append((x, y))
                    break
    d = ImageDraw.Draw(img)
    for (x, y) in marked:
        # 輪郭は元の色を少し暗くする（上書きしない）
        r, g, b, a = pxs[x, y]
        d.point((x, y), fill=(max(0, r - 70), max(0, g - 70), max(0, b - 70), a))
    return img


# ---------------------------------------------------------------------------
# プレイヤーと敵
# ---------------------------------------------------------------------------
def sprite_player():
    """主人公：緑のフード＋買い物カゴを持つドパガキ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # カゴ（左手）
    box(d, 3, 18, 9, 24, (150, 100, 45, 255))
    box(d, 4, 19, 8, 20, (200, 140, 60, 255))
    box(d, 4, 21, 6, 23, (230, 60, 60, 255))
    box(d, 7, 22, 8, 23, (90, 200, 90, 255))
    # 胴体（緑のフード付きパーカー）
    box(d, 10, 14, 22, 28, (70, 160, 80, 255))
    box(d, 10, 14, 22, 16, (100, 200, 110, 255))
    box(d, 10, 26, 22, 28, (45, 110, 55, 255))
    # 顔＋フード
    box(d, 11, 4, 21, 13, (50, 130, 65, 255))
    box(d, 12, 6, 20, 13, (255, 205, 150, 255))
    box(d, 13, 9, 14, 10, (30, 20, 20, 255))
    box(d, 18, 9, 19, 10, (30, 20, 20, 255))
    box(d, 14, 12, 18, 12, (180, 90, 70, 255))
    # 足
    box(d, 11, 28, 14, 30, (70, 50, 40, 255))
    box(d, 18, 28, 21, 30, (70, 50, 40, 255))
    return outline_sprite(img)


def sprite_customer():
    """客：ピンクの服＋買い物袋。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_base(d, (255, 140, 190, 255), (200, 90, 130, 255), (255, 180, 210, 255))
    # 買い物袋
    box(d, 24, 19, 28, 25, (255, 255, 255, 255))
    box(d, 25, 17, 27, 19, (200, 200, 200, 255))
    # 髪
    box(d, 10, 3, 21, 6, (90, 60, 40, 255))
    face(d)
    box(d, 11, 15, 14, 28, (200, 90, 130, 255))
    return outline_sprite(img)


def sprite_clerk():
    """店員：オレンジのエプロン＋キャップ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_base(d, (255, 140, 50, 255), (200, 95, 25, 255), (255, 175, 90, 255))
    # エプロンの紐とポケット
    box(d, 14, 15, 18, 22, (255, 240, 220, 255))
    box(d, 13, 23, 19, 25, (220, 200, 180, 255))
    # キャップ
    box(d, 10, 3, 21, 6, (230, 90, 30, 255))
    box(d, 10, 6, 22, 7, (200, 70, 20, 255))
    face(d)
    return outline_sprite(img)


def sprite_guard():
    """警備員：紺の制服＋帽子＋バッジ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_base(d, (50, 60, 110, 255), (30, 38, 70, 255), (80, 95, 150, 255))
    # バッジとボタン
    box(d, 18, 18, 20, 20, (255, 215, 80, 255))
    box(d, 15, 15, 16, 26, (220, 220, 230, 255))
    # 帽子
    box(d, 10, 2, 21, 6, (35, 45, 85, 255))
    box(d, 9, 6, 22, 7, (25, 32, 60, 255))
    box(d, 14, 3, 17, 4, (255, 215, 80, 255))
    face(d, skin=(235, 190, 140, 255))
    return outline_sprite(img)


def sprite_mascot(body, ear, belly, name_c):
    """着ぐるみ共通：丸い頭＋耳＋胴体。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # 耳
    box(d, 9, 1, 12, 6, ear)
    box(d, 19, 1, 22, 6, ear)
    box(d, 10, 3, 11, 5, belly)
    box(d, 20, 3, 21, 5, belly)
    # 頭
    box(d, 9, 4, 22, 15, body)
    box(d, 9, 4, 22, 6, light(body, 1.2))
    # 顔
    box(d, 12, 8, 19, 13, belly)
    box(d, 13, 9, 14, 10, (30, 20, 20, 255))
    box(d, 17, 9, 18, 10, (30, 20, 20, 255))
    box(d, 14, 11, 17, 12, (60, 40, 30, 255))
    # 胴体
    body_base(d, body, dark(body, 0.7), light(body, 1.15))
    box(d, 13, 17, 18, 25, belly)
    return outline_sprite(img)


def sprite_dog():
    """逃げ出した犬：黄土色＋垂れ耳＋鼻。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_c = (205, 165, 90, 255)
    # 垂れ耳
    box(d, 8, 4, 11, 12, (150, 110, 55, 255))
    box(d, 20, 4, 23, 12, (150, 110, 55, 255))
    # 頭＋鼻
    box(d, 10, 4, 21, 14, body_c)
    box(d, 12, 10, 19, 14, (235, 205, 140, 255))
    box(d, 14, 11, 17, 13, (60, 40, 25, 255))
    box(d, 12, 7, 13, 8, (30, 20, 15, 255))
    box(d, 18, 7, 19, 8, (30, 20, 15, 255))
    body_base(d, body_c, dark(body_c, 0.7), light(body_c, 1.15))
    # しっぽ
    box(d, 23, 16, 26, 20, (150, 110, 55, 255))
    return outline_sprite(img)


def sprite_monkey():
    """逃げ出したサル：赤茶色＋顔＋しっぽ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_c = (190, 140, 75, 255)
    # 耳
    box(d, 8, 6, 11, 9, (160, 110, 60, 255))
    box(d, 20, 6, 23, 9, (160, 110, 60, 255))
    # 頭＋顔
    box(d, 10, 4, 21, 14, body_c)
    box(d, 12, 7, 19, 13, (240, 200, 150, 255))
    box(d, 13, 9, 14, 10, (30, 20, 15, 255))
    box(d, 17, 9, 18, 10, (30, 20, 15, 255))
    box(d, 14, 11, 17, 12, (120, 70, 50, 255))
    body_base(d, body_c, dark(body_c, 0.7), light(body_c, 1.15))
    # 長いしっぽ
    box(d, 24, 18, 27, 19, (160, 110, 60, 255))
    box(d, 26, 19, 27, 24, (160, 110, 60, 255))
    return outline_sprite(img)


def sprite_tsuchinoko():
    """ツチノコ：緑の太い胴体＋目＋腹。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_c = (75, 205, 75, 255)
    # 太い胴体（S字）
    box(d, 8, 18, 23, 27, body_c)
    box(d, 8, 18, 23, 20, light(body_c, 1.25))
    box(d, 8, 25, 23, 27, dark(body_c, 0.65))
    box(d, 11, 21, 20, 24, (220, 240, 180, 255))
    # 頭
    box(d, 18, 8, 26, 18, body_c)
    box(d, 18, 8, 26, 10, light(body_c, 1.25))
    box(d, 20, 11, 21, 12, (20, 20, 20, 255))
    box(d, 23, 11, 24, 12, (20, 20, 20, 255))
    box(d, 21, 14, 24, 15, (200, 60, 60, 255))
    # しっぽ
    box(d, 5, 20, 8, 23, body_c)
    return outline_sprite(img)


def sprite_yeti():
    """雪男：白っぽい水色の大きな毛むくじゃら。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_c = (215, 230, 240, 255)
    # 毛のギザギザ（頭と胴体）
    for x in (10, 14, 18, 21):
        box(d, x, 3, x + 2, 6, body_c)
    box(d, 9, 5, 22, 15, body_c)
    box(d, 12, 8, 19, 13, (180, 205, 225, 255))
    box(d, 13, 9, 14, 10, (25, 25, 35, 255))
    box(d, 17, 9, 18, 10, (25, 25, 35, 255))
    box(d, 14, 11, 17, 12, (120, 140, 160, 255))
    body_base(d, body_c, (165, 185, 205, 255), (240, 248, 255, 255))
    # 腕の毛
    box(d, 6, 16, 8, 24, body_c)
    box(d, 23, 16, 25, 24, body_c)
    return outline_sprite(img)


def sprite_angel(body=(255, 230, 130, 255), wing=(255, 250, 230, 255), halo=True):
    """天使：金色の体＋白い翼＋光輪。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # 翼
    box(d, 2, 8, 9, 16, wing)
    box(d, 22, 8, 29, 16, wing)
    box(d, 2, 10, 7, 12, light(wing, 1.1))
    box(d, 24, 10, 29, 12, light(wing, 1.1))
    body_base(d, body, dark(body, 0.7), light(body, 1.2))
    # 白いローブ
    box(d, 12, 16, 19, 28, (250, 245, 230, 255))
    if halo:
        d.ellipse([12, 1, 19, 6], outline=(255, 240, 120, 255), width=2)
    face(d, skin=(255, 225, 190, 255))
    # 金髪
    box(d, 10, 4, 21, 6, (255, 215, 90, 255))
    return outline_sprite(img)


def sprite_lion_god():
    """獅子の神：金のたてがみ＋ライオンの顔。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    mane = (220, 170, 40, 255)
    # たてがみ（放射状）
    for (x0, y0, x1, y1) in [(8, 2, 12, 6), (13, 1, 18, 5), (20, 2, 24, 6),
                             (7, 7, 10, 12), (22, 7, 25, 12), (9, 12, 12, 16),
                             (20, 12, 23, 16)]:
        box(d, x0, y0, x1, y1, mane)
    # 顔
    box(d, 10, 5, 21, 15, (245, 210, 130, 255))
    box(d, 12, 6, 19, 8, (255, 230, 160, 255))
    box(d, 12, 9, 14, 10, (30, 20, 15, 255))
    box(d, 17, 9, 19, 10, (30, 20, 15, 255))
    box(d, 13, 12, 18, 14, (200, 150, 90, 255))
    box(d, 14, 12, 17, 13, (90, 50, 30, 255))
    # 胴体（金の衣）
    body_base(d, (240, 195, 70, 255), (180, 140, 40, 255), (255, 220, 110, 255))
    box(d, 14, 18, 17, 22, (255, 240, 150, 255))
    return outline_sprite(img)


def sprite_manager():
    """店長（ボス）：紫のスーツ＋ネクタイ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    body_base(d, (140, 70, 190, 255), (95, 45, 130, 255), (175, 100, 220, 255))
    # 白シャツ＋ネクタイ
    box(d, 14, 15, 17, 26, (245, 245, 245, 255))
    box(d, 15, 16, 16, 24, (200, 40, 60, 255))
    # 髪（オールバック）
    box(d, 10, 3, 21, 6, (40, 30, 35, 255))
    face(d)
    # 口ひげ
    box(d, 13, 12, 18, 13, (40, 30, 35, 255))
    return outline_sprite(img)


def sprite_giant_mascot():
    """巨大着ぐるみ（ボス）：大きな茶色のクマ。"""
    img = sprite_mascot((150, 100, 50, 255), (150, 100, 50, 255),
                        (230, 200, 150, 255), None)
    return img


def sprite_archangel():
    """大天使（ボス）：明るい金の天使＋大きな翼。"""
    return sprite_angel(body=(255, 240, 110, 255), wing=(255, 255, 240, 255), halo=True)


# ---------------------------------------------------------------------------
# アイテム
# ---------------------------------------------------------------------------
def sprite_drink():
    """ドリンク：緑のボトル＋キャップ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    box(d, 13, 4, 18, 7, (220, 60, 60, 255))
    box(d, 11, 8, 20, 26, (70, 220, 100, 255))
    box(d, 11, 8, 20, 10, (140, 255, 170, 255))
    box(d, 13, 12, 18, 22, (240, 255, 240, 255))
    box(d, 14, 13, 17, 16, (255, 220, 60, 255))
    outline(d, 11, 8, 20, 26, (20, 90, 40, 255))
    return outline_sprite(img)


def sprite_onigiri():
    """おにぎり：白い三角＋海苔。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # 三角（階段状に描く）
    for i, w in enumerate((6, 10, 14, 18, 22)):
        x0 = 16 - w // 2
        box(d, x0, 8 + i * 4, x0 + w - 1, 11 + i * 4, (250, 245, 235, 255))
    box(d, 8, 24, 23, 26, (235, 230, 220, 255))
    # 海苔
    box(d, 12, 20, 19, 26, (25, 35, 25, 255))
    box(d, 13, 21, 18, 22, (45, 60, 45, 255))
    return outline_sprite(img)


def sprite_sweets():
    """スイーツ：ピンクのカップケーキ。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # カップ
    box(d, 10, 16, 21, 26, (220, 140, 90, 255))
    box(d, 12, 16, 14, 26, (240, 170, 120, 255))
    box(d, 17, 16, 19, 26, (240, 170, 120, 255))
    # クリーム
    box(d, 11, 10, 20, 16, (255, 170, 200, 255))
    box(d, 13, 7, 18, 10, (255, 200, 220, 255))
    box(d, 15, 4, 16, 7, (230, 60, 90, 255))
    # トッピング
    px(d, 12, 12, (255, 255, 255, 255))
    px(d, 19, 13, (255, 255, 255, 255))
    px(d, 16, 11, (255, 240, 100, 255))
    return outline_sprite(img)


def sprite_umbrella():
    """傘：茶色の傘布（ドーム型・波状の裾）＋J字の柄。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # 傘布（ドーム型：段ごとに幅を広げる）
    dome = (180, 150, 110, 255)
    dome_l = (205, 175, 135, 255)
    box(d, 13, 3, 18, 5, dome_l)
    box(d, 10, 6, 21, 8, dome_l)
    box(d, 7, 9, 24, 11, dome)
    box(d, 5, 12, 26, 14, dome)
    box(d, 4, 15, 27, 16, dome)
    # 傘布の区切り線
    box(d, 10, 6, 10, 16, (140, 110, 70, 255))
    box(d, 15, 3, 15, 16, (140, 110, 70, 255))
    box(d, 21, 6, 21, 16, (140, 110, 70, 255))
    # 裾の波（スカラップ）
    for x in (5, 11, 17, 23):
        box(d, x, 16, x + 3, 18, dome)
    # 柄（J字フック）
    box(d, 15, 17, 16, 25, (120, 85, 50, 255))
    box(d, 15, 25, 19, 26, (120, 85, 50, 255))
    box(d, 18, 23, 19, 26, (120, 85, 50, 255))
    # 先端
    box(d, 15, 1, 16, 3, (120, 85, 50, 255))
    return outline_sprite(img)


def sprite_frying_pan():
    """フライパン：灰色のパン＋柄。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    d.ellipse([6, 10, 21, 25], fill=(120, 125, 135, 255))
    d.ellipse([8, 12, 19, 23], fill=(70, 75, 85, 255))
    d.ellipse([10, 14, 17, 21], fill=(200, 190, 120, 255))
    box(d, 21, 15, 29, 18, (90, 70, 50, 255))
    box(d, 21, 15, 29, 16, (130, 100, 70, 255))
    return outline_sprite(img)


def sprite_apron():
    """エプロン：白いエプロン＋紐。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    # 紐
    box(d, 10, 5, 12, 9, (230, 230, 230, 255))
    box(d, 19, 5, 21, 9, (230, 230, 230, 255))
    # 本体
    box(d, 10, 9, 21, 26, (245, 245, 240, 255))
    box(d, 10, 9, 21, 11, (255, 255, 255, 255))
    box(d, 8, 20, 23, 26, (235, 235, 230, 255))
    # ポケット
    box(d, 13, 17, 18, 21, (220, 220, 215, 255))
    outline(d, 10, 9, 21, 26, (180, 180, 175, 255))
    return outline_sprite(img)


def sprite_jacket():
    """ジャケット：青いジャケット＋襟。"""
    img = canvas()
    d = ImageDraw.Draw(img)
    shadow(d)
    box(d, 9, 8, 22, 26, (70, 120, 220, 255))
    box(d, 9, 8, 22, 10, (100, 150, 240, 255))
    # 襟
    box(d, 11, 6, 14, 9, (50, 90, 180, 255))
    box(d, 17, 6, 20, 9, (50, 90, 180, 255))
    # 前合わせ
    box(d, 15, 10, 16, 26, (40, 75, 160, 255))
    # ボタン
    box(d, 14, 14, 15, 15, (240, 240, 240, 255))
    box(d, 14, 20, 15, 21, (240, 240, 240, 255))
    # ポケット
    box(d, 11, 22, 14, 24, (50, 90, 180, 255))
    box(d, 17, 22, 20, 24, (50, 90, 180, 255))
    return outline_sprite(img)


# ---------------------------------------------------------------------------
# 生成
# ---------------------------------------------------------------------------
SPRITES = {
    # 地形
    "floor_a": lambda: tile_floor((196, 178, 150, 255), (150, 132, 108, 255), (170, 150, 122, 255)),
    "floor_b": lambda: tile_floor((180, 190, 175, 255), (140, 152, 138, 255), (155, 168, 152, 255)),
    "floor_c": lambda: tile_floor((190, 165, 140, 255), (145, 122, 100, 255), (165, 140, 115, 255)),
    "wall": tile_wall,
    "shelf": tile_shelf,
    "machine": tile_machine,
    "table": tile_table,
    "counter": tile_counter,
    "stage": tile_stage,
    "seat": tile_seat,
    "escalator": tile_escalator,
    "stairs": tile_stairs,
    # プレイヤー
    "player": sprite_player,
    # 敵
    "enemy_customer": sprite_customer,
    "enemy_clerk": sprite_clerk,
    "enemy_guard": sprite_guard,
    "enemy_bear_mascot": lambda: sprite_mascot((150, 105, 55, 255), (150, 105, 55, 255), (230, 200, 150, 255), None),
    "enemy_rabbit_mascot": lambda: sprite_mascot((245, 235, 240, 255), (245, 190, 200, 255), (255, 200, 215, 255), None),
    "enemy_dog": sprite_dog,
    "enemy_monkey": sprite_monkey,
    "enemy_tsuchinoko": sprite_tsuchinoko,
    "enemy_yeti": sprite_yeti,
    "enemy_angel": lambda: sprite_angel(),
    "enemy_lion_god": sprite_lion_god,
    "boss_manager": sprite_manager,
    "boss_giant_mascot": sprite_giant_mascot,
    "boss_archangel": sprite_archangel,
    # アイテム
    "item_drink": sprite_drink,
    "item_onigiri": sprite_onigiri,
    "item_sweets": sprite_sweets,
    "item_umbrella": sprite_umbrella,
    "item_frying_pan": sprite_frying_pan,
    "item_apron": sprite_apron,
    "item_jacket": sprite_jacket,
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, fn in SPRITES.items():
        img = fn()
        if img.size != (SIZE, SIZE):
            img = img.resize((SIZE, SIZE))
        path = os.path.join(OUT_DIR, name + ".png")
        img.save(path, optimize=True)
        print("wrote", path, img.size, os.path.getsize(path), "bytes")
    print("total:", len(SPRITES), "sprites")


if __name__ == "__main__":
    main()

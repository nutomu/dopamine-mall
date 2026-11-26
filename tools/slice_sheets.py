#!/usr/bin/env python3
"""画像生成モデルで作ったスプライトシート（WebP、マゼンタ背景）から
スプライトを切り出して PNG（透過あり）にするスクリプト。

使い方: python3 tools/slice_sheets.py
出力先: android/assets/sprites2d/<名前>.png
        samples/sheets/contact.png（目視確認用のコンタクトシート）

切り出し方法：前景（マゼンタ以外）の連結成分を scipy で検出する。
スプライト同士はマゼンタで離れているため、成分＝スプライトになる。
浮いたパーツ（天使の輪など）は近接マージで同じスプライトにまとめる。
行・列の帯による区切りは使わない（縦3段のシートは段同士が
縦方向に重なっており、水平射影では区切れないため）。
"""
import json
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHEETS_DIR = os.path.join(BASE, "samples", "sheets")
OUT_DIR = os.path.join(BASE, "android", "assets", "sprites2d")

PAD = 2       # 切り詰め時の余白
MIN_AREA = 20    # これより小さい成分はノイズとして捨てる
MAX_DIM = 160    # 切り出したスプライトの最大辺（これを超えたら縮小。APK の軽量化）

SHEETS = [
    ("terrain", "media-generation-dopamine-mall-sheet-terrain-0-59a8b927-da06-4b06-95c2-2b592bf80395.webp",
     3, 4, ["floor_mall", "floor_wood", "floor_carpet", "wall",
            "bookshelf", "shelf", "counter", "table",
            "arcade", "escalator", "stairs", "stage"]),
    ("characters", "media-generation-dopamine-mall-sheet-characters-0-f9484427-cb2b-4aad-866a-6124358a4161.webp",
     3, 5, ["hero", "customer", "clerk", "guard", "bear",
            "rabbit", "dog", "monkey", "tsuchinoko", "yeti",
            "angel", "lion", "manager", "giant_bear", "archangel"]),
    ("items", "media-generation-dopamine-mall-sheet-items-0-0a5db17a-213f-4041-9d83-cafce0862b49.webp",
     2, 4, ["drink", "onigiri", "cake", "umbrella",
            "pan", "apron", "jacket", "chest"]),
    ("props", "media-generation-dopamine-mall-sheet-props-0-c9dfa7b3-961e-47c7-810d-ebfae9cf4cf1.webp",
     2, 5, ["lantern", "plant", "vending", "trash", "cart",
            "sign", "bench", "lamp", "fog", "coins"]),
]


def is_bg_arr(arr):
    """RGB 配列から背景（マゼンタ系）マスクを作る。"""
    return (arr[:, :, 0] > 200) & (arr[:, :, 1] < 120) & (arr[:, :, 2] > 200)


def slice_sheet(sheet_name, filename, rows, cols, names):
    path = os.path.join(SHEETS_DIR, filename)
    im = Image.open(path).convert("RGB")
    arr = np.asarray(im)
    fg = ~is_bg_arr(arr)
    labeled, nlab = ndimage.label(fg, structure=np.ones((3, 3), dtype=int))
    areas = np.bincount(labeled.ravel())
    objects = ndimage.find_objects(labeled)
    # グリッド割当て：成分の中心が属するセル＝そのスプライト。
    # 浮いたパーツ（天使の輪・カウンターの湯気など）も同じセルに落ちる。
    cell_w = im.size[0] / cols
    cell_h = im.size[1] / rows
    cells = {}
    kept = 0
    for i, sl in enumerate(objects, start=1):
        if sl is None or areas[i] < MIN_AREA:
            continue
        kept += 1
        ysl, xsl = sl
        cx = (xsl.start + xsl.stop) / 2
        cy = (ysl.start + ysl.stop) / 2
        col = min(cols - 1, max(0, int(cx / cell_w)))
        row = min(rows - 1, max(0, int(cy / cell_h)))
        cells.setdefault((row, col), []).append(
            (xsl.start, ysl.start, xsl.stop, ysl.stop))
    print("  components: %d (kept %d, cells %d)" % (nlab, kept, len(cells)))
    results = {}
    for ri in range(rows):
        for ci in range(cols):
            comps = cells.get((ri, ci))
            if not comps:
                print("  WARN: %s (%d行目,%d列目) に成分なし" % (sheet_name, ri, ci))
                continue
            x0 = min(c[0] for c in comps)
            y0 = min(c[1] for c in comps)
            x1 = max(c[2] for c in comps)
            y1 = max(c[3] for c in comps)
            name = names[ri * cols + ci]
            cell = im.crop((x0, y0, x1, y1)).convert("RGBA")
            data = cell.load()
            cw, ch = cell.size
            for yy in range(ch):
                for xx in range(cw):
                    r, g, b, a = data[xx, yy]
                    if r > 200 and g < 120 and b > 200:
                        data[xx, yy] = (0, 0, 0, 0)
            # 余白を足す（cell は既に外接矩形）
            padded = Image.new("RGBA", (cw + 2 * PAD, ch + 2 * PAD), (0, 0, 0, 0))
            padded.paste(cell, (PAD, PAD))
            # 大きすぎるスプライトは縮小する（描画時の 2〜2.5 倍あれば十分）
            if max(padded.size) > MAX_DIM:
                scale = MAX_DIM / max(padded.size)
                nw = max(1, round(padded.size[0] * scale))
                nh = max(1, round(padded.size[1] * scale))
                padded = padded.resize((nw, nh), Image.LANCZOS)
            out = os.path.join(OUT_DIR, name + ".png")
            padded.save(out, optimize=True)
            results[name] = {"file": name + ".png", "w": padded.size[0],
                             "h": padded.size[1]}
            print("  %-12s %4dx%-4d (parts %d)" % (name, padded.size[0],
                                                 padded.size[1], len(comps)))
    if len(results) != len(names):
        print("  WARN: %s 期待 %d 個に対し %d 個しか切り出せなかった"
              % (sheet_name, len(names), len(results)))
    return results


def contact_sheet(all_sprites):
    """切り出したスプライトを名前付きで並べた目視確認用シート。"""
    cell_w, cell_h = 200, 220
    cols = 5
    names = sorted(all_sprites.keys())
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), (240, 240, 240))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 16)
    except Exception:
        font = ImageFont.load_default()
    for i, name in enumerate(names):
        info = all_sprites[name]
        im = Image.open(os.path.join(OUT_DIR, info["file"])).convert("RGBA")
        im.thumbnail((cell_w - 10, cell_h - 30))
        x = (i % cols) * cell_w
        y = (i // cols) * cell_h
        sheet.paste(im, (x + (cell_w - im.size[0]) // 2, y + 5), im)
        draw.text((x + 8, y + cell_h - 24), name, fill=(0, 0, 0), font=font)
    out = os.path.join(SHEETS_DIR, "contact.png")
    sheet.save(out)
    print("contact sheet: %s" % out)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    all_sprites = {}
    for sheet_name, filename, rows, cols, names in SHEETS:
        print("[%s] %s (%d sprites)" % (sheet_name, filename, len(names)))
        all_sprites.update(slice_sheet(sheet_name, filename, rows, cols, names))
    manifest = os.path.join(OUT_DIR, "manifest.json")
    with open(manifest, "w") as f:
        json.dump(all_sprites, f, indent=1, sort_keys=True)
    total = sum(os.path.getsize(os.path.join(OUT_DIR, n + ".png"))
                for n in all_sprites)
    print("total: %d sprites, %d bytes" % (len(all_sprites), total))
    contact_sheet(all_sprites)


if __name__ == "__main__":
    main()

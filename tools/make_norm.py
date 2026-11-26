#!/usr/bin/env python3
"""キャラクタースプライトの実寸を測り、方向ごとの表示サイズ正規化の
倍率を android/assets/sprites2d/norm.json に書き出す。

使い方: python3 tools/make_norm.py
出力: android/assets/sprites2d/norm.json

測るもの（スプライトごと）:
- bbox: 不透明ピクセル（alpha > 10）の bounding box（x, y は左上起点、w, h）
- area: 不透明ピクセルの数（見た目の「大きさ」の目安）

正規化の考え方:
- スプライトは切り出し時に最大辺160pxにそろえてあるため、内容の高さは
  ほぼ全方向で 156〜160px とそろっている。向きによる「大きさ」の
  ばらつきは、実は高さではなくシルエットの面積（横幅・かさ）に現れる
  （例：主人公の停止フレームは方向で 8771〜12629px、犬は 9127〜15768px）。
- そこで方向ごとに倍率 scale を決める：その方向のフレームの面積の
  中央値を、そのキャラクター種別の基準面積（8方向の中央値）で割った
  平方根。同じ方向の全フレームに同じ倍率を使うため、歩行中に
  大きさが脈打つことはない。
- 1枚絵しかない種類（ウサギ・サル・ツチノコ・雪男・獅子の神・ボス3種）は
  方向によるばらつきがないため倍率は 1.0。

norm.json はゲーム本体（Sprites.java）が読み、キャラクター描画時に
この倍率を掛ける。デバッグモードの実寸表示にも同じ値を使う。
"""
import json
import os
import statistics

import numpy as np
from PIL import Image

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(BASE, "android", "assets", "sprites2d")
OUT = os.path.join(SPRITES, "norm.json")

HERO_DIRS = 8
HERO_FRAMES = 4
DIR8_KINDS = ["customer", "clerk", "guard", "bear", "dog", "angel"]
SINGLE_KINDS = ["rabbit", "monkey", "tsuchinoko", "yeti", "lion",
                "manager", "giant_bear", "archangel"]


def measure(name):
    """スプライトの bbox と不透明ピクセル数を測る。"""
    path = os.path.join(SPRITES, name + ".png")
    im = Image.open(path).convert("RGBA")
    alpha = np.asarray(im.split()[3])
    mask = alpha > 10
    area = int(mask.sum())
    ys, xs = np.where(mask)
    bbox = {"x": int(xs.min()), "y": int(ys.min()),
            "w": int(xs.max() - xs.min() + 1),
            "h": int(ys.max() - ys.min() + 1)}
    return bbox, area


def main():
    norm = {}
    report = []

    # 主人公：8方向 × 4フレーム。方向ごとの面積の中央値で倍率を決める。
    hero_dir_area = []
    hero_names = {}
    for d in range(HERO_DIRS):
        areas = []
        for f in range(HERO_FRAMES):
            name = "hero_d%d_f%d" % (d, f)
            bbox, area = measure(name)
            hero_names[name] = (bbox, area)
            areas.append(area)
        hero_dir_area.append(statistics.median(areas))
    hero_ref = statistics.median(hero_dir_area)
    for d in range(HERO_DIRS):
        scale = (hero_ref / hero_dir_area[d]) ** 0.5
        for f in range(HERO_FRAMES):
            name = "hero_d%d_f%d" % (d, f)
            bbox, area = hero_names[name]
            norm[name] = {"x": bbox["x"], "y": bbox["y"], "w": bbox["w"],
                          "h": bbox["h"], "area": area,
                          "scale": round(scale, 3)}
    report.append("hero: ref area %.0f, dir areas %s, scales %s"
                  % (hero_ref, [round(a) for a in hero_dir_area],
                     [round((hero_ref / a) ** 0.5, 3) for a in hero_dir_area]))

    # 8方向の敵：方向＝1フレーム。8方向の面積の中央値を基準にする。
    for kind in DIR8_KINDS:
        areas = {}
        bboxes = {}
        for d in range(8):
            name = "%s_%d" % (kind, d)
            bbox, area = measure(name)
            bboxes[name] = bbox
            areas[name] = area
        ref = statistics.median(areas.values())
        scales = []
        for d in range(8):
            name = "%s_%d" % (kind, d)
            scale = (ref / areas[name]) ** 0.5
            scales.append(round(scale, 3))
            norm[name] = {"x": bboxes[name]["x"], "y": bboxes[name]["y"],
                          "w": bboxes[name]["w"], "h": bboxes[name]["h"],
                          "area": areas[name], "scale": round(scale, 3)}
        report.append("%s: ref area %.0f, areas %s, scales %s"
                      % (kind, ref, [areas["%s_%d" % (kind, d)] for d in range(8)],
                         scales))

    # 1枚絵の種類：倍率 1.0。
    for kind in SINGLE_KINDS:
        bbox, area = measure(kind)
        norm[kind] = {"x": bbox["x"], "y": bbox["y"], "w": bbox["w"],
                      "h": bbox["h"], "area": area, "scale": 1.0}
        report.append("%s: area %d, scale 1.0" % (kind, area))

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(norm, f, ensure_ascii=False, indent=1, sort_keys=True)
    print("wrote", OUT, "(%d sprites)" % len(norm))
    for line in report:
        print(" ", line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""主人公の歩行フレームを合成する（品質ゲート付き）。

4コマ版シート（hero4_d*_f*）は部分的に壊れているため、コンタクトシートで
目視確認した結果をもとに、方向ごとに使うフレームを決める。
不良セルは予備の2フレーム版シート（hero2_d*_f*）の同じ向きのフレームで
置き換える。列全体が使えない向きは予備シートの2フレームをそのまま使う
（f0,f1,f0,f1 と並べて4フレーム化する）。

品質ゲートの結果（2026-11-26、tools/check_sprites.py のコンタクトシートで確認）：
  d0 南：f2 が頭を下げていて顔が隠れる → f2 を hero2_d0_f1 で置き換え
  d1 南西：f1・f2 が頭を下げて顔が隠れる → 列全体を予備の2フレームにフォールバック
  d2 西：f2 が頭を下げていて顔が隠れる → f2 を hero2_d2_f1 で置き換え
  d3 北西：f2 が頭を下げていて顔が隠れる → f2 を hero2_d3_f1 で置き換え
  d4 北：f1 に後頭部に顔が描かれている → f1 を hero4_d4_f3（同じ列の通過フレーム）で置き換え
  d5 北東：f1 に後頭部に顔が描かれている → f1 を hero4_d5_f3 で置き換え
  d6 東：f0・f2 は背面（東を向いていない）、f1 は後頭部に顔、f3 のみ正しい
         → 列全体を予備の2フレームにフォールバック
  d7 南東：f0 は右向きの横顔（東であって南東ではない）、f1〜f3 はかがみ込み
         → 列全体を予備の2フレームにフォールバック

使い方: python3 tools/compose_hero.py
  - android/assets/sprites2d/hero_d{d}_f{r}.png（r=0..3）を合成する
  - 作業用の中間ファイル hero4_*.png / hero2_*.png を削除する
  - manifest.json を実際の PNG から作り直す
"""
import json
import os
import shutil
from PIL import Image

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(BASE, "android", "assets", "sprites2d")
RECORD = os.path.join(BASE, "samples", "sheets-flat", "hero_composition.json")

# 方向ごと：最終フレーム f0..f3 に使う中間ファイル
# 4コマ版の行：f0=左足前、f1=通過、f2=右足前、f3=通過
COMPOSITION = {
    0: ["hero4_d0_f0", "hero4_d0_f1", "hero2_d0_f1", "hero4_d0_f3"],
    1: ["hero2_d1_f0", "hero2_d1_f1", "hero2_d1_f0", "hero2_d1_f1"],
    2: ["hero4_d2_f0", "hero4_d2_f1", "hero2_d2_f1", "hero4_d2_f3"],
    3: ["hero4_d3_f0", "hero4_d3_f1", "hero2_d3_f1", "hero4_d3_f3"],
    4: ["hero4_d4_f0", "hero4_d4_f3", "hero4_d4_f2", "hero4_d4_f3"],
    5: ["hero4_d5_f0", "hero4_d5_f3", "hero4_d5_f2", "hero4_d5_f3"],
    6: ["hero2_d6_f0", "hero2_d6_f1", "hero2_d6_f0", "hero2_d6_f1"],
    7: ["hero2_d7_f0", "hero2_d7_f1", "hero2_d7_f0", "hero2_d7_f1"],
}


def main():
    for d in range(8):
        for r in range(4):
            src = os.path.join(OUT_DIR, COMPOSITION[d][r] + ".png")
            dst = os.path.join(OUT_DIR, "hero_d%d_f%d.png" % (d, r))
            shutil.copyfile(src, dst)
            print("  hero_d%d_f%d <- %s" % (d, r, COMPOSITION[d][r]))
    # 作業用の中間ファイルを削除する
    for name in os.listdir(OUT_DIR):
        if name.startswith("hero4_") or name.startswith("hero2_"):
            os.remove(os.path.join(OUT_DIR, name))
    # 合成の記録を残す
    os.makedirs(os.path.dirname(RECORD), exist_ok=True)
    with open(RECORD, "w") as f:
        json.dump({"composition": {str(d): COMPOSITION[d] for d in range(8)}},
                  f, indent=1, sort_keys=True)
    # manifest.json を実際の PNG から作り直す
    manifest = {}
    for name in sorted(os.listdir(OUT_DIR)):
        if not name.endswith(".png"):
            continue
        with Image.open(os.path.join(OUT_DIR, name)) as im:
            manifest[name[:-4]] = {"file": name, "w": im.size[0], "h": im.size[1]}
    with open(os.path.join(OUT_DIR, "manifest.json"), "w") as f:
        json.dump(manifest, f, indent=1, sort_keys=True)
    total = sum(os.path.getsize(os.path.join(OUT_DIR, n + ".png"))
                for n in manifest)
    print("manifest: %d sprites, %d bytes" % (len(manifest), total))


if __name__ == "__main__":
    main()

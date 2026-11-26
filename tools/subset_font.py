#!/usr/bin/env python3
"""日本語フォント（Noto Sans JP）をゲームで使う文字だけにサブセット化する。

使い方:
    python3 tools/subset_font.py /tmp/NotoSansJP-Regular.ttf

- 入力: インスタンス化済みの Noto Sans JP Regular（フルフォント）。
  フルフォントの入手方法:
      curl -L -o /tmp/NotoSansJP-full.ttf \
        "https://raw.githubusercontent.com/google/fonts/main/ofl/notosansjp/NotoSansJP%5Bwght%5D.ttf"
      python3 -m fontTools.varLib.instancer /tmp/NotoSansJP-full.ttf wght=400 \
        -o /tmp/NotoSansJP-Regular.ttf
- 対象文字: core/src/*.java に出てくるすべての文字（ASCII 印字可能文字を含む）。
  ゲームの表示文字列は Localization クラスと敵・アイテム・フロアタイプの
  名前に集約されているため、ソースを走査すれば必要文字がすべて集まる。
- 出力: android/assets/fonts/NotoSansJP-subset.ttf（SIL Open Font License 1.1）。

フォントのライセンス表示は README.md と android/assets/fonts/OFL.txt を参照。
"""
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIR = os.path.join(ROOT, "core", "src")
OUT = os.path.join(ROOT, "android", "assets", "fonts", "NotoSansJP-subset.ttf")
CHARS_TXT = os.path.join(ROOT, "tools", "chars.txt")


def collect_chars():
    chars = set()
    for c in range(32, 127):
        chars.add(chr(c))
    for dirpath, _dirnames, filenames in os.walk(SRC_DIR):
        for name in filenames:
            if not name.endswith(".java"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as f:
                for ch in f.read():
                    if ord(ch) > 126:
                        chars.add(ch)
    return "".join(sorted(chars))


def main():
    if len(sys.argv) < 2:
        print("usage: subset_font.py <NotoSansJP-Regular.ttf>")
        return 1
    src = sys.argv[1]
    text = collect_chars()
    with open(CHARS_TXT, "w", encoding="utf-8") as f:
        f.write(text)
    print("characters collected: %d (see tools/chars.txt)" % len(text))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    cmd = [
        sys.executable, "-m", "fontTools.subset", src,
        "--output-file=" + OUT,
        "--text-file=" + CHARS_TXT,
        "--layout-features=kern",
        "--no-hinting",
        "--notdef-outline",
        "--recommended-glyphs",
    ]
    subprocess.check_call(cmd)
    print("subset font written: %s (%d bytes)"
          % (OUT, os.path.getsize(OUT)))
    return 0


if __name__ == "__main__":
    sys.exit(main())

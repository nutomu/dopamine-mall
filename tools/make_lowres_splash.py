#!/usr/bin/env python3
"""Downscale a splash illustration to a true low-res pixel-art splash.

Nearest-neighbor downscale + adaptive palette quantization (no dither),
then snap near-magenta background pixels to exact #FF00FF chroma key.
"""
import argparse
from PIL import Image


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input")
    ap.add_argument("output")
    ap.add_argument("--width", type=int, default=128)
    ap.add_argument("--height", type=int, default=256)
    ap.add_argument("--colors", type=int, default=20)
    args = ap.parse_args()
    im = Image.open(args.input).convert("RGB")
    small = im.resize((args.width, args.height), Image.NEAREST)
    q = small.quantize(colors=args.colors, method=Image.Quantize.MEDIANCUT,
                       dither=Image.Dither.NONE)
    rgb = q.convert("RGB")
    # snap near-magenta background to exact chroma key
    px = rgb.load()
    for y in range(rgb.height):
        for x in range(rgb.width):
            r, g, b = px[x, y]
            if r > 200 and g < 100 and b > 180:
                px[x, y] = (255, 0, 255)
    rgb.save(args.output)
    cols = sorted(set(rgb.getdata()))
    print(f"{args.input} {im.size} -> {args.output} {rgb.size}, "
          f"{len(cols)} colors")
    print("palette:", cols)


if __name__ == "__main__":
    main()

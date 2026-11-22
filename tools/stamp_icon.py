"""Stamps the version number on a banner across the bottom of the banner and legacy icons.
usage: stamp_icon.py <version-text>   (run from repo root, before the Gradle build)"""
import sys, glob
from PIL import Image, ImageDraw, ImageFont

ver = sys.argv[1]
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def stamp(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    band = int(h * 0.20)                       # banner across the bottom
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    y0 = h - band
    d.rectangle([0, y0, w, h], fill=(244, 201, 93, 245))            # gold
    d.rectangle([0, y0, w, y0 + max(1, h // 90)], fill=(255, 235, 170, 255))
    f = ImageFont.truetype(FONT, int(band * 0.72))
    while d.textlength(ver, font=f) > w * 0.80 and f.size > 6:
        f = ImageFont.truetype(FONT, f.size - 1)
    tw = d.textlength(ver, font=f)
    bb = d.textbbox((0, 0), ver, font=f)
    d.text(((w - tw) / 2 - bb[0], y0 + (band - (bb[3] - bb[1])) / 2 - bb[1]), ver, font=f, fill=(7, 30, 22, 255))
    im.alpha_composite(layer)
    im.save(path)

for p in glob.glob("app/src/main/res/mipmap-*/ic_launcher.png") + glob.glob("app/src/main/res/drawable-*/banner.png"):
    stamp(p)
    print("stamped", p)

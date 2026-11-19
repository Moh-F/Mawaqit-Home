"""Stamps the version number on a diagonal corner ribbon of the banner and legacy icons.
usage: stamp_icon.py <version-text>   (run from repo root, before the Gradle build)"""
import sys, glob
from PIL import Image, ImageDraw, ImageFont

ver = sys.argv[1]
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def stamp(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    s = min(w, h)
    band = int(s * 0.16)            # ribbon thickness
    size = int(s * 0.64)            # ribbon length
    rib = Image.new("RGBA", (size, band), (0, 0, 0, 0))
    d = ImageDraw.Draw(rib)
    d.rectangle([0, 0, size, band], fill=(244, 201, 93, 245))        # gold
    f = ImageFont.truetype(FONT, int(band * 0.70))
    while d.textlength(ver, font=f) > size * 0.66 and f.size > 6:
        f = ImageFont.truetype(FONT, f.size - 1)
    tw = d.textlength(ver, font=f)
    d.text(((size - tw) / 2, band * 0.10), ver, font=f, fill=(7, 30, 22, 255))
    rib = rib.rotate(-45, expand=True, resample=Image.BICUBIC)   # top-right corner, slanted
    rw, rh = rib.size
    # place so the ribbon's centre sits on the corner's diagonal
    cx = w - int(size * 0.235)
    cy = int(size * 0.235)
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    layer.paste(rib, (cx - rw // 2, cy - rh // 2), rib)
    # keep only what is inside the original picture's corner triangle region
    im.alpha_composite(layer)
    im.save(path)

for p in glob.glob("app/src/main/res/mipmap-*/ic_launcher.png") + glob.glob("app/src/main/res/drawable-*/banner.png"):
    stamp(p)
    print("stamped", p)

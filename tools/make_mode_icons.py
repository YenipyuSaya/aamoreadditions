# Alloy Furnace mode button icons (16x16), reproducible.
#   mode_alloy.png     : vanilla ingot silhouette/shading, copper left, gold right, rose-gold middle; the blend follows the
#                        surface (diagonal on the top face, vertical on the front face)
#   mode_efficient.png : vanilla furnace "lit_progress" flame (full), GUI background grey (198) made transparent, centred
# Needs the vanilla textures extracted under VAN (see README of the preview scripts).
import sys
from PIL import Image
VAN = sys.argv[1] if len(sys.argv) > 1 else '/tmp/tex/van/assets/minecraft/textures/'
OUT = 'src/main/resources/assets/aamoreadditions/textures/gui/'

cu = Image.open(VAN + 'item/copper_ingot.png').convert('RGBA')
au = Image.open(VAN + 'item/gold_ingot.png').convert('RGBA')
assert cu.size == au.size == (16, 16)

# One shading structure for the whole ingot: gold's pixel layout (cleanest highlights), each pixel = a shade rank 0 (darkest) .. 7 (specular).
# Copper, rose gold and gold each supply an 8-step ramp; a pixel takes the ramp colour of its rank, so highlights line up.
def ramp(img):
    cols = sorted({img.getpixel((x, y))[:3] for x in range(16) for y in range(16) if img.getpixel((x, y))[3]},
                  key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    assert len(cols) == 8, cols
    return cols
CU, AU = ramp(cu), ramp(au)
ROSE = [(112, 42, 52), (150, 62, 74), (178, 84, 96), (196, 102, 114), (226, 136, 150), (242, 170, 184), (252, 204, 214), (255, 234, 240)]
rank = {c: i for i, c in enumerate(CU)}
grank = {c: i for i, c in enumerate(AU)}

# Gradient coordinate follows the ingot surface: on the top face the boundary runs diagonally down-right (s = x - y);
# on the front/side face (the darker lower-right band: gold shade ranks 0, 2, 3; rank 1 is the upper-left outline)
# it turns and runs straight down: a front pixel takes the s of the lowest top-face pixel above it in its column.
def _front_raw(x, y):
    c = au.getpixel((x, y))
    return bool(c[3]) and AU.index(c[:3]) in (0, 2, 3)
def front(x, y):
    # once a column has entered the side face, everything below it belongs to that face (lighter edge pixels too)
    return any(_front_raw(x, yy) for yy in range(0, y + 1)) and bool(au.getpixel((x, y))[3]) and y >= 5
def s_of(x, y):
    if front(x, y):
        yy = y
        while yy >= 0 and au.getpixel((x, yy))[3] and front(x, yy): yy -= 1
        if yy >= 0 and au.getpixel((x, yy))[3]: return x - yy
    return x - y
S_C = 1                       # copper|gold boundary: pixels with d = s - S_C < 0 are copper side, d >= 0 gold side
                              # (top face: (6,5) -> (9,8) diagonal, then straight down the front face)
LO, HI = -3, 2                # rose band d = -3..2: 6 px, centred on the boundary (3 px each side)
ROSE_W = {-3: 0.4, -2: 0.75, -1: 1.0, 0: 1.0, 1: 0.75, 2: 0.4}   # symmetric, even steps, no dithering
def blend(a, b, t): return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))
def colour(r, s):
    d = s - S_C
    if d < LO: return CU[r]
    if d > HI: return AU[r]
    return blend(CU[r] if d < 0 else AU[r], ROSE[r], ROSE_W[d])

out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        c = au.getpixel((x, y))
        if not c[3]: continue
        out.putpixel((x, y), colour(grank[c[:3]], s_of(x, y)) + (255,))
out.save(OUT + 'mode_alloy.png')

lit = Image.open(VAN + 'gui/sprites/container/furnace/lit_progress.png').convert('RGBA')
assert lit.size == (14, 14)
flame = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y in range(14):
    for x in range(14):
        p = lit.getpixel((x, y))
        if p[:3] == (198, 198, 198): continue      # furnace GUI background -> transparent
        flame.putpixel((x + 1, y + 1), p)          # 14x14 centred in 16x16
flame.save(OUT + 'mode_efficient.png')
print('ok')

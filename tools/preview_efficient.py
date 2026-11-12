# Static 4x previews: Efficient-Smelting flame progress in the Alloy Furnace GUI + JEI page mocks (composites, not in-game).
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import preview_alloy_select as P
from PIL import Image
OUT = '/workspace/export/aama_efficient/'
os.makedirs(OUT, exist_ok=True)
S = P.S
furnace = P.load(P.VAN + 'gui/container/furnace.png')
lit = P.load(P.VAN + 'gui/sprites/container/furnace/lit_progress.png')
burn = P.load(P.VAN + 'gui/sprites/container/furnace/burn_progress.png')
raw_iron = P.load(P.VAN + 'item/raw_iron.png')
flame_empty = furnace.crop((56, 36, 70, 50))

def save(im, name): im.resize((im.width * S, im.height * S), Image.NEAREST).save(OUT + name); return im

def efficient_gui(rows):
    """Mirror of AlloyFurnaceScreen.renderFlameProgress: cover arrow (109,43 7x18), empty flame + lit rows at (106,45)."""
    im = P.base(inputs=False, mode_eff=True)
    gx, gy = P.GX, P.GY
    P.fill(im, gx + 109, gy + 43, gx + 116, gy + 61, 0xFFC6C6C6)
    im.alpha_composite(flame_empty, (gx + 106, gy + 45))
    if rows > 0: im.alpha_composite(lit.crop((0, 14 - rows, 14, 14)), (gx + 106, gy + 45 + 14 - rows))
    P.item(im, raw_iron, gx + 71, gy + 21); P.item(im, raw_iron, gx + 109, gy + 21)
    return im

steps = [0, 4, 9, 14]
for r in steps: save(efficient_gui(r), f'1_gui_efficient_flame_{r:02d}of14.png')
# side-by-side crops of the channel: alloy arrow 50% | flame 0 / 4 / 9 / 14 ; click area outlined
crops = [P.base(inputs=False, arrow=9).crop((P.GX + 96, P.GY + 34, P.GX + 130, P.GY + 70))]
for r in steps: crops.append(efficient_gui(r).crop((P.GX + 96, P.GY + 34, P.GX + 130, P.GY + 70)))
strip = Image.new('RGBA', (len(crops) * 38 - 4, 36), (40, 40, 48, 255))
for i, c in enumerate(crops): strip.alpha_composite(c, (i * 38, 0))
big = strip.resize((strip.width * 8, strip.height * 8), Image.NEAREST)
from PIL import ImageDraw
d = ImageDraw.Draw(big)
for i in range(len(crops)):   # JEI click area (106,40)-(119,63), unchanged in both modes
    x0 = (i * 38 + 10) * 8; y0 = 6 * 8
    d.rectangle([x0, y0, x0 + 14 * 8 - 1, y0 + 24 * 8 - 1], outline=(255, 0, 255, 255), width=2)
big.save(OUT + '2_channel_8x_alloyArrow_vs_flame0-4-9-14_clickArea.png')

# JEI page mocks: grouped entries (one per shape), each cycling through the metals in sync
IEDUST = '/workspace/research/ImmersiveEngineering/src/main/resources/assets/immersiveengineering/textures/item/metal_dust_%s.png'
V = P.VAN
import numpy as np
def _persp(dst, src):
    """PIL PERSPECTIVE coefficients mapping output points dst -> source points src."""
    A, b = [], []
    for (x, y), (u, v) in zip(dst, src):
        A += [[x, y, 1, 0, 0, 0, -u * x, -u * y], [0, 0, 0, x, y, 1, -v * x, -v * y]]; b += [u, v]
    return np.linalg.solve(np.array(A, float), np.array(b, float)).tolist()
def block_icon(face):
    """Inventory-style isometric block icon (top / left / right faces, vanilla GUI shading), like JEI renders a block item."""
    N = 64; f = face.resize((16, 16), Image.NEAREST); out = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    sq = [(0, 0), (16, 0), (16, 16), (0, 16)]
    faces = [([(32, 3), (61, 17), (32, 31), (3, 17)], 1.0),      # top: back, right, front, left corners
             ([(3, 17), (32, 31), (32, 61), (3, 47)], 0.8),      # left (front-left) face
             ([(32, 31), (61, 17), (61, 47), (32, 61)], 0.6)]    # right (front-right) face
    for quad, shade in faces:
        t = f.transform((N, N), Image.PERSPECTIVE, _persp(quad, sq), Image.NEAREST)
        m = Image.new('L', (N, N), 0); from PIL import ImageDraw as D; D.Draw(m).polygon(quad, fill=255)
        px = t.load()
        for y in range(N):
            for x in range(N):
                r, g, b_, al = px[x, y]; px[x, y] = (round(r * shade), round(g * shade), round(b_ * shade), al)
        out.paste(t, (0, 0), m)
    return out.resize((16, 16), Image.NEAREST)
tex = {m: dict(raw=P.load(V + f'item/raw_{m}.png'), ore=block_icon(P.load(V + f'block/{m}_ore.png')), block=block_icon(P.load(V + f'block/raw_{m}_block.png')),
               dust=P.load(IEDUST % m), ingot=P.load(V + f'item/{m}_ingot.png')) for m in ('iron', 'copper', 'gold')}
def jei_bg(w, h): return Image.new('RGBA', (w, h), (198, 198, 198, 255))
def big_slot(im, x, y):  # JEI output slot 26x26
    P.fill(im, x, y, x + 26, y + 26, 0xFF8B8B8B)
    P.fill(im, x, y, x + 25, y + 1, 0xFF373737); P.fill(im, x, y, x + 1, y + 25, 0xFF373737)
    P.fill(im, x + 1, y + 25, x + 26, y + 26, 0xFFFFFFFF); P.fill(im, x + 25, y + 1, x + 26, y + 26, 0xFFFFFFFF)
SHAPES = [('raw', 2, 3, 80), ('raw', 3, 5, 80), ('ore', 1, 2, 80), ('block', 1, 16, 640), ('dust', 1, 1, 80)]
def entry(shape, metal):
    k, n, c, t = shape
    im = jei_bg(120, 46)
    for i in range(n):
        P.slot_frame(im, 1 + i * 18, 16); P.item(im, tex[metal][k], 2 + i * 18, 17)
    im.alpha_composite(flame_empty, (63, 18)); im.alpha_composite(lit.crop((0, 6, 14, 14)), (63, 24))
    s_ = f'{t // 20}s'; P.text(im, s_, 70 - P.text_w(s_) // 2, 34, (128, 128, 128), shadow=False)
    big_slot(im, 94, 11); P.item(im, tex[metal]['ingot'], 99, 16, c)
    return im
cols = [('iron', 'step 1: iron'), ('copper', 'step 2: copper'), ('gold', 'step 3: gold')]
sheet = Image.new('RGBA', (16 + 124 * len(cols), 14 + 50 * len(SHAPES)), (40, 40, 48, 255))
for ci, (m, label) in enumerate(cols):
    P.text(sheet, label, 8 + ci * 124, 3, (255, 255, 255))
    for ri, sh in enumerate(SHAPES): sheet.alpha_composite(entry(sh, m), (8 + ci * 124, 14 + ri * 50))
save(sheet, '3_jei_grouped_entries_cycling.png')   # 5 entries total, all slots cycle in sync per metal
# focus: U on raw gold -> raw x2 / raw x3 entries, gold only; R on copper ingot -> all 5 entries, copper only
foc = Image.new('RGBA', (16 + 124 * 2, 14 + 50 * len(SHAPES)), (40, 40, 48, 255))
P.text(foc, 'U: raw gold', 8, 3, (255, 255, 255)); P.text(foc, 'R: copper ingot', 132, 3, (255, 255, 255))
for ri, sh in enumerate(SHAPES[:2]): foc.alpha_composite(entry(sh, 'gold'), (8, 14 + ri * 50))
for ri, sh in enumerate(SHAPES): foc.alpha_composite(entry(sh, 'copper'), (132, 14 + ri * 50))
save(foc, '4_jei_focus_lookup.png')
for old in ('3_jei_pages_mock.png',):
    if os.path.exists(OUT + old): os.remove(OUT + old)
print('ok', sorted(os.listdir(OUT)))

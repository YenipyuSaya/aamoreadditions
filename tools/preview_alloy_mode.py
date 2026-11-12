# Static 4x previews of the Alloy Furnace mode button (composites, not in-game).
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import preview_alloy_select as P
from PIL import Image, ImageDraw, ImageFont
OUT = '/workspace/export/aama_alloy_mode/'
os.makedirs(OUT, exist_ok=True)
S = P.S

# 16x zoom of both icons
Z = 16
c = Image.new('RGBA', (16 * Z * 2 + 4 * Z, 16 * Z), (120, 120, 160, 255))
c.alpha_composite(P.mode_alloy_icon.resize((16 * Z, 16 * Z), Image.NEAREST), (0, 0))
c.alpha_composite(P.mode_eff_icon.resize((16 * Z, 16 * Z), Image.NEAREST), (20 * Z, 0))
c.save(OUT + '1_icons_16x_alloy_efficient.png')

def save(im, name): im.resize((im.width * S, im.height * S), Image.NEAREST).save(OUT + name)

# both buttons in each state (crop of the GUI's left edge): alloy normal / hover, efficient normal / hover; lock hover too
states = [dict(), dict(mode_hover=True), dict(mode_eff=True), dict(mode_eff=True, mode_hover=True), dict(lock_hover=True)]
strip = Image.new('RGBA', (len(states) * 64 - 4, 56), (40, 40, 48, 255))
for i, kw in enumerate(states):
    strip.alpha_composite(P.base(inputs=False, **kw).crop((0, 0, 60, 56)), (i * 64, 0))
save(strip, '2_buttons_states.png')   # alloy, alloy hover, efficient, efficient hover, lock hover

# full GUI in each mode
save(P.base(inputs=False), '3_gui_alloy_mode.png')
save(P.base(inputs=False, mode_eff=True), '3_gui_efficient_mode.png')

# zh tooltips (vanilla box, text drawn at 4x with a CJK font; title white, second line grey)
font = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc', 8 * S, index=0)
d0 = ImageDraw.Draw(Image.new('RGBA', (1, 1)))
for eff, name in ((False, '4_tooltip_zh_alloy.png'), (True, '4_tooltip_zh_efficient.png')):
    lines = [('高效烧炼模式' if eff else '合金模式', (255, 255, 255)), ('按下切换至' + ('合金模式' if eff else '高效烧炼模式'), (170, 170, 170))]
    im = P.base(inputs=False, mode_eff=eff, mode_hover=True)
    mx, my = P.GX + 8, P.GY + 32
    w = max(int(d0.textlength(t, font=font)) for t, _ in lines) // S + 1
    P.tooltip_box(im, mx + 12, my - 12, w, 18)
    big = im.resize((im.width * S, im.height * S), Image.NEAREST); d = ImageDraw.Draw(big)
    for i, (t, col) in enumerate(lines):
        x, y = (mx + 12) * S, (my - 12 + i * 10) * S - 3 * S
        d.text((x + S, y + S), t, font=font, fill=tuple(v // 4 for v in col) + (255,))
        d.text((x, y), t, font=font, fill=col + (255,))
    big.save(OUT + name)
print('ok')

# old vs new alloy icon (16x) and the gradient guide (needs /tmp/mode_alloy_old.png for "old")
if os.path.exists('/tmp/mode_alloy_old.png'):
    old = Image.open('/tmp/mode_alloy_old.png').convert('RGBA')
    c = Image.new('RGBA', (16 * Z * 2 + 4 * Z, 16 * Z), (120, 120, 160, 255))
    c.alpha_composite(old.resize((16 * Z, 16 * Z), Image.NEAREST), (0, 0))
    c.alpha_composite(P.mode_alloy_icon.resize((16 * Z, 16 * Z), Image.NEAREST), (20 * Z, 0))
    c.save(OUT + '5_alloy_old_vs_new_16x.png')

import importlib.util
spec = importlib.util.spec_from_file_location('mk', os.path.join(os.path.dirname(__file__), 'make_mode_icons.py'))
mk = importlib.util.module_from_spec(spec); spec.loader.exec_module(mk)   # regenerates the same icons (deterministic)
g = P.mode_alloy_icon.resize((16 * Z, 16 * Z), Image.NEAREST)
d = ImageDraw.Draw(g)
pts = []
for y in range(16):
    for x in range(16):
        if not mk.au.getpixel((x, y))[3]: continue
        dd = mk.s_of(x, y) - mk.S_C
        box = (x * Z, y * Z, (x + 1) * Z - 1, (y + 1) * Z - 1)
        if dd == 0: d.rectangle(box, outline=(255, 0, 0, 255), width=2)           # boundary: first gold-side pixel (band centred on its left edge)
        elif dd in (mk.LO, mk.HI): d.rectangle(box, outline=(0, 255, 255, 255), width=1)  # last blended step each side
        if mk.front(x, y): d.line((box[0] + 2, box[3] - 2, box[2] - 2, box[3] - 2), fill=(255, 255, 255, 255), width=2)  # front face
c = Image.new('RGBA', (16 * Z, 16 * Z), (120, 120, 160, 255)); c.alpha_composite(g)
c.save(OUT + '6_alloy_gradient_guide_16x.png')
print('ok2')

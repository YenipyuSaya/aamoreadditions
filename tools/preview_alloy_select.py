# Static 4x previews of the Alloy Furnace same-tag selection UI (not an in-game screenshot).
# Uses the mod's real GUI textures, the vanilla button sprites/font and the AA energy bar texture.
from PIL import Image, ImageDraw, ImageFont
import sys
RES = 'src/main/resources/assets/aamoreadditions/textures/gui/'
VAN = '/tmp/tex/van/assets/minecraft/textures/'
AAINV = '/workspace/aa/ActuallyAdditions/src/main/resources/assets/actuallyadditions/textures/gui/gui_inventory.png'
OUT = '/workspace/export/aama_alloy_select/'
S = 4
GX, GY = 8, 8          # GUI origin on the canvas
W, H = 330, 196

def load(p): return Image.open(p).convert('RGBA')
top = load(RES + 'alloyfurnace.png'); inv = load(RES + 'alloyfurnace_inventory.png')
lock = load(RES + 'recipe_lock.png'); unlock = load(RES + 'recipe_unlock.png'); sel = load(RES + 'alloy_select.png')
aainv = load(AAINV); ascii_png = load(VAN + 'font/ascii.png')
btn = {k: load(VAN + 'gui/sprites/widget/%s.png' % k) for k in ('button', 'button_highlighted', 'button_disabled')}
iron = load(VAN + 'item/iron_ingot.png'); coal = load('/tmp/tex/mek_coal.png')
mek = load('/tmp/tex/mek_steel.png'); ori = load('/tmp/tex/ori_steel.png')
mode_alloy_icon = load(RES + 'mode_alloy.png'); mode_eff_icon = load(RES + 'mode_efficient.png')

def nine(src, w, h, b=3):
    sw, sh = src.size; o = Image.new('RGBA', (w, h))
    xs = [(0, b, 0, b), (b, sw - b, b, w - b), (sw - b, sw, w - b, w)]
    ys = [(0, b, 0, b), (b, sh - b, b, h - b), (sh - b, sh, h - b, h)]
    for sx0, sx1, dx0, dx1 in xs:
        for sy0, sy1, dy0, dy1 in ys:
            if dx1 > dx0 and dy1 > dy0:
                o.paste(src.crop((sx0, sy0, sx1, sy1)).resize((dx1 - dx0, dy1 - dy0), Image.NEAREST), (dx0, dy0))
    return o

def fill(im, x0, y0, x1, y1, argb):
    a = (argb >> 24) & 255; c = ((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, a)
    ov = Image.new('RGBA', (x1 - x0, y1 - y0), c); im.alpha_composite(ov, (x0, y0))

def grad(im, x0, y0, x1, y1, ca, cb):
    for y in range(y0, y1):
        t = (y - y0) / max(1, y1 - y0 - 1)
        c = 0
        for sh in (24, 16, 8, 0):
            va = (ca >> sh) & 255; vb = (cb >> sh) & 255
            c |= int(va + (vb - va) * t) << sh
        fill(im, x0, y, x1, y + 1, c)

# --- vanilla bitmap font (ascii.png) ---
def glyph(ch):
    o = ord(ch); gx, gy = (o % 16) * 8, (o // 16) * 8
    g = ascii_png.crop((gx, gy, gx + 8, gy + 8))
    w = 0
    for x in range(8):
        if any(g.getpixel((x, y))[3] for y in range(8)): w = x + 1
    return g, (4 if ch == ' ' else w + 1)
def text_w(s): return sum(glyph(c)[1] for c in s)
def text(im, s, x, y, rgb, shadow=True):
    for pass_ in ((1, (rgb[0] // 4, rgb[1] // 4, rgb[2] // 4)), (0, rgb)) if shadow else ((0, rgb),):
        off, col = pass_; cx = x
        for c in s:
            g, adv = glyph(c)
            tint = Image.new('RGBA', g.size, col + (255,)); tint.putalpha(g.split()[3])
            im.alpha_composite(tint, (cx + off, y + off)); cx += adv

def tooltip_box(im, x, y, w, h):
    # vanilla 1.21.1 TooltipRenderUtil colours
    fill(im, x - 3, y - 4, x + w + 3, y - 3, 0xF0100010); fill(im, x - 3, y + h + 3, x + w + 3, y + h + 4, 0xF0100010)
    fill(im, x - 3, y - 3, x + w + 3, y + h + 3, 0xF0100010)
    fill(im, x - 4, y - 3, x - 3, y + h + 3, 0xF0100010); fill(im, x + w + 3, y - 3, x + w + 4, y + h + 3, 0xF0100010)
    grad(im, x - 3, y - 3 + 1, x - 3 + 1, y + h + 3 - 1, 0x505000FF, 0x5028007F)
    grad(im, x + w + 2, y - 3 + 1, x + w + 3, y + h + 3 - 1, 0x505000FF, 0x5028007F)
    fill(im, x - 3, y - 3, x + w + 3, y - 2, 0x505000FF); fill(im, x - 3, y + h + 2, x + w + 3, y + h + 3, 0x5028007F)

def tooltip(im, lines, mx, my):
    """lines: list of (text, rgb). Positioned like vanilla: mouse + (12, -12)."""
    w = max(text_w(t) for t, _ in lines); h = 8 + (len(lines) - 1) * 10
    x, y = mx + 12, my - 12
    tooltip_box(im, x, y, w, h)
    for i, (t, c) in enumerate(lines): text(im, t, x, y + i * 10, c)

def slot_frame(im, x, y):
    fill(im, x, y, x + 18, y + 18, 0xFF8B8B8B)
    fill(im, x, y, x + 17, y + 1, 0xFF373737); fill(im, x, y, x + 1, y + 17, 0xFF373737)
    fill(im, x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF); fill(im, x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF)

def item(im, tex, x, y, count=None):
    im.alpha_composite(tex, (x, y))
    if count and count > 1:
        s = str(count); text(im, s, x + 17 - text_w(s), y + 9, (255, 255, 255))

def base(lit=False, hover_sel=False, inputs=True, lock_hover=False, ghost=None, arrow=0, check=False, lock_icon=None, mode_eff=False, mode_hover=False):
    im = Image.new('RGBA', (W, H), (40, 40, 48, 255))
    g = Image.new('RGBA', (176, 179), (0, 0, 0, 0))
    g.alpha_composite(inv.crop((0, 0, 176, 86)), (0, 93)); g.alpha_composite(top.crop((0, 0, 176, 93)), (0, 0))
    if arrow: g.alpha_composite(top.crop((176, 0, 183, arrow)), (109, 43))
    # energy bar (AA texture), ~60% full, AA wheel colour approximated
    g.alpha_composite(aainv.crop((18, 171, 36, 256)), (27, 5))
    h = 50; bar = aainv.crop((36, 172, 52, 172 + h)).copy()
    tint = Image.new('RGBA', bar.size, (255, 80, 60, 255)); bar = Image.composite(Image.blend(bar, tint, 0.45), bar, bar)
    g.alpha_composite(bar, (28, 5 + 84 - h))
    im.alpha_composite(g, (GX, GY))
    # recipe lock button: vanilla button body + icon (16x16 at gui (0,5))
    # 18x18 body at gui (-1,4), 16x16 icon centred at gui (0,5)
    body = nine(btn['button_highlighted' if lock_hover else 'button'], 18, 18)
    im.alpha_composite(body, (GX - 1, GY + 4)); im.alpha_composite(lock_icon or unlock, (GX + 0, GY + 5))
    # mode button: same body directly below (gui (-1,23)), icon centred at (0,24)
    im.alpha_composite(nine(btn['button_highlighted' if mode_hover else 'button'], 18, 18), (GX - 1, GY + 23))
    im.alpha_composite(mode_eff_icon if mode_eff else mode_alloy_icon, (GX + 0, GY + 24))
    # same-tag button 10x10 at gui (132,72)
    u = (40 if hover_sel else 30) if check else 0 if not lit else (20 if hover_sel else 10)
    im.alpha_composite(sel.crop((u, 0, u + 10, 10)), (GX + 132, GY + 72))
    if inputs:
        item(im, iron, GX + 71, GY + 21, 12); item(im, coal, GX + 109, GY + 21, 12)
    if ghost is not None:
        x, y = GX + 109, GY + 69; im.alpha_composite(ghost, (x, y)); fill(im, x, y, x + 16, y + 16, 0x998B8B8B)
    return im

def save(im, name):
    big = im.resize((im.width * S, im.height * S), Image.NEAREST); big.save(OUT + name); return big

def main():
    # 1) dark: only one candidate (or no recipe) -> pressed, inert
  save(base(lit=False, inputs=False), '1_button_dark.png')
  # 2) lit + tooltip (en / zh)
  mx, my = GX + 137, GY + 77
  im = base(lit=True, hover_sel=True)
  tooltip(im, [('Same-Tag items detected.', (255, 255, 255)), ('Press the button to select one.', (255, 255, 255))], mx, my)
  save(im, '2_button_lit_tooltip_en.png')
  im = base(lit=True, hover_sel=True)
  zh = ['检测到相同标签的物品。', '按下以选择一个。']
  font = ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc', 8 * S, index=0)
  d0 = ImageDraw.Draw(Image.new('RGBA', (1, 1)))
  wz = max(int(d0.textlength(t, font=font)) for t in zh) // S + 1
  tooltip_box(im, mx + 12, my - 12, wz, 18)
  big = save(im, '2_button_lit_tooltip_zh.png')
  d = ImageDraw.Draw(big)
  for i, t in enumerate(zh):
      d.text(((mx + 12) * S + S, (my - 12 + i * 10) * S - 3 * S + S), t, font=font, fill=(63, 63, 63, 255))
      d.text(((mx + 12) * S, (my - 12 + i * 10) * S - 3 * S), t, font=font, fill=(255, 255, 255, 255))
  big.save(OUT + '2_button_lit_tooltip_zh.png')
  # 3) selection overlay with 2 items, hovering the 2nd
  im = base(lit=True)
  grad(im, 0, 0, W, H, 0xC0101010, 0xD0101010)
  for i, tex in enumerate((mek, ori)):
      fx, fy = GX + 127 + i * 18, GY + 43
      slot_frame(im, fx, fy); item(im, tex, fx + 1, fy + 1)
  fx = GX + 127 + 18; fill(im, fx + 1, GY + 44, fx + 17, GY + 60, 0x80FFFFFF)
  tooltip(im, [('Steel Ingot', (255, 255, 255)), ('Oritech', (85, 85, 255))], fx + 9, GY + 52)
  save(im, '3_selection_overlay.png')
  # 4) chosen: ghost of the chosen item in the empty output, furnace working (arrow), button still lit
  save(base(check=True, ghost=ori, arrow=9), '4_output_ghost.png')
  # 5) lock button with background: normal and hovered, cropped
  tiles = [base(inputs=False).crop((0, 0, 60, 40)), base(inputs=False, lock_hover=True).crop((0, 0, 60, 40)),
           base(inputs=False, lock_icon=lock).crop((0, 0, 60, 40)), base(inputs=False, lock_hover=True, lock_icon=lock).crop((0, 0, 60, 40))]
  c = Image.new('RGBA', (252, 40), (40, 40, 48, 255))
  for i, t in enumerate(tiles): c.alpha_composite(t, (i * 64, 0))
  save(c, '5_lock_button_normal_hover.png')   # unlocked normal, unlocked hover, locked normal, locked hover
  # 6) icon before/after (16x zoom; changed pixels outlined in cyan: unlock (6,3) and (9,3); lock unchanged)
  Z = 16
  cmp_ = Image.new('RGBA', (4 * 16 * Z + 3 * 8 * Z // 2, 16 * Z), (120, 120, 160, 255))
  pairs = [load('/tmp/lockbefore/recipe_lock.png'), lock, load('/tmp/lockbefore/recipe_unlock.png'), unlock]
  for i, ic in enumerate(pairs):
      ox = i * (16 * Z + 4 * Z)
      cmp_.alpha_composite(ic.resize((16 * Z, 16 * Z), Image.NEAREST), (ox, 0))
      d = ImageDraw.Draw(cmp_)
      for px_, py_ in (((6, 3), (9, 3)) if i >= 2 else ()):
          d.rectangle((ox + px_ * Z, py_ * Z, ox + (px_ + 1) * Z - 1, (py_ + 1) * Z - 1), outline=(0, 255, 255, 255), width=2)
  cmp_.save(OUT + '6_lock_icon_before_after_16x.png')   # lock before, lock after, unlock before, unlock after
  # 7) check state: a choice exists -> green check, clickable; shown whether or not the furnace runs
  save(base(check=True, ghost=ori, arrow=11), '7_button_check.png')                       # running
  save(base(check=True, ghost=ori, arrow=0), '7_button_check_idle.png')      # chosen but idle (e.g. no energy): still check
  im = base(check=True, hover_sel=True, ghost=ori, arrow=11)
  tooltip(im, [('Selected: Steel Ingot', (255, 255, 255)), ('Press the button to change it.', (255, 255, 255))], GX + 137, GY + 77)
  save(im, '7_button_check_hover.png')
  print('ok')

if __name__ == '__main__':
    main()

# Alloy Furnace same-tag selection button, 50x10: u=0 dark/pressed (inactive), u=10 lit '!', u=20 lit '!' hover,
# u=30 green check (a same-tag choice is set; clickable to change), u=40 green check hover.
from PIL import Image
OUT = 'src/main/resources/assets/aamoreadditions/textures/gui/alloy_select.png'
im = Image.new('RGBA', (50, 10), (0, 0, 0, 0)); px = im.load()
BLACK = (0, 0, 0, 255)
EXCL = [(x, y) for x in (4, 5) for y in (2, 3, 4, 5, 7)]          # "!" : bar y2..5, dot y7
CHECK = [(2, 4), (2, 5), (3, 5), (3, 6), (4, 6), (4, 7), (5, 6), (5, 5), (6, 5), (6, 4), (7, 3), (7, 2)]  # check mark, 2px thick
def state(ox, body, tl, br, mark, shape=EXCL):
    for x in range(10):
        for y in range(10):
            px[ox + x, y] = BLACK if x in (0, 9) or y in (0, 9) else body
    for i in range(1, 9):
        px[ox + i, 1] = tl; px[ox + 1, i] = tl      # top/left bevel
        px[ox + i, 8] = br; px[ox + 8, i] = br      # bottom/right bevel
    px[ox + 8, 1] = body; px[ox + 1, 8] = body
    for x, y in shape: px[ox + x, y] = mark
state(0,  (60, 60, 60, 255),    (35, 35, 35, 255),    (85, 85, 85, 255),    (110, 30, 30, 255))   # pressed: inset, dull
state(10, (205, 205, 205, 255), (255, 255, 255, 255), (120, 120, 120, 255), (225, 20, 20, 255))  # lit: raised, red
state(20, (235, 235, 235, 255), (255, 255, 255, 255), (150, 150, 150, 255), (255, 50, 50, 255))  # lit hover
state(30, (205, 205, 205, 255), (255, 255, 255, 255), (120, 120, 120, 255), (30, 160, 30, 255), CHECK)  # check: raised, green
state(40, (235, 235, 235, 255), (255, 255, 255, 255), (150, 150, 150, 255), (50, 200, 50, 255), CHECK)  # check hover
im.save(OUT)

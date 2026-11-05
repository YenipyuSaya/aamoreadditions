from PIL import Image
G='/workspace/export/gui/'; OUT='/workspace/aa/addon/src/main/resources/assets/aamoreadditions/textures/gui/'
im=Image.open(G+'gui_furnace_double.png').convert('RGBA')
BG=(198,198,198,255); D=(55,55,55,255); M=(139,139,139,255); W=(255,255,255,255)
px=im.load()
for x in range(30,146):
    for y in range(16,90): px[x,y]=BG
for x in range(176,256):
    for y in range(0,256): px[x,y]=(0,0,0,0)
def frame(x,y,s):
    for i in range(s):
        for j in range(s): px[x+i,y+j]=M
    for i in range(s-1): px[x+i,y]=D; px[x,y+i]=D
    for i in range(1,s): px[x+i,y+s-1]=W; px[x+s-1,y+i]=W
    px[x+s-1,y]=M; px[x,y+s-1]=M
frame(68,18,22); frame(106,18,22)   # inputs 22x22
frame(104,64,26)                     # output 26x26 (AA)
frame(62,68,18)                      # extra 18x18
# arrow shape 16 wide x 19 tall; empty at gui pos, fill at u=176 (v=0) 
ARROW=[]
for r in range(11): ARROW.append((6,10,r))
for k in range(8): ARROW.append((k,16-k,11+k))
def arrow(ox,oy,c):
    for a,b,r in ARROW:
        for x in range(a,b): px[ox+x,oy+r]=c
# arrows removed in v5 (u>=176 area left transparent)
# channel between inputs: inner y30..33 (4px), top bevel y29 dark, bottom bevel y34 light, x89..106 (opens both frames)
for x in range(89,107):
    px[x,29]=D; px[x,34]=W
    for y in range(30,34): px[x,y]=M
px[89,29]=M; px[106,34]=M
# vertical channel right input -> output: left bevel x120 dark, inner x121..124, right bevel x125 light, y39..64
for y in range(39,65):
    px[120,y]=D; px[125,y]=W
    for x in range(121,125): px[x,y]=M
px[120,39]=M; px[125,64]=M
# horizontal channel extra -> output: top bevel y74 dark, inner y75..78, bottom bevel y79 light, x79..104
for x in range(79,105):
    px[x,74]=D; px[x,79]=W
    for y in range(75,79): px[x,y]=M
px[79,74]=M; px[104,79]=M
# green plus, top-left of extra frame (frame at 62,68), arms 2px thick, 6px span, dark edge
GR=(60,220,60,255); GD=(25,110,25,255)
cells=[(x,y) for x in range(57,63) for y in (63,64)]+[(x,y) for x in (59,60) for y in range(61,67)]
cs=set(cells)
for x,y in cells:
    for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
        n=(x+dx,y+dy)
        if n not in cs and not (63<=n[0]<=78 and 69<=n[1]<=84): px[n]=GD
for c in cells: px[c]=GR
# progress arrow (v8): shaft 3 wide x 14 rows, head 7/5/3/1, total 18 rows; bg = slot grey, fill sprite at u=176,v=0
AX,AY=109,43; FILL=(40,91,18,255)   # FILL = AA gui_grinder.png progress fill (u=176)
SHAPE=[(2,5)]*14+[(0,7),(1,6),(2,5),(3,4)]
def parrow(ox,oy,c):
    for r,(a,b) in enumerate(SHAPE):
        for x in range(a,b): px[ox+x,oy+r]=c
parrow(AX,AY,M); parrow(176,0,FILL)
im.save(OUT+'alloyfurnace.png')
strip=Image.new('RGBA',(11*11,22),BG)
for k in range(11):
    strip.alpha_composite(im.crop((AX-2,AY-2,AX+9,AY+20)),(k*11,0))
    rows=(k*18+5)//10
    if rows: strip.alpha_composite(im.crop((176,0,183,rows)),(k*11+2,2))
strip.resize((11*11*4,22*4),Image.NEAREST).save(G+'alloyfurnace_arrow_progress_strip_v8.png')
inv=Image.open(G+'gui_inventory.png').convert('RGBA'); inv.save(OUT+'alloyfurnace_inventory.png')
comp=Image.new('RGBA',(176,179),(0,0,0,0))
comp.alpha_composite(inv.crop((0,0,176,86)),(0,93)); comp.alpha_composite(im.crop((0,0,176,93)),(0,0))
big=comp.resize((704,716),Image.NEAREST)
from PIL import ImageDraw
ov=Image.new('RGBA',big.size,(0,0,0,0)); d=ImageDraw.Draw(ov)
d.rectangle((27*4,5*4,46*4-1,91*4-1),fill=(255,0,0,70),outline=(255,0,0,200),width=3)   # energy bar 27..45 x 5..90 (preview only)
d.rectangle((0,5*4,16*4-1,21*4-1),fill=(0,90,255,70),outline=(0,90,255,220),width=3)   # S/L button 16x16 at (0,5)
d.text((4,5*4+20),'L',fill=(0,0,0,255)); d.text((27*4+6,40*4),'FE',fill=(150,0,0,255))
big.alpha_composite(ov); big.save(G+'alloyfurnace_gui_preview_v8.png')

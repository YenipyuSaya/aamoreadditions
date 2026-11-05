# Generates textures/gui/casing_cross.png (176x93 top panel; progress arrow fill at u=176,v=0, 22x9)
from PIL import Image
W,H=176,93
im=Image.new('RGBA',(256,256),(0,0,0,0)); p=im.putpixel
BK,WH,BG,DK,SL=(0,0,0,255),(255,255,255,255),(198,198,198,255),(55,55,55,255),(139,139,139,255)
def rect(x0,y0,x1,y1,c):
    for x in range(x0,x1):
        for y in range(y0,y1): p((x,y),c)
# vanilla-style panel (bottom open, inventory joined below)
rect(3,3,W-3,H,BG)
rect(3,0,W-3,1,BK); rect(0,3,1,H,BK); rect(W-1,3,W,H,BK)
rect(3,1,W-3,3,WH); rect(1,3,3,H,WH); rect(W-3,3,W-1,H,DK)
p((1,1),BK);p((2,1),BK);p((1,2),BK);p((2,2),WH);p((W-2,1),BK);p((W-2,2),BK);p((W-3,1),BK)
def slot(x,y,size=18):  # x,y = item pos; single bevel frame around it
    b=(size-16)//2; x0,y0=x-b,y-b; x1,y1=x0+size,y0+size
    rect(x0,y0,x1,y1,SL)
    rect(x0,y0,x1-1,y0+1,DK); rect(x0,y0,x0+1,y1-1,DK)
    rect(x0+1,y1-1,x1,y1,WH); rect(x1-1,y0+1,x1,y1,WH)
# layout shifted 22px left (energy bar now at x=5)
for s_ in [(72,14),(72,66),(44,40),(100,40),(128,66)]: slot(*s_)
slot(72,40,20)  # centre: 20px (one ring thinner than before), still > 18
# arrow 28x9, solid (no edge pixels): 3px shaft rows 3..5 at x0..18, symmetric 2:1 head x19..27; empty and filled share the silhouette
def arrow(ox,oy,c):
    for x in range(19):
        for y in range(3,6): p((ox+x,oy+y),c)
    for k in range(9):
        h=4-k//2  # 4,4,3,3,2,2,1,1,0
        for y in range(4-h,5+h): p((ox+19+k,oy+y),c)
arrow(94,70,SL); arrow(176,0,WH)
im.save('src/main/resources/assets/aamoreadditions/textures/gui/casing_cross.png')

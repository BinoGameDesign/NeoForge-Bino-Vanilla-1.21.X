# Hotbar no estilo do inventario do Bino: pocos escuros arredondados (rgba(33,33,33,0.84)) com fio de luz em cima e a
# moldura branca de selecao (igual a "select" do inventario). Substitui os desenhos do jogo (hud/hotbar*).
from PIL import Image
OUT='src/main/resources/assets/minecraft/textures/gui/sprites/hud/'
SLOT=(33,33,33,214); LIGHT=(255,255,255,26); DARK=(0,0,0,60)
def cell(im,x0,y0,s=18,r=3):
    for y in range(s):
        for x in range(s):
            # canto arredondado
            dx=max(r-x, x-(s-1-r), 0); dy=max(r-y, y-(s-1-r), 0)
            if dx*dx+dy*dy > r*r: continue
            c=SLOT
            if y==0 or (y==1 and (x<r or x>s-1-r)): c=(55,55,55,220)        # fio de luz em cima
            elif y==s-1: c=(20,20,20,225)                                   # sombra embaixo
            im.putpixel((x0+x,y0+y),c)
# (27/11) painel grande em volta dos itens, como o painel do inventario (cinza escuro translucido, 0xA8222426), com
# cantos arredondados e um fio de luz fraco na borda de cima
PANEL=(34,36,38,168)
def panel(im,x0,y0,w,h,r=4):
    for y in range(h):
        for x in range(w):
            dx=max(r-x, x-(w-1-r), 0); dy=max(r-y, y-(h-1-r), 0)
            if dx*dx+dy*dy > r*r+1: continue
            c=PANEL
            edge = dx*dx+dy*dy > (r-1)*(r-1) and (dx or dy)
            if y==0 or (edge and y<r): c=(min(255,PANEL[0]+40),min(255,PANEL[1]+40),min(255,PANEL[2]+40),PANEL[3]+20)
            im.putpixel((x0+x,y0+y),c)
hb=Image.new('RGBA',(182,22),(0,0,0,0))
panel(hb,0,0,182,22)
for i in range(9): cell(hb,2+20*i,2)
hb.save(OUT+'hotbar.png')
# selecao 24x23: moldura branca de 20x20 em volta do poco (cantos recortados como a "select")
sel=Image.new('RGBA',(24,23),(0,0,0,0))
x0,y0,s=2,2,20
W=(255,255,255,255); G=(231,231,231,255)
for i in range(s):
    for (x,y) in [(x0+i,y0),(x0+i,y0+s-1),(x0,y0+i),(x0+s-1,y0+i)]:
        if (x,y) in [(x0,y0),(x0+s-1,y0),(x0,y0+s-1),(x0+s-1,y0+s-1)]: continue
        sel.putpixel((x,y),W if (i%7) else G)
for (x,y) in [(x0+1,y0+1),(x0+s-2,y0+1),(x0+1,y0+s-2),(x0+s-2,y0+s-2)]: sel.putpixel((x,y),W)
sel.save(OUT+'hotbar_selection.png')
# mao secundaria (29x24): um poco so
l=Image.new('RGBA',(29,24),(0,0,0,0)); panel(l,0,1,22,22); cell(l,2,3); l.save(OUT+'hotbar_offhand_left.png')
r_=Image.new('RGBA',(29,24),(0,0,0,0)); panel(r_,7,1,22,22); cell(r_,9,3); r_.save(OUT+'hotbar_offhand_right.png')
prev=Image.new('RGBA',(230,30),(90,140,70,255)); prev.alpha_composite(l,(0,3)); prev.alpha_composite(hb,(30,4)); prev.alpha_composite(sel,(30-1+40,3))
prev.resize((prev.width*5,prev.height*5),Image.NEAREST).save('/tmp/claude-0/hotbar_prev.png')

# Rascunho dos icones de nivel (estilo de patentes que evoluem a cada 10 niveis). 10 faixas, 16x16 cada.
from PIL import Image
W=16
def sh(c,k): return tuple(max(0,min(255,int(v*k))) for v in c)
# escudo 10x12 (x 3..12, y 2..13)
SHIELD=[
"..########..",
".#........#.",
"#..........#",
"#..........#",
"#..........#",
"#..........#",
"#..........#",
".#........#.",
".#........#.",
"..#......#..",
"...#....#...",
"....####....",
]
TIERS=[ # (nome, metal/borda, preenchimento, gema ou None, divisas, extras)
 ("madeira", (92,60,32),(150,102,58), None, 1, ""),
 ("pedra",   (70,70,74),(130,130,136), None, 2, ""),
 ("cobre",   (120,60,30),(196,108,64), None, 2, "stud"),
 ("ferro",   (90,96,104),(206,212,220), None, 3, "stud"),
 ("ouro",    (150,100,10),(244,196,48), None, 3, "stud"),
 ("esmeralda",(150,100,10),(244,196,48),(40,200,90), 1, "stud"),
 ("diamante",(40,120,140),(100,226,232),(230,255,255), 1, "stud star"),
 ("ametista",(70,40,120),(160,110,220),(250,210,255), 1, "wings star"),
 ("netherite",(30,26,30),(78,66,74),(230,50,50), 2, "wings star"),
 ("lendario",(160,110,10),(255,226,90),(255,255,255), 2, "wings crown star"),
]
def px(im,x,y,c):
    if 0<=x<W and 0<=y<W: im.putpixel((x,y),c+(255,))
def tier(t):
    name,edge,fill,gem,chev,ex=t
    im=Image.new('RGBA',(W,W),(0,0,0,0))
    ox,oy=2,3 if "crown" in ex else 2
    for j,row in enumerate(SHIELD):
        inside=False
        for i,ch in enumerate(row):
            if ch=='#': px(im,ox+i,oy+j,sh(edge,0.8))
        # preencher entre as bordas
        xs=[i for i,ch in enumerate(row) if ch=='#']
        if len(xs)>=2:
            for i in range(xs[0]+1,xs[-1]):
                k=1.18 if (i+j)<7 else (0.82 if (i-j)>3 or j>8 else 1.0)
                px(im,ox+i,oy+j,sh(fill,k))
        elif j==0:
            pass
    # topo do escudo preenchido
    for i in range(3,9): px(im,ox+i,oy,sh(edge,0.8))
    # brilho
    px(im,ox+2,oy+2,sh(fill,1.4)); px(im,ox+3,oy+2,sh(fill,1.3)); px(im,ox+2,oy+3,sh(fill,1.3))
    # divisas (chevrons) = progresso dentro da faixa
    for c in range(chev):
        y=(oy+6 if not gem else oy+7)+c*3 - (1 if chev==3 and not gem else 0)
        if y>oy+10: break
        for d in range(3):
            px(im,ox+5-d,y-d,sh(edge,0.75)); px(im,ox+6+d,y-d,sh(edge,0.75))
    if gem:
        gx,gy=ox+5,oy+3
        for (dx,dy,k) in [(0,0,1.0),(1,0,0.85),(0,1,0.85),(1,1,0.7),(0,-1,1.1),(1,-1,1.0),(-1,0,0.9),(2,0,0.75)]:
            px(im,gx+dx,gy+dy,sh(gem,k))
        px(im,gx,gy-1,(255,255,255))
    if "stud" in ex:
        for (x,y) in [(ox+2,oy+1),(ox+9,oy+1)]: px(im,x,y,sh(edge,1.25))
    if "wings" in ex:
        wc=(235,235,245) if name!="netherite" else (120,60,60)
        feather=[(1,4,1.1),(0,5,1.0),(1,5,1.05),(0,6,0.95),(1,6,1.0),(1,7,0.9),(0,7,0.85),(1,8,0.8),(1,9,0.7)]
        for (x,y,k) in feather:
            px(im,x,y,sh(wc,k)); px(im,W-1-x,y,sh(wc,k))
    if "crown" in ex:
        cc=(255,200,40)
        for x in range(5,11): px(im,x,2,sh(cc,0.85))
        for x in (5,7,8,10): px(im,x,1,cc)
        px(im,5,0,cc); px(im,10,0,cc); px(im,7,0,(255,255,255)); px(im,8,0,(255,255,255))
    if "star" in ex and "crown" not in ex:
        sc=(255,240,140)
        for (x,y) in [(7,0),(8,0),(7,1),(8,1),(6,1),(9,1)]: px(im,x,y,sc)
    return im
sheet=Image.new('RGBA',(W*10,W),(0,0,0,0))
for i,t in enumerate(TIERS): sheet.paste(tier(t),(i*W,0))
sheet.save('/tmp/claude-0/lvl/nivel_patentes.png')
prev=Image.new('RGBA',(W*10+18,W+4),(55,55,60,255)); 
prev=Image.new('RGBA',(10*(W+2)+2,W+4),(55,55,60,255))
for i in range(10): prev.alpha_composite(sheet.crop((i*W,0,i*W+W,W)),(2+i*(W+2),2))
prev.resize((prev.width*8,prev.height*8),Image.NEAREST).save('/tmp/claude-0/lvl/preview.png')

# Rascunho dos icones de nivel (estilo DDTank): um icone por nivel, 0 a 100, 16x16 cada.
# A dezena escolhe a patente (0-9 madeira, 10-19 pedra ... 90-99 lendaria) e a unidade acende os pontinhos de baixo:
# 1-5 = 1 a 5 pontos prateados; 6-9 = os 5 pontos, com 1 a 4 deles dourados. Nivel 100 = lendaria radiante.
# Folha: 10 colunas (unidade) x 11 linhas (dezena; a ultima so tem o 100).
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
def tier(t, unit=0, radiant=False):
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
    # pontinhos da unidade (embaixo do escudo)
    silver,gold=(214,220,230),(255,206,60)
    for k in range(5):
        x=3+k*2+ (1 if k>=0 else 0)
        lit = unit>=k+1 if unit<=5 else True
        if not lit: continue
        isgold = unit>5 and k < unit-5
        px(im,x,15,gold if isgold else silver)
    if radiant:
        for (x,y) in [(0,1),(15,1),(0,13),(15,13),(1,0),(14,0)]: px(im,x,y,(255,250,210))
        for k in range(5): px(im,4+k*2,15,(255,240,120))
    return im
sheet=Image.new('RGBA',(W*10,W*11),(0,0,0,0))
for lv in range(100):
    sheet.paste(tier(TIERS[lv//10], lv%10),( (lv%10)*W, (lv//10)*W ))
sheet.paste(tier(TIERS[9], 0, radiant=True),(0,10*W))
sheet.save('/tmp/claude-0/lvl/nivel_patentes.png')
G=2
prev=Image.new('RGBA',(10*(W+G)+G,11*(W+G)+G),(55,55,60,255))
for r in range(11):
    for c in range(10):
        if r==10 and c>0: continue
        prev.alpha_composite(sheet.crop((c*W,r*W,c*W+W,r*W+W)),(G+c*(W+G),G+r*(W+G)))
prev.resize((prev.width*5,prev.height*5),Image.NEAREST).save('/tmp/claude-0/lvl/preview.png')

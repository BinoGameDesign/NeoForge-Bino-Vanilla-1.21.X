# Numeros em pixel art para o nivel (estilo DDTank): digitos 0-9 com volume (lado e sombra para baixo/direita) e
# brilho em cima. Uma linha de cores para cada dezena (0-9, 10-19 ... 90-99) e a ultima para o 100.
# Folha: 10 colunas (digito 0..9) x 11 linhas (estilo), celula 14x18.
from PIL import Image
D={
'0':[".####.","##..##","##..##","##..##","##..##","##..##","##..##",".####."],
'1':["..##..",".###..","####..","..##..","..##..","..##..","..##..","######"],
'2':[".####.","##..##","....##","...##.","..##..",".##...","##....","######"],
'3':[".####.","##..##","....##","..###.","....##","....##","##..##",".####."],
'4':["##..##","##..##","##..##","######","....##","....##","....##","....##"],
'5':["######","##....","##....","#####.","....##","....##","##..##",".####."],
'6':[".####.","##....","##....","#####.","##..##","##..##","##..##",".####."],
'7':["######","....##","....##","...##.","..##..","..##..","..##..","..##.."],
'8':[".####.","##..##","##..##",".####.","##..##","##..##","##..##",".####."],
'9':[".####.","##..##","##..##","##..##",".#####","....##","....##",".####."],
}
import sys
S=int(sys.argv[1]) if len(sys.argv)>1 else 2   # cada pixel do desenho vira SxS
CW,CH=6*S+2,8*S+2
# estilos: (brilho, face, lado, sombra)
# cores do Bino (27/11), do nivel mais baixo ao mais alto: uma por dezena e a ultima para o 100
FACES=[(191,191,191),(32,255,107),(32,176,255),(255,124,21),(255,191,0),(242,255,32),(143,255,32),(114,32,255),(255,64,224),(255,32,111),(255,255,255)]
def mix(a,b,k): return tuple(int(a[i]+(b[i]-a[i])*k) for i in range(3))
STY=[(mix(f,(255,255,255),0.55), f, mix(f,(0,0,0),0.28), mix(f,(0,0,0),0.55)) for f in FACES]
def glyph(d, st, rainbow=False):
    hi,face,side,shadow=st
    im=Image.new('RGBA',(CW,CH),(0,0,0,0))
    on=set()
    for y,row in enumerate(D[d]):
        for x,c in enumerate(row):
            if c=='#':
                for a in range(S):
                    for b in range(S): on.add((x*S+a,y*S+b))
    for (x,y) in on: im.putpixel((x+2,y+2),shadow+(255,))
    for (x,y) in on: im.putpixel((x+1,y+1),side+(255,))
    for (x,y) in on:
        col=face
        if rainbow:
            k=(x+y)%12
            col=[(255,120,120),(255,200,90),(255,240,120),(140,240,140),(110,200,255),(200,150,255)][k//2]
        if (x,y-1) not in on: col=hi                  # borda de cima: brilho
        elif (x-1,y) not in on: col=tuple(min(255,int(v*1.08)) for v in col)   # borda esquerda um pouco mais clara
        im.putpixel((x,y),col+(255,))
    return im
sheet=Image.new('RGBA',(CW*10,CH*11),(0,0,0,0))
for r,st in enumerate(STY):
    for c in range(10): sheet.alpha_composite(glyph(str(c),st,rainbow=False),(c*CW,r*CH))
sheet.save('tools/ui/nivel_numeros.png' if S==2 else 'src/main/resources/assets/binosvanillaplus/textures/gui/level_digits.png')
# previa: alguns niveis montados
def word(n):
    st=STY[10] if n>=100 else STY[n//10]; s=str(n)
    w=Image.new('RGBA',(len(s)*(CW-1)+1,CH),(0,0,0,0))
    for i,ch in enumerate(s): w.alpha_composite(glyph(ch,st,rainbow=False),(i*(CW-1),0))
    return w
prev=Image.new('RGBA',(CW*10+20+260,CH*11+20),(40,36,60,255))
prev.alpha_composite(sheet,(10,10))
y=10
for n in [1,7,15,23,38,42,56,64,77,85,99,100]:
    w=word(n); prev.alpha_composite(w,(CW*10+40,y)); y+=CH+2
    if y>prev.height-CH: break
prev.resize((prev.width*4,prev.height*4),Image.NEAREST).save('/tmp/claude-0/num/preview.png')

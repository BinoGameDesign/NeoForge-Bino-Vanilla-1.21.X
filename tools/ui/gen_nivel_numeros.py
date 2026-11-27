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
S=2            # cada pixel do desenho vira 2x2
CW,CH=6*S+2,8*S+2
# estilos: (brilho, face, lado, sombra)
STY=[
 ((255,255,255),(214,220,230),(150,158,172),(92,98,112)),    # 0-9   prata
 ((220,255,200),(120,214,96),(66,150,60),(36,92,40)),        # 10-19 verde
 ((210,240,255),(96,180,250),(48,110,200),(28,62,130)),      # 20-29 azul
 ((240,220,255),(176,120,240),(118,70,190),(70,36,120)),     # 30-39 roxo
 ((255,255,235),(255,226,70),(240,150,40),(180,84,30)),      # 40-49 amarelo
 ((255,236,210),(255,160,60),(220,96,30),(150,50,24)),       # 50-59 laranja
 ((255,220,220),(240,70,70),(170,36,46),(104,20,34)),        # 60-69 vermelho
 ((255,225,245),(250,110,200),(190,60,150),(120,30,96)),     # 70-79 rosa
 ((225,255,255),(80,240,230),(30,170,180),(16,100,120)),     # 80-89 ciano
 ((255,255,240),(255,214,60),(214,140,20),(140,80,10)),      # 90-99 ouro
 ((255,255,255),(255,240,150),(255,170,60),(200,60,120)),    # 100   lendario
]
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
    for c in range(10): sheet.alpha_composite(glyph(str(c),st,rainbow=(r==10)),(c*CW,r*CH))
sheet.save('/tmp/claude-0/num/nivel_numeros.png')
# previa: alguns niveis montados
def word(n):
    st=STY[10] if n>=100 else STY[n//10]; s=str(n)
    w=Image.new('RGBA',(len(s)*(CW-1)+1,CH),(0,0,0,0))
    for i,ch in enumerate(s): w.alpha_composite(glyph(ch,st,rainbow=n>=100),(i*(CW-1),0))
    return w
prev=Image.new('RGBA',(CW*10+20+260,CH*11+20),(40,36,60,255))
prev.alpha_composite(sheet,(10,10))
y=10
for n in [1,7,15,23,38,42,56,64,77,85,99,100]:
    w=word(n); prev.alpha_composite(w,(CW*10+40,y)); y+=CH+2
    if y>prev.height-CH: break
prev.resize((prev.width*4,prev.height*4),Image.NEAREST).save('/tmp/claude-0/num/preview.png')

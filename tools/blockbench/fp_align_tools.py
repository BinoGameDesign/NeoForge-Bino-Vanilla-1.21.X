# Calcula a pose de primeira pessoa padrao dos modelos 3D de ferramentas: mesma pose da ferramenta 2D vanilla
# (item/handheld: rotacao [0,-90,25], translacao [1.13,3.2,1.13], escala 0.68), com o cabo na mao e a lamina
# virada como no sprite. Escala pelo comprimento do modelo.
import json,glob,math,sys
import numpy as np
D='src/main/resources/assets/binosvanillaplus/models/item/'   # rodar da raiz do repo
def Rx(a):c,s=math.cos(a),math.sin(a);return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def Ry(a):c,s=math.cos(a),math.sin(a);return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def Rz(a):c,s=math.cos(a),math.sin(a);return np.array([[c,-s,0],[s,c,0],[0,0,1]])
def eul(rx,ry,rz):r=math.radians;return Rx(r(rx))@Ry(r(ry))@Rz(r(rz))
def dec(R):
    b=math.asin(max(-1,min(1,R[0,2]))); a=math.atan2(-R[1,2],R[2,2]); c=math.atan2(-R[0,1],R[0,0])
    return [round(math.degrees(v),1) for v in (a,b,c)]
C=np.array([8,8,8.])
RV=eul(0,-90,25); TV=np.array([1.13,3.2,1.13]); SV=0.68
GS=np.array([3.,3.,8.])        # onde a mao segura na ferramenta 2D vanilla (pixel do cabo)
d=np.array([1,1,0])/math.sqrt(2); p=np.array([-1,1,0])/math.sqrt(2); n=np.cross(d,p)
TARGET=TV+RV@(SV*(GS-C))
def elems(m): return m.get('elements',[])
def analyze(m):
    el=elems(m)
    ymin=min(e['from'][1] for e in el); ymax=max(e['to'][1] for e in el)
    gy=ymin+3
    low=[e for e in el if e['from'][1]<=ymin+1.5]
    gx=np.mean([(e['from'][0]+e['to'][0])/2 for e in low]); gz=np.mean([(e['from'][2]+e['to'][2])/2 for e in low])
    # eixo da cabeca: o que mais se espalha nas partes de cima
    top=[e for e in el if e['to'][1]>=ymax-8]
    vol=lambda e:np.prod(np.array(e['to'])-np.array(e['from']))
    def spread(i,c):
        w=sum(vol(e) for e in top); return sum(vol(e)*((e['from'][i]+e['to'][i])/2-c) for e in top)/max(w,1e-6)
    ex=max(e['to'][0] for e in top)-min(e['from'][0] for e in top); ez=max(e['to'][2] for e in top)-min(e['from'][2] for e in top)
    return ymin,ymax,np.array([gx,gy,gz]),ex,ez,spread(0,gx),spread(2,gz)
out={}
for f in sorted(glob.glob(D+'*_3d.json')):
    name=f.split('/')[-1][:-8]
    kind=name.split('_')[-1]
    if kind not in ('sword','axe','pickaxe','shovel','hoe'): continue
    m=json.load(open(f))
    ymin,ymax,g,ex,ez,sx,sz=analyze(m)
    if ez>=ex: h=np.array([0,0,1.]); sgn=sz
    else: h=np.array([1.,0,0]); sgn=sx
    if kind in ('axe','hoe') and sgn<0: h=-h          # lamina para cima-esquerda, como no sprite vanilla
    u=np.array([0,1.,0]); w=np.cross(u,h)
    RA=np.column_stack([d,p,n])@np.column_stack([u,h,w]).T
    RT=RV@RA
    L=ymax-ymin; S=round(SV*19.8/L*1.08,3)
    t=TARGET-RT@(S*(g-C))
    rot=dec(RT); assert np.allclose(eul(*rot),RT,atol=1e-2),name
    tr=[round(float(v),2) for v in t]
    TL=np.array([-1.13,3.2,1.13])+RV@(SV*(GS-C)); tl=TL-RT@(S*(g-C))
    out[name]=dict(rot=rot,tr=tr,S=S,g=g.tolist(),h=h.tolist(),rotL=[rot[0],-rot[1],-rot[2]],trL=[round(-float(tl[0]),2),round(float(tl[1]),2),round(float(tl[2]),2)])
    print(name,kind,'h',h,'grip',np.round(g,1),'L',L,'->',rot,tr,S)
json.dump(out,open('/tmp/fp_align.json','w'),indent=1)  # depois copie os valores para o display dos modelos

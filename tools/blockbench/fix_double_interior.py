# Uso: python3 tools/blockbench/fix_double_interior.py <textura nova do chest_double.png>  (rodar da raiz do repo)
# Leva o conserto do interior do bau duplo (textura nova do Bino) para os outros duplos.
import json, io, base64, sys, numpy as np
from PIL import Image
U=sys.argv[1]; C='tools/blockbench/chests/'
def tex_of(m): return Image.open(io.BytesIO(base64.b64decode(m['textures'][0]['source'].split(',',1)[1]))).convert('RGBA')
def set_tex(m,img):
    b=io.BytesIO(); img.save(b,'PNG'); m['textures'][0]['source']='data:image/png;base64,'+base64.b64encode(b.getvalue()).decode()
new=np.array(Image.open(U).convert('RGBA')).astype(int)
old=np.array(Image.open(C+'chest_double.png').convert('RGBA')).astype(int)
d=(np.abs(new-old).sum(2)>0); ys,xs=np.nonzero(d)
same=~d & (old[...,3]>0)
for n in ['chest_double','trapped_chest_double','christmas_chest_double','mimic_chest_double']:
    m=json.load(open(C+n+'.bbmodel')); v=np.array(tex_of(m)).astype(int)
    if n=='chest_double': out=new.copy()
    else:
        out=v.copy()
        # pares (cor no duplo normal -> cor nesta versao) nos pixels que nao mudaram e tem o mesmo desenho
        P=np.argwhere(same[:v.shape[0]] & (v[...,3]>0))
        src=old[P[:,0],P[:,1],:3]; dst=v[P[:,0],P[:,1],:3]
        for y,x in zip(ys,xs):
            c=new[y,x]
            if c[3]==0: out[y,x]=c; continue
            if (np.abs(v[y,x]-old[y,x]).sum()==0): out[y,x]=c; continue   # aqui esta versao e' igual ao normal
            k=np.argmin(((src-c[:3])**2).sum(1))
            out[y,x,:3]=np.clip(dst[k]+(c[:3]-src[k]),0,255); out[y,x,3]=c[3]
    img=Image.fromarray(out.astype('uint8'),'RGBA'); set_tex(m,img)
    json.dump(m,open(C+n+'.bbmodel','w')); img.save(C+n+'.png'); print(n,'ok')

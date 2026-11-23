import re, json, math, uuid, base64, io, sys
import numpy as np
from PIL import Image, ImageDraw

SRC = open('/home/claude/neoforge-bino-vanilla-1.21.x/src/main/java/com/bino/binosvanillaplus/client/combat/MoveAnims.java').read()
HX,HY,HZ,AY,AP,ER,TW,LN,SP,RL,FL,HOP,ST,CR,DX = range(15); N=15
REST=[0.56,-0.52,-0.72,-8,35,0,0,0,0,0,0,0,0,0,0]
E={'LIN':0,'W':1,'S':2,'F':3,'R':4}

def block(name):
    i = SRC.index('private static Key[] %s(CombatMove m)' % name)
    j = SRC.index('private static', i+10) if name=='sword' else SRC.index('private static float ease', i)
    return SRC[i:j]

def parse(name):
    b = block(name); out = {}
    for m in re.finditer(r'case ([A-Z_0-9]+) -> (keys\((.*?)\);|%s\(CombatMove\.([A-Z_0-9]+)\);)' % name, b, re.S):
        if m.group(4): out[m.group(1)] = ('alias', m.group(4)); continue
        body = m.group(3); keys = []
        for km in re.finditer(r'(k\(([\d.]+)f?, (\w)\)((?:\.\w\([^)]*\))*)|rest\(([^)]*)\))', body):
            v = REST[:]
            if km.group(2):
                t = float(km.group(2)); ease = E[km.group(3)]
                for c in re.finditer(r'\.(\w)\(([^)]*)\)', km.group(4)):
                    a = [float(x.strip().rstrip('f')) for x in c.group(2).split(',')]
                    idx = {'h':[HX,HY,HZ],'a':[AY,AP,ER],'b':[TW,LN],'r':[SP,RL,FL],'m':[HOP,ST,CR],'d':[DX]}[c.group(1)]
                    for ii,val in zip(idx,a): v[ii]=val
            else:
                a = [float(x.strip().rstrip('f')) for x in km.group(5).split(',')]
                t = a[0]; ease = E['R']
                if len(a) == 3: v[SP]=a[1]; v[RL]=0; v[FL]=a[2]
            keys.append((t, ease, v))
        out[m.group(1)] = [(0.0, 0, REST[:])] + keys
    for k, v in list(out.items()):
        if v[0] == 'alias': out[k] = out[v[1]]
    return out

def ease(e, a):
    if e in (1,4): return a*a*(3-2*a)
    if e == 2: return a*a*a
    if e == 3: return 1-(1-a)**3
    return a

def sample(k, u):
    if u <= k[0][0]: return k[0][2][:]
    for i in range(len(k)-1):
        if u <= k[i+1][0]:
            a = (u-k[i][0])/max(1e-4, k[i+1][0]-k[i][0]); a = ease(k[i+1][1], a)
            p0=k[max(0,i-1)][2]; p1=k[i][2]; p2=k[i+1][2]; p3=k[min(len(k)-1,i+2)][2]
            out=[]
            for j in range(N):
                cr = 0.5*(2*p1[j]+(-p0[j]+p2[j])*a+(2*p0[j]-5*p1[j]+4*p2[j]-p3[j])*a*a+(-p0[j]+3*p1[j]-3*p2[j]+p3[j])*a**3)
                lin = p1[j]+(p2[j]-p1[j])*a
                out.append(lin if j in (SP,FL) else cr)
            return out
    return k[-1][2][:]

def tpw(u):
    w = min(1, u/0.1)*min(1, (1-u)/0.22); w = max(0,min(1,w)); return w*w*(3-2*w)

# --- rotacoes (mesmas contas do JOML)
def Rx(a): c,s=math.cos(a),math.sin(a); return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def Ry(a): c,s=math.cos(a),math.sin(a); return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def Rz(a): c,s=math.cos(a),math.sin(a); return np.array([[c,-s,0],[s,c,0],[0,0,1]])
def axis(ax, a):
    ax = ax/np.linalg.norm(ax); x,y,z = ax; c,s = math.cos(a), math.sin(a); C=1-c
    return np.array([[c+x*x*C, x*y*C-z*s, x*z*C+y*s],[y*x*C+z*s, c+y*y*C, y*z*C-x*s],[z*x*C-y*s, z*y*C+x*s, c+z*z*C]])
def rotTo(f, t):
    f=f/np.linalg.norm(f); t=t/np.linalg.norm(t); v=np.cross(f,t); c=np.dot(f,t)
    if c < -0.999999:
        o = np.cross(f,[1,0,0]);
        if np.linalg.norm(o)<1e-6: o=np.cross(f,[0,0,1])
        return axis(o, math.pi)
    vx=np.array([[0,-v[2],v[1]],[v[2],0,-v[0]],[-v[1],v[0],0]])
    return np.eye(3)+vx+vx@vx*(1/(1+c))
def eulerZYX(M):   # M = Rz*Ry*Rx -> (x,y,z)
    y = math.asin(max(-1,min(1,-M[2,0]))); x = math.atan2(M[2,1], M[2,2]); z = math.atan2(M[1,0], M[0,0]); return x,y,z
def bladeDir(ay, ap): return Ry(math.radians(-ay)) @ Rx(math.radians(-ap)) @ np.array([0,1,0])

BASE_MAIN_X = -math.pi/10   # braco com item (pose do jogo)

def pose(v, w, air):
    """pose do Java -> valores do Blockbench por osso: (rot xyz em graus, pos xyz em px)"""
    side = 1
    out = {}
    d = np.array([v[HX]*side-0.30*side, v[HY]+0.45, v[HZ]])
    arm = np.array([-d[0], -d[1], d[2]]); arm/=np.linalg.norm(arm)
    bd = bladeDir(v[AY]*side, v[AP]); blade = np.array([-bd[0], -bd[1], bd[2]])
    q = rotTo(np.array([0,1,0]), arm)
    s0 = q @ np.array([0,0,-1])
    t = blade - arm*np.dot(arm, blade)
    if np.dot(t,t) > 1e-5:
        t/=np.linalg.norm(t); roll = math.atan2(np.dot(np.cross(s0,t),arm), np.dot(s0,t)); q = axis(arm, roll) @ q
    ex,ey,ez = eulerZYX(q)
    lerp = lambda w,a,b: a+(b-a)*w
    tw = math.radians(v[TW])*side
    by = lerp(w, 0, tw)
    cr = v[CR]*w
    main = (lerp(w,BASE_MAIN_X,ex), lerp(w,0,ey), lerp(w,0,ez))
    other = (lerp(w,0,-0.35-math.radians(v[LN])*0.6+tw*0.4), lerp(w,0,by), lerp(w,0,-(0.45+cr*0.4)*side))
    head = (0, -by*0.5*w, 0)
    st = v[ST]
    if not air:
        front = (lerp(w,0,-st*0.42-cr*0.25), 0, lerp(w,0,0.06*side)); back = (lerp(w,0,st*0.32+cr*0.15), 0, lerp(w,0,-0.06*side))
    else:
        front = (lerp(w,0,-0.55),0,0); back = (lerp(w,0,0.2),0,0)
    bb = lambda r: [round(-math.degrees(r[0]),2), round(-math.degrees(r[1]),2), round(math.degrees(r[2]),2)]
    out['head'] = (bb(head), [0,0,0])
    out['body'] = (bb((0,by,0)), [0,0,0])
    out['right_arm'] = (bb(main), [round(5*math.cos(by)-5,3), 0, round(5*math.sin(by),3)])
    out['left_arm'] = (bb(other), [round(5-5*math.cos(by),3), 0, round(-5*math.sin(by),3)])
    out['right_leg'] = (bb(front), [0,0,0]); out['left_leg'] = (bb(back), [0,0,0])
    out['root'] = ([round(-v[LN]*0.3*w,2), round(-v[SP],2), 0], [0, round((v[HOP]-v[CR]*0.12)*w*16,3), round(-v[DX]*w*16,3)])
    out['tumble'] = ([round(-v[FL],2), 0, round(-v[RL]*w,2)], [0,0,0])
    return out

# --- duracoes (MoveTiming, espada 1,6 ataques/s = 12,5 ticks; machado 0,9 = 22,2 ticks)
BASE = dict(LIGHT_1=420,LIGHT_2=440,LIGHT_3=460,LIGHT_4=460,HEAVY=760,JUMP=560,FORWARD=480,TEMPEST=820,JUDGMENT=1000,GUARD_BREAK=640,
    BREAK_1=400,BREAK_2=560,ROLL_SLASH=480,ROLL_THRUST=620,PARRY=320,ISSEN=560,RUSH=700,CRITICAL=900,DASH=300,DASH_SLASH=460,PIERCE=620,
    LAUNCHER=600,AIR_1=380,AIR_2=380,AIR_3=480,METEOR=700,DANCE=820,BACKSTAB=1300,LIGHT_5=520,TORMENTA=900,SURPRISE=640)
SERVER = {'HEAVY','JUDGMENT','GUARD_BREAK','ROLL_THRUST','PIERCE','LAUNCHER','METEOR','DANCE','TORMENTA','SURPRISE'}
VISUAL = {'PARRY','ISSEN','RUSH','CRITICAL','DASH','BACKSTAB'}
def length(m, delay):
    b = BASE[m]
    if m not in ('PARRY','ISSEN','RUSH','DASH','BACKSTAB'): b = int(b*max(0.65,min(1.7,(delay/12.5)**0.6)))
    if m not in SERVER and m not in VISUAL:
        cd = int(delay*50*(1.2 if m=='TEMPEST' else 1)); b = max(b, min(cd,1400))
    return b/1000.0

AIR = {'AIR_1','AIR_2','AIR_3','METEOR'}
MOVES = [('LIGHT_1','base_1','Base 1 (L)'),('LIGHT_2','base_2','Base 2 (L L)'),('LIGHT_3','base_3','Base 3 (L L L)'),
    ('LIGHT_4','base_4','Base 4 (L L L L)'),('LIGHT_5','base_5','Base 5 (L L L L L)'),
    ('SURPRISE','surpresa','Surpresa (L P)'),('GUARD_BREAK','quebra_guarda','Quebra-Guarda (L L P)'),('JUDGMENT','juizo','Juizo (L L L P)'),
    ('TORMENTA','tormenta','Tormenta (L L L L P)'),('LAUNCHER','lanca_ceu','Lanca-Ceu (Shift + P)'),
    ('AIR_1','aereo_1','Aereo 1'),('AIR_2','aereo_2','Aereo 2'),('AIR_3','aereo_3','Aereo 3'),('METEOR','meteoro','Meteoro (P no ar)'),
    ('FORWARD','investida','Investida (correndo)'),('DASH','avanco','Avanco (W W)'),('DASH_SLASH','avanco_corte','Corte apos avanco (L)'),
    ('PIERCE','perfurador','Perfurador (P apos avanco)'),('ROLL_SLASH','rolamento_corte','Corte apos rolamento (L)'),
    ('ROLL_THRUST','rolamento_estocada','Estocada apos rolamento (P)'),
    ('PARRY','aparar','Aparar (R)'),('ISSEN','issen','Issen'),('RUSH','rajada','Rajada'),('CRITICAL','critico','Golpe critico'),
    ('BACKSTAB','pelas_costas','Pelas costas')]

def U(): return str(uuid.uuid4())

def box_faces(u, v, w, h, d, tex=0):
    f = lambda x0,y0,sx,sy: {"uv":[u+x0, v+y0, u+x0+sx, v+y0+sy], "texture":tex}
    return {"north":f(d,d,w,h),"east":f(0,d,d,h),"south":f(2*d+w,d,w,h),"west":f(d+w,d,d,h),
            "up":f(d+w,d,-w,-d),"down":f(d+2*w,0,-w,d)}

def cube(name, frm, to, pivot, uv):
    w,h,d = [round(to[i]-frm[i],3) for i in range(3)]
    return {"name":name,"box_uv":True,"rescale":False,"locked":False,"render_order":"default","allow_mirror_modeling":True,
            "from":frm,"to":to,"autouv":0,"color":0,"origin":pivot,"uv_offset":uv,"faces":box_faces(uv[0],uv[1],w,h,d),
            "type":"cube","uuid":U()}

def texture(weapon):
    im = Image.new('RGBA',(64,64),(0,0,0,0)); g = ImageDraw.Draw(im)
    def r(x,y,w,h,c): g.rectangle([x,y,x+w-1,y+h-1], fill=c)
    r(0,0,32,16,(214,160,120,255)); r(8,0,8,8,(90,60,40,255)); r(0,8,32,2,(90,60,40,255))   # cabeca, cabelo
    r(8,8,8,2,(90,60,40,255)); r(9,12,2,1,(255,255,255,255)); r(10,12,1,1,(60,60,160,255)); r(13,12,2,1,(255,255,255,255)); r(13,12,1,1,(60,60,160,255))
    r(10,14,4,1,(160,90,80,255))
    r(16,16,24,16,(70,120,200,255)); r(20,20,8,12,(80,140,220,255)); r(23,20,2,12,(250,250,250,255))   # corpo (frente com faixa)
    r(40,16,16,16,(230,140,70,255)); r(44,16,4,4,(250,250,250,255))                                    # braco DIREITO (laranja)
    r(32,48,16,16,(120,200,120,255))                                                                   # braco esquerdo (verde)
    r(0,16,16,16,(50,60,120,255)); r(16,48,16,16,(90,60,140,255))                                      # perna direita / esquerda
    if weapon == 'sword':
        r(0,32,30,16,(215,225,235,255)); r(32,32,4,6,(220,180,60,255)); r(40,32,10,5,(110,70,40,255))
    else:
        r(0,32,30,15,(120,80,45,255)); r(32,32,10,9,(170,175,185,255))
    buf = io.BytesIO(); im.save(buf,'PNG')
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

def build(weapon):
    keysets = parse(weapon); delay = 12.5 if weapon=='sword' else 22.2
    G = {}
    def group(name, origin, children):
        g = {"name":name,"origin":origin,"color":0,"uuid":U(),"export":True,"mirror_uv":False,"isOpen":True,"locked":False,
             "visibility":True,"autouv":0,"children":children}
        G[name] = g; return g
    els = []
    def C(*a):
        c = cube(*a); els.append(c); return c["uuid"]
    head = group("head",[0,24,0],[C("head",[-4,24,-4],[4,32,4],[0,24,0],[0,0])])
    body = group("body",[0,24,0],[C("body",[-4,12,-2],[4,24,2],[0,24,0],[16,16])])
    rarm_children = [C("right_arm",[4,12,-2],[8,24,2],[5,22,0],[40,16])]
    if weapon == 'sword':
        item = [C("lamina",[5.5,12,-16],[6.5,14,-2],[6,13,0],[0,32]), C("guarda",[5.5,10.5,-2],[6.5,15.5,-1],[6,13,0],[32,32]),
                C("cabo",[5.5,12.5,-1],[6.5,13.5,3],[6,13,0],[40,32])]
    else:
        item = [C("cabo",[5.5,12.5,-12],[6.5,13.5,2],[6,13,0],[0,32]), C("cabeca_machado",[5.5,13.5,-12],[6.5,18.5,-8],[6,13,0],[32,32])]
    itemg = group("item",[6,13,0],item)
    rarm = group("right_arm",[5,22,0],rarm_children+[itemg])
    larm = group("left_arm",[-5,22,0],[C("left_arm",[-8,12,-2],[-4,24,2],[-5,22,0],[32,48])])
    rleg = group("right_leg",[1.9,12,0],[C("right_leg",[-0.1,0,-2],[3.9,12,2],[1.9,12,0],[0,16])])
    lleg = group("left_leg",[-1.9,12,0],[C("left_leg",[-3.9,0,-2],[0.1,12,2],[-1.9,12,0],[16,48])])
    tumble = group("tumble",[0,14.4,0],[head,body,rarm,larm,rleg,lleg])
    root = group("root",[0,0,0],[tumble])

    anims = []
    def anim(name, L, frames):
        animators = {}
        for bone in ['root','tumble','head','body','right_arm','left_arm','right_leg','left_leg']:
            kfs = []
            for t, po in frames:
                rot, pos = po[bone]
                kfs.append({"channel":"rotation","data_points":[{"x":rot[0],"y":rot[1],"z":rot[2]}],"uuid":U(),"time":t,"color":-1,"interpolation":"catmullrom","bezier_linked":True,
                            "bezier_left_time":[-0.1,-0.1,-0.1],"bezier_left_value":[0,0,0],"bezier_right_time":[0.1,0.1,0.1],"bezier_right_value":[0,0,0]})
                if any(abs(x)>1e-4 for x in pos) or bone in ('root','right_arm','left_arm'):
                    kfs.append({"channel":"position","data_points":[{"x":pos[0],"y":pos[1],"z":pos[2]}],"uuid":U(),"time":t,"color":-1,"interpolation":"catmullrom","bezier_linked":True,
                                "bezier_left_time":[-0.1,-0.1,-0.1],"bezier_left_value":[0,0,0],"bezier_right_time":[0.1,0.1,0.1],"bezier_right_value":[0,0,0]})
            animators[G[bone]["uuid"]] = {"name":bone,"type":"bone","keyframes":kfs}
        anims.append({"uuid":U(),"name":name,"loop":"once","override":False,"length":L,"snapping":100,"selected":False,
                      "anim_time_update":"","blend_weight":"","start_delay":"","loop_delay":"","animators":animators})
    summary = []
    for move, key, label in MOVES:
        k = keysets[move]; L = round(length(move, delay), 3)
        us = sorted(set([round(x[0],4) for x in k] + [round((k[i][0]+k[i+1][0])/2,4) for i in range(len(k)-1)]))
        frames = [(round(u*L,3), pose(sample(k,u), tpw(u), move in AIR)) for u in us]
        anim(key, L, frames); summary.append((key, label, L, len(us)))
    base = pose(REST, 0, False)
    anim("defender", 1.0, [(0.0, base), (1.0, base)])
    anim("carregar_pesado", 1.5, [(0.0, base), (1.5, base)])
    model = {"meta":{"format_version":"5.0","model_format":"bedrock","box_uv":True},
             "name":"combate_"+('espada' if weapon=='sword' else 'machado'),"model_identifier":"bino_player","visible_box":[1,2,0],
             "variable_placeholders":"","variable_placeholder_buttons":[],"timeline_setups":[],"unhandled_root_fields":{},
             "resolution":{"width":64,"height":64},"elements":els,"outliner":[root],
             "textures":[{"path":"","name":"jogador.png","folder":"","namespace":"","id":"0","width":64,"height":64,"uv_width":64,"uv_height":64,
                          "particle":False,"use_as_default":True,"layers_enabled":False,"sync_to_project":"","render_mode":"default","render_sides":"auto",
                          "frame_time":1,"frame_order_type":"loop","frame_order":"","frame_interpolate":False,"visible":True,"internal":True,"saved":False,
                          "uuid":U(),"relative_path":"","source":texture(weapon)}],
             "animations":anims}
    return model, summary

import os
os.makedirs('/tmp/claude-0/anim/out', exist_ok=True)
for wpn, fn in [('sword','combate_espada.bbmodel'),('axe','combate_machado.bbmodel')]:
    m, s = build(wpn)
    json.dump(m, open('/tmp/claude-0/anim/out/'+fn,'w'))
    print(fn, len(m['animations']))
    for row in s: print('  ', row)

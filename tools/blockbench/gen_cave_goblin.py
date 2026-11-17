# Gera cave_goblin.bbmodel (Blockbench 5, formato "Modded Entity", box UV 64x64) + textura + animacoes.
# Rodar: python3 tools/blockbench/gen_cave_goblin.py
import json, uuid, base64, io, random, os
from PIL import Image

OUT = os.path.dirname(os.path.abspath(__file__))
random.seed(7)
U = lambda: str(uuid.uuid4())

# ------------------------------------------------------------------ esqueleto (px, chao y=0, frente = -z)
# bone: nome -> (pai, pivo)
BONES = {
    "root":   (None,   (0, 0, 0)),
    "leg_l":  ("root", (1.5, 5, 0.5)),
    "leg_r":  ("root", (-1.5, 5, 0.5)),
    "body":   ("root", (0, 5, 0.5)),
    "cloth":  ("body", (0, 6, -2)),
    "sack":   ("body", (0, 9, 3.5)),
    "arm_l":  ("body", (4.5, 13, 1)),
    "arm_r":  ("body", (-4.5, 13, 1)),
    "head":   ("body", (0, 13, -1)),
    "jaw":    ("head", (0, 11, -3)),
    "lid_l":  ("head", (2, 15, -7)),
    "lid_r":  ("head", (-2, 15, -7)),
    "ear_l":  ("head", (4, 14.5, -4)),
    "ear_r":  ("head", (-4, 14.5, -4)),
}
# cubos: (bone, nome, from, to)
CUBES = [
    ("leg_l", "leg_l",  (0.5, 1, -0.5), (2.5, 6, 1.5)),
    ("leg_l", "foot_l", (0, 0, -2.5),   (3, 1, 1.5)),
    ("leg_r", "leg_r",  (-2.5, 1, -0.5), (-0.5, 6, 1.5)),
    ("leg_r", "foot_r", (-3, 0, -2.5),  (0, 1, 1.5)),
    ("body", "pelvis",  (-3, 5, -2),    (3, 10, 3)),
    ("body", "chest",   (-3.5, 10, -1), (3.5, 14, 4)),
    ("body", "hump",    (-2.5, 13, 1),  (2.5, 16, 5)),
    ("cloth", "cloth",  (-2.5, 3, -2.01), (2.5, 6, -2.01)),
    ("sack", "sack",    (-2, 6, 3),     (2, 10, 6)),
    ("arm_l", "arm_l",  (3.5, 4, 0),    (5.5, 13, 2)),
    ("arm_l", "hand_l", (3, 2, -0.5),   (6, 4, 2.5)),
    ("arm_r", "arm_r",  (-5.5, 4, 0),   (-3.5, 13, 2)),
    ("arm_r", "hand_r", (-6, 2, -0.5),  (-3, 4, 2.5)),
    ("head", "head",    (-4, 11, -7),   (4, 17, -1)),
    ("head", "nose",    (-1, 12, -9),   (1, 14, -7)),
    ("jaw", "jaw",      (-3, 10, -7),   (3, 11, -3)),
    ("lid_l", "lid_l",  (1, 15, -9),    (3, 16, -7)),
    ("lid_r", "lid_r",  (-3, 15, -9),   (-1, 16, -7)),
    ("ear_l", "ear_l",  (4, 14, -5),    (7, 16, -4)),
    ("ear_l", "ear_tip_l", (7, 15.5, -5), (9, 17.5, -4.5)),
    ("ear_r", "ear_r",  (-7, 14, -5),   (-4, 16, -4)),
    ("ear_r", "ear_tip_r", (-9, 15.5, -5), (-7, 17.5, -4.5)),
]

def size(c):
    f, t = c[2], c[3]
    return tuple(max(0, round(t[i] - f[i])) for i in range(3))

# ------------------------------------------------------------------ empacotar UV (box UV)
W = H = 64
order = sorted(range(len(CUBES)), key=lambda i: -(size(CUBES[i])[2] + size(CUBES[i])[1]))
uv = {}
x = y = rowh = 0
for i in order:
    w, h, d = size(CUBES[i])
    bw, bh = 2 * (w + d), d + h
    if x + bw > W:
        x, y, rowh = 0, y + rowh, 0
    uv[i] = (x, y); x += bw; rowh = max(rowh, bh)
assert y + rowh <= H, "textura cheia"

def faces_uv(i):
    u, v = uv[i]; w, h, d = size(CUBES[i])
    return {
        "east":  [u, v + d, u + d, v + d + h],
        "north": [u + d, v + d, u + d + w, v + d + h],
        "west":  [u + d + w, v + d, u + 2 * d + w, v + d + h],
        "south": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
        "up":    [u + d + w, v + d, u + d, v],
        "down":  [u + d + 2 * w, v, u + d + w, v + d],
    }

# ------------------------------------------------------------------ pintura
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
P = img.load()
SKIN = [(88, 128, 54)] * 7 + [(80, 118, 48)] * 2 + [(98, 140, 60)]
SKIN_DK = [(60, 92, 40)] * 6 + [(54, 84, 36), (66, 100, 44)]
CLOTH = [(96, 68, 42)] * 5 + [(86, 60, 36), (106, 76, 48)]
BURLAP = [(150, 120, 78)] * 5 + [(138, 108, 68), (162, 132, 88)]

def jit(pal):
    return random.choice(pal) + (255,)

def rect(r, pal, shade=0):
    x0, y0, x1, y1 = r
    xa, xb = sorted((x0, x1)); ya, yb = sorted((y0, y1))
    n = max(1, yb - ya - 1)
    for yy in range(ya, yb):
        g = 6 - round(12 * (yy - ya) / n) if yb - ya > 2 else 0   # mais claro em cima, mais escuro embaixo
        for xx in range(xa, xb):
            c = jit(pal)
            P[xx, yy] = tuple(max(0, min(255, c[k] + shade + g)) for k in range(3)) + (255,)

def px(x, y, c):
    P[x, y] = c + (255,) if len(c) == 3 else c

def box(i, pal, top=None, bottom=None, back_shade=-10):
    f = faces_uv(i)
    for k, r in f.items():
        p = pal
        if k == "up" and top: p = top
        if k == "down" and bottom: p = bottom
        rect(r, p, back_shade if k == "south" else (-18 if k == "down" else 0))
    return f

idx = {c[1]: n for n, c in enumerate(CUBES)}
for name in ("leg_l", "leg_r", "arm_l", "arm_r", "chest", "ear_l", "ear_r", "ear_tip_l", "ear_tip_r", "nose", "jaw", "lid_l", "lid_r"):
    box(idx[name], SKIN)
for name in ("foot_l", "foot_r", "hand_l", "hand_r", "hump"):
    box(idx[name], SKIN_DK)
box(idx["head"], SKIN, top=SKIN_DK)
f = box(idx["pelvis"], SKIN)
# tanga/cinto na pelvis: 2 linhas de cinto, resto pano
for k in ("north", "east", "west", "south"):
    x0, y0, x1, y1 = f[k]
    rect((x0, y0, x1, y0 + 1), [(60, 40, 24), (66, 44, 26)])           # cinto
    rect((x0, y0 + 1, x1, y1), CLOTH)
    for xx in range(min(x0, x1), max(x0, x1), 2):                       # barra rasgada
        P[xx, y1 - 1] = (0, 0, 0, 0) if k != "north" else P[xx, y1 - 1]
px(f["north"][0] + 2, f["north"][1], (180, 150, 60))                 # fivela
px(f["north"][0] + 3, f["north"][1], (180, 150, 60))
# pano da frente (aba)
f = box(idx["cloth"], CLOTH)
x0, y0, x1, y1 = f["north"]
for xx in range(x0, x1):
    if (xx * 7) % 3 == 0: P[xx, y1 - 1] = (0, 0, 0, 0)
# saco
f = box(idx["sack"], BURLAP)
for k in ("north", "south", "east", "west"):
    x0, y0, x1, y1 = f[k]
    rect((x0, y0, x1, y0 + 1), [(96, 72, 40)])                       # corda
# peito: sombra de costelas
f = faces_uv(idx["chest"])
x0, y0, x1, y1 = f["north"]
for xx in range(x0 + 1, x1 - 1):
    P[xx, y0 + 2] = SKIN_DK[1] + (255,)
# corcunda: pintas e pelos
f = faces_uv(idx["hump"])
for k in ("up", "south"):
    x0, y0, x1, y1 = f[k]
    for _ in range(4):
        P[random.randrange(min(x0, x1), max(x0, x1)), random.randrange(min(y0, y1), max(y0, y1))] = (40, 52, 30, 255)
# garras nas maos e unhas nos pes
for name in ("hand_l", "hand_r"):
    f = faces_uv(idx[name])
    for k in ("north", "east", "west", "south"):
        x0, y0, x1, y1 = f[k]
        for xx in range(min(x0, x1), max(x0, x1), 1):
            if xx % 2 == 0: P[xx, y1 - 1] = (214, 204, 170, 255)
for name in ("foot_l", "foot_r"):
    x0, y0, x1, y1 = faces_uv(idx[name])["north"]
    for xx in range(x0, x1, 2): P[xx, y0] = (214, 204, 170, 255)
# orelhas: parte interna rosada
for name in ("ear_l", "ear_r", "ear_tip_l", "ear_tip_r"):
    x0, y0, x1, y1 = faces_uv(idx[name])["north"]
    rect((x0 + (1 if x1 - x0 > 2 else 0), y0 + 1 if y1 - y0 > 1 else y0, x1 - 1 if x1 - x0 > 2 else x1, y1), [(168, 112, 104), (156, 104, 96)])
# nariz: ponta mais escura e narinas
x0, y0, x1, y1 = faces_uv(idx["nose"])["north"]
P[x0, y1 - 1] = (30, 40, 22, 255); P[x1 - 1, y1 - 1] = (30, 40, 22, 255)
# rosto (frente da cabeca, 8x6): sobrancelha, olhos amarelos com pupila em fenda, bochechas, boca com dentes
x0, y0, x1, y1 = faces_uv(idx["head"])["north"]
rect((x0, y0, x1, y0 + 1), SKIN_DK)
for cx in (x0 + 1, x0 + 5):                                          # olhos (2x2) linhas 2-3
    for dy in (2, 3):
        P[cx, y0 + dy] = (236, 206, 64, 255); P[cx + 1, y0 + dy] = (236, 206, 64, 255)
    P[cx + (1 if cx == x0 + 1 else 0), y0 + 2] = (24, 18, 10, 255)  # pupila olhando pro centro
    P[cx + (1 if cx == x0 + 1 else 0), y0 + 3] = (24, 18, 10, 255)
    P[cx + (0 if cx == x0 + 1 else 1), y0 + 2] = (255, 244, 160, 255)  # reflexo
for xx in range(x0 + 1, x1 - 1): P[xx, y0 + 5] = (34, 20, 16, 255)    # boca
P[x0 + 2, y0 + 5] = (226, 220, 190, 255); P[x1 - 3, y0 + 5] = (226, 220, 190, 255)  # presas
# mandibula: dentes na borda de cima da frente
x0, y0, x1, y1 = faces_uv(idx["jaw"])["north"]
for xx in range(x0, x1): P[xx, y0] = (34, 20, 16, 255)
P[x0 + 1, y0] = (226, 220, 190, 255); P[x1 - 2, y0] = (226, 220, 190, 255)
# palpebras: frente = sobrancelha escura; face de cima (vira a frente quando fecha) = palpebra com cilio embaixo
for name in ("lid_l", "lid_r"):
    f = faces_uv(idx[name])
    rect(f["north"], SKIN_DK)
    rect(f["up"], SKIN, 6)
    xa, ya, xb, yb = f["up"]
    for xx in range(min(xa, xb), max(xa, xb)): P[xx, max(ya, yb) - 1] = (28, 36, 20, 255)

buf = io.BytesIO(); img.save(buf, "PNG")
img.save(os.path.join(OUT, "cave_goblin.png"))
TEX_B64 = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

# ------------------------------------------------------------------ elementos e outliner
bone_uuid = {b: U() for b in BONES}
elements, children = [], {b: [] for b in BONES}
for n, (bone, name, fr, to) in enumerate(CUBES):
    e = {"name": name, "box_uv": True, "render_order": "default", "rescale": False, "locked": False,
         "allow_mirror_modeling": True, "from": list(fr), "to": list(to), "autouv": 0, "color": n % 8,
         "origin": list(BONES[bone][1]), "uv_offset": list(uv[n]),
         "faces": {k: {"uv": v, "texture": 0} for k, v in faces_uv(n).items()},
         "type": "cube", "uuid": U()}
    elements.append(e); children[bone].append(e["uuid"])
for b, (parent, _) in BONES.items():
    if parent: children[parent].append(b)
groups = [{"name": b, "origin": list(p), "color": 0, "uuid": bone_uuid[b], "export": True, "mirror_uv": False,
           "isOpen": True, "locked": False, "visibility": True, "autouv": 0, "rotation": [0, 0, 0], "children": []}
          for b, (_, p) in BONES.items()]
def tree(b):
    return {"uuid": bone_uuid[b], "isOpen": True,
            "children": [tree(c) if c in BONES else c for c in children[b]]}
outliner = [tree("root")]

# ------------------------------------------------------------------ animacoes
# Convencao (Blockbench/Bedrock): rotacao X positiva = frente desce (inclinar pra frente / olhar pra baixo / perna vai pra tras / braco vai pra tras)
#                                 orelha_l: Z+ levanta, Y+ deita pra tras; orelha_r: Z- levanta, Y- deita pra tras
def anim(name, length, loop, tracks):
    animators = {}
    for bone, chans in tracks.items():
        kfs = []
        for ch, pts in chans.items():
            for p in pts:
                t, v = p[0], p[1]
                interp = p[2] if len(p) > 2 else "catmullrom"
                kfs.append({"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}],
                            "uuid": U(), "time": t, "color": -1, "interpolation": interp})
        animators[bone_uuid[bone]] = {"name": bone, "type": "bone", "keyframes": kfs}
    return {"uuid": U(), "name": "animation.cave_goblin." + name, "loop": loop, "override": False,
            "length": length, "snapping": 24, "selected": False, "anim_time_update": "", "blend_weight": "",
            "start_delay": "", "loop_delay": "", "animators": animators}

def cyc(length, a, b, n=2, axis=0, phase=0.0):
    """oscila entre a e b num eixo; n = quantos meios-ciclos"""
    pts = []
    for k in range(n + 1):
        t = round(length * k / n, 4)
        val = a if (k + phase) % 2 == 0 else b
        v = [0, 0, 0]; v[axis] = val
        pts.append((t, v))
    return pts

R = "rotation"; Pz = "position"; S = "scale"
A = []

# 1. parado: respiracao, corpo balanca, orelha tremendo, maos coçando
A.append(anim("idle", 3.0, "loop", {
    "body":  {R: [(0, [12, 0, 0]), (1.5, [15, 0, 0]), (3, [12, 0, 0])], Pz: [(0, [0, 0, 0]), (1.5, [0, -0.3, 0]), (3, [0, 0, 0])]},
    "head":  {R: [(0, [-10, 0, 0]), (0.8, [-12, 8, 0]), (1.6, [-9, -6, 3]), (2.4, [-11, 4, 0]), (3, [-10, 0, 0])]},
    "chest" if False else "arm_l": {R: [(0, [-6, 0, -4]), (1.5, [-9, 0, -6]), (3, [-6, 0, -4])]},
    "arm_r": {R: [(0, [-6, 0, 4]), (1.5, [-9, 0, 6]), (3, [-6, 0, 4])]},
    "ear_l": {R: [(0, [0, 0, 8]), (1.9, [0, 0, 8]), (2.0, [0, 0, 22]), (2.1, [0, 0, 6]), (2.2, [0, 0, 16]), (2.35, [0, 0, 8]), (3, [0, 0, 8])]},
    "ear_r": {R: [(0, [0, 0, -8]), (3, [0, 0, -8])]},
    "sack":  {R: [(0, [0, 0, 0]), (1.5, [3, 0, 0]), (3, [0, 0, 0])]},
}))
# 2. piscar (sobrepor a qualquer animacao): piscada simples + piscada dupla
A.append(anim("blink", 5.0, "loop", {
    "lid_l": {R: [(0, [0, 0, 0], "linear"), (2.0, [0, 0, 0], "linear"), (2.06, [90, 0, 0], "linear"), (2.16, [90, 0, 0], "linear"), (2.24, [0, 0, 0], "linear"),
                  (4.3, [0, 0, 0], "linear"), (4.36, [90, 0, 0], "linear"), (4.42, [0, 0, 0], "linear"), (4.5, [90, 0, 0], "linear"), (4.58, [0, 0, 0], "linear"), (5, [0, 0, 0], "linear")]},
    "lid_r": {R: [(0, [0, 0, 0], "linear"), (2.0, [0, 0, 0], "linear"), (2.06, [90, 0, 0], "linear"), (2.16, [90, 0, 0], "linear"), (2.24, [0, 0, 0], "linear"),
                  (4.3, [0, 0, 0], "linear"), (4.36, [90, 0, 0], "linear"), (4.42, [0, 0, 0], "linear"), (4.5, [90, 0, 0], "linear"), (4.58, [0, 0, 0], "linear"), (5, [0, 0, 0], "linear")]},
}))
# 3. olhar em volta (nervoso)
A.append(anim("look_around", 2.4, "once", {
    "head":  {R: [(0, [-10, 0, 0]), (0.35, [-14, 40, 4]), (0.9, [-14, 40, 4]), (1.15, [-12, -42, -4]), (1.75, [-12, -42, -4]), (2.4, [-10, 0, 0])]},
    "body":  {R: [(0, [12, 0, 0]), (0.35, [12, 10, 0]), (1.15, [12, -10, 0]), (2.4, [12, 0, 0])]},
    "ear_l": {R: [(0, [0, 0, 8]), (0.3, [0, 0, 28]), (1.2, [0, 0, 28]), (2.4, [0, 0, 8])]},
    "ear_r": {R: [(0, [0, 0, -8]), (0.3, [0, 0, -28]), (1.2, [0, 0, -28]), (2.4, [0, 0, -8])]},
}))
# 4. andar normal (gingado corcunda)
L = 1.0
A.append(anim("walk", L, "loop", {
    "leg_l": {R: cyc(L, -28, 28)}, "leg_r": {R: cyc(L, 28, -28)},
    "arm_l": {R: [(0, [24, 0, -6]), (0.5, [-24, 0, -6]), (1, [24, 0, -6])]},
    "arm_r": {R: [(0, [-24, 0, 6]), (0.5, [24, 0, 6]), (1, [-24, 0, 6])]},
    "body":  {R: [(0, [14, 0, -5]), (0.5, [14, 0, 5]), (1, [14, 0, -5])], Pz: [(0, [0, 0, 0]), (0.25, [0, 0.6, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.6, 0]), (1, [0, 0, 0])]},
    "head":  {R: [(0, [-12, 0, 5]), (0.5, [-12, 0, -5]), (1, [-12, 0, 5])]},
    "ear_l": {R: [(0, [0, 6, 6]), (0.25, [0, 10, 0]), (0.5, [0, 6, 6]), (0.75, [0, 10, 0]), (1, [0, 6, 6])]},
    "ear_r": {R: [(0, [0, -6, -6]), (0.25, [0, -10, 0]), (0.5, [0, -6, -6]), (0.75, [0, -10, 0]), (1, [0, -6, -6])]},
    "sack":  {R: [(0, [0, 0, 6]), (0.5, [0, 0, -6]), (1, [0, 0, 6])]},
}))
# 5. andar sorrateiro (pe ante pe, agachado, maos a frente, orelhas deitadas)
L = 1.8
A.append(anim("sneak", L, "loop", {
    "body":  {R: [(0, [32, 0, -3]), (0.9, [32, 0, 3]), (1.8, [32, 0, -3])], Pz: [(0, [0, -1.6, 0]), (0.45, [0, -1.2, 0]), (0.9, [0, -1.6, 0]), (1.35, [0, -1.2, 0]), (1.8, [0, -1.6, 0])]},
    "head":  {R: [(0, [-30, 0, 0]), (0.9, [-28, 6, 0]), (1.8, [-30, 0, 0])]},
    "leg_l": {R: [(0, [-30, 0, 0]), (0.3, [-55, 0, 0]), (0.9, [10, 0, 0]), (1.8, [-30, 0, 0])]},
    "leg_r": {R: [(0, [10, 0, 0]), (0.9, [-30, 0, 0]), (1.2, [-55, 0, 0]), (1.8, [10, 0, 0])]},
    "arm_l": {R: [(0, [-55, -10, -8]), (0.45, [-62, -10, -6]), (0.9, [-55, -10, -8]), (1.35, [-48, -10, -6]), (1.8, [-55, -10, -8])]},
    "arm_r": {R: [(0, [-48, 10, 8]), (0.45, [-55, 10, 6]), (0.9, [-62, 10, 8]), (1.35, [-55, 10, 6]), (1.8, [-48, 10, 8])]},
    "ear_l": {R: [(0, [0, 35, -10]), (1.8, [0, 35, -10])]},
    "ear_r": {R: [(0, [0, -35, 10]), (1.8, [0, -35, 10])]},
    "sack":  {R: [(0, [-8, 0, 0]), (1.8, [-8, 0, 0])]},
}))
# 6. espreita (parado agachado, esfregando as maos, cabeca acompanhando o alvo)
L = 2.4
A.append(anim("lurk", L, "loop", {
    "body":  {R: [(0, [38, 0, 0]), (1.2, [40, 0, 0]), (2.4, [38, 0, 0])], Pz: [(0, [0, -2.2, 0]), (1.2, [0, -2.4, 0]), (2.4, [0, -2.2, 0])]},
    "head":  {R: [(0, [-36, 0, 0]), (0.6, [-38, 6, -6]), (1.2, [-36, 0, 0]), (1.8, [-38, -6, 6]), (2.4, [-36, 0, 0])]},
    "leg_l": {R: [(0, [-60, 0, 0]), (2.4, [-60, 0, 0])]},
    "leg_r": {R: [(0, [-40, 0, 0]), (2.4, [-40, 0, 0])]},
    "arm_l": {R: [(0, [-70, -25, -4]), (0.15, [-74, -30, -4]), (0.3, [-70, -25, -4]), (0.45, [-74, -30, -4]), (0.6, [-70, -25, -4]), (2.4, [-70, -25, -4])]},
    "arm_r": {R: [(0, [-70, 25, 4]), (0.15, [-66, 20, 4]), (0.3, [-70, 25, 4]), (0.45, [-66, 20, 4]), (0.6, [-70, 25, 4]), (2.4, [-70, 25, 4])]},
    "ear_l": {R: [(0, [0, 30, -6]), (1.6, [0, 30, -6]), (1.7, [0, 22, 6]), (1.85, [0, 30, -6]), (2.4, [0, 30, -6])]},
    "ear_r": {R: [(0, [0, -30, 6]), (2.4, [0, -30, 6])]},
    "jaw":   {R: [(0, [0, 0, 0]), (0.9, [0, 0, 0]), (1.0, [8, 0, 0]), (1.3, [8, 0, 0]), (1.4, [0, 0, 0]), (2.4, [0, 0, 0])]},
}))
# 7. fuga correndo (desesperado, inclinado, bracos agitando pra tras, saco pulando)
L = 0.5
A.append(anim("flee", L, "loop", {
    "leg_l": {R: cyc(L, -60, 55)}, "leg_r": {R: cyc(L, 55, -60)},
    "arm_l": {R: [(0, [40, 0, -30]), (0.25, [70, 0, -45]), (0.5, [40, 0, -30])]},
    "arm_r": {R: [(0, [70, 0, 45]), (0.25, [40, 0, 30]), (0.5, [70, 0, 45])]},
    "body":  {R: [(0, [36, 0, -4]), (0.25, [36, 0, 4]), (0.5, [36, 0, -4])], Pz: [(0, [0, 0, 0]), (0.125, [0, 1.2, 0]), (0.25, [0, 0, 0]), (0.375, [0, 1.2, 0]), (0.5, [0, 0, 0])]},
    "head":  {R: [(0, [-30, 0, 0]), (0.125, [-24, 0, 0]), (0.25, [-30, 0, 0]), (0.375, [-24, 0, 0]), (0.5, [-30, 0, 0])]},
    "jaw":   {R: [(0, [22, 0, 0]), (0.5, [22, 0, 0])]},
    "ear_l": {R: [(0, [0, 45, 0]), (0.125, [0, 55, 8]), (0.25, [0, 45, 0]), (0.375, [0, 55, 8]), (0.5, [0, 45, 0])]},
    "ear_r": {R: [(0, [0, -45, 0]), (0.125, [0, -55, -8]), (0.25, [0, -45, 0]), (0.375, [0, -55, -8]), (0.5, [0, -45, 0])]},
    "sack":  {R: [(0, [-10, 0, 8]), (0.125, [-30, 0, 0]), (0.25, [-10, 0, -8]), (0.375, [-30, 0, 0]), (0.5, [-10, 0, 8])]},
    "cloth": {R: [(0, [-20, 0, 0]), (0.125, [-40, 0, 0]), (0.25, [-20, 0, 0]), (0.375, [-40, 0, 0]), (0.5, [-20, 0, 0])]},
}))
# 8. roubar (bote rapido com a mao direita e recolhe pro saco)
A.append(anim("steal", 0.9, "once", {
    "body":  {R: [(0, [38, 0, 0]), (0.15, [48, -15, 0]), (0.35, [50, -15, 0]), (0.55, [30, 20, 0]), (0.9, [20, 0, 0])], Pz: [(0, [0, -2.2, 0]), (0.15, [0, -2.0, 0]), (0.55, [0, 0.5, 0]), (0.9, [0, 0, 0])]},
    "arm_r": {R: [(0, [-70, 25, 4]), (0.15, [-110, -5, -6]), (0.35, [-105, -5, -6]), (0.6, [30, 40, 40]), (0.75, [40, 40, 50]), (0.9, [-10, 0, 6])]},
    "arm_l": {R: [(0, [-70, -25, -4]), (0.15, [-30, 0, -20]), (0.6, [-20, 0, -15]), (0.9, [-10, 0, -6])]},
    "head":  {R: [(0, [-36, 0, 0]), (0.15, [-30, -15, 0]), (0.55, [-20, 25, 0]), (0.9, [-14, 0, 0])]},
    "jaw":   {R: [(0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.6, [14, 0, 0]), (0.9, [0, 0, 0])]},
    "sack":  {R: [(0, [0, 0, 0]), (0.65, [0, 0, 0]), (0.75, [-20, 0, 0]), (0.9, [0, 0, 0])]},
    "ear_l": {R: [(0, [0, 30, -6]), (0.5, [0, 0, 30]), (0.9, [0, 0, 8])]},
    "ear_r": {R: [(0, [0, -30, 6]), (0.5, [0, 0, -30]), (0.9, [0, 0, -8])]},
}))
# 9. risadinha depois do furto (ombros chacoalhando, boca abrindo)
A.append(anim("giggle", 1.0, "once", {
    "body":  {R: [(0, [10, 0, 0]), (0.1, [6, 0, 0]), (0.2, [10, 0, 0]), (0.3, [6, 0, 0]), (0.4, [10, 0, 0]), (0.5, [6, 0, 0]), (0.6, [10, 0, 0]), (1, [12, 0, 0])]},
    "head":  {R: [(0, [-20, 0, 0]), (0.1, [-26, 0, 4]), (0.2, [-20, 0, -4]), (0.3, [-26, 0, 4]), (0.4, [-20, 0, -4]), (0.5, [-26, 0, 4]), (0.6, [-20, 0, 0]), (1, [-10, 0, 0])]},
    "jaw":   {R: [(0, [0, 0, 0]), (0.1, [18, 0, 0]), (0.2, [4, 0, 0]), (0.3, [18, 0, 0]), (0.4, [4, 0, 0]), (0.5, [18, 0, 0]), (0.6, [0, 0, 0]), (1, [0, 0, 0])]},
    "arm_l": {R: [(0, [-40, -30, -10]), (0.6, [-40, -30, -10]), (1, [-6, 0, -4])]},
    "arm_r": {R: [(0, [-40, 30, 10]), (0.6, [-40, 30, 10]), (1, [-6, 0, 4])]},
}))
# 10. susto (pulo pra cima, orelhas em pe, bracos pro alto, depois encolhe)
A.append(anim("startled", 0.7, "once", {
    "root":  {Pz: [(0, [0, 0, 0]), (0.12, [0, 3, 0]), (0.3, [0, 0, 0]), (0.7, [0, 0, 0])]},
    "body":  {R: [(0, [12, 0, 0]), (0.12, [-10, 0, 0]), (0.35, [30, 0, 0]), (0.7, [24, 0, 0])]},
    "head":  {R: [(0, [-10, 0, 0]), (0.12, [10, 0, 0]), (0.35, [-30, 0, 0]), (0.7, [-24, 0, 0])]},
    "arm_l": {R: [(0, [-6, 0, -4]), (0.12, [-150, 0, -30]), (0.35, [-80, -30, -10]), (0.7, [-80, -30, -10])]},
    "arm_r": {R: [(0, [-6, 0, 4]), (0.12, [-150, 0, 30]), (0.35, [-80, 30, 10]), (0.7, [-80, 30, 10])]},
    "leg_l": {R: [(0, [0, 0, 0]), (0.12, [-20, 0, -10]), (0.3, [0, 0, 0])]},
    "leg_r": {R: [(0, [0, 0, 0]), (0.12, [-20, 0, 10]), (0.3, [0, 0, 0])]},
    "jaw":   {R: [(0, [0, 0, 0]), (0.1, [25, 0, 0]), (0.5, [10, 0, 0]), (0.7, [0, 0, 0])]},
    "ear_l": {R: [(0, [0, 0, 8]), (0.1, [0, 0, 45]), (0.7, [0, 0, 30])]},
    "ear_r": {R: [(0, [0, 0, -8]), (0.1, [0, 0, -45]), (0.7, [0, 0, -30])]},
    "lid_l": {R: [(0, [0, 0, 0]), (0.7, [0, 0, 0])]},
}))
# 11. encolhido de medo (tremendo, quando encurralado por mob/jogador)
A.append(anim("cower", 0.4, "loop", {
    "body":  {R: [(0, [45, 0, -2]), (0.1, [45, 0, 2]), (0.2, [45, 0, -2]), (0.3, [45, 0, 2]), (0.4, [45, 0, -2])], Pz: [(0, [0, -2.6, 0]), (0.4, [0, -2.6, 0])]},
    "head":  {R: [(0, [10, 0, 0]), (0.4, [10, 0, 0])]},
    "arm_l": {R: [(0, [-140, -30, 20]), (0.2, [-136, -30, 22]), (0.4, [-140, -30, 20])]},
    "arm_r": {R: [(0, [-140, 30, -20]), (0.2, [-136, 30, -22]), (0.4, [-140, 30, -20])]},
    "leg_l": {R: [(0, [-70, 0, 0]), (0.4, [-70, 0, 0])]},
    "leg_r": {R: [(0, [-70, 0, 0]), (0.4, [-70, 0, 0])]},
    "ear_l": {R: [(0, [0, 40, -30]), (0.4, [0, 40, -30])]},
    "ear_r": {R: [(0, [0, -40, 30]), (0.4, [0, -40, 30])]},
    "lid_l": {R: [(0, [70, 0, 0]), (0.4, [70, 0, 0])]},
    "lid_r": {R: [(0, [70, 0, 0]), (0.4, [70, 0, 0])]},
}))
# 12. comer (agachado, leva comida a boca com as duas maos e mastiga)
L = 1.4
A.append(anim("eat", L, "loop", {
    "body":  {R: [(0, [30, 0, 0]), (0.7, [34, 0, 0]), (1.4, [30, 0, 0])], Pz: [(0, [0, -2.4, 0]), (1.4, [0, -2.4, 0])]},
    "leg_l": {R: [(0, [-75, -10, 0]), (1.4, [-75, -10, 0])]},
    "leg_r": {R: [(0, [-75, 10, 0]), (1.4, [-75, 10, 0])]},
    "arm_l": {R: [(0, [-90, -30, 0]), (0.25, [-120, -35, 0]), (0.6, [-110, -32, 0]), (0.9, [-90, -30, 0]), (1.4, [-90, -30, 0])]},
    "arm_r": {R: [(0, [-90, 30, 0]), (0.25, [-120, 35, 0]), (0.6, [-110, 32, 0]), (0.9, [-90, 30, 0]), (1.4, [-90, 30, 0])]},
    "head":  {R: [(0, [-6, 0, 0]), (0.3, [6, 0, 0]), (0.5, [0, 0, 3]), (0.7, [6, 0, 0]), (0.9, [0, 0, -3]), (1.1, [6, 0, 0]), (1.4, [-6, 0, 0])]},
    "jaw":   {R: [(0, [0, 0, 0]), (0.25, [22, 0, 0]), (0.4, [0, 0, 0]), (0.55, [14, 0, 0]), (0.7, [0, 0, 0]), (0.85, [14, 0, 0]), (1.0, [0, 0, 0]), (1.15, [12, 0, 0]), (1.3, [0, 0, 0]), (1.4, [0, 0, 0])]},
    "ear_l": {R: [(0, [0, 0, 10]), (0.5, [0, 0, 18]), (1.0, [0, 0, 10]), (1.4, [0, 0, 10])]},
    "ear_r": {R: [(0, [0, 0, -10]), (0.5, [0, 0, -18]), (1.0, [0, 0, -10]), (1.4, [0, 0, -10])]},
}))
# 13. colocar o anel (gira o anel no dedo, olha pros lados e some — o codigo aplica a invisibilidade no fim)
A.append(anim("ring_on", 1.2, "once", {
    "body":  {R: [(0, [14, 0, 0]), (0.4, [24, 0, 0]), (1.2, [30, 0, 0])], Pz: [(0, [0, 0, 0]), (1.2, [0, -1.6, 0])]},
    "arm_l": {R: [(0, [-6, 0, -4]), (0.3, [-80, -40, 0]), (0.8, [-80, -40, 0]), (1.2, [-55, -10, -8])]},
    "arm_r": {R: [(0, [-6, 0, 4]), (0.3, [-80, 45, 0]), (0.45, [-84, 50, 0]), (0.6, [-80, 45, 0]), (0.75, [-84, 50, 0]), (1.2, [-48, 10, 8])]},
    "head":  {R: [(0, [-10, 0, 0]), (0.3, [15, 0, 0]), (0.8, [15, 0, 0]), (0.95, [-20, 30, 0]), (1.1, [-20, -30, 0]), (1.2, [-30, 0, 0])]},
    "jaw":   {R: [(0, [0, 0, 0]), (0.85, [0, 0, 0]), (0.95, [10, 0, 0]), (1.2, [0, 0, 0])]},
    "ear_l": {R: [(0, [0, 0, 8]), (1.0, [0, 0, 8]), (1.2, [0, 35, -10])]},
    "ear_r": {R: [(0, [0, 0, -8]), (1.0, [0, 0, -8]), (1.2, [0, -35, 10])]},
}))

model = {
    "meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True},
    "name": "cave_goblin", "model_identifier": "cave_goblin", "modded_entity_version": "1.17",
    "modded_entity_flip_y": True, "visible_box": [1, 1.1, 0.55], "variable_placeholders": "",
    "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
    "resolution": {"width": W, "height": H},
    "elements": elements, "groups": groups, "outliner": outliner,
    "textures": [{"path": "", "name": "cave_goblin.png", "folder": "", "namespace": "", "id": "0",
                  "width": W, "height": H, "uv_width": W, "uv_height": H, "particle": False,
                  "render_mode": "default", "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop",
                  "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": False,
                  "uuid": U(), "source": TEX_B64}],
    "animations": A,
}
json.dump(model, open(os.path.join(OUT, "cave_goblin.bbmodel"), "w"))
print("ok", len(elements), "cubos,", len(A), "animacoes, uv", uv)

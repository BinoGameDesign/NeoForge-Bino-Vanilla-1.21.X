# Gera rat.bbmodel (Blockbench 5, Modded Entity, box UV 64x32) + rat.png + rat_eyes.png (olhos luminosos) + rat_blink.png (olhos fechados).
# Convencao das rotacoes (como o Blockbench mostra): X+ = frente sobe (cabeca olha pra cima / perna vai pra frente / cauda desce);
# Y+ = frente vira para -x; Z+ = lado +x sobe.   Rodar: python3 tools/blockbench/gen_rat.py
import json, uuid, base64, io, random, os
from PIL import Image
OUT = os.path.dirname(os.path.abspath(__file__)); random.seed(11); U = lambda: str(uuid.uuid4())

BONES = {   # nome: (pai, pivo)
    "root":     (None, (0, 0, 0)),
    "leg_bl":   ("root", (1.8, 3.5, 3.5)),
    "leg_br":   ("root", (-1.8, 3.5, 3.5)),
    "body":     ("root", (0, 3, 3.5)),
    "head":     ("body", (0, 4, -1.5)),
    "whiskers": ("head", (0, 3.5, -6.5)),
    "ear_l":    ("head", (1.2, 5.5, -3)),
    "ear_r":    ("head", (-1.2, 5.5, -3)),
    "leg_fl":   ("body", (1.2, 3, -1)),
    "leg_fr":   ("body", (-1.2, 3, -1)),
    "tail":     ("body", (0, 3, 5)),
    "tail_tip": ("tail", (0, 3, 9)),
}
CUBES = [   # (osso, nome, from, to, inflate)
    ("body", "chest", (-2, 2, -2), (2, 5, 2), 0),
    ("body", "rump", (-2.5, 1, 1), (2.5, 5, 5), 0),
    ("head", "head", (-1.5, 2.5, -5.5), (1.5, 5.5, -1.5), 0),
    ("head", "snout", (-1, 2.5, -7.5), (1, 4.5, -5.5), 0),
    ("whiskers", "whiskers", (-3, 3, -6.5), (3, 4, -6.5), 0),
    ("ear_l", "ear_l", (0.7, 5.5, -3.5), (2.7, 7.5, -2.5), 0),
    ("ear_r", "ear_r", (-2.7, 5.5, -3.5), (-0.7, 7.5, -2.5), 0),
    ("leg_fl", "leg_fl", (0.7, 0, -1.5), (1.7, 3, -0.5), -0.05),
    ("leg_fl", "paw_fl", (0.7, 0, -2.5), (1.7, 1, -0.5), 0.05),
    ("leg_fr", "leg_fr", (-1.7, 0, -1.5), (-0.7, 3, -0.5), -0.05),
    ("leg_fr", "paw_fr", (-1.7, 0, -2.5), (-0.7, 1, -0.5), 0.05),
    ("leg_bl", "thigh_bl", (0.8, 1, 2), (2.8, 4, 5), 0),
    ("leg_bl", "foot_bl", (1.3, 0, 1.5), (2.3, 1, 4.5), 0),
    ("leg_br", "thigh_br", (-2.8, 1, 2), (-0.8, 4, 5), 0),
    ("leg_br", "foot_br", (-2.3, 0, 1.5), (-1.3, 1, 4.5), 0),
    ("tail", "tail", (-0.5, 2.5, 5), (0.5, 3.5, 9), -0.1),
    ("tail_tip", "tail_tip", (-0.5, 2.5, 9), (0.5, 3.5, 13), -0.2),
]
size = lambda c: tuple(round(c[3][i] - c[2][i]) for i in range(3))
W, H = 64, 32
order = sorted(range(len(CUBES)), key=lambda i: -(size(CUBES[i])[2] + size(CUBES[i])[1]))
uv = {}; x = y = rowh = 0
for i in order:
    w, h, d = size(CUBES[i]); bw, bh = 2 * (w + d), d + h
    if x + bw > W: x, y, rowh = 0, y + rowh, 0
    uv[i] = (x, y); x += bw; rowh = max(rowh, bh)
assert y + rowh <= H
def faces_uv(i):
    u, v = uv[i]; w, h, d = size(CUBES[i])
    return {"east": [u, v + d, u + d, v + d + h], "north": [u + d, v + d, u + d + w, v + d + h],
            "west": [u + d + w, v + d, u + 2 * d + w, v + d + h], "south": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h],
            "up": [u + d + w, v + d, u + d, v], "down": [u + d + 2 * w, v, u + d + w, v + d]}
idx = {c[1]: n for n, c in enumerate(CUBES)}

# ------------------------------------------------------------------ pintura
img = Image.new("RGBA", (W, H)); P = img.load()
FUR = [(118, 112, 108)] * 6 + [(108, 102, 98), (128, 122, 116), (100, 95, 92)]
FUR_DK = [(86, 81, 78)] * 5 + [(78, 74, 71), (94, 89, 85)]
BELLY = [(168, 160, 150)] * 5 + [(158, 150, 140)]
PINK = [(206, 150, 146)] * 4 + [(194, 138, 134)]
def norm(r):
    x0, y0, x1, y1 = r; return min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1)
def rect(r, pal, shade=0):
    xa, ya, xb, yb = norm(r); n = max(1, yb - ya - 1)
    for yy in range(ya, yb):
        g = 6 - round(12 * (yy - ya) / n) if yb - ya > 2 else 0
        for xx in range(xa, xb):
            c = random.choice(pal); P[xx, yy] = tuple(max(0, min(255, c[k] + shade + g)) for k in range(3)) + (255,)
def box(name, pal, top=None, bottom=None):
    f = faces_uv(idx[name])
    for k, r in f.items():
        rect(r, top if (k == "up" and top) else bottom if (k == "down" and bottom) else pal, -10 if k == "south" else 0)
    return f
for n in ("chest", "rump", "head"): box(n, FUR, top=FUR_DK, bottom=BELLY)
box("snout", FUR, bottom=BELLY)
for n in ("ear_l", "ear_r"):
    f = box(n, PINK, top=FUR); rect(f["south"], FUR_DK)                      # costas da orelha com pelo
for n in ("leg_fl", "leg_fr", "thigh_bl", "thigh_br"): box(n, FUR, bottom=PINK)
for n in ("paw_fl", "paw_fr", "foot_bl", "foot_br"): box(n, PINK)
for n in ("tail", "tail_tip"): box(n, PINK, top=[(190, 136, 132)])
# barriga e costas
for n in ("chest", "rump"):
    f = faces_uv(idx[n]); xa, ya, xb, yb = norm(f["up"])
    for xx in range(xa, xb): P[xx, ya + (yb - ya) // 2] = (90, 85, 82, 255)  # listra escura no dorso
# focinho: nariz rosa e boquinha
xa, ya, xb, yb = norm(faces_uv(idx["snout"])["north"])
P[xa, ya] = (226, 140, 150, 255); P[xb - 1, ya] = (226, 140, 150, 255)
for xx in range(xa, xb): P[xx, yb - 1] = (60, 50, 50, 255)
# bigodes (plano): so os fios, resto transparente
xa, ya, xb, yb = norm(faces_uv(idx["whiskers"])["north"])
for xx in range(xa, xb): P[xx, ya] = (0, 0, 0, 0)
for xx in list(range(xa, xa + 2)) + list(range(xb - 2, xb)): P[xx, ya] = (220, 216, 210, 255)
for k in ("south",):
    xa2, ya2, xb2, yb2 = norm(faces_uv(idx["whiskers"])[k])
    for xx in range(xa2, xb2): P[xx, ya2] = P[xb - 1 - (xx - xa2), ya] if xx - xa2 < xb - xa else (0, 0, 0, 0)
# olhos: laterais da cabeca (east = +x, coluna da direita e a frente; west = -x, coluna da esquerda e a frente)
EYE, GLOW = (232, 30, 30, 255), (255, 120, 110, 255)
eyes = []
f = faces_uv(idx["head"])
xa, ya, xb, yb = norm(f["east"]); eyes += [(xb - 2, ya), (xb - 2, ya + 1)]
xa, ya, xb, yb = norm(f["west"]); eyes += [(xa + 1, ya), (xa + 1, ya + 1)]
for (ex, ey) in eyes: P[ex, ey] = EYE
P[eyes[0]] = GLOW; P[eyes[2]] = GLOW
img.save(os.path.join(OUT, "rat.png"))
# camada luminosa (so os olhos) e piscada (olhos fechados = pelo escuro)
eyes_img = Image.new("RGBA", (W, H))
for e in eyes: eyes_img.putpixel(e, img.getpixel(e))
eyes_img.save(os.path.join(OUT, "rat_eyes.png"))
blink = img.copy()
for (ex, ey) in eyes: blink.putpixel((ex, ey), (70, 66, 64, 255) if ey == eyes[0][1] or ey == eyes[2][1] else (96, 91, 88, 255))
blink.save(os.path.join(OUT, "rat_blink.png"))
buf = io.BytesIO(); img.save(buf, "PNG"); TEX = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

# ------------------------------------------------------------------ estrutura
bone_uuid = {b: U() for b in BONES}; children = {b: [] for b in BONES}; elements = []
for n, (bone, name, fr, to, inf) in enumerate(CUBES):
    e = {"name": name, "box_uv": True, "render_order": "default", "rescale": False, "locked": False, "allow_mirror_modeling": True,
         "from": list(fr), "to": list(to), "autouv": 0, "color": n % 8, "origin": list(BONES[bone][1]), "uv_offset": list(uv[n]),
         "faces": {k: {"uv": v, "texture": 0} for k, v in faces_uv(n).items()}, "type": "cube", "uuid": U()}
    if inf: e["inflate"] = inf
    elements.append(e); children[bone].append(e["uuid"])
for b, (p, _) in BONES.items():
    if p: children[p].append(b)
groups = [{"name": b, "origin": list(p), "color": 0, "uuid": bone_uuid[b], "export": True, "mirror_uv": False, "isOpen": True,
           "locked": False, "visibility": True, "autouv": 0, "rotation": [0, 0, 0], "children": []} for b, (_, p) in BONES.items()]
tree = lambda b: {"uuid": bone_uuid[b], "isOpen": True, "children": [tree(c) if c in BONES else c for c in children[b]]}

# ------------------------------------------------------------------ animacoes
def anim(name, length, loop, tracks):
    animators = {}
    for bone, chans in tracks.items():
        kfs = []
        for ch, pts in chans.items():
            for p in pts:
                kfs.append({"channel": ch, "data_points": [{"x": p[1][0], "y": p[1][1], "z": p[1][2]}], "uuid": U(),
                            "time": round(p[0], 4), "color": -1, "interpolation": p[2] if len(p) > 2 else "catmullrom"})
        animators[bone_uuid[bone]] = {"name": bone, "type": "bone", "keyframes": kfs}
    return {"uuid": U(), "name": "animation.rat." + name, "loop": loop, "override": False, "length": length, "snapping": 24,
            "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}
V = lambda x=0, y=0, z=0: [x, y, z]
R, Pn = "rotation", "position"
def loopk(L, *vals): n = len(vals); return [(L * i / n, vals[i]) for i in range(n)] + [(L, vals[0])]
def twitch(L, a, b, n):   # tremidinhas rapidas (focinho/bigode)
    return [(L * i / n, a if i % 2 == 0 else b, "linear") for i in range(n)] + [(L, a, "linear")]
A = []

# 1. idle: respira, focinho e bigode tremendo, cabeca espiando, cauda balancando devagar
L = 2.4
A.append(anim("idle", L, "loop", {
    "body":  {R: loopk(L, V(0), V(1)), Pn: loopk(L, V(0, 0, 0), V(0, 0.15, 0))},
    "head":  {R: [(0, V(0)), (0.6, V(4, 12, 0)), (1.2, V(-2, 0, 0)), (1.8, V(3, -10, 3)), (L, V(0))]},
    "whiskers": {R: twitch(L, V(0), V(6), 24)},
    "ear_l": {R: [(0, V(0)), (1.5, V(0)), (1.55, V(0, -15, 10)), (1.65, V(0)), (L, V(0))]},
    "ear_r": {R: loopk(L, V(0), V(0, 5, -4))},
    "tail":  {R: loopk(L, V(-8, 10, 0), V(-6, -10, 0))}, "tail_tip": {R: loopk(L, V(-6, 14, 0), V(-4, -14, 0))},
}))
# 2. sniff: fareja rapido, cabeca subindo e descendo, bigodes a mil
L = 1.0
A.append(anim("sniff", L, "loop", {
    "head":  {R: [(0, V(8)), (0.1, V(14)), (0.2, V(8)), (0.3, V(14)), (0.5, V(10, 12)), (0.6, V(16, 12)), (0.7, V(10, 12)), (0.8, V(16, 12)), (L, V(8))]},
    "whiskers": {R: twitch(L, V(0), V(10), 20)},
    "body":  {R: loopk(L, V(3), V(4))},
    "ear_l": {R: loopk(L, V(0, 0, 8), V(0, 0, 12))}, "ear_r": {R: loopk(L, V(0, 0, -8), V(0, 0, -12))},
    "tail":  {R: loopk(L, V(-10, 4), V(-10, -4))},
}))
# 3. walk: passo diagonal (dianteira esq + traseira dir juntas), corpo ondulando, cauda serpenteando
L = 0.6
A.append(anim("walk", L, "loop", {
    "leg_fl": {R: loopk(L, V(30), V(-25))}, "leg_br": {R: loopk(L, V(25), V(-25))},
    "leg_fr": {R: loopk(L, V(-25), V(30))}, "leg_bl": {R: loopk(L, V(-25), V(25))},
    "body":  {R: loopk(L, V(0, 4, 0), V(0, -4, 0)), Pn: loopk(L, V(0, 0, 0), V(0, 0.3, 0), V(0, 0, 0), V(0, 0.3, 0))},
    "head":  {R: loopk(L, V(-2, -5, 0), V(-2, 5, 0))},
    "tail":  {R: loopk(L, V(-6, 18), V(-6, -18))}, "tail_tip": {R: loopk(L, V(0, 22), V(0, -22))},
    "whiskers": {R: twitch(L, V(0), V(5), 6)},
}))
# 4. run: corridinha aos pulos (galope), orelhas pra tras, cauda esticada
L = 0.3
A.append(anim("run", L, "loop", {
    "leg_fl": {R: loopk(L, V(55), V(-40))}, "leg_fr": {R: loopk(L, V(45), V(-50))},
    "leg_bl": {R: loopk(L, V(-50), V(45))}, "leg_br": {R: loopk(L, V(-40), V(55))},
    "body":  {R: loopk(L, V(8), V(-8)), Pn: loopk(L, V(0, 0, 0), V(0, 1.0, 0))},
    "head":  {R: loopk(L, V(-10), V(4))},
    "ear_l": {R: loopk(L, V(-30, 0, 0), V(-30, 0, 0))}, "ear_r": {R: loopk(L, V(-30, 0, 0), V(-30, 0, 0))},
    "tail":  {R: loopk(L, V(4, 6), V(4, -6))}, "tail_tip": {R: loopk(L, V(2, 8), V(2, -8))},
}))
# 5. groom: senta, esfrega o rosto com as patinhas alternando, lambe as patas, passa nas orelhas
L = 3.2
rub = []
for i in range(10):
    t = 0.5 + i * 0.16
    rub.append((t, i % 2))
A.append(anim("groom", L, "once", {
    "body":  {R: [(0, V(0)), (0.4, V(40)), (2.8, V(40)), (L, V(0))], Pn: [(0, V(0)), (0.4, V(0, 0.3, 0)), (2.8, V(0, 0.3, 0)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.4, V(-40)), (1.3, V(-40, 0, 8)), (1.35, V(-48, 0, -8)), (2.0, V(-30, 20, 15)), (2.4, V(-30, -20, -15)), (2.8, V(-40)), (L, V(0))]},
    "leg_fl": {R: [(0, V(0))] + [(t, V(130 if s else 105, 0, -15)) for t, s in rub] + [(2.0, V(150, -30, -20)), (2.4, V(110)), (2.8, V(110)), (L, V(0))]},
    "leg_fr": {R: [(0, V(0))] + [(t, V(105 if s else 130, 0, 15)) for t, s in rub] + [(2.0, V(110)), (2.4, V(150, 30, 20)), (2.8, V(110)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.4, V(-40)), (2.8, V(-40)), (L, V(0))]},
    "leg_br": {R: [(0, V(0)), (0.4, V(-40)), (2.8, V(-40)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (2.0, V(0)), (2.1, V(-20, 0, -20)), (2.3, V(0)), (L, V(0))]},
    "ear_r": {R: [(0, V(0)), (2.4, V(0)), (2.5, V(-20, 0, 20)), (2.7, V(0)), (L, V(0))]},
    "tail":  {R: [(0, V(-6)), (0.4, V(-20, 40)), (2.8, V(-20, 40)), (L, V(-6))]}, "tail_tip": {R: [(0, V(0)), (0.4, V(0, 50)), (2.8, V(0, 50)), (L, V(0))]},
    "whiskers": {R: twitch(L, V(0), V(5), 16)},
}))
# 6. stand_up: levanta nas patas de tras igual suricato, olha em volta, fareja, desce
L = 3.4
A.append(anim("stand_up", L, "once", {
    "body":  {R: [(0, V(0)), (0.35, V(72)), (2.9, V(72)), (L, V(0))], Pn: [(0, V(0)), (0.35, V(0, 0.8, 0)), (2.9, V(0, 0.8, 0)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.35, V(-70)), (0.9, V(-62, 35)), (1.3, V(-62, 35)), (1.6, V(-60, -40)), (2.1, V(-60, -40)), (2.3, V(-58, 0)),
                  (2.4, V(-52)), (2.5, V(-58)), (2.6, V(-52)), (2.9, V(-70)), (L, V(0))]},
    "leg_fl": {R: [(0, V(0)), (0.35, V(-60, 0, -10)), (2.9, V(-60, 0, -10)), (L, V(0))]},
    "leg_fr": {R: [(0, V(0)), (0.35, V(-60, 0, 10)), (2.9, V(-60, 0, 10)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.35, V(-30)), (2.9, V(-30)), (L, V(0))]},
    "leg_br": {R: [(0, V(0)), (0.35, V(-30)), (2.9, V(-30)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.4, V(10, 0, 15)), (2.9, V(10, 0, 15)), (L, V(0))]}, "ear_r": {R: [(0, V(0)), (0.4, V(10, 0, -15)), (2.9, V(10, 0, -15)), (L, V(0))]},
    "tail":  {R: [(0, V(-6)), (0.35, V(-70)), (2.9, V(-70)), (L, V(-6))]}, "tail_tip": {R: [(0, V(0)), (0.35, V(-20)), (2.9, V(-20)), (L, V(0))]},
    "whiskers": {R: twitch(L, V(0), V(8), 30)},
}))
# 7. eat: sentadinho segurando a comida com as maos, mordiscando rapido
L = 1.0
A.append(anim("eat", L, "loop", {
    "body":  {R: loopk(L, V(38), V(40)), Pn: loopk(L, V(0, 0.3, 0), V(0, 0.3, 0))},
    "head":  {R: [(0, V(-30)), (0.1, V(-38)), (0.2, V(-30)), (0.3, V(-38)), (0.4, V(-30)), (0.6, V(-34, 6)), (0.7, V(-40, 6)), (0.8, V(-34, 6)), (L, V(-30))]},
    "leg_fl": {R: loopk(L, V(120, 0, -18), V(126, 0, -18))}, "leg_fr": {R: loopk(L, V(120, 0, 18), V(126, 0, 18))},
    "leg_bl": {R: loopk(L, V(-38), V(-38))}, "leg_br": {R: loopk(L, V(-38), V(-38))},
    "whiskers": {R: twitch(L, V(0), V(8), 10)},
    "tail":  {R: loopk(L, V(-20, 35), V(-20, 30))}, "tail_tip": {R: loopk(L, V(0, 40), V(0, 35))},
}))
# 8. attack: bote com mordida
L = 0.45
A.append(anim("attack", L, "once", {
    "body":  {R: [(0, V(0)), (0.1, V(-10)), (0.2, V(6)), (L, V(0))], Pn: [(0, V(0)), (0.1, V(0, 0, 1.0)), (0.2, V(0, 0.6, -2.0)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.1, V(20)), (0.2, V(-20)), (0.28, V(-10)), (L, V(0))]},
    "leg_fl": {R: [(0, V(0)), (0.2, V(60)), (L, V(0))]}, "leg_fr": {R: [(0, V(0)), (0.2, V(60)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.1, V(20)), (0.2, V(-40)), (L, V(0))]}, "leg_br": {R: [(0, V(0)), (0.1, V(20)), (0.2, V(-40)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.1, V(-35)), (L, V(0))]}, "ear_r": {R: [(0, V(0)), (0.1, V(-35)), (L, V(0))]},
}))
# 9. cower: medo — achatado no chao, orelhas coladas, tremendo
L = 0.3
A.append(anim("cower", L, "loop", {
    "body":  {R: loopk(L, V(0, 0, -2), V(0, 0, 2)), Pn: loopk(L, V(0, -1.0, 0), V(0, -1.0, 0))},
    "head":  {R: loopk(L, V(-8), V(-8))},
    "leg_fl": {R: loopk(L, V(40), V(40))}, "leg_fr": {R: loopk(L, V(40), V(40))},
    "leg_bl": {R: loopk(L, V(-40), V(-40))}, "leg_br": {R: loopk(L, V(-40), V(-40))},
    "ear_l": {R: loopk(L, V(-50, 0, -10), V(-50, 0, -10))}, "ear_r": {R: loopk(L, V(-50, 0, 10), V(-50, 0, 10))},
    "tail":  {R: loopk(L, V(-10, 60), V(-10, 62))}, "tail_tip": {R: loopk(L, V(0, 60), V(0, 60))},
}))
# 10. scratch: coca a orelha com a pata de tras, bem rapido
L = 1.6
sc = [(0.35 + i * 0.07, V(95 if i % 2 else 70, 0, 20)) for i in range(14)]
A.append(anim("scratch", L, "once", {
    "body":  {R: [(0, V(0)), (0.3, V(25, 0, 15)), (1.35, V(25, 0, 15)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.3, V(-15, -10, 30)), (1.35, V(-15, -10, 30)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.3, V(80, 0, 20))] + sc + [(1.35, V(80, 0, 20)), (L, V(0))]},
    "leg_br": {R: [(0, V(0)), (0.3, V(-20)), (1.35, V(-20)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.35, V(-20, 0, -10))] + [(0.35 + i * 0.07, V(-20 - (8 if i % 2 else 0), 0, -10)) for i in range(14)] + [(L, V(0))]},
    "tail":  {R: [(0, V(-6)), (0.3, V(-6, -40)), (1.35, V(-6, -40)), (L, V(-6))]},
}))
# 11. sleep: enrolado, cabeca enfiada, cauda abraçando, respiracao lenta
L = 3.0
A.append(anim("sleep", L, "loop", {
    "body":  {R: loopk(L, V(0, 0, 0), V(-1, 0, 0)), Pn: loopk(L, V(0, -1.4, 0), V(0, -1.25, 0))},
    "head":  {R: loopk(L, V(-25, 35, -10), V(-26, 35, -10))},
    "ear_l": {R: loopk(L, V(-30), V(-30))}, "ear_r": {R: loopk(L, V(-30), V(-30))},
    "leg_fl": {R: loopk(L, V(60), V(60))}, "leg_fr": {R: loopk(L, V(60), V(60))},
    "leg_bl": {R: loopk(L, V(-50), V(-50))}, "leg_br": {R: loopk(L, V(-50), V(-50))},
    "tail":  {R: loopk(L, V(-20, -80), V(-20, -80))}, "tail_tip": {R: loopk(L, V(0, -90), V(0, -88))},
}))
# 12. squeak: guincho — cabeca pra cima, orelhas tremem (com o som)
L = 0.5
A.append(anim("squeak", L, "once", {
    "head":  {R: [(0, V(0)), (0.08, V(25)), (0.15, V(20)), (0.22, V(25)), (0.3, V(20)), (L, V(0))]},
    "body":  {R: [(0, V(0)), (0.1, V(8)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.08, V(15, 0, 15)), (L, V(0))]}, "ear_r": {R: [(0, V(0)), (0.08, V(15, 0, -15)), (L, V(0))]},
    "whiskers": {R: twitch(L, V(0), V(12), 8)},
}))

model = {"meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True}, "name": "rat", "model_identifier": "rat",
         "modded_entity_version": "1.17", "modded_entity_flip_y": True, "visible_box": [0.5, 0.5, 0.25], "variable_placeholders": "",
         "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {}, "resolution": {"width": W, "height": H},
         "elements": elements, "groups": groups, "outliner": [tree("root")],
         "textures": [{"path": "", "name": "rat.png", "folder": "", "namespace": "", "id": "0", "width": W, "height": H, "uv_width": W, "uv_height": H,
                       "particle": False, "render_mode": "default", "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
                       "frame_interpolate": False, "visible": True, "internal": True, "saved": False, "uuid": U(), "source": TEX}],
         "animations": A}
json.dump(model, open(os.path.join(OUT, "rat.bbmodel"), "w"))
print("ok", len(elements), "cubos,", len(A), "animacoes")

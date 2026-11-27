# King Rat (Rei Rato): rascunho do modelo, textura e animacoes para o Bino refinar no Blockbench.
# Rato em pe (bipede), da familia dos ratos do mod (mesma densidade de textura: escala 1,45 no jogo = ~altura de um
# golem de ferro), coroa de ouro, garras, cauda longa em 3 partes.
# Saida: tools/blockbench/king_rat.bbmodel (+ king_rat.png, king_rat_eyes.png). Rodar da raiz do repo.
# Convencao do Blockbench (igual aos ratos): frente = -Z; X+ = frente sobe / membro vai para a frente / cauda desce;
# mandibula X+ fecha.
import json, uuid, base64, io, math, random
from PIL import Image

U = lambda: str(uuid.uuid4())
TEX_W, TEX_H = 128, 64
rnd = random.Random(7)

# ------------------------------------------------------------------ ossos e cubos
# osso: nome, pai, pivo, rotacao de descanso
BONES = [
    ("root", None, (0, 0, 0), (0, 0, 0)),
    ("leg_l", "root", (2.5, 9, 1.5), (0, 0, 0)),
    ("leg_r", "root", (-2.5, 9, 1.5), (0, 0, 0)),
    ("body", "root", (0, 10, 1.5), (-8, 0, 0)),
    ("head", "body", (0, 21.5, 0.5), (8, 0, 0)),
    ("jaw", "head", (0, 21.5, -3.5), (0, 0, 0)),
    ("ear_l", "head", (2.5, 25.5, -0.5), (0, 0, -15)),
    ("ear_r", "head", (-2.5, 25.5, -0.5), (0, 0, 15)),
    ("whiskers_l", "head", (2, 23, -6.5), (0, 0, 0)),
    ("whiskers_r", "head", (-2, 23, -6.5), (0, 0, 0)),
    ("crown", "head", (0, 26, -1), (0, 0, 0)),
    ("arm_l", "body", (5.5, 20.5, 1), (5, 0, -8)),
    ("hand_l", "arm_l", (5.5, 15, 1), (-15, 0, 0)),
    ("arm_r", "body", (-5.5, 20.5, 1), (5, 0, 8)),
    ("hand_r", "arm_r", (-5.5, 15, 1), (-15, 0, 0)),
    ("tail", "body", (0, 11, 5), (30, 0, 0)),
    ("tail_mid", "tail", (0, 11, 11), (-15, 0, 0)),
    ("tail_tip", "tail_mid", (0, 11, 17), (-12, 0, 0)),
]

FUR, BELLY, SKIN, CLAW, NOSE, TOOTH, GOLD, TAIL = (108, 102, 98), (150, 142, 135), (214, 152, 150), (232, 224, 204), (232, 112, 124), (238, 232, 210), (232, 182, 44), (186, 140, 138)

# cubo: osso, nome, from, to, cor, (extras)
CUBES = [
    ("leg_l", "thigh_l", (0.8, 5, -0.5), (4.3, 10, 4.5), FUR),
    ("leg_l", "ankle_l", (1.3, 2, 1.5), (3.8, 5, 4), FUR),
    ("leg_l", "foot_l", (1, 0, -3), (4, 2, 3), SKIN),
    ("leg_r", "thigh_r", (-4.3, 5, -0.5), (-0.8, 10, 4.5), FUR),
    ("leg_r", "ankle_r", (-3.8, 2, 1.5), (-1.3, 5, 4), FUR),
    ("leg_r", "foot_r", (-4, 0, -3), (-1, 2, 3), SKIN),
    ("body", "belly", (-4, 9, -2), (4, 17, 5), BELLY),
    ("body", "chest", (-3.5, 16.5, -2), (3.5, 21.5, 4), FUR),
    ("body", "back_fur", (-3, 12, 4.5), (3, 20, 6), FUR),
    ("head", "head", (-3, 21, -3.5), (3, 26, 1.5), FUR),
    ("head", "snout", (-2, 21.5, -7.5), (2, 24.5, -3.5), FUR),
    ("head", "nose", (-0.5, 23.5, -8.5), (0.5, 24.5, -7.5), NOSE),
    ("head", "teeth", (-1, 20.5, -7.4), (1, 21.5, -6.4), TOOTH),
    ("jaw", "jaw", (-1.5, 20.5, -6.8), (1.5, 21.5, -3.5), SKIN),
    ("ear_l", "ear_l", (1.5, 25, -1), (5.5, 29, 0), SKIN),
    ("ear_r", "ear_r", (-5.5, 25, -1), (-1.5, 29, 0), SKIN),
    ("whiskers_l", "whiskers_l", (2, 22, -6.5), (6, 24, -6.5), CLAW),
    ("whiskers_r", "whiskers_r", (-6, 22, -6.5), (-2, 24, -6.5), CLAW),
    ("crown", "crown_band", (-2.5, 26, -3), (2.5, 27.5, 1), GOLD),
    ("crown", "crown_spike_fl", (-2.5, 27.5, -3), (-1.5, 29, -2), GOLD),
    ("crown", "crown_spike_fr", (1.5, 27.5, -3), (2.5, 29, -2), GOLD),
    ("crown", "crown_spike_bl", (-2.5, 27.5, 0), (-1.5, 29, 1), GOLD),
    ("crown", "crown_spike_br", (1.5, 27.5, 0), (2.5, 29, 1), GOLD),
    ("crown", "crown_gem", (-0.5, 27.5, -3.2), (0.5, 30, -2.2), GOLD),
    ("arm_l", "upper_arm_l", (4, 15, -0.5), (7, 21, 2.5), FUR),
    ("hand_l", "forearm_l", (4.2, 10, -0.3), (6.8, 15, 2.3), FUR),
    ("hand_l", "paw_l", (4, 8, -0.8), (7, 10, 2.2), SKIN),
    ("hand_l", "claws_l", (4.2, 7, -1.6), (6.8, 8, -0.6), CLAW),
    ("arm_r", "upper_arm_r", (-7, 15, -0.5), (-4, 21, 2.5), FUR),
    ("hand_r", "forearm_r", (-6.8, 10, -0.3), (-4.2, 15, 2.3), FUR),
    ("hand_r", "paw_r", (-7, 8, -0.8), (-4, 10, 2.2), SKIN),
    ("hand_r", "claws_r", (-6.8, 7, -1.6), (-4.2, 8, -0.6), CLAW),
    ("tail", "tail_base", (-1, 10, 5), (1, 12, 11), TAIL),
    ("tail_mid", "tail_mid", (-0.75, 10.25, 11), (0.75, 11.75, 17), TAIL),
    ("tail_tip", "tail_tip", (-0.5, 10.5, 17), (0.5, 11.5, 24), TAIL),
]

# ------------------------------------------------------------------ UV (box UV) e pintura
def dims(c):
    f, t = c[2], c[3]
    return [max(0, math.ceil(t[i] - f[i] - 1e-6)) for i in range(3)]

placed = {}
x = y = shelf = 0
for c in sorted(CUBES, key=lambda c: -(dims(c)[2] + dims(c)[1])):
    w, h, d = dims(c)
    fw, fh = 2 * (w + d), d + h
    if x + fw > TEX_W: x, y, shelf = 0, y + shelf, 0
    placed[c[1]] = (x, y)
    x += fw; shelf = max(shelf, fh)
assert y + shelf <= TEX_H, "textura cheia"

img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
eyes = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))

def shade(col, k):
    return tuple(max(0, min(255, int(v * k))) for v in col) + (255,)

def fill(u, v, w, h, col, k, noise=0.06):
    for j in range(h):
        for i in range(w):
            n = 1 + (rnd.random() - 0.5) * 2 * noise
            img.putpixel((u + i, v + j), shade(col, k * n))

def faces(u, v, w, h, d):
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
            "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}

for c in CUBES:
    w, h, d = dims(c)
    u, v = placed[c[1]]
    col = c[4]
    fz = faces(u, v, w, h, d)
    k = {"up": 1.12, "down": 0.7, "north": 1.0, "south": 0.92, "east": 0.88, "west": 0.88}
    noise = 0.02 if col in (GOLD, CLAW, TOOTH, NOSE) else 0.08
    for name, (fu, fv, fw, fh) in fz.items():
        if fw and fh: fill(fu, fv, fw, fh, col, k[name], noise)
    n = c[1]
    if n == "head":   # olhos vermelhos (camada que brilha) e sobrancelha escura
        fu, fv, fw, fh = fz["north"]
        for ex in (1, fw - 2):
            img.putpixel((fu + ex, fv + 1), (255, 48, 48, 255)); eyes.putpixel((fu + ex, fv + 1), (255, 60, 60, 255))
            img.putpixel((fu + ex, fv), shade(FUR, 0.55))
    if n == "belly":  # pelo mais escuro nos lados e em cima
        for side in ("east", "west"):
            fu, fv, fw, fh = fz[side]; fill(fu, fv, fw, fh, FUR, 0.85)
    if n.startswith("ear"):   # borda de pelo na orelha
        for side in ("north", "south"):
            fu, fv, fw, fh = fz[side]
            for i in range(fw): img.putpixel((fu + i, fv), shade(FUR, 0.9))
            for j in range(fh): img.putpixel((fu, fv + j), shade(FUR, 0.9)); img.putpixel((fu + fw - 1, fv + j), shade(FUR, 0.9))
    if n == "crown_gem":
        fu, fv, fw, fh = fz["north"]
        for j in range(1, 2): img.putpixel((fu, fv + j), (210, 30, 60, 255))
    if n == "crown_band":   # pedrinhas na faixa
        fu, fv, fw, fh = fz["north"]
        for i in range(0, fw, 2): img.putpixel((fu + i, fv), (90, 160, 230, 255))

# ------------------------------------------------------------------ montar o bbmodel
groups, gid = [], {}
for name, parent, piv, rot in BONES:
    g = {"name": name, "uuid": U(), "export": True, "locked": False, "scope": 0, "selected": False, "visibility": True,
         "origin": list(piv), "rotation": list(rot), "color": 0, "children": [], "reset": False, "shade": True,
         "mirror_uv": False, "autouv": 0, "isOpen": True}
    groups.append(g); gid[name] = g
elements = []
for c in CUBES:
    bone, name, f, t, col = c
    u, v = placed[name]
    w, h, d = dims(c)
    fz = faces(u, v, w, h, d)
    fj = {k: {"uv": [fu, fv, fu + fw, fv + fh], "texture": 0} for k, (fu, fv, fw, fh) in fz.items()}
    e = {"name": name, "box_uv": True, "render_order": "default", "locked": False, "export": True, "scope": 0,
         "allow_mirror_modeling": True, "from": list(f), "to": list(t), "autouv": 0, "color": 0, "inflate": 0,
         "rotation": [0, 0, 0], "origin": list(gid[bone]["origin"]), "uv_offset": [u, v], "faces": fj, "type": "cube", "uuid": U()}
    elements.append(e)
    gid[bone]["children"].append(e["uuid"])

def outline(name):
    g = gid[name]
    kids = [e for e in g["children"]] + [outline(b[0]) for b in BONES if b[1] == name]
    return {"uuid": g["uuid"], "isOpen": True, "children": kids}

for g in groups: g["children"] = []
for c, e in zip(CUBES, elements): gid[c[0]]["children"].append(e["uuid"])
outliner = [outline("root")]
for g in groups: g["children"] = []   # o Blockbench guarda os filhos no outliner

def png64(im):
    b = io.BytesIO(); im.save(b, "PNG"); return "data:image/png;base64," + base64.b64encode(b.getvalue()).decode()

# ------------------------------------------------------------------ animacoes
def key(ch, t, v, interp="catmullrom"):
    return {"channel": ch, "data_points": [{"x": round(v[0], 2), "y": round(v[1], 2), "z": round(v[2], 2)}], "uuid": U(),
            "time": round(t, 4), "color": -1, "interpolation": interp}

def anim(name, length, loop, tracks):
    animators = {}
    for bone, chans in tracks.items():
        ks = [key(ch, t, v) for ch, pts in chans.items() for t, v in pts]
        animators[gid[bone]["uuid"]] = {"name": bone, "type": "bone", "rotation_global": False, "quaternion_interpolation": False, "keyframes": ks}
    return {"uuid": U(), "name": "animation.king_rat." + name, "loop": loop, "override": False, "length": length, "snapping": 24,
            "selected": False, "scope": 0, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}

def cyc(L, vals):
    n = len(vals); return [(L * i / n, vals[i]) for i in range(n)] + [(L, vals[0])]

Z = (0, 0, 0)
A = []
L = 2.4
A.append(anim("idle", L, "loop", {
    "body": {"rotation": cyc(L, [(0, 0, 0), (-1.5, 0, 0.5), (0, 0, 0), (-1.5, 0, -0.5)]), "position": cyc(L, [Z, (0, 0.2, 0), Z, (0, 0.2, 0)])},
    "head": {"rotation": cyc(L, [(0, 0, 0), (2, 6, 0), (0, 0, 0), (2, -6, 0)])},
    "jaw": {"rotation": cyc(L, [(0, 0, 0), (0, 0, 0), (-6, 0, 0), (0, 0, 0)])},
    "arm_l": {"rotation": cyc(L, [(0, 0, 0), (3, 0, -2), (0, 0, 0), (3, 0, 0)])},
    "arm_r": {"rotation": cyc(L, [(0, 0, 0), (3, 0, 2), (0, 0, 0), (3, 0, 0)])},
    "tail": {"rotation": cyc(L, [(0, -12, 0), (3, 0, 0), (0, 12, 0), (3, 0, 0)])},
    "tail_mid": {"rotation": cyc(L, [(0, -10, 0), (0, -6, 0), (0, 10, 0), (0, 6, 0)])},
    "tail_tip": {"rotation": cyc(L, [(0, -14, 0), (0, -10, 0), (0, 14, 0), (0, 10, 0)])},
    "whiskers_l": {"rotation": cyc(L, [(0, 0, 0), (0, 8, 4), (0, 0, 0), (0, -4, 0)])},
    "whiskers_r": {"rotation": cyc(L, [(0, 0, 0), (0, -8, -4), (0, 0, 0), (0, 4, 0)])},
    "ear_l": {"rotation": cyc(L, [(0, 0, 0), (0, 0, 0), (0, 0, -10), (0, 0, 0)])},
}))
L = 1.1
A.append(anim("walk", L, "loop", {
    "root": {"position": cyc(L, [(0, 0, 0), (0, 0.6, 0), (0, 0, 0), (0, 0.6, 0)])},
    "body": {"rotation": cyc(L, [(-3, 6, 3), (-3, 0, 0), (-3, -6, -3), (-3, 0, 0)])},
    "head": {"rotation": cyc(L, [(2, -6, -2), (0, 0, 0), (2, 6, 2), (0, 0, 0)])},
    "leg_l": {"rotation": cyc(L, [(28, 0, 0), (0, 0, 0), (-24, 0, 0), (0, 0, 0)])},
    "leg_r": {"rotation": cyc(L, [(-24, 0, 0), (0, 0, 0), (28, 0, 0), (0, 0, 0)])},
    "arm_l": {"rotation": cyc(L, [(-18, 0, -4), (0, 0, -2), (22, 0, -4), (0, 0, -2)])},
    "arm_r": {"rotation": cyc(L, [(22, 0, 4), (0, 0, 2), (-18, 0, 4), (0, 0, 2)])},
    "tail": {"rotation": cyc(L, [(0, 14, 0), (4, 0, 0), (0, -14, 0), (4, 0, 0)])},
    "tail_mid": {"rotation": cyc(L, [(0, 10, 0), (0, 14, 0), (0, -10, 0), (0, -14, 0)])},
    "tail_tip": {"rotation": cyc(L, [(0, 8, 0), (0, 16, 0), (0, -8, 0), (0, -16, 0)])},
}))
# garrada dupla: prepara (0-0,4), patada direita (0,45), patada esquerda (0,7), volta
A.append(anim("claw", 1.0, "once", {
    "body": {"rotation": [(0, Z), (0.35, (6, -18, 0)), (0.45, (-14, 22, 0)), (0.6, (6, 18, 0)), (0.7, (-14, -22, 0)), (1.0, Z)]},
    "head": {"rotation": [(0, Z), (0.35, (10, 10, 0)), (0.45, (-6, -12, 0)), (0.6, (10, -10, 0)), (0.7, (-6, 12, 0)), (1.0, Z)]},
    "jaw": {"rotation": [(0, Z), (0.3, (-25, 0, 0)), (0.75, (-25, 0, 0)), (1.0, Z)]},
    "arm_r": {"rotation": [(0, Z), (0.35, (150, 0, 25)), (0.45, (20, 0, -15)), (0.7, (10, 0, 0)), (1.0, Z)]},
    "hand_r": {"rotation": [(0, Z), (0.35, (30, 0, 0)), (0.45, (-20, 0, 0)), (1.0, Z)]},
    "arm_l": {"rotation": [(0, Z), (0.35, (40, 0, -10)), (0.6, (150, 0, -25)), (0.7, (20, 0, 15)), (1.0, Z)]},
    "hand_l": {"rotation": [(0, Z), (0.6, (30, 0, 0)), (0.7, (-20, 0, 0)), (1.0, Z)]},
    "tail": {"rotation": [(0, Z), (0.45, (0, -25, 0)), (0.7, (0, 25, 0)), (1.0, Z)]},
}))
# cuspe: puxa a cabeca para tras (0-0,4), cospe (0,5)
A.append(anim("spit", 1.0, "once", {
    "body": {"rotation": [(0, Z), (0.4, (12, 0, 0)), (0.5, (-12, 0, 0)), (1.0, Z)]},
    "head": {"rotation": [(0, Z), (0.4, (28, 0, 0)), (0.5, (-22, 0, 0)), (0.75, (-10, 0, 0)), (1.0, Z)]},
    "jaw": {"rotation": [(0, Z), (0.4, (0, 0, 0)), (0.5, (-40, 0, 0)), (0.75, (-30, 0, 0)), (1.0, Z)]},
    "arm_l": {"rotation": [(0, Z), (0.4, (-20, 0, -20)), (0.55, (30, 0, -10)), (1.0, Z)]},
    "arm_r": {"rotation": [(0, Z), (0.4, (-20, 0, 20)), (0.55, (30, 0, 10)), (1.0, Z)]},
}))
# salto esmagador: agacha (0-0,35), salta (0,35-0,8), cai e esmaga (0,85), levanta
A.append(anim("leap", 1.3, "once", {
    "root": {"position": [(0, Z), (0.35, (0, -3, 0)), (0.5, (0, 4, -6)), (0.8, (0, 3, -10)), (0.85, (0, -2.5, -12)), (1.3, (0, 0, -12))]},
    "body": {"rotation": [(0, Z), (0.35, (-25, 0, 0)), (0.55, (10, 0, 0)), (0.85, (-30, 0, 0)), (1.3, Z)]},
    "leg_l": {"rotation": [(0, Z), (0.35, (40, 0, 0)), (0.55, (-30, 0, 0)), (0.85, (30, 0, -10)), (1.3, Z)]},
    "leg_r": {"rotation": [(0, Z), (0.35, (40, 0, 0)), (0.55, (-30, 0, 0)), (0.85, (30, 0, 10)), (1.3, Z)]},
    "arm_l": {"rotation": [(0, Z), (0.35, (-40, 0, -10)), (0.6, (160, 0, -20)), (0.85, (40, 0, -10)), (1.3, Z)]},
    "arm_r": {"rotation": [(0, Z), (0.35, (-40, 0, 10)), (0.6, (160, 0, 20)), (0.85, (40, 0, 10)), (1.3, Z)]},
    "head": {"rotation": [(0, Z), (0.35, (20, 0, 0)), (0.85, (25, 0, 0)), (1.3, Z)]},
    "tail": {"rotation": [(0, Z), (0.5, (-30, 0, 0)), (0.85, (20, 0, 0)), (1.3, Z)]},
}))
# guincho do rei (chama os ratos): levanta o peito, cabeca para cima, boca aberta, bracos abertos, tremendo
A.append(anim("roar", 1.6, "once", {
    "body": {"rotation": [(0, Z), (0.35, (14, 0, 0)), (0.6, (16, 0, 2)), (0.8, (16, 0, -2)), (1.0, (16, 0, 2)), (1.2, (14, 0, 0)), (1.6, Z)]},
    "head": {"rotation": [(0, Z), (0.35, (35, 0, 0)), (0.6, (38, 0, -4)), (0.8, (38, 0, 4)), (1.0, (38, 0, -4)), (1.25, (30, 0, 0)), (1.6, Z)]},
    "jaw": {"rotation": [(0, Z), (0.35, (-45, 0, 0)), (1.25, (-40, 0, 0)), (1.6, Z)]},
    "arm_l": {"rotation": [(0, Z), (0.4, (20, 0, -70)), (1.2, (20, 0, -65)), (1.6, Z)]},
    "arm_r": {"rotation": [(0, Z), (0.4, (20, 0, 70)), (1.2, (20, 0, 65)), (1.6, Z)]},
    "ear_l": {"rotation": [(0, Z), (0.4, (-20, 0, -30)), (1.2, (-20, 0, -30)), (1.6, Z)]},
    "ear_r": {"rotation": [(0, Z), (0.4, (-20, 0, 30)), (1.2, (-20, 0, 30)), (1.6, Z)]},
    "tail": {"rotation": [(0, Z), (0.4, (-35, 0, 0)), (1.2, (-30, 0, 0)), (1.6, Z)]},
}))
# acorda (nasce na cama): enrolado de lado, levanta e se espreguica
A.append(anim("wake", 2.2, "once", {
    "root": {"rotation": [(0, (0, 0, 80)), (0.6, (0, 0, 70)), (1.1, (0, 0, 0)), (2.2, Z)], "position": [(0, (0, -4, 0)), (1.1, Z), (2.2, Z)]},
    "body": {"rotation": [(0, (-50, 0, 0)), (1.1, (-10, 0, 0)), (1.6, (12, 0, 0)), (2.2, Z)]},
    "head": {"rotation": [(0, (30, 0, 0)), (1.1, (0, 0, 0)), (1.6, (30, 0, 0)), (2.2, Z)]},
    "jaw": {"rotation": [(0, Z), (1.3, Z), (1.6, (-35, 0, 0)), (1.9, Z), (2.2, Z)]},
    "arm_l": {"rotation": [(0, (60, 0, 0)), (1.1, (0, 0, 0)), (1.6, (170, 0, -15)), (2.2, Z)]},
    "arm_r": {"rotation": [(0, (60, 0, 0)), (1.1, (0, 0, 0)), (1.6, (170, 0, 15)), (2.2, Z)]},
    "leg_l": {"rotation": [(0, (60, 0, 0)), (1.1, Z), (2.2, Z)]},
    "leg_r": {"rotation": [(0, (60, 0, 0)), (1.1, Z), (2.2, Z)]},
    "tail": {"rotation": [(0, (0, 60, 0)), (1.1, Z), (2.2, Z)]},
}))
# morte: cambaleia e cai de costas
A.append(anim("death", 1.6, "once", {
    "root": {"rotation": [(0, Z), (0.4, (-8, 0, 6)), (1.1, (80, 0, 6)), (1.6, (80, 0, 6))], "position": [(0, Z), (1.1, (0, 0, 2)), (1.6, (0, 0, 2))]},
    "head": {"rotation": [(0, Z), (0.4, (-20, 0, 0)), (1.1, (20, 20, 0)), (1.6, (20, 20, 0))]},
    "jaw": {"rotation": [(0, Z), (0.4, (-30, 0, 0)), (1.6, (-20, 0, 0))]},
    "arm_l": {"rotation": [(0, Z), (0.4, (40, 0, -30)), (1.1, (-40, 0, -60)), (1.6, (-40, 0, -60))]},
    "arm_r": {"rotation": [(0, Z), (0.4, (40, 0, 30)), (1.1, (-40, 0, 60)), (1.6, (-40, 0, 60))]},
    "tail": {"rotation": [(0, Z), (1.1, (-40, 0, 0)), (1.6, (-40, 0, 0))]},
}))

model = {
    "meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True},
    "name": "king_rat", "model_identifier": "king_rat", "modded_entity_entity_class": "", "modded_entity_version": "1.17",
    "modded_entity_flip_y": True, "visible_box": [1.5, 3, 1], "variable_placeholders": "", "variable_placeholder_buttons": [],
    "timeline_setups": [], "unhandled_root_fields": {}, "resolution": {"width": TEX_W, "height": TEX_H},
    "elements": elements, "groups": groups, "outliner": outliner,
    "textures": [{"name": "king_rat.png", "path": "", "folder": "", "namespace": "", "id": "0", "group": "", "scope": 0,
                  "width": TEX_W, "height": TEX_H, "uv_width": TEX_W, "uv_height": TEX_H, "particle": False,
                  "use_as_default": False, "layers_enabled": False, "sync_to_project": "", "file_format": "png",
                  "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color",
                  "fps": 7, "force_translucent": False, "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
                  "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": U(),
                  "relative_path": "king_rat.png", "source": png64(img)}],
    "animations": A,
}
json.dump(model, open("tools/blockbench/king_rat.bbmodel", "w"))
img.save("tools/blockbench/king_rat.png"); eyes.save("tools/blockbench/king_rat_eyes.png")
print("ok", len(elements), "cubos,", len(A), "animacoes")

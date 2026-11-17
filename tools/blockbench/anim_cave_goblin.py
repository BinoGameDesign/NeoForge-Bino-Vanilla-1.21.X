# Recebe o modelo do Bino (cave_goblin_bino_original.bbmodel), adiciona ossos de cotovelo/joelho
# (sem mover nenhum cubo) e escreve cave_goblin.bbmodel com as animacoes novas.
# Rodar: python3 tools/blockbench/anim_cave_goblin.py
import json, uuid, os, copy
D = os.path.dirname(os.path.abspath(__file__))
m = json.load(open(os.path.join(D, "cave_goblin_bino_original.bbmodel")))
U = lambda: str(uuid.uuid4())

els = {e["name"]: e for e in m["elements"]}
grp = {g["name"]: g for g in m["groups"]}
gid = {g["uuid"]: g for g in m["groups"]}

def find(node, name):
    if isinstance(node, dict):
        if gid[node["uuid"]]["name"] == name: return node
        for c in node["children"]:
            r = find(c, name)
            if r: return r
    return None

def add_bone(parent, name, origin, cube_names):
    tmpl = copy.deepcopy(grp[parent]); g = tmpl
    g.update({"name": name, "origin": origin, "uuid": U(), "rotation": [0, 0, 0]})
    m["groups"].append(g); grp[name] = g; gid[g["uuid"]] = g
    pnode = find(m["outliner"][0], parent)
    ids = [els[c]["uuid"] for c in cube_names]
    pnode["children"] = [c for c in pnode["children"] if c not in ids]
    pnode["children"].append({"uuid": g["uuid"], "isOpen": True, "children": ids})

if "forearm_bone_l" not in grp:
    add_bone("arm_l", "forearm_bone_l", [8.9, 12.6, 0.1], ["forearm_l", "hand_l", "thumb_l"])
    add_bone("arm_r", "forearm_bone_r", [-7.8, 12.6, 0.1], ["forearm_r", "hand_r", "thumb_r"])
    add_bone("leg_l", "calf_bone_l", [1.8, 3, 1.8], ["calf_l", "foot_l"])
    add_bone("leg_r", "calf_bone_r", [-1.8, 3, 1.8], ["calf_r", "foot_r"])

# ------------------------------------------------------------------ helpers
# Convencoes deste rig (Blockbench, ordem ZYX, X e Y invertidos no animador):
#  corpo/cabeca/pernas: X+ = frente desce (inclina pra frente / perna vai pra tras / panturrilha dobra o joelho)
#  bracos estao em T: o braco "pendurado" e Z (esq -90 / dir +90); a frente/tras e Y.
def AL(hang=78, fwd=0, roll=0):  return [roll, -fwd, -hang]   # braco esquerdo: hang 90=reto pra baixo, 0=em T, <0 = acima
def AR(hang=78, fwd=0, roll=0):  return [roll, fwd, hang]
def FL(bend=0, roll=0):          return [roll, -bend, 0]      # cotovelo esq: bend+ = antebraco vai pra frente
def FR(bend=0, roll=0):          return [roll, bend, 0]
def V(x=0, y=0, z=0):            return [x, y, z]

R, P, S = "rotation", "position", "scale"
def anim(name, length, loop, tracks):
    animators = {}
    for bone, chans in tracks.items():
        kfs = []
        for ch, pts in chans.items():
            for p in pts:
                t, v = p[0], p[1]
                kfs.append({"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "uuid": U(),
                            "time": round(t, 4), "color": -1, "interpolation": p[2] if len(p) > 2 else "catmullrom"})
        animators[grp[bone]["uuid"]] = {"name": bone, "type": "bone", "keyframes": kfs}
    return {"uuid": U(), "name": "animation.cave_goblin." + name, "loop": loop, "override": False, "length": length,
            "snapping": 24, "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "",
            "loop_delay": "", "animators": animators}

def loopk(L, *vals):
    """keyframes igualmente espacados que fecham o ciclo (ultimo = primeiro)"""
    n = len(vals)
    return [(L * i / n, vals[i]) for i in range(n)] + [(L, vals[0])]

A = []

# ===== 1. idle: corcunda relaxado, joelhos dobrados, respirando, cabeca observando, orelha tremendo
L = 3.2
A.append(anim("idle", L, "loop", {
    "body":  {R: loopk(L, V(20), V(23)), P: loopk(L, V(0, -0.7), V(0, -1.0))},
    "head":  {R: [(0, V(-16)), (0.8, V(-18, 10, 2)), (1.6, V(-15, -4, -2)), (2.4, V(-17, -12, 0)), (L, V(-16))]},
    "jaw":   {R: loopk(L, V(0), V(3))},
    "leg_l": {R: loopk(L, V(-14, -4), V(-16, -4))}, "calf_bone_l": {R: loopk(L, V(22), V(26))},
    "leg_r": {R: loopk(L, V(-10, 6), V(-12, 6))},   "calf_bone_r": {R: loopk(L, V(18), V(22))},
    "arm_l": {R: loopk(L, AL(80, 18), AL(76, 22))}, "forearm_bone_l": {R: loopk(L, FL(28, 0), FL(34, 8))},
    "arm_r": {R: loopk(L, AR(80, 16), AR(77, 20))}, "forearm_bone_r": {R: loopk(L, FR(30, 0), FR(36, -8))},
    "ear_l": {R: [(0, V(0, 0, 6)), (2.0, V(0, 0, 6)), (2.06, V(0, 0, 22)), (2.14, V(0, 0, 4)), (2.22, V(0, 0, 16)), (2.4, V(0, 0, 6)), (L, V(0, 0, 6))]},
    "ear_r": {R: loopk(L, V(0, 0, -6), V(0, -4, -9))},
    "sack":  {R: loopk(L, V(0, 0, 0), V(4, 0, 2))},
    "cloth": {R: loopk(L, V(-6), V(-4))},
}))

# ===== 2. idle_fidget: coca a cabeca com a mao direita, cheira os dedos
L = 2.8
A.append(anim("idle_fidget", L, "once", {
    "body":  {R: [(0, V(20)), (0.5, V(16, 0, -4)), (2.2, V(16, 0, -4)), (L, V(20))], P: [(0, V(0, -0.7)), (L, V(0, -0.7))]},
    "head":  {R: [(0, V(-16)), (0.5, V(-6, -10, -14)), (1.6, V(-6, -10, -14)), (1.9, V(0, -20, 0)), (2.3, V(4, -24, 0)), (L, V(-16))]},
    "arm_r": {R: [(0, AR(80, 16)), (0.45, AR(-10, 70, 30)), (1.6, AR(-10, 70, 30)), (1.9, AR(40, 70)), (2.3, AR(40, 70)), (L, AR(80, 16))]},
    "forearm_bone_r": {R: [(0, FR(30)), (0.45, FR(120)), (0.6, FR(120, -25)), (0.75, FR(120, 20)), (0.9, FR(120, -25)), (1.05, FR(120, 20)),
                           (1.2, FR(120, -25)), (1.35, FR(120, 20)), (1.6, FR(120)), (1.9, FR(110)), (2.3, FR(110)), (L, FR(30))]},
    "arm_l": {R: [(0, AL(80, 18)), (L, AL(80, 18))]}, "forearm_bone_l": {R: [(0, FL(28)), (L, FL(28))]},
    "leg_l": {R: [(0, V(-14, -4)), (L, V(-14, -4))]}, "calf_bone_l": {R: [(0, V(22)), (L, V(22))]},
    "leg_r": {R: [(0, V(-10, 6)), (L, V(-10, 6))]},   "calf_bone_r": {R: [(0, V(18)), (L, V(18))]},
    "lid_l": {R: [(0, V(0)), (0.5, V(40)), (1.6, V(40)), (1.8, V(0)), (L, V(0))]},
    "lid_r": {R: [(0, V(0)), (0.5, V(40)), (1.6, V(40)), (1.8, V(0)), (L, V(0))]},
    "jaw":   {R: [(0, V(0)), (0.5, V(6)), (1.6, V(6)), (1.95, V(0)), (2.05, V(4)), (2.15, V(0)), (2.25, V(4)), (L, V(0))]},
    "ear_r": {R: [(0, V(0, 0, -6)), (0.5, V(0, 0, -30)), (1.6, V(0, 0, -30)), (L, V(0, 0, -6))]},
}))

# ===== 3. blink: piscada simples e dupla (tocar por cima de qualquer animacao)
def blink_track():
    rot, sca = [], []
    for t0 in (2.0, 4.3, 4.52):
        dur = 0.16 if t0 == 2.0 else 0.11
        rot += [(t0, V(0), "linear"), (t0 + 0.05, V(90), "linear"), (t0 + dur, V(90), "linear"), (t0 + dur + 0.06, V(0), "linear")]
        sca += [(t0, V(1, 1, 1), "linear"), (t0 + 0.05, V(1, 1, 2.1), "linear"), (t0 + dur, V(1, 1, 2.1), "linear"), (t0 + dur + 0.06, V(1, 1, 1), "linear")]
    return {R: [(0, V(0), "linear")] + rot + [(5, V(0), "linear")], S: [(0, V(1, 1, 1), "linear")] + sca + [(5, V(1, 1, 1), "linear")]}
A.append(anim("blink", 5.0, "loop", {"lid_l": blink_track(), "lid_r": blink_track()}))

# ===== 4. look_around: olha nervoso pros lados, orelhas em pe
L = 2.6
A.append(anim("look_around", L, "once", {
    "head":  {R: [(0, V(-16)), (0.3, V(-20, 45, 6)), (0.9, V(-20, 45, 6)), (1.05, V(-22, 38, 4)), (1.3, V(-18, -48, -6)), (1.9, V(-18, -48, -6)), (2.1, V(-26, 0, 0)), (L, V(-16))]},
    "body":  {R: [(0, V(20)), (0.3, V(18, 12)), (1.3, V(18, -12)), (L, V(20))], P: [(0, V(0, -0.7)), (L, V(0, -0.7))]},
    "ear_l": {R: [(0, V(0, 0, 6)), (0.25, V(0, -10, 30)), (2.0, V(0, -10, 30)), (L, V(0, 0, 6))]},
    "ear_r": {R: [(0, V(0, 0, -6)), (0.25, V(0, 10, -30)), (2.0, V(0, 10, -30)), (L, V(0, 0, -6))]},
    "arm_l": {R: [(0, AL(80, 18)), (0.3, AL(70, 35)), (2.0, AL(70, 35)), (L, AL(80, 18))]}, "forearm_bone_l": {R: [(0, FL(28)), (0.3, FL(60)), (2.0, FL(60)), (L, FL(28))]},
    "arm_r": {R: [(0, AR(80, 16)), (0.3, AR(70, 35)), (2.0, AR(70, 35)), (L, AR(80, 16))]}, "forearm_bone_r": {R: [(0, FR(30)), (0.3, FR(60)), (2.0, FR(60)), (L, FR(30))]},
    "lid_l": {R: [(0, V(0)), (0.3, V(-8)), (2.0, V(-8)), (L, V(0))]}, "lid_r": {R: [(0, V(0)), (0.3, V(-8)), (2.0, V(-8)), (L, V(0))]},
}))

# ===== 5. walk: gingado corcunda, joelhos dobrando de verdade
L = 1.0
A.append(anim("walk", L, "loop", {
    "leg_l": {R: loopk(L, V(-30), V(-8), V(24), V(-4))}, "calf_bone_l": {R: loopk(L, V(10), V(8), V(20), V(50))},
    "leg_r": {R: loopk(L, V(24), V(-4), V(-30), V(-8))}, "calf_bone_r": {R: loopk(L, V(20), V(50), V(10), V(8))},
    "arm_l": {R: loopk(L, AL(78, -10), AL(80, 15), AL(78, 40), AL(80, 15))}, "forearm_bone_l": {R: loopk(L, FL(15), FL(30), FL(50), FL(30))},
    "arm_r": {R: loopk(L, AR(78, 40), AR(80, 15), AR(78, -10), AR(80, 15))}, "forearm_bone_r": {R: loopk(L, FR(50), FR(30), FR(15), FR(30))},
    "body":  {R: loopk(L, V(22, 6, -4), V(20, 0, 0), V(22, -6, 4), V(20, 0, 0)), P: loopk(L, V(0, -0.8), V(0, 0.1), V(0, -0.8), V(0, 0.1))},
    "head":  {R: loopk(L, V(-18, -5, 4), V(-15, 0, 0), V(-18, 5, -4), V(-15, 0, 0))},
    "ear_l": {R: loopk(L, V(0, 6, 4), V(0, 12, 10), V(0, 6, 4), V(0, 12, 10))},
    "ear_r": {R: loopk(L, V(0, -6, -4), V(0, -12, -10), V(0, -6, -4), V(0, -12, -10))},
    "sack":  {R: loopk(L, V(4, 0, 8), V(-4, 0, 0), V(4, 0, -8), V(-4, 0, 0))},
    "cloth": {R: loopk(L, V(-14), V(-4), V(-14), V(-4))},
}))

# ===== 6. sneak: andar sorrateiro — agachado, pe ante pe, dedos esticados a frente, orelhas deitadas
L = 2.0
A.append(anim("sneak", L, "loop", {
    "body":  {R: loopk(L, V(38, 4, -3), V(40, 0, 0), V(38, -4, 3), V(40, 0, 0)), P: loopk(L, V(0, -2.4), V(0, -2.0), V(0, -2.4), V(0, -2.0))},
    "head":  {R: loopk(L, V(-34, -4), V(-36, 0), V(-34, 4), V(-36, 0))},
    "leg_l": {R: [(0, V(-50)), (0.35, V(-70)), (0.7, V(-40)), (1.0, V(-10)), (1.5, V(-30)), (L, V(-50))]},
    "calf_bone_l": {R: [(0, V(40)), (0.35, V(85)), (0.7, V(50)), (1.0, V(30)), (1.5, V(40)), (L, V(40))]},
    "leg_r": {R: [(0, V(-10)), (0.5, V(-30)), (1.0, V(-50)), (1.35, V(-70)), (1.7, V(-40)), (L, V(-10))]},
    "calf_bone_r": {R: [(0, V(30)), (0.5, V(40)), (1.0, V(40)), (1.35, V(85)), (1.7, V(50)), (L, V(30))]},
    "arm_l": {R: loopk(L, AL(55, 60), AL(52, 66), AL(55, 60), AL(58, 54))}, "forearm_bone_l": {R: loopk(L, FL(55, -10), FL(45, -10), FL(55, -10), FL(65, -10))},
    "arm_r": {R: loopk(L, AR(55, 60), AR(58, 54), AR(55, 60), AR(52, 66))}, "forearm_bone_r": {R: loopk(L, FR(55, 10), FR(65, 10), FR(55, 10), FR(45, 10))},
    "ear_l": {R: loopk(L, V(0, 40, -12), V(0, 36, -8))}, "ear_r": {R: loopk(L, V(0, -40, 12), V(0, -36, 8))},
    "lid_l": {R: loopk(L, V(25), V(28))}, "lid_r": {R: loopk(L, V(25), V(28))},
    "sack":  {R: loopk(L, V(-14), V(-10))}, "cloth": {R: loopk(L, V(-20), V(-18))},
}))

# ===== 7. lurk: espreita — parado bem baixo, esfregando as maos, cabeca seguindo o alvo, sorrisinho
L = 2.6
rub_l = [(i * 0.15, FL(95, (-35 if i % 2 else 25))) for i in range(9)] + [(1.6, FL(95, 0)), (L, FL(95, 25))]
rub_r = [(i * 0.15, FR(95, (35 if i % 2 else -25))) for i in range(9)] + [(1.6, FR(95, 0)), (L, FR(95, -25))]
A.append(anim("lurk", L, "loop", {
    "body":  {R: loopk(L, V(44), V(46)), P: loopk(L, V(0, -3.2), V(0, -3.4))},
    "head":  {R: [(0, V(-40)), (0.65, V(-42, 8, -6)), (1.3, V(-40)), (1.95, V(-42, -8, 6)), (L, V(-40))]},
    "leg_l": {R: [(0, V(-70, -10)), (L, V(-70, -10))]}, "calf_bone_l": {R: [(0, V(110)), (L, V(110))]},
    "leg_r": {R: [(0, V(-50, 10)), (L, V(-50, 10))]},   "calf_bone_r": {R: [(0, V(90)), (L, V(90))]},
    "arm_l": {R: loopk(L, AL(60, 55), AL(62, 58))}, "forearm_bone_l": {R: rub_l},
    "arm_r": {R: loopk(L, AR(60, 55), AR(62, 58))}, "forearm_bone_r": {R: rub_r},
    "ear_l": {R: [(0, V(0, 32, -6)), (1.7, V(0, 32, -6)), (1.78, V(0, 20, 8)), (1.9, V(0, 32, -6)), (L, V(0, 32, -6))]},
    "ear_r": {R: loopk(L, V(0, -32, 6), V(0, -30, 4))},
    "jaw":   {R: [(0, V(0)), (1.0, V(0)), (1.12, V(9)), (1.5, V(9)), (1.62, V(0)), (L, V(0))]},
    "lid_l": {R: loopk(L, V(35), V(38))}, "lid_r": {R: loopk(L, V(35), V(38))},
    "sack":  {R: loopk(L, V(-20), V(-18))},
}))

# ===== 8. flee: fuga desesperada — inclinado, joelhos altos, bracos agitando, boca aberta, saco quicando
L = 0.5
A.append(anim("flee", L, "loop", {
    "leg_l": {R: loopk(L, V(-65), V(-20), V(50), V(10))}, "calf_bone_l": {R: loopk(L, V(30), V(20), V(40), V(100))},
    "leg_r": {R: loopk(L, V(50), V(10), V(-65), V(-20))}, "calf_bone_r": {R: loopk(L, V(40), V(100), V(30), V(20))},
    "arm_l": {R: loopk(L, AL(40, -60, 20), AL(30, -40), AL(40, -60, -20), AL(30, -40))}, "forearm_bone_l": {R: loopk(L, FL(-10), FL(30), FL(-10), FL(30))},
    "arm_r": {R: loopk(L, AR(40, -60, -20), AR(30, -40), AR(40, -60, 20), AR(30, -40))}, "forearm_bone_r": {R: loopk(L, FR(-10), FR(30), FR(-10), FR(30))},
    "body":  {R: loopk(L, V(40, 0, -5), V(38), V(40, 0, 5), V(38)), P: loopk(L, V(0, 0), V(0, 1.4), V(0, 0), V(0, 1.4))},
    "head":  {R: loopk(L, V(-34, 0, 4), V(-26), V(-34, 0, -4), V(-26))},
    "jaw":   {R: loopk(L, V(24), V(18))},
    "lid_l": {R: loopk(L, V(-10), V(-10))}, "lid_r": {R: loopk(L, V(-10), V(-10))},
    "ear_l": {R: loopk(L, V(0, 50, 0), V(0, 60, 10))}, "ear_r": {R: loopk(L, V(0, -50, 0), V(0, -60, -10))},
    "sack":  {R: loopk(L, V(-10, 0, 10), V(-34), V(-10, 0, -10), V(-34))},
    "cloth": {R: loopk(L, V(-30), V(-50))},
}))

# ===== 9. steal: bote rapido com a mao direita, gira o corpo e enfia no saco
L = 1.0
A.append(anim("steal", L, "once", {
    "body":  {R: [(0, V(44)), (0.15, V(52, -20)), (0.35, V(54, -20)), (0.6, V(30, 35)), (0.8, V(26, 40)), (L, V(22))],
              P: [(0, V(0, -3.2)), (0.15, V(0, -2.8)), (0.6, V(0, 0.4)), (L, V(0, -0.8))]},
    "arm_r": {R: [(0, AR(60, 55)), (0.15, AR(30, 100, 0)), (0.35, AR(30, 100, -30)), (0.6, AR(60, -70)), (0.8, AR(70, -90)), (L, AR(80, 16))]},
    "forearm_bone_r": {R: [(0, FR(95)), (0.15, FR(5)), (0.35, FR(60)), (0.6, FR(40)), (0.8, FR(10)), (L, FR(30))]},
    "arm_l": {R: [(0, AL(60, 55)), (0.15, AL(40, 10)), (0.6, AL(60, 0)), (L, AL(80, 18))]}, "forearm_bone_l": {R: [(0, FL(95)), (0.15, FL(20)), (L, FL(28))]},
    "head":  {R: [(0, V(-40)), (0.15, V(-34, -20)), (0.6, V(-20, 30)), (0.8, V(-10, 45)), (L, V(-16))]},
    "jaw":   {R: [(0, V(0)), (0.55, V(0)), (0.65, V(16)), (L, V(4))]},
    "leg_l": {R: [(0, V(-70, -10)), (0.15, V(-50)), (0.6, V(-20)), (L, V(-14))]}, "calf_bone_l": {R: [(0, V(110)), (0.15, V(70)), (0.6, V(40)), (L, V(22))]},
    "leg_r": {R: [(0, V(-50, 10)), (0.15, V(-20)), (0.6, V(-30)), (L, V(-10))]},  "calf_bone_r": {R: [(0, V(90)), (0.15, V(40)), (0.6, V(60)), (L, V(18))]},
    "sack":  {R: [(0, V(-20)), (0.7, V(-20)), (0.8, V(-40, 0, 10)), (0.9, V(-5)), (L, V(0))]},
    "ear_l": {R: [(0, V(0, 32, -6)), (0.5, V(0, -10, 34)), (L, V(0, 0, 6))]}, "ear_r": {R: [(0, V(0, -32, 6)), (0.5, V(0, 10, -34)), (L, V(0, 0, -6))]},
    "lid_l": {R: [(0, V(35)), (0.15, V(0)), (L, V(0))]}, "lid_r": {R: [(0, V(35)), (0.15, V(0)), (L, V(0))]},
}))

# ===== 10. giggle: risadinha depois do furto — mao na boca, ombros chacoalhando
L = 1.2
shake = lambda a, b, n=8, dt=0.08: [(i * dt, a if i % 2 == 0 else b) for i in range(n)]
A.append(anim("giggle", L, "once", {
    "body":  {R: shake(V(18), V(13)) + [(0.8, V(18)), (L, V(20))], P: [(0, V(0, -0.7)), (L, V(0, -0.7))]},
    "head":  {R: shake(V(-10, 0, 5), V(-18, 0, -5)) + [(0.8, V(-12)), (L, V(-16))]},
    "jaw":   {R: shake(V(18), V(4)) + [(0.8, V(0)), (L, V(0))]},
    "arm_r": {R: [(0, AR(30, 70)), (0.8, AR(30, 70)), (L, AR(80, 16))]}, "forearm_bone_r": {R: [(0, FR(115)), (0.8, FR(115)), (L, FR(30))]},
    "arm_l": {R: [(0, AL(70, 40)), (0.8, AL(70, 40)), (L, AL(80, 18))]}, "forearm_bone_l": {R: [(0, FL(70)), (0.8, FL(70)), (L, FL(28))]},
    "lid_l": {R: [(0, V(60)), (0.8, V(60)), (L, V(0))]}, "lid_r": {R: [(0, V(60)), (0.8, V(60)), (L, V(0))]},
    "ear_l": {R: shake(V(0, 0, 14), V(0, 0, 4)) + [(L, V(0, 0, 6))]}, "ear_r": {R: shake(V(0, 0, -14), V(0, 0, -4)) + [(L, V(0, 0, -6))]},
}))

# ===== 11. startled: susto — pula, olhos arregalados, bracos pro alto, depois encolhe
L = 0.8
A.append(anim("startled", L, "once", {
    "root":  {P: [(0, V(0)), (0.12, V(0, 3.5)), (0.3, V(0, 0)), (L, V(0))]},
    "body":  {R: [(0, V(20)), (0.12, V(-8)), (0.35, V(34)), (L, V(30))], P: [(0, V(0, -0.7)), (0.12, V(0)), (0.35, V(0, -1.6)), (L, V(0, -1.6))]},
    "head":  {R: [(0, V(-16)), (0.12, V(8)), (0.35, V(-34)), (L, V(-30))]},
    "arm_l": {R: [(0, AL(80, 18)), (0.12, AL(-50, 20)), (0.35, AL(50, 60)), (L, AL(55, 60))]}, "forearm_bone_l": {R: [(0, FL(28)), (0.12, FL(10)), (0.35, FL(80)), (L, FL(80))]},
    "arm_r": {R: [(0, AR(80, 16)), (0.12, AR(-50, 20)), (0.35, AR(50, 60)), (L, AR(55, 60))]}, "forearm_bone_r": {R: [(0, FR(30)), (0.12, FR(10)), (0.35, FR(80)), (L, FR(80))]},
    "leg_l": {R: [(0, V(-14)), (0.12, V(-30, 0, -10)), (0.3, V(-30)), (L, V(-30))]}, "calf_bone_l": {R: [(0, V(22)), (0.12, V(60)), (0.3, V(50)), (L, V(50))]},
    "leg_r": {R: [(0, V(-10)), (0.12, V(-30, 0, 10)), (0.3, V(-30)), (L, V(-30))]}, "calf_bone_r": {R: [(0, V(18)), (0.12, V(60)), (0.3, V(50)), (L, V(50))]},
    "jaw":   {R: [(0, V(0)), (0.1, V(26)), (0.5, V(12)), (L, V(6))]},
    "lid_l": {R: [(0, V(0)), (0.08, V(-20)), (L, V(-12))]}, "lid_r": {R: [(0, V(0)), (0.08, V(-20)), (L, V(-12))]},
    "ear_l": {R: [(0, V(0, 0, 6)), (0.1, V(0, -15, 50)), (L, V(0, -10, 34))]}, "ear_r": {R: [(0, V(0, 0, -6)), (0.1, V(0, 15, -50)), (L, V(0, 10, -34))]},
    "sack":  {R: [(0, V(0)), (0.12, V(-30)), (0.35, V(10)), (L, V(0))]},
}))

# ===== 12. cower: encurralado — agachado, maos sobre a cabeca, tremendo, olhos apertados
L = 0.4
A.append(anim("cower", L, "loop", {
    "body":  {R: loopk(L, V(50, 0, -2), V(50, 0, 2)), P: loopk(L, V(0, -3.6), V(0, -3.6))},
    "head":  {R: loopk(L, V(12), V(14))},
    "arm_l": {R: loopk(L, AL(-20, 80, 0), AL(-18, 82, 0))}, "forearm_bone_l": {R: loopk(L, FL(120), FL(116))},
    "arm_r": {R: loopk(L, AR(-20, 80, 0), AR(-18, 82, 0))}, "forearm_bone_r": {R: loopk(L, FR(120), FR(116))},
    "leg_l": {R: loopk(L, V(-80, -8), V(-80, -8))}, "calf_bone_l": {R: loopk(L, V(130), V(130))},
    "leg_r": {R: loopk(L, V(-80, 8), V(-80, 8))},  "calf_bone_r": {R: loopk(L, V(130), V(130))},
    "ear_l": {R: loopk(L, V(0, 45, -30), V(0, 47, -32))}, "ear_r": {R: loopk(L, V(0, -45, 30), V(0, -47, 32))},
    "lid_l": {R: loopk(L, V(80), V(80))}, "lid_r": {R: loopk(L, V(80), V(80))},
    "jaw":   {R: loopk(L, V(5), V(2))},
}))

# ===== 13. eat: de cocoras, segura a comida com as duas maos, morde, puxa, mastiga
L = 1.6
A.append(anim("eat", L, "loop", {
    "body":  {R: loopk(L, V(26), V(30), V(26), V(28)), P: loopk(L, V(0, -3.4), V(0, -3.4))},
    "leg_l": {R: loopk(L, V(-85, -14), V(-85, -14))}, "calf_bone_l": {R: loopk(L, V(140), V(140))},
    "leg_r": {R: loopk(L, V(-85, 14), V(-85, 14))},  "calf_bone_r": {R: loopk(L, V(140), V(140))},
    "arm_l": {R: [(0, AL(60, 50)), (0.3, AL(40, 75)), (0.45, AL(45, 65, -20)), (0.7, AL(60, 50)), (L, AL(60, 50))]},
    "forearm_bone_l": {R: [(0, FL(70)), (0.3, FL(115)), (0.45, FL(100)), (0.7, FL(70)), (L, FL(70))]},
    "arm_r": {R: [(0, AR(60, 50)), (0.3, AR(40, 75)), (0.45, AR(45, 65, 20)), (0.7, AR(60, 50)), (L, AR(60, 50))]},
    "forearm_bone_r": {R: [(0, FR(70)), (0.3, FR(115)), (0.45, FR(100)), (0.7, FR(70)), (L, FR(70))]},
    "head":  {R: [(0, V(-8)), (0.3, V(6)), (0.45, V(-4, 0, 8)), (0.7, V(0)), (0.9, V(4, 0, -3)), (1.1, V(0)), (1.3, V(4, 0, 3)), (L, V(-8))]},
    "jaw":   {R: [(0, V(0)), (0.25, V(24)), (0.35, V(0)), (0.55, V(14)), (0.7, V(0)), (0.85, V(14)), (1.0, V(0)), (1.15, V(12)), (1.3, V(0)), (L, V(0))]},
    "lid_l": {R: [(0, V(0)), (0.3, V(50)), (0.5, V(0)), (L, V(0))]}, "lid_r": {R: [(0, V(0)), (0.3, V(50)), (0.5, V(0)), (L, V(0))]},
    "ear_l": {R: loopk(L, V(0, 0, 10), V(0, 0, 20))}, "ear_r": {R: loopk(L, V(0, 0, -10), V(0, 0, -20))},
}))

# ===== 14. ring_on: tira o anel, gira no dedo, coloca, olha pros lados e se agacha (invisibilidade no fim, pelo codigo)
L = 1.6
A.append(anim("ring_on", L, "once", {
    "body":  {R: [(0, V(20)), (0.4, V(28)), (1.2, V(30)), (L, V(40))], P: [(0, V(0, -0.7)), (1.2, V(0, -1.4)), (L, V(0, -2.4))]},
    "arm_l": {R: [(0, AL(80, 18)), (0.35, AL(50, 70)), (1.0, AL(50, 70)), (L, AL(55, 60))]}, "forearm_bone_l": {R: [(0, FL(28)), (0.35, FL(80, -40)), (1.0, FL(80, -40)), (L, FL(55))]},
    "arm_r": {R: [(0, AR(80, 16)), (0.35, AR(45, 72)), (0.5, AR(42, 76)), (0.65, AR(45, 72)), (0.8, AR(42, 76)), (1.0, AR(45, 72)), (L, AR(55, 60))]},
    "forearm_bone_r": {R: [(0, FR(30)), (0.35, FR(85, 30)), (0.5, FR(85, -30)), (0.65, FR(85, 30)), (0.8, FR(85, -30)), (1.0, FR(85)), (L, FR(55))]},
    "head":  {R: [(0, V(-16)), (0.35, V(14)), (1.0, V(14)), (1.15, V(-24, 35)), (1.35, V(-24, -35)), (L, V(-34))]},
    "jaw":   {R: [(0, V(0)), (0.9, V(0)), (1.0, V(10)), (1.1, V(0)), (L, V(0))]},
    "lid_l": {R: [(0, V(0)), (0.35, V(20)), (1.0, V(20)), (1.1, V(0)), (L, V(30))]}, "lid_r": {R: [(0, V(0)), (0.35, V(20)), (1.0, V(20)), (1.1, V(0)), (L, V(30))]},
    "leg_l": {R: [(0, V(-14)), (L, V(-50))]}, "calf_bone_l": {R: [(0, V(22)), (L, V(80))]},
    "leg_r": {R: [(0, V(-10)), (L, V(-40))]}, "calf_bone_r": {R: [(0, V(18)), (L, V(70))]},
    "ear_l": {R: [(0, V(0, 0, 6)), (1.1, V(0, 0, 6)), (L, V(0, 38, -10))]}, "ear_r": {R: [(0, V(0, 0, -6)), (1.1, V(0, 0, -6)), (L, V(0, -38, 10))]},
}))

# ===== 15. shield_light: ofuscado pela luz — vira o rosto, braco na frente dos olhos, recua
L = 1.4
A.append(anim("shield_light", L, "loop", {
    "body":  {R: loopk(L, V(10, 25, -6), V(12, 28, -6)), P: loopk(L, V(0, -1.0), V(0, -1.2))},
    "head":  {R: loopk(L, V(10, 30, 8), V(12, 34, 8))},
    "arm_l": {R: loopk(L, AL(10, 75, 0), AL(14, 72, 0))}, "forearm_bone_l": {R: loopk(L, FL(120, -30), FL(124, -30))},
    "arm_r": {R: loopk(L, AR(70, -20), AR(72, -24))}, "forearm_bone_r": {R: loopk(L, FR(40), FR(44))},
    "leg_l": {R: loopk(L, V(-20), V(-22))}, "calf_bone_l": {R: loopk(L, V(30), V(34))},
    "leg_r": {R: loopk(L, V(10), V(12))},   "calf_bone_r": {R: loopk(L, V(30), V(34))},
    "lid_l": {R: loopk(L, V(75), V(80))}, "lid_r": {R: loopk(L, V(75), V(80))},
    "jaw":   {R: loopk(L, V(8), V(4))},
    "ear_l": {R: loopk(L, V(0, 40, -20), V(0, 42, -22))}, "ear_r": {R: loopk(L, V(0, -40, 20), V(0, -42, 22))},
}))

# ===== 16. sniff: fareja o ar (achou rato / item) — cabeca empurrando pra frente em golpes curtos
L = 1.2
pk = lambda a, b: [(0, a), (0.1, b), (0.2, a), (0.3, b), (0.4, a), (0.9, a), (1.0, b), (1.1, a), (L, a)]
A.append(anim("sniff", L, "loop", {
    "head":  {R: pk(V(-30), V(-24)), P: pk(V(0, 0, 0), V(0, 0, -0.6))},
    "body":  {R: loopk(L, V(28), V(30)), P: loopk(L, V(0, -1.2), V(0, -1.3))},
    "ear_l": {R: loopk(L, V(0, -6, 26), V(0, -10, 30))}, "ear_r": {R: loopk(L, V(0, 6, -26), V(0, 10, -30))},
    "arm_l": {R: loopk(L, AL(70, 30), AL(70, 32))}, "forearm_bone_l": {R: loopk(L, FL(60), FL(62))},
    "arm_r": {R: loopk(L, AR(70, 30), AR(70, 32))}, "forearm_bone_r": {R: loopk(L, FR(60), FR(62))},
    "leg_l": {R: loopk(L, V(-24), V(-24))}, "calf_bone_l": {R: loopk(L, V(40), V(40))},
    "leg_r": {R: loopk(L, V(-20), V(-20))}, "calf_bone_r": {R: loopk(L, V(36), V(36))},
    "lid_l": {R: pk(V(30), V(45))}, "lid_r": {R: pk(V(30), V(45))},
}))

# ------------------------------------------------------------------ pe no chao
# altura do pe = 5 - 2cos(coxa) - 3cos(coxa+joelho); o root desce/sobe pro pe mais baixo tocar o chao.
import math
def track(a, bone, ch="rotation"):
    an = a["animators"].get(grp[bone]["uuid"])
    if not an: return []
    return sorted([(k["time"], k["data_points"][0]["x"]) for k in an["keyframes"] if k["channel"] == ch])
def sample(tr, t):
    if not tr: return 0.0
    if t <= tr[0][0]: return tr[0][1]
    for (t0, v0), (t1, v1) in zip(tr, tr[1:]):
        if t0 <= t <= t1: return v0 + (v1 - v0) * (t - t0) / max(1e-6, t1 - t0)
    return tr[-1][1]
for a in A:
    legs = {b: track(a, b) for b in ("leg_l", "leg_r", "calf_bone_l", "calf_bone_r")}
    if not legs["leg_l"]: continue
    times = sorted({t for tr in legs.values() for t, _ in tr})
    body = a["animators"].get(grp["body"]["uuid"])
    if body: body["keyframes"] = [k for k in body["keyframes"] if k["channel"] != "position"]
    root = a["animators"].setdefault(grp["root"]["uuid"], {"name": "root", "type": "bone", "keyframes": []})
    jump = sorted([(k["time"], k["data_points"][0]["y"]) for k in root["keyframes"] if k["channel"] == "position"])
    root["keyframes"] = [k for k in root["keyframes"] if k["channel"] != "position"]
    times = sorted(set(times) | {t for t, _ in jump})
    for t in times:
        h = min(5 - 2 * math.cos(math.radians(sample(legs["leg_" + s], t))) - 3 * math.cos(math.radians(sample(legs["leg_" + s], t) + sample(legs["calf_bone_" + s], t))) for s in "lr")
        y = round(-h + sample(jump, t) if jump else -h, 3)
        root["keyframes"].append({"channel": "position", "data_points": [{"x": 0, "y": y, "z": 0}], "uuid": U(), "time": t, "color": -1, "interpolation": "catmullrom"})

m["animations"] = A
json.dump(m, open(os.path.join(D, "cave_goblin.bbmodel"), "w"))
print("ok:", len(A), "animacoes;", [a["name"].split(".")[-1] for a in A])

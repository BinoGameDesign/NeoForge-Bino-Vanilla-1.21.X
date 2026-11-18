# Refaz as animacoes do rat.bbmodel (modelo e textura do Bino) sem mexer nos cubos.
# Convencao do Blockbench: X+ = frente sobe (cabeca olha pra cima / perna vai pra frente / cauda desce / mandibula fecha);
# Y+ = frente vira para -x; Z+ = lado +x sobe.  Rodar: python3 tools/blockbench/anim_rat.py
import json, uuid, os, math
D = os.path.dirname(os.path.abspath(__file__)); P = os.path.join(D, "rat.bbmodel")
m = json.load(open(P)); U = lambda: str(uuid.uuid4())
grp = {g["name"]: g for g in m["groups"]}
grp["jaw"]["origin"] = [0, 2.9, -4.6]   # pivo da mandibula no fundo da boca (estava no mesmo ponto da orelha)

R, Pn = "rotation", "position"
V = lambda x=0, y=0, z=0: [x, y, z]
def anim(name, length, loop, tracks):
    animators = {}
    for bone, chans in tracks.items():
        kfs = []
        for ch, pts in chans.items():
            for p in sorted(pts, key=lambda q: q[0]):
                kfs.append({"channel": ch, "data_points": [{"x": round(p[1][0], 3), "y": round(p[1][1], 3), "z": round(p[1][2], 3)}], "uuid": U(),
                            "time": round(p[0], 4), "color": -1, "interpolation": p[2] if len(p) > 2 else "catmullrom"})
        animators[grp[bone]["uuid"]] = {"name": bone, "type": "bone", "keyframes": kfs}
    return {"uuid": U(), "name": "animation.rat." + name, "loop": loop, "override": False, "length": length, "snapping": 24,
            "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}
def loopk(L, *vals): n = len(vals); return [(L * i / n, vals[i]) for i in range(n)] + [(L, vals[0])]
def hold(L, v): return [(0, v), (L, v)]

# bigodes: os dois lados juntos (o lado esquerdo esta girado 180 em Z, entao o sinal inverte)
def whisk(tr, L, amp, n):
    a = [(L * i / n, V(0, 0, amp if i % 2 else 0), "linear") for i in range(n)] + [(L, V(), "linear")]
    tr["whiskers"] = {R: a}
    tr["whiskers2"] = {R: [(t, V(0, 0, -v[2]), ip) for t, v, ip in a]}

# cauda em onda: 3 segmentos, cada um atrasado e com mais amplitude que o anterior (a onda "corre" ate a ponta)
def tail(tr, L, side=0.0, curl=0.0, base=(0, 0, 0), cycles=1, lag=0.18, keys=12, curl_phase=0.25):
    for k, bone in enumerate(("tail", "tail_tip", "tail_tip2")):
        gain = (1.0, 1.35, 1.7)[k]
        pts = []
        for i in range(keys + 1):
            t = L * i / keys
            ph = 2 * math.pi * (cycles * t / L - lag * k)
            pts.append((t, V(base[0] * (1 if k == 0 else 0.5) + curl * gain * math.sin(ph + 2 * math.pi * curl_phase),
                             base[1] * (1 if k == 0 else 0.6) + side * gain * math.sin(ph),
                             base[2])))
        tr[bone] = {R: pts}
    return tr

A = []
# ===== 1. idle: respira, fareja, cabeca espiando, cauda serpenteando devagar
L = 2.4; t = {
    "body":  {R: loopk(L, V(0), V(1)), Pn: loopk(L, V(0, 0, 0), V(0, 0.15, 0))},
    "head":  {R: [(0, V(0)), (0.6, V(4, 12, 0)), (1.2, V(-2, 0, 0)), (1.8, V(3, -10, 3)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (1.5, V(0)), (1.55, V(0, -15, 10)), (1.65, V(0)), (L, V(0))]},
    "ear_r": {R: loopk(L, V(0), V(0, 5, -4))},
    "jaw":   {R: [(0, V(0)), (1.9, V(0)), (2.0, V(-6)), (2.1, V(0)), (L, V(0))]},
}
whisk(t, L, 6, 24); tail(t, L, side=10, curl=4); A.append(anim("idle", L, "loop", t))

# ===== 2. sniff: fareja rapido, bigodes a mil, cauda agitada
L = 1.0; t = {
    "head":  {R: [(0, V(8)), (0.1, V(14)), (0.2, V(8)), (0.3, V(14)), (0.5, V(10, 12)), (0.6, V(16, 12)), (0.7, V(10, 12)), (0.8, V(16, 12)), (L, V(8))]},
    "body":  {R: loopk(L, V(3), V(4))},
    "ear_l": {R: loopk(L, V(0, 0, 8), V(0, 0, 12))}, "ear_r": {R: loopk(L, V(0, 0, -8), V(0, 0, -12))},
}
whisk(t, L, 10, 20); tail(t, L, side=6, curl=3, cycles=2); A.append(anim("sniff", L, "loop", t))

# ===== 3. walk: passo diagonal, corpo ondulando, cauda serpenteando no ritmo
L = 0.6; t = {
    "leg_fl": {R: loopk(L, V(30), V(-25))}, "leg_br": {R: loopk(L, V(25), V(-25))},
    "leg_fr": {R: loopk(L, V(-25), V(30))}, "leg_bl": {R: loopk(L, V(-25), V(25))},
    "body":  {R: loopk(L, V(0, 4, 0), V(0, -4, 0)), Pn: loopk(L, V(0, 0, 0), V(0, 0.3, 0), V(0, 0, 0), V(0, 0.3, 0))},
    "head":  {R: loopk(L, V(-2, -5, 0), V(-2, 5, 0))},
}
whisk(t, L, 5, 6); tail(t, L, side=16, curl=3, lag=0.22); A.append(anim("walk", L, "loop", t))

# ===== 4. run: galope, orelhas pra tras, cauda erguida e chicoteando
L = 0.3; t = {
    "leg_fl": {R: loopk(L, V(55), V(-40))}, "leg_fr": {R: loopk(L, V(45), V(-50))},
    "leg_bl": {R: loopk(L, V(-50), V(45))}, "leg_br": {R: loopk(L, V(-40), V(55))},
    "body":  {R: loopk(L, V(8), V(-8)), Pn: loopk(L, V(0, 0, 0), V(0, 1.0, 0))},
    "head":  {R: loopk(L, V(-10), V(4))},
    "ear_l": {R: hold(L, V(-30))}, "ear_r": {R: hold(L, V(-30))},
    "jaw":   {R: hold(L, V(-5))},
}
tail(t, L, side=8, curl=10, base=(-8, 0, 0), keys=8); A.append(anim("run", L, "loop", t))

# ===== 5. groom: senta, esfrega o rosto com as patinhas alternando, lambe, passa nas orelhas
L = 3.2; rub = [(0.5 + i * 0.16, i % 2) for i in range(10)]; t = {
    "body":  {R: [(0, V(0)), (0.4, V(40)), (2.8, V(40)), (L, V(0))], Pn: [(0, V(0)), (0.4, V(0, 0.3, 0)), (2.8, V(0, 0.3, 0)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.4, V(-40)), (1.3, V(-40, 0, 8)), (1.35, V(-48, 0, -8)), (2.0, V(-30, 20, 15)), (2.4, V(-30, -20, -15)), (2.8, V(-40)), (L, V(0))]},
    "jaw":   {R: [(0, V(0)), (0.5, V(0))] + [(0.5 + i * 0.16, V(-10 if i % 2 else 0)) for i in range(10)] + [(L, V(0))]},
    "leg_fl": {R: [(0, V(0))] + [(tt, V(130 if s else 105, 0, -15)) for tt, s in rub] + [(2.0, V(150, -30, -20)), (2.4, V(110)), (2.8, V(110)), (L, V(0))]},
    "leg_fr": {R: [(0, V(0))] + [(tt, V(105 if s else 130, 0, 15)) for tt, s in rub] + [(2.0, V(110)), (2.4, V(150, 30, 20)), (2.8, V(110)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.4, V(-40)), (2.8, V(-40)), (L, V(0))]},
    "leg_br": {R: [(0, V(0)), (0.4, V(-40)), (2.8, V(-40)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (2.0, V(0)), (2.1, V(-20, 0, -20)), (2.3, V(0)), (L, V(0))]},
    "ear_r": {R: [(0, V(0)), (2.4, V(0)), (2.5, V(-20, 0, 20)), (2.7, V(0)), (L, V(0))]},
}
whisk(t, L, 5, 16)
tail(t, L, side=6, curl=3, base=(-10, 40, 0), cycles=2)
for b in ("tail", "tail_tip", "tail_tip2"):   # entra e sai da pose de lado suavemente
    pts = t[b][R]; t[b][R] = [(0, V())] + [p for p in pts if 0.4 <= p[0] <= 2.8] + [(L, V())]
A.append(anim("groom", L, "once", t))

# ===== 6. stand_up: em pe como suricato, olha em volta, fareja; cauda apoiada no chao com a ponta mexendo
L = 3.4; t = {
    "body":  {R: [(0, V(0)), (0.35, V(72)), (2.9, V(72)), (L, V(0))], Pn: [(0, V(0)), (0.35, V(0, 0.8, 0)), (2.9, V(0, 0.8, 0)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.35, V(-70)), (0.9, V(-62, 35)), (1.3, V(-62, 35)), (1.6, V(-60, -40)), (2.1, V(-60, -40)), (2.3, V(-58, 0)),
                  (2.4, V(-52)), (2.5, V(-58)), (2.6, V(-52)), (2.9, V(-70)), (L, V(0))]},
    "leg_fl": {R: [(0, V(0)), (0.35, V(-60, 0, -10)), (2.9, V(-60, 0, -10)), (L, V(0))]},
    "leg_fr": {R: [(0, V(0)), (0.35, V(-60, 0, 10)), (2.9, V(-60, 0, 10)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.35, V(-30)), (2.9, V(-30)), (L, V(0))]},
    "leg_br": {R: [(0, V(0)), (0.35, V(-30)), (2.9, V(-30)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.4, V(10, 0, 15)), (2.9, V(10, 0, 15)), (L, V(0))]}, "ear_r": {R: [(0, V(0)), (0.4, V(10, 0, -15)), (2.9, V(10, 0, -15)), (L, V(0))]},
    "tail":  {R: [(0, V()), (0.35, V(-70)), (2.9, V(-70)), (L, V())]},
    "tail_tip": {R: [(0, V()), (0.35, V(-15)), (2.9, V(-15)), (L, V())]},
    "tail_tip2": {R: [(0, V()), (0.35, V(0, 0, 0))] + [(0.35 + i * 0.32, V(0, 25 if i % 2 else -25, 0)) for i in range(1, 8)] + [(L, V())]},
}
whisk(t, L, 8, 30); A.append(anim("stand_up", L, "once", t))

# ===== 7. eat: sentadinho, segura a comida, mordisca (mandibula), cauda enrolada de lado
L = 1.0; t = {
    "body":  {R: loopk(L, V(38), V(40)), Pn: hold(L, V(0, 0.3, 0))},
    "head":  {R: [(0, V(-30)), (0.1, V(-38)), (0.2, V(-30)), (0.3, V(-38)), (0.4, V(-30)), (0.6, V(-34, 6)), (0.7, V(-40, 6)), (0.8, V(-34, 6)), (L, V(-30))]},
    "jaw":   {R: [(0, V(0)), (0.1, V(-14)), (0.2, V(0)), (0.3, V(-14)), (0.4, V(0)), (0.6, V(0)), (0.7, V(-12)), (0.8, V(0)), (L, V(0))]},
    "leg_fl": {R: loopk(L, V(120, 0, -18), V(126, 0, -18))}, "leg_fr": {R: loopk(L, V(120, 0, 18), V(126, 0, 18))},
    "leg_bl": {R: hold(L, V(-38))}, "leg_br": {R: hold(L, V(-38))},
}
whisk(t, L, 8, 10); tail(t, L, side=4, curl=2, base=(-10, 35, 0)); A.append(anim("eat", L, "loop", t))

# ===== 8. attack: bote, boca abre e morde, cauda chicoteia
L = 0.45; t = {
    "body":  {R: [(0, V(0)), (0.1, V(-10)), (0.2, V(6)), (L, V(0))], Pn: [(0, V(0)), (0.1, V(0, 0, 1.0)), (0.2, V(0, 0.6, -2.0)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.1, V(20)), (0.2, V(-20)), (0.28, V(-10)), (L, V(0))]},
    "jaw":   {R: [(0, V(0)), (0.1, V(-35)), (0.2, V(0)), (L, V(0))]},
    "leg_fl": {R: [(0, V(0)), (0.2, V(60)), (L, V(0))]}, "leg_fr": {R: [(0, V(0)), (0.2, V(60)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.1, V(20)), (0.2, V(-40)), (L, V(0))]}, "leg_br": {R: [(0, V(0)), (0.1, V(20)), (0.2, V(-40)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.1, V(-35)), (L, V(0))]}, "ear_r": {R: [(0, V(0)), (0.1, V(-35)), (L, V(0))]},
    "tail":  {R: [(0, V()), (0.1, V(-10, 25)), (0.2, V(-10, -30)), (0.3, V(0, 10)), (L, V())]},
    "tail_tip": {R: [(0, V()), (0.12, V(0, 35)), (0.22, V(0, -40)), (0.32, V(0, 15)), (L, V())]},
    "tail_tip2": {R: [(0, V()), (0.14, V(0, 45)), (0.24, V(0, -50)), (0.34, V(0, 20)), (L, V())]},
}
A.append(anim("attack", L, "once", t))

# ===== 9. cower: achatado, orelhas coladas, tremendo, cauda enrolada junto ao corpo
L = 0.3; t = {
    "body":  {R: loopk(L, V(0, 0, -2), V(0, 0, 2)), Pn: hold(L, V(0, -1.0, 0))},
    "head":  {R: hold(L, V(-8))},
    "leg_fl": {R: hold(L, V(40))}, "leg_fr": {R: hold(L, V(40))},
    "leg_bl": {R: hold(L, V(-40))}, "leg_br": {R: hold(L, V(-40))},
    "ear_l": {R: hold(L, V(-50, 0, -10))}, "ear_r": {R: hold(L, V(-50, 0, 10))},
    "tail":  {R: loopk(L, V(-10, 55), V(-10, 58))}, "tail_tip": {R: loopk(L, V(0, 50), V(0, 52))}, "tail_tip2": {R: loopk(L, V(0, 45), V(0, 48))},
}
whisk(t, L, 4, 6); A.append(anim("cower", L, "loop", t))

# ===== 10. scratch: coca a orelha com a pata de tras; cauda balanca com o tranco
L = 1.6; sc = [(0.35 + i * 0.07, V(95 if i % 2 else 70, 0, 20)) for i in range(14)]; t = {
    "body":  {R: [(0, V(0)), (0.3, V(25, 0, 15)), (1.35, V(25, 0, 15)), (L, V(0))]},
    "head":  {R: [(0, V(0)), (0.3, V(-15, -10, 30)), (1.35, V(-15, -10, 30)), (L, V(0))]},
    "leg_bl": {R: [(0, V(0)), (0.3, V(80, 0, 20))] + sc + [(1.35, V(80, 0, 20)), (L, V(0))]},
    "leg_br": {R: [(0, V(0)), (0.3, V(-20)), (1.35, V(-20)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.35, V(-20, 0, -10))] + [(0.35 + i * 0.07, V(-20 - (8 if i % 2 else 0), 0, -10)) for i in range(14)] + [(L, V(0))]},
}
tail(t, L, side=5, curl=0, base=(-6, -40, 0), cycles=8, keys=32)
for b in ("tail", "tail_tip", "tail_tip2"):
    pts = t[b][R]; t[b][R] = [(0, V())] + [p for p in pts if 0.3 <= p[0] <= 1.35] + [(L, V())]
A.append(anim("scratch", L, "once", t))

# ===== 11. sleep: enrolado, cauda abracando o corpo, respiracao lenta, ponta da cauda tremendo de vez em quando
L = 3.0; t = {
    "body":  {R: loopk(L, V(0), V(-1)), Pn: loopk(L, V(0, -1.4, 0), V(0, -1.25, 0))},
    "head":  {R: loopk(L, V(-25, 35, -10), V(-26, 35, -10))},
    "ear_l": {R: hold(L, V(-30))}, "ear_r": {R: hold(L, V(-30))},
    "leg_fl": {R: hold(L, V(60))}, "leg_fr": {R: hold(L, V(60))},
    "leg_bl": {R: hold(L, V(-50))}, "leg_br": {R: hold(L, V(-50))},
    "tail":  {R: hold(L, V(-10, -70))}, "tail_tip": {R: hold(L, V(0, -60))},
    "tail_tip2": {R: [(0, V(0, -55)), (2.0, V(0, -55)), (2.1, V(0, -40)), (2.2, V(0, -58)), (L, V(0, -55))]},
}
A.append(anim("sleep", L, "loop", t))

# ===== 12. squeak: guincho — cabeca pra cima, boca abre, cauda da um tranco
L = 0.5; t = {
    "head":  {R: [(0, V(0)), (0.08, V(25)), (0.15, V(20)), (0.22, V(25)), (0.3, V(20)), (L, V(0))]},
    "jaw":   {R: [(0, V(0)), (0.08, V(-22)), (0.3, V(-18)), (0.4, V(0)), (L, V(0))]},
    "body":  {R: [(0, V(0)), (0.1, V(8)), (L, V(0))]},
    "ear_l": {R: [(0, V(0)), (0.08, V(15, 0, 15)), (L, V(0))]}, "ear_r": {R: [(0, V(0)), (0.08, V(15, 0, -15)), (L, V(0))]},
    "tail":  {R: [(0, V()), (0.08, V(-25)), (0.25, V(-10)), (L, V())]}, "tail_tip": {R: [(0, V()), (0.1, V(-20)), (0.3, V(-5)), (L, V())]},
    "tail_tip2": {R: [(0, V()), (0.12, V(-25)), (0.32, V(0)), (L, V())]},
}
whisk(t, L, 12, 8); A.append(anim("squeak", L, "once", t))

m["animations"] = A
json.dump(m, open(P, "w"))
print("ok:", [a["name"].split(".")[-1] for a in A])

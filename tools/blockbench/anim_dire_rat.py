# Animacoes proprias do Rato Gigante (dire_rat.bbmodel do Bino), sem mexer nos cubos nem nas outras animacoes:
#  - attack: mordida mais agressiva (empina, abre a boca, da o bote e sacode a cabeca)
#  - tail_whip: chicotada de cauda (gira o corpo, a cauda varre na frente com atraso em cada ponta)
# Convencao do Blockbench: X+ = frente sobe / perna vai pra frente / cauda desce / mandibula fecha; Y+ = frente vira para -x.
# Rodar: python3 tools/blockbench/anim_dire_rat.py   (depois: bbmodel_to_java.py para gerar DireRatModelData/DireRatAnimations)
import json, uuid, os
D = os.path.dirname(os.path.abspath(__file__)); P = os.path.join(D, "dire_rat.bbmodel")
m = json.load(open(P)); U = lambda: str(uuid.uuid4())
grp = {g["name"]: g for g in m["groups"]}
R, Pn = "rotation", "position"
V = lambda x=0, y=0, z=0: [x, y, z]

def anim(name, length, tracks):
    animators = {}
    for bone, chans in tracks.items():
        kfs = []
        for ch, pts in chans.items():
            for p in sorted(pts, key=lambda q: q[0]):
                kfs.append({"channel": ch, "data_points": [{"x": round(p[1][0], 3), "y": round(p[1][1], 3), "z": round(p[1][2], 3)}], "uuid": U(),
                            "time": round(p[0], 4), "color": -1, "interpolation": p[2] if len(p) > 2 else "catmullrom"})
        animators[grp[bone]["uuid"]] = {"name": bone, "type": "bone", "keyframes": kfs}
    return {"uuid": U(), "name": "animation.rat." + name, "loop": "once", "override": False, "length": length, "snapping": 24,
            "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}

# ---------------- mordida (0,55 s; o dano sai no bote ~0,22 s)
L = 0.55
attack = anim("attack", L, {
    "body":   {R: [(0, V()), (0.12, V(14, 0, 0)), (0.22, V(-12, 0, 0)), (0.32, V(-6, 0, 0)), (L, V())],
               Pn: [(0, V()), (0.12, V(0, 0.3, 1.2)), (0.22, V(0, 0.5, -3.2)), (0.32, V(0, 0.2, -2.4)), (L, V())]},
    "head":   {R: [(0, V()), (0.12, V(28, 0, 0)), (0.22, V(-28, 0, 0)), (0.3, V(-18, 18, 12)), (0.38, V(-18, -18, -12)), (0.46, V(-8, 8, 4)), (L, V())]},
    "jaw":    {R: [(0, V()), (0.12, V(-50, 0, 0)), (0.2, V(-50, 0, 0)), (0.23, V(2, 0, 0)), (0.4, V(0, 0, 0)), (L, V())]},
    "ear_l":  {R: [(0, V()), (0.1, V(-40, 0, 0)), (0.4, V(-40, 0, 0)), (L, V())]},
    "ear_r":  {R: [(0, V()), (0.1, V(-40, 0, 0)), (0.4, V(-40, 0, 0)), (L, V())]},
    "leg_fl": {R: [(0, V()), (0.12, V(-25, 0, 0)), (0.22, V(70, 0, 0)), (0.36, V(40, 0, 0)), (L, V())]},
    "leg_fr": {R: [(0, V()), (0.12, V(-25, 0, 0)), (0.22, V(70, 0, 0)), (0.36, V(40, 0, 0)), (L, V())]},
    "leg_bl": {R: [(0, V()), (0.12, V(25, 0, 0)), (0.22, V(-45, 0, 0)), (L, V())]},
    "leg_br": {R: [(0, V()), (0.12, V(25, 0, 0)), (0.22, V(-45, 0, 0)), (L, V())]},
    "tail":      {R: [(0, V()), (0.12, V(-25, 0, 0)), (0.22, V(-10, 20, 0)), (0.34, V(-5, -15, 0)), (L, V())]},
    "tail_tip":  {R: [(0, V()), (0.14, V(-10, 0, 0)), (0.24, V(0, 30, 0)), (0.36, V(0, -20, 0)), (L, V())]},
    "tail_tip2": {R: [(0, V()), (0.16, V(-10, 0, 0)), (0.26, V(0, 35, 0)), (0.38, V(0, -25, 0)), (L, V())]},
})

# ---------------- chicotada de cauda (1,0 s; o golpe pega ~0,5 s)
L2 = 1.0
whip = anim("tail_whip", L2, {
    # o corpo todo gira (raiz): prepara para um lado e gira forte para o outro, a cauda passa pela frente
    "root":   {R: [(0, V()), (0.25, V(0, -35, 0)), (0.42, V(0, 120, 0)), (0.52, V(0, 165, 0)), (0.7, V(0, 150, 0)), (L2, V())]},
    "body":   {R: [(0, V()), (0.25, V(6, 0, -6)), (0.45, V(-4, 0, 10)), (0.6, V(0, 0, 6)), (L2, V())],
               Pn: [(0, V()), (0.25, V(0, -0.4, 0)), (0.45, V(0, 0.3, 0)), (L2, V())]},
    "head":   {R: [(0, V()), (0.25, V(10, -20, 0)), (0.45, V(-5, 35, 0)), (0.7, V(0, 20, 0)), (L2, V())]},
    "jaw":    {R: [(0, V()), (0.25, V(-30, 0, 0)), (0.5, V(-20, 0, 0)), (0.7, V()), (L2, V())]},
    "ear_l":  {R: [(0, V()), (0.2, V(-40, 0, 0)), (0.7, V(-30, 0, 0)), (L2, V())]},
    "ear_r":  {R: [(0, V()), (0.2, V(-40, 0, 0)), (0.7, V(-30, 0, 0)), (L2, V())]},
    "leg_fl": {R: [(0, V()), (0.25, V(-20, 0, -10)), (0.5, V(20, 0, 15)), (L2, V())]},
    "leg_fr": {R: [(0, V()), (0.25, V(-20, 0, -10)), (0.5, V(20, 0, 15)), (L2, V())]},
    "leg_bl": {R: [(0, V()), (0.25, V(20, 0, 10)), (0.5, V(-15, 0, -15)), (L2, V())]},
    "leg_br": {R: [(0, V()), (0.25, V(20, 0, 10)), (0.5, V(-15, 0, -15)), (L2, V())]},
    # cauda: levanta, vai para tras do giro e chicoteia; cada ponta atrasa um pouco (efeito chicote)
    "tail":      {R: [(0, V()), (0.25, V(-35, 45, 0)), (0.42, V(-25, -40, 0)), (0.52, V(-15, -70, 0)), (0.7, V(-5, -20, 0)), (L2, V())]},
    "tail_tip":  {R: [(0, V()), (0.28, V(-10, 40, 0)), (0.46, V(-5, -45, 0)), (0.56, V(0, -60, 0)), (0.74, V(0, -10, 0)), (L2, V())]},
    "tail_tip2": {R: [(0, V()), (0.31, V(-5, 45, 0)), (0.5, V(0, -50, 0)), (0.6, V(0, -55, 0)), (0.78, V(0, 10, 0)), (L2, V())]},
    "tail_tip3": {R: [(0, V()), (0.34, V(0, 40, 0)), (0.53, V(0, -45, 0)), (0.63, V(0, -40, 0)), (0.82, V(0, 15, 0)), (L2, V())]},
})

m["animations"] = [a for a in m["animations"] if a["name"] not in ("animation.rat.attack", "animation.rat.tail_whip")] + [attack, whip]
json.dump(m, open(P, "w"))
print("ok:", [a["name"] for a in m["animations"]])

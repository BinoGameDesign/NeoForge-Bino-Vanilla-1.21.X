# Gera eat_rig.bbmodel: rig de PRIMEIRA PESSOA para comer (Blockbench, Modded Entity).
# A origem (0,0,0) e o OLHO do jogador; a frente e -z; a direita e +x. Unidade = pixel (16 = 1 bloco).
# Ossos: root > camera (giro da camera: X- olha pra baixo, Z inclina)
#             > right_arm (braco direito real do jogador; o cubo e so para ver no Blockbench)
#                 > food (onde a comida 3D e desenhada; ESCALA da comida = mordidas)
#             > left_arm
# Os cubos NAO aparecem no jogo: o jogo desenha seus bracos (skin + manga da armadura) e a comida nesses ossos.
# Convencao do Blockbench: X+ leva o braco pendurado pra frente/cima; Y+ gira a frente para -x (braco direito vai pro centro).
import json, uuid, base64, io, os
from PIL import Image
OUT = os.path.dirname(os.path.abspath(__file__)); U = lambda: str(uuid.uuid4())
BONES = {"root": (None, (0, 0, 0), (0, 0, 0)), "camera": ("root", (0, 0, 0), (0, 0, 0)),
         "right_arm": ("root", (7, -6, 2), (85, 12, 0)), "food": ("right_arm", (7, -20.5, 1), (0, 0, 0)),
         "left_arm": ("root", (-7, -7, 2), (82, -12, 0))}
CUBES = [("right_arm", "braco_d", (5, -18, 0), (9, -6, 4)), ("left_arm", "braco_e", (-9, -19, 0), (-5, -7, 4)),
         ("food", "comida", (5, -22, -1.5), (9, -19, 3.5)), ("camera", "olho", (-0.5, -0.5, -0.5), (0.5, 0.5, 0.5))]
W = H = 64
img = Image.new("RGBA", (W, H))
for y in range(H):
    for x in range(W): img.putpixel((x, y), (200, 160, 120, 255) if y < 32 else (240, 200, 90, 255))
b = io.BytesIO(); img.save(b, "PNG"); TEX = "data:image/png;base64," + base64.b64encode(b.getvalue()).decode()
uvs = {"braco_d": (0, 0), "braco_e": (16, 0), "comida": (0, 32), "olho": (32, 32)}
def faces(u, v, w, h, d):
    return {"east": [u, v + d, u + d, v + d + h], "north": [u + d, v + d, u + d + w, v + d + h], "west": [u + d + w, v + d, u + 2 * d + w, v + d + h],
            "south": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], "up": [u + d + w, v + d, u + d, v], "down": [u + d + 2 * w, v, u + d + w, v + d]}
bu = {b: U() for b in BONES}; kids = {b: [] for b in BONES}; els = []
for bone, name, fr, to in CUBES:
    w, h, d = (round(to[i] - fr[i]) for i in range(3)); u, v = uvs[name]
    e = {"name": name, "box_uv": True, "render_order": "default", "rescale": False, "locked": False, "allow_mirror_modeling": True, "from": list(fr), "to": list(to),
         "autouv": 0, "color": len(els), "origin": list(BONES[bone][1]), "uv_offset": [u, v], "faces": {k: {"uv": f, "texture": 0} for k, f in faces(u, v, w, h, d).items()},
         "type": "cube", "uuid": U()}
    els.append(e); kids[bone].append(e["uuid"])
for b, (p, _, _) in BONES.items():
    if p: kids[p].append(b)
groups = [{"name": b, "origin": list(o), "rotation": list(r), "color": 0, "uuid": bu[b], "export": True, "mirror_uv": False, "isOpen": True, "locked": False,
           "visibility": True, "autouv": 0, "children": []} for b, (_, o, r) in BONES.items()]
tree = lambda b: {"uuid": bu[b], "isOpen": True, "children": [tree(c) if c in BONES else c for c in kids[b]]}
V = lambda x=0, y=0, z=0: [x, y, z]
def anim(name, length, tracks):
    an = {}
    for bone, chans in tracks.items():
        kf = []
        for ch, pts in chans.items():
            for p in pts:
                kf.append({"channel": ch, "data_points": [{"x": p[1][0], "y": p[1][1], "z": p[1][2]}], "uuid": U(), "time": p[0], "color": -1,
                           "interpolation": p[2] if len(p) > 2 else "catmullrom"})
        an[bu[bone]] = {"name": bone, "type": "bone", "keyframes": kf}
    return {"uuid": U(), "name": "animation.eat_rig." + name, "loop": "once", "override": False, "length": length, "snapping": 24, "selected": False,
            "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": an}
def bites(times, push, base):     # o braco empurra a comida ate a boca em cada mordida
    pts = []
    for t in times: pts += [(t - 0.12, base), (t, push), (t + 0.15, base)]
    return pts
R, S = "rotation", "scale"
A = []
# queijo: leva a boca com as duas maos, 3 mordidas, a fatia vai diminuindo, abaixa
UPR, UPL = V(18, 30, 0), V(14, -26, 0)
A.append(anim("eat_cheese", 2.0, {
    "right_arm": {R: [(0, V()), (0.3, UPR)] + bites((0.5, 1.0, 1.5), V(24, 32, -4), UPR) + [(1.75, UPR), (2.0, V())]},
    "left_arm":  {R: [(0, V()), (0.32, UPL)] + bites((0.5, 1.0, 1.5), V(19, -28, 4), UPL) + [(1.75, UPL), (2.0, V())]},
    "food":      {S: [(0, V(1, 1, 1), "linear"), (0.55, V(1, 1, 1), "linear"), (0.6, V(0.78, 0.85, 0.78), "linear"), (1.05, V(0.78, 0.85, 0.78), "linear"),
                      (1.1, V(0.55, 0.7, 0.55), "linear"), (1.55, V(0.55, 0.7, 0.55), "linear"), (1.6, V(0.3, 0.5, 0.3), "linear"), (2.0, V(0.3, 0.5, 0.3), "linear")],
                  R: [(0, V()), (0.3, V(0, -20, 10)), (1.75, V(0, -20, 10)), (2.0, V())]},
    "camera":    {R: [(0, V())] + [p for t in (0.5, 1.0, 1.5) for p in ((t - 0.05, V()), (t + 0.05, V(-3, 0, 1.5)), (t + 0.25, V(-1, 0, -1)), (t + 0.4, V()))] + [(2.0, V())]},
}))
# carne: so a mao direita; morde e PUXA (rasga) girando, 3 vezes
UPM = V(20, 34, 0)
A.append(anim("eat_meat", 2.4, {
    "right_arm": {R: [(0, V()), (0.35, UPM)] + [p for t in (0.6, 1.2, 1.8) for p in ((t - 0.12, UPM), (t, V(27, 36, 0)), (t + 0.12, V(16, 30, -18)), (t + 0.3, UPM))] + [(2.1, UPM), (2.4, V())]},
    "left_arm":  {R: [(0, V()), (0.4, V(-8, 0, 0)), (2.1, V(-8, 0, 0)), (2.4, V())]},
    "food":      {S: [(0, V(1, 1, 1), "linear"), (0.66, V(1, 1, 1), "linear"), (0.72, V(0.8, 0.85, 0.8), "linear"), (1.26, V(0.8, 0.85, 0.8), "linear"),
                      (1.32, V(0.58, 0.7, 0.58), "linear"), (1.86, V(0.58, 0.7, 0.58), "linear"), (1.92, V(0.35, 0.5, 0.35), "linear"), (2.4, V(0.35, 0.5, 0.35), "linear")],
                  R: [(0, V()), (0.35, V(0, 30, -15)), (2.1, V(0, 30, -15)), (2.4, V())]},
    "camera":    {R: [(0, V())] + [p for t in (0.6, 1.2, 1.8) for p in ((t - 0.05, V()), (t + 0.06, V(-4, 2, 0)), (t + 0.2, V(1, -3, -2)), (t + 0.45, V()))] + [(2.4, V())]},
}))
m = {"meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True}, "name": "eat_rig", "model_identifier": "eat_rig", "modded_entity_version": "1.17",
     "modded_entity_flip_y": True, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
     "resolution": {"width": W, "height": H}, "elements": els, "groups": groups, "outliner": [tree("root")],
     "textures": [{"path": "", "name": "eat_rig.png", "folder": "", "namespace": "", "id": "0", "width": W, "height": H, "uv_width": W, "uv_height": H, "particle": False,
                   "render_mode": "default", "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
                   "visible": True, "internal": True, "saved": False, "uuid": U(), "source": TEX}], "animations": A}
json.dump(m, open(os.path.join(OUT, "eat_rig.bbmodel"), "w"))
print("ok")

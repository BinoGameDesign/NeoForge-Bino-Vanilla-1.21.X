# Monta o conjunto de baus a partir do modelo do Bino (tools/blockbench/chests/src/chest_bino.bbmodel):
#  - reorganiza o UV (cada cubo com o seu espaco, sem sobrepor) e PINTA a textura no estilo do jogo, por material
#    (tabuas de madeira, aro, moldura/dobradicas/alcas de metal, fechadura com buraco);
#  - versoes: normal, armadilha, natal, ender (simples) + duplo (normal, armadilha, natal) + mimico;
#  - poe um osso "root" em volta de tudo e cria as animacoes (abrir/fechar; no mimico: respirar, espiada,
#    revelar, morder, pular).
# Frente (fechadura) = SUL (+Z). Rodar da raiz do repo: python3 tools/blockbench/gen_chest_set.py
import json, uuid, os, base64, io, math, random, copy
from PIL import Image

D = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(D, "chests")
SRC = os.path.join(OUT, "src", "chest_bino.bbmodel")
U = lambda: str(uuid.uuid4())

# ------------------------------------------------------------------ paletas
def pal(wood, wood_light, wood_dark, inside, metal, metal_hl, metal_sh, hole, accent=None, speck=None, ribbon=None):
    return dict(wood=wood, wood_light=wood_light, wood_dark=wood_dark, inside=inside, metal=metal, metal_hl=metal_hl,
                metal_sh=metal_sh, hole=hole, accent=accent, speck=speck, ribbon=ribbon)
PAL = {
    "normal":    pal((150, 98, 52), (172, 117, 64), (94, 59, 30), (70, 44, 22), (214, 168, 58), (247, 216, 122), (148, 106, 30), (38, 24, 10)),
    "trapped":   pal((140, 88, 50), (160, 104, 60), (86, 52, 28), (64, 40, 22), (168, 168, 174), (214, 214, 220), (98, 98, 108), (30, 26, 26), accent=(178, 36, 36)),
    "christmas": pal((168, 40, 40), (198, 62, 56), (108, 22, 24), (80, 20, 22), (220, 176, 60), (250, 222, 130), (150, 110, 30), (40, 20, 10), ribbon=(40, 128, 62)),
    "ender":     pal((30, 22, 40), (46, 34, 60), (16, 11, 22), (12, 8, 18), (26, 66, 60), (66, 146, 126), (10, 32, 30), (6, 4, 10), accent=(70, 190, 130), speck=(104, 70, 150)),
}
TEETH = ((236, 231, 214), (196, 186, 162))
TONGUE = ((192, 62, 82), (232, 112, 132), (132, 32, 52))

# ------------------------------------------------------------------ utilidades do modelo
def size(e): return [e["to"][i] - e["from"][i] for i in range(3)]

def role(e):
    n = e["name"]; w, h, d = size(e); inf = e.get("inflate", 0) or 0
    if n == "bottom": return "body"
    if n == "lid":
        if h <= 1.2: return "rim"
        if inf < 0: return "top"
        return "body"
    if n.startswith("lock"): return "lock"
    if n.startswith("hinge"): return "hinge"
    if n.startswith("handle"): return "metal"
    if n.startswith("tooth"): return "tooth"
    if n.startswith("tongue"): return "tongue"
    return "metal"   # moldura (cubos "cube")

def face_rects(u, v, w, h, d):
    return {"east": (u, v + d, d, h), "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h), "up": (u + d, v, w, d), "down": (u + d + w, v, w, d)}

def faces_json(u, v, w, h, d, mirror=False):
    f = {"north": [u+d, v+d, u+d+w, v+d+h], "east": [u, v+d, u+d, v+d+h], "south": [u+2*d+w, v+d, u+2*d+2*w, v+d+h],
         "west": [u+d+w, v+d, u+2*d+w, v+d+h], "up": [u+d+w, v+d, u+d, v], "down": [u+2*w+d, v, u+d+w, v+d]}
    if mirror:   # mesmo jeito que o Blockbench espelha (troca leste/oeste e inverte u)
        f = {"north": [f["north"][2], f["north"][1], f["north"][0], f["north"][3]],
             "south": [f["south"][2], f["south"][1], f["south"][0], f["south"][3]],
             "east": [f["west"][2], f["west"][1], f["west"][0], f["west"][3]],
             "west": [f["east"][2], f["east"][1], f["east"][0], f["east"][3]],
             "up": [f["up"][2], f["up"][1], f["up"][0], f["up"][3]], "down": [f["down"][2], f["down"][1], f["down"][0], f["down"][3]]}
    return {k: {"uv": [round(x, 4) for x in val], "texture": 0} for k, val in f.items()}

def pack(elements, W):
    """UV novo: um espaco por cubo (cubos espelhados com o mesmo tamanho dividem). Prateleiras de cima para baixo."""
    keyed = {}
    for e in elements:
        w, h, d = size(e)
        key = (e["name"].replace("_l", "_X").replace("_r", "_X"), round(w, 3), round(h, 3), round(d, 3)) if e["name"].startswith("handle") else e["uuid"]
        keyed.setdefault(key, []).append(e)
    items = sorted(keyed.values(), key=lambda es: -(size(es[0])[2] + size(es[0])[1]))
    x = y = shelf = 0
    for es in items:
        w, h, d = size(es[0])
        fw, fh = math.ceil(2 * (w + d)), math.ceil(d + h)
        if x + fw > W: x, y, shelf = 0, y + shelf, 0
        for e in es:
            e["uv_offset"] = [x, y]
            e["faces"] = faces_json(x, y, w, h, d, mirror=bool(e.get("mirror_uv")))
        x += fw; shelf = max(shelf, fh)
    return y + shelf

# ------------------------------------------------------------------ pintura
def shade(c, k): return tuple(max(0, min(255, round(v * k))) for v in c)
def jitter(c, r, amt): return tuple(max(0, min(255, v + r.randint(-amt, amt))) for v in c)

def paint_wood(img, rect, P, r, face, base_key="wood", ribbon=False):
    u, v, w, h = rect; u0, v0 = int(u), int(v); W, H = max(1, math.ceil(w)), max(1, math.ceil(h))
    base = P[base_key]
    board = 4 if face in ("up", "down") else 3
    tones = [shade(base, k) for k in (1.0, 0.93, 1.06, 0.97)]
    for j in range(H):
        bi = j // board; seam = (j % board == board - 1) and j < H - 1
        for i in range(W):
            c = tones[(bi + (i // 9 if face in ("up", "down") else 0)) % 4]
            c = jitter(c, r, 6)
            if seam: c = shade(base, 0.62)
            if r.random() < 0.05: c = shade(c, 0.85)        # veio da madeira
            if P["speck"] and r.random() < 0.06: c = P["speck"]
            if i == 0 or i == W - 1 or j == 0 or j == H - 1: c = shade(c, 0.78)   # borda escura
            if ribbon and P["ribbon"] and abs(i - (W - 1) / 2) <= 1.0: c = jitter(P["ribbon"], r, 6)
            img.putpixel((u0 + i, v0 + j), c + (255,))

def paint_flat(img, rect, c, r, amt=5, edge=0.8):
    u, v, w, h = rect; u0, v0 = int(u), int(v); W, H = max(1, math.ceil(w)), max(1, math.ceil(h))
    for j in range(H):
        for i in range(W):
            cc = jitter(c, r, amt)
            if i == 0 or i == W - 1 or j == 0 or j == H - 1: cc = shade(cc, edge)
            img.putpixel((u0 + i, v0 + j), cc + (255,))

def paint_metal(img, rect, P, r, rivets=True, dark=False):
    u, v, w, h = rect; u0, v0 = int(u), int(v); W, H = max(1, math.ceil(w)), max(1, math.ceil(h))
    base = shade(P["metal"], 0.85) if dark else P["metal"]
    for j in range(H):
        for i in range(W):
            c = jitter(base, r, 5)
            if j == 0 or i == 0: c = P["metal_hl"]
            if j == H - 1 or i == W - 1: c = P["metal_sh"]
            img.putpixel((u0 + i, v0 + j), c + (255,))
    if rivets and max(W, H) >= 5:
        for (i, j) in ((1, 1), (W - 2, H - 2)) if W >= H else ((1, 1), (W - 2, H - 2)):
            if 0 < i < W - 1 and 0 < j < H - 1: img.putpixel((u0 + i, v0 + j), P["metal_hl"] + (255,))

def paint_lock(img, rect, P, r, face):
    paint_metal(img, rect, P, r, rivets=False)
    if face != "south": return
    u, v, w, h = rect; u0, v0 = int(u), int(v); W, H = max(1, math.ceil(w)), max(1, math.ceil(h))
    cx = (W - 1) // 2
    if P["accent"] and P is PAL["ender"]:   # olho de ender
        for j in range(H):
            for i in range(W):
                if 0 < i < W - 1 and 0 < j < H - 1: img.putpixel((u0 + i, v0 + j), P["accent"] + (255,))
        img.putpixel((u0 + cx, v0 + H // 2), P["hole"] + (255,))
        return
    for j in range(H // 2, min(H, H // 2 + 2)):      # buraco da chave
        img.putpixel((u0 + cx, v0 + j), P["hole"] + (255,))
    if P["accent"]: img.putpixel((u0 + cx, v0 + max(0, H // 2 - 1)), P["accent"] + (255,))   # gancho vermelho (armadilha)

def paint(elements, P, tw, th, seed=7):
    r = random.Random(seed)
    img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    done = set()
    for e in elements:
        key = tuple(e["uv_offset"])
        if key in done: continue
        done.add(key)
        w, h, d = size(e); u, v = e["uv_offset"]; rl = role(e)
        for face, rect in face_rects(u, v, w, h, d).items():
            if rect[2] <= 0 or rect[3] <= 0: continue
            if rl == "body":
                if face == "up" and e["name"] == "bottom": paint_flat(img, rect, P["inside"], r, 4, 0.7)      # fundo por dentro
                elif face == "down" and e["name"] == "lid": paint_flat(img, rect, shade(P["inside"], 1.15), r, 4, 0.7)
                else: paint_wood(img, rect, P, r, face, ribbon=(P["ribbon"] is not None and face in ("north", "south", "up")))
            elif rl == "top": paint_wood(img, rect, P, r, face, "wood_light", ribbon=P["ribbon"] is not None and face == "up")
            elif rl == "rim": paint_flat(img, rect, P["wood_dark"], r, 4, 0.85)
            elif rl == "lock": paint_lock(img, rect, P, r, face)
            elif rl == "hinge": paint_metal(img, rect, P, r, rivets=False, dark=True)
            elif rl == "tooth":
                paint_flat(img, rect, TEETH[0], r, 3, 1.0)
                uu, vv, ww, hh = rect
                if face not in ("up", "down"):
                    for i in range(max(1, math.ceil(ww))): img.putpixel((int(uu) + i, int(vv) + max(0, math.ceil(hh) - 1)), TEETH[1] + (255,))
            elif rl == "tongue":
                paint_flat(img, rect, TONGUE[0], r, 6, 0.75)
                uu, vv, ww, hh = rect
                if face == "up":
                    cx = int(uu) + max(0, math.ceil(ww) // 2)
                    for j in range(max(1, math.ceil(hh))): img.putpixel((cx, int(vv) + j), TONGUE[2] + (255,))
            else: paint_metal(img, rect, P, r)
    return img

# ------------------------------------------------------------------ grupos, animacoes
def new_group(name, origin):
    return {"name": name, "uuid": U(), "export": True, "locked": False, "scope": 0, "selected": False, "visibility": True,
            "origin": list(origin), "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False, "shade": True,
            "mirror_uv": False, "autouv": 0, "isOpen": True}

def wrap_root(m):
    root = new_group("root", [0, 0, 0])
    m["groups"].insert(0, root)
    m["outliner"] = [{"uuid": root["uuid"], "isOpen": True, "children": m["outliner"]}]

def find_node(nodes, uuid_):
    for n in nodes:
        if isinstance(n, dict):
            if n["uuid"] == uuid_: return n
            f = find_node(n["children"], uuid_)
            if f: return f
    return None

R, Pn, S = "rotation", "position", "scale"
V = lambda x=0, y=0, z=0: [x, y, z]
def anim(m, name, length, loop, tracks):
    grp = {g["name"]: g for g in m["groups"]}
    animators = {}
    for bone, chans in tracks.items():
        if bone not in grp: continue
        kfs = []
        for ch, pts in chans.items():
            for p in sorted(pts, key=lambda q: q[0]):
                kfs.append({"channel": ch, "data_points": [{"x": round(p[1][0], 3), "y": round(p[1][1], 3), "z": round(p[1][2], 3)}], "uuid": U(),
                            "time": round(p[0], 4), "color": -1, "interpolation": p[2] if len(p) > 2 else "catmullrom"})
        animators[grp[bone]["uuid"]] = {"name": bone, "type": "bone", "keyframes": kfs}
    return {"uuid": U(), "name": "animation.chest." + name, "loop": "loop" if loop else "once", "override": False, "length": length,
            "snapping": 24, "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators}

OPEN = -92   # tampa aberta (graus; negativo = a frente sobe)
def chest_anims(m):
    return [
        anim(m, "open", 0.5, False, {"lid": {R: [(0, V()), (0.08, V(2, 0, 0)), (0.32, V(OPEN - 10, 0, 0)), (0.5, V(OPEN, 0, 0))]}}),
        anim(m, "close", 0.45, False, {"lid": {R: [(0, V(OPEN, 0, 0)), (0.22, V(0, 0, 0), "linear"), (0.3, V(-5, 0, 0)), (0.38, V(0, 0, 0)), (0.45, V())]},
                                       "root": {Pn: [(0, V()), (0.22, V()), (0.25, V(0, -0.3, 0)), (0.32, V())]}}),
    ]

def mimic_anims(m):
    hid, shown = V(0.01, 0.01, 0.01), V(1, 1, 1)
    teeth_in = lambda t0, t1: {S: [(0, hid, "linear"), (t0, hid, "linear"), (t1, shown), (99, shown)]}
    A = chest_anims(m)
    A += [
        # respira de leve (pista para o jogador atento)
        anim(m, "breath", 3.0, True, {"lid": {R: [(0, V()), (1.5, V(-1.6, 0, 0)), (3.0, V())]},
                                       "root": {S: [(0, V(1, 1, 1)), (1.5, V(1.01, 1.02, 1.01)), (3.0, V(1, 1, 1))]}}),
        # espiada: a tampa da um tranco, aparece a pontinha dos dentes e a lingua lambe rapido
        anim(m, "tell", 0.8, False, {
            "lid": {R: [(0, V()), (0.15, V(-7, 0, 0)), (0.45, V(-5, 0, 0)), (0.6, V(0, 0, 0), "linear"), (0.8, V())]},
            "teeth_top": {S: [(0, hid, "linear"), (0.12, shown), (0.55, shown), (0.6, hid, "linear"), (0.8, hid, "linear")]},
            "tongue": {S: [(0, hid, "linear"), (0.15, shown), (0.5, shown), (0.58, hid, "linear"), (0.8, hid, "linear")],
                       R: [(0, V()), (0.25, V(-15, 0, 0)), (0.45, V(5, 0, 0)), (0.8, V())]},
            "tongue_tip": {R: [(0, V()), (0.25, V(-25, 0, 0)), (0.4, V(20, 0, 0)), (0.8, V())]}}),
        # revelar (com a tampa aberta, no menu): dentes crescem, lingua sai e se contorce
        anim(m, "reveal", 0.9, False, {
            "lid": {R: [(0, V(OPEN, 0, 0)), (0.1, V(OPEN - 6, 0, 2)), (0.2, V(OPEN + 4, 0, -2)), (0.3, V(OPEN - 4, 0, 2)), (0.9, V(OPEN - 8, 0, 0))]},
            "teeth_top": teeth_in(0.05, 0.3), "teeth_bottom": teeth_in(0.05, 0.3),
            "tongue": {S: [(0, hid, "linear"), (0.15, hid, "linear"), (0.4, shown), (0.9, shown)],
                       R: [(0, V()), (0.4, V(-35, 0, 0)), (0.6, V(-20, 10, 0)), (0.75, V(-30, -10, 0)), (0.9, V(-25, 0, 0))]},
            "tongue_mid": {R: [(0, V()), (0.45, V(25, 0, 0)), (0.65, V(10, 0, 10)), (0.8, V(20, 0, -10)), (0.9, V(15, 0, 0))]},
            "tongue_tip": {R: [(0, V()), (0.5, V(35, 0, 0)), (0.7, V(10, 15, 0)), (0.85, V(30, -15, 0)), (0.9, V(25, 0, 0))]},
            "root": {Pn: [(0, V()), (0.1, V(0.3, 0, 0)), (0.2, V(-0.3, 0, 0)), (0.3, V(0, 0, 0))]}}),
        # mordida: a tampa fecha com tudo
        anim(m, "bite", 0.4, False, {
            "lid": {R: [(0, V(OPEN - 8, 0, 0)), (0.06, V(OPEN - 18, 0, 0)), (0.14, V(4, 0, 0), "linear"), (0.2, V(-6, 0, 0)), (0.28, V(0, 0, 0)), (0.4, V())]},
            "teeth_top": {S: [(0, shown), (0.4, shown)]}, "teeth_bottom": {S: [(0, shown), (0.4, shown)]},
            "tongue": {S: [(0, shown), (0.1, hid, "linear"), (0.4, hid, "linear")]},
            "root": {Pn: [(0, V()), (0.14, V(0, -0.6, 0)), (0.2, V(0, 0.4, 0)), (0.3, V()), (0.4, V())],
                     R: [(0, V()), (0.14, V(4, 0, 0)), (0.3, V()), (0.4, V())]}}),
        # pulo (andar/atacar como criatura): sobe, abre a boca no ar, fecha ao cair
        anim(m, "hop", 0.7, True, {
            "root": {Pn: [(0, V()), (0.1, V(0, -0.8, 0)), (0.35, V(0, 5, 0)), (0.6, V(0, 0, 0), "linear"), (0.65, V(0, -0.6, 0)), (0.7, V())],
                     R: [(0, V()), (0.1, V(4, 0, 0)), (0.35, V(-8, 0, 0)), (0.6, V(2, 0, 0)), (0.7, V())],
                     S: [(0, V(1, 1, 1)), (0.1, V(1.06, 0.92, 1.06)), (0.3, V(0.96, 1.06, 0.96)), (0.62, V(1.08, 0.9, 1.08)), (0.7, V(1, 1, 1))]},
            "lid": {R: [(0, V()), (0.2, V(-15, 0, 0)), (0.35, V(-45, 0, 0)), (0.58, V(0, 0, 0), "linear"), (0.7, V())]},
            "teeth_top": {S: [(0, shown), (0.7, shown)]}, "teeth_bottom": {S: [(0, shown), (0.7, shown)]},
            "tongue": {S: [(0, shown), (0.7, shown)], R: [(0, V()), (0.35, V(-30, 0, 0)), (0.6, V()), (0.7, V())]},
            "tongue_tip": {R: [(0, V()), (0.35, V(30, 0, 0)), (0.7, V())]}}),
    ]
    return A

# ------------------------------------------------------------------ mimico: dentes e lingua
def tooth(name, frm, to):
    return {"name": name, "box_uv": True, "render_order": "default", "locked": False, "export": True, "scope": 0,
            "allow_mirror_modeling": True, "from": list(frm), "to": list(to), "autouv": 0, "color": 1, "origin": [0, 0, 0],
            "inflate": -0.1, "uv_offset": [0, 0], "type": "cube", "uuid": U()}

def add_mimic_parts(m):
    grp = {g["name"]: g for g in m["groups"]}
    lid_node = find_node(m["outliner"], grp["lid"]["uuid"]); bot_node = find_node(m["outliner"], grp["bottom"]["uuid"])
    gt = new_group("teeth_top", [0, 7.9, -7]); gb = new_group("teeth_bottom", [0, 8, 0])
    g1 = new_group("tongue", [0, 8.4, -4]); g2 = new_group("tongue_mid", [0, 8.4, 1]); g3 = new_group("tongue_tip", [0, 8.4, 4.5])
    m["groups"] += [gt, gb, g1, g2, g3]
    els = []
    # dentes de cima: penduram da tampa (frente e laterais); fechado, ficam escondidos dentro do fundo
    top = [tooth("tooth_top", (x, 6.3, 5.4), (x + 1.2, 7.95, 6.6)) for x in (-6, -4, -2, 0.6, 2.6, 4.6)]
    top += [tooth("tooth_top", (s, 6.5, z), (s + 1.1, 7.95, z + 1.2)) for s in (-6.4, 5.3) for z in (-4, -1, 2)]
    # dentes de baixo: sobem da borda do fundo (escondidos dentro da tampa quando fechado)
    bot = [tooth("tooth_bottom", (x, 7.95, 5.6), (x + 1.2, 9.5, 6.8)) for x in (-5, -3, -1, 1, 3.6)]
    bot += [tooth("tooth_bottom", (s, 7.95, z), (s + 1.1, 9.3, z + 1.2)) for s in (-6.6, 5.5) for z in (-3, 0, 3)]
    tongue = [dict(tooth("tongue", (-2, 7.9, -4), (2, 8.9, 1)), inflate=0, color=2),
              dict(tooth("tongue_mid", (-1.7, 7.9, 1), (1.7, 8.9, 4.5)), inflate=-0.05, color=2),
              dict(tooth("tongue_tip", (-1.4, 7.9, 4.5), (1.4, 8.9, 7.8)), inflate=-0.1, color=2)]
    els = top + bot + tongue
    m["elements"] += els
    lid_node["children"].append({"uuid": gt["uuid"], "isOpen": True, "children": [e["uuid"] for e in top]})
    bot_node["children"].append({"uuid": gb["uuid"], "isOpen": True, "children": [e["uuid"] for e in bot]})
    bot_node["children"].append({"uuid": g1["uuid"], "isOpen": True, "children": [tongue[0]["uuid"],
        {"uuid": g2["uuid"], "isOpen": True, "children": [tongue[1]["uuid"],
            {"uuid": g3["uuid"], "isOpen": True, "children": [tongue[2]["uuid"]]}]}]})

# ------------------------------------------------------------------ bau duplo (2 blocos, centrado na emenda)
def make_double(m):
    for e in m["elements"]:
        x0, x1 = e["from"][0], e["to"][0]; c = (x0 + x1) / 2; w = x1 - x0
        if w >= 10: e["from"][0] -= 8; e["to"][0] += 8                  # atravessa o bau: estica
        elif c > 0.5: e["from"][0] += 8; e["to"][0] += 8; e["origin"][0] += 8 if e.get("origin") and abs(e["origin"][0]) > 1 else 0
        elif c < -0.5: e["from"][0] -= 8; e["to"][0] -= 8; e["origin"][0] -= 8 if e.get("origin") and abs(e["origin"][0]) > 1 else 0
    m["visible_box"] = [2, 1, 0.5]

# ------------------------------------------------------------------ montar
def build(name, variant, double=False, mimic=False):
    m = json.load(open(SRC))
    m["name"] = name; m["model_identifier"] = name
    for e in m["elements"]:
        e.setdefault("uv_offset", [0, 0]); e.setdefault("origin", [0, 0, 0])
    if double: make_double(m)
    if mimic: add_mimic_parts(m)
    wrap_root(m)
    W = 128
    H = pack(m["elements"], W)
    TH = 128 if H <= 128 else 256
    m["resolution"] = {"width": W, "height": TH}
    tex = paint(m["elements"], PAL[variant], W, TH)
    buf = io.BytesIO(); tex.save(buf, "PNG")
    t = m["textures"][0]
    t.update({"name": name + ".png", "width": W, "height": TH, "uv_width": W, "uv_height": TH,
              "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()})
    m["textures"] = [t]
    m["animations"] = mimic_anims(m) if mimic else chest_anims(m)
    json.dump(m, open(os.path.join(OUT, name + ".bbmodel"), "w"))
    tex.save(os.path.join(OUT, name + ".png"))
    print("ok", name, f"{W}x{TH}", len(m["elements"]), "cubos")

JOBS = [("chest", "normal", False, False), ("trapped_chest", "trapped", False, False), ("christmas_chest", "christmas", False, False),
        ("ender_chest", "ender", False, False), ("chest_double", "normal", True, False), ("trapped_chest_double", "trapped", True, False),
        ("christmas_chest_double", "christmas", True, False), ("mimic_chest", "normal", False, True)]
for j in JOBS: build(*j)

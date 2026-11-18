# Gera modelos de BLOCO/ITEM (Blockbench 5, formato Java Block/Item) + texturas:
#   queijo inteiro em 5 estados (cheese_0..4 = 5..1 pedacos), pedaco de queijo 3D, ratoeira (armada/disparada), gaiola (aberta/fechada)
# Saida: tools/blockbench/blocks/*.bbmodel, *.png  e  os .json prontos para o jogo (models/block|item).
# Rodar: python3 tools/blockbench/gen_blocks_cheese_trap_cage.py
import json, uuid, base64, io, math, os, random
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(HERE, "blocks"); os.makedirs(OUT, exist_ok=True)
MOD = os.path.normpath(os.path.join(HERE, "..", "..", "src/main/resources/assets/binosvanillaplus"))
U = lambda: str(uuid.uuid4()); random.seed(5)

def png_b64(img): b = io.BytesIO(); img.save(b, "PNG"); return "data:image/png;base64," + base64.b64encode(b.getvalue()).decode()

def bbmodel(name, elements, tex_name, img, display=None, parent="block/block"):
    res = img.size[0]
    els = []
    for e in elements:
        faces = {k: ({"uv": e["faces"][k], "texture": 0} if k in e["faces"] else {"uv": [0, 0, 0, 0], "texture": None})
                 for k in ("north", "east", "south", "west", "up", "down")}
        d = {"name": e.get("name", "cube"), "box_uv": False, "render_order": "default", "rescale": False, "locked": False, "shade_direction_override": "",
             "light_emission": 0, "export": True, "scope": 0, "allow_mirror_modeling": True,
             "from": e["from"], "to": e["to"], "autouv": 0, "color": e.get("color", 0), "origin": e.get("origin", [8, 8, 8]),
             "faces": faces, "type": "cube", "uuid": U()}
        if e.get("rotation"): d["rotation"] = e["rotation"]
        els.append(d)
    m = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": parent,
         "java_block_version": "26.3", "multi_file_ruleset": "", "groups": [],
         "ambientocclusion": True, "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [],
         "unhandled_root_fields": {}, "resolution": {"width": res, "height": res}, "elements": els, "outliner": [e["uuid"] for e in els],
         "textures": [{"path": "", "name": tex_name + ".png", "folder": "block", "namespace": "", "id": "0", "width": res, "height": res,
                       "uv_width": res, "uv_height": res, "group": "", "scope": 0, "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
                       "file_format": "png", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "force_translucent": False, "particle": True, "render_mode": "default", "render_sides": "auto", "frame_time": 1,
                       "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": False,
                       "uuid": U(), "source": png_b64(img)}]}
    if display: m["display"] = display
    json.dump(m, open(os.path.join(OUT, name + ".bbmodel"), "w"))

def java_json(path, elements, tex, res, parent="block/block", display=None, particle=None):
    k = 16 / res
    j = ({"parent": parent} if parent else {}) | {"textures": {"0": tex, "particle": particle or tex}, "elements": []}
    for e in elements:
        el = {"from": e["from"], "to": e["to"], "faces": {f: {"uv": [round(c * k, 4) for c in uv], "texture": "#0"} for f, uv in e["faces"].items()}}
        if e.get("rotation"):
            ax = "xyz"[[i for i in range(3) if e["rotation"][i]][0]]; ang = [r for r in e["rotation"] if r][0]
            el["rotation"] = {"angle": ang, "axis": ax, "origin": e["origin"]}
        j["elements"].append(el)
    if display: j["display"] = display
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(j, open(path, "w"), indent=1)

# ============================================================ QUEIJO
Y, Wm, Om, D = (255, 228, 111), (237, 181, 62), (217, 158, 32), (98, 41, 28)        # amarelo claro, medio, casca, contorno
LIGHT, SPOT, HOLE, HOLE_D = (253, 235, 159), (153, 48, 23), (170, 99, 49), (132, 81, 51)
def tex_cheese():
    img = Image.new("RGBA", (32, 32)); P = img.load()
    def area(x0, y0, w, h, base, alt, holes=0, spots=0):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                P[x, y] = (alt if random.random() < 0.18 else base) + (255,)
        for _ in range(holes):
            x, y = x0 + random.randrange(w - 1), y0 + random.randrange(h - 1)
            P[x, y] = HOLE_D + (255,); P[x + 1, y] = HOLE + (255,)
        for _ in range(spots): P[x0 + random.randrange(w), y0 + random.randrange(h)] = SPOT + (255,)
    area(0, 0, 16, 16, Y, LIGHT, holes=7, spots=3)            # topo (casca de cima)
    area(16, 0, 16, 16, LIGHT, Y, holes=9)                     # miolo (face cortada)
    area(0, 16, 16, 6, Wm, Om, holes=3, spots=2)               # lateral (casca)
    for x in range(16): P[x, 16] = Om + (255,); P[x, 21] = D + (255,)
    area(16, 16, 16, 16, Om, Wm)                               # fundo
    for x in range(16, 32): P[x, 16 + 15] = D + (255,)
    return img
CHEESE_IMG = Image.open(os.path.join(OUT, "cheese.png")).convert("RGBA") if os.path.exists(os.path.join(OUT, "cheese.png")) else tex_cheese()   # textura do Bino
H = 6                              # altura em pixels; roda de 12 px (2..14) com cantos chanfrados
def wheel(x, z):                   # celula (x, z) dentro da roda?
    dx, dz = min(x - 2, 13 - x), min(z - 2, 13 - z)
    return 2 <= x <= 13 and 2 <= z <= 13 and dx + dz >= 2      # chanfro de 2 px nos cantos
# os 5 pedacos em quartos (como nos desenhos): Q = quadrante (x>=8 ?, z>=8 ?); o 5o pedaco e metade do ultimo quarto
QUAD = {"fd": lambda x, z: x >= 8 and z < 8, "fe": lambda x, z: x < 8 and z < 8, "td": lambda x, z: x >= 8 and z >= 8, "te": lambda x, z: x < 8 and z >= 8}
def present_cells(bites):
    removed = [["fd"], ["fd", "fe"], ["fd", "fe", "td"], ["fd", "fe", "td"], ["fd", "fe", "td"]][bites - 1] if bites else []
    cells = {(x, z) for x in range(16) for z in range(16) if wheel(x, z) and not any(QUAD[q](x, z) for q in removed)}
    if bites == 4: cells = {(x, z) for (x, z) in cells if x + (z - 8) <= 8}      # ultimo: so uma fatia do quarto que sobrou
    return cells
def rects(cells):                  # junta as celulas em poucos retangulos (sem sobrepor), cortando nas linhas x=8 e z=8
    left = set(cells); out = []
    for z in range(16):
        for x in range(16):
            if (x, z) not in left: continue
            x1 = x
            while (x1 + 1, z) in left and x1 + 1 != 8: x1 += 1
            z1 = z
            while all((xx, z1 + 1) in left for xx in range(x, x1 + 1)) and z1 + 1 != 8: z1 += 1
            for xx in range(x, x1 + 1):
                for zz in range(z, z1 + 1): left.discard((xx, zz))
            out.append((x, z, x1 + 1, z1 + 1))
    return out
TOP, INNER, RIND, BOT = (0, 0), (16, 0), (0, 16), (16, 16)
def cheese_model(cells):
    els = []
    full = {(x, z) for x in range(16) for z in range(16) if wheel(x, z)}
    for n, (x0, z0, x1, z1) in enumerate(rects(cells)):
        def kind(nbs):              # face para fora: casca se a vizinha esta fora da roda; miolo se e corte; None se encostada em queijo
            if all(nb in cells for nb in nbs): return None
            return "inner" if any(nb in full and nb not in cells for nb in nbs) else "rind"
        f = {"up": [x0, z0, x1, z1], "down": [16 + x0, 16 + z0, 16 + x1, 16 + z1]}
        sides = {"north": ([(x, z0 - 1) for x in range(x0, x1)], x0, x1), "south": ([(x, z1) for x in range(x0, x1)], x0, x1),
                 "west": ([(x0 - 1, z) for z in range(z0, z1)], z0, z1), "east": ([(x1, z) for z in range(z0, z1)], z0, z1)}
        for side, (nbs, a, b) in sides.items():
            k = kind(nbs)
            if k == "rind": f[side] = [a, 16, b, 16 + H]
            elif k == "inner": f[side] = [16 + a, 0, 16 + b, H]
        els.append({"name": f"queijo_{n}", "from": [x0, 0, z0], "to": [x1, H, z1], "faces": f, "color": n % 8})
    return els
DISPLAY_BLOCK = {"gui": {"rotation": [30, 225, 0], "scale": [0.8, 0.8, 0.8], "translation": [0, 2, 0]},
                 "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                 "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                 "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
                 "fixed": {"scale": [0.6, 0.6, 0.6]}}
for bites in range(1, 5):          # o cheese_0 e o modelo do Bino; daqui saem os outros 4 com a mesma textura e o mesmo jeito de UV
    els = cheese_model(present_cells(bites))
    bbmodel(f"cheese_{bites}", els, "cheese", CHEESE_IMG, DISPLAY_BLOCK)
    java_json(f"{MOD}/models/block/cheese_{bites}.json", els, "binosvanillaplus:block/cheese", 32, display=DISPLAY_BLOCK)
# pedaco: fatia triangular em degraus (ponta para -x, casca na parte larga em +x), centrado
slice_cells = set()
for i, x in enumerate(range(4, 13)):
    half = 1 + i * 0.5
    for z in range(16):
        if abs(z + 0.5 - 8) <= half: slice_cells.add((x, z))
def slice_model():
    els = []
    xs = sorted({x for x, _ in slice_cells})
    for n, x0 in enumerate(range(4, 13, 2)):        # blocos de 2 px de largura
        zs = [z for (x, z) in slice_cells if x == x0]
        z0, z1 = min(zs), max(zs) + 1
        x1 = min(x0 + 2, 13)
        f = {"up": [x0, z0, x1, z1], "down": [16 + x0, 16 + z0, 16 + x1, 16 + z1],
             "north": [16 + x0, 0, 16 + x1, 5], "south": [16 + x0, 0, 16 + x1, 5]}
        f["west"] = [16 + z0, 0, 16 + z1, 5]                            # degrau: a parte mais larga aparece
        if x1 == 13: f["east"] = [z0, 16, z1, 21]                       # casca na parte de tras
        els.append({"name": f"fatia_{n}", "from": [x0, 0, z0], "to": [x1, 5, z1], "faces": f, "color": n})
    return els
# (pedaco de queijo agora e o modelo do Bino: cheese_slice_3d.bbmodel)

# ============================================================ RATOEIRA: agora feita pelo Bino (rat_trap.bbmodel); so as cores usadas pela gaiola
WOOD, WOOD_D, WOOD_L = (176, 128, 78), (140, 98, 56), (196, 150, 98)
STEEL, STEEL_D, STEEL_L = (160, 166, 172), (104, 108, 116), (214, 218, 222)
COPPER, COPPER_D = (214, 122, 80), (160, 84, 56)
def box(name, fr, to, uvs, **kw):
    e = {"name": name, "from": list(fr), "to": list(to), "faces": {k: uvs for k in ("north", "south", "east", "west", "up", "down")}}
    e.update(kw); return e
DISPLAY_FLAT = {"gui": {"rotation": [30, 225, 0], "scale": [0.9, 0.9, 0.9], "translation": [0, 2, 0]}, "ground": {"scale": [0.5, 0.5, 0.5], "translation": [0, 3, 0]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 2], "scale": [0.5, 0.5, 0.5]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0], "scale": [0.6, 0.6, 0.6]}, "fixed": {"scale": [0.8, 0.8, 0.8]}}

# ============================================================ GAIOLA (refeita com cubos de verdade: barras de 0,5 px, todas as faces)
# porta tipo guilhotina na frente (-z): aberta = levantada acima do teto; fechada = embaixo
def tex_cage():
    img = Image.new("RGBA", (32, 32)); P = img.load()
    for y in range(16):
        for x in range(32): P[x, y] = (WOOD_D if (y % 4 == 0 or random.random() < 0.07) else WOOD_L if random.random() < 0.12 else WOOD) + (255,)   # madeira
    for y in range(16, 24):
        for x in range(32): P[x, y] = (STEEL_L if y == 16 else STEEL_D if y == 23 else STEEL) + (255,)                                             # ferro
    for y in range(24, 32):
        for x in range(16): P[x, y] = (COPPER_D if (x + y) % 5 == 0 else COPPER) + (255,)                                                         # prato de isca
        for x in range(16, 32): P[x, y] = (STEEL_D if (x + y) % 2 else STEEL) + (255,)                                                            # alca / trinco
    return img
CAGE_IMG = tex_cage()
W_ = lambda a, b, c, d: [a, b, c, d]
def cube(name, fr, to, area):     # uv proporcional ao tamanho, dentro da area (x0, y0) da textura
    x0, y0 = area
    sx, sy, sz = (max(0.5, to[i] - fr[i]) for i in range(3))
    def uv(w, h): return [x0, y0, x0 + min(16, w), y0 + min(8, h)]
    return {"name": name, "from": list(fr), "to": list(to), "faces": {
        "north": uv(sx, sy), "south": uv(sx, sy), "east": uv(sz, sy), "west": uv(sz, sy), "up": uv(sx, sz), "down": uv(sx, sz)}}
WOODA, STEELA, PLATEA, DARKA = (0, 0), (0, 16), (0, 24), (16, 24)
X0, X1, Z0, Z1, TOPY = 3, 13, 1, 15, 8
def cage(closed):
    els = [cube("base", (X0, 0, Z0), (X1, 1, Z1), WOODA), cube("prato", (6.5, 1, 11), (9.5, 1.25, 14), PLATEA)]
    # colunas dos cantos e moldura de cima
    for (x, z) in ((X0, Z0), (X1 - 0.5, Z0), (X0, Z1 - 0.5), (X1 - 0.5, Z1 - 0.5)):
        els.append(cube("coluna", (x, 1, z), (x + 0.5, TOPY, z + 0.5), STEELA))
    els += [cube("moldura_e", (X0, TOPY - 0.5, Z0), (X0 + 0.5, TOPY, Z1), STEELA), cube("moldura_d", (X1 - 0.5, TOPY - 0.5, Z0), (X1, TOPY, Z1), STEELA),
            cube("moldura_f", (X0, TOPY - 0.5, Z0), (X1, TOPY, Z0 + 0.5), STEELA), cube("moldura_t", (X0, TOPY - 0.5, Z1 - 0.5), (X1, TOPY, Z1), STEELA)]
    # barras dos lados (a cada 2 px), do fundo e do teto
    for z in range(Z0 + 2, Z1 - 1, 2):
        els.append(cube("barra_e", (X0, 1, z), (X0 + 0.5, TOPY - 0.5, z + 0.5), STEELA))
        els.append(cube("barra_d", (X1 - 0.5, 1, z), (X1, TOPY - 0.5, z + 0.5), STEELA))
        els.append(cube("teto", (X0 + 0.5, TOPY - 0.5, z), (X1 - 0.5, TOPY, z + 0.5), STEELA))
    for x in range(X0 + 2, X1 - 1, 2):
        els.append(cube("barra_fundo", (x, 1, Z1 - 0.5), (x + 0.5, TOPY - 0.5, Z1), STEELA))
    # alca em cima
    els += [cube("alca_e", (6.5, TOPY, 7.5), (7, TOPY + 1.5, 8.5), DARKA), cube("alca_d", (9, TOPY, 7.5), (9.5, TOPY + 1.5, 8.5), DARKA),
            cube("alca_topo", (6.5, TOPY + 1.5, 7.5), (9.5, TOPY + 2, 8.5), DARKA)]
    # porta (grade da frente) que desliza nos trilhos das colunas da frente
    dy = 0 if closed else TOPY - 1.5
    els.append(cube("porta_base", (X0 + 0.5, 1 + dy, Z0), (X1 - 0.5, 1.5 + dy, Z0 + 0.5), STEELA))
    els.append(cube("porta_topo", (X0 + 0.5, TOPY - 1 + dy, Z0), (X1 - 0.5, TOPY - 0.5 + dy, Z0 + 0.5), STEELA))
    for x in range(X0 + 2, X1 - 1, 2):
        els.append(cube("porta_barra", (x, 1.5 + dy, Z0), (x + 0.5, TOPY - 1 + dy, Z0 + 0.5), STEELA))
    for n, e in enumerate(els): e["name"] = f"{e['name']}_{n}"; e["color"] = n % 8
    return els
for st in (False, True):
    n = "cage" + ("_closed" if st else "")
    els = cage(st); bbmodel(n, els, "cage", CAGE_IMG, DISPLAY_FLAT)
    java_json(f"{MOD}/models/block/{n}.json", els, "binosvanillaplus:block/cage", 32, display=DISPLAY_FLAT)
CAGE_IMG.save(f"{MOD}/textures/block/cage.png"); CAGE_IMG.save(os.path.join(OUT, "cage.png"))

# ============================================================ icones 2D (ratoeira e gaiola) 16x16
def icon_trap():
    im = Image.new("RGBA", (16, 16)); P = im.load()
    for x in range(2, 14):
        for y in range(8, 13): P[x, y] = (WOOD if y < 12 else WOOD_D) + (255,)
    for x in range(2, 14): P[x, 7] = D + (255,); P[x, 13] = D + (255,)
    for y in range(8, 13): P[1, y] = D + (255,); P[14, y] = D + (255,)
    for x in range(4, 12): P[x, 6] = STEEL_L + (255,)
    for y in range(6, 9): P[4, y] = STEEL + (255,); P[11, y] = STEEL + (255,)
    for x in range(6, 10): P[x, 10] = COPPER + (255,)
    P[7, 9] = STEEL_D + (255,); P[8, 9] = STEEL_D + (255,)
    return im
def icon_cage():
    im = Image.new("RGBA", (16, 16)); P = im.load()
    for x in range(2, 14):
        for y in range(4, 13):
            if x % 3 == 2 or y in (4, 12): P[x, y] = (STEEL_L if y == 4 else STEEL) + (255,)
    for x in range(2, 14): P[x, 13] = WOOD + (255,); P[x, 14] = WOOD_D + (255,)
    for x in range(6, 10): P[x, 3] = STEEL_D + (255,)
    P[6, 2] = STEEL + (255,); P[9, 2] = STEEL + (255,); P[7, 2] = STEEL + (255,); P[8, 2] = STEEL + (255,)
    return im
icon_cage().save(f"{MOD}/textures/item/cage.png"); icon_cage().save(os.path.join(OUT, "cage_icon.png"))
# (carne de rato 3D agora e o modelo do Bino: rat_meat_3d.bbmodel)
print("ok")

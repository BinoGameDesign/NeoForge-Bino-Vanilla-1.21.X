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
CHEESE_IMG = tex_cheese()
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
for bites in range(5):
    els = cheese_model(present_cells(bites))
    bbmodel(f"cheese_{bites}", els, "cheese", CHEESE_IMG, DISPLAY_BLOCK)
    java_json(f"{MOD}/models/block/cheese_{bites}.json", els, "binosvanillaplus:block/cheese", 32, display=DISPLAY_BLOCK)
CHEESE_IMG.save(f"{MOD}/textures/block/cheese.png"); CHEESE_IMG.save(os.path.join(OUT, "cheese.png"))
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
slice_els = slice_model()
DISPLAY_SLICE = {"thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 1], "scale": [0.55, 0.55, 0.55]},
                 "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2, 1], "scale": [0.55, 0.55, 0.55]},
                 "firstperson_righthand": {"rotation": [0, -20, 10], "translation": [1, 4, 0], "scale": [0.7, 0.7, 0.7]},
                 "firstperson_lefthand": {"rotation": [0, -20, 10], "translation": [1, 4, 0], "scale": [0.7, 0.7, 0.7]},
                 "ground": {"translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
                 "gui": {"rotation": [30, 225, 0], "translation": [2, 2, 0], "scale": [1, 1, 1]}, "head": {"translation": [0, 13, 0]}, "fixed": {"scale": [0.8, 0.8, 0.8]}}
bbmodel("cheese_slice_3d", slice_els, "cheese", CHEESE_IMG, DISPLAY_SLICE, parent="")
java_json(f"{MOD}/models/item/cheese_slice_3d.json", slice_els, "binosvanillaplus:block/cheese", 32, parent=None, display=DISPLAY_SLICE)

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

# ============================================================ GAIOLA (barras de ferro, base de madeira, porta que desce)
def tex_cage():
    img = Image.new("RGBA", (32, 32)); P = img.load()
    for y in range(16):                       # barras: colunas de 1 px a cada 2, com travessas em cima/embaixo
        for x in range(16):
            bar = x % 3 == 0 or y in (0, 15)
            if bar: P[x, y] = (STEEL_L if y == 0 else STEEL_D if y == 15 or x % 6 == 0 else STEEL) + (255,)
    for y in range(16):
        for x in range(16, 32): P[x, y] = (WOOD_D if (y % 4 == 0 or random.random() < 0.07) else WOOD) + (255,)
    for y in range(16, 32):                   # tela do topo (grade)
        for x in range(16):
            if x % 3 == 0 or y % 3 == 1: P[x, y] = STEEL + (255,)
    for y in range(16, 24):
        for x in range(16, 32): P[x, y] = (STEEL_L if y == 16 else STEEL_D if y == 23 else STEEL) + (255,)
    for y in range(24, 32):
        for x in range(16, 24): P[x, y] = COPPER + (255,)
    return img
CAGE_IMG = tex_cage()
BARS, WOODUV2, MESH, SOLID, PLATE = [0, 0, 16, 16], [16, 0, 32, 4], [0, 16, 16, 32], [16, 16, 32, 18], [16, 24, 24, 30]
def cage(closed):
    x0, x1, z0, z1, h = 3, 13, 1, 15, 8
    els = [box("base", (x0, 0, z0), (x1, 1, z1), WOODUV2),
           {"name": "lado_e", "from": [x0, 1, z0], "to": [x0, h, z1], "faces": {"east": BARS, "west": BARS}},
           {"name": "lado_d", "from": [x1, 1, z0], "to": [x1, h, z1], "faces": {"east": BARS, "west": BARS}},
           {"name": "fundo", "from": [x0, 1, z1], "to": [x1, h, z1], "faces": {"north": BARS, "south": BARS}},
           {"name": "teto", "from": [x0, h, z0], "to": [x1, h, z1], "faces": {"up": MESH, "down": MESH}},
           box("alca", (7, h, 6), (9, h + 1.5, 10), SOLID), box("prato", (6.5, 1, 10), (9.5, 1.25, 13), PLATE)]
    if closed: els.append({"name": "porta", "from": [x0, 1, z0], "to": [x1, h, z0], "faces": {"north": BARS, "south": BARS}})
    else:      els.append({"name": "porta", "from": [x0, h, z0], "to": [x1, h + 0.01, z0 + 7], "faces": {"up": BARS, "down": BARS}})   # porta levantada, deitada no teto
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
# ============================================================ CARNE DE RATO 3D (corpinho + cauda enrolada); cozida = mesma forma, outra textura
def tex_meat(cooked):
    pal = ([(126, 56, 40), (110, 46, 34), (142, 70, 50)], (86, 34, 26), (150, 90, 70)) if cooked else \
          ([(214, 64, 58), (196, 48, 46), (232, 96, 86)], (150, 26, 30), (240, 150, 140))
    body, dark, fat = pal
    img = Image.new("RGBA", (32, 32)); P = img.load()
    for y in range(16):
        for x in range(32): P[x, y] = random.choice(body) + (255,)
    for _ in range(18): P[random.randrange(32), random.randrange(16)] = fat + (255,)   # gordura/marmorizado
    for x in range(32): P[x, 15] = dark + (255,)
    for y in range(16, 20):                                                           # cauda
        for x in range(32): P[x, y] = (dark if y == 19 else body[1]) + (255,)
    return img
MEAT_EL = [box("corpo", (5, 0, 5.5), (11, 3, 10.5), [0, 0, 6, 3]),
           box("topo", (6, 3, 6.5), (10, 4, 9.5), [6, 0, 10, 1]),
           box("lado_e", (4, 0.5, 6.5), (5, 2.5, 9.5), [10, 0, 13, 2]), box("lado_d", (11, 0.5, 6.5), (12, 2.5, 9.5), [13, 0, 16, 2]),
           box("cauda_1", (12, 1, 7.5), (14, 1.75, 8.25), [0, 16, 4, 17]), box("cauda_2", (13.25, 1, 8.25), (14, 1.75, 10.5), [4, 16, 8, 17]),
           box("cauda_3", (11.5, 1, 10.5), (14, 1.75, 11.25), [8, 16, 12, 17])]
# faces com uv na escala da textura 32: up/down usam a area do corpo
for e in MEAT_EL:
    e["faces"]["up"] = e["faces"]["down"] = [int(e["from"][0]), int(e["from"][2]) - 4, int(e["to"][0]), int(e["to"][2]) - 4] if e["name"].startswith(("corpo", "topo", "lado")) else e["faces"]["north"]
DISPLAY_MEAT = {"thirdperson_righthand": {"rotation": [75, 0, 0], "translation": [0, 3, 1], "scale": [0.7, 0.7, 0.7]},
                "thirdperson_lefthand": {"rotation": [75, 0, 0], "translation": [0, 3, 1], "scale": [0.7, 0.7, 0.7]},
                "firstperson_righthand": {"rotation": [10, -30, 15], "translation": [1, 5, -1], "scale": [0.8, 0.8, 0.8]},
                "firstperson_lefthand": {"rotation": [10, -30, 15], "translation": [1, 5, -1], "scale": [0.8, 0.8, 0.8]},
                "ground": {"translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]}, "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.1, 1.1, 1.1]},
                "head": {"translation": [0, 13, 0]}, "fixed": {"scale": [0.9, 0.9, 0.9]}}
for cooked in (False, True):
    n = "cooked_rat_meat" if cooked else "rat_meat"
    img = tex_meat(cooked); img.save(f"{MOD}/textures/block/{n}_3d.png"); img.save(os.path.join(OUT, f"{n}_3d.png"))
    bbmodel(f"{n}_3d", MEAT_EL, f"{n}_3d", img, DISPLAY_MEAT, parent="")
    java_json(f"{MOD}/models/item/{n}_3d.json", MEAT_EL, f"binosvanillaplus:block/{n}_3d", 32, parent=None, display=DISPLAY_MEAT)
print("ok")

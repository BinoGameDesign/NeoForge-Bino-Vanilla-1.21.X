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
        d = {"name": e.get("name", "cube"), "box_uv": False, "rescale": False, "locked": False, "render_order": "default", "allow_mirror_modeling": True,
             "from": e["from"], "to": e["to"], "autouv": 0, "color": e.get("color", 0), "origin": e.get("origin", [8, 8, 8]),
             "faces": {k: {"uv": v, "texture": 0} for k, v in e["faces"].items()}, "type": "cube", "uuid": U()}
        if e.get("rotation"): d["rotation"] = e["rotation"]
        els.append(d)
    m = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": parent,
         "ambientocclusion": True, "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [],
         "unhandled_root_fields": {}, "resolution": {"width": res, "height": res}, "elements": els, "outliner": [e["uuid"] for e in els],
         "textures": [{"path": "", "name": tex_name + ".png", "folder": "block", "namespace": "", "id": "0", "width": res, "height": res,
                       "uv_width": res, "uv_height": res, "particle": True, "render_mode": "default", "render_sides": "auto", "frame_time": 1,
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
R, H = 6, 6                        # raio e altura em pixels; centro em (8, 8)
def inside(cx, cz): return (cx + 0.5 - 8) ** 2 + (cz + 0.5 - 8) ** 2 <= R * R + 1.5
def sector(cx, cz):                # 0..4 (72 graus cada), comecando na frente-direita e girando
    a = math.degrees(math.atan2((cz + 0.5) - 8, (cx + 0.5) - 8)) % 360
    return int(((a + 90) % 360) // 72)
def cheese_elements(present):
    cells = {(x, z) for x in range(16) for z in range(16) if inside(x, z) and sector(x, z) in present}
    removed = {(x, z) for x in range(16) for z in range(16) if inside(x, z) and sector(x, z) not in present}
    els = []
    for z in range(16):            # uma faixa por linha (corre em x)
        x = 0
        while x < 16:
            if (x, z) in cells:
                x0 = x
                while x < 16 and (x, z) in cells: x += 1
                x1 = x
                def side(nb):      # casca se a vizinha esta fora da roda; miolo se foi um pedaco tirado
                    return "cut" if nb in removed else "rind"
                f = {"up": [x0, z, x1, z + 1], "down": [16 + x0, 16 + z, 16 + x1, 16 + z + 1]}
                north = all(side((xx, z - 1)) == "cut" for xx in range(x0, x1)) and any((xx, z - 1) in removed for xx in range(x0, x1))
                south = any((xx, z + 1) in removed for xx in range(x0, x1))
                f["north"] = [16 + x0, 0, 16 + x1, 6] if north else [x0, 16, x1, 22]
                f["south"] = [16 + x0, 6, 16 + x1, 12] if south else [x0, 16, x1, 22]
                f["west"] = [16 + z, 0, 16 + z + 1, 6] if (x0 - 1, z) in removed else [z, 16, z + 1, 22]
                f["east"] = [16 + z, 0, 16 + z + 1, 6] if (x1, z) in removed else [z, 16, z + 1, 22]
                # faces internas entre faixas vizinhas nao aparecem; o jogo esconde por oclusao so entre blocos, entao pulamos as cobertas
                if all((xx, z - 1) in cells for xx in range(x0, x1)): f.pop("north")
                if all((xx, z + 1) in cells for xx in range(x0, x1)): f.pop("south")
                els.append({"name": f"fatia_{z}", "from": [x0, 0, z], "to": [x1, H, z + 1], "faces": f, "color": z % 8})
            else: x += 1
    return els
DISPLAY_BLOCK = {"gui": {"rotation": [30, 225, 0], "scale": [0.8, 0.8, 0.8], "translation": [0, 2, 0]},
                 "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                 "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                 "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
                 "fixed": {"scale": [0.6, 0.6, 0.6]}}
for bites in range(5):             # bites = pedacos ja tirados
    present = set(range(bites, 5))
    els = cheese_elements(present)
    bbmodel(f"cheese_{bites}", els, "cheese", CHEESE_IMG, DISPLAY_BLOCK)
    java_json(f"{MOD}/models/block/cheese_{bites}.json", els, "binosvanillaplus:block/cheese", 32, display=DISPLAY_BLOCK)
CHEESE_IMG.save(f"{MOD}/textures/block/cheese.png"); CHEESE_IMG.save(os.path.join(OUT, "cheese.png"))
# pedaco de queijo 3D (um setor de 72 graus, deitado no meio do item)
slice_els = cheese_elements({0})
DISPLAY_SLICE = {"thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 1], "scale": [0.55, 0.55, 0.55]},
                 "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2, 1], "scale": [0.55, 0.55, 0.55]},
                 "firstperson_righthand": {"rotation": [0, -20, 10], "translation": [1, 4, 0], "scale": [0.7, 0.7, 0.7]},
                 "firstperson_lefthand": {"rotation": [0, -20, 10], "translation": [1, 4, 0], "scale": [0.7, 0.7, 0.7]},
                 "ground": {"translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
                 "gui": {"rotation": [30, 225, 0], "translation": [2, 2, 0], "scale": [1, 1, 1]}, "head": {"translation": [0, 13, 0]}, "fixed": {"scale": [0.8, 0.8, 0.8]}}
bbmodel("cheese_slice_3d", slice_els, "cheese", CHEESE_IMG, DISPLAY_SLICE, parent="")
java_json(f"{MOD}/models/item/cheese_slice_3d.json", slice_els, "binosvanillaplus:block/cheese", 32, parent=None, display=DISPLAY_SLICE)

# ============================================================ RATOEIRA (madeira + arame de aco + isca de cobre)
WOOD, WOOD_D, WOOD_L = (176, 128, 78), (140, 98, 56), (196, 150, 98)
STEEL, STEEL_D, STEEL_L = (160, 166, 172), (104, 108, 116), (214, 218, 222)
COPPER, COPPER_D = (214, 122, 80), (160, 84, 56)
def tex_trap():
    img = Image.new("RGBA", (32, 32)); P = img.load()
    for y in range(16):
        for x in range(32):
            P[x, y] = (WOOD_D if (y % 4 == 0 or random.random() < 0.07) else WOOD_L if random.random() < 0.15 else WOOD) + (255,)
    for y in range(16, 24):
        for x in range(32): P[x, y] = (STEEL_L if y == 16 else STEEL_D if y == 23 else STEEL) + (255,)
    for y in range(24, 32):
        for x in range(16): P[x, y] = (COPPER_D if (x + y) % 5 == 0 else COPPER) + (255,)
    return img
TRAP_IMG = tex_trap()
def box(name, fr, to, uvs, **kw):
    e = {"name": name, "from": list(fr), "to": list(to), "faces": {k: uvs for k in ("north", "south", "east", "west", "up", "down")}}
    e.update(kw); return e
WOODUV, STEELUV, COPUV = [0, 0, 16, 4], [0, 16, 16, 18], [0, 24, 8, 30]
def trap(sprung):
    els = [box("base", (4, 0, 1), (12, 1, 15), WOODUV),
           box("mola", (5.5, 1, 7.5), (10.5, 2, 8.5), STEELUV),
           box("isca", (6.5, 1, 10.5), (9.5, 1.5, 13.5), COPUV),
           box("trava", (7.5, 1, 4), (8.5, 1.25, 10.5), STEELUV),
           box("grampo_e", (5, 1, 2), (5.5, 1.5, 3), STEELUV), box("grampo_d", (10.5, 1, 2), (11, 1.5, 3), STEELUV)]
    # arco (U de arame): deitado para tras quando armado; para frente quando disparado (gira 180 em x pelo eixo da mola)
    bail = [box("arco_e", (5, 1.25, 8), (5.5, 1.75, 14), STEELUV), box("arco_d", (10.5, 1.25, 8), (11, 1.75, 14), STEELUV),
            box("arco_frente", (5, 1.25, 13.5), (11, 1.75, 14), STEELUV)]
    if not sprung:     # armado: arco aberto para tras (em cima da trava)
        bail = [box("arco_e", (5, 1.25, 2), (5.5, 1.75, 8), STEELUV), box("arco_d", (10.5, 1.25, 2), (11, 1.75, 8), STEELUV),
                box("arco_tras", (5, 1.25, 2), (11, 1.75, 2.5), STEELUV)]
    return els + bail
DISPLAY_FLAT = {"gui": {"rotation": [30, 225, 0], "scale": [0.9, 0.9, 0.9], "translation": [0, 2, 0]}, "ground": {"scale": [0.5, 0.5, 0.5], "translation": [0, 3, 0]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 2], "scale": [0.5, 0.5, 0.5]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0], "scale": [0.6, 0.6, 0.6]}, "fixed": {"scale": [0.8, 0.8, 0.8]}}
for st in (False, True):
    n = "rat_trap" + ("_sprung" if st else "")
    els = trap(st); bbmodel(n, els, "rat_trap", TRAP_IMG, DISPLAY_FLAT)
    java_json(f"{MOD}/models/block/{n}.json", els, "binosvanillaplus:block/rat_trap", 32, display=DISPLAY_FLAT)
TRAP_IMG.save(f"{MOD}/textures/block/rat_trap.png"); TRAP_IMG.save(os.path.join(OUT, "rat_trap.png"))

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
icon_trap().save(f"{MOD}/textures/item/rat_trap.png"); icon_trap().save(os.path.join(OUT, "rat_trap_icon.png"))
icon_cage().save(f"{MOD}/textures/item/cage.png"); icon_cage().save(os.path.join(OUT, "cage_icon.png"))
print("ok")

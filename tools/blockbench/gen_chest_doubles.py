# Baus duplos a partir de baus simples ja editados pelo Bino (modelo + textura deles):
#   trapped_chest.bbmodel -> trapped_chest_double.bbmodel
#   mimic_chest.bbmodel   -> mimic_chest_double.bbmodel (dentes da frente duplicados para cobrir os 2 blocos,
#                            dentes das laterais vao para as pontas, lingua fica no meio)
# Usa a mesma regra dos outros duplos (gen_chest_variants.make_double): o que atravessa o bau estica e a pintura e'
# copiada repetindo o meio; o resto so muda de lugar. Rodar da raiz do repo: python3 tools/blockbench/gen_chest_doubles.py
import json, os, copy, uuid
D = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(D, "chests")
src = open(os.path.join(D, "gen_chest_variants.py")).read().split("\nbase = json.load(open(BASE))")[0]
g = {"__file__": os.path.join(D, "gen_chest_variants.py")}; exec(src, g)

def find_parent(nodes, uid):
    for n in nodes:
        if isinstance(n, dict):
            if uid in n["children"]: return n
            f = find_parent(n["children"], uid)
            if f: return f
    return None

def shift(e, k):
    e["from"][0] += k; e["to"][0] += k
    if "origin" in e: e["origin"][0] += k

def split_teeth(m):
    """tira os dentes da regra geral: frente reta = 3 copias; cantos e laterais vao para o lado deles. Devolve os dentes."""
    teeth = [e for e in m["elements"] if e["name"].startswith("tooth")]
    for e in teeth:
        c = (e["from"][0] + e["to"][0]) / 2; zc = (e["from"][2] + e["to"][2]) / 2
        if zc > 4.5 and abs(c) < 4:   # fileira reta da frente: 3 copias (-8, 0, +8) cobrem os 2 blocos
            par = find_parent(m["outliner"], e["uuid"])
            for k in (-8, 8):
                twin = copy.deepcopy(e); twin["uuid"] = str(uuid.uuid4()); shift(twin, k)
                m["elements"].append(twin); par["children"].append(twin["uuid"])
        else:                          # cantos curvos e laterais: vao para a ponta do lado deles
            shift(e, 8 if c > 0 else -8)
    return teeth

def build(src_name, out_name):
    m = json.load(open(os.path.join(OUT, src_name + ".bbmodel"))); img = g["tex_of"](m)
    for e in m["elements"]:
        e.setdefault("uv_offset", [0, 0]); e.setdefault("origin", [0, 0, 0])
    for gr in m["groups"]:
        if gr["name"] == "teeth_botton": gr["name"] = "teeth_bottom"   # nome que o codigo procura
    keep = []
    if any(e["name"].startswith("tooth") for e in m["elements"]):
        teeth = split_teeth(m)
        ids = {e["uuid"] for e in m["elements"] if e["name"].startswith("tooth")}
        keep = [e for e in m["elements"] if e["uuid"] in ids]
        m["elements"] = [e for e in m["elements"] if e["uuid"] not in ids]
    new_img = g["make_double"](m, img)
    m["elements"] += keep
    g["save"](m, new_img, out_name)

build("trapped_chest", "trapped_chest_double")
build("mimic_chest", "mimic_chest_double")
# o mimico simples tambem passa a usar o nome teeth_bottom
m = json.load(open(os.path.join(OUT, "mimic_chest.bbmodel")))
for gr in m["groups"]:
    if gr["name"] == "teeth_botton": gr["name"] = "teeth_bottom"
json.dump(m, open(os.path.join(OUT, "mimic_chest.bbmodel"), "w"))

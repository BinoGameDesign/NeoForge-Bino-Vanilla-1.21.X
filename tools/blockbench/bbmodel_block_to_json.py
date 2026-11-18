# Converte um .bbmodel (formato Java Block/Item) no .json do jogo (Minecraft 1.21.1).
# - aplica o "inflate" nas medidas (o json nao tem inflate)
# - rotacao: o 1.21.1 so aceita UM eixo e angulos -45/-22.5/0/22.5/45 -> arredonda para o mais proximo e avisa
# - uv na escala do json (0..16), usando o uv_width da textura
# Uso: python3 tools/blockbench/bbmodel_block_to_json.py <arquivo.bbmodel> <saida.json> <textura do jogo, ex binosvanillaplus:block/cheese> [parent]
import json, sys
src, out, tex = sys.argv[1:4]
parent = sys.argv[4] if len(sys.argv) > 4 else None
m = json.load(open(src))
uvw = m["textures"][0].get("uv_width") or m["resolution"]["width"]
k = 16 / uvw
j = ({"parent": parent} if parent else {}) | {"textures": {"0": tex, "particle": tex}, "elements": []}
for e in m["elements"]:
    if e.get("export") is False: continue
    inf = e.get("inflate", 0) or 0
    el = {"from": [round(c - inf, 4) for c in e["from"]], "to": [round(c + inf, 4) for c in e["to"]], "faces": {}}
    for f, v in e["faces"].items():
        if v.get("texture") is None: continue
        face = {"uv": [round(c * k, 4) for c in v["uv"]], "texture": "#0"}
        if v.get("rotation"): face["rotation"] = v["rotation"]
        el["faces"][f] = face
    r = e.get("rotation")
    if r and any(abs(a) > 1e-6 for a in r):
        ax = max(range(3), key=lambda i: abs(r[i]))
        ang = max(-45.0, min(45.0, round(r[ax] / 22.5) * 22.5))
        if abs(ang - r[ax]) > 1e-3 or sum(1 for a in r if abs(a) > 1e-6) > 1:
            print(f"  aviso: {e.get('name')} rotacao {r} -> {'xyz'[ax]} {ang}")
        if ang != 0: el["rotation"] = {"angle": ang, "axis": "xyz"[ax], "origin": e["origin"]}
    j["elements"].append(el)
if m.get("display"): j["display"] = m["display"]
json.dump(j, open(out, "w"), indent=1)
print("ok", out, len(j["elements"]), "cubos")

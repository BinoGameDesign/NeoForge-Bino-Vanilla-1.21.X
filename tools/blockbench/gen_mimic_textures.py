# Texturas do mimico para cada bau em que ele pode estar (normal, armadilha, natal; simples e duplo):
# o corpo do mimico usa a pintura da versao do bau (peca por peca, pelo mesmo cubo do modelo), dentes e lingua ficam
# os do mimico. Saida: textures/entity/chest_bino/mimic_<versao>.png. Rodar da raiz: python3 tools/blockbench/gen_mimic_textures.py
import json, os, io, base64, math
from PIL import Image
D = os.path.dirname(os.path.abspath(__file__)); C = os.path.join(D, "chests")
OUT = os.path.join(D, "..", "..", "src", "main", "resources", "assets", "binosvanillaplus", "textures", "entity", "chest_bino")
os.makedirs(OUT, exist_ok=True)

def load(n):
    m = json.load(open(os.path.join(C, n + ".bbmodel")))
    t = m["textures"][0]
    img = Image.open(io.BytesIO(base64.b64decode(t["source"].split(",", 1)[1]))).convert("RGBA")
    sx = img.width / m["resolution"]["width"]
    return m, img, sx

def rects(e, s):
    out = {}
    for f, fd in e["faces"].items():
        u1, v1, u2, v2 = fd["uv"]
        out[f] = (round(min(u1, u2) * s), round(min(v1, v2) * s), round(abs(u2 - u1) * s), round(abs(v2 - v1) * s))
    return out

def copy_rect(src, sr, dst, dr):
    su, sv, sw, sh = sr; du, dv, dw, dh = dr
    if sw <= 0 or sh <= 0 or dw <= 0 or dh <= 0: return
    for j in range(dh):
        for i in range(dw):
            dst.putpixel((du + i, dv + j), src.getpixel((su + min(sw - 1, i * sw // dw), sv + min(sh - 1, j * sh // dh))))

def build(mimic, variant_model, out_name):
    mm, mimg, ms = load(mimic); vm, vimg, vs = load(variant_model)
    vel = {e["uuid"]: e for e in vm["elements"]}
    by_shape = {}
    for e in vm["elements"]: by_shape.setdefault((e["name"], tuple(e["from"]), tuple(e["to"])), e)
    out = mimg.copy(); n = 0
    for e in mm["elements"]:
        if e["name"].startswith(("tooth", "tongue")): continue
        ve = vel.get(e["uuid"]) or by_shape.get((e["name"], tuple(e["from"]), tuple(e["to"])))
        if ve is None: continue
        mr, vr = rects(e, ms), rects(ve, vs)
        for f in mr:
            if f in vr: copy_rect(vimg, vr[f], out, mr[f])
        n += 1
    out.save(os.path.join(OUT, out_name + ".png"))
    print("ok", out_name, n, "pecas copiadas de", variant_model)

for mimic, suffix in (("mimic_chest", ""), ("mimic_chest_double", "_double")):
    m, img, s = load(mimic); img.save(os.path.join(OUT, "mimic_normal" + suffix + ".png"))
    build(mimic, "trapped_chest" + suffix, "mimic_trapped" + suffix)
    build(mimic, "christmas_chest" + suffix, "mimic_christmas" + suffix)

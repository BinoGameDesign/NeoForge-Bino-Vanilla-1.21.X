# Gera as versoes dos baus a partir do bau normal do Bino (tools/blockbench/chests/chest.bbmodel, modelo + textura dele):
#  - armadilha, natal, ender: mesma geometria e UV; a textura dele e' RECOLORIDA por peca (madeira -> material da
#    versao, metal -> metal da versao), mantendo o desenho dele; detalhes por versao (fita de natal, olho de ender, gancho).
#  - duplos (normal, armadilha, natal): pecas que atravessam o bau esticam para 2 blocos; a pintura delas e' copiada da
#    face original repetindo o meio (as bordas ficam iguais). Pecas so deslocadas usam o mesmo UV.
#  - mimico: o bau dele + dentes e lingua (pintados num espaco livre) + animacoes do mimico.
# Rodar da raiz do repo: python3 tools/blockbench/gen_chest_variants.py
import json, os, io, base64, copy, math, colorsys, random, importlib.util
from PIL import Image

D = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(D, "chests")
BASE = os.path.join(OUT, "chest.bbmodel")
spec = importlib.util.spec_from_file_location("cs", os.path.join(D, "gen_chest_set.py"))
src = open(os.path.join(D, "gen_chest_set.py")).read().split("# ------------------------------------------------------------------ montar")[0]
cs = {"__file__": os.path.join(D, "gen_chest_set.py")}; exec(src, cs)   # reaproveita paleta, funcoes de face, mimico, animacoes

def tex_of(m):
    t = m["textures"][0]
    return Image.open(io.BytesIO(base64.b64decode(t["source"].split(",", 1)[1]))).convert("RGBA")

def set_tex(m, img, name):
    buf = io.BytesIO(); img.save(buf, "PNG"); t = m["textures"][0]
    t.update({"name": name + ".png", "width": img.width, "height": img.height, "uv_width": m["resolution"]["width"],
              "uv_height": m["resolution"]["height"], "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()})

def rects(e):
    out = {}
    for f, fd in e["faces"].items():
        u1, v1, u2, v2 = fd["uv"]
        out[f] = (min(u1, u2), min(v1, v2), abs(u2 - u1), abs(v2 - v1))
    return out

# ------------------------------------------------------------------ papel de cada pixel (madeira / metal / ...)
def role_map(m, W, H):
    rm = [[None] * W for _ in range(H)]
    for e in m["elements"]:
        rl = cs["role"](e)
        kind = "wood" if rl in ("body", "top", "rim") else ("lock" if rl == "lock" else "metal")
        for f, (u, v, w, h) in rects(e).items():
            for y in range(int(v), min(H, math.ceil(v + h))):
                for x in range(int(u), min(W, math.ceil(u + w))):
                    rm[y][x] = (kind, e["name"], f, (u, v, w, h))
    return rm

def hls(c): return colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
def rgb(h, l, s): r, g, b = colorsys.hls_to_rgb(h % 1.0, max(0, min(1, l)), max(0, min(1, s))); return (round(r * 255), round(g * 255), round(b * 255))

# como cada versao muda a madeira e o metal (matiz alvo, fator de saturacao, fator/soma de brilho)
VARIANTS = {
    "trapped":   dict(wood=(None, 0.95, 0.92, 0.0), metal=("gray", 0.0, 1.0, 0.0)),
    "christmas": dict(wood=(0.0, 1.9, 0.95, 0.05), metal=(None, 1.0, 1.0, 0.0)),
    "ender":     dict(wood=(0.76, 0.6, 0.38, 0.0), metal=(0.45, 0.8, 0.55, 0.0)),
}

def recolor(img, rm, variant):
    V = VARIANTS[variant]; out = img.copy(); rnd = random.Random(3)
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3] == 0 or y >= len(rm) or rm[y][x] is None: continue
            kind = rm[y][x][0]
            h, l, s = hls(c)
            # pela cor do desenho do Bino: dourado/laranja saturado = metal (mesmo na faixa da tampa); marrom = madeira
            if kind != "lock": kind = "metal" if (s > 0.55 and l > 0.3) else "wood"
            spec_ = V["wood"] if kind == "wood" else V["metal"]
            th, ks, kl, bl = spec_
            if th == "gray": nc = rgb(0.6, l * kl + bl, 0.06)
            else: nc = rgb(h if th is None else th, l * kl + bl, s * ks)
            if variant == "ender" and kind == "wood" and rnd.random() < 0.05: nc = (104, 70, 150)
            out.putpixel((x, y), nc + (c[3],))
    return out

def detail(img, m, variant):
    for e in m["elements"]:
        rl = cs["role"](e)
        for f, (u, v, w, h) in rects(e).items():
            u0, v0, W_, H_ = int(u), int(v), max(1, math.ceil(w)), max(1, math.ceil(h))
            if variant == "christmas" and rl in ("body", "top") and f in ("north", "south", "up"):   # fita verde no meio
                for j in range(H_):
                    for i in range(W_):
                        if abs(i - (W_ - 1) / 2) <= 1.0:
                            g = (40, 128, 62) if (j + i) % 5 else (60, 160, 84)
                            img.putpixel((u0 + i, v0 + j), g + (255,))
            if rl == "lock" and f == "south" and W_ >= 2:
                cx = (W_ - 1) // 2
                if variant == "ender":     # olho de ender
                    for j in range(1, H_ - 1):
                        for i in range(1, W_ - 1): img.putpixel((u0 + i, v0 + j), (70, 190, 130, 255))
                    img.putpixel((u0 + cx, v0 + H_ // 2), (10, 30, 20, 255))
                elif variant == "trapped":  # ganchinho vermelho de armadilha
                    img.putpixel((u0 + cx, v0 + max(0, H_ // 2 - 1)), (178, 36, 36, 255))
    return img

# ------------------------------------------------------------------ duplo
def used_height(img):
    for y in range(img.height - 1, -1, -1):
        if any(img.getpixel((x, y))[3] for x in range(img.width)): return y + 1
    return 0

def tile_copy(src, srect, dst, drect):
    su, sv, sw, sh = [int(round(a)) for a in srect]; du, dv, dw, dh = [int(round(a)) for a in drect]
    if sw <= 0 or sh <= 0 or dw <= 0 or dh <= 0: return
    edge = 2 if sw > 6 else 0
    for j in range(dh):
        sj = min(sh - 1, j * sh // dh) if dh != sh else j
        for i in range(dw):
            if dw == sw: si = i
            elif i < edge: si = i
            elif i >= dw - edge: si = sw - (dw - i)
            else: si = edge + ((i - edge) % max(1, sw - 2 * edge))
            dst.putpixel((du + i, dv + j), src.getpixel((su + si, sv + sj)))

def make_double(m, img):
    W = m["resolution"]["width"]
    new = Image.new("RGBA", (W, 256), (0, 0, 0, 0)); new.paste(img, (0, 0))
    x = 0; y = max(used_height(img), 0); shelf = 0
    if y > 128: raise SystemExit("textura cheia")
    y = 128   # pecas esticadas ganham espaco novo na metade de baixo
    for e in m["elements"]:
        x0, x1 = e["from"][0], e["to"][0]; c = (x0 + x1) / 2; w = x1 - x0
        if w >= 10:
            old = rects(e)
            e["from"][0] -= 8; e["to"][0] += 8
            sw, sh, sd = cs["size"](e)
            fw, fh = math.ceil(2 * (sw + sd)), math.ceil(sd + sh)
            if x + fw > W: x, y, shelf = 0, y + shelf, 0
            e["uv_offset"] = [x, y]; e["faces"] = cs["faces_json"](x, y, sw, sh, sd, mirror=bool(e.get("mirror_uv")))
            for f, r in rects(e).items(): tile_copy(img, old[f], new, r)
            x += fw; shelf = max(shelf, fh)
        elif c > 0.5 or c < -0.5:
            k = 8 if c > 0 else -8
            e["from"][0] += k; e["to"][0] += k
            if abs(e.get("origin", [0])[0]) > 1: e["origin"][0] += k
    m["resolution"] = {"width": W, "height": 256}
    m["visible_box"] = [2, 1, 0.5]
    return new

# ------------------------------------------------------------------ mimico
def make_mimic(m, img):
    before = {e["uuid"] for e in m["elements"]}
    cs["add_mimic_parts"](m)
    W = m["resolution"]["width"]
    y0 = used_height(img) + 1; x = 0; y = y0; shelf = 0
    new_els = [e for e in m["elements"] if e["uuid"] not in before]
    big = Image.new("RGBA", (W, 256), (0, 0, 0, 0)); big.paste(img, (0, 0))
    for e in new_els:
        w, h, d = cs["size"](e); fw, fh = math.ceil(2 * (w + d)), math.ceil(d + h)
        if x + fw > W: x, y, shelf = 0, y + shelf, 0
        e["uv_offset"] = [x, y]; e["faces"] = cs["faces_json"](x, y, w, h, d)
        x += fw; shelf = max(shelf, fh)
    H = 128 if y + shelf <= 128 else 256
    m["resolution"] = {"width": W, "height": H}
    part = cs["paint"](new_els, cs["PAL"]["normal"], W, 256)
    big.alpha_composite(part)
    return big.crop((0, 0, W, H))

# ------------------------------------------------------------------ montar
def save(m, img, name):
    m["name"] = name; m["model_identifier"] = name
    set_tex(m, img, name)
    json.dump(m, open(os.path.join(OUT, name + ".bbmodel"), "w"))
    img.save(os.path.join(OUT, name + ".png"))
    print("ok", name, img.size, len(m["elements"]), "cubos")

base = json.load(open(BASE)); base_img = tex_of(base)
for e in base["elements"]:
    e.setdefault("uv_offset", [0, 0]); e.setdefault("origin", [0, 0, 0])
W, H = base["resolution"]["width"], base["resolution"]["height"]
rm = role_map(base, W, H)

variants = {"normal": base_img}
recolored = {v: recolor(base_img, rm, v) for v in ("trapped", "christmas", "ender")}
for v in ("trapped", "christmas", "ender"): variants[v] = detail(recolored[v].copy(), base, v)

save(copy.deepcopy(base), base_img, "chest")
for v, name in (("trapped", "trapped_chest"), ("christmas", "christmas_chest"), ("ender", "ender_chest")):
    save(copy.deepcopy(base), variants[v], name)
for v, name in (("normal", "chest_double"), ("trapped", "trapped_chest_double"), ("christmas", "christmas_chest_double")):
    m = copy.deepcopy(base)
    img = make_double(m, base_img if v == "normal" else recolored[v])   # estica antes dos detalhes (fita/gancho uma vez so)
    if v != "normal": img = detail(img, m, v)
    save(m, img, name)
m = copy.deepcopy(base); img = make_mimic(m, base_img)
m["animations"] = [a for a in m["animations"]] + [a for a in cs["mimic_anims"](m) if a["name"] not in {x["name"] for x in m["animations"]}]
save(m, img, "mimic_chest")

# Gera os .bbmodel dos baus do jogo (normal, armadilha, natal, ender; simples e duplo) para o Bino editar no Blockbench.
# Modelo do jogo (ChestRenderer, 1.21.1): fundo, tampa (gira na dobradica de tras) e fechadura (gira junto com a tampa).
# O jogo desenha o bau sem virar o modelo; o Blockbench (formato de entidade) mostra como os mobs, virado 180 graus em Z.
# Por isso a textura e' REORGANIZADA pixel a pixel: no Blockbench (e no jogo, pelo nosso renderizador) o bau fica igual
# ao original. Frente (fechadura) = lado SUL (+Z) do Blockbench. Texturas originais em tools/blockbench/chests/vanilla/.
# Rodar: python3 tools/blockbench/gen_chests.py
import json, uuid, os, base64, io
from PIL import Image
D = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(D, "chests"); VAN = os.path.join(OUT, "vanilla")
U = lambda: str(uuid.uuid4())

# caixas do jogo em coordenadas do bloco (pixels): (nome, texOffs, pivo, min relativo ao pivo, tamanho)
SINGLE = [("bottom", (0, 19), (0, 0, 0), (1, 0, 1), (14, 10, 14)),
          ("lid", (0, 0), (0, 9, 1), (1, 0, 0), (14, 5, 14)),
          ("lock", (0, 0), (0, 9, 1), (7, -2, 14), (2, 4, 1))]
LEFT = [("bottom", (0, 19), (0, 0, 0), (0, 0, 1), (15, 10, 14)),
        ("lid", (0, 0), (0, 9, 1), (0, 0, 0), (15, 5, 14)),
        ("lock", (0, 0), (0, 9, 1), (0, -2, 14), (1, 4, 1))]
RIGHT = [("bottom", (0, 19), (0, 0, 0), (1, 0, 1), (15, 10, 14)),
         ("lid", (0, 0), (0, 9, 1), (1, 0, 0), (15, 5, 14)),
         ("lock", (0, 0), (0, 9, 1), (15, -2, 14), (1, 4, 1))]

# faces de um cubo do jogo (ModelPart.Cube): cada face = 4 vertices (posicao) com o seu uv
def polys(mn, size, uv):
    x0, y0, z0 = mn; w, h, d = size; x1, y1, z1 = x0 + w, y0 + h, z0 + d; u, v = uv
    V7, V0, V1, V2 = (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)
    V3, V4, V5, V6 = (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    def P(vs, u1, v1, u2, v2): return list(zip(vs, [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]))
    return [P([V4, V3, V7, V0], f5, f10, f6, f11), P([V1, V2, V6, V5], f6, f11, f7, f10),
            P([V7, V3, V6, V2], f4, f11, f5, f12), P([V0, V7, V2, V1], f5, f11, f6, f12),
            P([V4, V0, V1, V5], f6, f11, f8, f12), P([V3, V4, V5, V6], f8, f11, f9, f12)]

def relayout(src, boxes):
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for _, uv, piv, mn, size in boxes:
        world_min = tuple(piv[i] + mn[i] for i in range(3))
        van = polys(world_min, size, uv)
        # o mesmo cubo como o nosso pipeline monta (Blockbench -> Java de entidade -> desenhado virado em X e Y)
        jmin = (8 - (world_min[0] + size[0]), 24 - (world_min[1] + size[1]), world_min[2] - 8)
        for pj in polys(jmin, size, uv):
            W = [(8 - p[0], 24 - p[1], 8 + p[2]) for p, _ in pj]
            match = next((pv for pv in van if sorted(p for p, _ in pv) == sorted(W)), None)
            if match is None: raise SystemExit("face sem par")
            vuv = {p: t for p, t in match}
            corner = [vuv[w] for w in W]                     # uv do jogo em cada canto desta face
            (u2, v1), (u1, _), (_, v2), _ = [t for _, t in pj]
            if u1 == u2 or v1 == v2: continue
            for ty in range(int(min(v1, v2)), int(max(v1, v2))):
                for tx in range(int(min(u1, u2)), int(max(u1, u2))):
                    a = (tx + 0.5 - u1) / (u2 - u1); b = (ty + 0.5 - v1) / (v2 - v1)
                    # cantos: 0 = (u2,v1), 1 = (u1,v1), 2 = (u1,v2), 3 = (u2,v2)
                    su = (1-a)*(1-b)*corner[1][0] + a*(1-b)*corner[0][0] + (1-a)*b*corner[2][0] + a*b*corner[3][0]
                    sv = (1-a)*(1-b)*corner[1][1] + a*(1-b)*corner[0][1] + (1-a)*b*corner[2][1] + a*b*corner[3][1]
                    out.putpixel((tx, ty), src.getpixel((min(63, int(su)), min(63, int(sv)))))
    return out

def faces(u, v, w, h, d):
    return {"north": {"uv": [u+d, v+d, u+d+w, v+d+h], "texture": 0}, "east": {"uv": [u, v+d, u+d, v+d+h], "texture": 0},
            "south": {"uv": [u+d+w+d, v+d, u+2*d+2*w, v+d+h], "texture": 0}, "west": {"uv": [u+d+w, v+d, u+2*d+w, v+d+h], "texture": 0},
            "up": {"uv": [u+d+w, v+d, u+d, v], "texture": 0}, "down": {"uv": [u+2*w+d, v, u+d+w, v+d], "texture": 0}}

def group(name, origin):
    return {"name": name, "uuid": U(), "export": True, "locked": False, "scope": 0, "selected": False, "visibility": True,
            "origin": origin, "rotation": [0, 0, 0], "color": 0, "children": [], "reset": False, "shade": True,
            "mirror_uv": False, "autouv": 0, "isOpen": True}

def bbmodel(name, boxes, tex):
    els, grp = {}, {}
    for bname, uv, piv, mn, size in boxes:
        w0 = [piv[i] + mn[i] for i in range(3)]
        frm = [w0[0] - 8, w0[1], w0[2] - 8]; to = [frm[0] + size[0], frm[1] + size[1], frm[2] + size[2]]
        org = [piv[0] - 8 if bname == "bottom" else 0, piv[1], piv[2] - 8]
        if bname == "bottom": org = [0, 0, 0]
        e = {"name": bname, "box_uv": True, "render_order": "default", "locked": False, "export": True, "scope": 0,
             "allow_mirror_modeling": True, "from": frm, "to": to, "autouv": 0, "color": 0, "origin": org,
             "uv_offset": list(uv), "faces": faces(uv[0], uv[1], *size), "type": "cube", "uuid": U()}
        els[bname] = e
    g_bottom, g_lid, g_lock = group("bottom", [0, 0, 0]), group("lid", [0, 9, -7]), group("lock", [0, 9, -7])
    outliner = [{"uuid": g_bottom["uuid"], "isOpen": True, "children": [els["bottom"]["uuid"]]},
                {"uuid": g_lid["uuid"], "isOpen": True, "children": [els["lid"]["uuid"],
                    {"uuid": g_lock["uuid"], "isOpen": True, "children": [els["lock"]["uuid"]]}]}]
    buf = io.BytesIO(); tex.save(buf, "PNG")
    t = {"name": name + ".png", "path": "", "folder": "", "namespace": "", "id": "0", "group": "", "scope": 0, "width": 64, "height": 64,
         "uv_width": 64, "uv_height": 64, "particle": False, "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
         "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color",
         "fps": 7, "visible": True, "internal": True, "saved": True, "uuid": U(), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}
    return {"meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True}, "name": name, "model_identifier": name,
            "modded_entity_entity_class": "", "modded_entity_version": "1.17", "modded_entity_flip_y": True, "visible_box": [1, 1, 0.5],
            "variable_placeholders": "", "multi_file_ruleset": "", "variable_placeholder_buttons": [], "timeline_setups": [],
            "unhandled_root_fields": {}, "resolution": {"width": 64, "height": 64}, "elements": list(els.values()),
            "groups": [g_bottom, g_lid, g_lock], "outliner": outliner, "textures": [t], "animations": []}

JOBS = [("chest", "normal", SINGLE), ("chest_left", "normal_left", LEFT), ("chest_right", "normal_right", RIGHT),
        ("trapped_chest", "trapped", SINGLE), ("trapped_chest_left", "trapped_left", LEFT), ("trapped_chest_right", "trapped_right", RIGHT),
        ("christmas_chest", "christmas", SINGLE), ("christmas_chest_left", "christmas_left", LEFT), ("christmas_chest_right", "christmas_right", RIGHT),
        ("ender_chest", "ender", SINGLE), ("mimic_chest", "normal", SINGLE)]
for name, tex, boxes in JOBS:
    src = Image.open(os.path.join(VAN, tex + ".png")).convert("RGBA")
    t = relayout(src, boxes)
    t.save(os.path.join(OUT, name + ".png"))
    json.dump(bbmodel(name, boxes, t), open(os.path.join(OUT, name + ".bbmodel"), "w"))
    print("ok", name)

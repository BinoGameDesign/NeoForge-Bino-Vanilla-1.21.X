#!/usr/bin/env python3
"""Converte as armaduras 3D do Blockbench (tools/armor_models/<peca>.bbmodel, formato Modded Entity feito a
partir dos modelos base) em codigo do jogo (client/armor/Armor3DMeshes.java) e na textura
(textures/models/armor/<peca>_3d.png).

Uso: python3 tools/bbmodel_armor.py      (converte todos os .bbmodel da pasta)

Regras do modelo: grupos de topo cabeca/corpo/braco_direito/braco_esquerdo/perna_direita/perna_esquerda com os
pontos de giro do modelo base; cubos com "Box UV"; cubos "referencia" (export desligado) sao ignorados.
Coordenadas: jogo = (-x, 24 - y, z) do Blockbench; rotacao do cubo = (-rx, -ry, rz) (igual ao exportador Java).
"""
import base64, glob, io, json, math, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "tools", "armor_models")
TEX = os.path.join(ROOT, "src/main/resources/assets/binosvanillaplus/textures/models/armor")
OUT = os.path.join(ROOT, "src/main/java/com/bino/binosvanillaplus/client/armor/Armor3DMeshes.java")

BONES = {"cabeca": ("head", (0, 0, 0)), "corpo": ("body", (0, 0, 0)), "braco_direito": ("right_arm", (-5, 2, 0)),
         "braco_esquerdo": ("left_arm", (5, 2, 0)), "perna_direita": ("right_leg", (-1.9, 12, 0)),
         "perna_esquerda": ("left_leg", (1.9, 12, 0))}
ORDER = ["head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"]


def f(v):
    return f"{round(v, 5):g}F"


def camel(name):
    a = name.split("_")
    return a[0] + "".join(p.capitalize() for p in a[1:])


def convert(path):
    piece = os.path.splitext(os.path.basename(path))[0]
    d = json.load(open(path, encoding="utf-8"))
    els = {e["uuid"]: e for e in d["elements"]}
    groups = {g["uuid"]: g for g in d.get("groups", [])}
    owner = {}

    def walk(node, bone):
        if isinstance(node, str):
            if bone: owner[node] = bone
            return
        g = groups.get(node["uuid"], {})
        b = bone or BONES.get(g.get("name"), (None,))[0]
        for c in node.get("children", []): walk(c, b)

    for n in d["outliner"]: walk(n, None)

    # textura (aumenta a largura/altura se algum cubo passar da borda, repetindo a ultima coluna/linha)
    tex = Image.open(io.BytesIO(base64.b64decode(d["textures"][0]["source"].split(",", 1)[1]))).convert("RGBA")
    tw, th = d["resolution"]["width"], d["resolution"]["height"]
    if tex.size != (tw, th): tex = tex.resize((tw, th), Image.NEAREST)
    need_w, need_h = tw, th
    for e in d["elements"]:
        if e.get("export", True) is False: continue
        for fc in e["faces"].values():
            need_w = max(need_w, math.ceil(max(fc["uv"][0], fc["uv"][2])))
            need_h = max(need_h, math.ceil(max(fc["uv"][1], fc["uv"][3])))
    W, H = tw, th
    while W < need_w: W *= 2
    while H < need_h: H *= 2
    if (W, H) != (tw, th):
        big = Image.new("RGBA", (W, H), (0, 0, 0, 0)); big.paste(tex, (0, 0))
        px = big.load()
        for y in range(H):
            for x in range(tw, W): px[x, y] = px[tw - 1, y] if y < th else (0, 0, 0, 0)
        for y in range(th, H):
            for x in range(W): px[x, y] = px[x, th - 1] if x < tw else (0, 0, 0, 0)
        tex = big
        print(f"  {piece}: um cubo passa da borda da textura; textura aumentada para {W}x{H}")
    tex.save(os.path.join(TEX, piece + "_3d.png"))

    parts = {b: [] for b in ORDER}   # (codigo do cubo) por osso; filhos girados separados
    children = {b: [] for b in ORDER}
    for e in d["elements"]:
        if e.get("export", True) is False or e["uuid"] not in owner: continue
        if not e.get("box_uv", True):
            print(f"  aviso {piece}: '{e['name']}' sem Box UV (o jogo so usa Box UV) - usando o uv_offset")
        bone = owner[e["uuid"]]
        P = next(p for (n, p) in BONES.values() if n == bone)
        fr, to = e["from"], e["to"]
        size = [to[i] - fr[i] for i in range(3)]
        mn = (-to[0], 24 - to[1], fr[2])
        uv = e.get("uv_offset", [0, 0])
        mirror = e.get("mirror_uv", False)
        cube = (f".texOffs({uv[0]}, {uv[1]})" + (".mirror()" if mirror else "") +
                f".addBox({f(0)}, {f(0)}, {f(0)}, {f(size[0])}, {f(size[1])}, {f(size[2])}, new CubeDeformation({f(e.get('inflate', 0))}))" +
                (".mirror(false)" if mirror else ""))
        rot = e.get("rotation", [0, 0, 0])
        if any(abs(r) > 1e-6 for r in rot):
            o = e["origin"]; O = (-o[0], 24 - o[1], o[2])
            rel = [mn[i] - O[i] for i in range(3)]
            cube = cube.replace(f".addBox({f(0)}, {f(0)}, {f(0)},", f".addBox({f(rel[0])}, {f(rel[1])}, {f(rel[2])},")
            children[bone].append((cube, [O[i] - P[i] for i in range(3)], [-math.radians(rot[0]), -math.radians(rot[1]), math.radians(rot[2])]))
        else:
            rel = [mn[i] - P[i] for i in range(3)]
            parts[bone].append(cube.replace(f".addBox({f(0)}, {f(0)}, {f(0)},", f".addBox({f(rel[0])}, {f(rel[1])}, {f(rel[2])},"))

    pivots = {n: p for (n, p) in BONES.values()}
    pivots["hat"] = (0, 0, 0)
    lines = [f"    // {piece} (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/{piece}.bbmodel)",
             f"    public static LayerDefinition {camel(piece)}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    for b in ORDER:
        p = pivots[b]
        body = "CubeListBuilder.create()" + "".join("\n                " + c for c in parts[b])
        var = b.replace("_", "")
        lines.append(f"        PartDefinition {var} = root.addOrReplaceChild(\"{b}\", {body}, PartPose.offset({f(p[0])}, {f(p[1])}, {f(p[2])}));")
        for i, (c, off, r) in enumerate(children[b]):
            lines.append(f"        {var}.addOrReplaceChild(\"{b}_r{i}\", CubeListBuilder.create(){c},\n"
                         f"                PartPose.offsetAndRotation({f(off[0])}, {f(off[1])}, {f(off[2])}, {f(r[0])}, {f(r[1])}, {f(r[2])}));")
    lines.append(f"        return LayerDefinition.create(mesh, {W}, {H});")
    lines.append("    }")
    return "\n".join(lines)


def main():
    files = sorted(glob.glob(os.path.join(SRC, "*.bbmodel")))
    methods = []
    for p in files:
        print("convertendo", os.path.basename(p))
        methods.append(convert(p))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as o:
        o.write("""package com.bino.binosvanillaplus.client.armor;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

// ARQUIVO GERADO: nao edite a mao. Mude o .bbmodel em tools/armor_models e rode tools/bbmodel_armor.py
@SuppressWarnings("unused")
public final class Armor3DMeshes {
    private Armor3DMeshes() {}

""" + "\n\n".join(methods) + "\n}\n")


if __name__ == "__main__":
    main()

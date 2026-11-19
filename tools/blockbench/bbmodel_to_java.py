# Converte um .bbmodel (Blockbench, formato Modded Entity, box UV) em duas classes Java para o Minecraft vanilla (1.21.1, mojmap):
#   <Nome>ModelData.java   -> createBodyLayer() (LayerDefinition com ossos e cubos)
#   <Nome>Animations.java  -> AnimationDefinition de cada animacao (keyframes nativos do jogo)
# Uso: python3 tools/blockbench/bbmodel_to_java.py <arquivo.bbmodel> <pacote java> <Nome> <pasta de saida>
# Exemplo: python3 tools/blockbench/bbmodel_to_java.py tools/blockbench/cave_goblin.bbmodel com.bino.binosvanillaplus.client.goblin CaveGoblin src/main/java/com/bino/binosvanillaplus/client/goblin
#
# Eixos: o Java do Minecraft = Blockbench girado 180 graus em Z (x e y trocam de sinal; y ainda soma 24 na raiz).
#   posicao/pivo: (-x, 24 - y, z)      rotacao (pose e keyframes): (-x, -y, z)      posicao em keyframe: posVec(-x, y, z)
import json, sys, os, re

src, pkg, name, out = sys.argv[1:5]
m = json.load(open(src))
els = {e["uuid"]: e for e in m["elements"]}
grp = {g["uuid"]: g for g in m["groups"]}
W, H = m["resolution"]["width"], m["resolution"]["height"]

def f(v):
    v = float(v)
    if abs(v) < 1e-6: v = 0.0
    s = ("%.4f" % v).rstrip("0")
    return (s + "0" if s.endswith(".") else s) + "F"

def rad(d):
    return f"{f(d)[:-1]}F * DEG" if abs(float(d)) > 1e-6 else "0.0F"

lines = []
USED = {}
def part(node, parent_var, parent_origin, depth):
    g = grp[node["uuid"]]; o = g["origin"]
    if parent_origin is None: off = (-o[0], 24 - o[1], o[2])
    else: off = (-(o[0] - parent_origin[0]), -(o[1] - parent_origin[1]), o[2] - parent_origin[2])
    r = g.get("rotation", [0, 0, 0])
    var = re.sub(r"\W", "_", g["name"])
    cubes, rotated = [], []
    for c in node["children"]:
        if isinstance(c, dict): continue
        e = els[c]
        (rotated if any(abs(x) > 1e-6 for x in e.get("rotation", [0, 0, 0])) else cubes).append(e)
    def box(e, piv):
        fr, to = e["from"], e["to"]
        x = piv[0] - to[0]; y = -(to[1] - piv[1]); z = fr[2] - piv[2]
        w, h, d = to[0] - fr[0], to[1] - fr[1], to[2] - fr[2]
        u, v = e.get("uv_offset", [0, 0])
        inf = e.get("inflate", 0) or 0
        mir = ".mirror()" if e.get("mirror_uv") else ""
        return f".texOffs({u}, {v}){mir}.addBox({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)}, new CubeDeformation({f(inf)})){'.mirror(false)' if mir else ''}"
    cl = "CubeListBuilder.create()" + "".join(box(e, o) for e in cubes)
    pose = (f"PartPose.offsetAndRotation({f(off[0])}, {f(off[1])}, {f(off[2])}, {rad(-r[0])}, {rad(-r[1])}, {rad(r[2])})"
            if any(abs(x) > 1e-6 for x in r) else f"PartPose.offset({f(off[0])}, {f(off[1])}, {f(off[2])})")
    lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{g['name']}\", {cl}, {pose});")
    for e in rotated:   # cubo girado sozinho -> vira uma parte filha com a rotacao
        eo, er = e["origin"], e["rotation"]
        eoff = (-(eo[0] - o[0]), -(eo[1] - o[1]), eo[2] - o[2])
        n = re.sub(r"\W", "_", e["name"]) + "_r"
        k = (var, n); USED[k] = USED.get(k, 0) + 1
        if USED[k] > 1: n += str(USED[k])   # nomes repetidos (varios "cube" girados): senao o jogo guarda so o ultimo
        lines.append(f"        {var}.addOrReplaceChild(\"{n}\", CubeListBuilder.create(){box(e, eo)}, "
                     f"PartPose.offsetAndRotation({f(eoff[0])}, {f(eoff[1])}, {f(eoff[2])}, {rad(-er[0])}, {rad(-er[1])}, {rad(er[2])}));")
    for c in node["children"]:
        if isinstance(c, dict): part(c, var, o, depth + 1)

for node in m["outliner"]:
    if isinstance(node, dict): part(node, "meshRoot", None, 0)

model = f"""package {pkg};

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de {os.path.basename(src)} -- nao edite a mao, edite no Blockbench e rode o script.
public final class {name}ModelData {{
    private {name}ModelData() {{}}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
{chr(10).join(lines)}
        return LayerDefinition.create(mesh, {W}, {H});
    }}
}}
"""

def const(n):
    return re.sub(r"\W", "_", n.split(".")[-1]).upper()

anims = []
lengths = []
for a in m["animations"]:
    cname = const(a["name"])
    b = [f"    public static final AnimationDefinition {cname} = AnimationDefinition.Builder.withLength({f(a['length'])})" + (".looping()" if a["loop"] == "loop" else "")]
    for uid, an in a["animators"].items():
        kfs = an.get("keyframes", [])
        for ch, target, fn in (("rotation", "ROTATION", "degreeVec"), ("position", "POSITION", "posVec"), ("scale", "SCALE", "scaleVec")):
            ks = sorted([k for k in kfs if k["channel"] == ch], key=lambda k: float(k["time"]))
            if not ks: continue
            items = []
            for k in ks:
                d = k["data_points"][0]; x, y, z = (float(d[c]) for c in "xyz")
                if ch == "rotation": x, y = -x, -y
                elif ch == "position": x = -x
                ip = "CATMULLROM" if k.get("interpolation") == "catmullrom" else "LINEAR"
                items.append(f"new Keyframe({f(k['time'])}, KeyframeAnimations.{fn}({f(x)}, {f(y)}, {f(z)}), AnimationChannel.Interpolations.{ip})")
            b.append(f"            .addAnimation(\"{an['name']}\", new AnimationChannel(AnimationChannel.Targets.{target},\n                " + ",\n                ".join(items) + "))")
    b[-1] += "\n            .build();"
    if len(b) == 1: b[0] += ".build();"
    anims.append("\n".join(b))
    lengths.append(f"        case \"{a['name'].split('.')[-1]}\" -> {int(round(float(a['length']) * 20))};")

animsrc = f"""package {pkg};

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de {os.path.basename(src)} -- nao edite a mao, edite no Blockbench e rode o script.
public final class {name}Animations {{
    private {name}Animations() {{}}

{chr(10).join(anims)}

    // duracao em ticks de cada animacao
    public static int lengthTicks(String anim) {{
        return switch (anim) {{
{chr(10).join(lengths)}
            default -> 20;
        }};
    }}
}}
"""
os.makedirs(out, exist_ok=True)
open(os.path.join(out, f"{name}ModelData.java"), "w").write(model)
open(os.path.join(out, f"{name}Animations.java"), "w").write(animsrc)
print("ok:", len(lines), "partes,", len(anims), "animacoes")

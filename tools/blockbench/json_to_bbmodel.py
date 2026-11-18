# Converte um modelo .json do jogo (bloco/item) de volta para .bbmodel (Blockbench, Java Block/Item) para editar.
# Uso: python3 tools/blockbench/json_to_bbmodel.py <modelo.json> <textura.png> <saida.bbmodel>
import json, sys, uuid, base64, os
from PIL import Image
src, png, out = sys.argv[1:4]
j = json.load(open(src)); img = Image.open(png)
res = img.size[0]; ts = j.get("texture_size", [16, 16]); k = ts[0] / 16
U = lambda: str(uuid.uuid4())
els = []
for n, e in enumerate(j["elements"]):
    rot = [0, 0, 0]; origin = [8, 8, 8]
    if "rotation" in e:
        rot["xyz".index(e["rotation"]["axis"])] = e["rotation"]["angle"]; origin = e["rotation"]["origin"]
    faces = {}
    for f in ("north", "east", "south", "west", "up", "down"):
        if f in e["faces"]:
            fc = e["faces"][f]; uv = fc.get("uv", [0, 0, 16, 16])
            faces[f] = {"uv": [c * k for c in uv], "texture": 0}
            if fc.get("rotation"): faces[f]["rotation"] = fc["rotation"]
        else: faces[f] = {"uv": [0, 0, 0, 0], "texture": None}
    els.append({"name": e.get("name", f"cubo_{n}"), "box_uv": False, "render_order": "default", "rescale": False, "locked": False, "shade_direction_override": "",
                "light_emission": 0, "export": True, "scope": 0, "allow_mirror_modeling": True, "from": e["from"], "to": e["to"], "autouv": 0, "color": n % 8,
                "rotation": rot, "origin": origin, "faces": faces, "type": "cube", "uuid": U()})
name = os.path.splitext(os.path.basename(src))[0]
m = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": j.get("parent", ""), "java_block_version": "1.21.1",
     "ambientocclusion": True, "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "", "multi_file_ruleset": "", "variable_placeholder_buttons": [],
     "unhandled_root_fields": {}, "resolution": {"width": ts[0], "height": ts[1]}, "elements": els, "groups": [], "outliner": [e["uuid"] for e in els],
     "textures": [{"name": os.path.basename(png), "path": "", "folder": "item", "namespace": "", "id": "0", "group": "", "scope": 0, "width": img.size[0], "height": img.size[1],
                   "uv_width": ts[0], "uv_height": ts[1], "particle": True, "use_as_default": False, "layers_enabled": False, "sync_to_project": "", "file_format": "png",
                   "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "force_translucent": False, "frame_time": 1,
                   "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": False, "uuid": U(),
                   "source": "data:image/png;base64," + base64.b64encode(open(png, "rb").read()).decode()}]}
if "display" in j: m["display"] = j["display"]
json.dump(m, open(out, "w")); print("ok", out, len(els), "cubos")

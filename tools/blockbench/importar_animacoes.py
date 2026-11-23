# Leva animacoes de um .bbmodel do Blockbench para o mod (o jogo le o JSON gerado em assets/.../combat_anims/).
#   python3 importar_animacoes.py combate_espada.bbmodel sword base_1 base_2 base_3
# Guarda so os ossos e canais (rotacao, posicao, escala) com os keyframes e as curvas (linear, catmullrom,
# bezier, step), do jeito que o Blockbench guarda. Os marcadores da linha do tempo viram o momento do acerto.
import json, sys, os

def num(v):
    try: return float(v)
    except Exception: return 0.0

INTERP = {'linear': 0, 'catmullrom': 1, 'bezier': 2, 'step': 3}

def main():
    src, weapon, names = sys.argv[1], sys.argv[2], sys.argv[3:]
    m = json.load(open(src))
    here = os.path.dirname(os.path.abspath(__file__))
    out_path = os.path.join(here, '..', '..', 'src', 'main', 'resources', 'assets', 'binosvanillaplus', 'combat_anims', weapon + '.json')
    out = json.load(open(out_path)) if os.path.exists(out_path) else {}
    for a in m['animations']:
        if names and a['name'] not in names: continue
        bones = {}
        for an in a['animators'].values():
            if an.get('type', 'bone') != 'bone': continue
            chans = {}
            for ch in ('rotation', 'position', 'scale'):
                ks = sorted([k for k in an['keyframes'] if k['channel'] == ch], key=lambda k: k['time'])
                if not ks: continue
                rows = []
                for k in ks:
                    d = k['data_points'][0]
                    lt = k.get('bezier_left_time', [0, 0, 0]); lv = k.get('bezier_left_value', [0, 0, 0])
                    rt = k.get('bezier_right_time', [0, 0, 0]); rv = k.get('bezier_right_value', [0, 0, 0])
                    rows.append([round(num(k['time']), 4), num(d['x']), num(d['y']), num(d['z']), INTERP.get(k.get('interpolation'), 0)]
                                + [round(num(x), 4) for x in lt + lv + rt + rv])
                chans[ch] = rows
            if chans: bones[an['name']] = chans
        markers = sorted(num(mk['time']) for mk in a.get('markers', []))
        hit = markers[0] if markers else a['length'] * 0.35
        out[a['name']] = {'length': a['length'], 'hits': markers or [hit],
                          'strike': [round(max(0, hit - 0.07), 4), round(hit + 0.04, 4)], 'bones': bones}
        print(a['name'], a['length'], 'acerto', hit, 'ossos', sorted(bones))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    json.dump(out, open(out_path, 'w'), separators=(',', ':'))
    print('->', os.path.normpath(out_path))

main()

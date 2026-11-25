#!/usr/bin/env python3
"""Gera os arquivos de idioma do mod Mining (todas as chaves de gemas, equipamentos, forja, lapidario).

Fonte: data/*.py (um dicionario por idioma). Saida: mining/src/main/resources/assets/binosmining/lang/<codigo>.json
(as descobertas do diario, discovery.*, ficam no Bino's Vanilla+ e nao sao geradas aqui)
Uso:  python3 tools/lang/mining_lang.py          (gera e valida)
O pt_br.json existente e' a referencia das chaves e dos %s; nao e' sobrescrito.
"""
import importlib, json, os, pkgutil, re, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LANG = os.path.join(ROOT, 'mining/src/main/resources/assets/binosmining/lang')
NS = 'binosmining'
GEMS = ['rubi', 'sapphire', 'jade']
PARTS = ['sword', 'pickaxe', 'shovel', 'axe', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots']
TOOLTIPS = ['forge_stone_bonus', 'forge_stone_first', 'forge_stone_use', 'forged', 'diamond_tool', 'rubi_sword',
            'rubi_pickaxe', 'rubi_axe', 'rubi_shovel', 'rubi_hoe', 'sapphire_tool', 'jade_tool', 'diamond_set',
            'rubi_set', 'sapphire_set', 'jade_set', 'set_header']
HINTS = ['take_flint', 'place_flint', 'take_ore', 'place_ore', 'press', 'working', 'wrong', 'leave', 'more', 'nothing']
SUBS = ['lapidary_spin', 'lapidary_ready', 'lapidary_done', 'lapidary_contact']
DISC = ['rubi_ore', 'sapphire_ore', 'jade_ore']

# campos de cada idioma:
#  gem:    {rubi, sapphire, jade, diamond}      nomes das gemas
#  of:     {rubi, sapphire, jade}               forma usada nas pecas "<peca> de <gema>" (se omitido = gem)
#  gn:     {rubi, sapphire, jade}               forma usada em minerio/bloco (se omitido = of)
#  noun:   lista de 9 (espada, picareta, pa, machado, enxada, capacete, peitoral, calcas, botas)
#  pat:    {gear, ore, deep, block}             moldes com {g} (gema) e {n} (peca)
#  raw:    {diamond, rubi, sapphire, jade}      "<gema> bruta"
#  unique: {forge_stone, ph_fragment, ph_incomplete, ph_small, ph_large, forge_ore, ph_block, table}
#  tip:    17 tooltips (chaves em TOOLTIPS)     sub: 4 legendas     hint: 10 dicas     disc: 3 descobertas

def load():
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'data'))
    data, copies = {}, {}
    for m in pkgutil.iter_modules([os.path.join(os.path.dirname(__file__), 'data')]):
        mod = importlib.import_module(m.name)
        data.update(getattr(mod, 'DATA', {}))
        copies.update(getattr(mod, 'COPY', {}))
    return data, copies

def build(d):
    out = {}
    of = d.get('of', d['gem'])      # forma da gema nas pecas (ex.: adjetivo)
    gn = d.get('gn', of)            # forma da gema em minerio/bloco
    for g in GEMS:
        out[f'item.{NS}.{g}'] = d['gem'][g]
        for i, p in enumerate(PARTS):
            out[f'item.{NS}.{g}_{p}'] = d['pat']['gear'].format(g=of[g], n=d['noun'][i])
    for g in GEMS:
        out[f'block.{NS}.{g}_ore'] = d['pat']['ore'].format(g=gn[g])
        out[f'block.{NS}.{g}_deepslate_ore'] = d['pat']['deep'].format(g=gn[g])
        out[f'block.{NS}.{g}_block'] = d['pat']['block'].format(g=gn[g])
    for g in ['diamond'] + GEMS:
        out[f'item.{NS}.raw_{g}'] = d['raw'][g]
    u = d['unique']
    for n in range(1, 11):
        out[f'item.{NS}.forge_stone_{n}'] = f"{u['forge_stone']} +{n}"
    out[f'item.{NS}.philosopher_fragment'] = u['ph_fragment']
    out[f'item.{NS}.philosopher_stone_incomplete'] = u['ph_incomplete']
    out[f'item.{NS}.philosopher_stone_small'] = u['ph_small']
    out[f'item.{NS}.philosopher_stone_large'] = u['ph_large']
    out[f'block.{NS}.forge_ore'] = u['forge_ore']
    out[f'block.{NS}.philosopher_block'] = u['ph_block']
    out[f'block.{NS}.lapidary_table'] = u['table']
    out[f'container.{NS}.lapidary_table'] = u['table']
    for t in TOOLTIPS:
        out[f'tooltip.{NS}.{t}'] = d['tip'][t]
    for g in ['diamond'] + GEMS:
        out[f'tooltip.{NS}.name_{g}'] = d['gem'][g]
    for s in SUBS:
        out[f'subtitles.{NS}.{s}'] = d['sub'][s]
    for h in HINTS:
        out[f'lapidary.{NS}.' + ('hint.' + h if h not in ('more', 'nothing') else h)] = d['hint'][h]
    out[f'creativetab.{NS}.mining'] = 'Mining'
    return out

def spec(s):  # assinatura dos formatadores: quantidade de %s e %%
    return (len(re.findall(r'%\d*\$?s', s)), s.count('%%'))

def main():
    ref = json.load(open(os.path.join(LANG, 'pt_br.json'), encoding='utf-8'))
    data, copies = load()
    problems = 0
    base_keys = None
    for code, d in sorted(data.items()):
        try:
            out = build(d)
        except Exception as e:
            print('ERRO', code, type(e).__name__, e); problems += 1; continue
        if base_keys is None: base_keys = set(out)
        miss = [k for k in out if k not in ref]
        if miss: print('chave fora do pt_br', code, miss[:3]); problems += 1
        for k, v in out.items():
            if not v.strip(): print('vazio', code, k); problems += 1
            if k in ref and spec(v) != spec(ref[k]):
                print('placeholder diferente', code, k, repr(v), 'ref', repr(ref[k])); problems += 1
        write(code, out)
    for code, src in sorted(copies.items()):
        if src in data: write(code, build(data[src]))
        else: print('copia sem origem', code, src); problems += 1
    print(f'{len(data)} idiomas + {len(copies)} copias; problemas: {problems}')
    return 1 if problems else 0

def write(code, out):
    path = os.path.join(LANG, code + '.json')
    merged = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {}
    merged.update(out)   # mantem chaves escritas a mao (ex.: cenas da bancada/bigorna)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(merged, f, ensure_ascii=False, indent=2)
        f.write('\n')

if __name__ == '__main__':
    sys.exit(main())

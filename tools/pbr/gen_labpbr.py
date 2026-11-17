#!/usr/bin/env python3
"""Gera os mapas labPBR (<textura>_s.png) dos minerios e blocos de gema do Mining.

labPBR 1.3, textura _s: R = suavidade, G = reflectancia F0 (0-229), B = porosidade/SSS, A = emissao (0-254; 255 = sem emissao).
Os valores espelham a intencao dos arquivos euphoria/*.glsl (cristais brilham, blocos de gema sao polidos), para o brilho
aparecer tambem em shaders que nao sao o Complementary com Euphoria Patches (BSL, Photon, Solas, Nostalgia...).

Uso: python3 tools/pbr/gen_labpbr.py   (a partir da raiz do repositorio)
"""
import os
import sys
from PIL import Image

TEX = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'binosvanillaplus', 'textures', 'block')

# cristais dos minerios (textura 32x32 so com os cristais): suavidade, F0, emissao base (0-1)
CRYSTALS = {
    'rubi_ore_crystal':     dict(smooth=0.60, f0=0.12, emit=0.95),   # rubi: brilho forte (rubi_ore.glsl: emission 1.5 a 3)
    'sapphire_ore_crystal': dict(smooth=0.30, f0=0.10, emit=0.50),
    'jade_ore_crystal':     dict(smooth=0.30, f0=0.08, emit=0.50),
    'forge_ore_crystal':    dict(smooth=0.30, f0=0.08, emit=0.50),
    'diamond_ore_crystal':  dict(smooth=0.30, f0=0.10, emit=0.50),
}
# blocos de gema: polidos, sem emissao
BLOCKS = {
    'rubi_block':        dict(smooth=0.85, f0=0.18),
    'sapphire_block':    dict(smooth=0.85, f0=0.18),
    'jade_block':        dict(smooth=0.85, f0=0.18),
    'philosopher_block': dict(smooth=0.90, f0=0.12),
}


def byte(x):
    return max(0, min(255, int(round(x * 255))))


def crystal(name, p):
    src = Image.open(os.path.join(TEX, name + '.png')).convert('RGBA')
    px = src.load()
    w, h = src.size
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    top = max((lum(px[x, y]) for y in range(h) for x in range(w) if px[x, y][3] > 0), default=255) or 255
    out = Image.new('RGBA', (w, h))
    o = out.load()
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] == 0:
                o[x, y] = (0, 0, 0, 255)   # area sem textura: sem emissao
                continue
            e = p['emit'] * (0.65 + 0.35 * lum(c) / top)   # os pixels mais escuros do cristal brilham um pouco menos
            o[x, y] = (byte(p['smooth']), min(229, byte(p['f0'])), 0, min(254, max(1, int(round(e * 254)))))
    return out


def block(name, p):
    src = Image.open(os.path.join(TEX, name + '.png'))
    out = Image.new('RGBA', src.size, (byte(p['smooth']), min(229, byte(p['f0'])), 0, 255))
    return out


def main():
    made = []
    for n, p in CRYSTALS.items():
        crystal(n, p).save(os.path.join(TEX, n + '_s.png'))
        made.append(n)
    for n, p in BLOCKS.items():
        block(n, p).save(os.path.join(TEX, n + '_s.png'))
        made.append(n)
    print('gerados:', ', '.join(m + '_s.png' for m in made))


if __name__ == '__main__':
    sys.exit(main())

# Sons PROVISORIOS do mod Combat, sintetizados (sem copiar nada): golpe no ar, cortando carne, batendo em coisa dura e
# na agua, para espada e machado. Gera .ogg mono em assets/binoscombat/sounds/combat/.
# Trocar pelos sons definitivos e' so substituir os arquivos (mesmo nome) ou editar sounds.json.
# Rodar da raiz do repo: python3 tools/sounds/gen_combat_sfx.py
import numpy as np, os, subprocess, tempfile, wave
from scipy.signal import butter, sosfilt

SR = 44100
OUT = "combat/src/main/resources/assets/binoscombat/sounds/combat"
rng = np.random.default_rng(7)

def t(sec): return np.arange(int(SR * sec)) / SR
def noise(sec): return rng.standard_normal(int(SR * sec))
def env(n, a, d, shape=2.0):
    x = np.linspace(0, 1, n); a = max(a, 1e-4)
    e = np.where(x < a, x / a, np.clip(1 - (x - a) / max(d, 1e-4), 0, 1) ** shape)
    return e
def bp(x, lo, hi, order=2): return sosfilt(butter(order, [lo, hi], btype="band", fs=SR, output="sos"), x)
def lp(x, f, order=2): return sosfilt(butter(order, f, btype="low", fs=SR, output="sos"), x)
def hp(x, f, order=2): return sosfilt(butter(order, f, btype="high", fs=SR, output="sos"), x)

def sweep_noise(sec, f0, f1, width=0.5, blocks=24):
    # ruido filtrado com a frequencia central variando (o "vush" do golpe)
    n = int(SR * sec); x = noise(sec); y = np.zeros(n); edges = np.linspace(0, n, blocks + 1).astype(int)
    for i in range(blocks):
        f = f0 * (f1 / f0) ** (i / (blocks - 1))
        seg = bp(x, f * (1 - width / 2), f * (1 + width / 2))
        w = np.hanning(edges[i + 1] - edges[i] + 400)[200:-200] if edges[i + 1] - edges[i] > 0 else None
        y[edges[i]:edges[i + 1]] += seg[edges[i]:edges[i + 1]]
    return y

def norm(x, peak=0.9): return x / (np.max(np.abs(x)) + 1e-9) * peak

def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    x = norm(x)
    fade = int(SR * 0.01); x[-fade:] *= np.linspace(1, 0, fade)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f: tmp = f.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp, "-c:a", "libvorbis", "-q:a", "5", f"{OUT}/{name}.ogg"], check=True)
    os.remove(tmp); print("ok", name)

def swing(sec, f0, f1, width, peak_at):
    n = int(SR * sec)
    y = sweep_noise(sec, f0, f1, width) * env(n, peak_at, 1 - peak_at, 2.5)
    return y

def thud(sec, f, decay):
    tt = t(sec); return np.sin(2 * np.pi * f * tt * (1 - 0.3 * tt / sec)) * np.exp(-tt / decay)

def flesh(sec, heavy):
    n = int(SR * sec); tt = t(sec)
    slap = hp(noise(sec), 1500) * np.exp(-tt / 0.012)                     # estalo do contato
    body = thud(sec, 70 if heavy else 95, 0.07 if heavy else 0.05) * (1.0 if heavy else 0.7)
    wet = bp(noise(sec), 400, 2200) * np.exp(-tt / (0.09 if heavy else 0.06)) * (0.6 + 0.4 * np.sin(2 * np.pi * 38 * tt))
    return slap * 0.8 + body + wet * 0.9

def clank(sec, partials, decay, click=1.0):
    tt = t(sec); y = np.zeros_like(tt)
    for f, a in partials: y += a * np.sin(2 * np.pi * f * tt + rng.uniform(0, 6)) * np.exp(-tt / (decay * (1200 / f) ** 0.5))
    return y + click * hp(noise(sec), 2500) * np.exp(-tt / 0.006)

def splash(sec):
    tt = t(sec); n = len(tt)
    y = lp(noise(sec), 3500) * env(n, 0.03, 0.97, 1.6)
    for _ in range(9):   # bolhinhas
        st = rng.uniform(0.02, sec * 0.6); f = rng.uniform(500, 1300); d = 0.02
        m = tt > st; tb = tt[m] - st
        y[m] += 0.35 * np.sin(2 * np.pi * f * tb * (1 + 3 * tb)) * np.exp(-tb / d)
    return y

for i in range(3):   # variacoes (o jogo sorteia uma)
    k = 1 + (i - 1) * 0.08
    save(f"sword_swing{i+1}", swing(0.26, 900 * k, 3200 * k, 0.6, 0.45))
    save(f"axe_swing{i+1}", swing(0.34, 380 * k, 1300 * k, 0.7, 0.55) + 0.2 * swing(0.34, 120, 300, 0.8, 0.55))
    save(f"sword_hit{i+1}", flesh(0.28, False) + 0.25 * np.pad(swing(0.12, 2500 * k, 4000 * k, 0.5, 0.2), (0, int(SR * 0.16))))
    save(f"axe_hit{i+1}", flesh(0.34, True) * 1.0)
    save(f"sword_hit_hard{i+1}", clank(0.6, [(1530 * k, 1.0), (2810 * k, 0.6), (4170 * k, 0.45), (6230 * k, 0.25)], 0.12))
    save(f"axe_hit_hard{i+1}", clank(0.45, [(760 * k, 1.0), (1390 * k, 0.5), (2380 * k, 0.3)], 0.05, 1.5) + thud(0.45, 140, 0.04))
    save(f"hit_water{i+1}", splash(0.5 + 0.05 * i))

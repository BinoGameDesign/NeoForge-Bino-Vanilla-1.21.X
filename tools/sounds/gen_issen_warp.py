# Som do teleporte do Issen em cadeia (sintetizado, original): um "shun" agudo e rapido, no estilo dos teleportes de
# anime (sobe de tom num piscar, com brilho metalico e um baque grave curto no fim).
# Rodar da raiz do repo: python3 tools/sounds/gen_issen_warp.py  ->  assets/binosvanillaplus/sounds/combat/issen_warp.ogg
import numpy as np, os, subprocess, tempfile, wave
from scipy.signal import butter, sosfilt

SR = 44100
OUT = "src/main/resources/assets/binosvanillaplus/sounds/combat"
rng = np.random.default_rng(11)

def bp(x, lo, hi): return sosfilt(butter(2, [lo, hi], btype="band", fs=SR, output="sos"), x)
def hp(x, f): return sosfilt(butter(2, f, btype="high", fs=SR, output="sos"), x)

def chirp(sec, f0, f1, curve=3.0):
    t = np.arange(int(SR * sec)) / SR
    f = f0 + (f1 - f0) * (t / sec) ** curve           # sobe devagar e dispara no fim
    return np.sin(2 * np.pi * np.cumsum(f) / SR)

def warp():
    sec = 0.42; n = int(SR * sec); t = np.arange(n) / SR
    y = np.zeros(n)
    # 1) "sh" de ar que cresce (ruido brilhante, envelope ao contrario)
    k = int(SR * 0.09)
    sh = hp(rng.standard_normal(k), 3500) * np.linspace(0, 1, k) ** 3
    y[:k] += 0.3 * sh
    # 2) o "shun": dois tons que sobem juntos (quinta), com tremido rapido
    k2 = int(SR * 0.16); a = int(SR * 0.07)
    tone = chirp(0.16, 700, 3400) + 0.6 * chirp(0.16, 1050, 5100)
    trem = 0.75 + 0.25 * np.sin(2 * np.pi * 38 * np.arange(k2) / SR)
    e = np.minimum(1, np.arange(k2) / (0.03 * SR)) * np.exp(-np.maximum(0, np.arange(k2) - 0.10 * SR) / (0.02 * SR))
    y[a:a + k2] += 0.5 * tone * trem * e
    # 3) brilho metalico que fica soando (sinos agudos)
    b = int(SR * 0.17); m = n - b; tt = np.arange(m) / SR
    bell = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt / d) for f, d in ((2637, 0.09), (3951, 0.06), (5274, 0.04)))
    y[b:] += 0.22 * bell
    # 4) baque grave curto (o corpo aparecendo)
    tt = np.arange(int(SR * 0.12)) / SR
    thump = np.sin(2 * np.pi * np.cumsum(220 - 1400 * tt) / SR) * np.exp(-tt / 0.03)
    y[b:b + len(thump)] += 0.6 * thump
    return y

def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.9
    f = int(SR * 0.01); x[-f:] *= np.linspace(1, 0, f)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as fh: tmp = fh.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp, "-c:a", "libvorbis", "-q:a", "5", f"{OUT}/{name}.ogg"], check=True)
    os.remove(tmp); print("ok", name)

save("issen_warp", warp())

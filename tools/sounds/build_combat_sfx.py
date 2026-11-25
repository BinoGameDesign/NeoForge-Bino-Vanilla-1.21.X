# Monta os sons do mod Combat a partir dos sons CC0 em tools/sounds/src (Kenney RPG Audio e StarNinjas no OpenGameArt):
# corta silencio, ajusta tom, soma uma "pancada" grave nos acertos (para ter corpo) e grava .ogg mono 44,1 kHz em
# assets/binoscombat/sounds/combat/ com os nomes que o sounds.json usa. A agua continua a sintetizada
# (gen_combat_sfx.py), porque os pacotes nao tem som de agua.
# Rodar da raiz do repo: python3 tools/sounds/build_combat_sfx.py
import numpy as np, subprocess, os, tempfile, wave
SR = 44100
SRC = "tools/sounds/src"
OUT = "combat/src/main/resources/assets/binoscombat/sounds/combat"

def load(name):
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", f"{SRC}/{name}.ogg", "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.float32).astype(float)

def trim(x, lead=0.004, tail=0.10, thr=0.02):
    e = np.abs(x); idx = np.where(e > e.max() * thr)[0]
    a = max(0, idx[0] - int(lead * SR)); b = min(len(x), idx[-1] + int(tail * SR))
    return x[a:b]

def from_peak(x, pre=0.07):   # o "vush" chega no pico logo depois do clique (o som sai na hora do golpe)
    pk = int(np.argmax(np.abs(np.convolve(x, np.ones(256) / 256, 'same'))))
    a = max(0, pk - int(pre * SR)); y = x[a:].copy(); f = int(0.01 * SR); y[:f] *= np.linspace(0, 1, f); return y

def pitch(x, k):   # k < 1 = mais grave (e mais longo)
    n = int(len(x) / k); return np.interp(np.arange(n) * k, np.arange(len(x)), x)

def thud(sec, f, decay, amp):
    t = np.arange(int(SR * sec)) / SR
    return amp * np.sin(2 * np.pi * f * t * (1 - 0.35 * t / sec)) * np.exp(-t / decay)

def mix(*parts):
    n = max(len(p) for p in parts); y = np.zeros(n)
    for p in parts: y[:len(p)] += p
    return y

def norm(x, peak): return x / (np.max(np.abs(x)) + 1e-9) * peak

def save(name, x, peak=0.9):
    x = norm(x, peak); f = int(SR * 0.012); x[-f:] *= np.linspace(1, 0, f)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as fh: tmp = fh.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp, "-c:a", "libvorbis", "-q:a", "6", f"{OUT}/{name}.ogg"], check=True)
    os.remove(tmp); print("ok", name, f"{len(x)/SR:.2f}s")

S = {n: trim(load(n)) for n in ["sword.1", "sword.2", "sword.5", "sword.6", "sword.8", "sword_clash.2", "sword_clash.4",
                                "sword_clash.5", "sword_clash.8", "chop", "knifeSlice", "knifeSlice2"]}

# golpe no ar: espada = os "vush" do StarNinjas; machado = os mesmos mais graves e pesados
for i, n in enumerate(["sword.1", "sword.5", "sword.8"]):
    save(f"sword_swing{i+1}", from_peak(S[n]), 0.75)
    save(f"axe_swing{i+1}", from_peak(pitch(S[n], 0.68), 0.09), 0.8)
# acerto: corte (Kenney) + pancada grave
save("sword_hit1", mix(S["knifeSlice"], thud(0.25, 95, 0.05, 0.55)))
save("sword_hit2", mix(S["knifeSlice2"], thud(0.25, 90, 0.05, 0.55)))
save("sword_hit3", mix(S["sword.6"] * 0.8, thud(0.25, 100, 0.045, 0.6)))
save("axe_hit1", mix(S["chop"], pitch(S["knifeSlice2"], 0.8) * 0.6, thud(0.3, 70, 0.07, 0.8)))
save("axe_hit2", mix(pitch(S["chop"], 0.9), thud(0.3, 65, 0.08, 0.8)))
save("axe_hit3", mix(pitch(S["chop"], 1.08), pitch(S["knifeSlice"], 0.78) * 0.6, thud(0.3, 75, 0.06, 0.8)))
# coisa dura: espada = choque de lamina; machado = pancada seca com o choque mais grave por baixo
for i, n in enumerate(["sword_clash.2", "sword_clash.4", "sword_clash.8"]):
    save(f"sword_hit_hard{i+1}", S[n], 0.85)
save("axe_hit_hard1", mix(S["chop"], pitch(S["sword_clash.4"], 0.66) * 0.55, thud(0.3, 120, 0.04, 0.5)))
save("axe_hit_hard2", mix(pitch(S["chop"], 0.92), pitch(S["sword_clash.5"], 0.62) * 0.5))
save("axe_hit_hard3", mix(S["chop"], pitch(S["sword_clash.2"], 0.7) * 0.45, thud(0.25, 110, 0.035, 0.5)))

# parry (20/11): choque de lamina + "ting" metalico agudo; o perfeito tem pancada grave e eco (o som mais marcante)
def ting(sec, f, decay, amp):
    t = np.arange(int(SR * sec)) / SR
    return amp * (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * 2.76 * f * t) + 0.22 * np.sin(2 * np.pi * 5.4 * f * t)) * np.exp(-t / decay)

def echo(x, delay, fb, n):
    d = int(delay * SR); y = np.zeros(len(x) + d * n); y[:len(x)] += x
    for k in range(1, n + 1): y[k * d:k * d + len(x)] += x * (fb ** k)
    return y

save("parry1", mix(S["sword_clash.5"], ting(0.6, 2200, 0.10, 0.30)), 0.9)
save("parry2", mix(pitch(S["sword_clash.4"], 1.08), ting(0.6, 2500, 0.10, 0.30)), 0.9)
save("parry_perfect1", echo(mix(S["sword_clash.8"], ting(1.2, 3100, 0.32, 0.55), thud(0.3, 90, 0.05, 0.5)), 0.11, 0.38, 4), 0.95)
save("parry_perfect2", echo(mix(pitch(S["sword_clash.2"], 1.1), ting(1.2, 2800, 0.32, 0.55), thud(0.3, 85, 0.05, 0.5)), 0.11, 0.38, 4), 0.95)

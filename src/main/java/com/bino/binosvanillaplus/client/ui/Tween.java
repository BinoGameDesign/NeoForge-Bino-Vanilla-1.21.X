package com.bino.binosvanillaplus.client.ui;

// Valor que "persegue" um alvo de forma suave (aproximacao exponencial, independente do FPS).
// Ex.: hover de botao, destaque que desliza entre linhas, rolagem suave.
public final class Tween {
    private float value, target;
    private final float tauMs;      // menor = mais rapido (tempo para chegar ~63% do caminho)
    private long last = -1;

    public Tween(float initial, float tauMs) {
        this.value = this.target = initial;
        this.tauMs = tauMs;
    }

    public Tween to(float t) { target = t; return this; }

    public Tween snap(float t) { value = target = t; last = Ease.now(); return this; }

    public float target() { return target; }

    public float get() {
        long now = Ease.now();
        if (last < 0) last = now;
        float dt = Math.min(250f, now - last);
        last = now;
        if (dt > 0) value += (target - value) * (1f - (float) Math.exp(-dt / tauMs));
        if (Math.abs(target - value) < 0.0005f) value = target;
        return value;
    }

    // atalho: anda para o alvo e devolve o valor
    public float get(float newTarget) { target = newTarget; return get(); }
}

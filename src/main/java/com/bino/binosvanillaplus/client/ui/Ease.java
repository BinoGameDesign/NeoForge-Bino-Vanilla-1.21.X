package com.bino.binosvanillaplus.client.ui;

import net.minecraft.Util;

// Curvas de animacao (0..1 -> 0..1) e relogio em milissegundos.
public final class Ease {
    private Ease() {}

    public static long now() { return Util.getMillis(); }

    public static float clamp(float t) { return t < 0f ? 0f : (t > 1f ? 1f : t); }

    // progresso linear de uma animacao que comecou em 'start' e dura 'ms'
    public static float since(long start, int ms) { return clamp((now() - start) / (float) ms); }

    public static float outCubic(float t) { t = clamp(t); float u = 1f - t; return 1f - u * u * u; }

    public static float inCubic(float t) { t = clamp(t); return t * t * t; }

    public static float inOutCubic(float t) { t = clamp(t); return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f; }

    // passa um pouquinho do alvo e volta (efeito "pop")
    public static float outBack(float t) {
        t = clamp(t);
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    // mistura duas cores ARGB
    public static int mix(int a, int b, float t) {
        t = clamp(t);
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    // multiplica o alpha de uma cor ARGB
    public static int alpha(int argb, float a) {
        int al = (int) ((argb >>> 24) * clamp(a));
        return (al << 24) | (argb & 0xFFFFFF);
    }
}

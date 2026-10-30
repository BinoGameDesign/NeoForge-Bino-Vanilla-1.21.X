package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// Posicao do sol (ou da lua, a noite) a partir da hora do Minecraft. Usada na iluminacao do shader e nas sombras do minimapa.
public class Sun {
    public static float dx = 0.45f, dy = 0.85f, dz = 0.30f;   // direcao PARA o sol (normalizada)
    public static float day = 1f;                              // 1 = dia, ~0.55 = noite
    public static boolean shadows = false;                     // so ha sombra com o sol bem acima do horizonte

    public static void update(Minecraft mc) {
        ClientLevel l = mc.level;
        if (l == null) return;
        if (l.dimensionType().hasFixedTime() || !l.dimensionType().hasSkyLight()) {
            set(0.45f, 0.85f, 0.30f);
            day = 1f;
            shadows = false;
            return;
        }
        float a = l.getTimeOfDay(1.0f);                  // 0 = meio-dia, 0.25 = por do sol, 0.5 = meia-noite, 0.75 = nascer do sol
        double ang = a * Math.PI * 2.0;
        float sx = (float) -Math.sin(ang), sy = (float) Math.cos(ang);   // sol: leste (+x) de manha, oeste (-x) a tarde
        if (sy >= 0) set(sx, sy, 0.25f);
        else set(-sx, -sy, 0.25f);                       // noite: luz vem da lua (lado oposto)
        float t = Math.max(0f, Math.min(1f, (sy + 0.15f) / 0.30f));
        day = 0.55f + 0.45f * t * t * (3f - 2f * t);
        shadows = sy > 0.14f && l.getRainLevel(1.0f) < 0.5f;
    }

    private static void set(float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        dx = x / len; dy = y / len; dz = z / len;
    }

    // envia a direcao e a luz do dia para o shader
    static void applyUniforms() {
        if (DioramaRenderer.shader == null) return;
        DioramaRenderer.shader.safeGetUniform("SunDir").set(dx, dy, dz);
        DioramaRenderer.shader.safeGetUniform("DayLight").set(day);
    }
}

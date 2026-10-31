package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// Sol e lua a partir da hora do Minecraft (mesmo caminho do jogo: nasce no leste +X, passa pelo alto, se poe no oeste -X).
//  - (vx,vy,vz): direcao do SOL de verdade (para desenhar o sol/lua no ceu; a lua fica no lado oposto)
//  - (dx,dy,dz): direcao da LUZ principal (sol de dia, lua de noite), com uma leve inclinacao para as sombras nao ficarem
//    alinhadas com os blocos
public class Sun {
    public static float vx = 0f, vy = 1f, vz = 0f;            // sol visivel
    public static float dx = 0.45f, dy = 0.85f, dz = 0.30f;   // luz principal (normalizada)
    public static float day = 1f;                              // 1 = dia, ~0.55 = noite
    public static float night = 0f;                            // 0 = dia, 1 = noite fechada
    public static int moonPhase = 0;                           // 0 = cheia ... 4 = nova
    public static boolean shadows = false;                     // sombras do minimapa (sol/lua alto e sem chuva)
    public static boolean hasSky = true;

    public static void update(Minecraft mc) {
        ClientLevel l = mc.level;
        if (l == null) return;
        if (l.dimensionType().hasFixedTime() || !l.dimensionType().hasSkyLight()) {
            hasSky = false;
            vx = 0f; vy = 1f; vz = 0f;
            set(0.45f, 0.85f, 0.30f);
            day = 1f;
            night = 0f;
            shadows = false;
            return;
        }
        hasSky = true;
        float a = l.getTimeOfDay(mc.getTimer().getGameTimeDeltaPartialTick(false));   // 0 = meio-dia, 0.25 = por do sol, 0.5 = meia-noite
        double ang = a * Math.PI * 2.0;
        vx = (float) -Math.sin(ang);
        vy = (float) Math.cos(ang);
        vz = 0f;
        if (vy >= -0.05f) set(vx, Math.max(vy, 0.05f), 0.12f);
        else set(-vx, -vy, -0.12f);                      // noite: luz vem da lua (lado oposto)
        float t = Math.max(0f, Math.min(1f, (vy + 0.15f) / 0.30f));
        float s = t * t * (3f - 2f * t);
        day = 0.55f + 0.45f * s;
        night = 1f - s;
        moonPhase = l.getMoonPhase();
        boolean high = night < 0.5f ? vy > 0.14f : -vy > 0.25f;
        shadows = high && l.getRainLevel(1.0f) < 0.5f;
    }

    private static void set(float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        dx = x / len; dy = y / len; dz = z / len;
    }

    // envia a direcao e a luz do dia para o shader do terreno
    static void applyUniforms() {
        if (DioramaRenderer.shader == null) return;
        DioramaRenderer.shader.safeGetUniform("SunDir").set(dx, dy, dz);
        DioramaRenderer.shader.safeGetUniform("SunVis").set(vx, vy, vz);
        DioramaRenderer.shader.safeGetUniform("DayLight").set(day);
        DioramaRenderer.shader.safeGetUniform("Night").set(night);
    }
}

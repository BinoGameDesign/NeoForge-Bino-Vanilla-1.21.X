package com.bino.binosvanillaplus.client.atmosphere;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

// Estado da neblina em volta do jogador (escrito do zero).
//  - a cada 4 ticks amostra os biomas numa grade 3x3 em volta (centro pesa mais) -> mistura das aparencias
//  - detecta caverna pela luz do ceu + profundidade abaixo da superficie
//  - tudo vai mudando devagar (nada de troca seca ao cruzar a borda de um bioma ou entrar numa caverna)
//  - compose(): junta com hora do dia, por/nascer do sol, nevoa da manha, chuva e trovoada -> valores finais
public final class AtmosphereState {
    private AtmosphereState() {}

    // componentes misturaveis de uma aparencia
    static final int DAY = 0, NIGHT = 3, MIST = 6, SKY = 9, SKY_S = 12, HAZE = 13, MIST_D = 14, MIST_H = 15,
            BASE = 16, START = 17, N = 18;

    static final float[] surfT = new float[N], surfC = new float[N];
    static final float[] caveT = new float[N], caveC = new float[N];
    static float caveFT = 0f, caveF = 0f;
    static boolean dimMode = false;      // Nether/End: aparencia direto do bioma, sem caverna nem hora
    private static boolean valid = false;
    private static ClientLevel lastLevel;
    private static double lastX, lastZ;
    private static float ground = 63f;
    private static int tick = 0;

    // ---------------------------------------------------------------- saida de compose() (valores finais)
    public static final float[] fog = {0.62f, 0.78f, 0.94f};        // nevoa alta / cor da neblina do jogo
    public static final float[] mist = {0.85f, 0.9f, 0.95f};        // nevoa baixa
    public static final float[] sunGlow = {0f, 0f, 0f};             // brilho do sol atravessando a neblina
    public static final float[] sunDir = {0f, 1f, 0f};
    public static float haze = 0.0016f, mistDensity = 0.0035f, mistHeight = 9f, mistBase = 63f, start = 4f;
    public static float cave = 0f;                                   // 0 ao ar livre .. 1 dentro de caverna
    public static boolean hasCeiling = false;
    // superficie (sem caverna, sem direcao do olhar): usada pelo minimapa e pelo mapa-mundi
    public static final float[] surfaceFog = {0.62f, 0.78f, 0.94f};
    public static float surfaceDensity = 1f;
    public static float night = 0f;                                   // 0 dia .. 1 noite (para os mapas clarearem)

    public static boolean valid() { return valid; }

    // ---------------------------------------------------------------- tick
    public static void tick(Minecraft mc) {
        ClientLevel lvl = mc.level;
        if (lvl == null || mc.player == null) { valid = false; lastLevel = null; return; }
        // Dimensao Portatil: ambiente todo branco, sem neblina por bioma
        if (lvl.dimension().equals(com.bino.binosvanillaplus.pocket.PocketDimension.LEVEL)) { valid = false; lastLevel = lvl; return; }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        if (!mc.gameRenderer.getMainCamera().isInitialized()) cam = mc.player.getEyePosition();
        boolean snap = !valid || lvl != lastLevel || Math.abs(cam.x - lastX) > 48 || Math.abs(cam.z - lastZ) > 48;
        if (lvl != lastLevel) FogPalette.clearCache();
        if (snap || (tick++ & 3) == 0) sample(lvl, cam, snap);
        lastLevel = lvl;
        lastX = cam.x;
        lastZ = cam.z;
        if (snap) {
            System.arraycopy(surfT, 0, surfC, 0, N);
            System.arraycopy(caveT, 0, caveC, 0, N);
            caveF = caveFT;
            valid = true;
            return;
        }
        // ~2.2 s para cores/densidades, ~1.4 s para entrar/sair de caverna
        float k = 1f - (float) Math.exp(-0.05 / 2.2);
        for (int i = 0; i < N; i++) {
            surfC[i] += (surfT[i] - surfC[i]) * k;
            caveC[i] += (caveT[i] - caveC[i]) * k * 1.6f;
        }
        caveF += (caveFT - caveF) * (1f - (float) Math.exp(-0.05 / 1.4));
    }

    private static void sample(ClientLevel lvl, Vec3 cam, boolean snap) {
        hasCeiling = lvl.dimensionType().hasCeiling();
        dimMode = hasCeiling || !lvl.dimensionType().hasSkyLight();
        BlockPos eye = BlockPos.containing(cam);

        if (!dimMode) {
            float g = groundLevel(lvl, cam, eye.getY());
            ground = snap ? g : ground + (g - ground) * 0.25f;
        }

        // superficie: grade 3x3 a cada 12 blocos
        float[] acc = new float[N];
        float wSum = 0f, skyW = 0f;
        float[] sky = new float[3];
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = eye.getX() + dx * 12, z = eye.getZ() + dz * 12;
                int y = eye.getY();
                if (!dimMode && lvl.hasChunk(x >> 4, z >> 4)) {
                    y = Math.max(lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), lvl.getSeaLevel()) + 1;
                }
                Holder<Biome> b = lvl.getBiome(new BlockPos(x, y, z));
                FogLook l = dimMode ? FogPalette.dimension(b) : FogPalette.surface(b);
                float w = dx == 0 && dz == 0 ? 4f : (dx == 0 || dz == 0 ? 1.5f : 1f);
                add(acc, l, w, l.absBase ? l.mistBase : ground + l.mistBase);
                wSum += w;
                if (l.sky >= 0) {
                    sky[0] += r(l.sky) * w * l.skyStrength;
                    sky[1] += g(l.sky) * w * l.skyStrength;
                    sky[2] += b(l.sky) * w * l.skyStrength;
                    skyW += w * l.skyStrength;
                }
            }
        }
        for (int i = 0; i < N; i++) surfT[i] = acc[i] / wSum;
        if (skyW > 0f) {
            surfT[SKY] = sky[0] / skyW; surfT[SKY + 1] = sky[1] / skyW; surfT[SKY + 2] = sky[2] / skyW;
            surfT[SKY_S] = skyW / wSum;
        } else {
            surfT[SKY] = surfT[DAY]; surfT[SKY + 1] = surfT[DAY + 1]; surfT[SKY + 2] = surfT[DAY + 2];
            surfT[SKY_S] = 0f;
        }

        // caverna
        if (dimMode) {
            caveFT = 0f;
            System.arraycopy(surfT, 0, caveT, 0, N);
            return;
        }
        FogLook c = FogPalette.cave(lvl.getBiome(eye), cam.y);
        Arrays.fill(acc, 0f);
        add(acc, c, 1f, (float) cam.y - 3f);   // nevoa baixa das cavernas acompanha o chao perto do jogador
        System.arraycopy(acc, 0, caveT, 0, N);
        caveT[SKY_S] = 0f;
        caveFT = caveFactor(lvl, eye);
    }

    // nivel do chao em volta (media da metade mais baixa de uma grade 5x5, para arvores nao contarem)
    private static float groundLevel(ClientLevel lvl, Vec3 cam, int fallback) {
        float[] hs = new float[25];
        int n = 0;
        int cx = (int) Math.floor(cam.x), cz = (int) Math.floor(cam.z);
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int x = cx + dx * 12, z = cz + dz * 12;
                if (!lvl.hasChunk(x >> 4, z >> 4)) continue;
                hs[n++] = Math.max(lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), lvl.getSeaLevel());
            }
        }
        if (n == 0) return Math.min(fallback, ground);
        Arrays.sort(hs, 0, n);
        int m = Math.max(1, (int) Math.ceil(n * 0.6));
        float s = 0f;
        for (int i = 0; i < m; i++) s += hs[i];
        return s / m;
    }

    // 0 ao ar livre .. 1 em caverna: pouca luz do ceu E bem abaixo da superficie (casa com teto nao vira caverna)
    private static float caveFactor(ClientLevel lvl, BlockPos eye) {
        int[][] off = {{0, 0, 0}, {3, 0, 0}, {-3, 0, 0}, {0, 0, 3}, {0, 0, -3}, {0, 2, 0}};
        float sum = 0f;
        for (int[] o : off) sum += lvl.getBrightness(LightLayer.SKY, eye.offset(o[0], o[1], o[2]));
        float avg = sum / off.length;
        float raw = 1f - smooth(3f, 11f, avg);
        if (raw <= 0f) return 0f;
        int surf = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, eye.getX(), eye.getZ());
        float deep = clamp((surf - eye.getY() - 2f) / 10f);
        return raw * (0.3f + 0.7f * deep);
    }

    private static void add(float[] acc, FogLook l, float w, float baseAbs) {
        acc[DAY] += r(l.day) * w; acc[DAY + 1] += g(l.day) * w; acc[DAY + 2] += b(l.day) * w;
        acc[NIGHT] += r(l.night) * w; acc[NIGHT + 1] += g(l.night) * w; acc[NIGHT + 2] += b(l.night) * w;
        acc[MIST] += r(l.mist) * w; acc[MIST + 1] += g(l.mist) * w; acc[MIST + 2] += b(l.mist) * w;
        acc[HAZE] += l.haze * w;
        acc[MIST_D] += l.mistDensity * w;
        acc[MIST_H] += l.mistHeight * w;
        acc[BASE] += baseAbs * w;
        acc[START] += l.start * w;
    }

    // ---------------------------------------------------------------- composicao final (por quadro)

    private static long lastCompose = 0;

    // lookX/lookZ: para onde a camera olha (o por do sol so esquenta a neblina do lado do sol); NaN = sem direcao
    public static void compose(Minecraft mc, float pt, float lookX, float lookZ) {
        ClientLevel lvl = mc.level;
        if (lvl == null || !valid) return;
        lastCompose = net.minecraft.Util.getMillis();

        float dayF = 1f, twil = 0f, morning = 0f, sunX = 0f, sunY = 1f;
        boolean timed = !lvl.dimensionType().hasFixedTime() && lvl.dimensionType().hasSkyLight();
        if (timed) {
            double ang = lvl.getTimeOfDay(pt) * Math.PI * 2.0;   // 0 = meio-dia
            sunX = (float) -Math.sin(ang);
            sunY = (float) Math.cos(ang);
            dayF = smooth(-0.12f, 0.22f, sunY);
            twil = 1f - smooth(0f, 0.32f, Math.abs(sunY - 0.02f));
            morning = sunX > 0f ? smooth(-0.15f, 0.05f, sunY) * (1f - smooth(0.25f, 0.55f, sunY)) : 0f;
        }
        sunDir[0] = sunX; sunDir[1] = sunY; sunDir[2] = 0f;
        night = timed ? 1f - dayF : 0f;

        float rain = timed ? lvl.getRainLevel(pt) : 0f, thunder = timed ? lvl.getThunderLevel(pt) : 0f;
        float precip = 1f;
        boolean snow = false;
        if (rain > 0.001f && mc.player != null) {
            Biome.Precipitation p = lvl.getBiome(mc.player.blockPosition()).value().getPrecipitationAt(mc.player.blockPosition());
            precip = p == Biome.Precipitation.NONE ? 0.3f : 1f;
            snow = p == Biome.Precipitation.SNOW;
        }

        float dirF = 0.6f;
        if (!Float.isNaN(lookX) && timed) {
            float len = (float) Math.sqrt(lookX * lookX + lookZ * lookZ);
            float d = len < 1e-4f ? 0f : lookX / len * Math.signum(sunX);
            dirF = 0.35f + 0.65f * Math.max(0f, d);
        }

        // superficie com hora/clima
        float[] sf = new float[3], sm = new float[3];
        surfaceColors(sf, sm, dayF, twil, dirF, rain * precip, thunder, snow);
        // a mesma coisa sem direcao do olhar, para os mapas
        float[] mf = new float[3], mm = new float[3];
        surfaceColors(mf, mm, dayF, twil, 0.6f, rain * precip, thunder, snow);
        surfaceFog[0] = mf[0]; surfaceFog[1] = mf[1]; surfaceFog[2] = mf[2];

        float wetHaze = 1f + rain * precip * 1.4f + thunder * 0.6f;
        float sHaze = surfC[HAZE] * wetHaze;
        float sMistD = surfC[MIST_D] * (1f + rain * precip * 0.8f) * (1f + 1.6f * morning + 0.5f * (1f - dayF));
        float sMistH = surfC[MIST_H] * (1f - 0.25f * morning);
        surfaceDensity = (float) Math.sqrt(Math.max(0.36f, Math.min(4f, sHaze / 0.0016f)));

        // brilho do sol na neblina: amarelado de dia, alaranjado no por do sol, azul fraquinho da lua
        float[] glow = new float[3];
        float dayGlow = dayF * (1f - twil) * 0.55f * (1f - rain * 0.8f);
        float twGlow = twil * 0.85f * (1f - rain * 0.7f);
        float moon = (1f - dayF) * 0.10f * (1f - rain);
        glow[0] = 1.0f * dayGlow + 1.0f * twGlow + 0.35f * moon;
        glow[1] = 0.92f * dayGlow + 0.55f * twGlow + 0.42f * moon;
        glow[2] = 0.75f * dayGlow + 0.30f * twGlow + 0.60f * moon;

        float c = cave = caveF;
        for (int i = 0; i < 3; i++) {
            fog[i] = lerp(sf[i], caveC[DAY + i], c);
            mist[i] = lerp(sm[i], caveC[MIST + i], c);
            sunGlow[i] = glow[i] * (1f - c) * (hasCeiling ? 0f : 1f);
        }
        haze = lerp(sHaze, caveC[HAZE], c);
        mistDensity = lerp(sMistD, caveC[MIST_D], c);
        mistHeight = Math.max(1f, lerp(sMistH, caveC[MIST_H], c));
        mistBase = lerp(surfC[BASE], caveC[BASE], c);
        start = lerp(surfC[START], caveC[START], c);
        if (!timed) sunDir[1] = -1f;   // Nether/End: sem sol
        else if (sunY < -0.1f) { sunDir[0] = -sunX; sunDir[1] = -sunY; }   // noite: o brilho vem da lua (lado oposto)
    }

    // recompõe se ninguem chamou compose() recentemente (ex.: tela do mapa-mundi aberta)
    public static void composeIfStale(Minecraft mc) {
        if (net.minecraft.Util.getMillis() - lastCompose > 120) compose(mc, mc.getTimer().getGameTimeDeltaPartialTick(false), Float.NaN, 0f);
    }

    private static void surfaceColors(float[] f, float[] m, float dayF, float twil, float dirF, float rain, float thunder, boolean snow) {
        for (int i = 0; i < 3; i++) {
            f[i] = lerp(surfC[NIGHT + i], surfC[DAY + i], dayF);
            float mistNight = lerp(surfC[NIGHT + i], surfC[MIST + i] * 0.16f, 0.55f);
            m[i] = lerp(mistNight, surfC[MIST + i], dayF);
        }
        if (twil > 0.001f) {
            float[] warm = {1.0f, 0.58f, 0.36f};
            float lumDay = Math.max(0.5f, lum(surfC[DAY], surfC[DAY + 1], surfC[DAY + 2]));
            float tf = twil * 0.42f * dirF, tm = twil * 0.30f * dirF;
            for (int i = 0; i < 3; i++) {
                f[i] = lerp(f[i], warm[i] * lumDay, tf);
                m[i] = lerp(m[i], (m[i] + warm[i]) * 0.5f, tm);
            }
        }
        if (rain > 0.001f) {
            float lf = lum(f[0], f[1], f[2]), lm = lum(m[0], m[1], m[2]);
            float[] tint = snow ? new float[]{0.96f, 0.98f, 1.02f} : new float[]{0.93f, 0.96f, 1.02f};
            float k = snow ? 0.95f : 0.74f;
            for (int i = 0; i < 3; i++) {
                f[i] = lerp(f[i], lf * k * tint[i], rain * 0.65f);
                m[i] = lerp(m[i], lm * k * tint[i], rain * 0.55f);
            }
        }
        if (thunder > 0.001f) {
            for (int i = 0; i < 3; i++) { f[i] *= 1f - thunder * 0.45f; m[i] *= 1f - thunder * 0.40f; }
        }
    }

    // ---------------------------------------------------------------- ceu (chamado pelo mixin do ClientLevel.getSkyColor)
    // troca so o MATIZ do ceu do jogo para o do bioma (o brilho continua o do jogo: noite, chuva etc.)
    public static Vec3 tintSky(Vec3 v) {
        if (!valid || dimMode) return v;
        float s = Math.min(0.9f, surfC[SKY_S] * 1.25f) * (1f - caveF);
        if (s < 0.01f) return v;
        float sr = surfC[SKY], sg = surfC[SKY + 1], sb = surfC[SKY + 2];
        float lv = lum((float) v.x, (float) v.y, (float) v.z), ls = Math.max(1e-3f, lum(sr, sg, sb));
        float k = lv / ls;
        return new Vec3(lerp((float) v.x, Math.min(1f, sr * k), s), lerp((float) v.y, Math.min(1f, sg * k), s),
                lerp((float) v.z, Math.min(1f, sb * k), s));
    }

    // ---------------------------------------------------------------- utilitarios
    private static float r(int c) { return ((c >> 16) & 255) / 255f; }
    private static float g(int c) { return ((c >> 8) & 255) / 255f; }
    private static float b(int c) { return (c & 255) / 255f; }
    static float lum(float r, float g, float b) { return 0.2126f * r + 0.7152f * g + 0.0722f * b; }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    static float smooth(float e0, float e1, float x) {
        float t = clamp((x - e0) / (e1 - e0));
        return t * t * (3f - 2f * t);
    }
}

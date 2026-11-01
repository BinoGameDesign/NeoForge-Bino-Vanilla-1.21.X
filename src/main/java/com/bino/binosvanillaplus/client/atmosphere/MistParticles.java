package com.bino.binosvanillaplus.client.atmosphere;

import com.bino.binosvanillaplus.client.map.FarTerrain;
import com.bino.binosvanillaplus.client.map.MapOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

// Semeia os fiapos de nevoa em volta do jogador conforme o lugar (escrito do zero):
//  - ao ar livre: deitados no chao e nos vales, mais densos na base da camada (exponencial pela altura), mais de manha,
//    a noite e na chuva; quantidade pela densidade de nevoa baixa do bioma (pantano/selva/pale garden cheios, deserto pouco)
//  - em caverna: perto do chao das galerias escuras, com a cor da caverna (verde nas lush, marrom nas dripstone...)
//  - Nether: bafo sobre o mar de lava; End: mar de nevoa roxa debaixo das ilhas
// Vento: direcao que gira devagar com o tempo do mundo, igual para todos os fiapos.
public final class MistParticles {
    private MistParticles() {}

    private static ClientLevel lastLevel;

    public static void tick(Minecraft mc) {
        ClientLevel lvl = mc.level;
        if (lvl != lastLevel) { MistParticle.alive = 0; lastLevel = lvl; }
        if (lvl == null || mc.player == null || mc.isPaused() || !AtmosphereState.valid()) return;
        MapOptions.ensureLoaded();
        if (MapOptions.atmosphere < 1) return;
        if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) return;

        RandomSource rnd = lvl.random;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float cave = AtmosphereState.caveNow();
        boolean dims = AtmosphereState.dims();

        // hora / clima
        float dayF = 1f, morning = 0f, rain = 0f;
        if (!dims) {
            double ang = lvl.getTimeOfDay(1f) * Math.PI * 2.0;
            float sunX = (float) -Math.sin(ang), sunY = (float) Math.cos(ang);
            dayF = AtmosphereState.smooth(-0.12f, 0.22f, sunY);
            morning = sunX > 0f ? AtmosphereState.smooth(-0.15f, 0.05f, sunY) * (1f - AtmosphereState.smooth(0.25f, 0.55f, sunY)) : 0f;
            rain = lvl.getRainLevel(1f);
        }
        // sem shaderpack a passada em camadas ja faz o grosso: fiapos so de detalhe
        float budget = FarTerrain.irisPack() || MapOptions.atmosphere < 2 ? 1f : 0.55f;
        if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.DECREASED) budget *= 0.5f;

        boolean inCave = cave > 0.5f;
        float[] src = inCave ? AtmosphereState.caveC : AtmosphereState.surfC;
        float mistD = src[AtmosphereState.MIST_D], haze = src[AtmosphereState.HAZE];
        int target;
        if (inCave) target = (int) (Math.min(70f, mistD / 0.006f * 30f + haze / 0.010f * 22f) * budget);
        else target = (int) (Math.min(220f, mistD / 0.0035f * 42f * (1f + 1.5f * morning + 0.4f * (1f - dayF) + 0.9f * rain)) * budget);
        if (MistParticle.alive >= target) return;

        // cor (albedo): matiz da nevoa do lugar, clareada se for muito escura (a luz do mundo/shader escurece depois)
        float r = src[AtmosphereState.MIST], g = src[AtmosphereState.MIST + 1], b = src[AtmosphereState.MIST + 2];
        if (rain > 0.01f && !inCave) {
            float l = AtmosphereState.lum(r, g, b);
            r = AtmosphereState.lerp(r, l, rain * 0.6f); g = AtmosphereState.lerp(g, l, rain * 0.6f); b = AtmosphereState.lerp(b, l, rain * 0.6f);
        }
        float mx = Math.max(r, Math.max(g, b));
        if (mx > 1e-3f) {
            float k = Math.max(0.45f, Math.min(1f, mx)) / mx;
            r *= k; g *= k; b *= k;
        }

        double windA = (lvl.getGameTime() % 96000L) / 96000.0 * Math.PI * 2.0;
        double wx = Math.cos(windA) * 0.018, wz = Math.sin(windA) * 0.018;

        int tries = Math.min(8, target - MistParticle.alive);
        for (int i = 0; i < tries; i++) {
            if (inCave) spawnCave(mc, lvl, rnd, cam, r, g, b, wx * 0.3, wz * 0.3);
            else if (dims) spawnLayer(mc, lvl, rnd, cam, src, r, g, b, wx, wz, true);
            else spawnLayer(mc, lvl, rnd, cam, src, r, g, b, wx, wz, false);
        }
    }

    // ao ar livre / Nether / End: num anel em volta, deitado na camada de nevoa baixa
    private static void spawnLayer(Minecraft mc, ClientLevel lvl, RandomSource rnd, Vec3 cam, float[] src, float r, float g, float b,
                                   double wx, double wz, boolean dims) {
        double rad = Math.sqrt(rnd.nextDouble() * (64.0 * 64.0 - 8.0 * 8.0) + 8.0 * 8.0);
        double ang = rnd.nextDouble() * Math.PI * 2.0;
        int x = (int) Math.floor(cam.x + Math.cos(ang) * rad), z = (int) Math.floor(cam.z + Math.sin(ang) * rad);
        if (!lvl.hasChunk(x >> 4, z >> 4)) return;
        float base = src[AtmosphereState.BASE], h = Math.max(2f, src[AtmosphereState.MIST_H]);
        double y;
        if (dims) {
            y = base + (rnd.nextDouble() - 0.35) * h * 1.6;
        } else {
            int ground = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            y = ground + 0.6 + rnd.nextDouble() * rnd.nextDouble() * h * 1.3;
        }
        if (y > cam.y + 48 || y < lvl.getMinBuildHeight()) return;
        // mais denso na base da camada; acima dela vai rareando
        float w = (float) Math.exp(-Math.max(-h, Math.min(400.0, y - base)) / h) / (float) Math.E;
        if (rnd.nextFloat() > Math.max(0.12f, w)) return;
        BlockPos p = BlockPos.containing(x, y, z);
        if (lvl.getBlockState(p).isSolidRender(lvl, p)) return;
        if (!dims && lvl.getBrightness(LightLayer.SKY, p) < 8) return;   // nao nasce dentro de caverna/casa
        float size = (3.2f + rnd.nextFloat() * 4.5f) * (float) (0.75 + rad / 90.0);
        float alpha = 0.07f + 0.11f * Math.min(1f, w) + rnd.nextFloat() * 0.03f;
        int life = 180 + rnd.nextInt(200);
        mc.particleEngine.add(new MistParticle(lvl, x + rnd.nextDouble(), y, z + rnd.nextDouble(), size, alpha,
                r, g, b, wx * (0.6 + 0.4 * rnd.nextDouble()), 0.0015 * rnd.nextDouble(), wz * (0.6 + 0.4 * rnd.nextDouble()), life));
    }

    // caverna: em galerias escuras perto do jogador, rente ao chao
    private static void spawnCave(Minecraft mc, ClientLevel lvl, RandomSource rnd, Vec3 cam, float r, float g, float b, double wx, double wz) {
        double dx = (rnd.nextDouble() * 2 - 1) * 22, dy = (rnd.nextDouble() * 2 - 1) * 8, dz = (rnd.nextDouble() * 2 - 1) * 22;
        if (dx * dx + dz * dz < 25) return;
        BlockPos.MutableBlockPos p = BlockPos.containing(cam.x + dx, cam.y + dy, cam.z + dz).mutable();
        if (!lvl.hasChunkAt(p) || !lvl.getBlockState(p).isAir() || lvl.getBrightness(LightLayer.SKY, p) > 3) return;
        int down = 0;
        while (down < 10 && lvl.getBlockState(p.below()).isAir()) { p.move(0, -1, 0); down++; }
        if (down >= 10) return;   // sem chao perto (abismo): pula
        double y = p.getY() + 0.3 + rnd.nextDouble() * 1.8;
        float size = 2.4f + rnd.nextFloat() * 3.2f;
        float alpha = 0.08f + rnd.nextFloat() * 0.07f;
        int life = 200 + rnd.nextInt(200);
        mc.particleEngine.add(new MistParticle(lvl, p.getX() + rnd.nextDouble(), y, p.getZ() + rnd.nextDouble(), size, alpha,
                r, g, b, wx * rnd.nextDouble(), 0.0008 * rnd.nextDouble(), wz * rnd.nextDouble(), life));
    }
}

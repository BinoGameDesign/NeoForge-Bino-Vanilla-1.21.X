package com.bino.binosmining.client.lapidary;

import com.bino.binosmining.client.gear.ForgeGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

// Faiscas e po do disco girando (chamado a cada tick enquanto o disco gira)
public final class LapidaryFx {
    private LapidaryFx() {}

    public static void sparks(ClientLevel level, BlockPos pos, LapidaryView v) {
        if (v.disc().isEmpty() || v.armDrop() < 1.4f) return;   // so quando o lapidador encosta na pedra
        Minecraft mc = Minecraft.getInstance();
        RandomSource rnd = level.random;
        Direction facing = LapidaryGeo.facing(level.getBlockState(pos));
        // ponto de contato: logo abaixo do silex, em cima da pedra
        Vec3 at = LapidaryGeo.world(pos, facing, LapidaryGeo.FLINT.x, LapidaryGeo.ORE.y + 0.9, LapidaryGeo.FLINT.z);
        int rgb = ForgeGlow.color(LapidaryRemote.gemOf(v.disc()).isEmpty() ? v.disc().getItem() : LapidaryRemote.gemOf(v.disc()).getItem());
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        int n = rnd.nextFloat() < v.speed() ? 1 + rnd.nextInt(v.speed() > 0.7f ? 3 : 2) : 0;
        for (int i = 0; i < n; i++) {
            // faiscas saem pela borda, na direcao do giro do disco (tangente), um pouco para cima
            double a = rnd.nextDouble() * Math.PI * 2;
            Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 vel = out.scale(0.08 + rnd.nextDouble() * 0.10).add(0, 0.03 + rnd.nextDouble() * 0.06, 0);
            Particle p = mc.particleEngine.createParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, vel.x * 4, vel.y * 4, vel.z * 4);
            if (p != null) {
                if (rnd.nextBoolean()) p.setColor(1f, 0.85f, 0.55f); else p.setColor(r, g, b);
                p.setLifetime(5 + rnd.nextInt(6));
                p.scale(0.45f);
            }
        }
        if (rnd.nextFloat() < 0.25f * v.speed()) {
            level.addParticle(new DustParticleOptions(new Vector3f(r * 0.8f + 0.2f, g * 0.8f + 0.2f, b * 0.8f + 0.2f), 0.45f),
                    at.x + (rnd.nextDouble() - 0.5) * 0.12, at.y, at.z + (rnd.nextDouble() - 0.5) * 0.12, 0, 0.01, 0);
        }
    }
}

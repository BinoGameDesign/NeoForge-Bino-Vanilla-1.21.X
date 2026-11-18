package com.bino.binosvanillaplus.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

// "Z" de sono: sobe devagar balancando de lado, cresce um pouco e some no fim
public class SleepZParticle extends TextureSheetParticle {
    private final float baseSize;
    private final float phase;

    protected SleepZParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 40 + random.nextInt(15);
        gravity = 0;
        hasPhysics = false;
        xd = 0; zd = 0; yd = 0.018;
        baseSize = 0.06f + random.nextFloat() * 0.03f;
        quadSize = baseSize;
        phase = random.nextFloat() * Mth.TWO_PI;
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        xd = Mth.sin(phase + age * 0.25f) * 0.006f;   // balanca de lado
        quadSize = baseSize * (1.0f + t * 0.8f);
        alpha = t < 0.7f ? 1.0f : 1.0f - (t - 0.7f) / 0.3f;
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SleepZParticle(level, x, y, z, sprites);
        }
    }
}

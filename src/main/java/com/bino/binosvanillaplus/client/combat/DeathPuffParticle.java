package com.bino.binosvanillaplus.client.combat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

// "Puf" do mob morrendo (estilo Zelda): bolas de fumaca roxa-escura que incham, sobem devagar e clareiam sumindo.
public class DeathPuffParticle extends TextureSheetParticle {
    private final float size0;

    protected DeathPuffParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.xd = vx * 0.5 + (random.nextDouble() - 0.5) * 0.03;
        this.yd = 0.02 + random.nextDouble() * 0.03;
        this.zd = vz * 0.5 + (random.nextDouble() - 0.5) * 0.03;
        this.friction = 0.9f;
        this.gravity = -0.02f;
        this.hasPhysics = false;
        this.lifetime = 14 + random.nextInt(10);
        this.size0 = 0.18f + random.nextFloat() * 0.14f;
        this.quadSize = size0;
        pickSprite(sprites);
        setColor(0.30f, 0.08f, 0.40f);
        this.alpha = 0.85f;
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        this.quadSize = size0 * (1f + 1.2f * t);
        setColor(Mth.lerp(t, 0.30f, 0.62f), Mth.lerp(t, 0.08f, 0.45f), Mth.lerp(t, 0.40f, 0.75f));
        this.alpha = 0.85f * (1f - t * t);
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new DeathPuffParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}

package com.bino.binosvanillaplus.client.combat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

// Faisca do golpe: sai rapida na direcao dada, perde velocidade, cai um pouco e apaga; branca-amarela que vira laranja.
// Brilha sozinha (nao depende da luz do lugar).
public class HitSparkParticle extends TextureSheetParticle {
    private final float size0;

    protected HitSparkParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.xd = vx; this.yd = vy; this.zd = vz;   // a velocidade vem pronta do servidor (direcao do golpe)
        this.friction = 0.86f;
        this.gravity = 0.9f;
        this.lifetime = 6 + random.nextInt(6);
        this.size0 = 0.035f + random.nextFloat() * 0.035f;
        this.quadSize = size0;
        this.hasPhysics = true;
        pickSprite(sprites);
        setColor(1f, 0.97f, 0.8f);
    }

    @Override
    public void tick() {
        super.tick();
        float u = (float) age / lifetime;
        setColor(1f, Mth.lerp(u, 0.95f, 0.55f), Mth.lerp(u, 0.75f, 0.15f));
        this.alpha = 1f - u * u;
        this.quadSize = size0 * (1f - 0.5f * u);
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new HitSparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}

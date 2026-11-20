package com.bino.binosvanillaplus.client.combat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

// Efeitos de impacto estilo Zelda (mod Combat): formas simples, brilhantes e rapidas, paradas no ponto do golpe.
//  - estrela de 4 pontas: estoura grande em ~0,05 s e encolhe sumindo (branca, amarelando no fim)
//  - anel: abre rapido a partir do ponto e some
// O tamanho vem na velocidade X (o servidor manda com contagem 0); velocidade Y = 1 deixa dourada (aviso de ataque).
public class ImpactParticle extends TextureSheetParticle {
    private final boolean ring, gold;
    private final float base;

    protected ImpactParticle(ClientLevel level, double x, double y, double z, double scale, boolean gold, SpriteSet sprites, boolean ring) {
        super(level, x, y, z, 0, 0, 0);
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.ring = ring;
        this.gold = gold;
        float s = scale > 0.05 ? (float) scale : 1f;
        this.base = (ring ? 0.75f : 0.42f) * s;
        this.lifetime = ring ? 7 : 5;
        this.roll = this.oRoll = ring ? 0 : (random.nextFloat() - 0.5f) * 0.6f;
        pickSprite(sprites);
        if (gold) setColor(1f, 0.82f, 0.3f); else setColor(1f, 1f, 1f);
    }

    @Override
    public float getQuadSize(float pt) {
        float t = Mth.clamp((age + pt) / lifetime, 0f, 1f);
        if (ring) return base * (0.25f + 0.95f * (1 - (1 - t) * (1 - t)));
        return t < 0.2f ? base * 1.25f * (t / 0.2f) : base * (1.25f - 0.75f * (t - 0.2f) / 0.8f);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        this.alpha = ring ? 0.9f * (1 - t) : 1f - t * t;
        if (!ring && !gold) setColor(1f, 1f, Mth.lerp(t, 1f, 0.55f));
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class StarProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public StarProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new ImpactParticle(level, x, y, z, vx, vy > 0.5, sprites, false);
        }
    }

    public static class RingProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public RingProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new ImpactParticle(level, x, y, z, vx, vy > 0.5, sprites, true);
        }
    }
}

package com.bino.binoscombat.client.combat;

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
// O tamanho vem na velocidade X (o servidor manda com contagem 0); velocidade Y = 1 dourada/amarela, 2 vermelha, 4 azul clara (brilho do parry).
// Velocidade Z = 1: tamanho de verdade no mundo (brilho do parry: perto da camera fica MAIOR na tela, longe menor).
public class ImpactParticle extends TextureSheetParticle {
    private final boolean ring, gold, red, blue, exact;
    private final float base;

    protected ImpactParticle(ClientLevel level, double x, double y, double z, double scale, double tint, SpriteSet sprites, boolean ring) {
        this(level, x, y, z, scale, tint, 0, sprites, ring);
    }

    protected ImpactParticle(ClientLevel level, double x, double y, double z, double scale, double tint, double flag, SpriteSet sprites, boolean ring) {
        super(level, x, y, z, 0, 0, 0);
        this.exact = flag > 0.5;
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.ring = ring;
        this.gold = tint > 0.5 && tint < 1.5;
        this.red = tint >= 1.5 && tint < 2.5;
        this.blue = tint >= 3.5;
        float s = scale > 0.05 ? (float) scale : 1f;
        this.base = (ring ? 0.75f : 0.42f) * s;
        this.lifetime = ring ? 7 : 5;
        this.roll = this.oRoll = ring ? 0 : (random.nextFloat() - 0.5f) * 0.6f;
        pickSprite(sprites);
        if (blue) setColor(0.55f, 0.88f, 1f); else if (red) setColor(1f, 0.22f, 0.18f); else if (gold) setColor(1f, 0.82f, 0.3f); else setColor(1f, 1f, 1f);
    }

    @Override
    public float getQuadSize(float pt) {
        float t = Mth.clamp((age + pt) / lifetime, 0f, 1f);
        float s = ring ? base * (0.25f + 0.95f * (1 - (1 - t) * (1 - t)))
                : t < 0.2f ? base * 1.25f * (t / 0.2f) : base * (1.25f - 0.75f * (t - 0.2f) / 0.8f);
        if (exact) {   // so some se estiver literalmente dentro da camera
            var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            return cam.distanceToSqr(x, y, z) < 0.35 * 0.35 ? 0f : s;
        }
        return s * near(x, y, z);
    }

    // colado na camera (mob encostado em voce) a forma encolhe, para nao cobrir a tela
    static float near(double x, double y, double z) {
        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double d = Math.sqrt(cam.distanceToSqr(x, y, z));
        // (com shaders o brilho espalha: encostado na camera some de vez em vez de virar um bloco branco na tela)
        return (float) Mth.clamp((d - 0.7) / 2.0, 0.15, 1.0);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        this.alpha = ring ? 0.9f * (1 - t) : 1f - t * t;
        if (!ring && !gold && !red && !blue) setColor(1f, 1f, Mth.lerp(t, 1f, 0.55f));
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class StarProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public StarProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new ImpactParticle(level, x, y, z, vx, vy, vz, sprites, false);
        }
    }

    public static class RingProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public RingProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new ImpactParticle(level, x, y, z, vx, vy, vz, sprites, true);
        }
    }
}

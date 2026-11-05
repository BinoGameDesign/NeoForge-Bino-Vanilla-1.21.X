package com.bino.binosvanillaplus.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

// Lasca de cristal que cai do minerio (usa um pedacinho da textura do cristal, como as particulas de quebra do jogo),
// gira enquanto cai, quica no chao e brilha no escuro.
public class CrystalShardParticle extends TextureSheetParticle {
    private final float uo, vo;
    private final float spin;

    public CrystalShardParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        setSprite(sprite);
        xd = vx; yd = vy; zd = vz;
        gravity = 1.0f;
        friction = 0.96f;
        hasPhysics = true;
        quadSize = 0.045f + random.nextFloat() * 0.05f;
        lifetime = 30 + random.nextInt(25);
        uo = random.nextFloat() * 3f;
        vo = random.nextFloat() * 3f;
        spin = (random.nextFloat() - 0.5f) * 0.6f;
        roll = random.nextFloat() * 6.28f;
        oRoll = roll;
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        if (!onGround) roll += spin;
        else { xd *= 0.6; zd *= 0.6; }
        if (age > lifetime - 8) alpha = Math.max(0f, (lifetime - age) / 8f);   // some devagar no fim
    }

    @Override
    protected float getU0() { return sprite.getU(uo / 4f); }
    @Override
    protected float getU1() { return sprite.getU((uo + 1f) / 4f); }
    @Override
    protected float getV0() { return sprite.getV(vo / 4f); }
    @Override
    protected float getV1() { return sprite.getV((vo + 1f) / 4f); }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }   // o cristal brilha

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }
}

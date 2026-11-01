package com.bino.binosvanillaplus.client.atmosphere;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.map.FarTerrain;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// Um "fiapo" de nevoa: quadrado grande, macio e bem transparente, virado para a camera, que nasce, vaga com o vento
// e some devagar. Varios juntos formam a nevoa em camadas.
// E' uma PARTICULA de proposito: com Iris, o shaderpack (Complementary) desenha particulas com os programas dele
// (luz, sombra, neblina, cor do ceu) - entao a nevoa aparece integrada no Complementary.
public class MistParticle extends SingleQuadParticle {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/particle/mist.png");
    public static ShaderInstance shader;   // sem shaderpack: shader proprio (o do jogo corta alpha < 0.1 e a nevoa e' mais fina que isso)

    // Desenho: textura propria, sem escrever profundidade (fiapos se sobrepoem sem "recorte"), mistura aditiva.
    // Com shaderpack usa o shader de particula do jogo, que o Iris troca pelo do pack.
    public static final ParticleRenderType SHEET = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            // SOMA de luz (aditivo): a nevoa so clareia, nunca escurece -> sem "bolhas" escuras contra o ceu ou na sombra,
            // e brilha de leve sobre o que esta atras (sensacao de magia)
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, TEXTURE);
            if (FarTerrain.irisPack() || shader == null) RenderSystem.setShader(GameRenderer::getParticleShader);
            else RenderSystem.setShader(() -> shader);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() { return BinosVanillaMod.MOD_ID + ":mist"; }
    };

    static int alive = 0;

    private final float peakAlpha, u0, v0;
    private final float spin;
    private boolean counted = true;

    MistParticle(ClientLevel level, double x, double y, double z, float size, float alpha, float r, float g, float b,
                 double vx, double vy, double vz, int life) {
        super(level, x, y, z);
        this.quadSize = size;
        this.peakAlpha = alpha;
        this.alpha = 0f;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = life;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.friction = 1f;
        int k = random.nextInt(4);           // 4 variacoes na textura (2x2)
        this.u0 = (k & 1) * 0.5f;
        this.v0 = (k >> 1) * 0.5f;
        this.roll = this.oRoll = (random.nextFloat() - 0.5f) * 0.3f;   // fiapos deitados (textura esticada na horizontal)
        this.spin = (random.nextFloat() - 0.5f) * 0.0008f;
        setSize(size, size);                 // caixa do tamanho do fiapo (para nao sumir na borda da tela)
        alive++;
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        oRoll = roll;
        roll += spin;
        if (age++ >= lifetime) { remove(); return; }
        move(xd, yd, zd);
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > 88) { remove(); return; }
        // envelope: entra em ~2 s, sai em ~3 s; some perto da camera (nada de borrao na cara) e bem longe
        float env = Math.min(1f, age / 40f) * Math.min(1f, (lifetime - age) / 60f);
        float near = AtmosphereState.smooth(1.5f, 7f, (float) d);
        float far = 1f - AtmosphereState.smooth(60f, 80f, (float) d);
        alpha = peakAlpha * env * near * far;
    }

    @Override
    public void remove() {
        super.remove();
        if (counted) { counted = false; alive--; }
    }

    // luz do lugar (se o centro caiu dentro de um bloco solido, usa a do ar logo acima - senao o fiapo fica preto)
    @Override
    protected int getLightColor(float partialTick) {
        net.minecraft.core.BlockPos.MutableBlockPos p = net.minecraft.core.BlockPos.containing(x, y, z).mutable();
        if (!level.hasChunkAt(p)) return 0xF000F0;
        for (int i = 0; i < 4 && level.getBlockState(p).isSolidRender(level, p); i++) p.move(0, 1, 0);
        return net.minecraft.client.renderer.LevelRenderer.getLightColor(level, p);
    }

    @Override protected float getU0() { return u0; }
    @Override protected float getU1() { return u0 + 0.5f; }
    @Override protected float getV0() { return v0; }
    @Override protected float getV1() { return v0 + 0.5f; }

    @Override
    public ParticleRenderType getRenderType() { return SHEET; }
}

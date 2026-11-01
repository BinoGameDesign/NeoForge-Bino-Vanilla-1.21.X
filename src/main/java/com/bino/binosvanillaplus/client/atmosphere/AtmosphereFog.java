package com.bino.binosvanillaplus.client.atmosphere;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.map.FarTerrain;
import com.bino.binosvanillaplus.client.map.MapAtmosphere;
import com.bino.binosvanillaplus.client.map.MapOptions;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

// Neblina do ambiente (escrita do zero). Tres caminhos, todos com a MESMA cor (AtmosphereState):
//  1) cor da neblina do jogo (ComputeFogColor): vale sempre - inclusive com Iris/Complementary, que leem a cor de neblina
//     e a cor do ceu do jogo (o ceu e' tingido pelo mixin do ClientLevel.getSkyColor)
//  2) sem shaderpack: passada propria de neblina em CAMADAS por cima do mundo (atmos_fog): le a profundidade da cena e,
//     para cada pixel, soma a nevoa alta (ar distante) + a nevoa baixa que deita nos vales e afina com a altura, com
//     manchas que andam com o vento e o brilho do sol atravessando. Nada de "parede" de neblina.
//  3) minimapa / mapa-mundi / distancia extra: MapAtmosphere.provider aponta para a cor de superficie daqui
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class AtmosphereFog {
    private AtmosphereFog() {}

    public static ShaderInstance fogShader;     // atmos_fog (calcula a neblina num alvo proprio)
    public static ShaderInstance blendShader;   // atmos_blend (aplica a neblina por cima da cena)
    private static TextureTarget fogTarget;
    private static VertexBuffer quad;

    static boolean colorOn() {
        MapOptions.ensureLoaded();
        return MapOptions.atmosphere >= 1;
    }

    static boolean layersOn() {
        MapOptions.ensureLoaded();
        return MapOptions.atmosphere >= 2 && fogShader != null && blendShader != null && !FarTerrain.irisPack();
    }

    // ---------------------------------------------------------------- mapas
    private static final MapAtmosphere.Provider MAP_PROVIDER = new MapAtmosphere.Provider() {
        @Override
        public float[] fogColor() {
            if (!colorOn() || !AtmosphereState.valid()) return MapAtmosphere.VANILLA.fogColor();
            AtmosphereState.composeIfStale(Minecraft.getInstance());
            float[] f = AtmosphereState.surfaceFog.clone();
            // a noite no mapa vira "luar": azul legivel em vez de quase preto
            float n = AtmosphereState.night * 0.75f;
            float[] moon = {0.17f, 0.22f, 0.36f};
            for (int i = 0; i < 3; i++) f[i] = Math.max(f[i], f[i] + (moon[i] - f[i]) * n);
            return f;
        }

        @Override
        public float[] skyColor() { return MapAtmosphere.VANILLA.skyColor(); }   // ja vem tingido pelo mixin

        @Override
        public float fogDensity() { return colorOn() && AtmosphereState.valid() ? AtmosphereState.surfaceDensity : 1f; }
    };

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (MapAtmosphere.provider == MapAtmosphere.VANILLA) MapAtmosphere.provider = MAP_PROVIDER;
        AtmosphereState.tick(Minecraft.getInstance());
        MistParticles.tick(Minecraft.getInstance());
    }

    private static boolean skip(Camera cam) {
        if (cam.getFluidInCamera() != FogType.NONE) return true;   // debaixo d'agua/lava/neve em po: neblina do jogo
        return cam.getEntity() instanceof LivingEntity le
                && (le.hasEffect(MobEffects.BLINDNESS) || le.hasEffect(MobEffects.DARKNESS));
    }

    // ---------------------------------------------------------------- 1) cor da neblina do jogo
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (!colorOn() || !AtmosphereState.valid() || skip(event.getCamera())) return;
        Vector3f look = event.getCamera().getLookVector();
        AtmosphereState.compose(mc, (float) event.getPartialTick(), look.x(), look.z());
        float[] f = AtmosphereState.fog;
        // o jogo escurece a neblina perto do fundo do mundo: respeita
        float k = 1f;
        if (mc.level != null) {
            double y = event.getCamera().getPosition().y - mc.level.getMinBuildHeight();
            float scale = mc.level.getLevelData().getClearColorScale() <= 0 ? 1f : (float) mc.level.getLevelData().getClearColorScale();
            k = (float) Math.max(0.0, Math.min(1.0, y * scale));
            k = k < 1f ? k * k : 1f;
        }
        event.setRed(f[0] * k);
        event.setGreen(f[1] * k);
        event.setBlue(f[2] * k);
    }

    // ---------------------------------------------------------------- distancias da neblina do jogo
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        boolean changed = false;
        float near = event.getNearPlaneDistance(), far = event.getFarPlaneDistance();
        // distancia extra (sem shaderpack): a neblina do jogo vai para o fim do terreno distante
        if (FarTerrain.active(mc) && !FarTerrain.irisPack()) {
            float farBlocks = MapOptions.farChunks * 16f;
            near = farBlocks * 0.62f;
            far = farBlocks * 0.98f;
            changed = true;
        }
        // com shaderpack (a passada em camadas nao roda): densidade do bioma/caverna nas distancias da neblina do jogo,
        // de leve, para packs que usam fogStart/fogEnd
        if (colorOn() && AtmosphereState.valid() && FarTerrain.irisPack() && !skip(event.getCamera())) {
            float k = (float) Math.max(0.6, Math.min(1.1, Math.sqrt(0.0016 / Math.max(1e-4, AtmosphereState.haze))));
            near *= k;
            far *= k;
            changed = true;
        }
        if (changed) {
            event.setNearPlaneDistance(near);
            event.setFarPlaneDistance(far);
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- 2) neblina em camadas (sem shaderpack)
    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !layersOn() || !AtmosphereState.valid() || skip(event.getCamera())) return;
        try {
            drawLayers(event, mc);
        } catch (Throwable t) {
            BinosVanillaMod.LOGGER.error("Neblina em camadas falhou; desligando a passada", t);
            fogShader = null;
        }
    }

    private static void drawLayers(RenderLevelStageEvent event, Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        if (fogTarget == null) {
            fogTarget = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            fogTarget.setFilterMode(GL11.GL_NEAREST);
        } else if (fogTarget.width != w || fogTarget.height != h) {
            fogTarget.resize(w, h, Minecraft.ON_OSX);
        }
        if (quad == null) {
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            b.addVertex(-1f, -1f, 0f);
            b.addVertex(1f, -1f, 0f);
            b.addVertex(1f, 1f, 0f);
            b.addVertex(-1f, 1f, 0f);
            MeshData md = b.build();
            quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            quad.bind();
            if (md != null) quad.upload(md);
            VertexBuffer.unbind();
        }
        int prevFbo = GlStateManager._getInteger(GL30.GL_FRAMEBUFFER_BINDING);

        Vec3 cam = event.getCamera().getPosition();
        Matrix4f invProjView = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
        int rd = mc.options.getEffectiveRenderDistance() * 16;
        float skyDist = Math.max(rd, FarTerrain.active(mc) ? MapOptions.farChunks * 16f : 0f) * 1.15f;
        float time = (mc.level.getGameTime() % 240000L + mc.getTimer().getGameTimeDeltaPartialTick(false)) / 20f;

        ShaderInstance sh = fogShader;
        sh.safeGetUniform("InvProjView").set(invProjView);
        sh.safeGetUniform("AtmCam").set((float) (cam.x - Math.floor(cam.x / 4096.0) * 4096.0), (float) cam.y,
                (float) (cam.z - Math.floor(cam.z / 4096.0) * 4096.0));
        float[] f = AtmosphereState.fog, m = AtmosphereState.mist, g = AtmosphereState.sunGlow, sd = AtmosphereState.sunDir;
        sh.safeGetUniform("AtmFog").set(f[0], f[1], f[2]);
        sh.safeGetUniform("AtmMist").set(m[0], m[1], m[2]);
        sh.safeGetUniform("AtmSunDir").set(sd[0], sd[1], sd[2]);
        sh.safeGetUniform("AtmSunCol").set(g[0], g[1], g[2]);
        // nevoa alta: densidade, comeco, opacidade maxima, distancia do ceu
        float maxOp = AtmosphereState.cave > 0.5f ? 0.97f : 0.92f;
        sh.safeGetUniform("AtmHaze").set(AtmosphereState.haze, AtmosphereState.start, maxOp, skyDist);
        // nevoa baixa: densidade, base, altura, manchas
        sh.safeGetUniform("AtmLayer").set(AtmosphereState.mistDensity, AtmosphereState.mistBase, AtmosphereState.mistHeight,
                0.55f * (1f - AtmosphereState.cave * 0.5f));
        // a nevoa alta afina com a altura ao ar livre (ceu limpo la em cima); em caverna/Nether e' igual em toda altura
        boolean flat = AtmosphereState.cave > 0.5f || AtmosphereState.hasCeiling;
        sh.safeGetUniform("AtmHazeH").set(flat ? 1.0e5f : 260f);
        sh.safeGetUniform("AtmMistFar").set(Math.min(rd * 1.05f, 384f));
        sh.safeGetUniform("AtmTime").set(time);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        // passo A: calcula a neblina (cor ja multiplicada pela opacidade) no alvo proprio, lendo a profundidade da cena
        fogTarget.setClearColor(0f, 0f, 0f, 0f);
        fogTarget.clear(Minecraft.ON_OSX);
        fogTarget.bindWrite(true);
        sh.setSampler("DepthTex", main.getDepthTextureId());
        RenderSystem.disableBlend();   // (o BlendMode do shader guarda cache; garante o estado certo)
        quad.bind();
        quad.drawWithShader(new Matrix4f(), new Matrix4f(), sh);

        // passo B: aplica por cima da cena
        main.bindWrite(true);
        blendShader.setSampler("FogTex", fogTarget.getColorTextureId());
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);   // cor ja vem multiplicada
        quad.drawWithShader(new Matrix4f(), new Matrix4f(), blendShader);
        VertexBuffer.unbind();

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        RenderSystem.viewport(0, 0, w, h);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}

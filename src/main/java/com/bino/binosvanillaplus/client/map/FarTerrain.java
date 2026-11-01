package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

// Distancia extra (escrita do zero, ideia parecida com a do Distant Horizons, mas bem mais leve):
// alem da render distance, desenha no mundo o terreno que o jogador JA EXPLOROU, usando as mesmas malhas do mapa-mundi
// (texturas reais, copas, agua). Um unico conjunto de dados alimenta minimapa, mapa-mundi e a distancia extra.
//  - desenha logo depois do ceu, com projecao propria (mais longe), e limpa a profundidade: o terreno de verdade
//    sempre fica por cima, sem brigar com o terreno distante
//  - so os chunks fora do que o jogo ja desenha; recorte pela visao da camera (frustum); poucas malhas montadas por quadro
//  - a neblina do jogo e' empurrada para o fim da distancia extra (e a do terreno distante usa a mesma cor)
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class FarTerrain {
    private static final int BUILD_PER_FRAME = 4;

    public static boolean active(Minecraft mc) {
        MapOptions.ensureLoaded();
        return mc.level != null && MapOptions.farChunks > mc.options.getEffectiveRenderDistance()
                && !mc.level.dimensionType().hasCeiling() && DioramaRenderer.ready() && !externalTakesOver();
    }

    // Com shaderpack (Iris: Complementary etc.) ou com o Distant Horizons instalado, a distancia extra se desliga sozinha:
    // o shaderpack assume o desenho do mundo inteiro e so sabe sombrear o terreno distante do DH (o Complementary tem
    // programas proprios para o DH). Assim nada fica quebrado e o DH + Complementary cuidam da distancia.
    private static long lastCheck = -1;
    private static boolean takenOver = false;

    // so o Distant Horizons instalado desliga a distancia extra (os dois desenhariam a mesma coisa)
    static boolean externalTakesOver() {
        refresh();
        return takenOver;
    }

    private static boolean irisPack = false;

    // shaderpack do Iris (Complementary etc.) em uso?
    public static boolean irisPack() {
        refresh();
        return irisPack;
    }

    private static void refresh() {
        long now = net.minecraft.Util.getMillis();
        if (now - lastCheck < 1000) return;
        lastCheck = now;
        takenOver = net.neoforged.fml.ModList.get().isLoaded("distanthorizons");
        boolean pack = false;
        try {   // API do Iris por reflexao (o mod nao depende do Iris)
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            pack = (Boolean) api.getMethod("isShaderPackInUse").invoke(inst);
        } catch (Throwable ignored) {
        }
        irisPack = pack;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) return;
        if (irisPack()) {   // com shaderpack: o terreno distante passa pelos shaders do pack (luz, agua, neblina do Complementary)
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) drawWithGameShaders(event, mc, false);
            else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) drawWithGameShaders(event, mc, true);
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        ShaderInstance sh = DioramaRenderer.shader;
        int rd = mc.options.getEffectiveRenderDistance();
        int far = MapOptions.farChunks;
        float farBlocks = far * 16f;
        Vec3 cam = event.getCamera().getPosition();

        // projecao com o mesmo campo de visao, mas indo bem mais longe
        Matrix4f vp = event.getProjectionMatrix();
        float m11 = vp.m11(), m00 = vp.m00();
        Matrix4f proj = new Matrix4f().perspective((float) (2.0 * Math.atan(1.0 / m11)), m11 / m00, 0.5f, farBlocks * 1.6f);
        Matrix4f view = new Matrix4f(event.getModelViewMatrix());
        Frustum frustum = new Frustum(view, proj);
        frustum.prepare(cam.x, cam.y, cam.z);

        Sun.update(mc);
        Sun.applyUniforms();
        float[] fog = com.bino.binosvanillaplus.client.atmosphere.AtmosphereState.valid() && MapOptions.atmosphere >= 1
                ? com.bino.binosvanillaplus.client.atmosphere.AtmosphereState.fog : MapAtmosphere.provider.fogColor();
        sh.safeGetUniform("MapFogColor").set(fog[0], fog[1], fog[2]);
        sh.safeGetUniform("MapFogStart").set(farBlocks * 0.62f);
        sh.safeGetUniform("MapFogEnd").set(farBlocks * 0.98f);
        sh.safeGetUniform("Quality").set(2f);          // luz parecida com a do jogo (para emendar com o terreno de verdade)
        sh.safeGetUniform("ShadowOn").set(0f);
        sh.safeGetUniform("Detail").set(1f);
        sh.safeGetUniform("Radius").set(1.0e7f);
        sh.safeGetUniform("Exposure").set(1f);
        sh.safeGetUniform("Offset").set(0f, 0f, 0f);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int ccx = (int) Math.floor(cam.x / 16), ccz = (int) Math.floor(cam.z / 16);
        int[] budget = {BUILD_PER_FRAME};
        java.util.List<long[]> water = new java.util.ArrayList<>();
        for (int dz = -far; dz <= far; dz++) {
            for (int dx = -far; dx <= far; dx++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > far * far) continue;
                int cx = ccx + dx, cz = ccz + dz;
                // o jogo ja desenha esse chunk: pula
                if (d2 <= (rd - 1) * (rd - 1) && mc.level.getChunkSource().getChunkNow(cx, cz) != null) continue;
                if (ExploredMap.tile(cx, cz) == null) continue;
                if (!frustum.isVisible(new AABB(cx * 16, mc.level.getMinBuildHeight(), cz * 16, cx * 16 + 16, mc.level.getMaxBuildHeight(), cz * 16 + 16))) continue;
                WorldMap3D.ChunkMesh m = WorldMap3D.mesh(cx, cz, budget);
                if (m == null) continue;
                if (m.has) draw(m.vb, sh, view, proj, cx, cz, cam);
                if (m.hasW) water.add(new long[]{cx, cz});
            }
        }
        RenderSystem.depthMask(false);
        for (long[] c : water) {
            WorldMap3D.ChunkMesh m = WorldMap3D.mesh((int) c[0], (int) c[1], new int[]{0});
            if (m != null && m.hasW) draw(m.wvb, sh, view, proj, (int) c[0], (int) c[1], cam);
        }
        VertexBuffer.unbind();
        RenderSystem.depthMask(true);
        sh.safeGetUniform("Quality").set(0f);
        // o terreno de verdade sempre por cima do terreno distante
        RenderSystem.clear(256, Minecraft.ON_OSX);
        RenderSystem.enableCull();
    }

    // Caminho com shaderpack (Iris): desenha as malhas com os shaders de TERRENO do jogo (cutout para blocos, translucent
    // para agua). Com shaderpack ligado, o Iris troca esses shaders pelos programas do pack (gbuffers_terrain / gbuffers_water),
    // entao o terreno distante recebe a luz, a agua e a neblina do Complementary. Usa a mesma projecao do jogo, entao o
    // alcance fica limitado ao "fundo" da camera do jogo (4x a render distance).
    private static void drawWithGameShaders(RenderLevelStageEvent event, Minecraft mc, boolean water) {
        int rd = mc.options.getEffectiveRenderDistance();
        int far = Math.min(MapOptions.farChunks, rd * 4 - 1);
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f view = new Matrix4f(event.getModelViewMatrix());
        Matrix4f proj = new Matrix4f(event.getProjectionMatrix());
        Frustum frustum = event.getFrustum();
        net.minecraft.client.renderer.RenderType type = water ? net.minecraft.client.renderer.RenderType.translucent()
                : net.minecraft.client.renderer.RenderType.cutoutMipped();
        type.setupRenderState();
        ShaderInstance sh = RenderSystem.getShader();
        if (sh == null) { type.clearRenderState(); return; }
        if (sh.CHUNK_OFFSET != null) sh.CHUNK_OFFSET.set(0f, 0f, 0f);
        int ccx = (int) Math.floor(cam.x / 16), ccz = (int) Math.floor(cam.z / 16);
        int[] budget = {water ? 0 : BUILD_PER_FRAME};
        for (int dz = -far; dz <= far; dz++) {
            for (int dx = -far; dx <= far; dx++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > far * far) continue;
                int cx = ccx + dx, cz = ccz + dz;
                if (d2 <= (rd - 1) * (rd - 1) && mc.level.getChunkSource().getChunkNow(cx, cz) != null) continue;
                if (ExploredMap.tile(cx, cz) == null) continue;
                if (frustum != null && !frustum.isVisible(new AABB(cx * 16, mc.level.getMinBuildHeight(), cz * 16, cx * 16 + 16, mc.level.getMaxBuildHeight(), cz * 16 + 16))) continue;
                WorldMap3D.ChunkMesh m = WorldMap3D.mesh(cx, cz, budget);
                if (m == null) continue;
                VertexBuffer vb = water ? (m.hasW ? m.wvb : null) : (m.has ? m.vb : null);
                if (vb == null) continue;
                Matrix4f mv = new Matrix4f(view).translate((float) (cx * 16 - cam.x), (float) (64 - cam.y), (float) (cz * 16 - cam.z));
                vb.bind();
                vb.drawWithShader(mv, proj, sh);
            }
        }
        VertexBuffer.unbind();
        type.clearRenderState();
    }

    private static void draw(VertexBuffer vb, ShaderInstance sh, Matrix4f view, Matrix4f proj, int cx, int cz, Vec3 cam) {
        float ox = (float) (cx * 16 - cam.x), oy = (float) (64 - cam.y), oz = (float) (cz * 16 - cam.z);
        sh.safeGetUniform("Origin").set((float) (cx * 16), 64f, (float) (cz * 16));
        sh.safeGetUniform("ShadowOrigin").set(ox, oy, oz);
        vb.bind();
        vb.drawWithShader(new Matrix4f(view).translate(ox, oy, oz), proj, sh);
    }

    // (a neblina do jogo e' empurrada para o fim da distancia extra em atmosphere/AtmosphereFog.onRenderFog)
}

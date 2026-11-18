package com.bino.binosvanillaplus.client.gear;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Contorno da forja (+4, +6, +8, +10) no item 3D: na mao (1a e 3a pessoa), no chao e em molduras.
//
// Como funciona (codigo proprio): o modelo do item e' desenhado de novo 8 vezes com uma COR LISA (so o recorte
// de transparencia da textura e' usado), cada copia deslocada um pouquinho numa direcao da tela e empurrada para
// tras do item. O item de verdade cobre o miolo das copias e sobra so a borda = o contorno.
// A cor de cada direcao segue a rampa dos contornos 2D do Bino: branco em cima-direita, indo para a cor
// principal do item (ForgeGlow.color) nos outros lados. O nivel muda espessura e opacidade; o +10 ganha um
// halo externo fraco. Funciona igual para sprite 2D e para os modelos 3D das ferramentas.
public final class ForgeOutline {
    private ForgeOutline() {}

    // ====== AJUSTES ======
    // por faixa (+4, +6, +8, +10): opacidade e espessura (multiplicador)
    private static final float[] ALPHA = {0f, 0.45f, 0.65f, 0.85f, 1.0f};
    private static final float[] WIDTH = {0f, 0.75f, 0.90f, 1.05f, 1.25f};
    private static final float WIDTH_HAND = 0.0045f;    // espessura base em primeira pessoa (em blocos, perto da camera)
    private static final float WIDTH_WORLD = 0.011f;    // espessura base no mundo (3a pessoa, chao, moldura)
    private static final float PUSH_HAND = 0.03f, PUSH_WORLD = 0.05f;   // quanto as copias vao para tras do item
    private static final float HALO_ALPHA = 0.22f;      // halo extra do +10

    // 8 direcoes (graus, 0 = direita, 90 = cima) em ordem: as mais brancas primeiro (ganham nas quinas)
    private static final int[] DIRS = {45, 90, 0, 135, 315, 180, 270, 225};

    public static void render(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps,
                              MultiBufferSource buf, BakedModel model) {
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.NONE) return;   // icone: ForgeIconOutline
        int tier = ForgeGlow.tier(stack);
        if (tier == 0 || model == null || model.isCustomRenderer()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean hand = ctx.firstPerson();
        // pulsacao lenta (3 s) so na espessura
        double t = (Util.getMillis() % 3000L) / 3000.0;
        float pulse = 0.92f + 0.08f * (float) Math.sin(t * Math.PI * 2);
        float w = (hand ? WIDTH_HAND : WIDTH_WORLD) * WIDTH[tier] * pulse;
        float push = hand ? PUSH_HAND : PUSH_WORLD;
        // na 1a pessoa a pose ja esta no espaco da camera; no mundo a rotacao da camera fica fora da pose
        Quaternionf toWorld = hand ? new Quaternionf() : new Quaternionf(mc.gameRenderer.getMainCamera().rotation());

        int rgb = ForgeGlow.color(stack.getItem());
        float br = ((rgb >> 16) & 255) / 255f, bg = ((rgb >> 8) & 255) / 255f, bb = (rgb & 255) / 255f;

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        ps.translate(-0.5f, -0.5f, -0.5f);
        Matrix4f base = new Matrix4f(ps.last().pose());
        PoseStack.Pose pose = ps.last();

        VertexConsumer vc = buf.getBuffer(type(tier, false));
        for (int i = 0; i < DIRS.length; i++) {
            double a = Math.toRadians(DIRS[i]);
            // rampa: 1 = lado do brilho (cima-direita), 0 = lado oposto
            float k = (float) ((Math.cos(a - Math.toRadians(45)) + 1) / 2);
            float white = k * k * 0.9f;
            float r = br + (1 - br) * white, g = bg + (1 - bg) * white, b = bb + (1 - bb) * white;
            copy(vc, pose, base, shown, stack, toWorld, (float) Math.cos(a) * w, (float) Math.sin(a) * w, push + i * 0.0008f, r, g, b);
        }
        if (tier >= 4) {   // halo do +10: mais largo, fraco, na cor principal
            VertexConsumer halo = buf.getBuffer(type(tier, true));
            for (int i = 0; i < DIRS.length; i++) {
                double a = Math.toRadians(DIRS[i]);
                copy(halo, pose, base, shown, stack, toWorld, (float) Math.cos(a) * w * 2.2f, (float) Math.sin(a) * w * 2.2f,
                        push * 1.5f + i * 0.0008f, br, bg, bb);
            }
        }
        ps.popPose();
    }

    private static final RandomSource RANDOM = RandomSource.create();

    // uma copia do modelo deslocada (dx, dy na tela, push para longe da camera)
    private static void copy(VertexConsumer vc, PoseStack.Pose pose, Matrix4f base, BakedModel model, ItemStack stack,
                             Quaternionf toWorld, float dx, float dy, float push, float r, float g, float b) {
        Vector3f off = new Vector3f(dx, dy, -push).rotate(toWorld);
        pose.pose().set(base).mulLocal(new Matrix4f().translation(off));   // desloca em relacao a camera
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (Direction d : Direction.values()) quads(vc, pose, pass, d, r, g, b);
            quads(vc, pose, pass, null, r, g, b);
        }
        pose.pose().set(base);
    }

    private static void quads(VertexConsumer vc, PoseStack.Pose pose, BakedModel model, Direction side, float r, float g, float b) {
        RANDOM.setSeed(42L);
        for (BakedQuad q : model.getQuads(null, side, RANDOM))
            vc.putBulkData(pose, q, r, g, b, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    // ---------------------------------------------------------------- tipo de desenho
    // Usa o shader do proprio jogo para contornos (rendertype_outline): cor lisa da vertice, recorte pela
    // transparencia da textura. Desenha na tela normal (nao no buffer de "Brilhante"), sem luz, com profundidade.
    // A opacidade vem do ColorModulator (um tipo por faixa).
    private static final RenderType[] TYPES = new RenderType[5], HALOS = new RenderType[5];

    private static RenderType type(int tier, boolean halo) {
        RenderType[] arr = halo ? HALOS : TYPES;
        if (arr[tier] == null) {
            float alpha = halo ? HALO_ALPHA : ALPHA[tier];
            arr[tier] = new RenderType("binosvanillaplus_forge_outline_" + tier + (halo ? "_halo" : ""),
                    DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
                    () -> {
                        RenderSystem.setShader(GameRenderer::getRendertypeOutlineShader);
                        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                        RenderSystem.disableCull();
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(515);   // GL_LEQUAL
                        RenderSystem.depthMask(!halo);  // contorno grava profundidade (uma copia por pixel); halo nao
                        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
                    },
                    () -> {
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                        RenderSystem.depthMask(true);
                        RenderSystem.enableCull();
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }) {};
        }
        return arr[tier];
    }
}

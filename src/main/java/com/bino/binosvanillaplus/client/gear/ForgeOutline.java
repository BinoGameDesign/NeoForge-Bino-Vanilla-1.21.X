package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Method;

// Contorno da forja (+4, +6, +8, +10): silhueta 2D na TELA, como o contorno "brilhando" dos mobs, mas feito por nos
// (vale tambem para o item na mao em primeira pessoa e funciona com shaderpack). Codigo proprio.
//  1) Cada item forjado desenhado em 3D (mao, chao, moldura) e' enviado TAMBEM para um tipo de desenho proprio
//     (MASK) que, na hora em que o jogo desenha o lote, grava a silhueta numa textura escondida (a mascara) com cor
//     lisa (rgb = cor do item, alfa = faixa). Como vai junto com o item, usa a mesma projecao dele (fica alinhado
//     mesmo quando o shaderpack desenha a mao com projecao propria).
//     O corpo de quem segura entra como "parede" (alfa ~0): esconde a parte do item atras dele e a borda nao passa
//     por cima do corpo.
//  2) Antes do HUD, o shader forge_edge le a mascara e pinta em volta da silhueta a borda de 8 px (degrade
//     branco -> cor) e um brilho suave. So na area dos itens (calculada aproximada no Java, com folga).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ForgeOutline {
    private ForgeOutline() {}

    public static ShaderInstance maskShader, edgeShader;   // registrados em MapClientEvents.registerShaders

    private static final float MARGIN_PX = 8f + 18f + 40f;   // borda + brilho + folga, em 1080p
    private static RenderTarget mask;
    private static boolean any;
    private static float minX, minY, maxX, maxY;              // area aproximada na tela (-1..1)
    private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(256);
    private static final RandomSource RANDOM = RandomSource.create();

    // ---------------------------------------------------------------- tipos de desenho da mascara
    private static int prevDraw, prevRead;

    private static RenderType maskType(String name, int depthFunc) {
        return new RenderType("binosvanillaplus_forge_mask_" + name, DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS, 4096, false, false,
                () -> {
                    prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                    prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
                    mask.bindWrite(false);
                    RenderSystem.setShader(() -> maskShader);
                    RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                    RenderSystem.disableBlend();
                    RenderSystem.disableCull();
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(depthFunc);
                    RenderSystem.depthMask(true);
                },
                () -> {
                    GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
                    GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.enableCull();
                }) {};
    }

    private static final RenderType WORLD = maskType("world", GL11.GL_LEQUAL);
    private static final RenderType HAND = maskType("hand", GL11.GL_ALWAYS);   // mao: sempre na frente

    private static boolean ready() {
        return maskShader != null && edgeShader != null && mask != null && !irisShadowPass();
    }

    // ---------------------------------------------------------------- 1) item
    public static void render(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps,
                              MultiBufferSource buf, BakedModel model) {
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.NONE) return;   // icone: ForgeIconOutline
        int tier = ForgeGlow.tier(stack);
        if (tier == 0 || model == null || model.isCustomRenderer() || !ready()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        ps.translate(-0.5f, -0.5f, -0.5f);
        PoseStack.Pose pose = ps.last();
        boolean hand = ctx.firstPerson();
        if (hand || !hidden(mc, pose)) {
            VertexConsumer vc = buf.getBuffer(hand ? HAND : WORLD);
            Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(pose.pose());
            int rgb = ForgeGlow.color(stack.getItem());
            float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f, a = tier / 4f;
            for (BakedModel pass : shown.getRenderPasses(stack, true)) {
                for (Direction d : Direction.values()) quads(vc, pose, mvp, pass, d, r, g, b, a);
                quads(vc, pose, mvp, pass, null, r, g, b, a);
            }
            any = true;
        }
        ps.popPose();
    }

    private static void quads(VertexConsumer vc, PoseStack.Pose pose, Matrix4f mvp, BakedModel model, Direction side,
                              float r, float g, float b, float a) {
        RANDOM.setSeed(42L);
        Vector4f p = new Vector4f();
        for (BakedQuad q : model.getQuads(null, side, RANDOM)) {
            int[] v = q.getVertices();
            int stride = v.length / 4;
            for (int k = 0; k < 4; k++) {
                int o = k * stride;
                float x = Float.intBitsToFloat(v[o]), y = Float.intBitsToFloat(v[o + 1]), z = Float.intBitsToFloat(v[o + 2]);
                vc.addVertex(pose, x, y, z).setUv(Float.intBitsToFloat(v[o + 4]), Float.intBitsToFloat(v[o + 5])).setColor(r, g, b, a);
                p.set(x, y, z, 1f);
                mvp.transform(p);
                if (p.w > 0.01f) {
                    float sx = p.x / p.w, sy = p.y / p.w;
                    minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
                    minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
                }
            }
        }
    }

    // item no mundo escondido atras de bloco? (a pose do item e' relativa a camera)
    private static boolean hidden(Minecraft mc, PoseStack.Pose pose) {
        Vector3f p = pose.pose().transformPosition(new Vector3f(0.5f, 0.5f, 0.5f));
        float dist = p.length();
        if (dist > 64) return true;
        if (dist < 1.5f) return false;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 from = cam.getPosition(), to = from.add(p.x, p.y, p.z);
        HitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(from) < dist - 0.4;
    }

    // ---------------------------------------------------------------- corpo de quem segura/veste (bloqueio)
    public static boolean wantsBlocker(LivingEntity e) {
        if (!ready() || e.isInvisible()) return false;
        if (ForgeGlow.tier(e.getMainHandItem()) > 0 || ForgeGlow.tier(e.getOffhandItem()) > 0) return true;
        for (ItemStack a : e.getArmorSlots()) if (ForgeGlow.tier(a) > 0) return true;
        return false;
    }

    // corpo: sem textura (uv < 0) e alfa minimo -> na mascara vira "parede"
    public static VertexConsumer blocker(MultiBufferSource buf) {
        return SOLID.to(buf.getBuffer(WORLD), 0f, 0f, 0f, 1f / 255f);
    }

    // peca de armadura vestida: o modelo da armadura entra na mascara com a cor e a faixa da peca
    public static VertexConsumer armor(MultiBufferSource buf, ItemStack stack) {
        if (!ready()) return null;
        int tier = ForgeGlow.tier(stack);
        if (tier == 0) return null;
        int rgb = ForgeGlow.color(stack.getItem());
        any = true;
        return SOLID.to(buf.getBuffer(WORLD), ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, tier / 4f);
    }

    private static final Solid SOLID = new Solid();

    // repassa so a posicao, com cor fixa e sem textura; anota a area na tela para o shader da borda
    private static final class Solid implements VertexConsumer {
        VertexConsumer out;
        float r, g, b, a;
        final Matrix4f mvp = new Matrix4f();
        final Vector4f p = new Vector4f();

        Solid to(VertexConsumer out, float r, float g, float b, float a) {
            this.out = out; this.r = r; this.g = g; this.b = b; this.a = a;
            mvp.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
            return this;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            out.addVertex(x, y, z).setUv(-1f, -1f).setColor(r, g, b, a);
            if (a > 0.1f) {
                p.set(x, y, z, 1f);
                mvp.transform(p);
                if (p.w > 0.01f) {
                    float sx = p.x / p.w, sy = p.y / p.w;
                    minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
                    minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
                }
            }
            return this;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }

    // ---------------------------------------------------------------- inicio do quadro: limpa a mascara
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (mask == null) mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        else if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height, Minecraft.ON_OSX);
        mask.setClearColor(0f, 0f, 0f, 0f);
        mask.clear(Minecraft.ON_OSX);
        main.bindWrite(true);
        any = false;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    // ---------------------------------------------------------------- 2) borda na tela (antes do HUD)
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        if (!any || edgeShader == null || mask == null) return;
        any = false;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        main.bindWrite(true);
        float scale = Math.max(0.6f, main.height / 1080f);
        // area dos itens com folga (10% do tamanho + margem): a conta no Java e' aproximada
        float fx = (maxX - minX) * 0.1f, fy = (maxY - minY) * 0.1f;
        float mx = MARGIN_PX * scale * 2f / main.width + fx, my = MARGIN_PX * scale * 2f / main.height + fy;
        float x0 = Math.max(-1f, minX - mx), x1 = Math.min(1f, maxX + mx), y0 = Math.max(-1f, minY - my), y1 = Math.min(1f, maxY + my);
        if (!(x0 < x1 && y0 < y1)) return;
        RenderSystem.setShader(() -> edgeShader);
        RenderSystem.setShaderTexture(0, mask.getColorTextureId());
        edgeShader.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
        edgeShader.safeGetUniform("Scale").set(scale);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        BufferBuilder q = new BufferBuilder(BYTES, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        q.addVertex(x0, y0, 0f);
        q.addVertex(x1, y0, 0f);
        q.addVertex(x1, y1, 0f);
        q.addVertex(x0, y1, 0f);
        BufferUploader.drawWithShader(q.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ---------------------------------------------------------------- Iris: nao gravar na passada de sombra
    private static Object irisApi;
    private static Method irisShadow;
    private static boolean irisChecked;

    private static boolean irisShadowPass() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = c.getMethod("getInstance").invoke(null);
                irisShadow = c.getMethod("isRenderingShadowPass");
            } catch (Throwable ignored) { irisApi = null; }
        }
        if (irisApi == null) return false;
        try { return (Boolean) irisShadow.invoke(irisApi); } catch (Throwable t) { return false; }
    }

    public static void clearCache() {}
}

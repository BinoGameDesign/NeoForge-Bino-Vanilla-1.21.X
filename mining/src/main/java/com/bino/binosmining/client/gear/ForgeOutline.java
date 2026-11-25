package com.bino.binosmining.client.gear;

import com.bino.binosmining.BinosMining;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
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
import net.minecraft.resources.ResourceLocation;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

// Contorno da forja (+4, +6, +8, +10): silhueta 2D na TELA, como o contorno "brilhando" dos mobs, mas feito por nos.
// Codigo proprio.
//  1) MUNDO (item na 3a pessoa, no chao, na moldura; armadura vestida; corpo de quem segura/veste): durante o quadro
//     so ANOTAMOS os vertices ja em coordenadas de tela, sem tocar no desenho do jogo (mexer nele deixava itens, pets e
//     o personagem invisiveis). Rgb = cor do item, alfa = faixa; o corpo entra com alfa ~0 ("parede").
//     MAO em 1a pessoa: vai num tipo de desenho proprio (HAND), junto com o item, porque o shaderpack desenha a mao com
//     projecao propria e so assim a silhueta fica alinhada. A mao e' desenhada depois do mundo, entao nao atrapalha nada.
//  2) Antes do HUD: as anotacoes vao para a mascara (textura escondida) e o shader forge_edge pinta em volta da
//     silhueta a borda de 8 px (degrade branco -> cor) e um brilho suave.
@EventBusSubscriber(modid = BinosMining.MOD_ID, value = Dist.CLIENT)
public final class ForgeOutline {
    private ForgeOutline() {}

    public static ShaderInstance maskShader, maskNdcShader, edgeShader;   // registrados em MapClientEvents.registerShaders

    private static final float MARGIN_PX = 8f + 18f + 40f;   // borda + brilho + folga, em 1080p
    private static RenderTarget mask;
    private static boolean any, hand;
    private static float minX, minY, maxX, maxY;              // area na tela (-1..1)
    private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(1 << 16);
    private static final RandomSource RANDOM = RandomSource.create();

    // ---------------------------------------------------------------- anotacoes (por textura)
    private static final int FLOATS = 9;   // x y z u v r g b a
    private static final class Batch {
        float[] v = new float[FLOATS * 4 * 64];
        int n;   // vertices
        void ensure(int more) { if ((n + more) * FLOATS > v.length) v = Arrays.copyOf(v, Math.max(v.length * 2, (n + more) * FLOATS)); }
    }
    private static final Map<ResourceLocation, Batch> BATCHES = new LinkedHashMap<>();

    // grava uma face de 4 vertices (posicoes antes da camera) ja convertida para a tela; pula se passar por tras da camera
    private static final float[] QUAD = new float[4 * FLOATS];
    private static final Vector4f P = new Vector4f();

    private static void quad(ResourceLocation tex, Matrix4f mvp) {
        for (int k = 0; k < 4; k++) {
            int o = k * FLOATS;
            P.set(QUAD[o], QUAD[o + 1], QUAD[o + 2], 1f);
            mvp.transform(P);
            if (P.w < 0.01f) return;
            QUAD[o] = P.x / P.w; QUAD[o + 1] = P.y / P.w; QUAD[o + 2] = (P.z / P.w) * 0.5f + 0.5f;
        }
        Batch b = BATCHES.computeIfAbsent(tex, t -> new Batch());
        b.ensure(4);
        System.arraycopy(QUAD, 0, b.v, b.n * FLOATS, 4 * FLOATS);
        b.n += 4;
        if (QUAD[8] > 0.1f) {   // so itens/armaduras contam para a area (o corpo nao)
            for (int k = 0; k < 4; k++) {
                float x = QUAD[k * FLOATS], y = QUAD[k * FLOATS + 1];
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
            any = true;
        }
    }

    private static Matrix4f cameraMvp() {
        return new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
    }

    private static boolean ready() {
        return maskShader != null && maskNdcShader != null && edgeShader != null && mask != null && !irisShadowPass();
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
        boolean first = ctx.firstPerson();
        if (first || !hidden(mc, pose)) {
            Matrix4f mvp = cameraMvp().mul(pose.pose());
            int rgb = ForgeGlow.color(stack.getItem());
            float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f, a = tier / 4f;
            VertexConsumer vc = first ? buf.getBuffer(HAND) : null;
            if (first) hand = true;
            for (BakedModel pass : shown.getRenderPasses(stack, true)) {
                for (Direction d : Direction.values()) itemQuads(vc, pose, mvp, pass, d, r, g, b, a);
                itemQuads(vc, pose, mvp, pass, null, r, g, b, a);
            }
        }
        ps.popPose();
    }

    private static void itemQuads(VertexConsumer vc, PoseStack.Pose pose, Matrix4f mvp, BakedModel model, Direction side,
                                  float r, float g, float b, float a) {
        RANDOM.setSeed(42L);
        Matrix4f cam = vc == null ? cameraMvp() : null;
        Vector3f p = new Vector3f();
        for (BakedQuad q : model.getQuads(null, side, RANDOM)) {
            int[] v = q.getVertices();
            int stride = v.length / 4;
            for (int k = 0; k < 4; k++) {
                int o = k * stride, i = k * FLOATS;
                float x = Float.intBitsToFloat(v[o]), y = Float.intBitsToFloat(v[o + 1]), z = Float.intBitsToFloat(v[o + 2]);
                float u = Float.intBitsToFloat(v[o + 4]), w = Float.intBitsToFloat(v[o + 5]);
                if (vc != null) {
                    // mao: silhueta pelo HAND; aqui so a area aproximada na tela
                    vc.addVertex(pose, x, y, z).setUv(u, w).setColor(r, g, b, a);
                    P.set(x, y, z, 1f);
                    mvp.transform(P);
                    if (P.w > 0.01f) {
                        float sx = P.x / P.w, sy = P.y / P.w;
                        minX = Math.min(minX, sx); maxX = Math.max(maxX, sx); minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
                    }
                    any = true;
                } else {
                    pose.pose().transformPosition(x, y, z, p);
                    QUAD[i] = p.x; QUAD[i + 1] = p.y; QUAD[i + 2] = p.z; QUAD[i + 3] = u; QUAD[i + 4] = w;
                    QUAD[i + 5] = r; QUAD[i + 6] = g; QUAD[i + 7] = b; QUAD[i + 8] = a;
                }
            }
            if (vc == null) quad(TextureAtlas.LOCATION_BLOCKS, cam);
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

    // ---------------------------------------------------------------- corpo (bloqueio) e armadura vestida
    public static boolean wantsBlocker(LivingEntity e) {
        if (!ready() || e.isInvisible()) return false;
        if (ForgeGlow.tier(e.getMainHandItem()) > 0 || ForgeGlow.tier(e.getOffhandItem()) > 0) return true;
        for (ItemStack a : e.getArmorSlots()) if (ForgeGlow.tier(a) > 0) return true;
        return false;
    }

    // corpo: anotado sem textura (uv < 0) e com alfa minimo -> "parede" na mascara. Nao usa o buffer do jogo.
    public static VertexConsumer blocker(MultiBufferSource buf) {
        return REC.start(TextureAtlas.LOCATION_BLOCKS, false, 0f, 0f, 0f, 1f / 255f);
    }

    // peca de armadura vestida: anotada com a textura da armadura (partes vazadas ficam fora), a cor e a faixa da peca
    public static VertexConsumer armor(MultiBufferSource buf, ItemStack stack, ResourceLocation texture) {
        if (!ready()) return null;
        int tier = ForgeGlow.tier(stack);
        if (tier == 0) return null;
        int rgb = ForgeGlow.color(stack.getItem());
        return REC.start(texture, true, ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, tier / 4f);
    }

    private static final Recorder REC = new Recorder();

    // recebe os vertices de um modelo de entidade (ModelPart) e anota de 4 em 4
    private static final class Recorder implements VertexConsumer {
        ResourceLocation tex;
        boolean keepUv;
        float r, g, b, a;
        Matrix4f mvp;
        int k;

        Recorder start(ResourceLocation tex, boolean keepUv, float r, float g, float b, float a) {
            this.tex = tex; this.keepUv = keepUv; this.r = r; this.g = g; this.b = b; this.a = a;
            this.mvp = cameraMvp();
            this.k = 0;
            return this;
        }

        // o ModelPart manda tudo de uma vez por aqui
        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            put(x, y, z, keepUv ? u : -1f, keepUv ? v : -1f);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            put(x, y, z, -1f, -1f);
            return this;
        }

        private void put(float x, float y, float z, float u, float v) {
            int i = k * FLOATS;
            QUAD[i] = x; QUAD[i + 1] = y; QUAD[i + 2] = z; QUAD[i + 3] = u; QUAD[i + 4] = v;
            QUAD[i + 5] = r; QUAD[i + 6] = g; QUAD[i + 7] = b; QUAD[i + 8] = a;
            if (++k == 4) { k = 0; quad(tex, mvp); }
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }

    // ---------------------------------------------------------------- mao: tipo de desenho que grava na mascara
    private static int prevDraw, prevRead;

    private static final RenderType HAND = new RenderType("binosmining_forge_mask_hand", DefaultVertexFormat.POSITION_TEX_COLOR,
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
                RenderSystem.depthFunc(GL11.GL_ALWAYS);   // mao grava a propria profundidade (perto): o mundo nao a cobre
                RenderSystem.depthMask(true);
            },
            () -> {
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.enableCull();
            }) {};

    // ---------------------------------------------------------------- inicio do quadro
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (mask == null) mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        else if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height, Minecraft.ON_OSX);
        mask.setClearColor(0f, 0f, 0f, 0f);
        mask.clear(Minecraft.ON_OSX);
        main.bindWrite(true);
        for (Batch b : BATCHES.values()) b.n = 0;
        any = hand = false;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    // ---------------------------------------------------------------- 2) mascara do mundo + borda (antes do HUD)
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        if (!any || maskNdcShader == null || edgeShader == null || mask == null) return;
        any = false;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();

        // mundo anotado -> mascara (a mao ja esta la e fica por cima: o mundo so entra onde ela nao esta)
        mask.bindWrite(true);
        RenderSystem.setShader(() -> maskNdcShader);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        for (var e : BATCHES.entrySet()) {
            Batch b = e.getValue();
            if (b.n == 0) continue;
            RenderSystem.setShaderTexture(0, e.getKey());
            BufferBuilder bb = new BufferBuilder(BYTES, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int v = 0; v < b.n; v++) {
                int i = v * FLOATS;
                bb.addVertex(b.v[i], b.v[i + 1], b.v[i + 2]).setUv(b.v[i + 3], b.v[i + 4]).setColor(b.v[i + 5], b.v[i + 6], b.v[i + 7], b.v[i + 8]);
            }
            MeshData mesh = bb.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();

        // borda + brilho, so na area dos itens (com folga; mais folga quando ha item na mao)
        main.bindWrite(true);
        float scale = Math.max(0.6f, main.height / 1080f);
        float extra = hand ? 0.25f : 0.1f;
        float fx = (maxX - minX) * extra, fy = (maxY - minY) * extra;
        float mx = MARGIN_PX * scale * 2f / main.width + fx, my = MARGIN_PX * scale * 2f / main.height + fy;
        float x0 = Math.max(-1f, minX - mx), x1 = Math.min(1f, maxX + mx), y0 = Math.max(-1f, minY - my), y1 = Math.min(1f, maxY + my);
        if (x0 < x1 && y0 < y1) {
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
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ---------------------------------------------------------------- Iris: nao anotar na passada de sombra
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

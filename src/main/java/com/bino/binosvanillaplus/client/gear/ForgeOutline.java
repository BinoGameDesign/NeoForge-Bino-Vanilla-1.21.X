package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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

import java.lang.reflect.Method;
import java.util.Arrays;

// Contorno da forja (+4, +6, +8, +10): silhueta 2D na TELA, como o contorno "brilhando" dos mobs, mas feito por nos
// (vale tambem para o item na mao em primeira pessoa e funciona com shaderpack). Codigo proprio.
//  1) Durante o quadro, cada item forjado desenhado em 3D (mao, chao, moldura) so e' ANOTADO: os vertices ja
//     convertidos para coordenadas de tela, com uma cor lisa (rgb = cor do item, alfa = faixa). Nada e' desenhado no
//     meio da renderizacao do mundo (isso atrapalhava o shaderpack: personagem sumia).
//  2) Antes do HUD: a silhueta anotada vai para uma textura escondida (mascara) e o shader forge_edge pinta em volta
//     dela uma borda de 8 px (degrade branco -> cor) e um brilho suave na cor do item.
// Item no mundo atras de parede nao ganha contorno (teste de visao da camera ate o item).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ForgeOutline {
    private ForgeOutline() {}

    public static ShaderInstance maskShader, edgeShader;   // registrados em MapClientEvents.registerShaders

    private static final float MARGIN_PX = 8f + 18f + 2f;   // borda + brilho (forge_edge.fsh), em 1080p
    private static final int FLOATS = 9;                     // x y z u v r g b a

    private static float[] verts = new float[FLOATS * 4 * 256];
    private static int count;                                // vertices anotados
    private static float minX, minY, maxX, maxY;             // area na tela (-1..1)
    private static RenderTarget mask;
    private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(1 << 16);
    private static final RandomSource RANDOM = RandomSource.create();

    // ---------------------------------------------------------------- 1) anotar
    public static void render(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps, BakedModel model) {
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.NONE) return;   // icone: ForgeIconOutline
        int tier = ForgeGlow.tier(stack);
        if (tier == 0 || model == null || model.isCustomRenderer() || maskShader == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || irisShadowPass()) return;

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        ps.translate(-0.5f, -0.5f, -0.5f);
        PoseStack.Pose pose = ps.last();
        boolean hand = ctx.firstPerson();
        if (hand || !hidden(mc, pose)) {
            Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(pose.pose());
            int rgb = ForgeGlow.color(stack.getItem());
            float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f, a = tier / 4f;
            for (BakedModel pass : shown.getRenderPasses(stack, true)) {
                for (Direction d : Direction.values()) quads(mvp, pass, d, hand, r, g, b, a);
                quads(mvp, pass, null, hand, r, g, b, a);
            }
        }
        ps.popPose();
    }

    private static void quads(Matrix4f mvp, BakedModel model, Direction side, boolean hand, float r, float g, float b, float a) {
        RANDOM.setSeed(42L);
        Vector4f p = new Vector4f();
        for (BakedQuad q : model.getQuads(null, side, RANDOM)) {
            int[] v = q.getVertices();
            int stride = v.length / 4;
            int start = count;
            ensure(count + 4);
            boolean ok = true;
            for (int k = 0; k < 4 && ok; k++) {
                int o = k * stride;
                p.set(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]), 1f);
                mvp.transform(p);
                if (p.w < 0.01f) { ok = false; break; }   // atras da camera: pula a face
                float x = p.x / p.w, y = p.y / p.w, z = p.z / p.w;
                z = hand ? z * 0.5f - 0.5f : z * 0.5f + 0.5f;   // mao sempre na frente do mundo
                int i = (start + k) * FLOATS;
                verts[i] = x; verts[i + 1] = y; verts[i + 2] = z;
                verts[i + 3] = Float.intBitsToFloat(v[o + 4]); verts[i + 4] = Float.intBitsToFloat(v[o + 5]);
                verts[i + 5] = r; verts[i + 6] = g; verts[i + 7] = b; verts[i + 8] = a;
            }
            if (!ok) continue;
            for (int k = 0; k < 4; k++) {
                int i = (start + k) * FLOATS;
                minX = Math.min(minX, verts[i]); maxX = Math.max(maxX, verts[i]);
                minY = Math.min(minY, verts[i + 1]); maxY = Math.max(maxY, verts[i + 1]);
            }
            count += 4;
        }
    }

    private static void ensure(int n) {
        if (n * FLOATS > verts.length) verts = Arrays.copyOf(verts, Math.max(verts.length * 2, n * FLOATS));
    }

    // ---------------------------------------------------------------- bloqueio: corpo de quem segura o item
    // O corpo entra na mascara so como "parede" (alfa minimo, sem textura): esconde a parte do item que fica atras
    // dele e impede a borda de ser pintada por cima dele.
    public static boolean wantsBlocker(net.minecraft.world.entity.LivingEntity e) {
        if (maskShader == null || irisShadowPass() || e.isInvisible()) return false;
        return ForgeGlow.tier(e.getMainHandItem()) > 0 || ForgeGlow.tier(e.getOffhandItem()) > 0;
    }

    private static final BlockerConsumer BLOCKER = new BlockerConsumer();

    public static com.mojang.blaze3d.vertex.VertexConsumer blocker() {
        BLOCKER.mvp.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        BLOCKER.n = 0;
        return BLOCKER;
    }

    // recebe os vertices do modelo (ja com a pose aplicada) e anota cada face de 4
    private static final class BlockerConsumer implements com.mojang.blaze3d.vertex.VertexConsumer {
        final Matrix4f mvp = new Matrix4f();
        final float[] buf = new float[12];
        int n;

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
            buf[n * 3] = x; buf[n * 3 + 1] = y; buf[n * 3 + 2] = z;
            if (++n == 4) {
                n = 0;
                ensure(count + 4);
                Vector4f p = new Vector4f();
                boolean ok = true;
                for (int k = 0; k < 4; k++) {
                    p.set(buf[k * 3], buf[k * 3 + 1], buf[k * 3 + 2], 1f);
                    mvp.transform(p);
                    if (p.w < 0.01f) { ok = false; break; }
                    int i = (count + k) * FLOATS;
                    verts[i] = p.x / p.w; verts[i + 1] = p.y / p.w; verts[i + 2] = (p.z / p.w) * 0.5f + 0.5f;
                    verts[i + 3] = -1f; verts[i + 4] = -1f;          // sem textura
                    verts[i + 5] = 0f; verts[i + 6] = 0f; verts[i + 7] = 0f; verts[i + 8] = 1f / 255f;
                }
                if (ok) count += 4;
            }
            return this;
        }

        @Override public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) { return this; }
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

    // ---------------------------------------------------------------- inicio do quadro
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        count = 0;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    // ---------------------------------------------------------------- 2) mascara + borda (antes do HUD)
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        if (count == 0 || maskShader == null || edgeShader == null) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (mask == null) mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        else if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height, Minecraft.ON_OSX);

        // mascara
        mask.setClearColor(0f, 0f, 0f, 0f);
        mask.clear(Minecraft.ON_OSX);
        mask.bindWrite(true);
        BufferBuilder bb = new BufferBuilder(BYTES, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int v = 0; v < count; v++) {
            int i = v * FLOATS;
            bb.addVertex(verts[i], verts[i + 1], verts[i + 2]).setUv(verts[i + 3], verts[i + 4])
                    .setColor(verts[i + 5], verts[i + 6], verts[i + 7], verts[i + 8]);
        }
        MeshData mesh = bb.build();
        if (mesh != null) {
            RenderSystem.setShader(() -> maskShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            BufferUploader.drawWithShader(mesh);
        }

        // borda + brilho, so na area em volta dos itens
        main.bindWrite(true);
        float scale = Math.max(0.6f, main.height / 1080f);
        float mx = MARGIN_PX * scale * 2f / main.width, my = MARGIN_PX * scale * 2f / main.height;
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
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        count = 0;
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

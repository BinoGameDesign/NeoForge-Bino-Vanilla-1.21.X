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
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Method;

// Contorno da forja (+4, +6, +8, +10): silhueta 2D na TELA, como o contorno "brilhando" dos mobs, mas feito por nos
// (vale tambem para o item na mao em primeira pessoa e funciona com shaderpack). Codigo proprio.
//  1) Cada item forjado desenhado em 3D (mao, chao, moldura) e' desenhado TAMBEM numa textura escondida (a mascara),
//     com uma cor lisa: rgb = cor do item, alfa = faixa (+4 = 0.25 ... +10 = 1.0).
//  2) Antes do HUD, o shader forge_edge le a mascara e pinta na tela os pixels de fora da silhueta que estao a ate
//     N pixels dela (N pela faixa). Cor: degrade branco (cima-direita) -> cor do item.
// Item no mundo atras de parede nao ganha contorno (teste de visao da camera ate o item).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ForgeOutline {
    private ForgeOutline() {}

    public static ShaderInstance maskShader, edgeShader;   // registrados em MapClientEvents.registerShaders

    private static RenderTarget mask;
    private static boolean dirty, handStarted;
    private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(1 << 16);
    private static final RandomSource RANDOM = RandomSource.create();

    // ---------------------------------------------------------------- 1) mascara
    public static void render(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps, BakedModel model) {
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.NONE) return;   // icone: ForgeIconOutline
        int tier = ForgeGlow.tier(stack);
        if (tier == 0 || model == null || model.isCustomRenderer() || maskShader == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mask == null || irisShadowPass()) return;

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        ps.translate(-0.5f, -0.5f, -0.5f);
        PoseStack.Pose pose = ps.last();
        if (!ctx.firstPerson() && hidden(mc, pose)) { ps.popPose(); return; }

        int rgb = ForgeGlow.color(stack.getItem());
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f, a = tier / 4f;
        BufferBuilder bb = new BufferBuilder(BYTES, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (BakedModel pass : shown.getRenderPasses(stack, true)) {
            for (Direction d : Direction.values()) quads(bb, pose, pass, d, r, g, b, a);
            quads(bb, pose, pass, null, r, g, b, a);
        }
        ps.popPose();
        MeshData mesh = bb.build();
        if (mesh == null) return;

        // desenha ja, na mascara, e devolve tudo como estava (inclusive o framebuffer do Iris, se houver)
        int prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        mask.bindWrite(false);
        if (ctx.firstPerson() && !handStarted) {   // a mao usa outra projecao: zera so a profundidade da mascara
            handStarted = true;
            RenderSystem.depthMask(true);
            RenderSystem.clearDepth(1.0);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
        RenderSystem.setShader(() -> maskShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.enableCull();
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        dirty = true;
    }

    private static void quads(BufferBuilder bb, PoseStack.Pose pose, BakedModel model, Direction side, float r, float g, float b, float a) {
        RANDOM.setSeed(42L);
        for (BakedQuad q : model.getQuads(null, side, RANDOM)) {
            int[] v = q.getVertices();
            int stride = v.length / 4;
            for (int k = 0; k < 4; k++) {
                int o = k * stride;
                bb.addVertex(pose, Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]))
                        .setUv(Float.intBitsToFloat(v[o + 4]), Float.intBitsToFloat(v[o + 5]))
                        .setColor(r, g, b, a);
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

    // ---------------------------------------------------------------- inicio do quadro: limpa a mascara
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (mask == null) {
            mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        } else if (mask.width != main.width || mask.height != main.height) {
            mask.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        mask.setClearColor(0f, 0f, 0f, 0f);
        mask.clear(Minecraft.ON_OSX);
        main.bindWrite(true);
        dirty = false;
        handStarted = false;
    }

    // ---------------------------------------------------------------- 2) borda na tela (antes do HUD)
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        if (!dirty || edgeShader == null || mask == null) return;
        dirty = false;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.setShader(() -> edgeShader);
        RenderSystem.setShaderTexture(0, mask.getColorTextureId());
        edgeShader.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
        edgeShader.safeGetUniform("Scale").set(Math.max(0.6f, main.height / 1080f));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        BufferBuilder bb = new BufferBuilder(BYTES, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.addVertex(-1f, -1f, 0f);
        bb.addVertex(1f, -1f, 0f);
        bb.addVertex(1f, 1f, 0f);
        bb.addVertex(-1f, 1f, 0f);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ---------------------------------------------------------------- Iris: nao desenhar na passada de sombra
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

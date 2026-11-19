package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Contorno da forja (+4, +6, +8, +10) no item 3D: na mao (1a e 3a pessoa), no chao e em molduras. Codigo proprio.
//
// Como funciona: o modelo do item e' desenhado de novo varias vezes com uma COR LISA, cada copia um pouco deslocada
// numa direcao da tela e "atras" do item (afastada da camera sem mudar de tamanho na tela). O item de verdade cobre o
// miolo e sobra so a borda. Para a cor ser lisa (e nao a textura tingida) cada textura do modelo ganha uma copia
// "silhueta": branca, so com a transparencia da original. Assim da para usar um tipo de desenho normal do jogo
// (entityTranslucentEmissive), que tambem funciona com shaderpack (Iris).
//
// Visual: borda grossa e clara (branco no lado de cima-direita indo para um tom claro da cor do item nos outros
// lados) + halo suave na cor do item. O nivel muda espessura, opacidade e o tamanho do halo.
public final class ForgeOutline {
    private ForgeOutline() {}

    // ====== AJUSTES (medidas em PIXELS DO ITEM, entao vale igual na mao, no chao e na moldura) ======
    private static final float[] WIDTH = {0f, 0.45f, 0.60f, 0.75f, 0.90f};        // borda: +4, +6, +8, +10
    private static final float[] ALPHA = {0f, 0.70f, 0.85f, 0.95f, 1.00f};
    private static final float[] HALO = {0f, 0.0f, 1.4f, 2.0f, 2.8f};             // largura do halo (0 = sem halo)
    private static final float[] HALO_ALPHA = {0f, 0f, 0.10f, 0.14f, 0.18f};
    private static final float TINT = 0.40f;    // quanto da cor do item entra na borda (0 = branca, 1 = cor pura)
    private static final float PUSH_PX = 3f;    // quanto as copias vao para tras do item

    // 8 direcoes (graus, 0 = direita, 90 = cima); a ultima desenhada fica por cima nas quinas: o brilho por ultimo
    private static final int[] DIRS = {225, 270, 180, 315, 135, 0, 90, 45};

    public static void render(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack ps,
                              MultiBufferSource buf, BakedModel model) {
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.NONE) return;   // icone: ForgeIconOutline
        int tier = ForgeGlow.tier(stack);
        if (tier == 0 || model == null || model.isCustomRenderer()) return;

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        ps.translate(-0.5f, -0.5f, -0.5f);
        PoseStack.Pose pose = ps.last();
        Matrix4f base = new Matrix4f(pose.pose());

        Map<TextureAtlasSprite, List<BakedQuad>> bySprite = collect(shown, stack);
        if (bySprite.isEmpty()) { ps.popPose(); return; }

        // 1 pixel do item em blocos (a pose ja inclui a escala do "display" e da entidade)
        float px = base.getScale(new Vector3f()).x / 16f;
        // centro do item em relacao a camera: a pose do item ja e' relativa a camera (mao e mundo)
        Vector3f center = base.transformPosition(new Vector3f(0.5f, 0.5f, 0.5f));
        float dist = Math.max(0.05f, center.length());
        Vector3f toCam = new Vector3f(center).normalize();
        // direita e cima da tela, no mesmo espaco da pose
        Vector3f right, up;
        if (ctx.firstPerson()) { right = new Vector3f(1, 0, 0); up = new Vector3f(0, 1, 0); }
        else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            right = new Vector3f(cam.getLeftVector()).negate();
            up = new Vector3f(cam.getUpVector());
        }
        // afasta da camera sem mudar o tamanho na tela: escala em volta da camera
        float k = 1f + PUSH_PX * px / dist;

        double t = (Util.getMillis() % 3000L) / 3000.0;
        float pulse = 0.93f + 0.07f * (float) Math.sin(t * Math.PI * 2);

        int rgb = ForgeGlow.color(stack.getItem());
        float br = ((rgb >> 16) & 255) / 255f, bg = ((rgb >> 8) & 255) / 255f, bb = (rgb & 255) / 255f;

        // halo (atras da borda: um pouco mais longe da camera)
        if (HALO[tier] > 0) {
            float kh = 1f + PUSH_PX * 1.6f * px / dist;
            for (float ring : new float[]{0.55f, 1f}) {
                float w = HALO[tier] * ring * px * pulse;
                for (int dir : DIRS)
                    copy(buf, pose, base, bySprite, right, up, dir, w, kh, br, bg, bb, HALO_ALPHA[tier]);
            }
        }
        // borda
        float w = WIDTH[tier] * px * pulse;
        for (int dir : DIRS) {
            double a = Math.toRadians(dir);
            float s = (float) ((Math.cos(a - Math.toRadians(45)) + 1) / 2);   // 1 = lado do brilho
            float tint = TINT * (1 - s * s);
            copy(buf, pose, base, bySprite, right, up, dir, w, k,
                    1 - (1 - br) * tint, 1 - (1 - bg) * tint, 1 - (1 - bb) * tint, ALPHA[tier]);
        }
        pose.pose().set(base);
        ps.popPose();
    }

    private static void copy(MultiBufferSource buf, PoseStack.Pose pose, Matrix4f base, Map<TextureAtlasSprite, List<BakedQuad>> bySprite,
                             Vector3f right, Vector3f up, int dirDeg, float w, float k, float r, float g, float b, float a) {
        double rad = Math.toRadians(dirDeg);
        Vector3f off = new Vector3f(right).mul((float) Math.cos(rad) * w * k).add(new Vector3f(up).mul((float) Math.sin(rad) * w * k));
        pose.pose().set(base).mulLocal(new Matrix4f().scaling(k)).mulLocal(new Matrix4f().translation(off));
        for (var e : bySprite.entrySet()) {
            ResourceLocation sil = silhouette(e.getKey());
            if (sil == null) continue;
            VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(sil));
            TextureAtlasSprite sp = e.getKey();
            float u0 = sp.getU0(), du = sp.getU1() - u0, v0 = sp.getV0(), dv = sp.getV1() - v0;
            for (BakedQuad q : e.getValue()) {
                int[] d = q.getVertices();
                int stride = d.length / 4;
                for (int i = 0; i < 4; i++) {
                    int o = i * stride;
                    float x = Float.intBitsToFloat(d[o]), y = Float.intBitsToFloat(d[o + 1]), z = Float.intBitsToFloat(d[o + 2]);
                    float u = (Float.intBitsToFloat(d[o + 4]) - u0) / du, v = (Float.intBitsToFloat(d[o + 5]) - v0) / dv;
                    vc.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, v)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
                }
            }
        }
    }

    private static final RandomSource RANDOM = RandomSource.create();

    private static Map<TextureAtlasSprite, List<BakedQuad>> collect(BakedModel model, ItemStack stack) {
        Map<TextureAtlasSprite, List<BakedQuad>> out = new LinkedHashMap<>();
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (Direction d : Direction.values()) add(out, pass, d);
            add(out, pass, null);
        }
        return out;
    }

    private static void add(Map<TextureAtlasSprite, List<BakedQuad>> out, BakedModel m, Direction side) {
        RANDOM.setSeed(42L);
        for (BakedQuad q : m.getQuads(null, side, RANDOM)) out.computeIfAbsent(q.getSprite(), s -> new ArrayList<>()).add(q);
    }

    // ---------------------------------------------------------------- silhuetas (textura branca com o alfa original)
    private static final Map<ResourceLocation, ResourceLocation> SILHOUETTES = new HashMap<>();
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "none");

    public static void clearCache() {
        for (ResourceLocation r : SILHOUETTES.values())
            if (r != NONE) Minecraft.getInstance().getTextureManager().release(r);
        SILHOUETTES.clear();
    }

    private static ResourceLocation silhouette(TextureAtlasSprite sprite) {
        ResourceLocation name = sprite.contents().name();
        ResourceLocation r = SILHOUETTES.computeIfAbsent(name, ForgeOutline::build);
        return r == NONE ? null : r;
    }

    private static ResourceLocation build(ResourceLocation name) {
        int w = 0, h = 0;
        var res = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(name.getNamespace(), "textures/" + name.getPath() + ".png"));
        if (res.isEmpty()) return NONE;
        try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            w = img.getWidth();
            h = Math.min(img.getHeight(), w);   // textura animada: so o primeiro quadro
            NativeImage out = new NativeImage(w, h, true);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int a = img.getPixelRGBA(x, y) >>> 24;
                    out.setPixelRGBA(x, y, a < 16 ? 0 : 0xFFFFFFFF);
                }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID,
                    "forge_silhouette/" + name.getNamespace() + "/" + name.getPath());
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
            return id;
        } catch (Exception e) {
            return NONE;
        }
    }
}

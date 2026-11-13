package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.util.ForgeUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Brilho de ressonancia da forja: equipamento +4 ou mais emite um brilho suave na cor principal do item
// (+4 de leve, +6 mais forte, +8 mais, +10 o maximo), pulsando devagar. Item na mao: ItemInHandRendererForgeGlowMixin.
// Armadura vestida: ForgeArmorGlowLayer. E' so visual (nao ilumina o chao).
public final class ForgeGlow {
    private ForgeGlow() {}

    // opacidade do brilho por faixa (+4, +6, +8, +10)
    private static final float[] ALPHA = {0f, 0.13f, 0.20f, 0.28f, 0.36f};

    public static int tier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int lv = ForgeUtil.getLevel(stack);
        return lv >= 10 ? 4 : lv >= 8 ? 3 : lv >= 6 ? 2 : lv >= 4 ? 1 : 0;
    }

    // opacidade da faixa com uma pulsacao lenta (ciclo de 3 s, +-15%)
    public static float alpha(int tier) {
        double t = (Util.getMillis() % 3000L) / 3000.0;
        return ALPHA[tier] * (float) (0.85 + 0.15 * Math.sin(t * Math.PI * 2));
    }

    // cor ARGB do brilho com a opacidade dada
    public static int argb(ItemStack stack, float alpha) {
        int rgb = color(stack.getItem());
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | rgb;
    }

    // ---------------------------------------------------------------- cor principal do item (lida do sprite 2D)
    private static final Map<Item, Integer> COLORS = new HashMap<>();
    private static final int FALLBACK = 0xCFE4FF;   // azul-prateado (ferro, itens sem cor forte)

    public static int color(Item item) {
        return COLORS.computeIfAbsent(item, ForgeGlow::readColor);
    }

    public static void clearCache() { COLORS.clear(); }

    private static int readColor(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png");
        var res = Minecraft.getInstance().getResourceManager().getResource(tex);
        if (res.isEmpty()) return FALLBACK;
        try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            return dominant(img);
        } catch (Exception e) {
            return FALLBACK;
        }
    }

    // Histograma de matiz (24 faixas) pesado por saturacao x brilho; ignora cinzas, pretos e o marrom dos cabos.
    // A cor final e' a media dos pixels da faixa vencedora (e vizinhas), clareada para brilhar.
    private static int dominant(NativeImage img) {
        float[] w = new float[24], sr = new float[24], sg = new float[24], sb = new float[24];
        float[] hsv = new float[3];
        float gr = 0, gg = 0, gb = 0, gw = 0;   // media dos pixels sem cor (para itens de ferro/prata)
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getPixelRGBA(x, y);             // formato ABGR
                if ((c >>> 24) < 128) continue;
                int r = c & 255, g = (c >> 8) & 255, b = (c >> 16) & 255;
                hsv(r, g, b, hsv);
                float h = hsv[0], s = hsv[1], v = hsv[2];
                if (v < 0.22f) continue;
                if (s < 0.22f) { gr += r; gg += g; gb += b; gw++; continue; }
                if (h >= 15 && h <= 45 && v < 0.62f) continue;   // madeira / couro do cabo
                int bin = Math.min(23, (int) (h / 15f));
                float k = s * v;
                w[bin] += k; sr[bin] += r * k; sg[bin] += g * k; sb[bin] += b * k;
            }
        }
        int best = -1; float bw = 0;
        for (int i = 0; i < 24; i++) {
            float tot = w[i] + 0.5f * (w[(i + 23) % 24] + w[(i + 1) % 24]);
            if (tot > bw) { bw = tot; best = i; }
        }
        float r, g, b;
        if (best < 0 || bw < 1.5f) {
            if (gw == 0) return FALLBACK;
            r = gr / gw; g = gg / gw; b = gb / gw;
            r = r * 0.8f + 0.2f * 207; g = g * 0.8f + 0.2f * 228; b = b * 0.8f + 0.2f * 255;   // puxa para azul-prateado
        } else {
            float tw = 0; r = g = b = 0;
            for (int d = -1; d <= 1; d++) {
                int i = (best + d + 24) % 24;
                float k = d == 0 ? 1f : 0.5f;
                r += sr[i] * k; g += sg[i] * k; b += sb[i] * k; tw += w[i] * k;
            }
            r /= tw; g /= tw; b /= tw;
        }
        float m = Math.max(r, Math.max(g, b));
        if (m < 1) return FALLBACK;
        float k = 255f / m;                              // clareia ate o canal mais forte chegar a 255
        int R = Math.round(Math.min(255, r * k * 0.82f + 255 * 0.18f));
        int G = Math.round(Math.min(255, g * k * 0.82f + 255 * 0.18f));
        int B = Math.round(Math.min(255, b * k * 0.82f + 255 * 0.18f));
        return (R << 16) | (G << 8) | B;
    }

    // RGB (0-255) -> matiz (0-360), saturacao e brilho (0-1)
    private static void hsv(int r, int g, int b, float[] out) {
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * ((b - r) / d + 2f);
        else h = 60f * ((r - g) / d + 4f);
        if (h < 0) h += 360f;
        out[0] = h; out[1] = max == 0 ? 0 : d / max; out[2] = max / 255f;
    }

    // ---------------------------------------------------------------- item na mao
    private static final RandomSource RANDOM = RandomSource.create();

    // Desenha duas "cascas" do modelo do item, um pouco maiores, translucidas e acesas, por cima do item ja desenhado.
    public static void renderHeld(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                  PoseStack ps, MultiBufferSource buf) {
        int tier = tier(stack);
        if (tier == 0 || entity.isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, entity.level(), entity, entity.getId() + ctx.ordinal());
        if (model.isCustomRenderer()) return;   // itens desenhados por codigo (escudo, tridente): sem brilho
        float a = alpha(tier);
        if (ctx.firstPerson()) a *= 0.7f;      // na propria mao, mais discreto
        int rgb = color(stack.getItem());
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));

        ps.pushPose();
        BakedModel shown = ClientHooks.handleCameraTransforms(ps, model, ctx, leftHand);
        float[][] shells = {{1.05f, a}, {1.12f + 0.02f * tier, a * 0.45f}};
        for (float[] sh : shells) {
            ps.pushPose();
            ps.scale(sh[0], sh[0], sh[0]);
            ps.translate(-0.5f, -0.5f, -0.5f);
            PoseStack.Pose pose = ps.last();
            for (BakedModel pass : shown.getRenderPasses(stack, true)) {
                for (Direction d : Direction.values()) quads(vc, pose, pass, d, r, g, b, sh[1]);
                quads(vc, pose, pass, null, r, g, b, sh[1]);
            }
            ps.popPose();
        }
        ps.popPose();
    }

    private static void quads(VertexConsumer vc, PoseStack.Pose pose, BakedModel model, Direction side,
                              float r, float g, float b, float a) {
        RANDOM.setSeed(42L);
        for (BakedQuad q : model.getQuads(null, side, RANDOM))
            vc.putBulkData(pose, q, r, g, b, a, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}

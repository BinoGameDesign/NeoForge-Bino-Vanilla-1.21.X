package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Desenhos pequenos da mesa: item deitado e a silhueta branca de onde colocar um item / apertar.
public final class LapidaryDraw {
    private LapidaryDraw() {}

    // item deitado no plano horizontal, centrado no ponto atual do PoseStack (size = lado em blocos)
    public static void flat(PoseStack ps, MultiBufferSource buf, ItemStack stack, float size, float yaw, int light) {
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(90f));
        ps.scale(size, size, size);
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
        ps.popPose();
    }

    // ---------------------------------------------------------------- silhueta branca
    private static final Map<Item, ResourceLocation> MASKS = new HashMap<>();
    private static ResourceLocation white;

    // quadrado branco deitado com o formato do item (sprite 2D), um pouco maior que ele; alpha pulsa
    public static void ghost(PoseStack ps, MultiBufferSource buf, ItemStack stack, float size, float yaw, float alpha) {
        ResourceLocation tex = mask(stack.getItem());
        if (tex == null) return;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yaw + 180f));   // mesmo sentido do item deitado (flat)
        quad(ps, buf.getBuffer(RenderType.entityTranslucentEmissive(tex)), size * 1.18f, size * 1.18f, alpha);
        ps.popPose();
    }

    // retangulo branco em pe (para o botao), virado para -x do modelo; w/h em blocos
    public static void glowRect(PoseStack ps, MultiBufferSource buf, float w, float h, float alpha) {
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(90f));   // o plano deitado vira de pe, olhando para -x
        quad(ps, buf.getBuffer(RenderType.entityTranslucentEmissive(white())), h, w, alpha);
        ps.popPose();
    }

    private static void quad(PoseStack ps, VertexConsumer vc, float w, float d, float alpha) {
        PoseStack.Pose pose = ps.last();
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        float x = w / 2, z = d / 2;
        vtx(vc, pose, -x, 0, -z, 0, 0, a);
        vtx(vc, pose, -x, 0, z, 0, 1, a);
        vtx(vc, pose, x, 0, z, 1, 1, a);
        vtx(vc, pose, x, 0, -z, 1, 0, a);
    }

    private static void vtx(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int a) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    private static ResourceLocation white() {
        if (white == null) {
            NativeImage img = new NativeImage(1, 1, true);
            img.setPixelRGBA(0, 0, 0xFFFFFFFF);
            white = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "lapidary/white");
            Minecraft.getInstance().getTextureManager().register(white, new DynamicTexture(img));
        }
        return white;
    }

    // mascara branca do sprite (textures/item/<nome>.png), com 1 pixel de borda
    private static ResourceLocation mask(Item item) {
        if (MASKS.containsKey(item)) return MASKS.get(item);
        ResourceLocation out = null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        var res = Minecraft.getInstance().getResourceManager().getResource(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"));
        if (res.isPresent()) {
            try (InputStream in = res.get().open(); NativeImage src = NativeImage.read(in)) {
                int w = src.getWidth(), h = Math.min(src.getHeight(), w), s = Math.max(1, w / 16);
                NativeImage img = new NativeImage(w + 2 * s, h + 2 * s, true);
                for (int y = 0; y < h + 2 * s; y++)
                    for (int x = 0; x < w + 2 * s; x++) {
                        boolean on = false;
                        for (int dy = -s; dy <= s && !on; dy++)
                            for (int dx = -s; dx <= s && !on; dx++) {
                                int sx = x - s + dx, sy = y - s + dy;
                                on = sx >= 0 && sy >= 0 && sx < w && sy < h && (src.getPixelRGBA(sx, sy) >>> 24) > 16;
                            }
                        img.setPixelRGBA(x, y, on ? 0xFFFFFFFF : 0);
                    }
                out = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "lapidary/mask/" + id.getNamespace() + "/" + id.getPath());
                Minecraft.getInstance().getTextureManager().register(out, new DynamicTexture(img));
            } catch (Exception ignored) {
                out = null;
            }
        }
        MASKS.put(item, out);
        return out;
    }

    public static void clear() { MASKS.clear(); }
}

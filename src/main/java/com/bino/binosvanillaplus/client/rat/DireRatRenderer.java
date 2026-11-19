package com.bino.binosvanillaplus.client.rat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.rat.RatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// Rato Atroz: modelo do rato (geometria propria em DireRatModelData), textura propria, piscada e olhos vermelhos acesos.
// O tamanho vem do atributo SCALE (DireRatEntity), entao a sombra tambem cresce.
public class DireRatRenderer extends MobRenderer<RatEntity, RatModel> {
    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/rat/" + n + ".png"); }
    private static final ResourceLocation TEX = tex("dire_rat"), BLINK = tex("dire_rat_blink"), EYES = tex("dire_rat_eyes");

    public DireRatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RatModel(ctx.bakeLayer(RatModel.DIRE_LAYER)), 0.2f);
        addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RatEntity e) { return e.eyesClosed() ? BLINK : TEX; }

    static class Eyes extends RenderLayer<RatEntity, RatModel> {
        Eyes(RenderLayerParent<RatEntity, RatModel> parent) { super(parent); }
        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, RatEntity e, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (e.isInvisible() || e.eyesClosed()) return;
            getParentModel().renderToBuffer(ps, buf.getBuffer(RenderType.eyes(EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}

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

// Rato: textura por variante, piscada por troca de textura (olhos fechados tambem dormindo)
// e olhos luminosos no escuro (camada "eyes", igual ao Enderman).
public class RatRenderer extends MobRenderer<RatEntity, RatModel> {
    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/rat/" + n + ".png"); }
    private static final ResourceLocation RAT = tex("rat"), RAT_BLINK = tex("rat_blink"), RAT_EYES = tex("rat_eyes");
    private static final ResourceLocation DEEP = tex("deep_rat"), DEEP_BLINK = tex("deep_rat_blink"), DEEP_EYES = tex("deep_rat_eyes");

    public RatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RatModel(ctx.bakeLayer(RatModel.LAYER)), 0.2f);
        addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RatEntity e) {
        boolean closed = e.eyesClosed();
        if (e.isDeep()) return closed ? DEEP_BLINK : DEEP;
        return closed ? RAT_BLINK : RAT;
    }

    static class Eyes extends RenderLayer<RatEntity, RatModel> {
        Eyes(RenderLayerParent<RatEntity, RatModel> parent) { super(parent); }
        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, RatEntity e, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (e.isInvisible() || e.eyesClosed()) return;
            getParentModel().renderToBuffer(ps, buf.getBuffer(RenderType.eyes(e.isDeep() ? DEEP_EYES : RAT_EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}

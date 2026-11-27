package com.bino.binosvanillaplus.client.rat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.rat.KingRatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// King Rat: modelo do king_rat.bbmodel, olhos vermelhos acesos (como os outros ratos). O tamanho vem do atributo SCALE.
public class KingRatRenderer extends MobRenderer<KingRatEntity, KingRatModel> {
    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/rat/" + n + ".png"); }
    private static final ResourceLocation TEX = tex("king_rat"), EYES = tex("king_rat_eyes");

    public KingRatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KingRatModel(ctx.bakeLayer(KingRatModel.LAYER)), 0.7f);
        addLayer(new Eyes(this));
    }

    @Override public ResourceLocation getTextureLocation(KingRatEntity e) { return TEX; }

    // sem o tombo de lado do jogo: a animacao de morte do Blockbench cuida disso
    @Override protected float getFlipDegrees(KingRatEntity e) { return 0f; }

    static class Eyes extends RenderLayer<KingRatEntity, KingRatModel> {
        Eyes(RenderLayerParent<KingRatEntity, KingRatModel> parent) { super(parent); }
        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, KingRatEntity e, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (e.isInvisible() || e.deathTime > 0) return;
            getParentModel().renderToBuffer(ps, buf.getBuffer(RenderType.eyes(EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}

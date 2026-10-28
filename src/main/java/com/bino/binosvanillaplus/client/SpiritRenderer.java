package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.SpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// Desenha o Espirito: semitransparente e brilhante, como um fantasma
public class SpiritRenderer extends EntityRenderer<SpiritEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/spirit.png");
    private static final int COLOR = 0xD9FFFFFF;   // 0xAARRGGBB: o "D9" (85%) e' a opacidade. FF = totalmente opaco.

    private final SpiritModel model;

    public SpiritRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SpiritModel(context.bakeLayer(SpiritModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SpiritEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);          // mesma transformacao dos mobs do jogo
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, COLOR);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);   // nome em cima
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritEntity entity) {
        return TEXTURE;
    }
}

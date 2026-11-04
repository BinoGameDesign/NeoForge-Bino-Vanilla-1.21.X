package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// Lanterna presa na lateral direita do capacete, apontando para a frente (o modelo 3D da lanterna, pequeno)
public class HelmetLampLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private ItemStack lamp;

    public HelmetLampLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.getOrDefault(ModDataComponents.HELMET_LAMP.get(), false) || player.isInvisible()) return;
        if (lamp == null) lamp = new ItemStack(ModItems.FLASHLIGHT.get());
        ps.pushPose();
        getParentModel().getHead().translateAndRotate(ps);
        ps.translate(-0.33, -0.32, -0.06);              // lado direito da cabeca, na altura dos olhos
        ps.mulPose(Axis.XP.rotationDegrees(-90f));      // o eixo da lanterna (+Y do modelo) passa a apontar para a frente
        ps.scale(0.38f, 0.38f, 0.38f);
        Minecraft.getInstance().getItemRenderer().renderStatic(lamp, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY,
                ps, buf, player.level(), player.getId());
        ps.popPose();
    }
}

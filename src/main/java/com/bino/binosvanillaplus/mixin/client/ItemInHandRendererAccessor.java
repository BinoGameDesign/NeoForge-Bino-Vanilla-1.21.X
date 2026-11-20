package com.bino.binosvanillaplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// desenhar a mao/arma em primeira pessoa com a pose do golpe (mod Combat, client/combat/CombatMoves)
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("renderArmWithItem")
    void binosvanillaplus$renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
                                            float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
                                            MultiBufferSource buffer, int light);
}

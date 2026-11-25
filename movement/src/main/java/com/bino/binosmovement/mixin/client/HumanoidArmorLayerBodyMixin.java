package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.FirstPersonBody;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// corpo da primeira pessoa: sem capacete (a camera fica dentro da cabeca) e sem as mangas da armadura do peito.
// (require = 0: se o NeoForge mudar o metodo, so o capacete volta a aparecer; o jogo nao quebra)
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerBodyMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void binosmovement$hideHelmet(PoseStack ps, MultiBufferSource buf, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        if (!FirstPersonBody.rendering()) return;
        if (slot == EquipmentSlot.HEAD) ci.cancel();
    }

    // o jogo liga as partes da peca (setPartVisibility); logo depois, as mangas da armadura do peito saem
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V", shift = At.Shift.AFTER),
            require = 0)
    private void binosmovement$hideSleeves(PoseStack ps, MultiBufferSource buf, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        if (FirstPersonBody.rendering() && slot == EquipmentSlot.CHEST) { model.rightArm.visible = false; model.leftArm.visible = false; }
    }
}

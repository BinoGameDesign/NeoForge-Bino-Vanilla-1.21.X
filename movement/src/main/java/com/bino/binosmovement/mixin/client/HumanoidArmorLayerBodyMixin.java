package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.FirstPersonBody;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Corpo da primeira pessoa: sem capacete (a camera fica dentro da cabeca) e sem as mangas da armadura do peito.
// Entra logo depois de o jogo escolher as partes visiveis da peca (setPartVisibility). Armaduras com modelo proprio
// (as armaduras 3D do Mining) copiam essas partes visiveis do modelo do jogo, entao somem junto.
// (antes era no renderArmorPiece, mas o NeoForge mudou os parametros dele e o mixin nao pegava: o capacete aparecia
// em volta da camera)
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerBodyMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Inject(method = "setPartVisibility", at = @At("TAIL"), require = 0)
    private void binosmovement$hideNearCamera(A model, EquipmentSlot slot, CallbackInfo ci) {
        if (!FirstPersonBody.rendering()) return;
        if (slot == EquipmentSlot.HEAD) model.setAllVisible(false);
        else if (slot == EquipmentSlot.CHEST) { model.rightArm.visible = false; model.leftArm.visible = false; model.head.visible = false; model.hat.visible = false; }
        else { model.head.visible = false; model.hat.visible = false; }
    }
}

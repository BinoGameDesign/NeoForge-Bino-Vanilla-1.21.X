package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.combat.CombatMoves;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Terceira pessoa: pose dos golpes do hack and slash (mod Combat) no braco e no corpo do jogador.
// (o PlayerModel copia mangas/jaqueta depois deste ponto, entao elas acompanham)
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelCombatMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void binosvanillaplus$combatPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                             float headPitch, CallbackInfo ci) {
        if (entity instanceof Player p) CombatMoves.posePlayer((HumanoidModel<?>) (Object) this, p);
    }
}

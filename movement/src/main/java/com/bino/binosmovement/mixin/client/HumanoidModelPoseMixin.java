package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.MovementHooks;
import com.bino.binosmovement.client.anim.Locomotion;
import com.bino.binosmovement.client.anim.PlayerLife;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pose do jogador por cima da animacao do jogo: primeiro a vida no corpo (andar, correr, parado, no ar), depois o que
// outros mods registraram (golpes e rolamento do Combat).
// (o PlayerModel copia mangas/jaqueta depois deste ponto, entao elas acompanham)
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelPoseMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void binosmovement$pose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                    float headPitch, CallbackInfo ci) {
        if (entity instanceof Player p) {
            HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
            // animacoes do dia a dia do Blockbench; sem elas (ou na guarda de combate), a vida procedural
            if (!Locomotion.pose(m, p, ageInTicks)) PlayerLife.pose(m, p, limbSwing, limbSwingAmount, ageInTicks);
            for (MovementHooks.BodyPoser poser : MovementHooks.POSERS) poser.pose(m, p);
            m.hat.copyFrom(m.head);   // a camada do cabelo/chapeu segue a cabeca (o jogo copia antes deste ponto)
        }
    }
}

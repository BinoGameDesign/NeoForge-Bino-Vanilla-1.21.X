package com.bino.binosmining.mixin.client;

import com.bino.binosmining.client.lapidary.LapidaryCut;
import com.bino.binosmining.client.lapidary.LapidaryRemote;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pose em terceira pessoa de quem esta usando a Mesa de Lapidacao (o que os outros jogadores veem): bracos para a
// frente, sobre a mesa; a esquerda aperta o botao no comeco e a direita leva a gema para o lado no fim.
// (O PlayerModel copia os bracos para as mangas depois deste ponto.)
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelLapidaryMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void binosmining$lapidaryPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                               float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player) || LapidaryRemote.tableOf(entity.getId()) == null) return;
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        double breathe = Math.sin(ageInTicks * 0.09) * 0.03;
        m.rightArm.xRot = (float) (-0.95 + breathe);
        m.rightArm.yRot = -0.22f;
        m.rightArm.zRot = 0.05f;
        m.leftArm.xRot = (float) (-0.90 - breathe);
        m.leftArm.yRot = 0.30f;
        m.leftArm.zRot = -0.05f;
        double t = LapidaryRemote.cutTime(entity.getId());
        if (t >= 0) {
            boolean all = LapidaryRemote.cutAll(entity.getId());
            double e = LapidaryCut.extra(all);
            // aperta o botao
            m.leftArm.xRot -= (float) (0.30 * LapidaryCut.hump(LapidaryCut.window(t, 0.10, 0.45)));
            // pega a gema no disco e leva para o lado
            double reach = LapidaryCut.hump(LapidaryCut.window(t, LapidaryCut.reachAt(all), LapidaryCut.placeAt(all) + 0.2));
            double carry = LapidaryCut.smoother(LapidaryCut.window(t, LapidaryCut.pickAt(all), LapidaryCut.placeAt(all)));
            m.rightArm.xRot -= (float) (0.25 * reach);
            m.rightArm.yRot += (float) (0.55 * carry * (1 - LapidaryCut.window(t, LapidaryCut.placeAt(all), LapidaryCut.length(all))));
        }
    }
}

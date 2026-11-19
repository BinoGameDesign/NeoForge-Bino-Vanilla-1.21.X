package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.mimic.MimicChest;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Pista do mimico: evento de bloco "respirar" (o servidor manda; no cliente o renderizador mexe a tampa de leve)
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMimicMixin {
    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void binosvanillaplus$breath(int id, int type, CallbackInfoReturnable<Boolean> cir) {
        if (id != MimicChest.EVENT_BREATH) return;
        BlockEntity be = (BlockEntity) (Object) this;
        if (be.getLevel() != null && be.getLevel().isClientSide) {
            MimicChest.startBreath(be.getBlockPos());
            // grunhido baixinho junto com a respiracao (a pista para quem presta atencao)
            var p = be.getBlockPos();
            var rnd = be.getLevel().random;
            be.getLevel().playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    rnd.nextBoolean() ? net.minecraft.sounds.SoundEvents.HOGLIN_AMBIENT : net.minecraft.sounds.SoundEvents.RAVAGER_AMBIENT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.3f, 0.4f + rnd.nextFloat() * 0.15f, false);
        }
        cir.setReturnValue(true);
    }
}

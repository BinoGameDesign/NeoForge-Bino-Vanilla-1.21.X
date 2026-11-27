package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.thirst.WaterBottles;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Garrafa de agua acumula ate 64 (as outras pocoes continuam 1 por espaco, como no jogo).
@Mixin(ItemStack.class)
public abstract class ItemStackWaterStackMixin {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void binosvanillaplus$waterBottleStack(CallbackInfoReturnable<Integer> cir) {
        int m = WaterBottles.maxStack((ItemStack) (Object) this);
        if (m > 0) cir.setReturnValue(m);
    }
}

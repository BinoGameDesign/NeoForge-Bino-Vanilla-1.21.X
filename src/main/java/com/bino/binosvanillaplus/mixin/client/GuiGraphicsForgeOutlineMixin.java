package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.gear.ForgeIconOutline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Silhueta colorida atras do icone de item +10 (ver ForgeIconOutline). Todas as formas de desenhar item na
// interface (inventario, hotbar, baus...) passam por este renderItem privado.
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsForgeOutlineMixin {
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD"), require = 0)
    private void binosvanillaplus$forgeOutline(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset,
                                               CallbackInfo ci) {
        if (!stack.isEmpty()) ForgeIconOutline.draw((GuiGraphics) (Object) this, stack, x, y);
    }
}

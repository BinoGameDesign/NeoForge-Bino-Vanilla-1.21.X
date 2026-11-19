package com.bino.binosvanillaplus.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// as duas metades do bau duplo (para saber se e' um mimico)
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {
    @Accessor("container1") Container binosvanillaplus$first();
    @Accessor("container2") Container binosvanillaplus$second();
}

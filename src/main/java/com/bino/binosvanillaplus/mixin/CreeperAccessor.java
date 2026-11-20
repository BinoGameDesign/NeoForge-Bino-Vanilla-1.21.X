package com.bino.binosvanillaplus.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// mod Combat: ler/mudar o pavio do creeper (para aparar a explosao)
@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("swell") int binosvanillaplus$getSwell();
    @Accessor("swell") void binosvanillaplus$setSwell(int value);
    @Accessor("maxSwell") int binosvanillaplus$getMaxSwell();
}

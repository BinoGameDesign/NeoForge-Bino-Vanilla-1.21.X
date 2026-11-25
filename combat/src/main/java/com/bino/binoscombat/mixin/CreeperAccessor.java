package com.bino.binoscombat.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// mod Combat: ler/mudar o pavio do creeper (para aparar a explosao)
@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("swell") int binoscombat$getSwell();
    @Accessor("swell") void binoscombat$setSwell(int value);
    @Accessor("maxSwell") int binoscombat$getMaxSwell();
}

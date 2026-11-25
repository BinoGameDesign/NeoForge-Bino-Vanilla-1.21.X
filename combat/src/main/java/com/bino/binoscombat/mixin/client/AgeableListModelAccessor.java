package com.bino.binoscombat.mixin.client;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// porco, vaca, ovelha, lobo... (modelos "AgeableListModel"): cabeca e corpo para a reacao ao golpe
@Mixin(AgeableListModel.class)
public interface AgeableListModelAccessor {
    @Invoker("headParts") Iterable<ModelPart> binoscombat$headParts();
    @Invoker("bodyParts") Iterable<ModelPart> binoscombat$bodyParts();
}

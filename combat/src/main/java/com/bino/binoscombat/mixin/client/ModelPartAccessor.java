package com.bino.binoscombat.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

// para achar as pecas pelo nome (cabeca, bracos, cauda...) em qualquer modelo (reacao ao golpe, mod Combat)
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("children")
    Map<String, ModelPart> binoscombat$children();
}

package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.faction.FactionRenderers;
import com.bino.binosvanillaplus.faction.FactionData;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Pillager civil: mesmo villager (modelo e Fresh Animations), corpo com pele de pillager
@Mixin(VillagerRenderer.class)
public abstract class VillagerRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/npc/Villager;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void binos$civilianSkin(Villager v, CallbackInfoReturnable<ResourceLocation> cir) {
        if (FactionData.isCivilian(v)) cir.setReturnValue(FactionRenderers.civilianBase());
    }
}

package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.faction.FactionRenderers;
import com.bino.binosvanillaplus.faction.FactionData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pillager civil: as roupas (bioma, profissao e nivel) saem na paleta pillager.
// So troca a textura na hora de desenhar (a leitura do "tipo de chapeu" continua com a original).
@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerMixin {
    @Unique private boolean binos$civilian;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void binos$begin(PoseStack ps, MultiBufferSource buf, int light, LivingEntity e, float a, float b, float c, float d, float f, float g, CallbackInfo ci) {
        binos$civilian = FactionData.isCivilian(e);
    }

    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "renderColoredCutoutModel"), index = 1, require = 0)
    private ResourceLocation binos$clothes(ResourceLocation tex) {
        return binos$civilian ? FactionRenderers.civilianClothes(tex) : tex;
    }
}

package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.combat.HitReact;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mod Combat: reacao ao golpe nas pecas do modelo (cabeca, bracos, cauda...) logo antes de desenhar o corpo, e
// desfeita no fim do desenho do mob (assim a armadura e os itens nas maos acompanham).
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererHitReactMixin {
    @Shadow protected EntityModel<?> model;

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String DRAW = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = DRAW), require = 0)
    private void binosvanillaplus$hitReact(LivingEntity entity, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light, CallbackInfo ci) {
        HitReact.apply(model, entity);
    }

    // desfaz no fim do desenho (depois das camadas: armadura e itens acompanham a cabeca e os bracos)
    @Inject(method = RENDER, at = @At("RETURN"), require = 0)
    private void binosvanillaplus$hitReactUndo(LivingEntity entity, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light, CallbackInfo ci) {
        HitReact.undo();
    }
}

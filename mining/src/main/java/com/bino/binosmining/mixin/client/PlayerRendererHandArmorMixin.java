package com.bino.binosmining.mixin.client;

import com.bino.binosmining.client.gear.HandArmor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Armadura (manga do peitoral) nos bracos em primeira pessoa, inclusive nos bracos da cena da Mesa de Lapidacao.
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHandArmorMixin {
    @Inject(method = "renderRightHand", at = @At("TAIL"), require = 0)
    private void binosmining$rightArmor(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, CallbackInfo ci) {
        HandArmor.render(ps, buf, light, player, true, ((PlayerRenderer) (Object) this).getModel());
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"), require = 0)
    private void binosmining$leftArmor(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, CallbackInfo ci) {
        HandArmor.render(ps, buf, light, player, false, ((PlayerRenderer) (Object) this).getModel());
    }
}

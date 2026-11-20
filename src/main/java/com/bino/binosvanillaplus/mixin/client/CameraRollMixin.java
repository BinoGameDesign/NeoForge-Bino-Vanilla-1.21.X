package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.combat.DodgeRoll;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Rolamento e golpes (mod Combat): em primeira pessoa a camera abaixa junto com o corpo (encolhido / agachado no golpe)
@Mixin(Camera.class)
public abstract class CameraRollMixin {
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void binosvanillaplus$rollDrop(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                           float partialTick, CallbackInfo ci) {
        if (detached || !(entity instanceof Player p)) return;
        float d = DodgeRoll.cameraDrop(p) + com.bino.binosvanillaplus.client.combat.CombatMoves.cameraDrop(p);
        if (d > 0.001f) setPosition(getPosition().add(0, -d, 0));
    }
}

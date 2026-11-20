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
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void binosvanillaplus$rollDrop(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                           float partialTick, CallbackInfo ci) {
        if (!(entity instanceof Player p)) return;
        float[] cine = com.bino.binosvanillaplus.client.combat.CombatMoves.cinemaCam(p, partialTick);   // facada pelas costas: camera de lado
        if (cine != null) {
            setPosition(new Vec3(cine[0], cine[1], cine[2]));
            setRotation(cine[3], cine[4]);
            return;
        }
        if (detached) {   // camera sobre o ombro (F5)
            Vec3 sp = com.bino.binosvanillaplus.client.ShoulderCam.position((Camera) (Object) this, level, entity, partialTick);
            if (sp != null) setPosition(sp);
            return;
        }
        float d = DodgeRoll.cameraDrop(p) + com.bino.binosvanillaplus.client.combat.CombatMoves.cameraDrop(p);
        float f = com.bino.binosvanillaplus.client.combat.CombatMoves.cameraForward(p);
        if (d < 0.001f && Math.abs(f) < 0.001f) return;
        org.joml.Vector3f look = ((Camera) (Object) this).getLookVector();
        double hl = Math.sqrt(look.x * look.x + look.z * look.z);
        double fx = hl > 1e-4 ? look.x / hl * f : 0, fz = hl > 1e-4 ? look.z / hl * f : 0;
        setPosition(getPosition().add(fx, -d, fz));
    }
}

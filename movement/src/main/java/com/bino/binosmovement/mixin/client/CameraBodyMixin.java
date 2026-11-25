package com.bino.binosmovement.mixin.client;

import com.bino.binosmovement.client.HeadCam;
import com.bino.binosmovement.client.MovementHooks;
import com.bino.binosmovement.client.ShoulderCam;
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

// Posicao da camera: camera de cinema (Combat), camera no ombro (F5), e em primeira pessoa a camera vai junto com o
// corpo da animacao (HeadCam: estocada leva a frente, cambalhota gira, agachar abaixa).
@Mixin(Camera.class)
public abstract class CameraBodyMixin {
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow public abstract Vec3 getPosition();
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void binosmovement$body(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                    float partialTick, CallbackInfo ci) {
        HeadCam.clear();
        if (!(entity instanceof Player p)) return;
        for (MovementHooks.CinemaCam c : MovementHooks.CINEMA) {
            float[] cine = c.cam(p, partialTick);
            if (cine != null) {
                setPosition(new Vec3(cine[0], cine[1], cine[2]));
                setRotation(cine[3], cine[4]);
                return;
            }
        }
        if (detached) {   // camera sobre o ombro (F5)
            Vec3 sp = ShoulderCam.position((Camera) (Object) this, level, entity, partialTick);
            if (sp != null) setPosition(sp);
            return;
        }
        float drop = 0f, fwd = 0f;
        for (MovementHooks.CameraNudge n : MovementHooks.NUDGES) {
            float[] v = n.nudge(p, partialTick);
            if (v != null) { drop += v[0]; fwd += v[1]; }
        }
        Vec3 pos = getPosition();
        if (drop > 0.001f || Math.abs(fwd) > 0.001f) {
            org.joml.Vector3f look = ((Camera) (Object) this).getLookVector();
            double hl = Math.sqrt(look.x * look.x + look.z * look.z);
            double fx = hl > 1e-4 ? look.x / hl * fwd : 0, fz = hl > 1e-4 ? look.z / hl * fwd : 0;
            pos = pos.add(fx, -drop, fz);
        }
        Vec3 body = HeadCam.offset(p, partialTick);   // a cabeca da animacao (null = sem animacao de corpo agora)
        if (body != null) pos = pos.add(body);
        setPosition(pos);
    }
}

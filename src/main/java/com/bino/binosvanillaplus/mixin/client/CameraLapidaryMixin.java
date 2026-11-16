package com.bino.binosvanillaplus.mixin.client;

import com.bino.binosvanillaplus.client.lapidary.LapidaryScene;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Camera da cena da Mesa de Lapidacao: depois que o jogo posiciona a camera normal, a cena a desliza ate o ponto
// de vista sobre a mesa (LapidaryScene.override). Mesma ideia do CameraMixin do Cinematic Interactions (MIT).
@Mixin(Camera.class)
public abstract class CameraLapidaryMixin {
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void binosvanillaplus$lapidaryScene(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                                float partialTick, CallbackInfo ci) {
        if (!LapidaryScene.active()) return;
        Camera self = (Camera) (Object) this;
        LapidaryScene.Pose pose = LapidaryScene.override(self.getPosition(), self.getYRot(), self.getXRot());
        if (pose == null) return;
        this.setRotation(pose.yaw(), pose.pitch());
        this.setPosition(pose.pos());
    }
}

package com.bino.binosvanillaplus.client.rat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.rat.RatEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

// Modelo do rato: geometria/animacoes geradas do rat.bbmodel (RatModelData / RatAnimations).
public class RatModel extends HierarchicalModel<RatEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "rat"), "main");
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final Vector3f CACHE = new Vector3f();

    private final ModelPart top;
    private final ModelPart head;

    public RatModel(ModelPart top) {
        this.top = top;
        this.head = top.getChild("root").getChild("body").getChild("head");
    }

    @Override public ModelPart root() { return top; }

    private static AnimationDefinition oneShot(String n) {
        return switch (n) {
            case "groom" -> RatAnimations.GROOM;
            case "stand_up" -> RatAnimations.STAND_UP;
            case "scratch" -> RatAnimations.SCRATCH;
            case "squeak" -> RatAnimations.SQUEAK;
            case "attack" -> RatAnimations.ATTACK;
            default -> RatAnimations.SNIFF;
        };
    }

    @Override
    public void setupAnim(RatEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        int st = e.getState();
        if (st != RatEntity.ST_SLEEP) {
            head.yRot += netHeadYaw * DEG * 0.5f;
            head.xRot += headPitch * DEG * 0.5f;
        }
        boolean one = e.oneShot.isStarted();
        boolean bite = one && e.oneShotName.equals("attack");
        if (one && !bite) { animate(e.oneShot, oneShot(e.oneShotName), ageInTicks); return; }   // limpar patas, levantar etc.: sozinha

        float w = Math.min(1.0f, limbSwingAmount * 2.5f);
        switch (st) {
            case RatEntity.ST_SLEEP -> loop(RatAnimations.SLEEP, ageInTicks, 1f);
            case RatEntity.ST_EAT -> loop(RatAnimations.EAT, ageInTicks, 1f);
            case RatEntity.ST_FLEE -> blend(RatAnimations.IDLE, RatAnimations.RUN, ageInTicks, limbSwing, w, 1.0f);
            default -> blend(RatAnimations.IDLE, RatAnimations.WALK, ageInTicks, limbSwing, w, 2.0f);
        }
        if (bite) animate(e.oneShot, RatAnimations.ATTACK, ageInTicks);   // mordida por cima do andar
    }

    // loop pelo relogio da entidade (ticks -> ms), com peso
    private void loop(AnimationDefinition def, float ageInTicks, float weight) {
        if (weight > 0.001f) KeyframeAnimations.animate(this, def, (long) (ageInTicks * 50.0f), weight, CACHE);
    }

    private void blend(AnimationDefinition still, AnimationDefinition move, float ageInTicks, float limbSwing, float w, float stepSpeed) {
        loop(still, ageInTicks, 1.0f - w);
        if (w > 0.001f) KeyframeAnimations.animate(this, move, (long) (limbSwing * 50.0f * stepSpeed), w, CACHE);
    }
}

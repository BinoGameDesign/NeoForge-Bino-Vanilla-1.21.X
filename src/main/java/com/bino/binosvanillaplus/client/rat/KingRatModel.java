package com.bino.binosvanillaplus.client.rat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.rat.KingRatEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

// King Rat: geometria e animacoes geradas do king_rat.bbmodel (KingRatModelData / KingRatAnimations).
// Parado/andando misturados pelo passo; golpes por cima (garrada, cuspe, salto, guincho); acordar e morrer sozinhos.
public class KingRatModel extends HierarchicalModel<KingRatEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "king_rat"), "main");
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final Vector3f CACHE = new Vector3f();

    private final ModelPart top, head;

    public KingRatModel(ModelPart top) {
        this.top = top;
        this.head = top.getChild("root").getChild("body").getChild("head");
    }

    @Override public ModelPart root() { return top; }

    @Override
    public void setupAnim(KingRatEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        if (e.deathTime > 0) {   // tombo
            KeyframeAnimations.animate(this, KingRatAnimations.DEATH, (long) ((e.deathTime + ageInTicks - (int) ageInTicks) * 50f), 1f, CACHE);
            return;
        }
        if (e.wake.isStarted() && e.wake.getAccumulatedTime() < KingRatAnimations.WAKE.lengthInSeconds() * 1000f) {
            animate(e.wake, KingRatAnimations.WAKE, ageInTicks);
            return;
        }
        head.yRot += netHeadYaw * DEG * 0.6f;
        head.xRot += headPitch * DEG * 0.5f;
        float w = Math.min(1f, limbSwingAmount * 2.2f);
        if (1f - w > 0.001f) KeyframeAnimations.animate(this, KingRatAnimations.IDLE, (long) (ageInTicks * 50f), 1f - w, CACHE);
        if (w > 0.001f) KeyframeAnimations.animate(this, KingRatAnimations.WALK, (long) (limbSwing * 50f * 2.2f), w, CACHE);
        play(e, e.claw, KingRatAnimations.CLAW, ageInTicks);
        play(e, e.spit, KingRatAnimations.SPIT, ageInTicks);
        play(e, e.leap, KingRatAnimations.LEAP, ageInTicks);
        play(e, e.roar, KingRatAnimations.ROAR, ageInTicks);
    }

    private void play(KingRatEntity e, net.minecraft.world.entity.AnimationState st, AnimationDefinition def, float age) {
        if (st.isStarted() && st.getAccumulatedTime() < def.lengthInSeconds() * 1000f) animate(st, def, age);
    }
}

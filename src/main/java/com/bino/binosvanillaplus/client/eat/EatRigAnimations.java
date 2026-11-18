package com.bino.binosvanillaplus.client.eat;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de eat_rig.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class EatRigAnimations {
    private EatRigAnimations() {}

    public static final AnimationDefinition EAT_CHEESE = AnimationDefinition.Builder.withLength(2.0F)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.38F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-24.0F, -32.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.88F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-24.0F, -32.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.15F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.38F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-24.0F, -32.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.65F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.75F, KeyframeAnimations.degreeVec(-18.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.38F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-19.0F, 28.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.88F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-19.0F, 28.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.15F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.38F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-19.0F, 28.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.65F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.75F, KeyframeAnimations.degreeVec(-14.0F, 26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("food", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 20.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.75F, KeyframeAnimations.degreeVec(0.0F, 20.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("food", new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.55F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.6F, KeyframeAnimations.scaleVec(0.78F, 0.85F, 0.78F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.05F, KeyframeAnimations.scaleVec(0.78F, 0.85F, 0.78F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.1F, KeyframeAnimations.scaleVec(0.55F, 0.7F, 0.55F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.55F, KeyframeAnimations.scaleVec(0.55F, 0.7F, 0.55F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.6F, KeyframeAnimations.scaleVec(0.3F, 0.5F, 0.3F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.0F, KeyframeAnimations.scaleVec(0.3F, 0.5F, 0.3F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("camera", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(3.0F, 0.0F, 1.5F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(1.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.95F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.05F, KeyframeAnimations.degreeVec(3.0F, 0.0F, 1.5F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.25F, KeyframeAnimations.degreeVec(1.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.55F, KeyframeAnimations.degreeVec(3.0F, 0.0F, 1.5F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.75F, KeyframeAnimations.degreeVec(1.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition EAT_MEAT = AnimationDefinition.Builder.withLength(2.4F)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.48F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-27.0F, -36.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.72F, KeyframeAnimations.degreeVec(-16.0F, -30.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.08F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-27.0F, -36.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.32F, KeyframeAnimations.degreeVec(-16.0F, -30.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.68F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(-27.0F, -36.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.92F, KeyframeAnimations.degreeVec(-16.0F, -30.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(-20.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("food", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, -30.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(0.0F, -30.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("food", new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.66F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.72F, KeyframeAnimations.scaleVec(0.8F, 0.85F, 0.8F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.26F, KeyframeAnimations.scaleVec(0.8F, 0.85F, 0.8F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.32F, KeyframeAnimations.scaleVec(0.58F, 0.7F, 0.58F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.86F, KeyframeAnimations.scaleVec(0.58F, 0.7F, 0.58F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.92F, KeyframeAnimations.scaleVec(0.35F, 0.5F, 0.35F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.4F, KeyframeAnimations.scaleVec(0.35F, 0.5F, 0.35F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("camera", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.66F, KeyframeAnimations.degreeVec(4.0F, -2.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-1.0F, 3.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.05F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.26F, KeyframeAnimations.degreeVec(4.0F, -2.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(-1.0F, 3.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.65F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.86F, KeyframeAnimations.degreeVec(4.0F, -2.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-1.0F, 3.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    // duracao em ticks de cada animacao
    public static int lengthTicks(String anim) {
        return switch (anim) {
        case "eat_cheese" -> 40;
        case "eat_meat" -> 48;
            default -> 20;
        };
    }
}

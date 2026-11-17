package com.bino.binosvanillaplus.client.goblin;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de cave_goblin.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class CaveGoblinAnimations {
    private CaveGoblinAnimations() {}

    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(3.2F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5833F, KeyframeAnimations.degreeVec(20.9784F, -4.6701F, -1.7881F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(23.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-18.0F, 10.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-15.0F, -4.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(-17.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(3.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-14.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-16.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(-14.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.875F, KeyframeAnimations.degreeVec(21.78F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.875F, KeyframeAnimations.degreeVec(30.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-9.9464F, 1.0754F, 0.8673F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-12.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(-10.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.2097F, -8.5562F, -2.8019F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(23.4233F, -19.6046F, -8.2551F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(18.1093F, -6.1816F, -2.0168F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -22.0F, -76.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(8.0F, -34.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 20.0F, 77.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-8.0F, 36.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.06F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.14F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.22F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 16.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -4.0F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("cloth", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.posVec(0.0F, -0.089F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.2097F, -8.5562F, -2.8019F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.125F, KeyframeAnimations.degreeVec(16.987F, -10.8269F, 4.4503F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2917F, KeyframeAnimations.degreeVec(18.5647F, -7.7234F, -5.3282F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.2083F, KeyframeAnimations.degreeVec(18.2097F, -8.5562F, -2.8019F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.2917F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.5F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();
    public static final AnimationDefinition IDLE_FIDGET = AnimationDefinition.Builder.withLength(2.8F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(16.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.2F, KeyframeAnimations.degreeVec(16.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-6.0F, -10.0F, -14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-6.0F, -10.0F, -14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.3F, KeyframeAnimations.degreeVec(4.0F, -24.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(30.0F, 70.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(67.4773F, 33.8825F, 46.0925F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(-27.778F, 17.9434F, 37.1158F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.3F, KeyframeAnimations.degreeVec(-1.9849F, -7.3624F, 37.7911F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.9167F, KeyframeAnimations.posVec(0.4762F, -0.3239F, -0.0912F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-85.5773F, 18.2732F, -29.8508F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-110.4175F, -7.7103F, 11.4742F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-154.1364F, -6.533F, -3.5507F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.875F, KeyframeAnimations.degreeVec(-129.7255F, -3.9633F, -43.0384F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0417F, KeyframeAnimations.degreeVec(-110.4175F, -7.7103F, 11.4742F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2083F, KeyframeAnimations.degreeVec(-110.4175F, -7.7103F, 11.4742F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(-198.1652F, 76.1535F, -163.5741F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.3F, KeyframeAnimations.degreeVec(146.2581F, 71.1034F, 174.8123F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.125F, KeyframeAnimations.degreeVec(3.9701F, -27.7396F, -8.4806F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-14.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-14.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-10.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6667F, KeyframeAnimations.degreeVec(18.016F, -2.3776F, -0.773F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1667F, KeyframeAnimations.degreeVec(43.4124F, 12.1673F, -12.5655F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1667F, KeyframeAnimations.degreeVec(41.9892F, -17.1966F, 18.1968F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.95F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.05F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.25F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("cloth", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.1155F, -7.7959F, -0.4969F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.875F, KeyframeAnimations.degreeVec(-14.8845F, -7.7959F, -0.4969F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.875F, KeyframeAnimations.degreeVec(-15.1178F, -12.6259F, 0.8177F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.7917F, KeyframeAnimations.degreeVec(-15.1178F, -12.6259F, 0.8177F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6667F, KeyframeAnimations.degreeVec(19.5625F, -4.2437F, 11.7634F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.3333F, KeyframeAnimations.degreeVec(20.1155F, -7.7959F, -0.4969F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.7917F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9167F, KeyframeAnimations.degreeVec(12.6646F, -25.1815F, -27.8397F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2083F, KeyframeAnimations.degreeVec(-12.6076F, -40.6747F, 26.7859F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.875F, KeyframeAnimations.degreeVec(-7.189F, -8.9374F, 14.6762F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0833F, KeyframeAnimations.degreeVec(-7.6149F, -8.7666F, 23.8123F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.7917F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition LOOK_AROUND = AnimationDefinition.Builder.withLength(2.6F)
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-20.0F, 45.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(-20.0F, 45.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.05F, KeyframeAnimations.degreeVec(-22.0F, 38.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-18.0F, -48.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(-18.0F, -48.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(-26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(18.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(18.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -35.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -35.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 35.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 35.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.2097F, -8.5562F, -2.8019F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5833F, KeyframeAnimations.degreeVec(18.2097F, -8.5562F, -2.8019F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition WALK = AnimationDefinition.Builder.withLength(1.0F).looping()
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -78.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -15.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -40.0F, -78.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -15.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -78.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 78.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 15.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 78.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 15.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 78.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.0F, 6.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(22.0F, -6.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(22.0F, 6.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-18.0F, -5.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-18.0F, 5.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-18.0F, -5.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 12.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 12.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -6.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -12.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -6.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -12.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -6.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(4.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("cloth", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.449F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -0.019F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -0.449F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -0.019F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -0.449F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition SNEAK = AnimationDefinition.Builder.withLength(2.0F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(38.0F, 4.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(38.0F, -4.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(38.0F, 4.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-34.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-36.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-34.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-36.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-34.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-70.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(85.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.35F, KeyframeAnimations.degreeVec(-70.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.7F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.35F, KeyframeAnimations.degreeVec(85.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.7F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -55.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -66.0F, -52.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -55.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, -54.0F, -58.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -55.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, -55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-10.0F, -45.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-10.0F, -55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-10.0F, -65.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-10.0F, -55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 55.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 54.0F, 58.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 55.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 66.0F, 52.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 55.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 65.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(10.0F, 55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(10.0F, 45.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(10.0F, 55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-1.9068F, 37.0651F, 0.8847F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8333F, KeyframeAnimations.degreeVec(-9.9676F, 27.8989F, -13.3584F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 36.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -36.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("cloth", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.211F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.posVec(0.0F, -0.25F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -0.314F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.posVec(0.0F, -0.426F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -0.211F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.35F, KeyframeAnimations.posVec(0.0F, -0.25F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, -0.314F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.7F, KeyframeAnimations.posVec(0.0F, -0.426F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, -0.211F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition LURK = AnimationDefinition.Builder.withLength(2.6F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(44.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(46.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(44.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(-42.0F, 8.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.95F, KeyframeAnimations.degreeVec(-42.0F, -8.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-70.3154F, -0.5908F, 3.4051F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-70.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(110.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(110.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-50.4542F, 6.1561F, -3.2302F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-50.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-13.629F, -42.9482F, -65.1014F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-11.6151F, -42.6159F, -67.836F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-2.7281F, -40.3146F, -114.8024F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(-35.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(25.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-35.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(25.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(-35.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(25.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.05F, KeyframeAnimations.degreeVec(-35.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(25.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(25.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.1119F, 39.9786F, 51.9176F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-6.0285F, 2.6105F, 98.6296F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.625F, KeyframeAnimations.degreeVec(-22.1119F, 39.9786F, 51.9176F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-25.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(35.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-25.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(35.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-25.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(35.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(-25.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.05F, KeyframeAnimations.degreeVec(35.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-25.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-25.0F, 95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 32.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.7F, KeyframeAnimations.degreeVec(0.0F, 32.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.78F, KeyframeAnimations.degreeVec(0.0F, 20.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 32.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 32.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -32.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(0.0F, -30.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, -32.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.12F, KeyframeAnimations.degreeVec(9.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(9.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.62F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.416F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, -1.416F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition FLEE = AnimationDefinition.Builder.withLength(0.5F).looping()
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(100.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(100.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 60.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 40.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-20.0F, 60.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 40.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(20.0F, 60.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, -60.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(20.0F, -60.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-20.0F, -60.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(-26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -50.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("cloth", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.697F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -0.121F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -1.697F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.posVec(0.0F, -0.121F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -1.697F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition STEAL = AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(44.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(52.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(54.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(30.0F, 35.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(26.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 55.0F, 60.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 100.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-30.0F, 100.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -70.0F, 60.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -90.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-100.7286F, 62.0841F, -99.5046F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 40.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-25.0F, -55.0F, -60.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -10.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -60.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -95.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(-34.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-20.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-10.0F, 45.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-70.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(110.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(70.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-50.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 32.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -32.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.416F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.posVec(0.0F, -0.302F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, -0.302F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition GIGGLE = AnimationDefinition.Builder.withLength(1.2F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(13.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(13.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(13.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.48F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.56F, KeyframeAnimations.degreeVec(13.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.48F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.56F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.48F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.56F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.8353F, -25.1427F, 58.7506F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-0.0692F, -12.0221F, 28.0521F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 115.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 115.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -40.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.5F, -70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9583F, KeyframeAnimations.degreeVec(-14.8571F, 2.4147F, 18.179F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0417F, KeyframeAnimations.degreeVec(-11.451F, 12.1004F, -12.678F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition STARTLED = AnimationDefinition.Builder.withLength(0.8F)
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.posVec(0.0F, 2.83F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, -0.449F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.posVec(0.0F, -0.449F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 50.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -50.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -55.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.375F, KeyframeAnimations.posVec(-0.8F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-26.6037F, -46.5528F, 26.5249F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -80.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 20.0F, -50.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-9.8132F, 41.2324F, 17.3346F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 55.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.375F, KeyframeAnimations.posVec(0.6F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 80.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 80.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 50.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 15.0F, -50.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("sack", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition COWER = AnimationDefinition.Builder.withLength(0.375F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(50.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(50.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-49.308F, -46.257F, -5.421F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-61.808F, -46.257F, -5.421F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-49.308F, -46.257F, -5.421F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -120.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(0.0F, -116.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, -120.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(9.4377F, 2.848F, -10.2266F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-1.8128F, -22.0994F, -20.3237F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(148.319F, 49.9701F, -166.8592F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2083F, KeyframeAnimations.degreeVec(141.8731F, 53.8489F, -194.6391F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-80.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(-80.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(-80.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(130.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(130.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(130.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-80.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(-80.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(-80.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(130.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(130.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(130.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -32.5F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1667F, KeyframeAnimations.degreeVec(6.3251F, 2.2627F, -25.7031F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2917F, KeyframeAnimations.degreeVec(2.5F, -32.5F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(2.5F, -32.5F, -30.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 20.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F, 7.5F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-7.5F, 22.5F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4167F, KeyframeAnimations.degreeVec(-7.5F, 22.5F, 30.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(97.0524F, 2.5587F, -19.8422F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1667F, KeyframeAnimations.degreeVec(97.2472F, 1.936F, -14.8773F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(97.0524F, 2.5587F, -19.8422F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(-0.7995F, 0.4546F, -0.2101F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(80.9206F, 4.2086F, 24.6657F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2083F, KeyframeAnimations.degreeVec(80.149F, 1.728F, 9.8511F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(80.149F, 1.728F, 9.8511F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.6996F, 0.6066F, -0.3502F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(2.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.724F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, -2.724F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.posVec(0.0F, -2.724F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition EAT = AnimationDefinition.Builder.withLength(1.6F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-85.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-85.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-85.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-85.0F, 14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-85.0F, 14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-85.0F, 14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(140.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -30.0F, -60.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(9.7953F, 6.9136F, -43.6758F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(7.2501F, -4.7893F, -87.1013F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(11.8764F, -13.941F, -65.732F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5833F, KeyframeAnimations.degreeVec(16.5837F, -7.697F, -90.3981F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -115.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -100.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, -70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 25.0F, 60.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-8.327F, -35.4758F, 91.0806F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(53.4972F, 7.3762F, 18.178F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(5.7605F, -12.046F, 65.4868F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.25F, KeyframeAnimations.degreeVec(41.0772F, -22.5783F, 63.6945F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.625F, KeyframeAnimations.degreeVec(41.0772F, -22.5783F, 63.6945F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 115.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 100.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 70.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(4.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.85F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.15F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 20.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -20.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.105F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.posVec(0.0F, -3.105F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.posVec(0.0F, -3.105F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition RING_ON = AnimationDefinition.Builder.withLength(1.6F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -18.0F, -80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(7.1161F, 14.0387F, -95.0038F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0833F, KeyframeAnimations.degreeVec(7.6018F, 1.3967F, -78.496F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -60.0F, -55.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(-40.0F, -80.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-40.0F, -80.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 80.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(29.8568F, -23.8279F, 92.1067F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5417F, KeyframeAnimations.degreeVec(41.3168F, -22.6208F, 98.2684F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9583F, KeyframeAnimations.degreeVec(11.2938F, -26.7038F, 52.1301F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 55.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(30.0F, 85.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-30.0F, 85.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(30.0F, 85.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(-30.0F, 85.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 85.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.15F, KeyframeAnimations.degreeVec(-24.0F, 35.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.35F, KeyframeAnimations.degreeVec(-24.0F, -35.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(80.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(70.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 38.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -38.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.06F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.posVec(0.0F, -0.87F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition SHIELD_LIGHT = AnimationDefinition.Builder.withLength(1.4F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 25.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(12.0F, 28.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(10.0F, 25.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 30.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(12.0F, 34.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(10.0F, 30.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -2.5F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(-16.8011F, 13.8136F, -11.6826F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.375F, KeyframeAnimations.degreeVec(0.0F, -2.5F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(87.5F, -60.0F, -180.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(85.8679F, -59.0025F, -169.9369F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.375F, KeyframeAnimations.degreeVec(82.2956F, -73.9307F, -166.0736F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 90.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 44.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.375F, KeyframeAnimations.degreeVec(19.4729F, 54.5753F, 29.6554F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(75.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(80.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(75.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(75.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(80.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(75.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 40.0F, -20.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 42.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 40.0F, -20.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 20.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, -42.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, -40.0F, 20.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.166F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.posVec(0.0F, -0.211F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.posVec(0.0F, -0.166F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
    public static final AnimationDefinition SNIFF = AnimationDefinition.Builder.withLength(1.2F).looping()
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.posVec(0.0F, 0.0F, -0.6F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, 0.0F, -0.6F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -0.6F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -6.0F, 26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, -6.0F, 26.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("ear_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 6.0F, -26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 10.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 6.0F, -26.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -30.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -32.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, -30.0F, -70.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -62.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, -60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 32.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 30.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("forearm_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 62.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 60.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-24.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("calf_bone_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(36.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(36.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(36.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("lid_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.1F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.237F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, -0.237F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, -0.237F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    // duracao em ticks de cada animacao
    public static int lengthTicks(String anim) {
        return switch (anim) {
        case "idle" -> 64;
        case "idle_fidget" -> 56;
        case "look_around" -> 52;
        case "walk" -> 20;
        case "sneak" -> 40;
        case "lurk" -> 52;
        case "flee" -> 10;
        case "steal" -> 20;
        case "giggle" -> 24;
        case "startled" -> 16;
        case "cower" -> 8;
        case "eat" -> 32;
        case "ring_on" -> 32;
        case "shield_light" -> 28;
        case "sniff" -> 24;
            default -> 20;
        };
    }
}

package com.bino.binosvanillaplus.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// Animacao "flutuar" do Espirito (exportada do Blockbench: cabeca e corpo balancam)
public class SpiritAnimations {
    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(2.91667F).looping()
            .addAnimation("Head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.0175F, -0.9998F, 1.0002F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.875F, KeyframeAnimations.degreeVec(2.035F, -0.9994F, -2.0003F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.66667F, KeyframeAnimations.degreeVec(1.9799F, -1.1045F, 0.9984F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.54167F, KeyframeAnimations.degreeVec(5.5373F, -1.0289F, 0.9997F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.875F, KeyframeAnimations.degreeVec(-0.0175F, -0.9998F, 1.0002F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("Head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0F, 1F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.91667F, KeyframeAnimations.posVec(0F, -1.8F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.83333F, KeyframeAnimations.posVec(0F, 0.7F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.875F, KeyframeAnimations.posVec(0F, 1F, 0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("Body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-1F, 0F, -2F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.91667F, KeyframeAnimations.degreeVec(2.0027F, 2.9982F, -1.8952F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.83333F, KeyframeAnimations.degreeVec(1.6534F, -5.1248F, 1.8387F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.875F, KeyframeAnimations.degreeVec(-1F, 0F, -2F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("Body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0F, 1.1F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(0F, -1.1F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.83333F, KeyframeAnimations.posVec(0F, 0.78F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.0F, KeyframeAnimations.posVec(0F, 0.7F, 0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.875F, KeyframeAnimations.posVec(0F, 1.1F, 0F), AnimationChannel.Interpolations.LINEAR)))
            .build();
}

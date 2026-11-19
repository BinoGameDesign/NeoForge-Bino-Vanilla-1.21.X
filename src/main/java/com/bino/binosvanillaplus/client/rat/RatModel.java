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
    // Rato Gigante: mesma estrutura de ossos (animacoes proprias em DireRatAnimations), geometria propria (DireRatModelData)
    public static final ModelLayerLocation DIRE_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "dire_rat"), "main");
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final Vector3f CACHE = new Vector3f();

    // animacoes de cada bicho (o Rato Gigante tem mordida propria e a chicotada de cauda)
    public record Anims(AnimationDefinition idle, AnimationDefinition walk, AnimationDefinition run, AnimationDefinition sleep,
                        AnimationDefinition eat, AnimationDefinition groom, AnimationDefinition standUp, AnimationDefinition scratch,
                        AnimationDefinition squeak, AnimationDefinition attack, AnimationDefinition sniff, AnimationDefinition tailWhip) {}
    public static final Anims RAT_ANIMS = new Anims(RatAnimations.IDLE, RatAnimations.WALK, RatAnimations.RUN, RatAnimations.SLEEP,
            RatAnimations.EAT, RatAnimations.GROOM, RatAnimations.STAND_UP, RatAnimations.SCRATCH, RatAnimations.SQUEAK,
            RatAnimations.ATTACK, RatAnimations.SNIFF, RatAnimations.ATTACK);
    public static final Anims DIRE_ANIMS = new Anims(DireRatAnimations.IDLE, DireRatAnimations.WALK, DireRatAnimations.RUN, DireRatAnimations.SLEEP,
            DireRatAnimations.EAT, DireRatAnimations.GROOM, DireRatAnimations.STAND_UP, DireRatAnimations.SCRATCH, DireRatAnimations.SQUEAK,
            DireRatAnimations.ATTACK, DireRatAnimations.SNIFF, DireRatAnimations.TAIL_WHIP);

    private final ModelPart top;
    private final ModelPart head;
    private final Anims a;

    public RatModel(ModelPart top) { this(top, RAT_ANIMS); }

    public RatModel(ModelPart top, Anims anims) {
        this.top = top;
        this.a = anims;
        this.head = top.getChild("root").getChild("body").getChild("head");
    }

    @Override public ModelPart root() { return top; }

    private AnimationDefinition oneShot(String n) {
        return switch (n) {
            case "groom" -> a.groom();
            case "stand_up" -> a.standUp();
            case "scratch" -> a.scratch();
            case "squeak" -> a.squeak();
            case "attack" -> a.attack();
            case "tail_whip" -> a.tailWhip();
            default -> a.sniff();
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
            case RatEntity.ST_SLEEP -> loop(a.sleep(), ageInTicks, 1f);
            case RatEntity.ST_EAT -> loop(a.eat(), ageInTicks, 1f);
            case RatEntity.ST_FLEE -> blend(a.idle(), a.run(), ageInTicks, limbSwing, w, 1.0f);
            default -> blend(a.idle(), a.walk(), ageInTicks, limbSwing, w, 2.0f);
        }
        if (bite) animate(e.oneShot, a.attack(), ageInTicks);   // mordida por cima do andar
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

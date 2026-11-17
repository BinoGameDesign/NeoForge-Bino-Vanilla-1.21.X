package com.bino.binosvanillaplus.client.goblin;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.goblin.CaveGoblinEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

// Modelo do Cave Goblin: geometria e animacoes vem do .bbmodel (CaveGoblinModelData / CaveGoblinAnimations, gerados).
// Aqui so se escolhe QUAL animacao toca, conforme o estado que o servidor manda.
public class CaveGoblinModel extends HierarchicalModel<CaveGoblinEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "cave_goblin"), "main");
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final Vector3f CACHE = new Vector3f();

    private final ModelPart top;
    private final ModelPart head;

    public CaveGoblinModel(ModelPart top) {
        this.top = top;
        this.head = top.getChild("root").getChild("body").getChild("head");
    }

    @Override
    public ModelPart root() { return top; }

    private static AnimationDefinition oneShot(String name) {
        return switch (name) {
            case "steal" -> CaveGoblinAnimations.STEAL;
            case "giggle" -> CaveGoblinAnimations.GIGGLE;
            case "startled" -> CaveGoblinAnimations.STARTLED;
            case "ring_on" -> CaveGoblinAnimations.RING_ON;
            case "look_around" -> CaveGoblinAnimations.LOOK_AROUND;
            case "idle_fidget" -> CaveGoblinAnimations.IDLE_FIDGET;
            default -> CaveGoblinAnimations.SNIFF;
        };
    }

    @Override
    public void setupAnim(CaveGoblinEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        int st = e.getState();
        boolean one = e.oneShot.isStarted();

        if (!one && st != CaveGoblinEntity.ST_COWER && st != CaveGoblinEntity.ST_SHIELD) {   // cabeca acompanha o olhar
            head.yRot += netHeadYaw * DEG * 0.6f;
            head.xRot += headPitch * DEG * 0.5f;
        }
        if (one) { animate(e.oneShot, oneShot(e.oneShotName), ageInTicks); return; }   // animacao de uma vez so manda sozinha

        // Os bracos do modelo estao em T: quem abaixa e a animacao. Por isso a animacao "parada" e a "em movimento"
        // sempre somam 100% (a de movimento pesa conforme a velocidade) -- nunca sobra pose base.
        float w = Math.min(1.0f, limbSwingAmount * 2.5f);
        switch (st) {
            case CaveGoblinEntity.ST_STALK -> blend(CaveGoblinAnimations.LURK, CaveGoblinAnimations.SNEAK, ageInTicks, limbSwing, w, 2.0f);
            case CaveGoblinEntity.ST_FLEE -> blend(CaveGoblinAnimations.IDLE, CaveGoblinAnimations.FLEE, ageInTicks, limbSwing, w, 1.0f);
            case CaveGoblinEntity.ST_COWER -> play(CaveGoblinAnimations.COWER, ageInTicks, 1.0f);
            case CaveGoblinEntity.ST_SHIELD -> play(CaveGoblinAnimations.SHIELD_LIGHT, ageInTicks, 1.0f);
            case CaveGoblinEntity.ST_EAT -> play(CaveGoblinAnimations.EAT, ageInTicks, 1.0f);
            default -> blend(CaveGoblinAnimations.IDLE, CaveGoblinAnimations.WALK, ageInTicks, limbSwing, w, 2.0f);
        }
    }

    // animacao em loop pelo relogio da entidade (ticks -> milissegundos), com peso
    private void play(AnimationDefinition def, float ageInTicks, float weight) {
        if (weight > 0.001f) KeyframeAnimations.animate(this, def, (long) (ageInTicks * 50.0f), weight, CACHE);
    }

    // parada (peso 1-w) + movimento no ritmo dos passos (peso w)
    private void blend(AnimationDefinition still, AnimationDefinition move, float ageInTicks, float limbSwing, float w, float stepSpeed) {
        play(still, ageInTicks, 1.0f - w);
        if (w > 0.001f) KeyframeAnimations.animate(this, move, (long) (limbSwing * 50.0f * stepSpeed), w, CACHE);
    }
}

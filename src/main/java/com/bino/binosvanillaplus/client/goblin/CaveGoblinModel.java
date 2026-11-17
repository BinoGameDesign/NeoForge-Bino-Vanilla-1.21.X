package com.bino.binosvanillaplus.client.goblin;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.goblin.CaveGoblinEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

// Modelo do Cave Goblin: geometria e animacoes vem do .bbmodel (CaveGoblinModelData / CaveGoblinAnimations, gerados).
// Aqui so se escolhe QUAL animacao toca, conforme o estado que o servidor manda.
public class CaveGoblinModel extends HierarchicalModel<CaveGoblinEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "cave_goblin"), "main");
    private static final float DEG = (float) (Math.PI / 180.0);

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

        switch (st) {
            case CaveGoblinEntity.ST_STALK -> {
                animateWalk(CaveGoblinAnimations.SNEAK, limbSwing, limbSwingAmount, 2.0f, 2.5f);
                animate(e.lurkState, CaveGoblinAnimations.LURK, ageInTicks);
            }
            case CaveGoblinEntity.ST_FLEE -> animateWalk(CaveGoblinAnimations.FLEE, limbSwing, limbSwingAmount, 1.0f, 2.0f);
            case CaveGoblinEntity.ST_COWER -> animate(e.cowerState, CaveGoblinAnimations.COWER, ageInTicks);
            case CaveGoblinEntity.ST_SHIELD -> animate(e.shieldState, CaveGoblinAnimations.SHIELD_LIGHT, ageInTicks);
            case CaveGoblinEntity.ST_EAT -> animate(e.eatState, CaveGoblinAnimations.EAT, ageInTicks);
            default -> {
                animateWalk(CaveGoblinAnimations.WALK, limbSwing, limbSwingAmount, 2.0f, 2.5f);
                animate(e.idleState, CaveGoblinAnimations.IDLE, ageInTicks);
            }
        }
    }
}

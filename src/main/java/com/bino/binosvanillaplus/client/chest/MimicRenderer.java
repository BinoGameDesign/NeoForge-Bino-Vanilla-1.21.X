package com.bino.binosvanillaplus.client.chest;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.mimic.MimicChest;
import com.bino.binosvanillaplus.mimic.MimicEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Mimico revelado: modelo do mimico do Bino (simples ou duplo) com a textura do bau em que ele estava
// (normal, armadilha ou natal). Animacoes do Blockbench: reveal (abrir a boca), bite (morder), hop (pulo) e, na agua e
// parado, a "water" do Bino com bolhas de vez em quando (ChestWater).
public class MimicRenderer extends MobRenderer<MimicEntity, MimicRenderer.Model> {
    private final Model singleModel, doubleModel;

    public MimicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ChestModels.MIMIC), false), 0.5f);
        this.singleModel = this.model;
        this.doubleModel = new Model(ctx.bakeLayer(ChestModels.MIMIC_DOUBLE), true);
    }

    @Override
    public void render(MimicEntity e, float yaw, float pt, PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
        this.model = e.isDouble() ? doubleModel : singleModel;
        this.shadowRadius = e.isDouble() ? 0.9f : 0.5f;
        super.render(e, yaw, pt, ps, buf, light);
    }

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/chest_bino/" + n + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity e) {
        String v = MimicChest.isChristmas() ? "christmas" : e.isTrapped() ? "trapped" : "normal";
        return tex("mimic_" + v + (e.isDouble() ? "_double" : ""));
    }

    // a frente do modelo (fechadura) e' +Z no Blockbench; o mob olha para o outro lado: gira 180
    @Override
    protected void setupRotations(MimicEntity e, PoseStack ps, float bob, float yaw, float pt, float scale) {
        super.setupRotations(e, ps, bob, yaw, pt, scale);
        ps.mulPose(Axis.YP.rotationDegrees(180f));
    }

    // reacao ao golpe propria (mod Combat): a tampa/boca abre num susto e a lingua sacode
    public static class Model extends HierarchicalModel<MimicEntity> implements com.bino.binoscombat.client.combat.HitReactive {
        @Override
        public boolean hitReact(float k, float side, float front) {
            getAnyDescendantWithName("lid").ifPresent(p -> p.xRot += (float) Math.toRadians(28) * k);
            getAnyDescendantWithName("tongue").ifPresent(p -> { p.xRot += 0.4f * k; p.yRot += 0.35f * k * side; });
            return true;
        }

        private final ModelPart top;
        private final boolean dbl;

        public Model(ModelPart top, boolean dbl) { this.top = top; this.dbl = dbl; }
        @Override public ModelPart root() { return top; }

        private AnimationDefinition reveal() { return dbl ? MimicChestDoubleAnimations.REVEAL : MimicChestAnimations.REVEAL; }
        private AnimationDefinition bite() { return dbl ? MimicChestDoubleAnimations.BITE : MimicChestAnimations.BITE; }
        private AnimationDefinition hop() { return dbl ? MimicChestDoubleAnimations.HOP : MimicChestAnimations.HOP; }
        private AnimationDefinition water() { return dbl ? MimicChestDoubleAnimations.WATER : MimicChestAnimations.WATER; }

        @Override
        public void setupAnim(MimicEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
            root().getAllParts().forEach(ModelPart::resetPose);
            if (e.reveal.isStarted() && e.reveal.getAccumulatedTime() < reveal().lengthInSeconds() * 1000f) {
                this.animate(e.reveal, reveal(), age);
                if (e.bite.isStarted()) this.animate(e.bite, bite(), age);
                return;
            }
            // boca entreaberta enquanto caca (mostra os dentes)
            getAnyDescendantWithName("lid").ifPresent(p -> p.xRot += (float) Math.toRadians(14));
            boolean hopping = e.hop.isStarted() && e.hop.getAccumulatedTime() < hop().lengthInSeconds() * 1000f;
            boolean biting = e.bite.isStarted() && e.bite.getAccumulatedTime() < bite().lengthInSeconds() * 1000f;
            if (hopping) this.animate(e.hop, hop(), age);
            if (biting) this.animate(e.bite, bite(), age);
            // na agua e parado: de vez em quando a tampa sobe e escapam bolhas, igual aos baus
            AnimationDefinition water = water();
            long wt = ChestWater.step(e, e.level(), age - (float) Math.floor(age), e.isInWater() && !hopping && !biting,
                    water.lengthInSeconds(), (n, big) -> {
                        double yaw = Math.toRadians(e.yBodyRot);
                        ChestWater.bubbles(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), -Math.sin(yaw), Math.cos(yaw),
                                dbl ? 0.92 : 0.42, dbl ? n * 2 : n, big);
                    });
            if (wt >= 0) net.minecraft.client.animation.KeyframeAnimations.animate(this, water, wt, 1f, WATER_CACHE);
        }
        private static final org.joml.Vector3f WATER_CACHE = new org.joml.Vector3f();
    }
}

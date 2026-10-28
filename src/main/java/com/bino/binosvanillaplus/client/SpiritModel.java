package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.SpiritEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

// Modelo do Espirito (feito no Blockbench, convertido para os nomes do Mojang)
public class SpiritModel extends HierarchicalModel<SpiritEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "spirit"), "main");

    private final ModelPart root;

    public SpiritModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootPart = mesh.getRoot();

        PartDefinition all = rootPart.addOrReplaceChild("All", CubeListBuilder.create(), PartPose.offset(1.5F, 23.5F, -1.5F));
        PartDefinition mid = all.addOrReplaceChild("Mid", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = mid.addOrReplaceChild("Head", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.0F, -3.5F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.4F)),
                PartPose.offset(-1.5F, -28.6F, 1.5F));

        head.addOrReplaceChild("Tears", CubeListBuilder.create()
                .texOffs(0, 23).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 23).addBox(-7.0F, 1.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 23).addBox(-2.0F, 3.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.6F, 6.7F, -4.8F));

        mid.addOrReplaceChild("Body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -7.0F, -3.0F, 8.0F, 14.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.5F, -16.5F, 1.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SpiritEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.animate(entity.idleAnimationState, SpiritAnimations.IDLE, ageInTicks, 1.0F);
    }
}

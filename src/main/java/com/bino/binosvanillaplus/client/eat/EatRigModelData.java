package com.bino.binosvanillaplus.client.eat;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de eat_rig.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class EatRigModelData {
    private EatRigModelData() {}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition camera = root.addOrReplaceChild("camera", CubeListBuilder.create().texOffs(32, 32).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, 6.0F, 2.0F, -85.0F * DEG, -12.0F * DEG, 0.0F));
        PartDefinition food = right_arm.addOrReplaceChild("food", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.5F, 0.0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 7.0F, 2.0F, -82.0F * DEG, 12.0F * DEG, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

package com.bino.binosvanillaplus.client.goblin;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de cave_goblin.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class CaveGoblinModelData {
    private CaveGoblinModelData() {}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(46, 12).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.8F, -5.0F, 1.8F));
        PartDefinition calf_bone_l = leg_l.addOrReplaceChild("calf_bone_l", CubeListBuilder.create().texOffs(46, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.1F)).texOffs(32, 22).addBox(-1.5F, 1.9F, -3.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 2.0F, 0.0F));
        PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(54, 12).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.8F, -5.0F, 1.8F));
        PartDefinition calf_bone_r = leg_r.addOrReplaceChild("calf_bone_r", CubeListBuilder.create().texOffs(54, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.1F)).texOffs(46, 22).addBox(-1.5F, 1.9F, -3.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 2.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(-1, 12).addBox(-3.5F, -5.0F, -1.2F, 7.0F, 5.0F, 5.0F, new CubeDeformation(-0.1F)).texOffs(32, 57).addBox(-1.5F, -5.0F, -1.4F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(27, 41).addBox(-3.5F, -9.0F, -1.5F, 7.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, 0.5F));
        PartDefinition cloth = body.addOrReplaceChild("cloth", CubeListBuilder.create().texOffs(50, 29).addBox(-3.5F, 0.0F, -0.01F, 7.0F, 3.0F, 0.0F, new CubeDeformation(-0.1F)), PartPose.offset(0.0F, -1.0F, -1.2F));
        PartDefinition sack = body.addOrReplaceChild("sack", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 4.3F));
        sack.addOrReplaceChild("sack_r", CubeListBuilder.create().texOffs(18, 22).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 4.0F, 3.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(2.2F, -0.9F, -0.5F, 0.0F, 0.0F, 10.0F * DEG));
        PartDefinition arm_l = body.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offset(-4.5F, -8.0F, 0.5F));
        arm_l.addOrReplaceChild("arm_l_r", CubeListBuilder.create().texOffs(28, 0).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4F, 0.4F, -0.9F, 0.0F, 0.0F, -90.0F * DEG));
        PartDefinition forearm_bone_l = arm_l.addOrReplaceChild("forearm_bone_l", CubeListBuilder.create().texOffs(0, 29).addBox(-7.5F, -1.1F, -1.4F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offset(-4.4F, 0.4F, -0.9F));
        forearm_bone_l.addOrReplaceChild("forearm_l_r", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.1F, 0.0F, 0.0F, 0.0F, 0.0F, -90.0F * DEG));
        forearm_bone_l.addOrReplaceChild("thumb_l_r", CubeListBuilder.create().texOffs(8, 54).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(-5.5265F, 1.366F, -1.5272F, 38.1733F * DEG, -57.7746F * DEG, 120.0129F * DEG));
        PartDefinition arm_r = body.addOrReplaceChild("arm_r", CubeListBuilder.create(), PartPose.offset(4.5F, -8.0F, 0.5F));
        arm_r.addOrReplaceChild("arm_r_r", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, 0.4F, -0.9F, 0.0F, 0.0F, -90.0F * DEG));
        PartDefinition forearm_bone_r = arm_r.addOrReplaceChild("forearm_bone_r", CubeListBuilder.create().texOffs(12, 29).addBox(4.2F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offset(3.3F, 0.4F, -0.9F));
        forearm_bone_r.addOrReplaceChild("forearm_r_r", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.1F, 0.0F, 0.0F, 0.0F, 0.0F, -90.0F * DEG));
        forearm_bone_r.addOrReplaceChild("thumb_r_r", CubeListBuilder.create().texOffs(21, 54).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(4.6F, 1.4F, -1.1F, 88.0372F * DEG, 30.6415F * DEG, -48.5279F * DEG));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(0, 41).addBox(-1.5F, -1.0F, -8.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, -1.5F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(24, 29).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, -2.0F));
        jaw.addOrReplaceChild("tooth_r_r", CubeListBuilder.create().texOffs(14, 41).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(2.5F, -0.7F, -4.4F, 0.0F, 0.0F, 10.0F * DEG));
        jaw.addOrReplaceChild("tooth_l_r", CubeListBuilder.create().texOffs(14, 41).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-2.3F, -0.7F, -4.4F, 0.0F, 0.0F, -5.0F * DEG));
        PartDefinition lid_l = head.addOrReplaceChild("lid_l", CubeListBuilder.create().texOffs(1, 35).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -2.0F, -6.0F));
        PartDefinition lid_r = head.addOrReplaceChild("lid_r", CubeListBuilder.create().texOffs(9, 35).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -2.0F, -6.0F));
        PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(16, 34).addBox(-3.0F, -1.5F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(32, 34).addBox(-5.0F, -3.0F, -1.0F, 2.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -1.5F, -3.0F));
        PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(24, 34).addBox(0.0F, -1.5F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(36, 34).addBox(3.0F, -3.0F, -1.0F, 2.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -1.5F, -3.0F));
        ear_r.addOrReplaceChild("earring_r", CubeListBuilder.create().texOffs(1, 50).addBox(-1.0F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(5.3F, -0.3F, -0.6F, 0.0F, 0.0F, -12.0F * DEG));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

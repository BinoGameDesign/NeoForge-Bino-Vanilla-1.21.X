package com.bino.binosvanillaplus.client.rat;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de king_rat.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class KingRatModelData {
    private KingRatModelData() {}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(56, 0).addBox(-1.8F, -1.0F, -2.0F, 3.5F, 5.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(48, 25).addBox(-1.3F, 4.0F, 0.0F, 2.5F, 3.0F, 2.5F, new CubeDeformation(0.0F)).texOffs(46, 15).addBox(-1.5F, 7.0F, -4.5F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, -9.0F, 1.5F));
        PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(74, 0).addBox(-1.7F, -1.0F, -2.0F, 3.5F, 5.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 25).addBox(-1.2F, 4.0F, 0.0F, 2.5F, 3.0F, 2.5F, new CubeDeformation(0.0F)).texOffs(64, 15).addBox(-1.5F, 7.0F, -4.5F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, -9.0F, 1.5F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -3.5F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(30, 0).addBox(-3.5F, -11.5F, -3.5F, 7.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(92, 0).addBox(-3.0F, -10.0F, 3.0F, 6.0F, 8.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.0F, 1.5F, 8.0F * DEG, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 15).addBox(-3.0F, -4.5F, -4.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(32, 25).addBox(-2.0F, -3.0F, -8.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(44, 33).addBox(-0.5F, -3.0F, -9.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(48, 33).addBox(-1.0F, 0.0F, -7.9F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.5F, -1.0F, -8.0F * DEG, 0.0F, 0.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(90, 25).addBox(-1.5F, 0.0F, -3.3F, 3.0F, 1.0F, 3.3F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(104, 25).addBox(-3.0F, -3.5F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, -4.0F, -1.0F, 0.0F, 0.0F, -15.0F * DEG));
        PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(114, 25).addBox(-1.0F, -3.5F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, -4.0F, -1.0F, 0.0F, 0.0F, 15.0F * DEG));
        PartDefinition whiskers_l = head.addOrReplaceChild("whiskers_l", CubeListBuilder.create().texOffs(54, 33).addBox(-4.0F, -1.0F, 0.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -1.5F, -7.0F));
        PartDefinition whiskers_r = head.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(62, 33).addBox(0.0F, -1.0F, 0.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -1.5F, -7.0F));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(72, 25).addBox(-2.5F, -1.5F, -2.0F, 5.0F, 1.5F, 4.0F, new CubeDeformation(0.0F)).texOffs(28, 33).addBox(1.5F, -3.0F, -2.0F, 1.0F, 1.5F, 1.0F, new CubeDeformation(0.0F)).texOffs(32, 33).addBox(-2.5F, -3.0F, -2.0F, 1.0F, 1.5F, 1.0F, new CubeDeformation(0.0F)).texOffs(36, 33).addBox(1.5F, -3.0F, 1.0F, 1.0F, 1.5F, 1.0F, new CubeDeformation(0.0F)).texOffs(40, 33).addBox(-2.5F, -3.0F, 1.0F, 1.0F, 1.5F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 33).addBox(-0.5F, -4.0F, -2.2F, 1.0F, 2.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.5F, -1.5F));
        PartDefinition arm_l = body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(22, 15).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, -10.5F, -0.5F, -5.0F * DEG, 0.0F, -8.0F * DEG));
        PartDefinition hand_l = arm_l.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(82, 15).addBox(-1.3F, 0.0F, -1.3F, 2.6F, 5.0F, 2.6F, new CubeDeformation(0.0F)).texOffs(0, 33).addBox(-1.5F, 5.0F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(70, 33).addBox(-1.3F, 7.0F, -2.6F, 2.6F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, 15.0F * DEG, 0.0F, 0.0F));
        PartDefinition arm_r = body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(34, 15).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -10.5F, -0.5F, -5.0F * DEG, 0.0F, 8.0F * DEG));
        PartDefinition hand_r = arm_r.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(94, 15).addBox(-1.3F, 0.0F, -1.3F, 2.6F, 5.0F, 2.6F, new CubeDeformation(0.0F)).texOffs(12, 33).addBox(-1.5F, 5.0F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(78, 33).addBox(-1.3F, 7.0F, -2.6F, 2.6F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, 15.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(106, 15).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 3.5F, -30.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail_mid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create().texOffs(0, 25).addBox(-0.75F, -0.75F, 0.0F, 1.5F, 1.5F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 15.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail_tip = tail_mid.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(16, 25).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 12.0F * DEG, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}

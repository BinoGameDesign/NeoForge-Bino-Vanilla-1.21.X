package com.bino.binosvanillaplus.client.rat;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// Geometria do Rato Atroz (gerada do dire_rat.bbmodel). Mesmos ossos do rato: as animacoes do rato servem.
public final class DireRatModelData {
    private DireRatModelData() {}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition leg_bl = root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(48, 0).addBox(-1.2F, -0.5F, -1.8F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.8F, -3.5F, 3.5F));
        leg_bl.addOrReplaceChild("foot_bl_r", CubeListBuilder.create().texOffs(46, 8).addBox(-0.5F, 2.5F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.6F, 7.0F * DEG, 0.0F, 0.0F));
        PartDefinition leg_br = root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(0, 8).addBox(-0.8F, -0.5F, -1.7F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.8F, -3.5F, 3.5F));
        leg_br.addOrReplaceChild("foot_br_r", CubeListBuilder.create().texOffs(54, 8).addBox(-0.5F, 2.5F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.6F, 7.0F * DEG, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 0).addBox(-2.0F, -2.0F, -5.5F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.0F, 3.5F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(34, 17).addBox(-2.0F, -1.5F, -4.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.2F)).texOffs(51, 25).addBox(-1.5F, -0.7F, -6.1F, 3.0F, 3.0F, 3.0F, new CubeDeformation(-0.5F)).texOffs(23, 26).addBox(-1.5F, -1.3F, -6.9F, 3.0F, 3.0F, 3.0F, new CubeDeformation(-0.9F)), PartPose.offset(0.0F, -1.0F, -5.0F));
        head.addOrReplaceChild("teeth_r", CubeListBuilder.create().texOffs(14, 28).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0F, 3.1F, -4.9F, 1.0F * DEG, 0.0F, 0.0F));
        PartDefinition whiskers = head.addOrReplaceChild("whiskers", CubeListBuilder.create(), PartPose.offset(0.0F, 1.1F, -5.0F));
        whiskers.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(26, 13).addBox(-0.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(-2.8F, -0.6F, 0.0F, 0.0F, 0.0F, 10.0F * DEG));
        whiskers.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(26, 13).addBox(-0.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(-2.6F, 0.6F, 0.0F, 0.0F, 0.0F, -12.5F * DEG));
        PartDefinition whiskers2 = head.addOrReplaceChild("whiskers2", CubeListBuilder.create(), PartPose.offsetAndRotation(1.897F, 1.0225F, -5.0F, 0.0F, 0.0F, -180.0F * DEG));
        whiskers2.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(26, 13).addBox(-0.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(-1.0806F, -0.5786F, 0.0F, 0.0F, 0.0F, 10.0F * DEG));
        whiskers2.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(26, 13).addBox(-0.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(-0.8806F, 0.6214F, 0.0F, 0.0F, 0.0F, -12.5F * DEG));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 1.1F, -3.1F));
        jaw.addOrReplaceChild("jaw_r", CubeListBuilder.create().texOffs(51, 25).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(0.0F, 0.9F, -1.3F, 9.0F * DEG, 0.0F, 0.0F));
        PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create(), PartPose.offset(2.8F, -0.8F, -1.5F));
        ear_r.addOrReplaceChild("ear_r_r", CubeListBuilder.create().texOffs(0, 26).addBox(-2.5F, -3.0F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0F, 0.4F, 0.0F, 0.0F, -19.0F * DEG, 7.5F * DEG));
        PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create(), PartPose.offset(-1.2F, -1.5F, -1.5F));
        ear_l.addOrReplaceChild("ear_l_r", CubeListBuilder.create().texOffs(0, 26).addBox(-2.5F, -3.0F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0F, 1.1F, 0.0F, 0.0F, 17.0F * DEG, -12.5F * DEG));
        PartDefinition leg_fl = body.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(13, 15).addBox(-0.4F, 2.2F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offset(-1.2F, 0.0F, -4.5F));
        leg_fl.addOrReplaceChild("leg_fl_r", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F * DEG));
        PartDefinition leg_fr = body.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(19, 15).addBox(-0.6F, 2.2F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offset(1.2F, 0.0F, -4.5F));
        leg_fr.addOrReplaceChild("leg_fr_r", CubeListBuilder.create().texOffs(42, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F * DEG));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(10, 8).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, 4.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail_tip = tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(20, 8).addBox(-0.5F, -0.5F, -0.4F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 6.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail_tip2 = tail_tip.addOrReplaceChild("tail_tip2", CubeListBuilder.create().texOffs(20, 8).addBox(-0.5F, -0.5F, -0.4F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 3.5F, 10.0F * DEG, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}

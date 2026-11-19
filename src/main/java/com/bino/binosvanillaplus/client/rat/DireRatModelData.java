package com.bino.binosvanillaplus.client.rat;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de dire_rat.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class DireRatModelData {
    private DireRatModelData() {}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition leg_bl = root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(49, 0).addBox(-1.0F, -1.7F, -1.8F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)), PartPose.offset(-1.8F, -3.5F, 3.5F));
        leg_bl.addOrReplaceChild("stone_r", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, 0.5F, 0.7F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(-2.6568F, 0.9382F, -1.6F, 0.0F, 0.0F, -104.0F * DEG));
        leg_bl.addOrReplaceChild("leg_bl_r", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(-0.3477F, 1.9991F, 0.3F, 25.0F * DEG, 0.0F, -2.0F * DEG));
        leg_bl.addOrReplaceChild("foot_bl_r", CubeListBuilder.create().texOffs(46, 8).addBox(-0.5F, 2.5F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.0F, -0.6F, 7.0F * DEG, 0.0F, 0.0F));
        PartDefinition leg_br = root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(1, 8).addBox(0.2F, -1.7F, -1.7F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)), PartPose.offset(1.8F, -3.5F, 3.5F));
        leg_br.addOrReplaceChild("leg_br_r", CubeListBuilder.create().texOffs(42, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0.6F, 0.1F, -0.3F, 25.0F * DEG, 0.0F, 1.0F * DEG));
        leg_br.addOrReplaceChild("foot_br_r", CubeListBuilder.create().texOffs(54, 8).addBox(-0.5F, 2.5F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, 0.0F, -0.6F, 7.0F * DEG, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 3.5F));
        body.addOrReplaceChild("chest_r", CubeListBuilder.create().texOffs(18, 0).addBox(-2.0F, -2.0F, -5.5F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(0.0F, -1.8F, 0.0F, 13.0F * DEG, 0.0F, 0.0F));
        body.addOrReplaceChild("belly_r", CubeListBuilder.create().texOffs(18, 0).addBox(-2.0F, -2.0F, -5.5F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, -0.2F, 1.0F, 5.0F * DEG, 0.0F, 0.0F));
        body.addOrReplaceChild("rump_r", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 4.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, -1.6F, 0.0F, -2.0F * DEG, 0.0F, 0.0F));
        body.addOrReplaceChild("stone_r", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(1.6F, -3.1F, 0.0F, 0.0F, 0.0F, 9.0F * DEG));
        body.addOrReplaceChild("stone_r2", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, 0.15F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(-1.9223F, -3.9161F, -4.8F, 9.0F * DEG, 0.0F, -23.0F * DEG));
        body.addOrReplaceChild("stone_r3", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -0.65F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.6F)), PartPose.offsetAndRotation(-1.9223F, -3.9161F, -4.8F, 9.0F * DEG, 0.0F, -23.0F * DEG));
        body.addOrReplaceChild("stone_r4", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -1.35F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(-1.9223F, -3.9161F, -4.8F, 9.0F * DEG, 0.0F, -23.0F * DEG));
        body.addOrReplaceChild("stone_r5", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(1.2F, -3.1F, 0.6F, 0.0F, 0.0F, -8.0F * DEG));
        body.addOrReplaceChild("stone_r6", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(0.6F, -3.1F, -1.6F, 0.0F, 0.0F, -8.0F * DEG));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(34, 17).addBox(-2.0F, -1.5F, -4.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.2F)), PartPose.offset(0.0F, -1.0F, -5.0F));
        head.addOrReplaceChild("snout_r", CubeListBuilder.create().texOffs(38, 27).addBox(-1.2F, -0.4F, -0.9F, 3.0F, 2.0F, 2.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(-0.3223F, -0.7586F, -4.3F, 5.0F * DEG, 0.0F, 0.0F));
        head.addOrReplaceChild("snout_r2", CubeListBuilder.create().texOffs(51, 25).addBox(-1.5888F, -1.8944F, -1.2062F, 3.0F, 3.0F, 3.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0888F, 1.4107F, -4.6F, 6.0F * DEG, 0.0F, 0.0F));
        head.addOrReplaceChild("snout_r3", CubeListBuilder.create().texOffs(23, 26).addBox(-1.5888F, -2.4804F, -2.0165F, 3.0F, 3.0F, 3.0F, new CubeDeformation(-0.9F)), PartPose.offsetAndRotation(0.0888F, 1.4107F, -4.6F, 12.0F * DEG, 0.0F, 0.0F));
        head.addOrReplaceChild("teeth_r", CubeListBuilder.create().texOffs(14, 28).addBox(-1.0888F, -0.0107F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0888F, 1.4107F, -4.7F, 7.0F * DEG, 0.0F, 0.0F));
        PartDefinition whiskers = head.addOrReplaceChild("whiskers", CubeListBuilder.create(), PartPose.offset(0.0F, 1.1F, -4.7F));
        whiskers.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(26, 13).addBox(-3.3801F, -0.2579F, -0.5984F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0888F, 0.3107F, 0.1F, 5.9918F * DEG, -0.3134F * DEG, 2.9836F * DEG));
        whiskers.addOrReplaceChild("whiskers_r2", CubeListBuilder.create().texOffs(26, 13).addBox(-3.468F, -0.6965F, -0.6124F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0888F, 0.3107F, 0.1F, 5.9095F * DEG, -1.04F * DEG, 9.9463F * DEG));
        whiskers.addOrReplaceChild("whiskers_r3", CubeListBuilder.create().texOffs(26, 13).addBox(-3.3395F, -0.1146F, -0.5827F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0888F, 0.3107F, 0.1F, 5.8588F * DEG, 1.2964F * DEG, -12.4337F * DEG));
        PartDefinition whiskers2 = head.addOrReplaceChild("whiskers2", CubeListBuilder.create(), PartPose.offsetAndRotation(1.897F, 1.0225F, -4.7F, 0.0F, 0.0F, -180.0F * DEG));
        whiskers2.addOrReplaceChild("whiskers_r", CubeListBuilder.create().texOffs(26, 13).addBox(-3.5516F, -1.1707F, -0.6228F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(1.8082F, -0.0882F, 0.1F, -5.9095F * DEG, 1.04F * DEG, 9.9463F * DEG));
        whiskers2.addOrReplaceChild("whiskers_r2", CubeListBuilder.create().texOffs(26, 13).addBox(-3.2137F, -0.6822F, -0.6437F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(1.8082F, -0.0882F, 0.1F, -5.8588F * DEG, -1.2964F * DEG, -12.4337F * DEG));
        whiskers2.addOrReplaceChild("whiskers_r3", CubeListBuilder.create().texOffs(26, 13).addBox(-3.1042F, -0.8515F, -0.6437F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(1.8082F, -0.0882F, 0.1F, -5.998F * DEG, -0.1568F * DEG, -1.4918F * DEG));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 1.1F, -3.1F));
        jaw.addOrReplaceChild("jaw_r", CubeListBuilder.create().texOffs(51, 25).addBox(-1.5888F, -0.0498F, -1.1286F, 3.0F, 2.0F, 3.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(0.0888F, 0.5107F, -1.5F, 17.0F * DEG, 0.0F, 0.0F));
        PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create(), PartPose.offset(2.8F, -0.8F, -1.5F));
        ear_r.addOrReplaceChild("ear_r_r", CubeListBuilder.create().texOffs(0, 26).addBox(-2.8F, -2.7F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(0.0F, 0.4F, 0.0F, 0.0F, -11.0F * DEG, 7.5F * DEG));
        PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create(), PartPose.offset(-1.2F, -1.5F, -1.5F));
        ear_l.addOrReplaceChild("ear_l_r", CubeListBuilder.create().texOffs(0, 21).addBox(-2.3F, -2.8F, -0.7F, 4.0F, 4.0F, 1.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(0.0F, 1.1F, 0.0F, 0.0F, 0.0F, -12.5F * DEG));
        PartDefinition leg_fl = body.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(13, 15).addBox(-1.4F, 2.2F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offset(-1.2F, 0.0F, -4.5F));
        leg_fl.addOrReplaceChild("leg_fl_r", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-1.1F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F * DEG));
        leg_fl.addOrReplaceChild("claw_fl_r", CubeListBuilder.create().texOffs(15, 23).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)), PartPose.offsetAndRotation(-0.9F, 0.5F, -1.1F, 15.0F * DEG, 0.0F, 0.0F));
        PartDefinition leg_fr = body.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(19, 15).addBox(0.3F, 2.2F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offset(1.2F, 0.0F, -4.5F));
        leg_fr.addOrReplaceChild("stone_r", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.7F)), PartPose.offsetAndRotation(0.7F, 0.8F, 0.0F, 0.0F, 0.0F, 104.0F * DEG));
        leg_fr.addOrReplaceChild("stone_r2", CubeListBuilder.create().texOffs(22, 20).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.8F)), PartPose.offsetAndRotation(0.6F, 1.5F, 0.2F, 0.0F, 0.0F, 104.0F * DEG));
        leg_fr.addOrReplaceChild("leg_fr_r", CubeListBuilder.create().texOffs(42, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.9F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F * DEG));
        leg_fr.addOrReplaceChild("claw_fr_r", CubeListBuilder.create().texOffs(15, 23).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)), PartPose.offsetAndRotation(0.8F, 0.5F, -1.1F, 15.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(10, 8).addBox(-0.5F, -2.4951F, 0.1395F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, 4.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail_tip = tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(20, 8).addBox(-0.5F, -2.4696F, -0.0527F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 6.0F * DEG, 0.0F, 0.0F));
        PartDefinition tail_tip2 = tail_tip.addOrReplaceChild("tail_tip2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 3.5F, 10.0F * DEG, 0.0F, 0.0F));
        tail_tip2.addOrReplaceChild("tail_tip2_r", CubeListBuilder.create().texOffs(20, 8).addBox(-0.5F, -2.3794F, 0.684F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.4F, 3.0F * DEG, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}

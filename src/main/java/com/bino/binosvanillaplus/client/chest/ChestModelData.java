package com.bino.binosvanillaplus.client.chest;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

// GERADO por tools/blockbench/bbmodel_to_java.py a partir de chest.bbmodel -- nao edite a mao, edite no Blockbench e rode o script.
public final class ChestModelData {
    private ChestModelData() {}
    private static final float DEG = (float) (Math.PI / 180.0);

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition bottom = root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(82, 46).addBox(-5.5F, -8.1F, -7.6F, 4.0F, 2.4F, 1.0F, new CubeDeformation(-0.4F)).texOffs(92, 46).addBox(1.6F, -8.1F, -7.6F, 4.0F, 2.4F, 1.0F, new CubeDeformation(-0.4F)).texOffs(0, 0).addBox(-7.0F, -8.0F, -7.0F, 14.0F, 8.0F, 14.0F, new CubeDeformation(0.0F)).texOffs(74, 37).addBox(5.4F, -1.7F, 5.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(82, 37).addBox(-7.6F, -1.7F, 5.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(120, 22).addBox(-7.4F, -8.8F, 5.4F, 2.0F, 8.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(0, 37).addBox(-7.4F, -8.8F, -7.4F, 2.0F, 8.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(8, 37).addBox(5.4F, -8.8F, -7.4F, 2.0F, 8.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(16, 37).addBox(5.4F, -8.8F, 5.4F, 2.0F, 8.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(90, 37).addBox(-7.6F, -1.7F, -7.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(98, 37).addBox(5.6F, -1.7F, -7.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        bottom.addOrReplaceChild("cube_r", CubeListBuilder.create().texOffs(46, 46).addBox(-12.9F, 1.1F, 9.8F, 13.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -11.3F, -4.4F, -90.0F * DEG, 0.0F, 0.0F));
        bottom.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 46).addBox(-12.9F, -11.9F, 9.8F, 13.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -11.3F, -4.4F, -90.0F * DEG, 0.0F, 0.0F));
        bottom.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(120, 0).addBox(-0.9F, -11.3F, 9.8F, 2.0F, 13.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -11.3F, -4.4F, -90.0F * DEG, 0.0F, 0.0F));
        bottom.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(112, 0).addBox(-13.8F, -11.3F, 9.8F, 2.0F, 13.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -11.3F, -4.4F, -90.0F * DEG, 0.0F, 0.0F));
        PartDefinition handle = bottom.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(122, 46).addBox(-7.8F, -1.65F, -0.55F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(122, 46).mirror().addBox(6.8F, -1.65F, -0.55F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.3F)).mirror(false), PartPose.offset(0.0F, -5.65F, -0.05F));
        handle.addOrReplaceChild("handle_r_r", CubeListBuilder.create().texOffs(56, 37).addBox(-0.4F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-7.6F, 0.65F, 0.05F, 5.0F * DEG, 0.0F, 0.0F));
        handle.addOrReplaceChild("handle_l_r", CubeListBuilder.create().texOffs(56, 37).mirror().addBox(-0.6F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(-0.2F)).mirror(false), PartPose.offsetAndRotation(7.6F, 0.65F, 0.05F, 5.0F * DEG, 0.0F, 0.0F));
        PartDefinition lid = root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(102, 46).addBox(-5.5F, -1.8F, -0.6F, 4.0F, 2.4F, 1.0F, new CubeDeformation(-0.4F)).texOffs(112, 46).addBox(1.6F, -1.8F, -0.6F, 4.0F, 2.4F, 1.0F, new CubeDeformation(-0.4F)).texOffs(56, 0).addBox(-7.0F, -4.0F, 0.0F, 14.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)).texOffs(24, 37).addBox(5.4F, -4.4F, 12.4F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(32, 37).addBox(-7.4F, -4.4F, 12.4F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(40, 37).addBox(-7.4F, -4.4F, -0.4F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(48, 37).addBox(5.4F, -4.4F, -0.4F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.3F)).texOffs(0, 22).addBox(-7.0F, -1.0F, 0.0F, 14.0F, 1.0F, 14.0F, new CubeDeformation(0.2F)).texOffs(72, 22).addBox(-7.0F, -5.2F, 2.0F, 14.0F, 2.0F, 10.0F, new CubeDeformation(-0.1F)), PartPose.offset(0.0F, -7.9F, -7.0F));
        lid.addOrReplaceChild("cube_r", CubeListBuilder.create().texOffs(106, 37).addBox(-1.0F, -0.4F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -3.4F, 2.6F, -90.0F * DEG, 0.0F, 0.0F));
        lid.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(56, 22).addBox(-1.0F, -9.8F, -2.4F, 2.0F, 11.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -3.4F, 2.6F, -90.0F * DEG, 0.0F, 0.0F));
        lid.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(64, 22).addBox(-13.8F, -9.8F, -2.4F, 2.0F, 11.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -3.4F, 2.6F, -90.0F * DEG, 0.0F, 0.0F));
        lid.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(114, 37).addBox(-1.0F, -10.4F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -3.4F, 2.6F, -90.0F * DEG, 0.0F, 0.0F));
        lid.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 46).addBox(-13.8F, -10.4F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -3.4F, 2.6F, -90.0F * DEG, 0.0F, 0.0F));
        lid.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(8, 46).addBox(-13.8F, -0.4F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(6.4F, -3.4F, 2.6F, -90.0F * DEG, 0.0F, 0.0F));
        PartDefinition lock = lid.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(64, 37).addBox(-2.0F, -2.6F, 13.9F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(76, 46).addBox(-1.0F, -2.6F, 14.4F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offset(0.0F, 1.1F, 0.0F));
        PartDefinition hinge = root.addOrReplaceChild("hinge", CubeListBuilder.create().texOffs(0, 50).addBox(1.6F, -0.5F, -0.7F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)).texOffs(10, 50).addBox(-5.5F, -0.5F, -0.7F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offset(0.0F, -7.9F, -7.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}

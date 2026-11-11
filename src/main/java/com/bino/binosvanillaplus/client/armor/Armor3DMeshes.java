package com.bino.binosvanillaplus.client.armor;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

// ARQUIVO GERADO: nao edite a mao. Mude o .bbmodel em tools/armor_models e rode tools/bbmodel_armor.py
@SuppressWarnings("unused")
public final class Armor3DMeshes {
    private Armor3DMeshes() {}

    // jade_boots (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/jade_boots.bbmodel)
    public static LayerDefinition jadeBoots() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(1.1F))
                .texOffs(18, 58).addBox(-3F, 10F, -4.9F, 6F, 3F, 2F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        rightleg.addOrReplaceChild("right_leg_r0", CubeListBuilder.create().texOffs(38, 59).addBox(-3F, -1.5F, 0F, 6F, 4F, 0F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(0F, 4.5F, -3.7F, 0.17453F, -0F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(1.15F)).mirror(false)
                .texOffs(18, 58).addBox(-3F, 10F, -4.95F, 6F, 3F, 2F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        leftleg.addOrReplaceChild("left_leg_r0", CubeListBuilder.create().texOffs(38, 59).addBox(-3F, -1.5F, 0F, 6F, 4F, 0F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(0F, 4.5F, -3.75F, 0.17453F, -0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // jade_chestplate (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/jade_chestplate.bbmodel)
    public static LayerDefinition jadeChestplate() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(1.1F))
                .texOffs(4, 39).addBox(-1F, 10.6F, -4F, 2F, 2F, 2F, new CubeDeformation(0F))
                .texOffs(14, 39).addBox(-1F, 10.6F, -3.1F, 2F, 2F, 2F, new CubeDeformation(0.6F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1F))
                .texOffs(41, 38).addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1.2F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(22, 43).mirror().addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1F)).mirror(false), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // jade_helmet (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/jade_helmet.bbmodel)
    public static LayerDefinition jadeHelmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F))
                .texOffs(23, 35).addBox(-1F, -8.5F, -6.4F, 2F, 6F, 2F, new CubeDeformation(0F))
                .texOffs(31, 5).addBox(-4F, -10F, -4F, 8F, 2F, 9F, new CubeDeformation(-0.1F))
                .texOffs(41, 47).addBox(4.86863F, -4.79443F, -4.01368F, 1F, 5F, 10F, new CubeDeformation(-0.1F))
                .texOffs(41, 47).mirror().addBox(-5.86863F, -4.79443F, -4.01368F, 1F, 5F, 10F, new CubeDeformation(-0.1F)).mirror(false), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("head_r0", CubeListBuilder.create().texOffs(17, 54).addBox(-1F, -4.5F, 0F, 2F, 9F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(0F, -6.5F, -6F, -0F, -0F, -1.5708F));
        head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 42).addBox(0.1913F, 0.41466F, -0.98632F, 2F, 3F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(7.86863F, -5.49443F, -0.01368F, 2.9458F, -0.72025F, -1.2787F));
        head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(24, 46).addBox(-0.51394F, -1.33705F, -0.98632F, 2F, 3F, 2F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(7.86863F, -5.49443F, -0.01368F, 2.52956F, -0.44922F, -0.55402F));
        head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(27, 53).addBox(-0.13163F, -3.58103F, -1.02236F, 2F, 3F, 2F, new CubeDeformation(-0.2F)),
                PartPose.offsetAndRotation(7.86863F, -5.49443F, -0.01368F, 2.45074F, -0.24797F, -0.31866F));
        head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(40, 24).addBox(-1F, -9F, -4F, 1F, 8F, 10F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(1.16863F, 1.20557F, 6.28632F, -0F, -1.5708F, 0F));
        head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(27, 53).mirror().addBox(-1.86837F, -3.58103F, -1.02236F, 2F, 3F, 2F, new CubeDeformation(-0.2F)).mirror(false),
                PartPose.offsetAndRotation(-7.86863F, -5.49443F, -0.01368F, 2.45074F, 0.24797F, 0.31866F));
        head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(24, 46).mirror().addBox(-1.48606F, -1.33705F, -0.98632F, 2F, 3F, 2F, new CubeDeformation(-0.1F)).mirror(false),
                PartPose.offsetAndRotation(-7.86863F, -5.49443F, -0.01368F, 2.52956F, 0.44922F, 0.55402F));
        head.addOrReplaceChild("head_r7", CubeListBuilder.create().texOffs(0, 42).mirror().addBox(-2.1913F, 0.41466F, -0.98632F, 2F, 3F, 2F, new CubeDeformation(0F)).mirror(false),
                PartPose.offsetAndRotation(-7.86863F, -5.49443F, -0.01368F, 2.9458F, 0.72025F, 1.2787F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    // jade_leggings (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/jade_leggings.bbmodel)
    public static LayerDefinition jadeLeggings() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.5F))
                .texOffs(18, 56).addBox(-2.6F, 12F, 2F, 5F, 7F, 1F, new CubeDeformation(0F))
                .texOffs(18, 56).addBox(-2.6F, 12F, -3F, 5F, 7F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.5F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.5F)).mirror(false), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

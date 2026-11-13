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

    // rubi_chestplate (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/rubi_chestplate.bbmodel)
    public static LayerDefinition rubiChestplate() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(1.1F))
                .texOffs(0, 52).addBox(-4.5F, -0.1F, -4F, 9F, 10F, 2F, new CubeDeformation(0F))
                .texOffs(0, 52).addBox(-4.5F, -0.1F, 2F, 9F, 10F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1F)), PartPose.offset(-5F, 2F, 0F));
        rightarm.addOrReplaceChild("right_arm_r0", CubeListBuilder.create().texOffs(42, 49).mirror().addBox(-1F, -2F, -3.5F, 4F, 8F, 7F, new CubeDeformation(-0.1F)).mirror(false),
                PartPose.offsetAndRotation(-3.5F, 0.3F, 0F, -0F, -0F, 0.21817F));
        rightarm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(42, 49).mirror().addBox(-1F, -2F, -3.5F, 4F, 8F, 7F, new CubeDeformation(0F)).mirror(false),
                PartPose.offsetAndRotation(-3.5F, -1.7F, 0F, -0F, -0F, 0.21817F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1F)).mirror(false), PartPose.offset(5F, 2F, 0F));
        leftarm.addOrReplaceChild("left_arm_r0", CubeListBuilder.create().texOffs(42, 49).addBox(-3F, -2F, -3.5F, 4F, 8F, 7F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(3.5F, -1.7F, 0F, -0F, -0F, -0.21817F));
        leftarm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(42, 49).addBox(-3F, -2F, -3.5F, 4F, 8F, 7F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(3.5F, 0.3F, 0F, -0F, -0F, -0.21817F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // rubi_helmet (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/rubi_helmet.bbmodel)
    public static LayerDefinition rubiHelmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F))
                .texOffs(32, 6).addBox(-4F, -10F, -4F, 8F, 2F, 8F, new CubeDeformation(-0.1F))
                .texOffs(0, 17).addBox(-6.5F, -7.9F, -6.6F, 13F, 2F, 2F, new CubeDeformation(-0.3F))
                .texOffs(70, 48).addBox(3.46863F, -8.09443F, -6.71368F, 2F, 14F, 2F, new CubeDeformation(-0.7F))
                .texOffs(70, 48).mirror().addBox(-5.46863F, -8.09443F, -6.71368F, 2F, 14F, 2F, new CubeDeformation(-0.7F)).mirror(false), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("head_r0", CubeListBuilder.create().texOffs(41, 47).addBox(-1F, -7F, -4F, 1F, 7F, 10F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(7.56863F, -0.59443F, -1.01368F, -0F, -0F, -0.34907F));
        head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(41, 47).addBox(-1F, -7F, -4F, 1F, 7F, 10F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(-6.63137F, -0.19443F, -1.01368F, -0F, -0F, 0.34907F));
        head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(41, 47).addBox(-0.5F, -3.5F, -5F, 1F, 7F, 10F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(-0.00415F, -3.65436F, 5.98632F, 1.5708F, -1.309F, -1.5708F));
        head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(0, 17).addBox(-6.5F, -1F, -1F, 13F, 2F, 2F, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0F, -6.9F, 5.4F, -0F, 3.14159F, 0F));
        head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(0, 22).addBox(-5.5F, -2F, -1F, 11F, 2F, 2F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(-5.5F, -5.9F, -0.1F, -0F, -1.5708F, 0F));
        head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(0, 22).addBox(-5.5F, -2F, -1F, 11F, 2F, 2F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(5.6F, -5.9F, -0.1F, -0F, -1.5708F, 0F));
        head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(112, 30).addBox(-4F, -8F, 1F, 8F, 8F, 0F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-0.13137F, -7.69443F, -6.61368F, -0.2618F, -0F, 0F));
        head.addOrReplaceChild("head_r7", CubeListBuilder.create().texOffs(1, 29).addBox(-1F, -2F, -1F, 4F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(6.46863F, -4.79443F, -5.01368F, -0F, -0F, -0.34907F));
        head.addOrReplaceChild("head_r8", CubeListBuilder.create().texOffs(1, 29).addBox(-1F, -2F, -1F, 4F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-8.53137F, -5.19443F, -5.01368F, -0F, -0F, 0.30543F));
        head.addOrReplaceChild("head_r9", CubeListBuilder.create().texOffs(0, 33).addBox(-1F, -2F, -1F, 4F, 3F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(6.46863F, -6.79443F, -5.71368F, -0F, -0F, -0.34907F));
        head.addOrReplaceChild("head_r10", CubeListBuilder.create().texOffs(0, 40).addBox(-1F, -2F, -1F, 4F, 3F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-8.53137F, -7.39443F, -5.51368F, -0F, -0F, 0.30543F));
        head.addOrReplaceChild("head_r11", CubeListBuilder.create().texOffs(1, 62).addBox(-4F, -2F, 1F, 8F, 2F, 0F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-0.13137F, -7.69443F, -7.01368F, -0.2618F, -0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    // sapphire_boots (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/sapphire_boots.bbmodel)
    public static LayerDefinition sapphireBoots() {
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

    // sapphire_chestplate (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/sapphire_chestplate.bbmodel)
    public static LayerDefinition sapphireChestplate() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(1.1F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1F))
                .texOffs(41, 38).addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1.2F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(22, 43).mirror().addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(1F)).mirror(false)
                .texOffs(22, 29).addBox(5.6F, -1F, -3.9F, 0F, 3F, 8F, new CubeDeformation(0F))
                .texOffs(32, 0).addBox(-2.4F, -4F, -3.9F, 8F, 3F, 8F, new CubeDeformation(0F))
                .texOffs(0, 1).addBox(-2.5F, 2F, -3.4F, 7F, 3F, 7F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // sapphire_helmet (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/sapphire_helmet.bbmodel)
    public static LayerDefinition sapphireHelmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("head_r0", CubeListBuilder.create().texOffs(18, 57).addBox(-2F, -2F, -1F, 3F, 2F, 5F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-3.875F, -8.3127F, -3.1424F, 0.74176F, -0F, 0F));
        head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(18, 44).addBox(-1F, -2F, -1F, 3F, 2F, 5F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(3.875F, -8.3127F, -3.1424F, 0.74176F, -0F, 0F));
        head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(38, 54).addBox(-3.5F, -2F, -1F, 6F, 5F, 5F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(0.625F, -8.0362F, -1.0141F, 0.74176F, -0F, 0F));
        head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(40, 37).addBox(-2.5F, -2F, -1F, 4F, 5F, 8F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(0.625F, -11.159F, 3.3749F, 0.43633F, -0F, 0F));
        head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(40, 22).addBox(-0.8F, -0.8F, -0.8F, 4F, 5F, 8F, new CubeDeformation(-1F)),
                PartPose.offsetAndRotation(-1.075F, -14.8668F, 9.5778F, -0.43633F, -0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // sapphire_leggings (gerado por tools/bbmodel_armor.py a partir de tools/armor_models/sapphire_leggings.bbmodel)
    public static LayerDefinition sapphireLeggings() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.5F)), PartPose.offset(0F, 0F, 0F));
        body.addOrReplaceChild("body_r0", CubeListBuilder.create().texOffs(16, 56).addBox(-3F, -3.5F, -0.5F, 6F, 7F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-4.9F, 12.5F, 0F, -0F, 1.5708F, 0.1309F));
        body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(16, 56).addBox(-3.5F, -3.5F, -0.5F, 6F, 7F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(4.9F, 12.5F, 0.5F, -0F, -1.5708F, -0.1309F));
        body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(46, 53).addBox(-4F, -3.5F, -1.5F, 7F, 9F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(4.7F, 12.5F, -0.7F, -0F, 1.5708F, -0.1309F));
        body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(46, 53).addBox(-4F, -3.5F, -1.5F, 7F, 9F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-4.6F, 12.5F, 0.5F, -0F, -1.5708F, 0.1309F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.5F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.5F)).mirror(false), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

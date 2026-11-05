package com.bino.binosvanillaplus.client.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.tags.ItemTags;

// Personagem do inventario novo: o jogador (skin, armadura e itens nas maos) desenhado com um modelo PROPRIO que tem
// cotovelos e joelhos (cada braco/perna em duas partes), para poses de acao. Independe do EMF/Fresh Animations.
public final class BendyPlayer {
    private BendyPlayer() {}

    // pecas do modelo: cabeca, corpo, e para cada membro a parte de cima com a de baixo (filha) pendurada no cotovelo/joelho
    private static final class Rig {
        ModelPart root, head, body, rArm, rArmLow, lArm, lArmLow, rLeg, rLegLow, lLeg, lLegLow;

        Rig(ModelPart root) {
            this.root = root;
            head = root.getChild("head"); body = root.getChild("body");
            rArm = root.getChild("r_arm"); rArmLow = rArm.getChild("low");
            lArm = root.getChild("l_arm"); lArmLow = lArm.getChild("low");
            rLeg = root.getChild("r_leg"); rLegLow = rLeg.getChild("low");
            lLeg = root.getChild("l_leg"); lLegLow = lLeg.getChild("low");
        }

        void pose(ActionPose.Angles a, float breathe) {
            set(head, a.head()); set(body, a.body());
            set(rArm, a.rArm()); rArm.xRot += breathe; rArmLow.xRot = a.rElbow();
            set(lArm, a.lArm()); lArm.xRot -= breathe; lArmLow.xRot = a.lElbow();
            set(rLeg, a.rLeg()); rLegLow.xRot = a.rKnee();
            set(lLeg, a.lLeg()); lLegLow.xRot = a.lKnee();
        }

        private static void set(ModelPart p, float[] r) { p.xRot = r[0]; p.yRot = r[1]; p.zRot = r[2]; }
    }

    private static Rig wide, slim, armorOuter, armorInner;

    // membro de 12 de altura dividido em dois de 6; a parte de baixo usa a textura 6 linhas abaixo (lados certos)
    private static void limb(PartDefinition root, String name, int u, int v, int ou, int ov, float x0, float w,
                             float px, float py, CubeDeformation base, CubeDeformation over, boolean overlay, boolean mirror) {
        CubeListBuilder up = CubeListBuilder.create().mirror(mirror).texOffs(u, v).addBox(x0, -2, -2, w, 6, 4, base);
        if (overlay) up.texOffs(ou, ov).addBox(x0, -2, -2, w, 6, 4, over);
        PartDefinition top = root.addOrReplaceChild(name, up, PartPose.offset(px, py, 0));
        CubeListBuilder lo = CubeListBuilder.create().mirror(mirror).texOffs(u, v + 6).addBox(x0, 0, -2, w, 6, 4, base);
        if (overlay) lo.texOffs(ou, ov + 6).addBox(x0, 0, -2, w, 6, 4, over);
        top.addOrReplaceChild("low", lo, PartPose.offset(0, 4, 0));
    }

    private static void leg(PartDefinition root, String name, int u, int v, int ou, int ov, float px,
                            CubeDeformation base, CubeDeformation over, boolean overlay, boolean mirror) {
        CubeListBuilder up = CubeListBuilder.create().mirror(mirror).texOffs(u, v).addBox(-2, 0, -2, 4, 6, 4, base);
        if (overlay) up.texOffs(ou, ov).addBox(-2, 0, -2, 4, 6, 4, over);
        PartDefinition top = root.addOrReplaceChild(name, up, PartPose.offset(px, 12, 0));
        CubeListBuilder lo = CubeListBuilder.create().mirror(mirror).texOffs(u, v + 6).addBox(-2, 0, -2, 4, 6, 4, base);
        if (overlay) lo.texOffs(ou, ov + 6).addBox(-2, 0, -2, 4, 6, 4, over);
        top.addOrReplaceChild("low", lo, PartPose.offset(0, 6, 0));
    }

    private static Rig skinRig(boolean slimArms) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE, hat = new CubeDeformation(0.5f), layer = new CubeDeformation(0.25f);
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, none)
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, hat), PartPose.ZERO);
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, none)
                .texOffs(16, 32).addBox(-4, 0, -2, 8, 12, 4, layer), PartPose.ZERO);
        float w = slimArms ? 3 : 4;
        limb(r, "r_arm", 40, 16, 40, 32, slimArms ? -2 : -3, w, -5, slimArms ? 2.5f : 2, none, layer, true, false);
        limb(r, "l_arm", 32, 48, 48, 48, -1, w, 5, slimArms ? 2.5f : 2, none, layer, true, false);
        leg(r, "r_leg", 0, 16, 0, 32, -1.9f, none, layer, true, false);
        leg(r, "l_leg", 16, 48, 0, 48, 1.9f, none, layer, true, false);
        return new Rig(LayerDefinition.create(mesh, 64, 64).bakeRoot());
    }

    private static Rig armorRig(CubeDeformation d) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, d), PartPose.ZERO);
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, d), PartPose.ZERO);
        limb(r, "r_arm", 40, 16, 0, 0, -3, 4, -5, 2, d, d, false, false);
        limb(r, "l_arm", 40, 16, 0, 0, -1, 4, 5, 2, d, d, false, true);
        leg(r, "r_leg", 0, 16, 0, 0, -1.9f, d, d, false, false);
        leg(r, "l_leg", 0, 16, 0, 0, 1.9f, d, d, false, true);
        return new Rig(LayerDefinition.create(mesh, 64, 32).bakeRoot());
    }

    private static void ensure() {
        if (wide != null) return;
        wide = skinRig(false);
        slim = skinRig(true);
        armorOuter = armorRig(new CubeDeformation(1.0f));
        armorInner = armorRig(new CubeDeformation(0.5f));
    }

    // desenha o jogador centrado em (cx, cy), 'pxPerBlock' pixels de GUI por bloco, virado 'yaw' graus
    public static void render(GuiGraphics g, AbstractClientPlayer p, float cx, float cy, float pxPerBlock, float yaw, ActionPose.Pose pose) {
        ensure();
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin skin = p.getSkin();
        Rig rig = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        ActionPose.Angles a = ActionPose.of(pose);
        float breathe = Mth.sin((p.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(false)) * 0.07f) * 0.03f;
        rig.pose(a, breathe);
        armorOuter.pose(a, breathe);
        armorInner.pose(a, breathe);

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 150);
        ps.scale(pxPerBlock, pxPerBlock, -pxPerBlock);
        ps.mulPose(Axis.XP.rotationDegrees(-6f));                  // camera um pouco de cima
        ps.mulPose(Axis.YP.rotationDegrees(yaw));                   // de frente para quem olha, girado 'yaw'
        ps.translate(0, -8 / 16f, 0);                               // centro do corpo (cabeca -8, pes 24)
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        int light = 0xF000F0;

        rig.root.render(ps, buf.getBuffer(RenderType.entityTranslucent(skin.texture())), light, OverlayTexture.NO_OVERLAY);
        armor(ps, buf, p, EquipmentSlot.HEAD, armorOuter, light);
        armor(ps, buf, p, EquipmentSlot.CHEST, armorOuter, light);
        armor(ps, buf, p, EquipmentSlot.LEGS, armorInner, light);
        armor(ps, buf, p, EquipmentSlot.FEET, armorOuter, light);

        boolean rightHanded = p.getMainArm() == HumanoidArm.RIGHT;
        held(ps, buf, p, rightHanded ? p.getMainHandItem() : p.getOffhandItem(), rig.rArm, rig.rArmLow, false, light);
        held(ps, buf, p, rightHanded ? p.getOffhandItem() : p.getMainHandItem(), rig.lArm, rig.lArmLow, true, light);

        buf.endBatch();
        Lighting.setupFor3DItems();
        ps.popPose();
    }

    private static void armor(PoseStack ps, MultiBufferSource buf, AbstractClientPlayer p, EquipmentSlot slot, Rig rig, int light) {
        ItemStack st = p.getItemBySlot(slot);
        if (!(st.getItem() instanceof ArmorItem ai) || ai.getEquipmentSlot() != slot) return;
        boolean inner = slot == EquipmentSlot.LEGS;
        ModelPart[] parts = switch (slot) {
            case HEAD -> new ModelPart[]{rig.head};
            case CHEST -> new ModelPart[]{rig.body, rig.rArm, rig.lArm};
            case LEGS -> new ModelPart[]{rig.body, rig.rLeg, rig.lLeg};
            default -> new ModelPart[]{rig.rLeg, rig.lLeg};
        };
        Holder<ArmorMaterial> mat = ai.getMaterial();
        for (ArmorMaterial.Layer layer : mat.value().layers()) {
            int color = layer.dyeable() ? DyedItemColor.getOrDefault(st, -6265536) | 0xFF000000 : -1;
            ResourceLocation tex = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(p, st, layer, inner, slot);
            for (ModelPart part : parts) {
                part.render(ps, buf.getBuffer(RenderType.armorCutoutNoCull(tex)), light, OverlayTexture.NO_OVERLAY, color);
                if (st.hasFoil()) part.render(ps, buf.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
            }
        }
    }

    // item na mao: preso na ponta da parte de baixo do braco (como o jogo faz, so que a partir do cotovelo)
    private static void held(PoseStack ps, MultiBufferSource buf, AbstractClientPlayer p, ItemStack st, ModelPart arm, ModelPart low, boolean left, int light) {
        if (st.isEmpty()) return;
        ps.pushPose();
        arm.translateAndRotate(ps);
        low.translateAndRotate(ps);
        ps.mulPose(Axis.XP.rotationDegrees(-90f));
        ps.mulPose(Axis.YP.rotationDegrees(180f));
        ps.translate((left ? -1 : 1) / 16f, 0.125f, -0.375f);
        Minecraft.getInstance().getItemRenderer().renderStatic(p, st,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, ps, buf, p.level(), light,
                OverlayTexture.NO_OVERLAY, p.getId());
        ps.popPose();
    }
}

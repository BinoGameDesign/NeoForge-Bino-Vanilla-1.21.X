package com.bino.binosvanillaplus.client.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.lwjgl.opengl.GL11;
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
// cotovelos e joelhos, para poses de acao. Independe do EMF/Fresh Animations.
// Bracos e pernas sao uma malha propria ("bendy"): uma caixa so, com a textura igual a do jogo, que dobra no meio
// (o anel do cotovelo/joelho gira metade do angulo, como uma junta). Sem caixas sobrepostas, sem tampas no lugar errado,
// e as faces de dentro nao sao desenhadas (era por elas que apareciam os quadrados pretos na mao).
public final class BendyPlayer {
    private BendyPlayer() {}

    public static net.minecraft.client.renderer.ShaderInstance shader;   // bino_figure (registrado em MapClientEvents)

    // pecas: cabeca e corpo sao caixas normais; bracos/pernas sao so "ossos" (posicao + rotacao) para a malha bendy
    private static final class Rig {
        final ModelPart root, head, body, rArm, rArmLow, lArm, lArmLow, rLeg, rLegLow, lLeg, lLegLow;

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
    private static boolean slimArms;

    private static void bones(PartDefinition r, float armY) {
        PartDefinition ra = r.addOrReplaceChild("r_arm", CubeListBuilder.create(), PartPose.offset(-5, armY, 0));
        ra.addOrReplaceChild("low", CubeListBuilder.create(), PartPose.offset(0, 4, 0));
        PartDefinition la = r.addOrReplaceChild("l_arm", CubeListBuilder.create(), PartPose.offset(5, armY, 0));
        la.addOrReplaceChild("low", CubeListBuilder.create(), PartPose.offset(0, 4, 0));
        PartDefinition rl = r.addOrReplaceChild("r_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12, 0));
        rl.addOrReplaceChild("low", CubeListBuilder.create(), PartPose.offset(0, 6, 0));
        PartDefinition ll = r.addOrReplaceChild("l_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12, 0));
        ll.addOrReplaceChild("low", CubeListBuilder.create(), PartPose.offset(0, 6, 0));
    }

    private static Rig skinRig(boolean slimArms) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE, hat = new CubeDeformation(0.5f), layer = new CubeDeformation(0.25f);
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, none)
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, hat), PartPose.ZERO);
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, none)
                .texOffs(16, 32).addBox(-4, 0, -2, 8, 12, 4, layer), PartPose.ZERO);
        bones(r, slimArms ? 2.5f : 2);
        return new Rig(LayerDefinition.create(mesh, 64, 64).bakeRoot());
    }

    private static Rig armorRig(CubeDeformation d) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, d), PartPose.ZERO);
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, d), PartPose.ZERO);
        bones(r, 2);
        return new Rig(LayerDefinition.create(mesh, 64, 32).bakeRoot());
    }

    private static void ensure() {
        if (wide != null) return;
        wide = skinRig(false);
        slim = skinRig(true);
        armorOuter = armorRig(new CubeDeformation(1.0f));
        armorInner = armorRig(new CubeDeformation(0.5f));
    }

    // ------------------------------------------------------------------ malha bendy

    private static float frontSign = 1f;   // sinal da area na tela de uma face virada para quem olha (para nao desenhar as de tras)

    private static void calibrate(PoseStack ps) {
        Matrix4f m = ps.last().pose();
        Vector3f a = m.transformPosition(1, 0, 0, new Vector3f()), b = m.transformPosition(0, 0, 0, new Vector3f()), c = m.transformPosition(0, 1, 0, new Vector3f());
        frontSign = Math.signum((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x));
        if (frontSign == 0) frontSign = 1;
    }

    // Um braco/perna: caixa (x0,y0,-2) tamanho (w,h,4), textura a partir de (u,v) no layout do jogo; dobra em y=joint.
    // endCap = desenha a ponta (mao/pe). Armadura e mangas ficam sem ponta (aberta) e as faces de dentro nao aparecem.
    private static void limb(PoseStack ps, VertexConsumer vc, ModelPart bone, ModelPart low, int u, int v, float x0, float y0,
                             float w, float h, float joint, float grow, boolean mirror, boolean endCap,
                             float texW, float texH, int light, int overlay, int color) {
        final float d = 4, z0 = -2;
        ps.pushPose();
        bone.translateAndRotate(ps);
        float bend = low.xRot;
        float minX = x0 - grow, maxX = x0 + w + grow, minY = y0 - grow, maxY = y0 + h + grow, minZ = z0 - grow, maxZ = z0 + d + grow;
        if (mirror) { float t = minX; minX = maxX; maxX = t; }
        float f4 = u, f5 = u + d, f6 = u + d + w, f7 = u + d + w + w, f8 = u + d + w + d, f9 = u + d + w + d + w;
        float f10 = v, f11 = v + d, f12 = v + d + h, fj = f11 + (joint - y0);
        // cantos: 0..7 como no jogo (0 = min,min,min); nivel: 0 = em cima (ombro), 1 = junta, 2 = ponta
        float[][] c = {{minX, minY, minZ}, {maxX, minY, minZ}, {maxX, maxY, minZ}, {minX, maxY, minZ},
                {minX, minY, maxZ}, {maxX, minY, maxZ}, {maxX, maxY, maxZ}, {minX, maxY, maxZ}};
        int[][] sides = {{0, 4, 7, 3}, {1, 0, 3, 2}, {5, 1, 2, 6}, {4, 5, 6, 7}};   // oeste, norte, leste, sul (A,B no ombro; C,D na ponta)
        float[][] sideU = {{f4, f5}, {f5, f6}, {f6, f8}, {f8, f9}};
        for (int i = 0; i < 4; i++) {
            float[] A = c[sides[i][0]], B = c[sides[i][1]], C = c[sides[i][2]], D = c[sides[i][3]];
            float u1 = sideU[i][0], u2 = sideU[i][1];
            float[] Aj = {A[0], joint, A[2]}, Bj = {B[0], joint, B[2]};
            quad(ps, vc, mirror, bend, joint, texW, texH, light, overlay, color,
                    new float[][]{A, B, Bj, Aj}, new int[]{0, 0, 1, 1}, new float[][]{{u2, f11}, {u1, f11}, {u1, fj}, {u2, fj}});
            quad(ps, vc, mirror, bend, joint, texW, texH, light, overlay, color,
                    new float[][]{Aj, Bj, C, D}, new int[]{1, 1, 2, 2}, new float[][]{{u2, fj}, {u1, fj}, {u1, f12}, {u2, f12}});
        }
        // ombro/quadril (topo) e ponta (mao/pe)
        quad(ps, vc, mirror, bend, joint, texW, texH, light, overlay, color,
                new float[][]{c[5], c[4], c[0], c[1]}, new int[]{0, 0, 0, 0}, new float[][]{{f6, f10}, {f5, f10}, {f5, f11}, {f6, f11}});
        if (endCap) quad(ps, vc, mirror, bend, joint, texW, texH, light, overlay, color,
                new float[][]{c[2], c[3], c[7], c[6]}, new int[]{2, 2, 2, 2}, new float[][]{{f7, f11}, {f6, f11}, {f6, f10}, {f7, f10}});
        ps.popPose();
    }

    private static void quad(PoseStack ps, VertexConsumer vc, boolean mirror, float bend, float joint, float texW, float texH,
                             int light, int overlay, int color, float[][] pts, int[] level, float[][] uv) {
        float[][] q = new float[4][];
        for (int i = 0; i < 4; i++) {
            float ang = level[i] == 0 ? 0 : level[i] == 1 ? bend * 0.5f : bend;
            float y = pts[i][1] - joint, z = pts[i][2];
            float cs = Mth.cos(ang), sn = Mth.sin(ang);
            if (level[i] == 0) { y = pts[i][1]; }
            else { float ny = y * cs - z * sn, nz = y * sn + z * cs; y = ny + joint; z = nz; }
            q[i] = new float[]{pts[i][0], y, z, uv[i][0], uv[i][1]};
        }
        if (mirror) { float[] t = q[0]; q[0] = q[3]; q[3] = t; t = q[1]; q[1] = q[2]; q[2] = t; }
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Vector3f[] w = new Vector3f[4];
        for (int i = 0; i < 4; i++) w[i] = m.transformPosition(q[i][0] / 16f, q[i][1] / 16f, q[i][2] / 16f, new Vector3f());
        float area = (w[1].x - w[0].x) * (w[2].y - w[0].y) - (w[1].y - w[0].y) * (w[2].x - w[0].x);
        if (area * frontSign <= 0) return;   // face de tras / de dentro: nao desenha
        Vector3f e1 = new Vector3f(q[1][0] - q[0][0], q[1][1] - q[0][1], q[1][2] - q[0][2]);
        Vector3f e2 = new Vector3f(q[2][0] - q[0][0], q[2][1] - q[0][1], q[2][2] - q[0][2]);
        Vector3f n = e1.cross(e2);
        if (n.lengthSquared() < 1e-8f) return;
        n.normalize();
        pose.normal().transform(n).normalize();
        for (int i = 0; i < 4; i++)
            vc.addVertex(w[i].x, w[i].y, w[i].z, color, q[i][3] / texW, q[i][4] / texH, overlay, light, n.x, n.y, n.z);
    }

    // skin: bracos, pernas e as camadas de cima (mangas/calca)
    private static void skinLimbs(PoseStack ps, VertexConsumer vc, Rig r, int light, int overlay, int color) {
        float aw = slimArms ? 3 : 4;
        limb(ps, vc, r.rArm, r.rArmLow, 40, 16, slimArms ? -2 : -3, -2, aw, 12, 4, 0, false, true, 64, 64, light, overlay, color);
        limb(ps, vc, r.rArm, r.rArmLow, 40, 32, slimArms ? -2 : -3, -2, aw, 12, 4, 0.25f, false, false, 64, 64, light, overlay, color);
        limb(ps, vc, r.lArm, r.lArmLow, 32, 48, -1, -2, aw, 12, 4, 0, false, true, 64, 64, light, overlay, color);
        limb(ps, vc, r.lArm, r.lArmLow, 48, 48, -1, -2, aw, 12, 4, 0.25f, false, false, 64, 64, light, overlay, color);
        limb(ps, vc, r.rLeg, r.rLegLow, 0, 16, -2, 0, 4, 12, 6, 0, false, true, 64, 64, light, overlay, color);
        limb(ps, vc, r.rLeg, r.rLegLow, 0, 32, -2, 0, 4, 12, 6, 0.25f, false, false, 64, 64, light, overlay, color);
        limb(ps, vc, r.lLeg, r.lLegLow, 16, 48, -2, 0, 4, 12, 6, 0, false, true, 64, 64, light, overlay, color);
        limb(ps, vc, r.lLeg, r.lLegLow, 0, 48, -2, 0, 4, 12, 6, 0.25f, false, false, 64, 64, light, overlay, color);
    }

    private static void skin(PoseStack ps, VertexConsumer vc, Rig r, int light, int overlay, int color) {
        r.head.render(ps, vc, light, overlay, color);
        r.body.render(ps, vc, light, overlay, color);
        skinLimbs(ps, vc, r, light, overlay, color);
    }

    // uma camada de uma peca de armadura
    private static void armorPiece(PoseStack ps, VertexConsumer vc, EquipmentSlot slot, ItemStack st, int light, int overlay, int color) {
        net.minecraft.client.model.HumanoidModel<?> m3 = com.bino.binosvanillaplus.client.armor.Armor3D.model(st);
        if (m3 != null) { armor3D(ps, vc, m3, slot, light, overlay, color); return; }
        boolean inner = slot == EquipmentSlot.LEGS;
        Rig r = inner ? armorInner : armorOuter;
        float g = inner ? 0.5f : 1.0f;
        switch (slot) {
            case HEAD -> r.head.render(ps, vc, light, overlay, color);
            case CHEST -> {
                r.body.render(ps, vc, light, overlay, color);
                limb(ps, vc, r.rArm, r.rArmLow, 40, 16, -3, -2, 4, 12, 4, g, false, false, 64, 32, light, overlay, color);
                limb(ps, vc, r.lArm, r.lArmLow, 40, 16, -1, -2, 4, 12, 4, g, true, false, 64, 32, light, overlay, color);
            }
            case LEGS -> {
                r.body.render(ps, vc, light, overlay, color);
                limb(ps, vc, r.rLeg, r.rLegLow, 0, 16, -2, 0, 4, 12, 6, g, false, false, 64, 32, light, overlay, color);
                limb(ps, vc, r.lLeg, r.lLegLow, 0, 16, -2, 0, 4, 12, 6, g, true, false, 64, 32, light, overlay, color);
            }
            default -> {   // botas: com a sola
                limb(ps, vc, r.rLeg, r.rLegLow, 0, 16, -2, 0, 4, 12, 6, g, false, true, 64, 32, light, overlay, color);
                limb(ps, vc, r.lLeg, r.lLegLow, 0, 16, -2, 0, 4, 12, 6, g, true, true, 64, 32, light, overlay, color);
            }
        }
    }

    // peca com modelo 3D: cada parte copia a pose do osso do boneco (o antebraco/canela dobrados nao entortam a peca)
    private static void armor3D(PoseStack ps, VertexConsumer vc, net.minecraft.client.model.HumanoidModel<?> m, EquipmentSlot slot,
                                int light, int overlay, int color) {
        Rig r = armorOuter;
        m.head.copyFrom(r.head); m.body.copyFrom(r.body);
        m.rightArm.copyFrom(r.rArm); m.leftArm.copyFrom(r.lArm);
        m.rightLeg.copyFrom(r.rLeg); m.leftLeg.copyFrom(r.lLeg);
        switch (slot) {
            case HEAD -> { m.head.render(ps, vc, light, overlay, color); m.hat.render(ps, vc, light, overlay, color); }
            case CHEST -> { m.body.render(ps, vc, light, overlay, color); m.rightArm.render(ps, vc, light, overlay, color); m.leftArm.render(ps, vc, light, overlay, color); }
            case LEGS -> { m.body.render(ps, vc, light, overlay, color); m.rightLeg.render(ps, vc, light, overlay, color); m.leftLeg.render(ps, vc, light, overlay, color); }
            default -> { m.rightLeg.render(ps, vc, light, overlay, color); m.leftLeg.render(ps, vc, light, overlay, color); }
        }
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private interface LayerSink { void accept(ResourceLocation tex, int color, EquipmentSlot slot, ItemStack st); }

    private static void eachArmorLayer(AbstractClientPlayer p, LayerSink sink) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack st = p.getItemBySlot(slot);
            if (!(st.getItem() instanceof ArmorItem ai) || ai.getEquipmentSlot() != slot) continue;
            boolean inner = slot == EquipmentSlot.LEGS;
            for (ArmorMaterial.Layer layer : ai.getMaterial().value().layers()) {
                int color = layer.dyeable() ? DyedItemColor.getOrDefault(st, -6265536) | 0xFF000000 : -1;
                sink.accept(net.neoforged.neoforge.client.ClientHooks.getArmorTexture(p, st, layer, inner, slot), color, slot, st);
            }
        }
    }

    // ------------------------------------------------------------------ desenho

    // desenha o jogador centrado em (cx, cy), 'pxPerBlock' pixels de GUI por bloco, virado 'yaw' graus
    public static void render(GuiGraphics g, AbstractClientPlayer p, float cx, float cy, float pxPerBlock, float yaw, ActionPose.Pose pose) {
        ensure();
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin sk = p.getSkin();
        slimArms = sk.model() == PlayerSkin.Model.SLIM;
        Rig rig = slimArms ? slim : wide;
        ActionPose.Angles a = ActionPose.of(pose);
        float breathe = Mth.sin((p.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(false)) * 0.07f) * 0.03f;
        rig.pose(a, breathe);
        armorOuter.pose(a, breathe);
        armorInner.pose(a, breathe);
        g.flush();

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        int light = 0xF000F0;
        lights();

        // 1) contorno: o boneco desenhado 8 vezes, 1 px para cada lado, todo claro (cor "flash" do overlay, como o creeper)
        int white = OverlayTexture.pack(OverlayTexture.u(1f), OverlayTexture.v(false));
        for (int i = 0; i < 8; i++) {
            float dx = (float) Math.round(Math.cos(i * Math.PI / 4)), dy = (float) Math.round(Math.sin(i * Math.PI / 4));
            PoseStack ps = begin(g, cx + dx, cy + dy, pxPerBlock, yaw);
            calibrate(ps);
            skin(ps, buf.getBuffer(RenderType.entityTranslucent(sk.texture())), rig, light, white, 0xFF000000);
            eachArmorLayer(p, (tex, color, slot, st) -> armorPiece(ps, buf.getBuffer(RenderType.entityTranslucent(tex)), slot, st, light, white, 0xFF000000));
            g.pose().popPose();
        }
        buf.endBatch();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);   // o boneco de verdade sempre por cima do contorno

        // 2) o boneco (com o shader de luz; sem ele, os tipos normais do jogo)
        PoseStack ps = begin(g, cx, cy, pxPerBlock, yaw);
        calibrate(ps);
        if (shader != null) {
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            draw(sk.texture(), vc -> skin(ps, vc, rig, light, OverlayTexture.NO_OVERLAY, -1));
            eachArmorLayer(p, (tex, color, slot, st) -> draw(tex, vc -> armorPiece(ps, vc, slot, st, light, OverlayTexture.NO_OVERLAY, color)));
            RenderSystem.enableCull();
        } else {
            skin(ps, buf.getBuffer(RenderType.entityTranslucent(sk.texture())), rig, light, OverlayTexture.NO_OVERLAY, -1);
            eachArmorLayer(p, (tex, color, slot, st) -> armorPiece(ps, buf.getBuffer(RenderType.armorCutoutNoCull(tex)), slot, st, light, OverlayTexture.NO_OVERLAY, color));
        }
        eachArmorLayer(p, (tex, color, slot, st) -> {
            if (st.hasFoil()) armorPiece(ps, buf.getBuffer(RenderType.armorEntityGlint()), slot, st, light, OverlayTexture.NO_OVERLAY, -1);
        });
        boolean rightHanded = p.getMainArm() == HumanoidArm.RIGHT;
        held(ps, buf, p, rightHanded ? p.getMainHandItem() : p.getOffhandItem(), rig.rArm, rig.rArmLow, false, light);
        held(ps, buf, p, rightHanded ? p.getOffhandItem() : p.getMainHandItem(), rig.lArm, rig.lArmLow, true, light);
        buf.endBatch();
        ps.popPose();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);   // slots e textos desenhados depois ficam por cima
        Lighting.setupFor3DItems();
    }

    private static PoseStack begin(GuiGraphics g, float cx, float cy, float px, float yaw) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 150);
        ps.scale(px, px, -px);
        ps.mulPose(Axis.XP.rotationDegrees(-6f));                  // camera um pouco de cima
        ps.mulPose(Axis.YP.rotationDegrees(yaw));                   // de frente para quem olha, girado 'yaw'
        ps.translate(0, -8 / 16f, 0);                               // centro do corpo (cabeca -8, pes 24)
        return ps;
    }

    private static void lights() {
        Lighting.setupForEntityInInventory();
        RenderSystem.setShaderLights(new Vector3f(-0.5f, -0.75f, 0.6f).normalize(), new Vector3f(0.75f, -0.1f, 0.3f).normalize());
    }

    private static void draw(ResourceLocation tex, java.util.function.Consumer<VertexConsumer> geo) {
        com.mojang.blaze3d.vertex.BufferBuilder bb = com.mojang.blaze3d.vertex.Tesselator.getInstance()
                .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
        geo.accept(bb);
        com.mojang.blaze3d.vertex.MeshData mesh = bb.build();
        if (mesh == null) return;
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, tex);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
    }

    // item na mao: preso na ponta do antebraco (osso do braco + dobra do cotovelo)
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

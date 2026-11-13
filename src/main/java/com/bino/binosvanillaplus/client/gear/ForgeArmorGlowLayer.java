package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.client.armor.Armor3D;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.List;

// Brilho de ressonancia da forja na armadura vestida (ver ForgeGlow): desenha de novo cada peca +4 ou mais,
// com as partes do modelo um pouco maiores, translucida e acesa na cor principal da peca.
public class ForgeArmorGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private final HumanoidModel<AbstractClientPlayer> inner, outer;

    public ForgeArmorGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                               EntityRendererProvider.Context ctx) {
        super(parent);
        inner = new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outer = new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            int tier = ForgeGlow.tier(stack);
            if (tier == 0 || !(stack.getItem() instanceof ArmorItem armor)) continue;
            List<ArmorMaterial.Layer> layers = armor.getMaterial().value().layers();
            if (layers.isEmpty()) continue;
            boolean innerModel = slot == EquipmentSlot.LEGS;
            HumanoidModel model = Armor3D.model(stack);
            if (model == null) model = innerModel ? inner : outer;
            ((HumanoidModel) getParentModel()).copyPropertiesTo(model);
            visible(model, slot);
            ResourceLocation tex = ClientHooks.getArmorTexture(player, stack, layers.get(0), innerModel, slot);
            var vc = buf.getBuffer(RenderType.entityTranslucentEmissive(tex));
            float a = ForgeGlow.alpha(tier);
            float[][] shells = {{1.04f, a}, {1.09f + 0.015f * tier, a * 0.45f}};
            for (float[] sh : shells) {
                scale(model, sh[0]);
                model.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ForgeGlow.argb(stack, sh[1]));
            }
            scale(model, 1f);
            model.setAllVisible(true);
        }
    }

    // so as partes daquela peca (igual a camada de armadura do jogo)
    private static void visible(HumanoidModel<?> m, EquipmentSlot slot) {
        m.setAllVisible(false);
        switch (slot) {
            case HEAD -> { m.head.visible = true; m.hat.visible = true; }
            case CHEST -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
            case LEGS -> { m.body.visible = true; m.rightLeg.visible = true; m.leftLeg.visible = true; }
            case FEET -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
            default -> {}
        }
    }

    // cada parte cresce a partir da propria junta (ombro, quadril, pescoco)
    private static void scale(HumanoidModel<?> m, float s) {
        for (ModelPart p : new ModelPart[]{m.head, m.hat, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) {
            p.xScale = s; p.yScale = s; p.zScale = s;
        }
    }
}

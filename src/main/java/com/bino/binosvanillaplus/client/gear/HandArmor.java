package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.client.armor.Armor3D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.ClientHooks;

// Manga do peitoral no braco em primeira pessoa (o jogo base so desenha a pele e a manga da camisa). Chamado no fim de
// PlayerRenderer.renderRightHand / renderLeftHand, entao vale tanto para a mao normal quanto para os bracos da cena
// da Mesa de Lapidacao: usa o mesmo PoseStack que acabou de desenhar o braco.
public final class HandArmor {
    private HandArmor() {}

    private static HumanoidModel<AbstractClientPlayer> outer;

    private static HumanoidModel<AbstractClientPlayer> outer() {
        if (outer == null) outer = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        return outer;
    }

    // chamado quando os recursos recarregam (os modelos sao refeitos)
    public static void reset() { outer = null; }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, boolean right,
                              PlayerModel<AbstractClientPlayer> source) {
        if (player.isInvisible()) return;
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != EquipmentSlot.CHEST) return;
        var layers = armor.getMaterial().value().layers();
        if (layers.isEmpty()) return;

        HumanoidModel model = Armor3D.model(stack);
        if (model == null) model = outer();
        ModelPart dst = right ? model.rightArm : model.leftArm;
        dst.copyFrom(right ? source.rightArm : source.leftArm);
        dst.visible = true;
        dst.xScale = dst.yScale = dst.zScale = 1f;

        for (ArmorMaterial.Layer layer : layers) {
            int color = layer.dyeable() ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, -6265536)) : -1;
            ResourceLocation tex = ClientHooks.getArmorTexture(player, stack, layer, false, EquipmentSlot.CHEST);
            VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buf, RenderType.armorCutoutNoCull(tex), stack.hasFoil());
            dst.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
        }
    }
}

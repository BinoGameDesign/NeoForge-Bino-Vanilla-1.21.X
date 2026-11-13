package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.network.HelmetLampTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

// Tecla da lanterna do capacete (K) e o desenho da lanterna na lateral do capacete
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class GearClientEvents {
    private GearClientEvents() {}

    public static final KeyMapping HELMET_LAMP = new KeyMapping("key.binosvanillaplus.helmet_lamp",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.binosvanillaplus");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(HELMET_LAMP); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        while (HELMET_LAMP.consumeClick()) {
            if (Minecraft.getInstance().player != null) PacketDistributor.sendToServer(HelmetLampTogglePayload.INSTANCE);
        }
    }

    // capacete com lanterna: avisa na dica do item
    @SubscribeEvent
    public static void onTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var s = event.getItemStack();
        if (!s.getOrDefault(com.bino.binosvanillaplus.item.ModDataComponents.HELMET_LAMP.get(), false)) return;
        boolean on = s.getOrDefault(com.bino.binosvanillaplus.item.ModDataComponents.FLASHLIGHT_ON.get(), false);
        event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.binosvanillaplus.helmet_lamp")
                .withStyle(on ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.GRAY));
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        ForgeGlow.clearCache();   // recarregou os recursos: le de novo a cor dos itens
        for (PlayerSkin.Model model : event.getSkins()) {
            if (event.getSkin(model) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new HelmetLampLayer(renderer));
                renderer.addLayer(new ForgeArmorGlowLayer(renderer, event.getContext()));   // brilho da forja +4 na armadura
            }
        }
    }
}

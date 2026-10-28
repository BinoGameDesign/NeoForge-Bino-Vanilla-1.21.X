package com.bino.binosvanillaplus.client.lookat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class LookAtEvents {

    // Tecla para ligar/desligar o painel (padrao: J; muda em Controles)
    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.binosvanillaplus.toggle_lookat", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,
            "key.categories.binosvanillaplus");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "lookat"),
                LookAtHud::render);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        while (TOGGLE.consumeClick()) {
            LookAtHud.enabled = !LookAtHud.enabled;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable(
                    LookAtHud.enabled ? "lookat.binosvanillaplus.on" : "lookat.binosvanillaplus.off"), true);
        }
    }
}

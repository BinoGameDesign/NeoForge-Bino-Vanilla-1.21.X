package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class MapClientEvents {
    public static final KeyMapping OPEN_MAP = new KeyMapping("key.binosvanillaplus.world_map",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.binosvanillaplus");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(MinimapHud.ID, MinimapHud::render);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ExploredMap.tick(mc);
        if (mc.player != null && mc.level != null && mc.player.tickCount % 3 == 0 && com.bino.binosvanillaplus.map.MapGate.hasMap(mc.player))
            MinimapHud.sample(mc);
        while (OPEN_MAP.consumeClick()) {
            if (mc.screen == null) MapClientBridge.openWorldMap();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ExploredMap.saveAndClear();
    }
}

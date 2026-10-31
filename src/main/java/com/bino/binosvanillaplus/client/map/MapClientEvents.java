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
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class MapClientEvents {
    public static final KeyMapping OPEN_MAP = new KeyMapping("key.binosvanillaplus.world_map",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.binosvanillaplus");

    public static final KeyMapping ADD_WAYPOINT = new KeyMapping("key.binosvanillaplus.add_waypoint",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.binosvanillaplus");

    public static final KeyMapping ZOOM_IN = new KeyMapping("key.binosvanillaplus.minimap_zoom_in",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, "key.categories.binosvanillaplus");
    public static final KeyMapping ZOOM_OUT = new KeyMapping("key.binosvanillaplus.minimap_zoom_out",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, "key.categories.binosvanillaplus");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ZOOM_IN);
        event.register(ZOOM_OUT);
        event.register(OPEN_MAP);
        event.register(ADD_WAYPOINT);
    }

    // shader proprio do minimapa diorama (assets/binosvanillaplus/shaders/core/diorama.*)
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "diorama"), DefaultVertexFormat.POSITION_COLOR_NORMAL),
                s -> DioramaRenderer.shader = s);
    }

    // ponto de morte automatico
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof DeathScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        MapOptions.ensureLoaded();
        if (MapOptions.deathPoint && mc.player != null)
            MapWaypoints.setDeathPoint(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(MinimapHud.ID, MinimapHud::render);
        event.registerAboveAll(WaypointBeacons.LABELS_ID, WaypointBeacons::renderLabels);
        // avisos (Toasts) tambem aparecem no jogo, depois que uma tela fecha
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "toasts"), (g, dt) -> {
            if (Minecraft.getInstance().screen == null)
                com.bino.binosvanillaplus.client.ui.Toasts.render(g, g.guiWidth(), g.guiHeight() - 50, -1, -1);
        });
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ExploredMap.tick(mc);
        if (mc.player != null && mc.level != null && mc.player.tickCount % 3 == 0 && com.bino.binosvanillaplus.map.MapGate.hasMap(mc.player))
            MinimapHud.sample(mc);
        while (ZOOM_IN.consumeClick()) { MapOptions.ensureLoaded(); MapOptions.minimapZoom = Math.max(0, MapOptions.minimapZoom - 1); MapOptions.save(); }
        while (ZOOM_OUT.consumeClick()) { MapOptions.ensureLoaded(); MapOptions.minimapZoom = Math.min(2, MapOptions.minimapZoom + 1); MapOptions.save(); }
        while (ADD_WAYPOINT.consumeClick()) {
            if (mc.screen == null && mc.player != null)
                mc.setScreen(new WaypointEditScreen(null, null, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
        }
        while (OPEN_MAP.consumeClick()) {
            if (mc.screen == null) MapClientBridge.openWorldMap();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ExploredMap.saveAndClear();
    }
}

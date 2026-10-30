package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;

// Ponte para o codigo comum (servidor dedicado) nao carregar classes de tela
public class MapClientBridge {
    public static void openWorldMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && com.bino.binosvanillaplus.map.MapGate.hasMap(mc.player)) mc.setScreen(new WorldMapScreen());
    }
}

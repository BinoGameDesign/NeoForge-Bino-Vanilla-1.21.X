package com.bino.binosvanillaplus.client.ui;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

// Com uma tela do mod aberta (mapa, marcacoes, opcoes...) o HUD do jogo e a mao somem, como ao apertar F1.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class HideHudWhileOpen {
    private static boolean active() { return Minecraft.getInstance().screen instanceof BinoScreen; }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }
}

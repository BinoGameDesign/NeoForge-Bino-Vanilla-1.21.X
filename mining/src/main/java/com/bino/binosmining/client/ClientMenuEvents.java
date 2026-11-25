package com.bino.binosmining.client;

import com.bino.binosmining.BinosMining;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.bino.binosmining.menu.MiningMenus;

// Liga cada menu à sua janela
@EventBusSubscriber(modid = BinosMining.MOD_ID, value = Dist.CLIENT)
public class ClientMenuEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MiningMenus.LAPIDARY.get(), LapidaryScreen::new);
    }
}

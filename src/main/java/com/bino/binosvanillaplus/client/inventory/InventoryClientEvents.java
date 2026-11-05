package com.bino.binosvanillaplus.client.inventory;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.menu.ModMenuTypes;
import com.bino.binosvanillaplus.network.InventoryActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Troca o inventario do jogo (tecla E) pelo inventario novo. No criativo continua o do jogo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class InventoryClientEvents {
    private InventoryClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BINO_INVENTORY.get(), BinoInventoryScreen::new);
    }

    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(event.getNewScreen() instanceof InventoryScreen) || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new InventoryActionPayload(InventoryActionPayload.OPEN));
    }
}

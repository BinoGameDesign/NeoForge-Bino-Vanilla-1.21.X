package com.bino.binosvanillaplus.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.map.MapClientBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Botao direito com um mapa na mao:
//   normal       -> abre o Mapa-Mundi (so no cliente)
//   shift        -> mapa vazio vira um mapa preenchido (fisico, para molduras); mapa ja preenchido avisa que ja esta salvo
// O comportamento vanilla (mapa vazio criando mapa na hora) e' cancelado.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class MapUseEvents {
    private static final byte SAVED_MAP_SCALE = 2;   // 0 = 128 blocos, 1 = 256, 2 = 512 ...

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!MapGate.isMap(stack)) return;
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (player.isShiftKeyDown()) {
            if (event.getLevel().isClientSide) return;
            saveMap((ServerLevel) event.getLevel(), player, hand, stack);
        } else if (event.getLevel().isClientSide) {
            MapClientBridge.openWorldMap();
        }
    }

    private static void saveMap(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.FILLED_MAP)) {
            player.displayClientMessage(Component.translatableWithFallback("message.binosvanillaplus.map_already_saved",
                    "Este mapa j\u00e1 est\u00e1 salvo. Coloque-o numa moldura."), true);
            return;
        }
        ItemStack map = MapItem.create(level, player.getBlockX(), player.getBlockZ(), SAVED_MAP_SCALE, true, false);
        MapItem.renderBiomePreviewMap(level, map);
        stack.shrink(1);
        if (stack.isEmpty()) player.setItemInHand(hand, map);
        else if (!player.getInventory().add(map)) player.drop(map, false);
        level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatableWithFallback("message.binosvanillaplus.map_saved",
                "Mapa salvo! Voc\u00ea pode coloc\u00e1-lo numa moldura."), true);
    }
}

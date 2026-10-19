package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.CompanionAllayManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class CompanionEvents {

    // Entrou no mundo: cria o pet
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CompanionAllayManager.ensurePet(player);
    }

    // Renasceu depois de morrer: cria o pet de novo
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CompanionAllayManager.ensurePet(player);
    }

    // Trocou de dimensão: o pet antigo some sozinho, aqui nasce o da dimensão nova
    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CompanionAllayManager.ensurePet(player);
    }

    // Saiu do jogo: remove o pet
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CompanionAllayManager.removePet(player);
    }

    // A cada 1 segundo confere se o pet ainda existe (cobre /tp, chunk descarregado etc.)
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            CompanionAllayManager.ensurePet(player);
        }
    }

    // Um mob morreu: se o jogador o matou (espada, arco, besta, tridente...), o pet busca os drops
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) return;   // morte de jogador não conta
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!CompanionAllayManager.hasPet(player)) return;

        for (ItemEntity drop : event.getDrops()) {
            // Dá 5 segundos para o pet chegar antes de o jogador pegar o item sozinho
            drop.setPickUpDelay(100);
            CompanionAllayManager.track(player, drop);
        }
    }

    // Fechou o mundo (ou o servidor): limpa a memória
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CompanionAllayManager.clearAll();
    }
}
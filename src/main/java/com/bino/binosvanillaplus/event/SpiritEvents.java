package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.GraveBlock;
import com.bino.binosvanillaplus.block.entity.GraveBlockEntity;
import com.bino.binosvanillaplus.network.GraveMarkPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Quando um jogador morre: coloca o Tumulo (num lugar seguro) e guarda nele os itens e o xp que cairiam no chao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class SpiritEvents {

    // false = guarda so o xp que o jogo normalmente soltaria (7 por nivel, no maximo 100)
    // true  = guarda TODO o xp que o jogador tinha
    private static final boolean STORE_ALL_XP = false;

    private record Pending(ServerLevel level, BlockPos grave, BlockPos death) {}

    // Tumulos criados NESTE tick por jogadores que acabaram de morrer
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {   // nada cai: so marca o lugar da morte
            PacketDistributor.sendToPlayer(player, new GraveMarkPayload(player.blockPosition(), GraveMarkPayload.DEATH));
            return;
        }
        BlockPos spot = GravePlacement.find(level, player.blockPosition());
        level.setBlock(spot, ModBlocks.GRAVE.get().defaultBlockState()
                .setValue(GraveBlock.FACING, player.getDirection().getOpposite()), 3);
        if (level.getBlockEntity(spot) instanceof GraveBlockEntity be) {
            be.setOwner(player);
            PENDING.put(player.getUUID(), new Pending(level, spot, player.blockPosition()));
        }
    }

    private static GraveBlockEntity pendingGrave(ServerPlayer player) {
        Pending p = PENDING.get(player.getUUID());
        return p != null && p.level().getBlockEntity(p.grave()) instanceof GraveBlockEntity be ? be : null;
    }

    // Itens: tiramos da lista de drops (o que nao couber continua caindo no chao). LOWEST = depois de outros mods.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GraveBlockEntity grave = pendingGrave(player);
        if (grave == null) return;

        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity drop = it.next();
            ItemStack rest = grave.getItems().addItem(drop.getItem().copy());
            if (rest.isEmpty()) it.remove(); else drop.setItem(rest);
            grave.markHadLoot();
        }
    }

    // Experiencia: em vez de esferas, o tumulo guarda o valor.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onXp(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GraveBlockEntity grave = pendingGrave(player);
        if (grave == null) return;

        grave.addExperience(STORE_ALL_XP ? player.totalExperience : event.getDroppedExperience());
        event.setDroppedExperience(0);
    }

    // Fim do tick: a morte ja foi toda processada. Tumulo que nao recebeu nada some; os outros viram ponto no mapa.
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (Map.Entry<UUID, Pending> e : PENDING.entrySet()) {
            Pending p = e.getValue();
            ServerPlayer player = p.level().getServer().getPlayerList().getPlayer(e.getKey());
            boolean loot = p.level().getBlockEntity(p.grave()) instanceof GraveBlockEntity be && be.hadLoot();
            if (!loot) p.level().removeBlock(p.grave(), false);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, loot
                        ? new GraveMarkPayload(p.grave(), GraveMarkPayload.GRAVE)
                        : new GraveMarkPayload(p.death(), GraveMarkPayload.DEATH));
            }
        }
        PENDING.clear();
    }
}

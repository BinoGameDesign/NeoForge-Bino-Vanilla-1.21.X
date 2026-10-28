package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.entity.SpiritEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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

// Quando um jogador morre: cria o Espirito e coloca nele os itens e o xp que cairiam no chao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class SpiritEvents {

    // false = guarda so o xp que o jogo normalmente soltaria (7 por nivel, no maximo 100)
    // true  = guarda TODO o xp que o jogador tinha
    private static final boolean STORE_ALL_XP = false;

    // Espiritos criados NESTE tick por jogadores que acabaram de morrer
    private static final Map<UUID, SpiritEntity> PENDING = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;   // nada cai: nao ha o que guardar

        SpiritEntity spirit = new SpiritEntity(ModEntities.SPIRIT.get(), level);
        spirit.setOwner(player);
        double y = Math.max(player.getY(), level.getMinBuildHeight() + 1);            // nao deixa nascer no vazio
        spirit.moveTo(player.getX(), y, player.getZ(), player.getYRot(), 0.0f);
        level.addFreshEntity(spirit);
        PENDING.put(player.getUUID(), spirit);
    }

    // Itens: tiramos da lista de drops (o que nao couber continua caindo no chao). LOWEST = depois de outros mods.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SpiritEntity spirit = PENDING.get(player.getUUID());
        if (spirit == null) return;

        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity drop = it.next();
            ItemStack rest = spirit.getItems().addItem(drop.getItem().copy());
            if (rest.isEmpty()) it.remove(); else drop.setItem(rest);
            spirit.markHadLoot();
        }
    }

    // Experiencia: em vez de esferas, o espirito guarda o valor.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onXp(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SpiritEntity spirit = PENDING.get(player.getUUID());
        if (spirit == null) return;

        spirit.addExperience(STORE_ALL_XP ? player.totalExperience : event.getDroppedExperience());
        event.setDroppedExperience(0);
    }

    // Fim do tick: a morte ja foi toda processada. Espirito que nao recebeu nada some.
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (SpiritEntity spirit : PENDING.values()) {
            if (!spirit.hadLoot()) spirit.discard();
        }
        PENDING.clear();
    }
}

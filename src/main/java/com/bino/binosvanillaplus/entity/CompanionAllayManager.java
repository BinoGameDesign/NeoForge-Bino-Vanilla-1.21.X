package com.bino.binosvanillaplus.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// Guarda quem tem pet, quais drops cada pet deve buscar e quem já foi avisado de "inventário cheio"
public class CompanionAllayManager {

    // Um drop que o pet deve buscar (guardamos o horário para esquecer drops que nunca apareceram)
    public record TrackedDrop(UUID itemId, long createdTick) {}

    private static final Map<UUID, UUID> PETS = new HashMap<>();                 // jogador -> pet
    private static final Map<UUID, List<TrackedDrop>> DROPS = new HashMap<>();   // jogador -> drops
    private static final Set<UUID> FULL_NOTIFIED = new HashSet<>();              // quem já recebeu o aviso

    public static boolean hasPet(ServerPlayer player) {
        UUID id = PETS.get(player.getUUID());
        if (id == null) return false;
        Entity pet = player.serverLevel().getEntity(id);
        return pet != null && pet.isAlive();
    }

    // Cria o pet se o jogador ainda não tem um vivo nesta dimensão
    public static void ensurePet(ServerPlayer player) {
        if (!player.isAlive() || hasPet(player)) return;

        ServerLevel level = player.serverLevel();
        CompanionAllayEntity pet = ModEntities.COMPANION_ALLAY.get().create(level);
        if (pet == null) return;

        Vec3 slot = CompanionAllayEntity.followSlot(player);
        pet.setOwner(player);
        pet.moveTo(slot.x, slot.y, slot.z, player.getYRot(), 0.0f);
        pet.setSilent(true);                 // sem os sons do Allay normal
        level.addFreshEntity(pet);
        PETS.put(player.getUUID(), pet.getUUID());
    }

    // Remove o pet (quando o jogador sai do jogo)
    public static void removePet(ServerPlayer player) {
        UUID id = PETS.remove(player.getUUID());
        if (id != null) {
            Entity pet = player.serverLevel().getEntity(id);
            if (pet != null) pet.discard();
        }
        DROPS.remove(player.getUUID());
        FULL_NOTIFIED.remove(player.getUUID());
    }

    public static void track(ServerPlayer player, ItemEntity drop) {
        drops(player.getUUID()).add(new TrackedDrop(drop.getUUID(), player.serverLevel().getGameTime()));
    }

    public static List<TrackedDrop> drops(UUID playerId) {
        return DROPS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public static void untrack(UUID playerId, UUID itemId) {
        drops(playerId).removeIf(d -> d.itemId().equals(itemId));
    }

    // true na primeira vez (deve avisar); false se já avisou e o inventário continua cheio
    public static boolean markFull(UUID playerId) { return FULL_NOTIFIED.add(playerId); }

    public static void clearFull(UUID playerId) { FULL_NOTIFIED.remove(playerId); }

    public static void clearAll() {
        PETS.clear();
        DROPS.clear();
        FULL_NOTIFIED.clear();
    }
}
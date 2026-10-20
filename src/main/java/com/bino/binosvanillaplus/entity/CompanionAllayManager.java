package com.bino.binosvanillaplus.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// Guarda quem tem pet, o que cada pet deve buscar, o nome dele e quem já foi avisado de "inventário cheio"
public class CompanionAllayManager {

    public record TrackedDrop(UUID itemId, long createdTick) {}
    public record TrackedXp(Vec3 pos, int amount, long createdTick) {}   // XP que ficou no ponto da morte do mob

    private static final String NAME_KEY = "binosvanillaplus_pet_name";

    private static final Map<UUID, UUID> PETS = new HashMap<>();                 // jogador -> pet
    private static final Map<UUID, List<TrackedDrop>> DROPS = new HashMap<>();   // jogador -> drops
    private static final Map<UUID, List<TrackedXp>> XP = new HashMap<>();        // jogador -> XP pendente
    private static final Map<UUID, String> NAMES = new HashMap<>();              // jogador -> nome do pet
    private static final Set<UUID> FULL_NOTIFIED = new HashSet<>();
    private static final Set<UUID> FRONT_CAMERA = new HashSet<>();   // jogadores em câmera frontal

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

        pet.setOwner(player);                       // antes de calcular a posição (define a direção de referência)
        pet.moveTo(player.getX(), player.getY() + 1.7, player.getZ(), player.getYRot(), 0.0f);
        pet.setSilent(true);

        // Nome: o que o jogador deu com a etiqueta, ou o padrão
        String saved = getName(player);
        pet.setCustomName(saved != null
                ? Component.literal(saved)
                : Component.translatable("entity.binosvanillaplus.companion_allay"));
        pet.setCustomNameVisible(true);

        level.addFreshEntity(pet);
        PETS.put(player.getUUID(), pet.getUUID());
    }

    // Remove o pet (quando o jogador sai do jogo). XP pendente é entregue ao jogador.
    public static void removePet(ServerPlayer player) {
        UUID id = PETS.remove(player.getUUID());
        if (id != null) {
            Entity pet = player.serverLevel().getEntity(id);
            if (pet != null) pet.discard();
        }
        for (TrackedXp xp : xp(player.getUUID())) {
            ExperienceOrb.award(player.serverLevel(), player.position(), xp.amount());
        }
        DROPS.remove(player.getUUID());
        XP.remove(player.getUUID());
        FULL_NOTIFIED.remove(player.getUUID());
        FRONT_CAMERA.remove(player.getUUID());
    }

    // ---------- Drops e XP ----------

    public static void track(ServerPlayer player, ItemEntity drop) {
        drops(player.getUUID()).add(new TrackedDrop(drop.getUUID(), player.serverLevel().getGameTime()));
    }

    public static void trackXp(ServerPlayer player, Vec3 pos, int amount) {
        xp(player.getUUID()).add(new TrackedXp(pos, amount, player.serverLevel().getGameTime()));
    }

    public static List<TrackedDrop> drops(UUID playerId) {
        return DROPS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public static List<TrackedXp> xp(UUID playerId) {
        return XP.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    // ---------- Nome (guardado na memória e nos dados do jogador) ----------

    public static void setName(Player player, String name) {
        NAMES.put(player.getUUID(), name);
        player.getPersistentData().putString(NAME_KEY, name);
    }

    public static String getName(Player player) {
        String name = NAMES.get(player.getUUID());
        if (name == null && player.getPersistentData().contains(NAME_KEY)) {
            name = player.getPersistentData().getString(NAME_KEY);
        }
        return name;
    }

    // ---------- Aviso de inventário cheio ----------

    // true na primeira vez (deve avisar); false se já avisou e o inventário continua cheio
    public static boolean markFull(UUID playerId) { return FULL_NOTIFIED.add(playerId); }

    public static void clearFull(UUID playerId) { FULL_NOTIFIED.remove(playerId); }

    public static void clearAll() {
        PETS.clear();
        DROPS.clear();
        XP.clear();
        NAMES.clear();
        FULL_NOTIFIED.clear();
        FRONT_CAMERA.clear();


    }

    public static void setFrontCamera(UUID playerId, boolean front) {
        if (front) FRONT_CAMERA.add(playerId); else FRONT_CAMERA.remove(playerId);
    }

    public static boolean isFrontCamera(UUID playerId) { return FRONT_CAMERA.contains(playerId); }
}
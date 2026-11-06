package com.bino.binosvanillaplus.faction;

import com.bino.binosvanillaplus.entity.faction.PillagerGolem;
import com.bino.binosvanillaplus.entity.faction.VillagerGuard;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import com.bino.binosvanillaplus.network.FactionSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.neoforge.network.PacketDistributor;

// Faccao guardada no jogador (sobrevive a morte) + de que lado cada mob esta
public final class FactionData {
    private FactionData() {}

    public static Faction get(Player p) { return Faction.of(p.getData(ModAttachments.FACTION)); }

    public static void set(ServerPlayer p, Faction f) {
        p.setData(ModAttachments.FACTION, f.ordinal());
        PacketDistributor.sendToAllPlayers(new FactionSyncPayload(p.getUUID(), f.ordinal()));
    }

    // Lado de um mob: VILLAGER (villagers, golens, guardas), PILLAGER (illagers, ravager, bruxa, vex, golem pillager) ou NONE
    public static Faction side(Entity e) {
        if (e instanceof Player p) return get(p);
        if (e instanceof VillagerGuard) return Faction.VILLAGER;
        if (e instanceof PillagerGolem) return Faction.PILLAGER;
        if (e instanceof IronGolem || e instanceof AbstractVillager) return Faction.VILLAGER;
        if (e instanceof Raider || e instanceof Vex) return Faction.PILLAGER;
        return Faction.NONE;
    }

    // mesmo lado (e os dois tem lado)? Para jogador Errante/sem faccao nunca e' aliado.
    public static boolean allied(Entity a, Entity b) {
        Faction fa = side(a), fb = side(b);
        return fa == fb && (fa == Faction.VILLAGER || fa == Faction.PILLAGER);
    }
}

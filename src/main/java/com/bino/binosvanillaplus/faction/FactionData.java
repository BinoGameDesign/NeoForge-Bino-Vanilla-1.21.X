package com.bino.binosvanillaplus.faction;

import com.bino.binosvanillaplus.leveling.ModAttachments;
import com.bino.binosvanillaplus.network.FactionSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import com.bino.binosvanillaplus.network.CivilianSyncPayload;
import net.minecraft.world.entity.LivingEntity;
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
        if (e instanceof FactionMob m) return m.faction();
        if (e instanceof AbstractVillager v) return isCivilian(v) ? Faction.PILLAGER : Faction.VILLAGER;
        if (e instanceof IronGolem) return Faction.VILLAGER;
        if (e instanceof Raider || e instanceof Vex) return Faction.PILLAGER;
        return Faction.NONE;
    }

    // villager "Pillager civil" (flag no proprio villager; o cliente recebe por pacote)
    public static boolean isCivilian(Entity e) { return e instanceof AbstractVillager && e.getData(ModAttachments.PILLAGER_CIVILIAN); }

    public static void setCivilian(Entity e, boolean on) {
        e.setData(ModAttachments.PILLAGER_CIVILIAN, on);
        if (!e.level().isClientSide()) PacketDistributor.sendToPlayersTrackingEntity(e, new CivilianSyncPayload(e.getId(), on));
    }

    // alvo de quem luta pelos villagers: jogador Pillager e mobs de combate do lado pillager (civis e mercadores nao)
    public static boolean villagerEnemy(LivingEntity t) { return side(t) == Faction.PILLAGER && !(t instanceof AbstractVillager); }

    // alvo extra dos pillagers: os mobs de combate do lado villager que sao nossos (guarda, mago, bruxa...)
    public static boolean villagerFighter(LivingEntity t) { return t instanceof FactionMob m && m.faction() == Faction.VILLAGER; }

    // mesmo lado (e os dois tem lado)? Para jogador Errante/sem faccao nunca e' aliado.
    public static boolean allied(Entity a, Entity b) {
        Faction fa = side(a), fb = side(b);
        return fa == fb && (fa == Faction.VILLAGER || fa == Faction.PILLAGER);
    }
}

package com.bino.binosvanillaplus.client.faction;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.network.FactionChoosePayload;
import com.bino.binosvanillaplus.network.FactionSyncPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

// Faccoes dos jogadores no cliente: nome vermelho para rivais e a tela de escolha
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class FactionClient {
    private FactionClient() {}

    private static final Map<UUID, Faction> FACTIONS = new ConcurrentHashMap<>();

    public static Faction of(UUID id) { return FACTIONS.getOrDefault(id, Faction.NONE); }

    public static void receive(FactionSyncPayload p) { FACTIONS.put(p.player(), Faction.of(p.faction())); }

    public static void civilian(com.bino.binosvanillaplus.network.CivilianSyncPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        net.minecraft.world.entity.Entity e = mc.level.getEntity(p.entity());
        if (e != null) e.setData(com.bino.binosvanillaplus.leveling.ModAttachments.PILLAGER_CIVILIAN, p.civilian());
    }

    public static void open(FactionChoosePayload p) {
        Minecraft.getInstance().setScreen(new FactionChoiceScreen());
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent e) {
        if (!(e.getEntity() instanceof Player other)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || other == mc.player) return;
        if (of(mc.player.getUUID()).rival(of(other.getUUID())))
            e.setContent(e.getContent().copy().withStyle(ChatFormatting.RED));
    }
}

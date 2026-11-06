package com.bino.binosvanillaplus.faction;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.network.FactionChoosePayload;
import com.bino.binosvanillaplus.network.FactionSyncPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Entrada do jogador (sincroniza as faccoes e abre a escolha se ainda nao escolheu), escolha e comando de operador
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class FactionEvents {
    private FactionEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (ServerPlayer o : p.server.getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(p, new FactionSyncPayload(o.getUUID(), FactionData.get(o).ordinal()));
        PacketDistributor.sendToAllPlayers(new FactionSyncPayload(p.getUUID(), FactionData.get(p).ordinal()));
        if (FactionData.get(p) == Faction.NONE) PacketDistributor.sendToPlayer(p, new FactionChoosePayload(-1));
    }

    // a tela so escolhe quem ainda nao tem faccao (trocar depois sera pelo ritual)
    public static void choose(ServerPlayer p, int faction) {
        Faction f = Faction.of(faction);
        if (f == Faction.NONE || FactionData.get(p) != Faction.NONE) return;
        FactionData.set(p, f);
        p.displayClientMessage(Component.translatable("faction.binosvanillaplus.joined", Component.translatable(f.langKey()).withStyle(f.color)), false);
    }

    private static final List<String> IDS = List.of("villager", "pillager", "wanderer", "escolher");

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("binosfaccao").requires(s -> s.hasPermission(2))
                .then(Commands.argument("faccao", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(IDS, b))
                        .executes(c -> run(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("alvo", EntityArgument.players())
                                .executes(c -> run(c, EntityArgument.getPlayers(c, "alvo"))))));
    }

    private static int run(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets) {
        String id = StringArgumentType.getString(c, "faccao");
        for (ServerPlayer p : targets) {
            if (id.equals("escolher")) {   // zera e abre a tela de escolha de novo
                FactionData.set(p, Faction.NONE);
                PacketDistributor.sendToPlayer(p, new FactionChoosePayload(-1));
                continue;
            }
            Faction f = Faction.byId(id);
            if (f == null || f == Faction.NONE) {
                c.getSource().sendFailure(Component.literal("Faccao desconhecida: " + id));
                return 0;
            }
            FactionData.set(p, f);
            c.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " -> ").append(Component.translatable(f.langKey()).withStyle(f.color)), true);
        }
        return targets.size();
    }
}

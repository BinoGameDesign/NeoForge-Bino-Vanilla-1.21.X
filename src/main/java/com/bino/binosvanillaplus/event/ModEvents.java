package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.custom.MultiBreakTool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;

// Registra esta classe automaticamente no barramento de eventos do jogo
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class ModEvents {

    // Lista de blocos que estão sendo quebrados "em cadeia" neste momento.
    // Evita um ciclo infinito: quebrar um bloco extra também dispara este evento.
    private static final Set<BlockPos> CHAIN_BREAKING = new HashSet<>();

    // Roda sempre que um jogador vai quebrar um bloco
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Só no servidor (ServerPlayer só existe lá)
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack mainHand = player.getMainHandItem();

        // Só continua se a ferramenta na mão é uma das nossas
        if (!(mainHand.getItem() instanceof MultiBreakTool tool)) return;

        BlockPos origin = event.getPos();

        // Se este bloco veio de uma quebra em cadeia, não repete a habilidade
        if (CHAIN_BREAKING.contains(origin)) return;

        // Pergunta à ferramenta quais blocos extras quebrar
        for (BlockPos extra : tool.getExtraBlocks(player, origin, event.getState())) {
            if (extra.equals(origin)) continue;    // o bloco original o jogo quebra sozinho
            if (mainHand.isEmpty()) break;         // a ferramenta quebrou? para tudo

            CHAIN_BREAKING.add(extra);                  // marca como "em cadeia"
            player.gameMode.destroyBlock(extra);        // quebra com drops, XP e desgaste normais
            CHAIN_BREAKING.remove(extra);               // desmarca
        }
    }
}
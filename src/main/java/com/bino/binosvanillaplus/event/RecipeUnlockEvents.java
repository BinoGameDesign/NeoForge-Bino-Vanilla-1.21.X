package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

// Livro de receitas com TODAS as receitas do jogo (e dos mods) destravadas, como no JEI.
// Roda quando o jogador entra e quando os datapacks recarregam (/reload). Nao gera avisos (toasts).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class RecipeUnlockEvents {
    // Mude para false para voltar ao comportamento normal (receitas so aparecem depois de descobertas)
    public static final boolean UNLOCK_ALL = true;

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (!UNLOCK_ALL) return;
        event.getRelevantPlayers().forEach(RecipeUnlockEvents::unlockAll);
    }

    private static void unlockAll(ServerPlayer player) {
        if (player.getServer() == null) return;
        ServerRecipeBook book = player.getRecipeBook();
        boolean changed = false;
        for (RecipeHolder<?> holder : player.getServer().getRecipeManager().getRecipes()) {
            if (!book.contains(holder)) {
                book.add(holder);
                changed = true;
            }
        }
        if (changed) book.sendInitialRecipeBook(player);   // reenvia a lista inteira ao cliente, sem toasts
    }
}

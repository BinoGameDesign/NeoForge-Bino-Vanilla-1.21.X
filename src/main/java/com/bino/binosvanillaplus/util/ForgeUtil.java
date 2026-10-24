package com.bino.binosvanillaplus.util;

import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.custom.ForgeStoneItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Funções de ajuda do sistema de forja
public class ForgeUtil {

    public static final int MAX_LEVEL = 10;
    public static final double BONUS_PER_LEVEL = 0.10;   // +10% por nível

    // Nível de forja do item (0 = sem forja)
    public static int getLevel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FORGE_LEVEL.get(), 0);
    }

    // Multiplicador de status: +3 = 1.3, +10 = 2.0
    public static double multiplier(int level) {
        return 1.0 + BONUS_PER_LEVEL * level;
    }

    // Equipamento = item que não empilha e tem durabilidade ou atributos (armas, ferramentas, armaduras, escudo...)
    public static boolean canForge(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() > 1 || stack.getItem() instanceof ForgeStoneItem) return false;
        return stack.isDamageableItem() || !stack.getAttributeModifiers().modifiers().isEmpty();
    }

    // Define o nível, escreve "nome +n" e ajusta a durabilidade máxima
    public static void setLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.FORGE_LEVEL.get(), level);

        // ITEM_NAME é o nome "de fábrica" (sem itálico). Se o jogador renomear na bigorna, o nome dele vence.
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable(stack.getItem().getDescriptionId()).append(" +" + level));

        // Durabilidade máxima também sobe 10% por nível (calculada sobre a durabilidade original do item)
        int base = stack.getItem().getDefaultInstance().getMaxDamage();
        if (base > 0) {
            stack.set(DataComponents.MAX_DAMAGE, (int) Math.round(base * multiplier(level)));
        }
    }
}

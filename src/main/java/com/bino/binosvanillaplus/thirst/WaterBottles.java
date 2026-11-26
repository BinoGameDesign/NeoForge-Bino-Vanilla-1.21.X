package com.bino.binosvanillaplus.thirst;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

// Garrafa de agua do jogo (pocao "agua", sem efeitos). Classe pequena e sem estado: o mixin da pilha de 64 chama
// isto o tempo todo, inclusive cedo no carregamento.
public final class WaterBottles {
    private WaterBottles() {}

    public static boolean is(ItemStack s) {
        if (s.isEmpty() || s.getItem() != Items.POTION) return false;
        PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
        return pc != null && pc.is(Potions.WATER) && pc.customEffects().isEmpty();
    }
}

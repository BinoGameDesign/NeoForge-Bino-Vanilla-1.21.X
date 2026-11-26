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

    /** pilha maxima especial: garrafa de agua e frasquinho de agua acumulam 64; -1 = a do item */
    public static int maxStack(ItemStack s) {
        if (s.isEmpty()) return -1;
        if (s.getItem() == Items.POTION) return is(s) ? 64 : -1;
        if (s.getItem() instanceof FilledVesselItem v && v.size == 1) {
            PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
            if (pc != null && pc.is(Potions.WATER) && pc.customEffects().isEmpty()) return 64;
        }
        return -1;
    }
}

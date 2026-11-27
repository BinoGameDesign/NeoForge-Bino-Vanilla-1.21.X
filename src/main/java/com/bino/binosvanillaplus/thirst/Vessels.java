package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.item.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

// FRASCOS DO BINO (26/11, refeito em 27/11): cinco tamanhos de recipiente para agua e pocoes. Cada frasco e' bebido
// de uma vez so; quanto maior, mais ele vale (e mais demora para beber). Todos acumulam 64.
//   1 Frasquinho      x0,5  bebe rapido (0,8 s); agua = 2 gotas; pocao com metade do tempo/forca
//   2 Garrafa         x1    a garrafa do jogo (agua = 4 gotas)
//   3 Frasco          x2    2 garrafas de conteudo
//   4 Frasco Grande   x3    3 garrafas
//   5 Garrafao        x4    4 garrafas
// Encher de agua: clicar na agua. Agua do mundo pode dar nausea; fervida (fornalha, defumador ou fogueira) nao.
// Pocoes: na mesa de trabalho (VesselPourRecipe) -- frasco vazio + garrafas da mesma pocao (quantas ele leva) enche;
// frasco cheio + garrafas vazias (quantas ele leva) esvazia; 1 garrafa + 2 frasquinhos vazios divide; 2 frasquinhos
// iguais + 1 garrafa vazia junta.
public final class Vessels {
    private Vessels() {}

    public static boolean boiled(ItemStack s) { return s.getOrDefault(ModDataComponents.BOILED.get(), false); }

    public static PotionContents contents(ItemStack s) { return s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY); }

    public static boolean isWater(PotionContents pc) { return pc.is(Potions.WATER) && pc.customEffects().isEmpty(); }

    /** garrafa do jogo (pocao ou Agua Fervida) -> {conteudo, fervida?}; null se nao for garrafa cheia */
    public static Object[] bottle(ItemStack s) {
        if (s.getItem() instanceof BoiledWaterItem) return new Object[]{PotionContents.EMPTY.withPotion(Potions.WATER), true};
        if (s.is(Items.POTION)) return new Object[]{contents(s), false};
        return null;
    }

    /** garrafa do jogo com este conteudo */
    public static ItemStack toBottle(PotionContents pc, boolean boiled) {
        if (boiled && isWater(pc)) return new ItemStack(com.bino.binosvanillaplus.item.ModItems.BOILED_WATER.get());
        ItemStack b = new ItemStack(Items.POTION);
        b.set(DataComponents.POTION_CONTENTS, pc);
        return b;
    }

    public static ItemStack filled(FilledVesselItem item, PotionContents pc, boolean boiled) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.POTION_CONTENTS, pc);
        if (boiled && isWater(pc)) s.set(ModDataComponents.BOILED.get(), true);
        return s;
    }

    /** nome do que esta dentro: "Agua", "Agua Fervida" ou o nome da pocao do jogo */
    public static Component contentsName(PotionContents pc, boolean boiled) {
        if (isWater(pc)) return Component.translatable(boiled ? "vessel.binosvanillaplus.boiled_water" : "vessel.binosvanillaplus.water");
        return Component.translatable(Potion.getName(pc.potion(), "item.minecraft.potion.effect."));
    }
}

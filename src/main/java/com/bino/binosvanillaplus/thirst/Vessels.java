package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.item.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

// FRASCOS DO BINO (26/11): cinco tamanhos de recipiente para agua e pocoes.
//   1 Frasquinho      meia dose; bebe rapido (0,8 s); agua = 2 gotas; pocao com metade do tempo/forca. Acumula 16 (agua: 64)
//   2 Garrafa         a garrafa do jogo: 1 dose (agua = 4 gotas)
//   3 Frasco          2 doses
//   4 Frasco Grande   3 doses
//   5 Garrafao        4 doses
// Cada gole de um frasco grande gasta 1 dose (= 1 garrafa) e o frasco volta vazio na ultima.
// Encher de agua: clicar na agua (ou completar um frasco de agua pela metade). Agua do mundo pode dar nausea;
// frasco CHEIO de agua fervido na fornalha, defumador ou fogueira vira agua fervida.
// Pocoes: na mesa de trabalho (VesselPourRecipe) -- frasco + garrafas da mesma pocao enche; frasco + garrafa vazia tira
// uma dose; 1 garrafa + 2 frasquinhos vazios divide em 2 meias doses; 2 frasquinhos iguais + garrafa vazia juntam.
public final class Vessels {
    private Vessels() {}

    public static int doses(ItemStack s) { return s.getOrDefault(ModDataComponents.DOSES.get(), 1); }
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

    public static ItemStack filled(FilledVesselItem item, PotionContents pc, int doses, boolean boiled) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.POTION_CONTENTS, pc);
        s.set(ModDataComponents.DOSES.get(), doses);
        if (boiled && isWater(pc)) s.set(ModDataComponents.BOILED.get(), true);
        return s;
    }

    /** nome do que esta dentro: "Agua", "Agua Fervida" ou o nome da pocao do jogo */
    public static Component contentsName(PotionContents pc, boolean boiled) {
        if (isWater(pc)) return Component.translatable(boiled ? "vessel.binosvanillaplus.boiled_water" : "vessel.binosvanillaplus.water");
        return Component.translatable(Potion.getName(pc.potion(), "item.minecraft.potion.effect."));
    }
}

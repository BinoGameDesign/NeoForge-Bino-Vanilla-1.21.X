package com.bino.binosvanillaplus.client.recipes;

import com.bino.binosvanillaplus.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

// Tipos de receita mostrados no navegador (cada um e' uma aba)
public enum RecipeCategory {
    CRAFTING("crafting", "Bancada", () -> new ItemStack(Items.CRAFTING_TABLE)),
    SMELTING("smelting", "Fornalha", () -> new ItemStack(Items.FURNACE)),
    BLASTING("blasting", "Alto-forno", () -> new ItemStack(Items.BLAST_FURNACE)),
    SMOKING("smoking", "Defumador", () -> new ItemStack(Items.SMOKER)),
    CAMPFIRE("campfire", "Fogueira", () -> new ItemStack(Items.CAMPFIRE)),
    STONECUTTING("stonecutting", "Cortador de pedra", () -> new ItemStack(Items.STONECUTTER)),
    SMITHING("smithing", "Bancada de ferreiro", () -> new ItemStack(Items.SMITHING_TABLE)),
    LAPIDARY("lapidary", "Mesa de Lapidação", () -> new ItemStack(ModBlocks.LAPIDARY_TABLE.get()));

    private final String id;
    private final String fallback;
    private final Supplier<ItemStack> icon;

    RecipeCategory(String id, String fallback, Supplier<ItemStack> icon) {
        this.id = id;
        this.fallback = fallback;
        this.icon = icon;
    }

    public ItemStack icon() { return icon.get(); }

    public Component title() {
        return Component.translatableWithFallback("recipes.binosvanillaplus.category." + id, fallback);
    }
}

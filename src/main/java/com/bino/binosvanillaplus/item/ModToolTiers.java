package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.util.ModTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
    public static final Tier RUBI = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_RUBI_TOOL,
            1000, 7.5f, 3f, 8,() -> Ingredient.of(ModItems.RUBI));

    // Safira e Jade: exatamente os status do diamante vanilla (durabilidade 1561, velocidade 8, dano 3, encantamento 10).
    // BlockTags.INCORRECT_FOR_DIAMOND_TOOL = minera tudo que o diamante minera.
    public static final Tier SAPPHIRE = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1561, 8.0f, 3.0f, 10, () -> Ingredient.of(ModItems.SAPPHIRE));

    public static final Tier JADE = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1561, 8.0f, 3.0f, 10, () -> Ingredient.of(ModItems.JADE));

}

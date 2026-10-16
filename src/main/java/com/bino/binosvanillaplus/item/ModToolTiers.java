package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.util.ModTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
    public static final Tier RUBI = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_RUBI_TOOL,
            1000, 7.5f, 3f, 8,() -> Ingredient.of(ModItems.RUBI));

}

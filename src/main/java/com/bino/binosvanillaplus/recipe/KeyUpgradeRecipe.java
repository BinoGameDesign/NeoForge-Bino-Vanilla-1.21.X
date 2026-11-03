package com.bino.binosvanillaplus.recipe;

import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.custom.DimensionalKeyItem;
import com.bino.binosvanillaplus.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Upgrade da Chave Dimensional: a chave no CENTRO da mesa e 8 minerios IGUAIS em volta = +1 nivel (sala +2x+2).
// O minerio precisa estar na tag binosvanillaplus:dimensional_key_ores e nao pode ter sido usado antes nessa chave.
public class KeyUpgradeRecipe extends CustomRecipe {
    public KeyUpgradeRecipe(CraftingBookCategory category) { super(category); }

    // o minerio das 8 casas em volta (ou null se a receita nao se aplica)
    @Nullable
    private static Item ore(CraftingInput in) {
        if (in.width() != 3 || in.height() != 3) return null;
        ItemStack key = in.getItem(4);
        if (!(key.getItem() instanceof DimensionalKeyItem)) return null;
        Item ore = null;
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            ItemStack s = in.getItem(i);
            if (s.isEmpty() || !s.is(ModTags.Items.KEY_ORES)) return null;
            if (ore == null) ore = s.getItem();
            else if (s.getItem() != ore) return null;      // os 8 tem que ser o mesmo minerio
        }
        if (ore == null) return null;
        if (DimensionalKeyItem.usedOres(key).contains(BuiltInRegistries.ITEM.getKey(ore))) return null;   // nao repete
        return ore;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) { return ore(input) != null; }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Item ore = ore(input);
        ItemStack key = input.getItem(4);
        if (ore == null) return ItemStack.EMPTY;
        ItemStack out = key.copyWithCount(1);
        out.set(ModDataComponents.KEY_LEVEL.get(), DimensionalKeyItem.level(key) + 1);
        List<ResourceLocation> used = new ArrayList<>(DimensionalKeyItem.usedOres(key));
        used.add(BuiltInRegistries.ITEM.getKey(ore));
        out.set(ModDataComponents.KEY_ORES.get(), used);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width >= 3 && height >= 3; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.KEY_UPGRADE.get(); }
}

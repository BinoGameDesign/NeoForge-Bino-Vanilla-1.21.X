package com.bino.binosvanillaplus.recipe;

import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.custom.FlashlightItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Capacete + Lanterna (em qualquer lugar da mesa) = capacete com a lanterna na lateral.
// Capacete com lanterna SOZINHO na mesa = devolve a lanterna (o capacete volta sem ela).
public class HelmetLampRecipe extends CustomRecipe {
    public HelmetLampRecipe(CraftingBookCategory category) { super(category); }

    private static boolean hasLamp(ItemStack s) { return s.getOrDefault(ModDataComponents.HELMET_LAMP.get(), false); }

    // {capacete, lanterna} ou null
    private static ItemStack[] parts(CraftingInput in) {
        ItemStack helmet = ItemStack.EMPTY, lamp = ItemStack.EMPTY;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(ItemTags.HEAD_ARMOR) && helmet.isEmpty()) helmet = s;
            else if (s.getItem() instanceof FlashlightItem && lamp.isEmpty()) lamp = s;
            else return null;
        }
        if (helmet.isEmpty()) return null;
        return new ItemStack[]{helmet, lamp};
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        ItemStack[] p = parts(in);
        if (p == null) return false;
        return p[1].isEmpty() ? hasLamp(p[0]) : !hasLamp(p[0]);
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
        ItemStack[] p = parts(in);
        if (p == null) return ItemStack.EMPTY;
        if (p[1].isEmpty()) return new ItemStack(ModItems.FLASHLIGHT.get());   // tirando a lanterna
        ItemStack out = p[0].copyWithCount(1);
        out.set(ModDataComponents.HELMET_LAMP.get(), true);
        out.set(ModDataComponents.FLASHLIGHT_ON.get(), false);
        return out;
    }

    // tirando a lanterna: o capacete fica na mesa, sem ela
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput in) {
        NonNullList<ItemStack> rest = NonNullList.withSize(in.size(), ItemStack.EMPTY);
        ItemStack[] p = parts(in);
        if (p == null || !p[1].isEmpty()) return rest;
        for (int i = 0; i < in.size(); i++) {
            if (in.getItem(i) == p[0]) {
                ItemStack h = p[0].copyWithCount(1);
                h.remove(ModDataComponents.HELMET_LAMP.get());
                h.remove(ModDataComponents.FLASHLIGHT_ON.get());
                rest.set(i, h);
            }
        }
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.HELMET_LAMP.get(); }
}

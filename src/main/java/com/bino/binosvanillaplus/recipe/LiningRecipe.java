package com.bino.binosvanillaplus.recipe;

import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.thirst.Temperature;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Forro na armadura (temperatura): 1 peca de armadura + 1 Forro de La (frio) ou Forro Leve (calor), em qualquer
// posicao da mesa. A armadura sai com o forro (troca o que tinha) e mantem tudo o resto (defesa, encantamentos).
public class LiningRecipe extends CustomRecipe {
    public LiningRecipe(CraftingBookCategory category) { super(category); }

    private static int liningOf(ItemStack s) {
        if (s.is(ModItems.WOOL_LINING.get())) return Temperature.LINING_WOOL;
        if (s.is(ModItems.LIGHT_LINING.get())) return Temperature.LINING_LIGHT;
        return -1;
    }

    // {armadura, tipo do forro} ou null
    private static Object[] parts(CraftingInput in) {
        ItemStack armor = ItemStack.EMPTY; int lining = -1;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            int l = liningOf(s);
            if (l >= 0) { if (lining >= 0) return null; lining = l; }
            else if (s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() != EquipmentSlot.BODY) { if (!armor.isEmpty()) return null; armor = s; }
            else return null;
        }
        if (armor.isEmpty() || lining < 0) return null;
        if (armor.getOrDefault(ModDataComponents.LINING.get(), 0) == lining) return null;   // ja tem esse forro
        return new Object[]{armor, lining};
    }

    @Override public boolean matches(CraftingInput in, Level level) { return parts(in) != null; }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
        Object[] p = parts(in);
        if (p == null) return ItemStack.EMPTY;
        ItemStack out = ((ItemStack) p[0]).copyWithCount(1);
        out.set(ModDataComponents.LINING.get(), (Integer) p[1]);
        return out;
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.LINING.get(); }
}

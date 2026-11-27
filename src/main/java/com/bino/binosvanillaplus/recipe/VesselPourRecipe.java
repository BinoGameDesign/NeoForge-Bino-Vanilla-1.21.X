package com.bino.binosvanillaplus.recipe;

import com.bino.binosvanillaplus.thirst.FilledVesselItem;
import com.bino.binosvanillaplus.thirst.VesselItem;
import com.bino.binosvanillaplus.thirst.Vessels;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// Passar agua e pocoes entre garrafas e frascos, na mesa de trabalho (sem posicao fixa). Cada frasco leva um numero
// certo de garrafas (Frasco 2, Frasco Grande 3, Garrafao 4) e e' sempre cheio ou vazio:
//  1. ENCHER: frasco vazio + garrafas da mesma pocao/agua (quantas ele leva) -> frasco cheio; garrafas vazias ficam.
//  2. ESVAZIAR: frasco cheio + garrafas vazias (quantas ele leva) -> as garrafas cheias; o frasco vazio fica na mesa.
//  3. DIVIDIR: 1 garrafa + 2 frasquinhos vazios -> 2 frasquinhos; a garrafa vazia fica na mesa.
//  4. JUNTAR: 2 frasquinhos iguais + 1 garrafa vazia -> 1 garrafa; os frasquinhos vazios ficam na mesa.
// Agua fervida so continua fervida se tudo o que entrou era fervido.
public class VesselPourRecipe extends CustomRecipe {
    public VesselPourRecipe(CraftingBookCategory category) { super(category); }

    private enum Kind { FILL, EMPTY, SPLIT, JOIN }

    // result = o que sai no resultado; fill = o que as garrafas vazias viram (ESVAZIAR)
    private record Plan(Kind kind, ItemStack result, ItemStack fill) {}

    private static boolean same(PotionContents a, PotionContents b) {
        return a.equals(b) || (Vessels.isWater(a) && Vessels.isWater(b));
    }

    private static Plan plan(CraftingInput in) {
        int bigFilled = -1, bigEmpty = -1, glass = 0;
        List<Integer> bottles = new ArrayList<>(), emptyVials = new ArrayList<>(), fullVials = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof FilledVesselItem f) {
                if (f.size == 1) fullVials.add(i);
                else if (bigFilled < 0 && bigEmpty < 0) bigFilled = i; else return null;
            } else if (s.getItem() instanceof VesselItem v) {
                if (v.size == 1) emptyVials.add(i);
                else if (bigFilled < 0 && bigEmpty < 0) bigEmpty = i; else return null;
            } else if (Vessels.bottle(s) != null) bottles.add(i);
            else if (s.is(Items.GLASS_BOTTLE)) glass++;
            else return null;
        }
        boolean vialsUsed = !emptyVials.isEmpty() || !fullVials.isEmpty();

        // 1. encher um frasco vazio com garrafas
        if (bigEmpty >= 0 && !bottles.isEmpty() && glass == 0 && !vialsUsed) {
            FilledVesselItem target = ((VesselItem) in.getItem(bigEmpty).getItem()).filled();
            if (bottles.size() != target.bottles()) return null;
            PotionContents pc = (PotionContents) Vessels.bottle(in.getItem(bottles.get(0)))[0];
            boolean boiled = true;
            for (int i : bottles) {
                Object[] b = Vessels.bottle(in.getItem(i));
                if (!same(pc, (PotionContents) b[0])) return null;
                boiled &= (Boolean) b[1];
            }
            return new Plan(Kind.FILL, Vessels.filled(target, pc, boiled), ItemStack.EMPTY);
        }
        // 2. esvaziar um frasco em garrafas vazias
        if (bigFilled >= 0 && bottles.isEmpty() && !vialsUsed) {
            ItemStack v = in.getItem(bigFilled);
            FilledVesselItem f = (FilledVesselItem) v.getItem();
            if (glass != f.bottles()) return null;
            ItemStack b = Vessels.toBottle(Vessels.contents(v), Vessels.boiled(v));
            return new Plan(Kind.EMPTY, b, b);
        }
        boolean big = bigFilled >= 0 || bigEmpty >= 0;
        // 3. dividir uma garrafa em 2 frasquinhos
        if (!big && bottles.size() == 1 && emptyVials.size() == 2 && glass == 0 && fullVials.isEmpty()) {
            Object[] b = Vessels.bottle(in.getItem(bottles.get(0)));
            FilledVesselItem vial = ((VesselItem) in.getItem(emptyVials.get(0)).getItem()).filled();
            ItemStack out = Vessels.filled(vial, (PotionContents) b[0], (Boolean) b[1]);
            out.setCount(2);
            return new Plan(Kind.SPLIT, out, ItemStack.EMPTY);
        }
        // 4. juntar 2 frasquinhos iguais numa garrafa
        if (!big && fullVials.size() == 2 && glass == 1 && bottles.isEmpty() && emptyVials.isEmpty()) {
            ItemStack a = in.getItem(fullVials.get(0)), b = in.getItem(fullVials.get(1));
            if (!same(Vessels.contents(a), Vessels.contents(b))) return null;
            boolean boiled = Vessels.boiled(a) && Vessels.boiled(b);
            return new Plan(Kind.JOIN, Vessels.toBottle(Vessels.contents(a), boiled), ItemStack.EMPTY);
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput in, Level level) { return plan(in) != null; }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
        Plan p = plan(in);
        return p == null ? ItemStack.EMPTY : p.result().copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput in) {
        NonNullList<ItemStack> rest = NonNullList.withSize(in.size(), ItemStack.EMPTY);
        Plan p = plan(in);
        if (p == null) return rest;
        boolean firstGlass = true;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            switch (p.kind()) {
                case FILL, SPLIT -> { if (Vessels.bottle(s) != null) rest.set(i, new ItemStack(Items.GLASS_BOTTLE)); }
                case EMPTY -> {
                    // uma garrafa sai no resultado; as outras viram garrafas cheias ali mesmo; o frasco volta vazio
                    if (s.getItem() instanceof FilledVesselItem f) rest.set(i, new ItemStack(f.empty()));
                    else if (s.is(Items.GLASS_BOTTLE)) { if (firstGlass) firstGlass = false; else rest.set(i, p.fill().copy()); }
                }
                case JOIN -> { if (s.getItem() instanceof FilledVesselItem f) rest.set(i, new ItemStack(f.empty())); }
            }
        }
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.VESSEL_POUR.get(); }
}

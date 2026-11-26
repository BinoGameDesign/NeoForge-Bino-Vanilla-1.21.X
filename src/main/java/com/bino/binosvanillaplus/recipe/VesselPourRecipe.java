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

// Passar agua e pocoes entre garrafas e frascos, na mesa de trabalho (sem posicao fixa):
//  1. ENCHER: frasco (vazio ou com a mesma coisa) + garrafas da mesma pocao/agua -> frasco com mais doses.
//     As garrafas vazias ficam na mesa.
//  2. TIRAR: frasco cheio + 1 garrafa vazia -> 1 garrafa daquilo; o frasco fica na mesa com uma dose a menos.
//  3. DIVIDIR: 1 garrafa + 2 frasquinhos vazios -> 2 frasquinhos (meia dose cada); a garrafa vazia fica na mesa.
//  4. JUNTAR: 2 frasquinhos iguais + 1 garrafa vazia -> 1 garrafa; os frasquinhos vazios ficam na mesa.
// Agua fervida so continua fervida se tudo o que entrou era fervido.
public class VesselPourRecipe extends CustomRecipe {
    public VesselPourRecipe(CraftingBookCategory category) { super(category); }

    private enum Kind { FILL, TAKE, SPLIT, JOIN }

    private record Plan(Kind kind, ItemStack result, int vesselSlot, ItemStack vesselAfter) {}

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
        boolean big = bigFilled >= 0 || bigEmpty >= 0;

        // 1. encher um frasco com garrafas
        if (big && !bottles.isEmpty() && glass == 0 && emptyVials.isEmpty() && fullVials.isEmpty()) {
            Object[] b0 = Vessels.bottle(in.getItem(bottles.get(0)));
            PotionContents pc = (PotionContents) b0[0];
            boolean boiled = true;
            for (int i : bottles) {
                Object[] b = Vessels.bottle(in.getItem(i));
                if (!pc.equals(b[0]) && !(Vessels.isWater(pc) && Vessels.isWater((PotionContents) b[0]))) return null;
                boiled &= (Boolean) b[1];
            }
            FilledVesselItem target;
            int doses = 0;
            if (bigFilled >= 0) {
                ItemStack v = in.getItem(bigFilled);
                PotionContents vpc = Vessels.contents(v);
                if (!vpc.equals(pc) && !(Vessels.isWater(vpc) && Vessels.isWater(pc))) return null;
                target = (FilledVesselItem) v.getItem();
                doses = Vessels.doses(v);
                boiled &= Vessels.boiled(v);
            } else target = ((VesselItem) in.getItem(bigEmpty).getItem()).filled();
            int total = doses + bottles.size();
            if (total > target.maxDoses) return null;
            return new Plan(Kind.FILL, Vessels.filled(target, pc, total, boiled), -1, ItemStack.EMPTY);
        }
        // 2. tirar uma dose para uma garrafa vazia
        if (bigFilled >= 0 && glass == 1 && bottles.isEmpty() && emptyVials.isEmpty() && fullVials.isEmpty()) {
            ItemStack v = in.getItem(bigFilled);
            FilledVesselItem f = (FilledVesselItem) v.getItem();
            int d = Vessels.doses(v);
            ItemStack after = d > 1 ? v.copyWithCount(1) : new ItemStack(f.empty());
            if (d > 1) after.set(com.bino.binosvanillaplus.item.ModDataComponents.DOSES.get(), d - 1);
            return new Plan(Kind.TAKE, Vessels.toBottle(Vessels.contents(v), Vessels.boiled(v)), bigFilled, after);
        }
        // 3. dividir uma garrafa em 2 frasquinhos
        if (!big && bottles.size() == 1 && emptyVials.size() == 2 && glass == 0 && fullVials.isEmpty()) {
            Object[] b = Vessels.bottle(in.getItem(bottles.get(0)));
            FilledVesselItem vial = ((VesselItem) in.getItem(emptyVials.get(0)).getItem()).filled();
            ItemStack out = Vessels.filled(vial, (PotionContents) b[0], 1, (Boolean) b[1]);
            out.setCount(2);
            return new Plan(Kind.SPLIT, out, -1, ItemStack.EMPTY);
        }
        // 4. juntar 2 frasquinhos iguais numa garrafa
        if (!big && fullVials.size() == 2 && glass == 1 && bottles.isEmpty() && emptyVials.isEmpty()) {
            ItemStack a = in.getItem(fullVials.get(0)), b = in.getItem(fullVials.get(1));
            if (!Vessels.contents(a).equals(Vessels.contents(b))) return null;
            boolean boiled = Vessels.boiled(a) && Vessels.boiled(b);
            return new Plan(Kind.JOIN, Vessels.toBottle(Vessels.contents(a), boiled), -1, ItemStack.EMPTY);
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
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            switch (p.kind()) {
                case FILL, SPLIT -> { if (Vessels.bottle(s) != null) rest.set(i, new ItemStack(Items.GLASS_BOTTLE)); }
                case TAKE -> { if (i == p.vesselSlot()) rest.set(i, p.vesselAfter()); }
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

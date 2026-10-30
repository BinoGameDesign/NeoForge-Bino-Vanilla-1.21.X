package com.bino.binosvanillaplus.client.recipes;

import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.menu.LapidaryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.*;

// Indice de receitas: "como fazer o item X" e "onde o item X e' usado". Reconstruido quando as receitas mudam.
public class RecipeIndex {
    private static RecipeIndex instance;
    private static int key;

    private final List<RecipeEntry> entries = new ArrayList<>();
    private final Map<Item, List<RecipeEntry>> byOutput = new HashMap<>();
    private final Map<Item, List<RecipeEntry>> byInput = new HashMap<>();

    private static List<ItemStack> allItems;

    public static RecipeIndex get() {
        ClientLevel level = Minecraft.getInstance().level;
        int k = level == null ? 0 : Objects.hash(System.identityHashCode(level), level.getRecipeManager().getRecipes().size());
        if (instance == null || k != key) {
            instance = new RecipeIndex();
            if (level != null) instance.build(level);
            key = k;
        }
        return instance;
    }

    public List<RecipeEntry> producing(Item item) { return byOutput.getOrDefault(item, List.of()); }
    public List<RecipeEntry> using(Item item) { return byInput.getOrDefault(item, List.of()); }

    // Todos os itens do jogo (sem o ar), na ordem do registro
    public static List<ItemStack> allItems() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) allItems.add(new ItemStack(item));
            }
        }
        return allItems;
    }

    private void build(ClientLevel level) {
        HolderLookup.Provider access = level.registryAccess();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            try {
                Recipe<?> r = holder.value();
                ItemStack out = r.getResultItem(access);
                if (out.isEmpty()) continue;   // receitas "especiais" (tingir armadura etc.) nao tem resultado fixo
                if (r instanceof CraftingRecipe cr) addCrafting(holder, cr, out);
                else if (r instanceof AbstractCookingRecipe c) addCooking(holder, c, out);
                else if (r instanceof StonecutterRecipe s) addSingle(holder, s, out);
                else if (r instanceof SmithingRecipe s) addSmithing(holder, s, out);
            } catch (Exception ignored) {
                // uma receita estranha de outro mod nao pode quebrar o navegador
            }
        }
        // Mesa de Lapidacao (nao e' uma receita do jogo: vem do mapa bruto -> gema do proprio menu)
        for (var raw : List.of(ModItems.RAW_DIAMOND, ModItems.RAW_RUBI, ModItems.RAW_SAPPHIRE, ModItems.RAW_JADE)) {
            Item gem = LapidaryMenu.gemFor(new ItemStack(raw.get()));
            if (gem == null) continue;
            add(new RecipeEntry(RecipeCategory.LAPIDARY, null,
                    List.of(List.of(new ItemStack(raw.get())), List.of(new ItemStack(Items.FLINT))),
                    new ItemStack(gem), 0, 0));
        }
    }

    private static List<ItemStack> alternatives(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        return items.length == 0 ? List.of() : Arrays.asList(items);
    }

    private void addCrafting(RecipeHolder<?> holder, CraftingRecipe r, ItemStack out) {
        NonNullList<Ingredient> ings = r.getIngredients();
        List<List<ItemStack>> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(List.of());
        if (r instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth(), h = shaped.getHeight();
            for (int y = 0; y < h && y < 3; y++)
                for (int x = 0; x < w && x < 3; x++)
                    grid.set(y * 3 + x, alternatives(ings.get(y * w + x)));
        } else {
            int cols = ings.size() <= 4 ? 2 : 3;   // receitas sem forma: preenche em fila
            for (int i = 0; i < ings.size() && i < 9; i++)
                grid.set((i / cols) * 3 + (i % cols), alternatives(ings.get(i)));
        }
        add(new RecipeEntry(RecipeCategory.CRAFTING, holder, grid, out, 0, 0));
    }

    private void addCooking(RecipeHolder<?> holder, AbstractCookingRecipe c, ItemStack out) {
        RecipeCategory cat;
        RecipeType<?> t = c.getType();
        if (t == RecipeType.BLASTING) cat = RecipeCategory.BLASTING;
        else if (t == RecipeType.SMOKING) cat = RecipeCategory.SMOKING;
        else if (t == RecipeType.CAMPFIRE_COOKING) cat = RecipeCategory.CAMPFIRE;
        else cat = RecipeCategory.SMELTING;
        add(new RecipeEntry(cat, holder, List.of(alternatives(c.getIngredients().get(0))), out, c.getCookingTime(), c.getExperience()));
    }

    private void addSingle(RecipeHolder<?> holder, StonecutterRecipe s, ItemStack out) {
        add(new RecipeEntry(RecipeCategory.STONECUTTING, holder, List.of(alternatives(s.getIngredients().get(0))), out, 0, 0));
    }

    // Os ingredientes da bancada de ferreiro nao tem acesso publico: descobrimos testando todos os itens
    private void addSmithing(RecipeHolder<?> holder, SmithingRecipe s, ItemStack out) {
        List<ItemStack> template = new ArrayList<>(), base = new ArrayList<>(), addition = new ArrayList<>();
        for (ItemStack st : allItems()) {
            if (s.isTemplateIngredient(st)) template.add(st);
            if (s.isBaseIngredient(st)) base.add(st);
            if (s.isAdditionIngredient(st)) addition.add(st);
        }
        add(new RecipeEntry(RecipeCategory.SMITHING, holder, List.of(template, base, addition), out, 0, 0));
    }

    private void add(RecipeEntry e) {
        entries.add(e);
        byOutput.computeIfAbsent(e.output().getItem(), k -> new ArrayList<>()).add(e);
        Set<Item> seen = new HashSet<>();
        for (List<ItemStack> slot : e.inputs())
            for (ItemStack st : slot)
                if (seen.add(st.getItem())) byInput.computeIfAbsent(st.getItem(), k -> new ArrayList<>()).add(e);
    }
}

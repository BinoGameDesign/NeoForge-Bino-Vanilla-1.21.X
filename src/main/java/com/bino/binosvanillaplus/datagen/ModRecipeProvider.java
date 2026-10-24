package com.bino.binosvanillaplus.datagen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    // Aqui você lista todas as receitas do mod
    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        // ----- FERRAMENTAS -----
        // 'R' = rubi, 'S' = graveto (stick). Cada linha do pattern é uma linha da mesa de trabalho.

        // Espada: rubi em cima, rubi no meio, graveto embaixo
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RUBI_SWORD.get())
                .pattern(" R ")
                .pattern(" R ")
                .pattern(" S ")
                .define('R', ModItems.RUBI.get())            // liga a letra R ao item rubi
                .define('S', Items.STICK)                    // liga a letra S ao graveto
                .unlockedBy("has_rubi", has(ModItems.RUBI.get()))  // a receita aparece no livro quando você tem rubi
                .save(recipeOutput);                         // grava a receita

        // Picareta
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.RUBI_PICKAXE.get())
                .pattern("RRR")
                .pattern(" S ")
                .pattern(" S ")
                .define('R', ModItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(ModItems.RUBI.get()))
                .save(recipeOutput);

        // Pá
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.RUBI_SHOVEL.get())
                .pattern(" R ")
                .pattern(" S ")
                .pattern(" S ")
                .define('R', ModItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(ModItems.RUBI.get()))
                .save(recipeOutput);

        // Machado
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.RUBI_AXE.get())
                .pattern("RR ")
                .pattern("RS ")
                .pattern(" S ")
                .define('R', ModItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(ModItems.RUBI.get()))
                .save(recipeOutput);

        // Enxada
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.RUBI_HOE.get())
                .pattern("RR ")
                .pattern(" S ")
                .pattern(" S ")
                .define('R', ModItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(ModItems.RUBI.get()))
                .save(recipeOutput);

        // ----- BLOCO DE RUBI -----

        // 9 rubis (3x3) viram 1 bloco de rubi
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.RUBI_BLOCK.get())
                .pattern("RRR")
                .pattern("RRR")
                .pattern("RRR")
                .define('R', ModItems.RUBI.get())
                .unlockedBy("has_rubi", has(ModItems.RUBI.get()))
                .save(recipeOutput);

        // ----- BASTÃO DE CYALUME -----
// 'G' = tinta de lula brilhante, 'S' = graveto. Diagonal: canto superior direito, centro, canto inferior esquerdo
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CYALUME_STICK.get())
                .pattern("  G")
                .pattern(" S ")
                .pattern("G  ")
                .define('G', Items.GLOW_INK_SAC)
                .define('S', Items.STICK)
                .unlockedBy("has_glow_ink_sac", has(Items.GLOW_INK_SAC))   // a receita aparece no livro ao pegar a tinta
                .save(recipeOutput);

        // 1 bloco de rubi vira 9 rubis (sem formato: "shapeless" = ingredientes em qualquer lugar).
        // O nome explícito evita conflito com a receita da fornalha, que também gera o rubi.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RUBI.get(), 9)
                .requires(ModBlocks.RUBI_BLOCK.get())
                .unlockedBy("has_rubi_block", has(ModBlocks.RUBI_BLOCK.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "rubi_from_rubi_block"));


        // ===== Safira e Jade =====
        gemSet(recipeOutput, "sapphire", ModItems.SAPPHIRE.get(), ModItems.SAPPHIRE_SWORD.get(), ModItems.SAPPHIRE_PICKAXE.get(),
                ModItems.SAPPHIRE_SHOVEL.get(), ModItems.SAPPHIRE_AXE.get(), ModItems.SAPPHIRE_HOE.get(),
                ModBlocks.SAPPHIRE_BLOCK.get(), ModBlocks.SAPPHIRE_ORE.get(), ModBlocks.SAPPHIRE_DEEPSLATE_ORE.get());
        gemSet(recipeOutput, "jade", ModItems.JADE.get(), ModItems.JADE_SWORD.get(), ModItems.JADE_PICKAXE.get(),
                ModItems.JADE_SHOVEL.get(), ModItems.JADE_AXE.get(), ModItems.JADE_HOE.get(),
                ModBlocks.JADE_BLOCK.get(), ModBlocks.JADE_ORE.get(), ModBlocks.JADE_DEEPSLATE_ORE.get());

        // ===== Armaduras =====
        armorSet(recipeOutput, "rubi", ModItems.RUBI.get(), ModItems.RUBI_HELMET.get(), ModItems.RUBI_CHESTPLATE.get(),
                ModItems.RUBI_LEGGINGS.get(), ModItems.RUBI_BOOTS.get());
        armorSet(recipeOutput, "sapphire", ModItems.SAPPHIRE.get(), ModItems.SAPPHIRE_HELMET.get(), ModItems.SAPPHIRE_CHESTPLATE.get(),
                ModItems.SAPPHIRE_LEGGINGS.get(), ModItems.SAPPHIRE_BOOTS.get());
        armorSet(recipeOutput, "jade", ModItems.JADE.get(), ModItems.JADE_HELMET.get(), ModItems.JADE_CHESTPLATE.get(),
                ModItems.JADE_LEGGINGS.get(), ModItems.JADE_BOOTS.get());

        // ===== Forja =====
        forgeRecipes(recipeOutput);

        // ===== Bloco de pedra filosofal e Mesa de Lapidação =====
        extraRecipes(recipeOutput);
    }

    // Cria TODAS as receitas de uma gema (ferramentas, bloco, desfazer bloco e fundição).
    // Assim safira e jade usam o mesmo código, só mudando os itens.
    private void gemSet(RecipeOutput out, String name, ItemLike gem, ItemLike sword, ItemLike pickaxe,
                        ItemLike shovel, ItemLike axe, ItemLike hoe, ItemLike block, ItemLike ore, ItemLike deepslateOre) {
        // Ferramentas ('G' = gema, 'S' = graveto)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, sword).pattern(" G ").pattern(" G ").pattern(" S ")
                .define('G', gem).define('S', Items.STICK).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, pickaxe).pattern("GGG").pattern(" S ").pattern(" S ")
                .define('G', gem).define('S', Items.STICK).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, shovel).pattern(" G ").pattern(" S ").pattern(" S ")
                .define('G', gem).define('S', Items.STICK).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, axe).pattern("GG ").pattern("GS ").pattern(" S ")
                .define('G', gem).define('S', Items.STICK).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, hoe).pattern("GG ").pattern(" S ").pattern(" S ")
                .define('G', gem).define('S', Items.STICK).unlockedBy("has_" + name, has(gem)).save(out);

        // 9 gemas viram 1 bloco
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block).pattern("GGG").pattern("GGG").pattern("GGG")
                .define('G', gem).unlockedBy("has_" + name, has(gem)).save(out);
        // 1 bloco vira 9 gemas (nome explícito para não colidir com a receita da fornalha)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, gem, 9).requires(block)
                .unlockedBy("has_" + name + "_block", has(block))
                .save(out, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name + "_from_" + name + "_block"));

    }

    // Receitas das 4 peças de armadura de uma gema ('G' = gema)
    private void armorSet(RecipeOutput out, String name, ItemLike gem, ItemLike helmet, ItemLike chestplate,
                          ItemLike leggings, ItemLike boots) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, helmet).pattern("GGG").pattern("G G")
                .define('G', gem).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, chestplate).pattern("G G").pattern("GGG").pattern("GGG")
                .define('G', gem).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, leggings).pattern("GGG").pattern("G G").pattern("G G")
                .define('G', gem).unlockedBy("has_" + name, has(gem)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, boots).pattern("G G").pattern("G G")
                .define('G', gem).unlockedBy("has_" + name, has(gem)).save(out);
    }

    // Receitas da forja e da pedra filosofal
    private void forgeRecipes(RecipeOutput out) {
        // 4 pedras +n em quadrado (2x2) -> 1 pedra +(n+1), de +1 até +5
        for (int n = 1; n <= 4; n++) {
            ItemLike from = ModItems.FORGE_STONES.get(n - 1).get();
            ItemLike to = ModItems.FORGE_STONES.get(n).get();
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, to).pattern("SS").pattern("SS")
                    .define('S', from).unlockedBy("has_forge_stone_" + n, has(from)).save(out);
        }

        // Fragmento de pedra filosofal = diamante + rubi + safira + jade (sem formato fixo)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PHILOSOPHER_FRAGMENT.get())
                .requires(Items.DIAMOND).requires(ModItems.RUBI.get())
                .requires(ModItems.SAPPHIRE.get()).requires(ModItems.JADE.get())
                .unlockedBy("has_rubi", has(ModItems.RUBI.get())).save(out);

        // 4 fragmentos -> incompleta; 4 incompletas -> pequena; 4 pequenas -> grande (todos em quadrado 2x2)
        square(out, ModItems.PHILOSOPHER_FRAGMENT.get(), ModItems.PHILOSOPHER_STONE_INCOMPLETE.get());
        square(out, ModItems.PHILOSOPHER_STONE_INCOMPLETE.get(), ModItems.PHILOSOPHER_STONE_SMALL.get());
        square(out, ModItems.PHILOSOPHER_STONE_SMALL.get(), ModItems.PHILOSOPHER_STONE_LARGE.get());

        // +6 = +5 + fragmento; +7 = +6 + incompleta; +8 = +7 + pequena; +9 = +8 + grande
        pair(out, 5, ModItems.PHILOSOPHER_FRAGMENT.get(), 6);
        pair(out, 6, ModItems.PHILOSOPHER_STONE_INCOMPLETE.get(), 7);
        pair(out, 7, ModItems.PHILOSOPHER_STONE_SMALL.get(), 8);
        pair(out, 8, ModItems.PHILOSOPHER_STONE_LARGE.get(), 9);

        // +10 = pedra +9 no centro e 8 pedras filosofais grandes ao redor (mesa de trabalho cheia)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FORGE_STONE_10.get())
                .pattern("LLL").pattern("LFL").pattern("LLL")
                .define('L', ModItems.PHILOSOPHER_STONE_LARGE.get())
                .define('F', ModItems.FORGE_STONE_9.get())
                .unlockedBy("has_forge_stone_9", has(ModItems.FORGE_STONE_9.get())).save(out);
    }

    // 4 itens iguais em quadrado 2x2 viram 1 item do próximo tipo
    private void square(RecipeOutput out, ItemLike from, ItemLike to) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, to).pattern("SS").pattern("SS")
                .define('S', from).unlockedBy("has_" + "ingredient", has(from)).save(out);
    }

    // Pedra +fromLevel + um item filosofal -> pedra +toLevel (sem formato fixo)
    private void pair(RecipeOutput out, int fromLevel, ItemLike extra, int toLevel) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FORGE_STONES.get(toLevel - 1).get())
                .requires(ModItems.FORGE_STONES.get(fromLevel - 1).get()).requires(extra)
                .unlockedBy("has_forge_stone_" + fromLevel, has(ModItems.FORGE_STONES.get(fromLevel - 1).get())).save(out);
    }

    // Bloco de pedra filosofal (ida e volta) e a Mesa de Lapidação
    private void extraRecipes(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PHILOSOPHER_BLOCK.get())
                .pattern("LLL").pattern("LLL").pattern("LLL")
                .define('L', ModItems.PHILOSOPHER_STONE_LARGE.get())
                .unlockedBy("has_philosopher_stone_large", has(ModItems.PHILOSOPHER_STONE_LARGE.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PHILOSOPHER_STONE_LARGE.get(), 9)
                .requires(ModBlocks.PHILOSOPHER_BLOCK.get())
                .unlockedBy("has_philosopher_block", has(ModBlocks.PHILOSOPHER_BLOCK.get()))
                .save(out, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "philosopher_stone_large_from_block"));

        // Mesa de Lapidação: R = redstone, F = barra de ferro, M = tábuas de madeira
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.LAPIDARY_TABLE.get())
                .pattern(" RF").pattern("MMM").pattern("M M")
                .define('R', Items.REDSTONE).define('F', Items.IRON_BARS).define('M', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE)).save(out);
    }
}

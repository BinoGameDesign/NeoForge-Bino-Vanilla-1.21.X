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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

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

        // ----- FUNDIÇÃO DO MINÉRIO -----

        // Os dois minérios (normal e deepslate) contam como o mesmo ingrediente
        Ingredient rubiOres = Ingredient.of(ModBlocks.RUBI_ORE.get(), ModBlocks.RUBI_DEEPSLATE_ORE.get());

        // Fornalha: 0.7 de XP por item, 200 ticks (10 segundos)
        SimpleCookingRecipeBuilder.smelting(rubiOres, RecipeCategory.MISC, ModItems.RUBI.get(), 0.7f, 200)
                .unlockedBy("has_rubi_ore", has(ModBlocks.RUBI_ORE.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "rubi_from_smelting"));

        // Alta-fornalha: mesma XP, metade do tempo (100 ticks)
        SimpleCookingRecipeBuilder.blasting(rubiOres, RecipeCategory.MISC, ModItems.RUBI.get(), 0.7f, 100)
                .unlockedBy("has_rubi_ore", has(ModBlocks.RUBI_ORE.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "rubi_from_blasting"));
    }
}
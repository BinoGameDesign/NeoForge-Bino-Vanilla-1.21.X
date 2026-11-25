package com.bino.binosmining.datagen;

import com.bino.binosmining.BinosMining;
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
import com.bino.binosmining.item.MiningItems;
import com.bino.binosmining.block.MiningBlocks;

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
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MiningItems.RUBI_SWORD.get())
                .pattern(" R ")
                .pattern(" R ")
                .pattern(" S ")
                .define('R', MiningItems.RUBI.get())            // liga a letra R ao item rubi
                .define('S', Items.STICK)                    // liga a letra S ao graveto
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get()))  // a receita aparece no livro quando você tem rubi
                .save(recipeOutput);                         // grava a receita

        // Picareta
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MiningItems.RUBI_PICKAXE.get())
                .pattern("RRR")
                .pattern(" S ")
                .pattern(" S ")
                .define('R', MiningItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get()))
                .save(recipeOutput);

        // Pá
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MiningItems.RUBI_SHOVEL.get())
                .pattern(" R ")
                .pattern(" S ")
                .pattern(" S ")
                .define('R', MiningItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get()))
                .save(recipeOutput);

        // Machado
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MiningItems.RUBI_AXE.get())
                .pattern("RR ")
                .pattern("RS ")
                .pattern(" S ")
                .define('R', MiningItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get()))
                .save(recipeOutput);

        // Enxada
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MiningItems.RUBI_HOE.get())
                .pattern("RR ")
                .pattern(" S ")
                .pattern(" S ")
                .define('R', MiningItems.RUBI.get())
                .define('S', Items.STICK)
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get()))
                .save(recipeOutput);

        // ----- BLOCO DE RUBI -----

        // 9 rubis (3x3) viram 1 bloco de rubi
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MiningBlocks.RUBI_BLOCK.get())
                .pattern("RRR")
                .pattern("RRR")
                .pattern("RRR")
                .define('R', MiningItems.RUBI.get())
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get()))
                .save(recipeOutput);


        // 1 bloco de rubi vira 9 rubis (sem formato: "shapeless" = ingredientes em qualquer lugar).
        // O nome explícito evita conflito com a receita da fornalha, que também gera o rubi.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MiningItems.RUBI.get(), 9)
                .requires(MiningBlocks.RUBI_BLOCK.get())
                .unlockedBy("has_rubi_block", has(MiningBlocks.RUBI_BLOCK.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "rubi_from_rubi_block"));


        // ===== Safira e Jade =====
        gemSet(recipeOutput, "sapphire", MiningItems.SAPPHIRE.get(), MiningItems.SAPPHIRE_SWORD.get(), MiningItems.SAPPHIRE_PICKAXE.get(),
                MiningItems.SAPPHIRE_SHOVEL.get(), MiningItems.SAPPHIRE_AXE.get(), MiningItems.SAPPHIRE_HOE.get(),
                MiningBlocks.SAPPHIRE_BLOCK.get(), MiningBlocks.SAPPHIRE_ORE.get(), MiningBlocks.SAPPHIRE_DEEPSLATE_ORE.get());
        gemSet(recipeOutput, "jade", MiningItems.JADE.get(), MiningItems.JADE_SWORD.get(), MiningItems.JADE_PICKAXE.get(),
                MiningItems.JADE_SHOVEL.get(), MiningItems.JADE_AXE.get(), MiningItems.JADE_HOE.get(),
                MiningBlocks.JADE_BLOCK.get(), MiningBlocks.JADE_ORE.get(), MiningBlocks.JADE_DEEPSLATE_ORE.get());

        // ===== Armaduras =====
        armorSet(recipeOutput, "rubi", MiningItems.RUBI.get(), MiningItems.RUBI_HELMET.get(), MiningItems.RUBI_CHESTPLATE.get(),
                MiningItems.RUBI_LEGGINGS.get(), MiningItems.RUBI_BOOTS.get());
        armorSet(recipeOutput, "sapphire", MiningItems.SAPPHIRE.get(), MiningItems.SAPPHIRE_HELMET.get(), MiningItems.SAPPHIRE_CHESTPLATE.get(),
                MiningItems.SAPPHIRE_LEGGINGS.get(), MiningItems.SAPPHIRE_BOOTS.get());
        armorSet(recipeOutput, "jade", MiningItems.JADE.get(), MiningItems.JADE_HELMET.get(), MiningItems.JADE_CHESTPLATE.get(),
                MiningItems.JADE_LEGGINGS.get(), MiningItems.JADE_BOOTS.get());

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
                .save(out, ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, name + "_from_" + name + "_block"));

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
            ItemLike from = MiningItems.FORGE_STONES.get(n - 1).get();
            ItemLike to = MiningItems.FORGE_STONES.get(n).get();
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, to).pattern("SS").pattern("SS")
                    .define('S', from).unlockedBy("has_forge_stone_" + n, has(from)).save(out);
        }

        // Fragmento de pedra filosofal = diamante + rubi + safira + jade (sem formato fixo)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MiningItems.PHILOSOPHER_FRAGMENT.get())
                .requires(Items.DIAMOND).requires(MiningItems.RUBI.get())
                .requires(MiningItems.SAPPHIRE.get()).requires(MiningItems.JADE.get())
                .unlockedBy("has_rubi", has(MiningItems.RUBI.get())).save(out);

        // 4 fragmentos -> incompleta; 4 incompletas -> pequena; 4 pequenas -> grande (todos em quadrado 2x2)
        square(out, MiningItems.PHILOSOPHER_FRAGMENT.get(), MiningItems.PHILOSOPHER_STONE_INCOMPLETE.get());
        square(out, MiningItems.PHILOSOPHER_STONE_INCOMPLETE.get(), MiningItems.PHILOSOPHER_STONE_SMALL.get());
        square(out, MiningItems.PHILOSOPHER_STONE_SMALL.get(), MiningItems.PHILOSOPHER_STONE_LARGE.get());

        // +6 = +5 + fragmento; +7 = +6 + incompleta; +8 = +7 + pequena; +9 = +8 + grande
        pair(out, 5, MiningItems.PHILOSOPHER_FRAGMENT.get(), 6);
        pair(out, 6, MiningItems.PHILOSOPHER_STONE_INCOMPLETE.get(), 7);
        pair(out, 7, MiningItems.PHILOSOPHER_STONE_SMALL.get(), 8);
        pair(out, 8, MiningItems.PHILOSOPHER_STONE_LARGE.get(), 9);

        // +10 = pedra +9 no centro e 8 pedras filosofais grandes ao redor (mesa de trabalho cheia)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MiningItems.FORGE_STONE_10.get())
                .pattern("LLL").pattern("LFL").pattern("LLL")
                .define('L', MiningItems.PHILOSOPHER_STONE_LARGE.get())
                .define('F', MiningItems.FORGE_STONE_9.get())
                .unlockedBy("has_forge_stone_9", has(MiningItems.FORGE_STONE_9.get())).save(out);
    }

    // 4 itens iguais em quadrado 2x2 viram 1 item do próximo tipo
    private void square(RecipeOutput out, ItemLike from, ItemLike to) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, to).pattern("SS").pattern("SS")
                .define('S', from).unlockedBy("has_" + "ingredient", has(from)).save(out);
    }

    // Pedra +fromLevel + um item filosofal -> pedra +toLevel (sem formato fixo)
    private void pair(RecipeOutput out, int fromLevel, ItemLike extra, int toLevel) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MiningItems.FORGE_STONES.get(toLevel - 1).get())
                .requires(MiningItems.FORGE_STONES.get(fromLevel - 1).get()).requires(extra)
                .unlockedBy("has_forge_stone_" + fromLevel, has(MiningItems.FORGE_STONES.get(fromLevel - 1).get())).save(out);
    }

    // Bloco de pedra filosofal (ida e volta) e a Mesa de Lapidação
    private void extraRecipes(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MiningBlocks.PHILOSOPHER_BLOCK.get())
                .pattern("LLL").pattern("LLL").pattern("LLL")
                .define('L', MiningItems.PHILOSOPHER_STONE_LARGE.get())
                .unlockedBy("has_philosopher_stone_large", has(MiningItems.PHILOSOPHER_STONE_LARGE.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MiningItems.PHILOSOPHER_STONE_LARGE.get(), 9)
                .requires(MiningBlocks.PHILOSOPHER_BLOCK.get())
                .unlockedBy("has_philosopher_block", has(MiningBlocks.PHILOSOPHER_BLOCK.get()))
                .save(out, ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "philosopher_stone_large_from_block"));

        // Mesa de Lapidação: R = redstone, F = barra de ferro, M = tábuas de madeira
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MiningBlocks.LAPIDARY_TABLE.get())
                .pattern(" RF").pattern("MMM").pattern("M M")
                .define('R', Items.REDSTONE).define('F', Items.IRON_BARS).define('M', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE)).save(out);
    }
}

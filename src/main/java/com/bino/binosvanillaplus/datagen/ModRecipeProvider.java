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

        // Fundição dos dois minérios
        Ingredient ores = Ingredient.of(ore, deepslateOre);
        SimpleCookingRecipeBuilder.smelting(ores, RecipeCategory.MISC, gem, 0.7f, 200)
                .unlockedBy("has_" + name + "_ore", has(ore))
                .save(out, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name + "_from_smelting"));
        SimpleCookingRecipeBuilder.blasting(ores, RecipeCategory.MISC, gem, 0.7f, 100)
                .unlockedBy("has_" + name + "_ore", has(ore))
                .save(out, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, name + "_from_blasting"));
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
}

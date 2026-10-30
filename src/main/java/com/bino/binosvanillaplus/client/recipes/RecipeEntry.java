package com.bino.binosvanillaplus.client.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Uma receita ja "traduzida" para o que o navegador desenha.
//  inputs: um item de lista por espaco (cada espaco tem alternativas, por exemplo tags); lista vazia = espaco vazio.
//  CRAFTING: 9 espacos (grade 3x3); as outras categorias: os espacos em fila.
public record RecipeEntry(RecipeCategory category, @Nullable RecipeHolder<?> holder, List<List<ItemStack>> inputs,
                          ItemStack output, int cookTime, float xp) {
}

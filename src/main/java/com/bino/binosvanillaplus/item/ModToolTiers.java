package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.util.ModTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
    // ===== Balanceamento (status brutos, antes da forja) =====
    // Ordem final: Diamante (com o bonus de +25%) > Jade > Safira > Rubi. Diamante vanilla sem bonus: 1561 / 8 / 3 / 10.
    //   Diamante com o bonus (DiamondBuffEvents): 1951 / 10 / x1,25 no dano e na velocidade de ataque.
    //   Jade   : os melhores status entre as gemas do mod (e +50% de XP, o que paga o custo de XP da forja).
    //   Safira : igual ao diamante sem bonus (e 1/3 de chance de +1 drop).
    //   Rubi   : o mais fraco em status; compensa com as habilidades de area (veio, arvore, 3x3, colheita).
    // BlockTags.INCORRECT_FOR_DIAMOND_TOOL = minera tudo que o diamante minera.
    // Formato: (blocos que nao dropam, durabilidade, velocidade de mineracao, bonus de dano, encantabilidade, material de conserto)
    public static final Tier RUBI = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_RUBI_TOOL,
            1000, 7.5f, 2.5f, 8,() -> Ingredient.of(ModItems.RUBI));

    public static final Tier SAPPHIRE = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1561, 8.0f, 3.0f, 10, () -> Ingredient.of(ModItems.SAPPHIRE));

    public static final Tier JADE = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1800, 9.0f, 3.5f, 12, () -> Ingredient.of(ModItems.JADE));

}

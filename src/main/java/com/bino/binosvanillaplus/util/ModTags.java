package com.bino.binosvanillaplus.util;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class ModTags {
    public static class Items {
        // minerios aceitos no upgrade da Chave Dimensional (editavel por datapack)
        public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> KEY_ORES = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "dimensional_key_ores"));
    }
}

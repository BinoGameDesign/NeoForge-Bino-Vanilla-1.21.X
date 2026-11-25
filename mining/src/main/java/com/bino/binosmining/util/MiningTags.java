package com.bino.binosmining.util;

import com.bino.binosmining.BinosMining;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class MiningTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_RUBI_TOOL = createTag("needs_rubi_tool");
        public static final TagKey<Block> INCORRECT_FOR_RUBI_TOOL = createTag("incorrect_for_rubi_tool");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, name));
        }
    }
}

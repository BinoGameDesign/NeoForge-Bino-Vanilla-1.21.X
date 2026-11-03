package com.bino.binosvanillaplus.recipe;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BinosVanillaMod.MOD_ID);

    public static final Supplier<RecipeSerializer<KeyUpgradeRecipe>> KEY_UPGRADE = SERIALIZERS.register("key_upgrade",
            () -> new SimpleCraftingRecipeSerializer<>(KeyUpgradeRecipe::new));

    public static void register(IEventBus eventBus) { SERIALIZERS.register(eventBus); }
}

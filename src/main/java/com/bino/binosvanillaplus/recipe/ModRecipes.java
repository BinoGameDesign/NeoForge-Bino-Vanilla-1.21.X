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

    public static final Supplier<RecipeSerializer<HelmetLampRecipe>> HELMET_LAMP = SERIALIZERS.register("helmet_lamp",
            () -> new SimpleCraftingRecipeSerializer<>(HelmetLampRecipe::new));

    // passar agua/pocoes entre garrafas e frascos (ver thirst/Vessels)
    public static final Supplier<RecipeSerializer<VesselPourRecipe>> VESSEL_POUR = SERIALIZERS.register("vessel_pour",
            () -> new SimpleCraftingRecipeSerializer<>(VesselPourRecipe::new));

    // forro da armadura (temperatura)
    public static final Supplier<RecipeSerializer<LiningRecipe>> LINING = SERIALIZERS.register("lining",
            () -> new SimpleCraftingRecipeSerializer<>(LiningRecipe::new));

    public static void register(IEventBus eventBus) { SERIALIZERS.register(eventBus); }
}

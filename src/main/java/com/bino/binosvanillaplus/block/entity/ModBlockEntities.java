package com.bino.binosvanillaplus.block.entity;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BinosVanillaMod.MOD_ID);

    public static final Supplier<BlockEntityType<RopeBlockEntity>> ROPE = BLOCK_ENTITIES.register("rope",
            () -> BlockEntityType.Builder.of(RopeBlockEntity::new, ModBlocks.ROPE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

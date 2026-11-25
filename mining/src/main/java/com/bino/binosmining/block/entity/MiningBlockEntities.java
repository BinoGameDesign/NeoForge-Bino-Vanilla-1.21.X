package com.bino.binosmining.block.entity;

import com.bino.binosmining.BinosMining;
import com.bino.binosmining.block.MiningBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MiningBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BinosMining.MOD_ID);

    public static final Supplier<BlockEntityType<LapidaryTableBlockEntity>> LAPIDARY_TABLE = BLOCK_ENTITIES.register("lapidary_table",
            () -> BlockEntityType.Builder.of(LapidaryTableBlockEntity::new, MiningBlocks.LAPIDARY_TABLE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

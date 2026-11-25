package com.bino.binosmining.item;

import com.bino.binosmining.BinosMining;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Componentes de item do Mining: nivel de forja (+1 a +10)
public class MiningComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BinosMining.MOD_ID);

    // persistent = é salvo no mundo; networkSynchronized = o servidor envia ao jogador (para a dica do item)
    public static final Supplier<DataComponentType<Integer>> FORGE_LEVEL = COMPONENTS.register("forge_level",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}

package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// "Componentes" são dados extras que um item carrega. Aqui: o nível de forja (+1 a +10) de um equipamento.
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BinosVanillaMod.MOD_ID);

    // persistent = é salvo no mundo; networkSynchronized = o servidor envia ao jogador (para a dica do item)
    public static final Supplier<DataComponentType<Integer>> FORGE_LEVEL = COMPONENTS.register("forge_level",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // Chave Dimensional: nivel (upgrades recebidos) e minerios ja usados (nao podem repetir)
    public static final Supplier<DataComponentType<Integer>> KEY_LEVEL = COMPONENTS.register("key_level",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final Supplier<DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>> KEY_ORES =
            COMPONENTS.register("key_ores",
                    () -> DataComponentType.<java.util.List<net.minecraft.resources.ResourceLocation>>builder()
                            .persistent(net.minecraft.resources.ResourceLocation.CODEC.listOf())
                            .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build());

    // Lanterna: ligada/desligada (na lanterna e no capacete com lanterna) e se o capacete tem a lanterna acoplada
    public static final Supplier<DataComponentType<Boolean>> FLASHLIGHT_ON = COMPONENTS.register("flashlight_on",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final Supplier<DataComponentType<Boolean>> HELMET_LAMP = COMPONENTS.register("helmet_lamp",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}

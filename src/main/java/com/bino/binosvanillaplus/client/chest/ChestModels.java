package com.bino.binosvanillaplus.client.chest;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.function.Supplier;

// Os baus do Bino (tools/blockbench/chests/*.bbmodel -> *ModelData / *Animations): camada do modelo, textura e
// animacoes de abrir/fechar e a de agua (ChestWater) de cada versao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ChestModels {
    private ChestModels() {}

    public enum Kind {
        NORMAL("chest", ChestModelData::createBodyLayer, ChestAnimations.OPEN, ChestAnimations.CLOSE, ChestAnimations.WATER),
        TRAPPED("trapped_chest", TrappedChestModelData::createBodyLayer, TrappedChestAnimations.OPEN, TrappedChestAnimations.CLOSE, TrappedChestAnimations.WATER),
        CHRISTMAS("christmas_chest", ChristmasChestModelData::createBodyLayer, ChristmasChestAnimations.OPEN, ChristmasChestAnimations.CLOSE, ChristmasChestAnimations.WATER),
        ENDER("ender_chest", EnderChestModelData::createBodyLayer, EnderChestAnimations.OPEN, EnderChestAnimations.CLOSE, EnderChestAnimations.WATER),
        NORMAL_DOUBLE("chest_double", ChestDoubleModelData::createBodyLayer, ChestDoubleAnimations.OPEN, ChestDoubleAnimations.CLOSE, ChestDoubleAnimations.WATER),
        TRAPPED_DOUBLE("trapped_chest_double", TrappedChestDoubleModelData::createBodyLayer, TrappedChestDoubleAnimations.OPEN, TrappedChestDoubleAnimations.CLOSE, TrappedChestDoubleAnimations.WATER),
        CHRISTMAS_DOUBLE("christmas_chest_double", ChristmasChestDoubleModelData::createBodyLayer, ChristmasChestDoubleAnimations.OPEN, ChristmasChestDoubleAnimations.CLOSE, ChristmasChestDoubleAnimations.WATER);

        public final ModelLayerLocation layer;
        public final ResourceLocation texture;
        public final Supplier<LayerDefinition> mesh;
        public final AnimationDefinition open, close, water;

        Kind(String name, Supplier<LayerDefinition> mesh, AnimationDefinition open, AnimationDefinition close, AnimationDefinition water) {
            this.layer = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "chest/" + name), "main");
            this.texture = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/entity/chest_bino/" + name + ".png");
            this.mesh = mesh; this.open = open; this.close = close; this.water = water;
        }

        public Kind doubled() {
            return switch (this) { case NORMAL -> NORMAL_DOUBLE; case TRAPPED -> TRAPPED_DOUBLE; case CHRISTMAS -> CHRISTMAS_DOUBLE; default -> this; };
        }
    }

    public static final ModelLayerLocation MIMIC = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "chest/mimic"), "main");
    public static final ModelLayerLocation MIMIC_DOUBLE = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "chest/mimic_double"), "main");

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (Kind k : Kind.values()) e.registerLayerDefinition(k.layer, k.mesh);
        e.registerLayerDefinition(MIMIC, MimicChestModelData::createBodyLayer);
        e.registerLayerDefinition(MIMIC_DOUBLE, MimicChestDoubleModelData::createBodyLayer);
    }
}

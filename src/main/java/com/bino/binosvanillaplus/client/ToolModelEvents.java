package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

// Liga o modelo 3D das ferramentas de rubi (models/item/<nome>_3d.json) ao item (que continua com o icone 2D)
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class ToolModelEvents {

    private static final List<String> TOOLS = List.of("rubi_sword", "rubi_pickaxe", "rubi_shovel", "rubi_axe", "rubi_hoe", "cyalume_stick", "climbing_hook");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, path);
    }

    private static ModelResourceLocation solidKey(String tool) {
        return ModelResourceLocation.standalone(id("item/" + tool + "_3d"));
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (String tool : TOOLS) event.register(solidKey(tool));
    }

    @SubscribeEvent
    public static void onModifyBaking(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (String tool : TOOLS) {
            ModelResourceLocation itemKey = ModelResourceLocation.inventory(id(tool));
            BakedModel flat = models.get(itemKey);
            BakedModel solid = models.get(solidKey(tool));
            if (flat == null || solid == null) continue;   // algo faltando: o item continua 2D
            models.put(itemKey, new HeldToolModel(flat, solid));
        }
    }
}

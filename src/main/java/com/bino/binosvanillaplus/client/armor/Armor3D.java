package com.bino.binosvanillaplus.client.armor;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// Armaduras com modelo 3D (feitas no Blockbench e convertidas por tools/bbmodel_armor.py).
// O jogo desenha a peca com o modelo daqui no lugar da caixa normal da armadura; mexe junto com o corpo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Armor3D {
    private Armor3D() {}

    private record Entry(Supplier<? extends Item> item, ModelLayerLocation layer, Supplier<LayerDefinition> mesh) {}

    private static final Map<Item, HumanoidModel<LivingEntity>> MODELS = new HashMap<>();

    private static Entry entry(Supplier<? extends Item> item, String name, Supplier<LayerDefinition> mesh) {
        return new Entry(item, new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "armor/" + name), "main"), mesh);
    }

    private static final Entry[] ENTRIES = {
            entry(ModItems.JADE_HELMET, "jade_helmet", Armor3DMeshes::jadeHelmet),
            entry(ModItems.JADE_CHESTPLATE, "jade_chestplate", Armor3DMeshes::jadeChestplate),
    };

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (Entry en : ENTRIES) e.registerLayerDefinition(en.layer(), en.mesh());
    }

    // os modelos sao montados de novo quando os recursos recarregam
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers e) {
        MODELS.clear();
        for (Entry en : ENTRIES) MODELS.put(en.item().get(), new HumanoidModel<>(e.getEntityModels().bakeLayer(en.layer())));
    }

    // modelo 3D da peca (ou null se ela usa a armadura normal)
    public static HumanoidModel<LivingEntity> model(ItemStack stack) {
        if (MODELS.isEmpty()) {
            var set = Minecraft.getInstance().getEntityModels();
            for (Entry en : ENTRIES) MODELS.put(en.item().get(), new HumanoidModel<>(set.bakeLayer(en.layer())));
        }
        return MODELS.get(stack.getItem());
    }

    @SubscribeEvent
    public static void onExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                HumanoidModel<LivingEntity> m = model(stack);
                return m != null ? m : original;
            }
        };
        Item[] items = new Item[ENTRIES.length];
        for (int i = 0; i < ENTRIES.length; i++) items[i] = ENTRIES[i].item().get();
        e.registerItem(ext, items);
    }
}

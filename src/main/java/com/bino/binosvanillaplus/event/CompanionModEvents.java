package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class CompanionModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPANION_ALLAY.get(),
                Allay.createAttributes()                 // parte dos atributos do Allay normal
                        .add(Attributes.FLYING_SPEED, 0.15)   // 0,1 do Allay normal x 1,5 = 50% mais rápido
                        .add(Attributes.SCALE, 0.7)           // 30% menor
                        .build());
    }
}
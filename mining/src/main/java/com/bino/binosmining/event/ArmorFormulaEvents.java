package com.bino.binosmining.event;

import com.bino.binosmining.BinosMining;
import com.bino.binosmining.util.ArmorModel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

// Troca a reducao de armadura do vanilla (teto de 20 pontos) pela curva do ArmorModel.
// So mexe quando o vanilla ja aplicou armadura (golpes que ignoram armadura ficam intactos).
@EventBusSubscriber(modid = BinosMining.MOD_ID)
public class ArmorFormulaEvents {
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        DamageContainer c = event.getContainer();
        if (c.getReduction(DamageContainer.Reduction.ARMOR) <= 0f) return;
        var victim = event.getEntity();
        float r = ArmorModel.reduction((float) victim.getAttributeValue(Attributes.ARMOR),
                (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS), ArmorModel.attackerLevel(event.getSource()));
        c.setReduction(DamageContainer.Reduction.ARMOR, c.getOriginalDamage() * r);
    }
}

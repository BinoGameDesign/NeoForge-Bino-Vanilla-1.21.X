package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.util.ArmorModel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

// Teto de hit para quem veste o set mais forte (ver ArmorModel). Roda DEPOIS de armadura/encantos/efeitos,
// entao vale com todos os buffs. O ataque especial do boss final usa a tag binosvanillaplus:boss_special para ignorar.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class ArmorHitCapEvents {
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        float cap = ArmorModel.hitCap((float) player.getAttributeValue(Attributes.ARMOR), player.getMaxHealth(), event.getSource());
        if (cap > 0 && event.getNewDamage() > cap) event.setNewDamage(cap);
    }
}

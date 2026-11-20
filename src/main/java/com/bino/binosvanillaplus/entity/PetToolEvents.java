package com.bino.binosvanillaplus.entity;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

// Totem entregue ao pet: quando o dono ia morrer (e nao tinha totem nas maos), o pet usa o totem e salva ele.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class PetToolEvents {
    private PetToolEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        CompanionAllayEntity pet = CompanionAllayManager.getPet(p);
        if (pet != null && pet.saveOwner(p)) e.setCanceled(true);
    }
}

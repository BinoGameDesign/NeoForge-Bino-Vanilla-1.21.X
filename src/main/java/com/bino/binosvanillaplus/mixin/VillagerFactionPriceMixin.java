package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Errante (sem faccao) paga 30% a mais nas trocas com villagers
@Mixin(Villager.class)
public abstract class VillagerFactionPriceMixin {
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"), require = 0)
    private void binosvanillaplus$wandererPrices(Player player, CallbackInfo ci) {
        if (FactionData.get(player) != Faction.WANDERER) return;
        for (MerchantOffer o : ((Villager) (Object) this).getOffers())
            o.addToSpecialPriceDiff(Math.max(1, Mth.ceil(o.getBaseCostA().getCount() * 0.3f)));
    }
}

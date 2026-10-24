package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.custom.ForgeStoneItem;
import com.bino.binosvanillaplus.util.ForgeUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

// Sistema de forja: melhora de equipamentos na bigorna e efeitos do nível
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class ForgeEvents {

    // Custo em níveis de experiência para forjar na bigorna (precisa ser pelo menos 1, senão a bigorna não libera o item)
    private static final int ANVIL_XP_COST = 1;

    // ---------- 1) BIGORNA: equipamento + pedra de forja ----------
    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!(right.getItem() instanceof ForgeStoneItem stone)) return;   // o 2º slot precisa ser uma pedra de forja
        if (!ForgeUtil.canForge(left)) return;                            // o 1º slot precisa ser um equipamento

        // A pedra +N só funciona em equipamento +(N-1). Exemplo: pedra +3 em item +2.
        if (stone.getLevel() != ForgeUtil.getLevel(left) + 1) return;

        ItemStack result = left.copy();
        ForgeUtil.setLevel(result, stone.getLevel());

        // Se o jogador digitou um nome novo na bigorna, mantém o nome dele
        String typed = event.getName();
        if (typed != null && !typed.isBlank() && !typed.equals(left.getHoverName().getString())) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(typed));
        }

        event.setOutput(result);
        event.setCost(ANVIL_XP_COST);
        event.setMaterialCost(1);     // gasta 1 pedra
    }

    // ---------- 2) ATRIBUTOS +10% por nível (dano, velocidade de ataque, armadura, resistência...) ----------
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        int level = ForgeUtil.getLevel(event.getItemStack());
        if (level <= 0) return;
        double mult = ForgeUtil.multiplier(level);

        List<ItemAttributeModifiers.Entry> entries = new ArrayList<>(event.getModifiers());
        for (ItemAttributeModifiers.Entry entry : entries) {
            AttributeModifier old = entry.modifier();
            if (old.operation() != AttributeModifier.Operation.ADD_VALUE) continue;

            // Base do jogador: dano 1 e velocidade de ataque 4. A conta é feita sobre o valor FINAL
            // (igual ao bônus do diamante) e depois tira-se a base de novo.
            double base = entry.attribute().equals(Attributes.ATTACK_DAMAGE) ? 1.0
                    : entry.attribute().equals(Attributes.ATTACK_SPEED) ? 4.0 : 0.0;
            if (base == 0.0 && old.amount() <= 0) continue;   // só melhora atributos positivos

            double newAmount = (base + old.amount()) * mult - base;
            event.replaceModifier(entry.attribute(),
                    new AttributeModifier(old.id(), newAmount, old.operation()), entry.slot());
        }
    }

    // ---------- 3) VELOCIDADE DE MINERAÇÃO +10% por nível ----------
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack held = event.getEntity().getMainHandItem();
        int level = ForgeUtil.getLevel(held);
        if (level <= 0 || held.getDestroySpeed(event.getState()) <= 1.0F) return;
        event.setNewSpeed((float) (event.getNewSpeed() * ForgeUtil.multiplier(level)));
    }
}

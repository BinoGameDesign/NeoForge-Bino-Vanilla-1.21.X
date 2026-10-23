package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModToolTiers;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

// Habilidades das ferramentas de Safira e Jade
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class GemAbilityEvents {

    // ====== VALORES QUE VOCÊ PODE AJUSTAR ======
    private static final int SAPPHIRE_EXTRA_DROP_CHANCE = 3; // 1 em 3 = 1/3 de chance
    private static final double JADE_XP_MULTIPLIER = 1.25;   // +25% de experiência

    // Diz se o item é uma espada/ferramenta feita com o material (tier) indicado
    private static boolean isMadeOf(ItemStack stack, Tier tier) {
        return stack.getItem() instanceof TieredItem tiered && tiered.getTier() == tier;
    }

    // Arredonda "no sorteio": 3 de XP x 1.25 = 3.75 -> vira 4 em 75% das vezes e 3 em 25%.
    // Sem isso, valores pequenos nunca receberiam o bônus.
    private static int boostXp(int xp, RandomSource random) {
        double boosted = xp * JADE_XP_MULTIPLIER;
        int whole = (int) boosted;
        return whole + (random.nextDouble() < boosted - whole ? 1 : 0);
    }

    // Roda quando um bloco quebrado vai soltar seus itens e sua experiência
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool(); // a ferramenta usada para quebrar

        // ----- SAFIRA: 1/3 de chance de +1 recurso -----
        if (isMadeOf(tool, ModToolTiers.SAPPHIRE) && !event.getDrops().isEmpty()
                && event.getLevel().random.nextInt(SAPPHIRE_EXTRA_DROP_CHANCE) == 0) {
            // Aumenta em 1 a quantidade do primeiro item que o bloco soltaria
            ItemEntity first = event.getDrops().get(0);
            first.setItem(first.getItem().copyWithCount(first.getItem().getCount() + 1));
        }

        // ----- JADE: +25% de experiência dos blocos (minérios) -----
        if (isMadeOf(tool, ModToolTiers.JADE) && event.getDroppedExperience() > 0) {
            event.setDroppedExperience(boostXp(event.getDroppedExperience(), event.getLevel().random));
        }
    }

    // Roda quando um mob morre e vai soltar experiência.
    // Prioridade ALTA: roda ANTES do pet recolher a XP (que zera o valor), senão o bônus de jade se perderia.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMobXp(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) return;

        // Jade também vale para a espada: matar mobs com ela dá +25% de XP
        if (isMadeOf(player.getMainHandItem(), ModToolTiers.JADE) && event.getDroppedExperience() > 0) {
            event.setDroppedExperience(boostXp(event.getDroppedExperience(), player.getRandom()));
        }
    }
}

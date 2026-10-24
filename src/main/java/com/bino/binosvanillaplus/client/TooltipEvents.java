package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.event.DiamondBuffEvents;
import com.bino.binosvanillaplus.event.GemAbilityEvents;
import com.bino.binosvanillaplus.event.GemArmorEvents;
import com.bino.binosvanillaplus.item.ModArmorMaterials;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Set;

// Escreve nas dicas dos itens (ao passar o mouse) o que cada um tem de especial.
// Os textos ficam no arquivo de idioma (lang/pt_br.json), nas chaves "tooltip.binosvanillaplus...".
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class TooltipEvents {

    private static final ResourceLocation DIAMOND_MATERIAL = ResourceLocation.withDefaultNamespace("diamond");

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        List<Component> tip = event.getToolTip();
        Player player = event.getEntity();   // pode ser nulo (por exemplo, no menu); aí contamos 0 peças

        // ----- Ferramentas e espada de DIAMANTE -----
        if (Set.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE).contains(item)) {
            ability(tip, "diamond_tool", percent(DiamondBuffEvents.TOOL_MULTIPLIER - 1));
        }

        // ----- Ferramentas de RUBI (cada uma tem a sua habilidade) -----
        if (item == ModItems.RUBI_SWORD.get()) ability(tip, "rubi_sword");
        if (item == ModItems.RUBI_PICKAXE.get()) ability(tip, "rubi_pickaxe");
        if (item == ModItems.RUBI_AXE.get()) ability(tip, "rubi_axe");
        if (item == ModItems.RUBI_SHOVEL.get()) ability(tip, "rubi_shovel");
        if (item == ModItems.RUBI_HOE.get()) ability(tip, "rubi_hoe");

        // ----- Safira e Jade: valem para espada e ferramentas -----
        if (item instanceof TieredItem tiered) {
            if (tiered.getTier() == ModToolTiers.SAPPHIRE) {
                ability(tip, "sapphire_tool", String.valueOf(GemAbilityEvents.SAPPHIRE_EXTRA_DROP_CHANCE));
            }
            if (tiered.getTier() == ModToolTiers.JADE) {
                ability(tip, "jade_tool", percent(GemAbilityEvents.JADE_XP_MULTIPLIER - 1));
            }
        }

        // ----- ARMADURAS: mostra "x/4" e o bônus do set (verde quando completo) -----
        if (item instanceof ArmorItem armor) {
            if (armor.getMaterial().is(DIAMOND_MATERIAL)) {
                int count = countDiamond(player);
                setLine(tip, "diamond", count, "diamond_set", percent(DiamondBuffEvents.SET_SPEED_BONUS));
            } else if (armor.getMaterial().is(ModArmorMaterials.RUBI.getId())) {
                int count = player == null ? 0 : GemArmorEvents.countPieces(player, ModArmorMaterials.RUBI);
                setLine(tip, "rubi", count, "rubi_set", percent(GemArmorEvents.RUBI_DODGE_CHANCE));
            } else if (armor.getMaterial().is(ModArmorMaterials.SAPPHIRE.getId())) {
                int count = player == null ? 0 : GemArmorEvents.countPieces(player, ModArmorMaterials.SAPPHIRE);
                setLine(tip, "sapphire", count, "sapphire_set", String.valueOf((int) GemArmorEvents.SAPPHIRE_MAGNET_RANGE));
            } else if (armor.getMaterial().is(ModArmorMaterials.JADE.getId())) {
                int count = player == null ? 0 : GemArmorEvents.countPieces(player, ModArmorMaterials.JADE);
                setLine(tip, "jade", count, "jade_set", percent(1 - GemArmorEvents.JADE_FALL_MULTIPLIER));
            }
        }
    }

    // Uma linha dourada com a habilidade da ferramenta
    private static void ability(List<Component> tip, String key, String... args) {
        tip.add(Component.translatable("tooltip.binosvanillaplus." + key, (Object[]) args)
                .withStyle(ChatFormatting.GOLD));
    }

    // Duas linhas para armaduras: "Set de X (2/4)" e o bônus. Fica verde com as 4 peças, cinza antes disso.
    private static void setLine(List<Component> tip, String gem, int count, String bonusKey, String arg) {
        ChatFormatting color = count >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
        tip.add(Component.translatable("tooltip.binosvanillaplus.set_header",
                Component.translatable("tooltip.binosvanillaplus.name_" + gem), count).withStyle(color));
        tip.add(Component.translatable("tooltip.binosvanillaplus." + bonusKey, arg).withStyle(color));
    }

    // Conta as peças de diamante vestidas
    private static int countDiamond(Player player) {
        if (player == null) return 0;
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).getItem() instanceof ArmorItem a && a.getMaterial().is(DIAMOND_MATERIAL)) count++;
        }
        return count;
    }

    // 0.25 -> "25"
    private static String percent(double fraction) {
        return String.valueOf(Math.round(fraction * 100));
    }
}

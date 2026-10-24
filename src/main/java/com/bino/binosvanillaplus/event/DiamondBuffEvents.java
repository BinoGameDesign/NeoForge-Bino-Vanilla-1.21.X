package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

// Registra automaticamente os métodos @SubscribeEvent desta classe no barramento do jogo
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class DiamondBuffEvents {

    // ====== VALORES QUE VOCÊ PODE AJUSTAR ======
    // Multiplicador de todos os status das ferramentas e da espada de diamante (1.25 = +25%)
    public static final double TOOL_MULTIPLIER = 1.25;
    // Bônus de velocidade do set completo (0.15 = +15% de velocidade de movimento)
    public static final double SET_SPEED_BONUS = 0.15;
    // Durabilidade original de qualquer ferramenta de diamante no vanilla
    private static final int DIAMOND_BASE_DURABILITY = 1561;

    // Identificador único do bônus do set (o jogo usa isso para achar e remover o bônus depois)
    private static final ResourceLocation SET_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "diamond_set_speed");

    // Valores base do jogador, usados na conta do multiplicador (dano base 1, velocidade de ataque base 4)
    private static final double PLAYER_BASE_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    // Diz se o item é espada ou ferramenta de diamante
    private static boolean isDiamondTool(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.DIAMOND_SWORD || item == Items.DIAMOND_PICKAXE
                || item == Items.DIAMOND_AXE || item == Items.DIAMOND_SHOVEL
                || item == Items.DIAMOND_HOE;
    }

    // ---------- 1) DURABILIDADE x1.25 ----------
    // Este método NÃO tem @SubscribeEvent: ele é registrado no barramento do mod, dentro do construtor
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        int newDurability = (int) (DIAMOND_BASE_DURABILITY * TOOL_MULTIPLIER); // 1561 -> 1951
        for (Item item : new Item[]{Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
                Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE}) {
            event.modify(item, builder -> builder.set(DataComponents.MAX_DAMAGE, newDurability));
        }
    }

    // ---------- 2) DANO E VELOCIDADE DE ATAQUE x1.25 ----------
    // Roda sempre que o jogo pergunta os atributos de um item (inclusive para mostrar a dica do item)
    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (!isDiamondTool(event.getItemStack())) return;

        // Copia a lista antes de mexer, para não alterar enquanto percorre
        List<ItemAttributeModifiers.Entry> entries = new ArrayList<>(event.getModifiers());
        for (ItemAttributeModifiers.Entry entry : entries) {
            AttributeModifier old = entry.modifier();

            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                // Valor final do jogador = base + bônus do item. Multiplicamos o TOTAL por 1.25
                // e depois tiramos a base de novo para achar o novo bônus do item.
                double total = (PLAYER_BASE_DAMAGE + old.amount()) * TOOL_MULTIPLIER;
                event.replaceModifier(entry.attribute(),
                        new AttributeModifier(old.id(), total - PLAYER_BASE_DAMAGE, old.operation()),
                        entry.slot());

            } else if (entry.attribute().equals(Attributes.ATTACK_SPEED)) {
                // Mesma conta para a velocidade de ataque
                double total = (PLAYER_BASE_ATTACK_SPEED + old.amount()) * TOOL_MULTIPLIER;
                event.replaceModifier(entry.attribute(),
                        new AttributeModifier(old.id(), total - PLAYER_BASE_ATTACK_SPEED, old.operation()),
                        entry.slot());
            }
        }
    }

    // ---------- 3) VELOCIDADE DE MINERAÇÃO x1.25 ----------
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack held = event.getEntity().getMainHandItem();
        if (!isDiamondTool(held)) return;

        // Só multiplica se a ferramenta é realmente eficaz no bloco (velocidade maior que 1)
        if (held.getDestroySpeed(event.getState()) <= 1.0F) return;

        // Multiplica o valor atual, então combina com Pressa, Eficiência etc.
        event.setNewSpeed((float) (event.getNewSpeed() * TOOL_MULTIPLIER));
    }

    // ---------- 4) BÔNUS DE VELOCIDADE DO SET COMPLETO ----------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return; // só o servidor decide (o valor é sincronizado com o cliente)

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean fullSet = player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS);

        boolean hasBonus = speed.getModifier(SET_SPEED_ID) != null;

        if (fullSet && !hasBonus) {
            // ADD_MULTIPLIED_TOTAL é a mesma operação da poção de Velocidade e do sprint:
            // cada uma multiplica o valor atual, então todas se acumulam (combam) entre si.
            // "Transient" = não é salvo no arquivo do jogador, é recriado a cada login.
            speed.addTransientModifier(new AttributeModifier(
                    SET_SPEED_ID, SET_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!fullSet && hasBonus) {
            speed.removeModifier(SET_SPEED_ID); // tirou uma peça: remove o bônus
        }
    }
}
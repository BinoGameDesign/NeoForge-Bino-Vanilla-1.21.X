package com.bino.binosvanillaplus.thirst;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.function.Supplier;

// Frasco cheio (agua ou pocao), bebido de uma vez. Vale "mult" garrafas: a pocao dura mult vezes o tempo (e a cura ou
// o dano instantaneo tambem multiplicam); a agua mata mult vezes a sede de uma garrafa. Volta o frasco vazio.
public class FilledVesselItem extends Item {
    public final int size, drinkTicks;
    public final float mult;
    private final Supplier<? extends Item> empty;

    public FilledVesselItem(int size, float mult, int drinkTicks, Supplier<? extends Item> empty, Properties props) {
        super(props);
        this.size = size; this.mult = mult; this.drinkTicks = drinkTicks; this.empty = empty;
    }

    public Item empty() { return empty.get(); }
    /** quantas garrafas cabem (o frasquinho e' meia: 2 frasquinhos = 1 garrafa) */
    public int bottles() { return Math.max(1, Math.round(mult)); }

    @Override
    public ItemStack getDefaultInstance() {
        return Vessels.filled(this, new PotionContents(net.minecraft.world.item.alchemy.Potions.WATER), false);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(), Vessels.contentsName(Vessels.contents(stack), Vessels.boiled(stack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        PotionContents pc = Vessels.contents(stack);
        boolean boiled = Vessels.boiled(stack);
        Player player = entity instanceof Player p ? p : null;
        if (entity instanceof ServerPlayer sp) {
            CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
            if (Vessels.isWater(pc)) {
                Thirst.add(sp, Thirst.BOTTLE * mult);
                if (!boiled) Thirst.dirty(sp, size == 1 ? 0.25f : Thirst.BOTTLE_NAUSEA);
            } else {
                Thirst.add(sp, 4f * mult);
            }
        }
        if (!level.isClientSide() && !Vessels.isWater(pc)) {
            float k = mult;
            pc.forEachEffect(e -> {
                if (e.getEffect().value().isInstantenous())
                    e.getEffect().value().applyInstantenousEffect(player, player, entity, e.getAmplifier(), k);
                else
                    entity.addEffect(k == 1f ? e : new MobEffectInstance(e.getEffect(), Math.max(1, (int) (e.getDuration() * k)),
                            e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
            });
        }
        if (player != null) player.awardStat(Stats.ITEM_USED.get(this));
        entity.gameEvent(GameEvent.DRINK);
        if (player != null && player.hasInfiniteMaterials()) return stack;
        stack.shrink(1);
        ItemStack emptyVessel = new ItemStack(empty());
        if (stack.isEmpty()) return emptyVessel;
        if (player != null && !player.getInventory().add(emptyVessel)) player.drop(emptyVessel, false);
        return stack;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return drinkTicks; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public SoundEvent getDrinkingSound() { return SoundEvents.GENERIC_DRINK; }
    @Override public SoundEvent getEatingSound() { return SoundEvents.GENERIC_DRINK; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        PotionContents pc = Vessels.contents(stack);
        tip.add(Component.translatable(size == 1 ? "tooltip.binosvanillaplus.vessel.half" : "tooltip.binosvanillaplus.vessel.mult",
                bottles()).withStyle(ChatFormatting.GRAY));
        if (Vessels.isWater(pc)) {
            if (Vessels.boiled(stack)) tip.add(Component.translatable("tooltip.binosvanillaplus.water.boiled").withStyle(ChatFormatting.AQUA));
            else {
                tip.add(Component.translatable("tooltip.binosvanillaplus.water.dirty").withStyle(ChatFormatting.GOLD));
                tip.add(Component.translatable("tooltip.binosvanillaplus.water.boil").withStyle(ChatFormatting.GRAY));
            }
        } else {
            pc.addPotionTooltip(tip::add, mult, ctx.tickRate());
        }
    }
}

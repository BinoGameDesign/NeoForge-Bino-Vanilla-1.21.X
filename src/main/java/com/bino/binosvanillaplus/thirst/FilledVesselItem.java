package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.Supplier;

// Frasco cheio (agua ou pocao). Cada gole gasta 1 dose; na ultima volta o frasco vazio. O Frasquinho (tamanho 1)
// e' meia dose: bebe rapido, mas a agua mata metade da sede e a pocao vale metade (tempo e cura/dano instantaneo).
public class FilledVesselItem extends Item {
    public final int size, maxDoses, drinkTicks;
    private final Supplier<? extends Item> empty;

    public FilledVesselItem(int size, int maxDoses, int drinkTicks, Supplier<? extends Item> empty, Properties props) {
        super(props);
        this.size = size; this.maxDoses = maxDoses; this.drinkTicks = drinkTicks; this.empty = empty;
    }

    public Item empty() { return empty.get(); }
    public boolean half() { return size == 1; }
    public float strength() { return half() ? 0.5f : 1f; }

    @Override
    public ItemStack getDefaultInstance() {
        return Vessels.filled(this, new PotionContents(net.minecraft.world.item.alchemy.Potions.WATER), maxDoses, false);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(), Vessels.contentsName(Vessels.contents(stack), Vessels.boiled(stack)));
    }

    // ---------------------------------------------------------------- beber / completar de agua
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // frasco de agua pela metade: clicando na agua, completa (e a agua deixa de ser fervida)
        if (Vessels.isWater(Vessels.contents(stack)) && Vessels.doses(stack) < maxDoses) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                if (level.mayInteract(player, pos) && level.getFluidState(pos).is(FluidTags.WATER)) {
                    level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1f, 0.9f);
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                    if (!level.isClientSide()) {
                        stack.set(ModDataComponents.DOSES.get(), maxDoses);
                        stack.remove(ModDataComponents.BOILED.get());
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        PotionContents pc = Vessels.contents(stack);
        boolean boiled = Vessels.boiled(stack);
        Player player = entity instanceof Player p ? p : null;
        if (entity instanceof ServerPlayer sp) {
            CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
            if (Vessels.isWater(pc)) {
                Thirst.add(sp, half() ? Thirst.BOTTLE / 2 : Thirst.BOTTLE);
                if (!boiled) Thirst.dirty(sp, half() ? 0.25f : Thirst.BOTTLE_NAUSEA);
            } else {
                Thirst.add(sp, half() ? 2 : 4);
            }
        }
        if (!level.isClientSide() && !Vessels.isWater(pc)) {
            float k = strength();
            pc.forEachEffect(e -> {
                if (e.getEffect().value().isInstantenous())
                    e.getEffect().value().applyInstantenousEffect(player, player, entity, e.getAmplifier(), k);
                else
                    entity.addEffect(k >= 1f ? e : new MobEffectInstance(e.getEffect(), Math.max(1, (int) (e.getDuration() * k)),
                            e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
            });
        }
        if (player != null) player.awardStat(Stats.ITEM_USED.get(this));
        entity.gameEvent(GameEvent.DRINK);
        if (player != null && player.hasInfiniteMaterials()) return stack;

        int d = Vessels.doses(stack);
        if (maxDoses > 1 && d > 1) {   // ainda sobra: o mesmo frasco com uma dose a menos
            stack.set(ModDataComponents.DOSES.get(), d - 1);
            return stack;
        }
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

    // ---------------------------------------------------------------- descricao
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        PotionContents pc = Vessels.contents(stack);
        if (maxDoses > 1) tip.add(Component.translatable("tooltip.binosvanillaplus.vessel.doses", Vessels.doses(stack), maxDoses).withStyle(ChatFormatting.GRAY));
        else tip.add(Component.translatable("tooltip.binosvanillaplus.vessel.half").withStyle(ChatFormatting.GRAY));
        if (Vessels.isWater(pc)) {
            if (Vessels.boiled(stack)) tip.add(Component.translatable("tooltip.binosvanillaplus.water.boiled").withStyle(ChatFormatting.AQUA));
            else {
                tip.add(Component.translatable("tooltip.binosvanillaplus.water.dirty").withStyle(ChatFormatting.GOLD));
                tip.add(Component.translatable(maxDoses > 1 ? "tooltip.binosvanillaplus.vessel.boil_full" : "tooltip.binosvanillaplus.water.boil").withStyle(ChatFormatting.GRAY));
            }
        } else {
            pc.addPotionTooltip(tip::add, strength(), ctx.tickRate());
        }
    }
}

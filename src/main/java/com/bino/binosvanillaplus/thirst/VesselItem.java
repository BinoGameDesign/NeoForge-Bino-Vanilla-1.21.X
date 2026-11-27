package com.bino.binosvanillaplus.thirst;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

// Frasco vazio (tamanhos 1, 3, 4 e 5; o 2 e' a garrafa do jogo). Clicar na agua enche (agua do mundo, nao fervida).
public class VesselItem extends Item {
    public final int size;
    private final Supplier<? extends Item> filled;

    public VesselItem(int size, Supplier<? extends Item> filled, Properties props) {
        super(props);
        this.size = size;
        this.filled = filled;
    }

    public FilledVesselItem filled() { return (FilledVesselItem) filled.get(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER)) return InteractionResultHolder.pass(stack);
        FilledVesselItem f = filled();
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1f, size >= 4 ? 0.85f : 1f);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.awardStat(Stats.ITEM_USED.get(this));
        ItemStack full = Vessels.filled(f, new PotionContents(Potions.WATER), false);
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, full), level.isClientSide());
    }
}

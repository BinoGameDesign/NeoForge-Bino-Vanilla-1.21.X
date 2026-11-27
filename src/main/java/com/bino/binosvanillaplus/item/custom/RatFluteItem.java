package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.entity.rat.RatEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// FLAUTA DOS RATOS (o que o King Rat deixa, 27/11): tocar chama um bando (1 Rato Gigante + 3 ratos) que defende o
// jogador por 60 s e depois vai embora; os ratos e Ratos Gigantes que ja estao por perto (16 blocos) tambem ficam do
// lado dele pelo mesmo tempo. Recarga de 2 min.
public class RatFluteItem extends Item {
    public static final int DURATION = 20 * 60, COOLDOWN = 20 * 120, RANGE = 16;

    public RatFluteItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // tres notas subindo
        for (int i = 0; i < 3; i++)
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1f, 0.8f + i * 0.25f);
        if (level instanceof ServerLevel sl) {
            for (RatEntity r : level.getEntitiesOfClass(RatEntity.class, player.getBoundingBox().inflate(RANGE), r -> r.isAlive()))
                r.befriend(player, DURATION, false);
            for (int i = 0; i < 4; i++) {
                EntityType<? extends RatEntity> type = i == 0 ? ModEntities.DIRE_RAT.get() : ModEntities.RAT.get();
                RatEntity r = type.create(sl);
                if (r == null) continue;
                double a = level.random.nextDouble() * Math.PI * 2, d = 1.5 + level.random.nextDouble() * 1.5;
                BlockPos p = BlockPos.containing(player.getX() + Math.cos(a) * d, player.getY(), player.getZ() + Math.sin(a) * d);
                r.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
                r.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
                r.befriend(player, DURATION, true);
                sl.addFreshEntity(r);
                sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 0.6, r.getZ(), 3, 0.3, 0.2, 0.3, 0.5);
            }
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.binosvanillaplus.rat_flute").withStyle(ChatFormatting.GRAY));
    }
}

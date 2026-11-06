package com.bino.binosvanillaplus.item;

import com.bino.binosvanillaplus.faction.FactionData;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

// Ovo do Pillager civil: cria um villager do jogo e marca ele como pillager.
// (Cores do ovo: ModClientEvents.onItemColors)
public class CivilianSpawnEggItem extends Item {
    public static final int BASE = 0xFF8A8F8C, SPOTS = 0xFF6B2A2E;

    public CivilianSpawnEggItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        ItemStack stack = ctx.getItemInHand();
        Villager v = EntityType.VILLAGER.spawn(level, stack, ctx.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true,
                !Objects.equals(clicked, pos) && face == Direction.UP);
        if (v != null) {
            FactionData.setCivilian(v, true);
            stack.consume(1, ctx.getPlayer());
            level.gameEvent(ctx.getPlayer(), GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }
}

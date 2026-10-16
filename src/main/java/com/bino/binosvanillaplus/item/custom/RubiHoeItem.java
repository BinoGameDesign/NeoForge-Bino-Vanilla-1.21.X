package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.util.ToolUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RubiHoeItem extends HoeItem {

    private static final int MAX_BLOCKS = 10;   // limite de plantações por clique

    public RubiHoeItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    // Roda quando você clica com o botão direito em um bloco usando a enxada
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        Player player = context.getPlayer();

        // Se não é plantação, ou ainda não está madura, usa o comportamento normal (arar a terra)
        if (player == null || !(clickedState.getBlock() instanceof CropBlock crop)) {
            return super.useOn(context);
        }
        if (!crop.isMaxAge(clickedState)) {
            return super.useOn(context);
        }

        // No cliente só mostramos a animação; quem faz o trabalho de verdade é o servidor
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();

        // Acha plantações do mesmo tipo, maduras e conectadas, até o limite
        List<BlockPos> crops = ToolUtil.findConnected(level, clickedPos,
                s -> s.is(clickedState.getBlock()) && crop.isMaxAge(s),
                MAX_BLOCKS);

        for (BlockPos pos : crops) {
            BlockState state = level.getBlockState(pos);

            // Solta os itens da colheita (respeita Fortuna da enxada)
            Block.dropResources(state, level, pos, null, player, stack);

            // Replanta: volta a plantação para a idade 0 (broto)
            level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
        }

        // Som de colheita
        level.playSound(null, clickedPos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

        // Gasta 1 de durabilidade por uso (não por bloco)
        EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, slot);

        return InteractionResult.SUCCESS;
    }
}
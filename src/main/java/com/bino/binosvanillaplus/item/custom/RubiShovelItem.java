package com.bino.binosvanillaplus.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RubiShovelItem extends ShovelItem implements MultiBreakTool {

    public RubiShovelItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public List<BlockPos> getExtraBlocks(ServerPlayer player, BlockPos origin, BlockState state) {
        List<BlockPos> result = new ArrayList<>();

        // Só age em blocos que a pá minera (terra, areia, cascalho...)
        if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return result;

        // Lança um "raio" dos olhos do jogador, na direção em que ele olha, com 6 blocos de alcance.
        // Serve para descobrir QUAL FACE do bloco ele está olhando.
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(6.0));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        // Segurança: se o raio não acertou o bloco quebrado, não faz nada
        if (!hit.getBlockPos().equals(origin)) return result;

        // O eixo da face decide o plano 3x3:
        // face de cima/baixo (Y) = plano horizontal | face leste/oeste (X) ou norte/sul (Z) = plano vertical
        Direction.Axis axis = hit.getDirection().getAxis();

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;     // o centro é o bloco original, já quebra sozinho

                BlockPos pos = switch (axis) {
                    case Y -> origin.offset(a, 0, b);
                    case X -> origin.offset(0, a, b);
                    case Z -> origin.offset(a, b, 0);
                };

                // Só entra na lista se for um bloco que a pá consegue minerar
                if (player.level().getBlockState(pos).is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                    result.add(pos);
                }
            }
        }
        return result;
    }
}
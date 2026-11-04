package com.bino.binosvanillaplus.block.custom;

import com.bino.binosvanillaplus.gear.FlashlightManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

// Luz invisivel da lanterna (igual ao bloco de luz do jogo). Se apaga sozinha poucos ticks depois de nao ser mais
// iluminada pela lanterna; assim nunca sobra luz perdida no mundo (nem depois de fechar o jogo).
// Como e' luz de BLOCO de verdade, funciona com qualquer shaderpack.
public class FlashlightLightBlock extends LightBlock {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final MapCodec<LightBlock> CODEC = (MapCodec) simpleCodec(FlashlightLightBlock::new);
    private static final int CHECK_TICKS = 4;

    public FlashlightLightBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<LightBlock> codec() { return CODEC; }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide) level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (FlashlightManager.wanted(level, pos)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
            return;
        }
        level.setBlock(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }
}

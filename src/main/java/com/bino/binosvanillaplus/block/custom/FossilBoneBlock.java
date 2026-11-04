package com.bino.binosvanillaplus.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

// Osso fossilizado (vertebra, costela ou membro). Os gerados no mundo sao NATURAL e sabem a especie (para a camera);
// os colocados pelo jogador sao so decoracao.
public class FossilBoneBlock extends RotatedPillarBlock {
    public static final IntegerProperty SPECIES = IntegerProperty.create("species", 0, 2);
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final MapCodec<RotatedPillarBlock> CODEC = (MapCodec) simpleCodec(p -> new FossilBoneBlock("vertebra", p));

    private final String part;

    public FossilBoneBlock(String part, Properties properties) {
        super(properties);
        this.part = part;
        registerDefaultState(defaultBlockState().setValue(SPECIES, 0).setValue(NATURAL, false));
    }

    public String part() { return part; }

    @Override
    public MapCodec<RotatedPillarBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SPECIES, NATURAL);
    }
}

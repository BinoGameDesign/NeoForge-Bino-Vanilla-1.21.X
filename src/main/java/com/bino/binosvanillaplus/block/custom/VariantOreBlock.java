package com.bino.binosvanillaplus.block.custom;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

// Minerio com 8 modelos 3D diferentes. A propriedade "variant" existe so para o blockstate JSON poder apontar
// para os 8 modelos; o bloco SEMPRE fica com variant=0 (nada e' salvo no mundo).
// Quem escolhe o modelo mostrado e' o RubiOreVariantModel (lado do cliente), pela posicao do bloco.
public class VariantOreBlock extends DropExperienceBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);

    public VariantOreBlock(IntProvider xpRange, Properties properties) {
        super(xpRange, properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }
}

package com.bino.binosvanillaplus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Mesa de Lapidacao: nao guarda nada. Existe para o jogo chamar o LapidaryTableRenderer (cliente), que desenha as
// pecas que se mexem (disco, lapidador, botao, visor) e os itens em cima da mesa.
public class LapidaryTableBlockEntity extends BlockEntity {
    public LapidaryTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAPIDARY_TABLE.get(), pos, state);
    }
}

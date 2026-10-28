package com.bino.binosvanillaplus.client.lookat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Para adicionar informacao nova no futuro: crie um provider e registre em LookAtRegistry.
// Cada provider so acrescenta linhas em "info"; o painel cuida do desenho.
public interface LookAtProvider {
    default void appendBlock(LookAtInfo info, Level level, BlockPos pos, BlockState state, LocalPlayer player) {}
    default void appendEntity(LookAtInfo info, Level level, Entity entity, LocalPlayer player) {}
}

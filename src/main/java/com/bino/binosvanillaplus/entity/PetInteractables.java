package com.bino.binosvanillaplus.entity;

import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

// Blocos que o pet sabe "apertar" (Alt + botao direito): so os de acionar, nada que abra menu (fornalha, bau...)
public final class PetInteractables {
    private PetInteractables() {}

    public static boolean allowed(BlockState s) {
        if (s.getBlock() instanceof ButtonBlock || s.getBlock() instanceof LeverBlock) return true;
        if (s.getBlock() instanceof DoorBlock door) return door.type().canOpenByHand();
        if (s.getBlock() instanceof TrapDoorBlock) return !s.is(Blocks.IRON_TRAPDOOR);
        return s.getBlock() instanceof FenceGateBlock || s.getBlock() instanceof BellBlock || s.getBlock() instanceof NoteBlock
                || s.getBlock() instanceof RepeaterBlock || s.getBlock() instanceof ComparatorBlock
                || s.getBlock() instanceof DaylightDetectorBlock;
    }
}

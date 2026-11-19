package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

// Bau perdido no chao de uma caverna (pequena, media ou grande): procura, a partir de um ponto aleatorio, um chao de
// pedra com ar em cima, longe da superficie e fora d'agua/lava. As vezes e' um bau duplo. Usa a tabela
// binosvanillaplus:chests/cave_chest. Como e' subterraneo, entra na rolagem de mimico (MimicWorldgen).
public class CaveChestFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "chests/cave_chest"));

    public CaveChestFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> fc) {
        WorldGenLevel level = fc.level();
        RandomSource rnd = fc.random();
        BlockPos o = fc.origin();
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX(), o.getZ());
        BlockPos.MutableBlockPos p = o.mutable();
        // desce ate achar ar com chao firme embaixo
        for (int i = 0; i < 24; i++, p.move(Direction.DOWN)) {
            if (p.getY() <= level.getMinBuildHeight() + 2) return false;
            if (!spot(level, p)) continue;
            if (p.getY() > surface - 8) return false;           // perto demais da superficie
            if (!isCave(level, p)) continue;
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
            BlockPos at = p.immutable();
            if (rnd.nextInt(4) == 0) {
                // duplo: metade LEFT aqui, a parceira fica em facing.getClockWise() (ChestBlock.getConnectedDirection)
                Direction side = facing.getClockWise();
                BlockPos other = at.relative(side);
                if (spot(level, other)) {
                    put(level, rnd, at, facing, ChestType.LEFT);
                    put(level, rnd, other, facing, ChestType.RIGHT);
                    return true;
                }
            }
            put(level, rnd, at, facing, ChestType.SINGLE);
            return true;
        }
        return false;
    }

    private static boolean spot(WorldGenLevel level, BlockPos p) {
        BlockState here = level.getBlockState(p), below = level.getBlockState(p.below());
        return here.isAir() && level.getBlockState(p.above()).isAir()
                && below.isFaceSturdy(level, p.below(), Direction.UP) && below.getFluidState().isEmpty()
                && level.getFluidState(p).isEmpty();
    }

    // dentro de uma caverna de verdade (nao numa fresta): bastante ar em volta
    private static boolean isCave(WorldGenLevel level, BlockPos p) {
        int air = 0;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int dy = 0; dy <= 2; dy++)
                    if (level.getBlockState(p.offset(dx, dy, dz)).isAir()) air++;
        return air >= 30;   // de 75
    }

    private static void put(WorldGenLevel level, RandomSource rnd, BlockPos p, Direction facing, ChestType type) {
        BlockState st = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).setValue(ChestBlock.TYPE, type);
        level.setBlock(p, st, 2);
        RandomizableContainer.setBlockEntityLootTable(level, rnd, p, LOOT);
    }
}

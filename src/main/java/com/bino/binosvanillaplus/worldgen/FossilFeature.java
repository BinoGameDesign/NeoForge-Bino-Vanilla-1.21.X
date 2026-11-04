package com.bino.binosvanillaplus.worldgen;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.FossilBoneBlock;
import com.bino.binosvanillaplus.block.custom.FossilSkullBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// Fosseis de monstros gigantes, montados com os blocos de osso fossilizado (cranio, vertebras, costelas, membros).
// Tres especies com formas diferentes; posicao, direcao e detalhes variam (pedacos faltando, curvas).
// buried = enterrado na pedra (aparece em cavernas); exposed = coluna e arcos das costelas saindo do chao.
public class FossilFeature extends Feature<NoneFeatureConfiguration> {
    private final boolean exposed;

    public FossilFeature(Codec<NoneFeatureConfiguration> codec, boolean exposed) {
        super(codec);
        this.exposed = exposed;
    }

    private record Ctx(WorldGenLevel level, BlockPos origin, int rot, int species, RandomSource rnd, boolean exposed) {}

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> fc) {
        WorldGenLevel level = fc.level();
        RandomSource rnd = fc.random();
        BlockPos o = fc.origin();
        int species;
        if (exposed) {
            species = rnd.nextBoolean() ? 0 : 2;                 // so os grandes saem do chao
            o = o.below(1 + rnd.nextInt(3));
            if (!level.getBlockState(o.below()).isSolid()) return false;
            if (!level.getFluidState(o).isEmpty() || !level.getFluidState(o.above()).isEmpty()) return false;
        } else {
            int r = rnd.nextInt(100);
            species = r < 40 ? 0 : r < 75 ? 1 : 2;
            if (level.getBlockState(o).isAir()) return false;    // comeca dentro da pedra
        }
        Ctx c = new Ctx(level, o, rnd.nextInt(4), species, rnd, exposed);
        switch (species) {
            case 0 -> beast(c);
            case 1 -> serpent(c);
            default -> leviathan(c);
        }
        return true;
    }

    // ---------------------------------------------------------------- formas (x = comprimento, z = lado, y = altura)

    private static void beast(Ctx c) {
        for (int x = -9; x <= 10; x++) {
            int y = (int) Math.round(1.2 * Math.sin((x + 9) / 19.0 * Math.PI));
            bone(c, "vertebra", x, y, 0, Direction.Axis.X, 0.95f);
            if (x >= -3 && x <= 5 && (x & 1) == 1) {
                for (int s = -1; s <= 1; s += 2) {
                    int[][] rib = {{y, 1}, {y - 1, 2}, {y - 2, 3}, {y - 3, 3}, {y - 4, 2}};
                    for (int[] p : rib) bone(c, "rib", x, p[0], s * p[1], Direction.Axis.Y, 0.85f);
                }
            }
        }
        for (int lx : new int[]{-5, 6}) {
            for (int s = -1; s <= 1; s += 2) {
                for (int y = -1; y >= -6; y--) bone(c, "limb", lx, y, s * 2, Direction.Axis.Y, 0.9f);
                bone(c, "limb", lx + 1, -6, s * 2, Direction.Axis.X, 0.8f);
            }
        }
        for (int i = 1; i <= 8; i++) bone(c, "vertebra", -9 - i, -i / 3, 0, Direction.Axis.X, 0.9f - i * 0.05f);   // cauda
        skull(c, 11, 0, 3, 2);
    }

    private static void serpent(Ctx c) {
        for (int x = -14; x <= 14; x++) {
            int z = (int) Math.round(3 * Math.sin(x / 4.5)), y = (int) Math.round(1.5 * Math.sin(x / 7.0));
            bone(c, "vertebra", x, y, z, Direction.Axis.X, 0.92f);
            if ((x & 1) == 0 && x > -12) {
                bone(c, "rib", x, y - 1, z - 1, Direction.Axis.Y, 0.8f);
                bone(c, "rib", x, y - 1, z + 1, Direction.Axis.Y, 0.8f);
            }
        }
        int ez = (int) Math.round(3 * Math.sin(15 / 4.5)), ey = (int) Math.round(1.5 * Math.sin(15 / 7.0));
        skull(c, 15, ey, 2, 1, ez);
    }

    private static void leviathan(Ctx c) {
        for (int x = -8; x <= 8; x++) {
            bone(c, "vertebra", x, 0, 0, Direction.Axis.X, 0.95f);
            if ((x & 1) == 0 && x >= -6 && x <= 6) {
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 1; i <= 9; i++) {
                        double th = i / 9.0 * Math.PI / 2;
                        int z = (int) Math.round(5 * Math.sin(th)), y = (int) Math.round(-5 * (1 - Math.cos(th)));
                        bone(c, "rib", x, y, s * z, i < 5 ? Direction.Axis.Z : Direction.Axis.Y, 0.88f);
                    }
                    bone(c, "rib", x, -6, s * 5, Direction.Axis.Y, 0.8f);
                    bone(c, "rib", x, -7, s * 5, Direction.Axis.Y, 0.7f);
                }
            }
        }
        for (int fx : new int[]{-3, 4}) {   // nadadeiras
            for (int s = -1; s <= 1; s += 2) {
                bone(c, "limb", fx, -6, s * 6, Direction.Axis.Z, 0.85f);
                bone(c, "limb", fx, -7, s * 7, Direction.Axis.Z, 0.8f);
                bone(c, "limb", fx + 1, -7, s * 7, Direction.Axis.X, 0.6f);
            }
        }
        for (int i = 1; i <= 5; i++) bone(c, "vertebra", -8 - i, 0, 0, Direction.Axis.X, 0.9f - i * 0.08f);
        skull(c, 9, -1, 3, 3);
    }

    private static void skull(Ctx c, int x0, int y0, int len, int size) { skull(c, x0, y0, len, size, 0); }

    // cranio: bloco de "len" de comprimento e "size" de altura/largura, com a frente virada para a ponta (+x)
    private static void skull(Ctx c, int x0, int y0, int len, int size, int zc) {
        int half = size / 2;
        for (int x = x0; x < x0 + len; x++) {
            for (int y = y0; y < y0 + size; y++) {
                for (int z = zc - half; z <= zc - half + size - 1; z++) {
                    BlockState s = ModBlocks.FOSSIL_SKULL.get().defaultBlockState()
                            .setValue(FossilSkullBlock.FACING, dir(c, Direction.EAST))
                            .setValue(FossilBoneBlock.SPECIES, c.species()).setValue(FossilBoneBlock.NATURAL, true);
                    set(c, x, y, z, s, 1f);
                }
            }
        }
    }

    // ---------------------------------------------------------------- ajudantes

    private static void bone(Ctx c, String part, int x, int y, int z, Direction.Axis axis, float chance) {
        var block = switch (part) {
            case "rib" -> ModBlocks.FOSSIL_RIB.get();
            case "limb" -> ModBlocks.FOSSIL_LIMB.get();
            default -> ModBlocks.FOSSIL_VERTEBRA.get();
        };
        Direction.Axis a = axis == Direction.Axis.Y ? axis : ((c.rot() & 1) == 1 ? (axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : axis);
        BlockState s = block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, a)
                .setValue(FossilBoneBlock.SPECIES, c.species()).setValue(FossilBoneBlock.NATURAL, true);
        set(c, x, y, z, s, chance);
    }

    private static Direction dir(Ctx c, Direction d) {
        for (int i = 0; i < c.rot(); i++) d = d.getClockWise();
        return d;
    }

    private static void set(Ctx c, int lx, int ly, int lz, BlockState state, float chance) {
        if (c.rnd().nextFloat() > chance) return;   // pedacos faltando: cada fossil fica diferente
        int x = lx, z = lz;
        for (int i = 0; i < c.rot(); i++) { int t = x; x = -z; z = t; }
        BlockPos p = c.origin().offset(x, ly, z);
        WorldGenLevel level = c.level();
        if (!level.ensureCanWrite(p)) return;
        BlockState cur = level.getBlockState(p);
        if (cur.is(Blocks.BEDROCK) || !cur.getFluidState().isEmpty() || cur.hasBlockEntity()) return;
        boolean ok = c.exposed()
                ? cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.DIRT) || cur.is(BlockTags.BASE_STONE_OVERWORLD) || cur.is(BlockTags.SAND)
                        || cur.is(Blocks.GRAVEL) || cur.is(BlockTags.TERRACOTTA)
                : cur.is(BlockTags.BASE_STONE_OVERWORLD) || cur.is(BlockTags.DIRT) || cur.is(Blocks.GRAVEL) || cur.is(BlockTags.SAND)
                        || cur.is(BlockTags.TERRACOTTA) || cur.isAir();
        if (ok) level.setBlock(p, state, 2);
    }
}

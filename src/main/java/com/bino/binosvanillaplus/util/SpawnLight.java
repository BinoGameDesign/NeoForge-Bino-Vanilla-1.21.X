package com.bino.binosvanillaplus.util;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;

// Luz que NAO impede monstros de nascer: a luz dos nossos minerios (tag binosvanillaplus:spawn_ignored_light).
// Usado pelo MonsterSpawnLightMixin quando o jogo diz "claro demais para nascer".
public final class SpawnLight {
    private SpawnLight() {}

    public static final TagKey<Block> IGNORED = TagKey.create(Registries.BLOCK,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "spawn_ignored_light"));

    // A luz de bloco neste ponto vem so de um minerio nosso? Sobe pela luz (sempre para o vizinho mais claro) ate
    // a fonte; se a fonte e' da tag, sim. Os minerios dao 6, entao so luz ate 5 pode ser deles.
    public static boolean onlyIgnoredLight(ServerLevelAccessor level, BlockPos pos) {
        int light = level.getBrightness(LightLayer.BLOCK, pos);
        if (light <= 0 || light > 5) return false;
        BlockPos.MutableBlockPos cur = pos.mutable(), next = new BlockPos.MutableBlockPos();
        for (int step = 0; step < 16; step++) {
            BlockPos best = null;
            int bestLight = light;
            for (Direction d : Direction.values()) {
                next.setWithOffset(cur, d);
                if (!level.hasChunkAt(next)) continue;
                int l = level.getBrightness(LightLayer.BLOCK, next);
                if (l > bestLight) { bestLight = l; best = next.immutable(); }
            }
            if (best == null) break;   // chegou na fonte (ninguem mais claro em volta)
            cur.set(best);
            light = bestLight;
        }
        return level.getBlockState(cur).is(IGNORED);
    }

    // O teste de escuridao do jogo (Monster.isDarkEnoughToSpawn), fingindo que nao ha luz de bloco
    public static boolean darkEnoughIgnoringBlockLight(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) return false;
        DimensionType dim = level.dimensionType();
        int darken = level.getLevel().isThundering() ? 10 : level.getSkyDarken();
        int raw = Math.max(0, level.getBrightness(LightLayer.SKY, pos) - darken);
        return dim.monsterSpawnLightTest().sample(random) >= raw;
    }
}

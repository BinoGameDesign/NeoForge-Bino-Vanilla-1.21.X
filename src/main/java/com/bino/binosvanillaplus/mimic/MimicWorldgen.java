package com.bino.binosvanillaplus.mimic;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Set;

// Mimicos que ja nascem com o mundo. Quando um pedaco do mundo acaba de ser GERADO, cada bau com tabela de loot
// (ainda nao aberto) tem CHANCE de ser um mimico se:
//  - estiver no subsolo do Overworld (abaixo da superficie), ou
//  - for de uma das estruturas da lista (templo do deserto, templo da selva, masmorra, tesouro enterrado,
//    camaras do julgamento) - essas valem mesmo perto da superficie.
// Os baus de caverna do mod (CaveChestFeature) sao subterraneos, entao entram na mesma conta.
// Bau duplo: a rolagem e' feita para o PAR (mesma semente para as duas metades), entao ou as duas viram ou nenhuma,
// mesmo que cada metade esteja num pedaco diferente do mundo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class MimicWorldgen {
    private MimicWorldgen() {}

    public static final float CHANCE = 0.20f;
    // quantos blocos abaixo do chao/fundo do mar conta como "subterraneo"
    private static final int DEPTH = 5;

    private static final Set<String> STRUCTURE_TABLES = Set.of(
            "minecraft:chests/desert_pyramid",
            "minecraft:chests/jungle_temple",
            "minecraft:chests/simple_dungeon",
            "minecraft:chests/buried_treasure",
            "minecraft:chests/abandoned_mineshaft");   // (na mina os baus sao carrinhos; so conta se for um bloco)

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!e.isNewChunk() || !(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (!(be instanceof ChestBlockEntity chest)) continue;
            ResourceKey<LootTable> table = chest.getLootTable();
            if (table == null) continue;
            String id = table.location().toString();
            BlockPos p = chest.getBlockPos();
            boolean listed = STRUCTURE_TABLES.contains(id) || id.startsWith("minecraft:chests/trial_chambers/");
            boolean underground = level.dimension() == Level.OVERWORLD
                    && p.getY() < chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, p.getX() & 15, p.getZ() & 15) - DEPTH;
            if (!listed && !underground) continue;
            if (!roll(level, pairKey(chest))) continue;
            chest.setData(ModAttachments.MIMIC, true);
            chunk.setUnsaved(true);
        }
    }

    // posicao que representa o par: a menor das duas (as duas metades chegam no mesmo valor)
    private static BlockPos pairKey(ChestBlockEntity chest) {
        BlockPos p = chest.getBlockPos();
        BlockState st = chest.getBlockState();
        if (!st.hasProperty(ChestBlock.TYPE) || st.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return p;
        BlockPos o = p.relative(ChestBlock.getConnectedDirection(st));
        return p.asLong() < o.asLong() ? p : o;
    }

    private static boolean roll(ServerLevel level, BlockPos key) {
        long s = level.getSeed() ^ (key.asLong() * 0x9E3779B97F4A7C15L) ^ 0x4D494D4943L;   // "MIMIC"
        s ^= s >>> 33; s *= 0xff51afd7ed558ccdL; s ^= s >>> 33; s *= 0xc4ceb9fe1a85ec53L; s ^= s >>> 33;
        return ((s >>> 40) / (float) (1 << 24)) < CHANCE;
    }
}

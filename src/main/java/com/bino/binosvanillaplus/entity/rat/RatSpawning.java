package com.bino.binosvanillaplus.entity.rat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Spawns extras do rato:
//  1) comida colocada como bloco (tag binosvanillaplus:rat_food: bolo, bolo com vela) e largada por 3 dias de jogo
//     -> chance de aparecerem ratos em volta dela, em qualquer lugar, so para comer;
//  2) dentro de estruturas: mansao, minas abandonadas, fortaleza (stronghold), trilha em ruinas, templo da selva
//     e vila abandonada (vila sem nenhum aldeao por perto).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class RatSpawning {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final long FOOD_AGE = 3 * 24000L;          // 3 dias de jogo
    public static final float FOOD_CHANCE = 0.25f;            // chance a cada checagem depois dos 3 dias
    public static final long FOOD_RETRY = 12000L;             // depois de aparecer rato, nova chance meio dia depois
    public static final int FOOD_CHECK_EVERY = 600;           // ticks (30 s)
    public static final int STRUCT_CHECK_EVERY = 200;         // ticks (10 s) por jogador
    public static final float STRUCT_CHANCE = 0.3f;
    public static final int STRUCT_MAX_RATS = 5;              // no maximo tantos ratos a 32 blocos

    public static final TagKey<Block> RAT_FOOD = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "rat_food"));

    private static final List<ResourceKey<Structure>> STRUCTURES = List.of(BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.MINESHAFT,
            BuiltinStructures.MINESHAFT_MESA, BuiltinStructures.STRONGHOLD, BuiltinStructures.TRAIL_RUINS, BuiltinStructures.JUNGLE_TEMPLE);
    private static final List<ResourceKey<Structure>> VILLAGES = List.of(BuiltinStructures.VILLAGE_PLAINS, BuiltinStructures.VILLAGE_DESERT,
            BuiltinStructures.VILLAGE_SAVANNA, BuiltinStructures.VILLAGE_SNOWY, BuiltinStructures.VILLAGE_TAIGA);

    // ------------------------------------------------------------------ comida largada
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && e.getPlacedBlock().is(RAT_FOOD))
            FoodData.get(sl).food.put(e.getPos().asLong(), sl.getGameTime());
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (now % FOOD_CHECK_EVERY == 0) checkFood(sl, now);
        if (now % STRUCT_CHECK_EVERY == 0) for (ServerPlayer p : sl.players()) checkStructures(sl, p);
    }

    private static void checkFood(ServerLevel sl, long now) {
        FoodData data = FoodData.get(sl);
        boolean dirty = false;
        for (Iterator<Map.Entry<Long, Long>> it = data.food.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, Long> en = it.next();
            BlockPos pos = BlockPos.of(en.getKey());
            if (!sl.isLoaded(pos)) continue;
            if (!sl.getBlockState(pos).is(RAT_FOOD)) { it.remove(); dirty = true; continue; }   // comeram ou tiraram
            if (now - en.getValue() < FOOD_AGE || sl.random.nextFloat() >= FOOD_CHANCE) continue;
            if (nearbyRats(sl, pos, 16) >= 3) continue;
            int n = 1 + sl.random.nextInt(2);
            for (int i = 0; i < n; i++) spawnNear(sl, pos, 3, 8, false);
            en.setValue(now - FOOD_AGE + FOOD_RETRY);
            dirty = true;
        }
        if (dirty) data.setDirty();
    }

    // ------------------------------------------------------------------ estruturas
    private static void checkStructures(ServerLevel sl, ServerPlayer p) {
        if (p.isSpectator() || sl.random.nextFloat() >= STRUCT_CHANCE) return;
        BlockPos at = p.blockPosition();
        ResourceKey<Structure> in = null;
        for (ResourceKey<Structure> k : STRUCTURES) if (inside(sl, at, k)) { in = k; break; }
        if (in == null) {
            for (ResourceKey<Structure> k : VILLAGES) {
                if (inside(sl, at, k)
                        && sl.getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(48)).isEmpty()) { in = k; break; }   // abandonada
            }
        }
        if (in == null || nearbyRats(sl, at, 32) >= STRUCT_MAX_RATS) return;
        ResourceKey<Structure> key = in;
        BlockPos spot = findSpot(sl, at, 8, 20, sl.random, q -> inside(sl, q, key));
        if (spot != null) spawnAt(sl, spot, 1 + sl.random.nextInt(2));
    }

    // ------------------------------------------------------------------ ajudantes
    // a posicao esta dentro de uma peca dessa estrutura? (o 1.21.1 pede uma condicao sobre o Holder da estrutura)
    private static boolean inside(ServerLevel sl, BlockPos pos, ResourceKey<Structure> key) {
        java.util.function.Predicate<net.minecraft.core.Holder<Structure>> is = h -> h.is(key);
        return sl.structureManager().getStructureWithPieceAt(pos, is).isValid();
    }

    private static int nearbyRats(ServerLevel sl, BlockPos pos, int r) {
        return sl.getEntitiesOfClass(RatEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(r)).size();
    }

    private static void spawnNear(ServerLevel sl, BlockPos center, int min, int max, boolean dark) {
        BlockPos spot = findSpot(sl, center, min, max, sl.random, q -> !dark || sl.getBrightness(LightLayer.BLOCK, q) <= 7);
        if (spot != null) spawnAt(sl, spot, 1);
    }

    // chao firme, 1 bloco livre, longe do centro entre min e max blocos
    private static BlockPos findSpot(ServerLevel sl, BlockPos c, int min, int max, RandomSource rnd, java.util.function.Predicate<BlockPos> ok) {
        for (int tries = 0; tries < 16; tries++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = min + rnd.nextDouble() * (max - min);
            int x = c.getX() + (int) Math.round(Math.cos(a) * d), z = c.getZ() + (int) Math.round(Math.sin(a) * d);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos q = new BlockPos(x, c.getY() + dy, z);
                if (sl.getBlockState(q).isAir() && sl.getBlockState(q.below()).isFaceSturdy(sl, q.below(), net.minecraft.core.Direction.UP)
                        && sl.getFluidState(q).isEmpty() && ok.test(q)) return q;
            }
        }
        return null;
    }

    private static void spawnAt(ServerLevel sl, BlockPos pos, int n) {
        for (int i = 0; i < n; i++) {
            RatEntity r = ModEntities.RAT.get().create(sl);
            if (r == null) return;
            r.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360, 0);
            r.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            sl.addFreshEntity(r);
        }
    }

    // ------------------------------------------------------------------ dados salvos (por dimensao)
    public static class FoodData extends SavedData {
        final Map<Long, Long> food = new HashMap<>();
        private static final String ID = "binosvanillaplus_rat_food";

        static FoodData get(ServerLevel sl) {
            return sl.getDataStorage().computeIfAbsent(new SavedData.Factory<>(FoodData::new, FoodData::load, null), ID);
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            food.forEach((pos, time) -> { CompoundTag t = new CompoundTag(); t.putLong("Pos", pos); t.putLong("Time", time); list.add(t); });
            tag.put("Food", list);
            return tag;
        }

        private static FoodData load(CompoundTag tag, HolderLookup.Provider registries) {
            FoodData d = new FoodData();
            for (Tag t : tag.getList("Food", Tag.TAG_COMPOUND)) { CompoundTag c = (CompoundTag) t; d.food.put(c.getLong("Pos"), c.getLong("Time")); }
            return d;
        }
    }
}

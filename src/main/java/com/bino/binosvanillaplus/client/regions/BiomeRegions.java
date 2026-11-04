package com.bino.binosvanillaplus.client.regions;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.map.ExploredMap;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Regioes nomeadas: o mundo e dividido em celulas de 8x8 blocos; celulas vizinhas com o MESMO bioma formam uma regiao,
// que ganha um nome aleatorio ("Planice do Luar"). Regioes que se encontram sao unidas (fica o nome da maior).
// Guardado em binos_map/<mundo>/<dimensao>.regions.json.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class BiomeRegions {
    private BiomeRegions() {}

    public static final int CELL = 8;
    private static final int SAMPLE_R = 6;   // celulas lidas em volta do jogador por rodada

    public static final class Region {
        public int id;
        public String name;
        public String biome;
        int cells;
    }

    public record Label(String name, int x, int z, int cells) {}

    private static final Map<Long, Integer> CELLS = new HashMap<>();
    private static final Map<Integer, Region> REGIONS = new HashMap<>();
    private static int nextId = 1;
    private static int version = 0;
    private static String world, dim;
    private static long lastSave = 0;
    private static boolean dirty = false;

    private static long cellKey(int cx, int cz) { return ((long) cx & 0xFFFFFFFFL) | ((long) cz << 32); }

    // ------------------------------------------------------------ consulta

    public static Region regionAtBlock(int x, int z) {
        Integer id = CELLS.get(cellKey(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)));
        return id == null ? null : REGIONS.get(id);
    }

    // ------------------------------------------------------------ contornos e nomes (em cache)

    private static int cacheVersion = -1;
    private static int[] edges = new int[0];            // x1,z1,x2,z2 por segmento
    private static List<Label> labels = new ArrayList<>();

    private static void rebuild() {
        if (cacheVersion == version) return;
        cacheVersion = version;
        List<int[]> list = new ArrayList<>();
        Map<Integer, long[]> acc = new HashMap<>();   // id -> soma x, soma z, n
        for (Map.Entry<Long, Integer> e : CELLS.entrySet()) {
            long k = e.getKey();
            int cx = (int) k, cz = (int) (k >> 32), id = e.getValue();
            long[] a = acc.computeIfAbsent(id, i -> new long[3]);
            a[0] += cx; a[1] += cz; a[2]++;
            // (contornos desligados: nao calcula as bordas)
        }
        edges = new int[list.size() * 4];
        for (int i = 0; i < list.size(); i++) System.arraycopy(list.get(i), 0, edges, i * 4, 4);

        // rotulo: a celula da regiao mais perto do centro dela
        Map<Integer, long[]> best = new HashMap<>();   // id -> cx, cz, dist2
        for (Map.Entry<Long, Integer> e : CELLS.entrySet()) {
            long[] a = acc.get(e.getValue());
            double mx = a[0] / (double) a[2], mz = a[1] / (double) a[2];
            long k = e.getKey();
            int cx = (int) k, cz = (int) (k >> 32);
            long d2 = (long) (((cx - mx) * (cx - mx) + (cz - mz) * (cz - mz)) * 100);
            long[] b = best.get(e.getValue());
            if (b == null || d2 < b[2]) best.put(e.getValue(), new long[]{cx, cz, d2});
        }
        List<Label> ls = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : best.entrySet()) {
            Region r = REGIONS.get(e.getKey());
            if (r == null || r.cells < 4) continue;   // regioes minusculas nao ganham rotulo no mapa
            ls.add(new Label(r.name, (int) e.getValue()[0] * CELL + CELL / 2, (int) e.getValue()[1] * CELL + CELL / 2, r.cells));
        }
        ls.sort((p, q) -> Integer.compare(q.cells(), p.cells()));
        labels = ls;
    }

    public static int[] edges() { rebuild(); return edges; }

    public static List<Label> labels() { rebuild(); return labels; }

    // ------------------------------------------------------------ amostragem

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        long t0 = com.bino.binosvanillaplus.client.Perf.start();
        try { onTickImpl(event); } finally { com.bino.binosvanillaplus.client.Perf.end("Regioes de bioma", t0); }
    }

    private static void onTickImpl(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) {
            if (world != null) { save(); clear(); }
            return;
        }
        String w = ExploredMap.currentWorld(), d = ExploredMap.currentDim();
        if (w == null || d == null) return;   // o mapa ainda nao escolheu o mundo
        if (!w.equals(world) || !d.equals(dim)) {
            save();
            clear();
            world = w;
            dim = d;
            load();
            lastSave = level.getGameTime();
        }
        if (d.startsWith(BinosVanillaMod.MOD_ID)) return;   // dimensao portatil: sem regioes
        long now = level.getGameTime();
        if (dirty && now - lastSave > 20 * 120) { save(); lastSave = now; }
        if (p.tickCount % 10 != 0) return;

        int pcx = Math.floorDiv(p.getBlockX(), CELL), pcz = Math.floorDiv(p.getBlockZ(), CELL);
        boolean ceiling = level.dimensionType().hasCeiling();
        int budget = 40;
        for (int r = 0; r <= SAMPLE_R && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    if (CELLS.containsKey(cellKey(cx, cz))) continue;
                    int x = cx * CELL + CELL / 2, z = cz * CELL + CELL / 2;
                    if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                    int y = ceiling ? p.getBlockY() : Math.max(level.getMinBuildHeight() + 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1);
                    String biome = level.getBiome(new BlockPos(x, y, z)).unwrapKey().map(k -> k.location().toString()).orElse(null);
                    if (biome == null) continue;
                    addCell(cx, cz, biome);
                    budget--;
                }
            }
        }
    }

    private static void addCell(int cx, int cz, String biome) {
        // regioes vizinhas (4 lados) com o mesmo bioma
        Set<Integer> near = new LinkedHashSet<>();
        int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] o : nb) {
            Integer id = CELLS.get(cellKey(cx + o[0], cz + o[1]));
            if (id != null && REGIONS.get(id) != null && REGIONS.get(id).biome.equals(biome)) near.add(id);
        }
        Region target;
        if (near.isEmpty()) {
            target = new Region();
            target.id = nextId++;
            target.biome = biome;
            Set<String> used = new HashSet<>();
            for (Region r : REGIONS.values()) used.add(r.name);
            target.name = RegionNames.generate(biome, new Random(((long) world.hashCode() << 32) ^ cellKey(cx, cz) ^ target.id), used);
            REGIONS.put(target.id, target);
        } else {
            target = null;
            for (int id : near) {
                Region r = REGIONS.get(id);
                if (target == null || r.cells > target.cells) target = r;
            }
            if (near.size() > 1) {   // junta as outras na maior
                Set<Integer> others = new HashSet<>(near);
                others.remove(target.id);
                for (Map.Entry<Long, Integer> e : CELLS.entrySet()) {
                    if (others.contains(e.getValue())) { e.setValue(target.id); target.cells++; }
                }
                for (int id : others) REGIONS.remove(id);
            }
        }
        CELLS.put(cellKey(cx, cz), target.id);
        target.cells++;
        version++;
        dirty = true;
    }

    // ------------------------------------------------------------ disco

    private static final class Data {
        int next = 1;
        List<RegionData> regions = new ArrayList<>();
        long[] keys = new long[0];
        int[] ids = new int[0];
    }

    private static final class RegionData {
        int id;
        String name, biome;
    }

    private static Path file() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("binos_map").resolve(world).resolve(dim + ".regions.json");
    }

    private static void clear() {
        CELLS.clear();
        REGIONS.clear();
        nextId = 1;
        version++;
        world = dim = null;
        dirty = false;
    }

    private static void save() {
        if (world == null || dim == null || !dirty) return;
        Data d = new Data();
        d.next = nextId;
        for (Region r : REGIONS.values()) {
            RegionData rd = new RegionData();
            rd.id = r.id; rd.name = r.name; rd.biome = r.biome;
            d.regions.add(rd);
        }
        d.keys = new long[CELLS.size()];
        d.ids = new int[CELLS.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : CELLS.entrySet()) { d.keys[i] = e.getKey(); d.ids[i] = e.getValue(); i++; }
        try {
            Files.createDirectories(file().getParent());
            try (Writer wr = Files.newBufferedWriter(file())) { new Gson().toJson(d, wr); }
            dirty = false;
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Data d = new Gson().fromJson(r, Data.class);
            if (d == null) return;
            nextId = Math.max(1, d.next);
            for (RegionData rd : d.regions) {
                Region reg = new Region();
                reg.id = rd.id; reg.name = rd.name; reg.biome = rd.biome;
                REGIONS.put(reg.id, reg);
            }
            for (int i = 0; i < d.keys.length && i < d.ids.length; i++) {
                Region reg = REGIONS.get(d.ids[i]);
                if (reg == null) continue;
                CELLS.put(d.keys[i], d.ids[i]);
                reg.cells++;
            }
            version++;
        } catch (Exception ignored) {
        }
    }
}

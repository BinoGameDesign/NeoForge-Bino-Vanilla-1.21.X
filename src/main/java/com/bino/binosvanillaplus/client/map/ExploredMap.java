package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// Memoria do que o jogador ja viu: uma "tile" de 16x16 colunas por chunk (cor + altura da superficie).
// Os chunks carregados em volta do jogador sao lidos de tempos em tempos; o resultado e' salvo em disco por mundo/dimensao.
public final class ExploredMap {
    public static final class Tile {
        public final int[] abgr = new int[256];     // cor no formato do NativeImage (ABGR), 0 = nunca visto
        public final short[] h = new short[256];
        long lastScan;
        boolean texDirty = true;
        DynamicTexture tex;
        ResourceLocation texId;
    }

    public static final int UNKNOWN = Integer.MIN_VALUE;
    private static final int SCAN_RADIUS = 7;          // em chunks
    private static final int PER_RUN_BUDGET = 8;       // chunks lidos por rodada
    private static final Map<Long, Tile> TILES = new HashMap<>();
    private static String curWorld = null, curDim = null;
    private static long lastSave = 0;
    private static int texCounter = 0;

    public static long key(int cx, int cz) { return ((long) cx & 0xFFFFFFFFL) | ((long) cz << 32); }
    public static Tile tile(int cx, int cz) { return TILES.get(key(cx, cz)); }
    public static Map<Long, Tile> all() { return TILES; }

    public static int heightAt(int x, int z) {
        Tile t = TILES.get(key(x >> 4, z >> 4));
        if (t == null) return UNKNOWN;
        int i = (z & 15) * 16 + (x & 15);
        return t.abgr[i] == 0 ? UNKNOWN : t.h[i];
    }

    // cor em ARGB (para fills), 0 se desconhecido
    public static int argbAt(int x, int z) {
        Tile t = TILES.get(key(x >> 4, z >> 4));
        if (t == null) return 0;
        int c = t.abgr[(z & 15) * 16 + (x & 15)];
        if (c == 0) return 0;
        return (c & 0xFF00FF00) | ((c & 0xFF) << 16) | ((c >> 16) & 0xFF);
    }

    public static String currentDim() { return curDim; }

    // ----------------------------------------------------------- ciclo de vida

    private static String worldKey(Minecraft mc) {
        String raw;
        if (mc.getSingleplayerServer() != null) raw = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        else if (mc.getCurrentServer() != null) raw = "mp_" + mc.getCurrentServer().ip;
        else raw = "unknown";
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Path worldDir() {
        return FMLPaths.GAMEDIR.get().resolve("binos_map").resolve(curWorld);
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer p = mc.player;
        if (level == null || p == null) return;

        String world = worldKey(mc);
        String dim = level.dimension().location().toString().replace(':', '_');
        if (!world.equals(curWorld) || !dim.equals(curDim)) {
            saveAndClear();
            curWorld = world;
            curDim = dim;
            load();
            MapWaypoints.load(worldDir(), curDim);
        }
        long now = level.getGameTime();
        if (now - lastSave > 20 * 180) { save(); lastSave = now; }
        if (p.tickCount % 5 != 0) return;

        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4, budget = PER_RUN_BUDGET;
        for (int d = 0; d <= SCAN_RADIUS && budget > 0; d++) {
            for (int dx = -d; dx <= d && budget > 0; dx++) {
                for (int dz = -d; dz <= d && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    Tile t = TILES.get(key(cx, cz));
                    boolean want = t == null
                            || (d <= 3 && now - t.lastScan > 60)
                            || (now - t.lastScan > 600);
                    if (!want) continue;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    if (t == null) { t = new Tile(); TILES.put(key(cx, cz), t); }
                    scan(level, chunk, t, p.getBlockY());
                    t.lastScan = now;
                    t.texDirty = true;
                    budget--;
                }
            }
        }
    }

    private static void scan(ClientLevel level, LevelChunk chunk, Tile t, int playerY) {
        int minY = level.getMinBuildHeight();
        boolean ceiling = level.dimensionType().hasCeiling();   // Nether: le a partir da altura do jogador
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] hs = new int[256];

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int wx = baseX + x, wz = baseZ + z, y;
                if (ceiling) {
                    y = Math.min(playerY + 2, level.getMaxBuildHeight() - 1);
                    while (y > minY && !chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
                    while (y > minY && chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
                    y += 1;
                } else {
                    y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                }
                int top = y - 1;
                BlockState st = chunk.getBlockState(pos.set(wx, top, wz));
                while (top > minY && st.getMapColor(level, pos) == MapColor.NONE && st.getFluidState().isEmpty()) {
                    top--;
                    st = chunk.getBlockState(pos.set(wx, top, wz));
                }
                int idx = z * 16 + x;
                hs[idx] = top;
                MapColor.Brightness b;
                MapColor color;
                if (!st.getFluidState().isEmpty() && st.getFluidState().is(FluidTags.WATER)) {
                    int d = 0, yy = top;
                    while (yy > minY && d < 12 && chunk.getBlockState(pos.set(wx, yy, wz)).getFluidState().is(FluidTags.WATER)) { yy--; d++; }
                    b = d < 3 ? MapColor.Brightness.HIGH : d < 6 ? MapColor.Brightness.NORMAL : d < 9 ? MapColor.Brightness.LOW : MapColor.Brightness.LOWEST;
                    color = MapColor.WATER;
                } else {
                    color = st.getMapColor(level, pos.set(wx, top, wz));
                    int north = z > 0 ? hs[(z - 1) * 16 + x] : top;
                    b = top > north ? MapColor.Brightness.HIGH : top < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                }
                int abgr = color == MapColor.NONE ? 0xFF000000 : color.calculateRGBColor(b);
                t.abgr[idx] = abgr == 0 ? 0xFF000001 : abgr;
                t.h[idx] = (short) top;
            }
        }
    }

    // ----------------------------------------------------------- texturas (mapa-mundi)

    public static ResourceLocation texture(Tile t) {
        Minecraft mc = Minecraft.getInstance();
        if (t.tex == null) {
            t.tex = new DynamicTexture(new NativeImage(16, 16, false));
            t.texId = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "explored/t" + (texCounter++));
            mc.getTextureManager().register(t.texId, t.tex);
            t.tex.setFilter(false, false);   // pixels nitidos (sem suavizar)
            t.texDirty = true;
        }
        if (t.texDirty) {
            NativeImage img = t.tex.getPixels();
            for (int i = 0; i < 256; i++) img.setPixelRGBA(i & 15, i >> 4, t.abgr[i] == 0 ? 0xFF1A1A1A : t.abgr[i]);
            t.tex.upload();
            t.texDirty = false;
        }
        return t.texId;
    }

    // ----------------------------------------------------------- disco

    public static void saveAndClear() {
        save();
        Minecraft mc = Minecraft.getInstance();
        for (Tile t : TILES.values()) if (t.texId != null) mc.getTextureManager().release(t.texId);
        TILES.clear();
        MapWaypoints.clear();
        curWorld = null;
        curDim = null;
    }

    private static Path file() { return worldDir().resolve(curDim + ".bin"); }

    private static void save() {
        if (curWorld == null || curDim == null || TILES.isEmpty()) return;
        try {
            Files.createDirectories(worldDir());
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(file()))))) {
                out.writeInt(1);
                out.writeInt(TILES.size());
                for (Map.Entry<Long, Tile> e : TILES.entrySet()) {
                    out.writeInt((int) (long) e.getKey());
                    out.writeInt((int) (e.getKey() >> 32));
                    for (int c : e.getValue().abgr) out.writeInt(c);
                    for (short h : e.getValue().h) out.writeShort(h);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void load() {
        TILES.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(f))))) {
            if (in.readInt() != 1) return;
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int cx = in.readInt(), cz = in.readInt();
                Tile t = new Tile();
                for (int k = 0; k < 256; k++) t.abgr[k] = in.readInt();
                for (int k = 0; k < 256; k++) t.h[k] = in.readShort();
                TILES.put(key(cx, cz), t);
            }
        } catch (IOException ignored) {
        }
    }
}

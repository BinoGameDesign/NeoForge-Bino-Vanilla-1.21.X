package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
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
        // copas de arvores: bit0 = ha copa (folhas/tronco no topo), bit1 = a coluna tem tronco
        public final byte[] canopy = new byte[256];
        public final short[] gh = new short[256];      // altura do chao (sob a copa); sem copa = h
        public final short[] cb = new short[256];      // altura da folha mais baixa da copa
        public final int[] gcol = new int[256];        // cor do chao sob a copa (ABGR)
        public final int[] tcol = new int[256];        // cor do tronco (ABGR)
        // texturas: id do estado do bloco (Block.getId) do topo, do chao, do tronco e do bloco abaixo do chao; tintas do bioma (RGB)
        public final int[] sTop = new int[256], sGround = new int[256], sTrunk = new int[256], sFill = new int[256];
        public final int[] tTop = new int[256], tGround = new int[256];
        public final int[] sDeep = new int[256];      // bloco 4 abaixo do chao (penhascos: pedra embaixo da terra)
        public final int[] sPlant = new int[256], tPlant = new int[256];   // planta em cima do chao (grama, flores...) + tinta
        // chao DESCOBERTO pelo jogador: onde ele andou (ou chegou perto) embaixo de arvores e em entradas de caverna.
        // Tem prioridade sobre o que a leitura automatica acha. NONE = nunca descoberto.
        public final short[] vFloor = new short[256], vRoof = new short[256];
        public final int[] vState = new int[256], vTint = new int[256], vFill = new int[256];
        Tile() { java.util.Arrays.fill(vFloor, NONE); java.util.Arrays.fill(vRoof, NONE); }
        long lastScan;
        public int version;                 // sobe a cada nova leitura (invalida malhas 3D em cache)
        boolean texDirty = true;
        DynamicTexture tex;
        ResourceLocation texId;
    }

    public static final int UNKNOWN = Integer.MIN_VALUE;
    public static final short NONE = Short.MIN_VALUE;
    private static final int PER_RUN_BUDGET = 12;      // chunks lidos por rodada
    // le todos os chunks carregados (a render distance inteira): os mesmos dados alimentam minimapa, mapa-mundi e a distancia extra
    private static int scanRadius() { return Math.max(7, Math.min(32, Minecraft.getInstance().options.getEffectiveRenderDistance())); }
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
        discover(level, p);

        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4, budget = PER_RUN_BUDGET;
        int scanR = scanRadius();
        for (int d = 0; d <= scanR && budget > 0; d++) {
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
                    t.version++;
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
                    // LevelChunk.getHeight devolve o Y do bloco mais alto; +1 = primeiro ar (o "top = y - 1" abaixo volta ao bloco)
                    y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
                }
                int top = y - 1;
                BlockState st = chunk.getBlockState(pos.set(wx, top, wz));
                while (top > minY && BlockLooks.empty(st, level, pos)) {   // ignora ar, grama alta, flores, tochas, vitoria-regia, tapetes...
                    top--;
                    st = chunk.getBlockState(pos.set(wx, top, wz));
                }
                int idx = z * 16 + x;
                hs[idx] = top;
                int argb, waterBed = Integer.MIN_VALUE, bedArgb = 0;
                if (!st.getFluidState().isEmpty() && st.getFluidState().is(FluidTags.WATER)) {
                    int d = 0, yy = top;
                    while (yy > minY && d < 64 && chunk.getBlockState(pos.set(wx, yy, wz)).getFluidState().is(FluidTags.WATER)) { yy--; d++; }   // oceano fundo: ate 64
                    argb = shade(BlockLooks.top(st, level, pos.set(wx, top, wz)), Math.max(0.60f, 1.05f - Math.min(d, 14) * 0.03f));
                    waterBed = yy;
                    bedArgb = BlockLooks.top(chunk.getBlockState(pos.set(wx, yy, wz)), level, pos);
                } else {
                    argb = BlockLooks.top(st, level, pos.set(wx, top, wz));
                    int north = z > 0 ? hs[(z - 1) * 16 + x] : top;
                    argb = shade(argb, top > north ? 1.10f : top < north ? 0.88f : 1.0f);
                }
                int abgr = 0xFF000000 | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
                t.abgr[idx] = abgr;
                // blocos de verdade (para as texturas)
                BlockState topState = st.getFluidState().isEmpty() ? st : st.getFluidState().createLegacyBlock();
                t.sTop[idx] = Block.getId(topState);
                t.tTop[idx] = BlockLooks.tint(topState, level, pos.set(wx, top, wz));
                t.sGround[idx] = t.sTop[idx];
                t.tGround[idx] = t.tTop[idx];
                t.sTrunk[idx] = 0;
                BlockState below = chunk.getBlockState(pos.set(wx, top - 1, wz));
                t.sFill[idx] = below.isAir() || !below.getFluidState().isEmpty() ? 0 : Block.getId(below);
                t.sDeep[idx] = deepId(chunk, pos, wx, top - 4, wz);
                plant(level, chunk, t, idx, pos, wx, top + 1, wz);
                t.h[idx] = (short) top;
                t.canopy[idx] = 0;
                t.tcol[idx] = 0;
                if (st.getFluidState().isEmpty()) {   // lateral propria (grama: terra + faixa verde, bit 8)
                    int sc = BlockLooks.side(st, level, pos.set(wx, top, wz));
                    if (BlockLooks.hasFringe(sc)) t.canopy[idx] = 8;
                    t.tcol[idx] = 0xFF000000 | ((sc & 0xFF) << 16) | (sc & 0xFF00) | ((sc >> 16) & 0xFF);
                }
                t.gh[idx] = (short) top;
                t.cb[idx] = (short) top;
                if (waterBed != Integer.MIN_VALUE) {   // agua: guarda o leito (altura + cor) para desenhar a agua translucida por cima
                    t.canopy[idx] = 4;
                    t.gh[idx] = (short) waterBed;
                    t.gcol[idx] = 0xFF000000 | ((bedArgb & 0xFF) << 16) | (bedArgb & 0xFF00) | ((bedArgb >> 16) & 0xFF);
                    BlockState bed = chunk.getBlockState(pos.set(wx, waterBed, wz));
                    t.sGround[idx] = Block.getId(bed);
                    t.tGround[idx] = BlockLooks.tint(bed, level, pos);
                    BlockState under = chunk.getBlockState(pos.set(wx, waterBed - 1, wz));
                    t.sFill[idx] = under.isAir() || !under.getFluidState().isEmpty() ? 0 : Block.getId(under);
        t.sDeep[idx] = deepId(chunk, pos, wx, y - 4, wz);
        plant(level, chunk, t, idx, pos, wx, y + 1, wz);
                    t.sDeep[idx] = deepId(chunk, pos, wx, waterBed - 4, wz);
                    t.sPlant[idx] = 0;
                }
                if (st.getFluidState().isEmpty() && (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS))) scanCanopy(level, chunk, t, idx, wx, wz, top, minY, pos);
                else if (!ceiling && st.getFluidState().isEmpty() && waterBed == Integer.MIN_VALUE) overhang(level, chunk, t, idx, wx, wz, top, minY, pos);
            }
        }
    }

    // teto de caverna / beirada (entrada lateral de caverna): o topo e' uma "laje" fina com ar embaixo.
    // Vira uma coluna de duas partes (como a copa das arvores): a laje em cima (bit 16) e o chao da caverna embaixo,
    // para a entrada da caverna aparecer aberta em vez de uma parede ate o chao.
    private static void overhang(ClientLevel level, LevelChunk chunk, Tile t, int idx, int wx, int wz, int top, int minY,
                                 BlockPos.MutableBlockPos pos) {
        int y = top - 1, k = 1;
        while (k < 10 && y > minY) {
            BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
            if (BlockLooks.empty(s, level, pos)) break;
            y--; k++;
        }
        if (k >= 10) return;                                              // terreno macico: coluna normal
        BlockState a2 = chunk.getBlockState(pos.set(wx, y - 1, wz));
        if (!BlockLooks.empty(a2, level, pos)) return;   // vao de 1 bloco: ignora
        int roofBottom = y + 1;
        int limit = Math.max(minY, y - 48);
        while (y > limit) {
            BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
            if (!s.getFluidState().isEmpty()) break;
            boolean empty = BlockLooks.empty(s, level, pos);
            if (!empty) {
                BlockState b = chunk.getBlockState(pos.set(wx, y - 1, wz));
                if (!b.isAir() && (!b.getFluidState().isEmpty() || !b.getCollisionShape(level, pos).isEmpty())) break;
            }
            y--;
        }
        if (y <= limit) return;
        BlockState ground = chunk.getBlockState(pos.set(wx, y, wz));
        int gc = BlockLooks.top(ground, level, pos);
        t.canopy[idx] = (byte) (1 | 16);
        t.gh[idx] = (short) y;
        t.cb[idx] = (short) roofBottom;
        t.gcol[idx] = 0xFF000000 | ((gc & 0xFF) << 16) | (gc & 0xFF00) | ((gc >> 16) & 0xFF);
        t.sGround[idx] = Block.getId(ground);
        t.tGround[idx] = BlockLooks.tint(ground, level, pos);
        BlockState under = chunk.getBlockState(pos.set(wx, y - 1, wz));
        t.sFill[idx] = under.isAir() || !under.getFluidState().isEmpty() ? 0 : Block.getId(under);
        t.sDeep[idx] = deepId(chunk, pos, wx, y - 4, wz);
        t.sTrunk[idx] = 0;
        plant(level, chunk, t, idx, pos, wx, y + 1, wz);
    }

    // ----------------------------------------------------------- chao descoberto pelo jogador

    private static final int DISCOVER_RADIUS = 5;

    // em volta do jogador, acha o piso "andavel" perto da altura dele (bloco solido com 2 de espaco livre em cima).
    // Se esse piso estiver embaixo de alguma coisa (copa de arvore, teto fino de caverna), grava como chao descoberto.
    private static void discover(ClientLevel level, LocalPlayer p) {
        int px = p.getBlockX(), py = p.getBlockY(), pz = p.getBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -DISCOVER_RADIUS; dz <= DISCOVER_RADIUS; dz++) {
            for (int dx = -DISCOVER_RADIUS; dx <= DISCOVER_RADIUS; dx++) {
                if (dx * dx + dz * dz > DISCOVER_RADIUS * DISCOVER_RADIUS) continue;
                int wx = px + dx, wz = pz + dz;
                Tile t = TILES.get(key(wx >> 4, wz >> 4));
                LevelChunk ch = level.getChunkSource().getChunkNow(wx >> 4, wz >> 4);
                if (t == null || ch == null) continue;
                int i = (wz & 15) * 16 + (wx & 15);
                if (t.abgr[i] == 0) continue;
                int top = t.h[i];
                // piso andavel perto da altura do jogador
                int floor = Integer.MIN_VALUE;
                for (int y = py + 1; y >= py - 3; y--) {
                    if (walkable(ch, level, pos, wx, y, wz)) { floor = y; break; }
                }
                if (floor == Integer.MIN_VALUE || floor >= top) continue;           // ceu aberto: nada a descobrir
                // o que tem em cima: primeiro bloco solido acima do piso
                int roof = Integer.MIN_VALUE;
                for (int y = floor + 3; y <= top; y++) {
                    BlockState s = ch.getBlockState(pos.set(wx, y, wz));
                    if (!passable(s, level, pos)) { roof = y; break; }
                }
                if (roof == Integer.MIN_VALUE) continue;
                BlockState roofState = ch.getBlockState(pos.set(wx, roof, wz));
                boolean tree = roofState.is(BlockTags.LEAVES) || roofState.is(BlockTags.LOGS);
                if (!tree && top - roof > 12) continue;                          // tunel fundo: nao mexe no mapa de cima
                if (t.vFloor[i] == floor && t.vRoof[i] == roof) continue;        // nada mudou
                BlockState fs = ch.getBlockState(pos.set(wx, floor, wz));
                t.vFloor[i] = (short) floor;
                t.vRoof[i] = (short) roof;
                t.vState[i] = Block.getId(fs);
                t.vTint[i] = BlockLooks.tint(fs, level, pos);
                BlockState under = ch.getBlockState(pos.set(wx, floor - 1, wz));
                t.vFill[i] = under.isAir() || !under.getFluidState().isEmpty() ? 0 : Block.getId(under);
                t.version++;                                                     // remonta a malha do mapa-mundi
            }
        }
    }

    private static boolean passable(BlockState s, ClientLevel level, BlockPos pos) {
        return s.isAir() || BlockLooks.invisible(s) || (s.getFluidState().isEmpty() && s.getCollisionShape(level, pos).isEmpty());
    }

    private static boolean walkable(LevelChunk ch, ClientLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        BlockState s = ch.getBlockState(pos.set(x, y, z));
        if (passable(s, level, pos) || s.is(BlockTags.LEAVES) || !s.getFluidState().isEmpty()) return false;
        BlockState a1 = ch.getBlockState(pos.set(x, y + 1, z));
        if (!passable(a1, level, pos)) return false;
        BlockState a2 = ch.getBlockState(pos.set(x, y + 2, z));
        return passable(a2, level, pos);
    }

    // chao descoberto na coluna (ou UNKNOWN)
    public static int discoveredFloor(int x, int z) {
        Tile t = TILES.get(key(x >> 4, z >> 4));
        if (t == null) return UNKNOWN;
        int i = (z & 15) * 16 + (x & 15);
        return t.vFloor[i] == NONE ? UNKNOWN : t.vFloor[i];
    }

    // grama alta, flores, samambaias... (sem colisao) logo acima do chao
    private static void plant(ClientLevel level, LevelChunk chunk, Tile t, int idx, BlockPos.MutableBlockPos pos, int wx, int y, int wz) {
        BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
        boolean ok = !s.isAir() && !BlockLooks.invisible(s) && s.getFluidState().isEmpty() && s.getCollisionShape(level, pos).isEmpty()
                && s.getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL;
        t.sPlant[idx] = ok ? Block.getId(s) : 0;
        t.tPlant[idx] = ok ? BlockLooks.tint(s, level, pos) : 0xFFFFFF;
    }

    private static int deepId(LevelChunk chunk, BlockPos.MutableBlockPos pos, int wx, int y, int wz) {
        BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
        return s.isAir() || !s.getFluidState().isEmpty() ? 0 : Block.getId(s);
    }

    // arvore: descobre onde estao as folhas, se ha tronco e onde fica o chao embaixo da copa
    private static void scanCanopy(ClientLevel level, LevelChunk chunk, Tile t, int idx, int wx, int wz, int top, int minY,
                                   BlockPos.MutableBlockPos pos) {
        // 1) copa de cima: folhas/troncos contiguos a partir do topo
        int y = top, lowestLeaf = Integer.MIN_VALUE, trunkCol = 0;
        boolean sawLog = false, lastWasLog = false;
        BlockState logState = null;
        while (y > minY) {
            BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
            if (s.is(BlockTags.LEAVES)) { if (!sawLog) lowestLeaf = y; lastWasLog = false; }
            else if (s.is(BlockTags.LOGS)) { if (!sawLog) { sawLog = true; logState = s; trunkCol = BlockLooks.side(s, level, pos); } lastWasLog = true; }
            else break;
            y--;
        }
        // tronco de verdade = os troncos descem direto ate o chao (galho solto no alto nao vira tronco falso)
        BlockState stop = chunk.getBlockState(pos.set(wx, y, wz));
        boolean trunk = sawLog && lastWasLog && !stop.isAir() && !stop.getCollisionShape(level, pos).isEmpty();
        t.sTrunk[idx] = trunk && logState != null ? Block.getId(logState) : 0;
        // 2) desce ate o chao de verdade: atravessa ar, plantas e tambem galhos e cachos de folhas mais baixos
        //    (antes um galho ou cacho de folhas embaixo da copa virava "chao" e aparecia um pilar ate o solo)
        //    (colmeias, galhos e cachos de folhas pendurados tambem): chao = bloco solido com algo solido embaixo
        int limit = Math.max(minY, y - 60);
        while (y > limit) {
            BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
            if (!s.getFluidState().isEmpty()) break;                         // agua: e' o chao
            boolean empty = BlockLooks.empty(s, level, pos);
            if (!empty && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS)) {
                BlockState b = chunk.getBlockState(pos.set(wx, y - 1, wz));
                boolean solid = !b.isAir() && (!b.getFluidState().isEmpty() || !b.getCollisionShape(level, pos).isEmpty());
                if (solid) break;                                            // chao de verdade
            }
            y--;
        }
        BlockState ground = chunk.getBlockState(pos.set(wx, y, wz));
        int gc = BlockLooks.top(ground, level, pos);
        t.sGround[idx] = Block.getId(ground);
        t.tGround[idx] = BlockLooks.tint(ground, level, pos);
        BlockState under = chunk.getBlockState(pos.set(wx, y - 1, wz));
        t.sFill[idx] = under.isAir() || !under.getFluidState().isEmpty() ? 0 : Block.getId(under);
        t.canopy[idx] = (byte) (1 | (trunk ? 2 : 0));
        t.gh[idx] = (short) y;
        t.cb[idx] = (short) (lowestLeaf == Integer.MIN_VALUE ? top : lowestLeaf);
        t.gcol[idx] = 0xFF000000 | ((gc & 0xFF) << 16) | (gc & 0xFF00) | ((gc >> 16) & 0xFF);
        t.tcol[idx] = 0xFF000000 | ((trunkCol & 0xFF) << 16) | (trunkCol & 0xFF00) | ((trunkCol >> 16) & 0xFF);
    }

    private static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f)), g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f)),
                b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
        WorldMap3D.clearAll();
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
                out.writeInt(9);
                out.writeInt(TILES.size());
                for (Map.Entry<Long, Tile> e : TILES.entrySet()) {
                    out.writeInt((int) (long) e.getKey());
                    out.writeInt((int) (e.getKey() >> 32));
                    for (int c : e.getValue().abgr) out.writeInt(c);
                    for (short h : e.getValue().h) out.writeShort(h);
                    Tile tt = e.getValue();
                    for (int k = 0; k < 256; k++) {
                        out.writeByte(tt.canopy[k]);
                        out.writeShort(tt.gh[k]);
                        out.writeShort(tt.cb[k]);
                        out.writeInt(tt.gcol[k]);
                        out.writeInt(tt.tcol[k]);
                        out.writeInt(tt.sTop[k]); out.writeInt(tt.sGround[k]); out.writeInt(tt.sTrunk[k]); out.writeInt(tt.sFill[k]);
                        out.writeInt(tt.tTop[k]); out.writeInt(tt.tGround[k]); out.writeInt(tt.sDeep[k]);
                        out.writeInt(tt.sPlant[k]); out.writeInt(tt.tPlant[k]);
                        out.writeShort(tt.vFloor[k]); out.writeShort(tt.vRoof[k]);
                        out.writeInt(tt.vState[k]); out.writeInt(tt.vTint[k]); out.writeInt(tt.vFill[k]);
                    }
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
            int ver = in.readInt();
            if (ver != 8 && ver != 9) return;   // versoes antigas tinham arvores lidas errado (pilares): o mapa se refaz   // formatos antigos eram lidos 1 bloco abaixo da superficie (terra no lugar da grama): descartados, o mapa se refaz
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int cx = in.readInt(), cz = in.readInt();
                Tile t = new Tile();
                for (int k = 0; k < 256; k++) t.abgr[k] = in.readInt();
                for (int k = 0; k < 256; k++) t.h[k] = in.readShort();
                if (ver >= 3) {
                    for (int k = 0; k < 256; k++) {
                        t.canopy[k] = in.readByte(); t.gh[k] = in.readShort(); t.cb[k] = in.readShort();
                        t.gcol[k] = in.readInt(); t.tcol[k] = in.readInt();
                        if (ver >= 5) {
                            t.sTop[k] = in.readInt(); t.sGround[k] = in.readInt(); t.sTrunk[k] = in.readInt(); t.sFill[k] = in.readInt();
                            t.tTop[k] = in.readInt(); t.tGround[k] = in.readInt();
                            if (ver >= 6) t.sDeep[k] = in.readInt();
                            if (ver >= 7) { t.sPlant[k] = in.readInt(); t.tPlant[k] = in.readInt(); }
                            if (ver >= 9) {
                                t.vFloor[k] = in.readShort(); t.vRoof[k] = in.readShort();
                                t.vState[k] = in.readInt(); t.vTint[k] = in.readInt(); t.vFill[k] = in.readInt();
                            }
                        }
                    }
                } else {
                    for (int k = 0; k < 256; k++) { t.gh[k] = t.h[k]; t.cb[k] = t.h[k]; }
                }
                TILES.put(key(cx, cz), t);
            }
        } catch (IOException ignored) {
        }
    }
}

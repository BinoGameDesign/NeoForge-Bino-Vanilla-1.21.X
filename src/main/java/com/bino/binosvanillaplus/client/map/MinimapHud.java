package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.joml.Matrix4f;

import java.util.Arrays;

// Minimapa "diorama" no canto inferior DIREITO. Cada coluna do terreno guarda ate 4 "blocos" empilhados
// (copa, tronco, chao), entao arvores aparecem inteiras: copa + tronco + chao. Desenhado em perspectiva inclinada,
// gira com o jogador, bordas esmaecem. So aparece se houver um mapa na hotbar.
public class MinimapHud {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "minimap");

    // ---- ajustes (mude aqui) ----
    static final int R = 16;           // raio em blocos
    private static final int BOX = 84;         // tamanho da caixa em pixels
    private static final float S = 2.5f;       // pixels por bloco
    private static final float TILT = 0.55f;   // 1 = vista de cima, menor = mais inclinado
    private static final float HS = 1.6f;      // pixels por bloco de altura
    private static final int MARGIN_X = 6, MARGIN_Y = 6;

    static final int N = 2 * R + 1, MAXR = 4;
    static final int[] RC = new int[N * N];                 // quantidade de blocos empilhados
    static final int[][] RTOP = new int[N * N][MAXR], RBOT = new int[N * N][MAXR], RCOL = new int[N * N][MAXR];
    static final boolean[] RGROUND = new boolean[N * N];    // o ultimo bloco e' o chao?
    private static final int[] GROUND = new int[N * N];             // altura do chao (UNKNOWN se nao ha)
    private static final long[] ORDER = new long[N * N];
    private static final int BG = 0x0B0F14, WATER = 0xFF3F6FD8;

    // ------------------------------------------------------------------ amostragem (a cada 3 ticks)

    public static void sample(Minecraft mc) {
        LocalPlayer p = mc.player;
        ClientLevel lvl = mc.level;
        if (p == null || lvl == null) return;
        int px = p.getBlockX(), pz = p.getBlockZ(), py = p.getBlockY();
        int minY = lvl.getMinBuildHeight();
        boolean ceiling = lvl.dimensionType().hasCeiling();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dz = -R; dz <= R; dz++) {
            for (int dx = -R; dx <= R; dx++) {
                int i = (dz + R) * N + (dx + R);
                RC[i] = 0;
                RGROUND[i] = false;
                GROUND[i] = ExploredMap.UNKNOWN;
                int wx = px + dx, wz = pz + dz;
                LevelChunk ch = lvl.getChunkSource().getChunkNow(wx >> 4, wz >> 4);
                if (ch == null) continue;

                int y;
                if (ceiling) {
                    y = Math.min(py + 2, lvl.getMaxBuildHeight() - 1);
                    while (y > minY && !ch.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
                    while (y > minY && ch.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
                } else {
                    y = ch.getHeight(Heightmap.Types.WORLD_SURFACE, wx & 15, wz & 15) - 1;
                }
                int limit = Math.max(minY, y - 48);
                int runs = 0, curTop = Integer.MIN_VALUE, curCol = 0;
                boolean curCanopy = false;

                for (; y >= limit && runs < MAXR; y--) {
                    BlockState st = ch.getBlockState(pos.set(wx, y, wz));
                    MapColor mc2 = st.getMapColor(lvl, pos);
                    boolean water = !st.getFluidState().isEmpty() && st.getFluidState().is(FluidTags.WATER);
                    boolean air = st.isAir() || (mc2 == MapColor.NONE && !water);
                    int color = 0;
                    boolean canopy = false;
                    if (!air) {
                        color = water ? WATER : toArgb(mc2.calculateRGBColor(MapColor.Brightness.NORMAL));
                        canopy = !water && (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS)
                                || st.getCollisionShape(lvl, pos).isEmpty());
                    }
                    if (curTop != Integer.MIN_VALUE && (air || color != curCol)) {
                        // fecha o bloco atual
                        RTOP[i][runs] = curTop; RBOT[i][runs] = y + 1; RCOL[i][runs] = curCol; runs++;
                        curTop = Integer.MIN_VALUE;
                        if (!curCanopy) { RGROUND[i] = true; break; }   // chegou ao chao: para
                    }
                    if (!air && curTop == Integer.MIN_VALUE && runs < MAXR) {
                        curTop = y; curCol = color; curCanopy = canopy;
                    }
                }
                if (curTop != Integer.MIN_VALUE && runs < MAXR) {
                    RTOP[i][runs] = curTop; RBOT[i][runs] = Math.max(minY, y + 1); RCOL[i][runs] = curCol; runs++;
                    if (!curCanopy) RGROUND[i] = true;
                }
                RC[i] = runs;
                if (runs > 0) GROUND[i] = RGROUND[i] ? RTOP[i][runs - 1] : RBOT[i][runs - 1];
                // o chao e' desenhado com so 1 bloco de espessura (as laterais cuidam do resto)
                if (RGROUND[i]) RBOT[i][runs - 1] = RTOP[i][runs - 1];
            }
        }
        DioramaRenderer.rebuild(R, N, RC, RTOP, RBOT, RCOL, RGROUND, px, py, pz);
    }

    private static int toArgb(int abgr) {
        return (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
    }

    // ------------------------------------------------------------------ desenho

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || !com.bino.binosvanillaplus.map.MapGate.hasMap(p)) return;

        int x0 = g.guiWidth() - BOX - MARGIN_X, y0 = g.guiHeight() - BOX - MARGIN_Y;
        int cx = x0 + BOX / 2, cy = y0 + BOX / 2 + 8;
        int px = p.getBlockX(), pz = p.getBlockZ(), py = p.getBlockY();

        g.fill(x0 - 2, y0 - 2, x0 + BOX + 2, y0 + BOX + 2, 0xFF1B1B1B);
        g.fill(x0 - 1, y0 - 1, x0 + BOX + 1, y0 + BOX + 1, 0xFF6E6E6E);
        g.fill(x0, y0, x0 + BOX, y0 + BOX, 0xFF000000 | BG);
        g.flush();

        if (DioramaRenderer.ready()) {
            renderGpu(g, mc, p, x0, y0);
            return;
        }

        double yaw = Math.toRadians(p.getYRot());
        float fx = (float) -Math.sin(yaw), fz = (float) Math.cos(yaw);     // frente do jogador
        float rx = (float) -Math.cos(yaw), rz = (float) -Math.sin(yaw);    // direita do jogador

        int count = 0;
        for (int dz = -R; dz <= R; dz++) {
            for (int dx = -R; dx <= R; dx++) {
                int i = (dz + R) * N + (dx + R);
                float f = dx * fx + dz * fz;
                ORDER[count++] = ((long) (int) ((f + 100f) * 1000f) << 32) | i;
            }
        }
        Arrays.sort(ORDER, 0, count);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        g.enableScissor(x0, y0, x0 + BOX, y0 + BOX);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int quads = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int k = count - 1; k >= 0; k--) {          // de tras para frente
            int i = (int) (ORDER[k] & 0xFFFFFFFFL);
            int n = RC[i];
            if (n == 0) continue;
            int dx = i % N - R, dz = i / N - R;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float fade = Math.max(0f, Math.min(1f, (R + 0.5f - dist) / 4f));
            float mix = 0.35f + 0.65f * fade;

            for (int r = n - 1; r >= 0; r--) {          // do mais baixo (chao) para o mais alto (copa)
                int top = RTOP[i][r] + 1, bot = RBOT[i][r];
                boolean ground = RGROUND[i] && r == n - 1;
                float hTop = clampH(top - py);
                int col = fadeColor(RCOL[i][r], mix);

                for (int[] d : dirs) {
                    if (d[0] * fx + d[1] * fz >= -0.01f) continue;       // so laterais voltadas para o jogador
                    int ni = (dz + d[1] + R) * N + (dx + d[0] + R);
                    boolean inside = Math.abs(dx + d[0]) <= R && Math.abs(dz + d[1]) <= R;
                    int sideBot;
                    if (ground) {
                        int ng = inside && GROUND[ni] != ExploredMap.UNKNOWN ? GROUND[ni] : top - 9;
                        sideBot = ng + 1;
                        if (sideBot >= top) continue;
                    } else sideBot = bot;
                    float hBot = clampH(sideBot - py);
                    float tx = -d[1] * 0.5f, tz = d[0] * 0.5f, ex = dx + d[0] * 0.5f, ez = dz + d[1] * 0.5f;
                    quads += quad(b, m, cx, cy, fx, fz, rx, rz, ex + tx, ez + tz, hTop, ex - tx, ez - tz, hTop,
                            ex - tx, ez - tz, hBot, ex + tx, ez + tz, hBot, shade(col, 0.68f));
                }
                quads += quad(b, m, cx, cy, fx, fz, rx, rz, dx - .5f, dz - .5f, hTop, dx + .5f, dz - .5f, hTop,
                        dx + .5f, dz + .5f, hTop, dx - .5f, dz + .5f, hTop, col);
            }
        }
        if (quads > 0) BufferUploader.drawWithShader(b.buildOrThrow());
        g.disableScissor();
        RenderSystem.enableCull();

        // marcacoes do mapa-mundi
        for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
            if (w.hidden || !MapOptions.minimapPins) continue;
            float dx = w.x + 0.5f - p.getBlockX(), dz = w.z + 0.5f - p.getBlockZ();
            float sx = (dx * rx + dz * rz) * S, sy = -(dx * fx + dz * fz) * S * TILT;
            int lim = BOX / 2 - 4;
            float lx = Math.max(-lim, Math.min(lim, sx)), ly = Math.max(-lim + 6, Math.min(lim - 6, sy));
            int wx = (int) (x0 + BOX / 2 + lx), wy = (int) (cy - 8 + ly);
            g.fill(wx - 2, wy - 2, wx + 3, wy + 3, 0xFF000000);
            g.fill(wx - 1, wy - 1, wx + 2, wy + 2, w.color);
        }

        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF000000);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        float nr = (float) Math.sin(yaw), nf = (float) -Math.cos(yaw);
        int nx = (int) (x0 + BOX / 2 + nr * (BOX / 2 - 7)), ny = (int) (y0 + BOX / 2 - nf * (BOX / 2 - 7) * 0.9f);
        g.drawCenteredString(mc.font, "N", nx, ny - 4, 0xFFFF5555);
    }

    // ---- caminho GPU: malha 3D + shader proprio (DioramaRenderer) ----
    private static void renderGpu(GuiGraphics g, Minecraft mc, LocalPlayer p, int x0, int y0) {
        MapOptions.ensureLoaded();
        DioramaRenderer.draw(g, p, x0, y0, BOX, R);

        if (MapOptions.minimapPins) {
            for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
                if (w.hidden) continue;
                float dx = (float) (w.x + 0.5 - p.getX()), dy = (float) (w.y - p.getY()), dz = (float) (w.z + 0.5 - p.getZ());
                float[] s = DioramaRenderer.project(dx, dy, dz, x0, y0, BOX);
                float cxm = x0 + BOX / 2f, cym = y0 + BOX / 2f, lim = BOX / 2f - 6f;
                float ox = s[0] - cxm, oy = s[1] - cym;
                if (s[2] == 0f) { ox = -ox; oy = -oy; }                       // atras da camera: empurra para a borda oposta
                float len = (float) Math.sqrt(ox * ox + oy * oy);
                boolean edge = s[2] == 0f || Math.abs(ox) > lim || Math.abs(oy) > lim;
                if (edge && len > 0.01f) {
                    float k = lim / Math.max(Math.abs(ox), Math.abs(oy));
                    ox *= k; oy *= k;
                }
                int sx = (int) (cxm + ox), sy = (int) (cym + oy);
                g.fill(sx - 4, sy - 4, sx + 5, sy + 5, edge ? 0xFFAAAAAA : 0xFF000000);
                g.fill(sx - 3, sy - 3, sx + 4, sy + 4, w.color);
                if (MapOptions.minimapLabels) g.drawCenteredString(mc.font, w.symbolOrInitial(), sx, sy - 4, 0xFFFFFFFF);
            }
        }
        double yaw = Math.toRadians(p.getYRot());
        float nr = (float) Math.sin(yaw), nf = (float) -Math.cos(yaw);
        int nx = (int) (x0 + BOX / 2 + nr * (BOX / 2 - 7)), ny = (int) (y0 + BOX / 2 - nf * (BOX / 2 - 7) * 0.9f);
        g.drawCenteredString(mc.font, "N", nx, ny - 4, 0xFFFF5555);
    }

    private static float clampH(float v) { return Math.max(-22f, Math.min(22f, v)); }

    private static int shade(int argb, float f) {
        int r = (int) (((argb >> 16) & 0xFF) * f), gr = (int) (((argb >> 8) & 0xFF) * f), bl = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    private static int fadeColor(int argb, float mix) {
        int r = (int) (((BG >> 16) & 0xFF) * (1 - mix) + ((argb >> 16) & 0xFF) * mix);
        int gr = (int) (((BG >> 8) & 0xFF) * (1 - mix) + ((argb >> 8) & 0xFF) * mix);
        int bl = (int) ((BG & 0xFF) * (1 - mix) + (argb & 0xFF) * mix);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    private static int quad(BufferBuilder b, Matrix4f m, int cx, int cy, float fx, float fz, float rx, float rz,
                            float ax, float az, float ah, float bx, float bz, float bh,
                            float cxx, float cz, float ch, float dx, float dz, float dh, int argb) {
        vtx(b, m, cx, cy, fx, fz, rx, rz, ax, az, ah, argb);
        vtx(b, m, cx, cy, fx, fz, rx, rz, bx, bz, bh, argb);
        vtx(b, m, cx, cy, fx, fz, rx, rz, cxx, cz, ch, argb);
        vtx(b, m, cx, cy, fx, fz, rx, rz, dx, dz, dh, argb);
        return 1;
    }

    private static void vtx(BufferBuilder b, Matrix4f m, int cx, int cy, float fx, float fz, float rx, float rz,
                            float dx, float dz, float h, int argb) {
        float sx = cx + (dx * rx + dz * rz) * S;
        float sy = cy - (dx * fx + dz * fz) * S * TILT - h * HS;
        b.addVertex(m, sx, sy, 0f).setColor(argb);
    }
}

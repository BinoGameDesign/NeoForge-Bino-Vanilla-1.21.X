package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

// Minimapa diorama no canto inferior direito. Cada coluna guarda ate 4 "blocos" empilhados (copa, tronco, chao) com a cor real
// do bloco (textura + cor do bioma). Em cavernas (ou no Nether) o mapa vira um CORTE na altura do jogador.
// Quem desenha e' o DioramaRenderer (malha na GPU + shader proprio).
public class MinimapHud {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "minimap");

    // ---- ajustes (mude aqui) ----
    static final int R = 20;                   // raio da malha, em blocos
    private static final int BOX = 76;         // tamanho da caixa em pixels
    private static final float[] DISTS = {22f, 30f, 42f};   // distancia da camera por nivel de zoom (teclas de zoom)
    private static final int MARGIN_X = 9, MARGIN_Y = 9;
    private static final int SLICE = 4;        // altura do corte (acima dos pes) em cavernas

    static final int N = 2 * R + 1, MAXR = 4;
    static final int[] RC = new int[N * N];
    static final int[][] RTOP = new int[N * N][MAXR], RBOT = new int[N * N][MAXR], RCOL = new int[N * N][MAXR], RSIDE = new int[N * N][MAXR];
    static final boolean[] RGROUND = new boolean[N * N];

    private static boolean cave = false;       // com historese, para nao piscar entre modos

    // ------------------------------------------------------------------ amostragem

    public static void sample(Minecraft mc) {
        LocalPlayer p = mc.player;
        ClientLevel lvl = mc.level;
        if (p == null || lvl == null || !DioramaRenderer.ready()) return;
        int px = p.getBlockX(), pz = p.getBlockZ(), py = p.getBlockY();
        int minY = lvl.getMinBuildHeight();
        boolean ceilingDim = lvl.dimensionType().hasCeiling();

        Sun.update(mc);
        int surf = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
        boolean roofed = !lvl.canSeeSky(p.blockPosition().above());
        int skyLight = lvl.getBrightness(net.minecraft.world.level.LightLayer.SKY, p.blockPosition().above());
        // entra no modo corte quando esta coberto e quase sem luz do ceu (caverna/mina/casa fechada); sai quando volta a ter ceu
        if (ceilingDim) cave = true;
        else if (!cave) cave = roofed && skyLight <= 5 && surf > py + 4;
        else cave = roofed && skyLight < 10 && surf > py + 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // altura do corte: logo abaixo do teto local (tunel baixo) ou 4 blocos acima da cabeca (caverna ampla)
        int slice = py + SLICE;
        if (cave) {
            for (int c = py + 2; c <= py + 8; c++) {
                BlockState above = lvl.getBlockState(pos.set(px, c, pz));
                if (!above.getCollisionShape(lvl, pos).isEmpty()) { slice = Math.max(py + 1, c - 1); break; }
            }
        }

        for (int dz = -R; dz <= R; dz++) {
            for (int dx = -R; dx <= R; dx++) {
                int i = (dz + R) * N + (dx + R);
                RC[i] = 0;
                RGROUND[i] = false;
                int wx = px + dx, wz = pz + dz;
                LevelChunk ch = lvl.getChunkSource().getChunkNow(wx >> 4, wz >> 4);
                if (ch == null) continue;

                int y = cave ? Math.min(slice, lvl.getMaxBuildHeight() - 1)
                             : ch.getHeight(Heightmap.Types.WORLD_SURFACE, wx & 15, wz & 15);   // LevelChunk.getHeight ja devolve o bloco mais alto (nao o ar acima)
                int limit = Math.max(minY, y - 56);
                int runs = 0;
                Object curKey = null;
                int curTop = 0, curCol = 0, curSide = 0;
                boolean curCanopy = false;

                for (; y >= limit && runs < MAXR; y--) {
                    BlockState st = ch.getBlockState(pos.set(wx, y, wz));
                    FluidState fs = st.getFluidState();
                    boolean fluid = !fs.isEmpty();
                    boolean air = st.isAir() || BlockLooks.invisible(st) || (!fluid && st.getCollisionShape(lvl, pos).isEmpty());   // ignora grama alta, flores, tochas...
                    Object key = air ? null : (fluid ? fs.getType() : st);

                    if (curKey != null && key != curKey) {
                        RTOP[i][runs] = curTop; RBOT[i][runs] = y + 1; RCOL[i][runs] = curCol; RSIDE[i][runs] = curSide; runs++;
                        curKey = null;
                        if (!curCanopy) { RGROUND[i] = true; break; }
                    }
                    if (key != null && curKey == null && runs < MAXR) {
                        curKey = key; curTop = y;
                        curCol = BlockLooks.top(st, lvl, pos);
                        curSide = BlockLooks.side(st, lvl, pos);
                        if (fluid) {   // agua escurece com a profundidade
                            int d = 0, yy = y;
                            while (yy > minY && d < 12 && ch.getBlockState(pos.set(wx, yy, wz)).getFluidState().isSource()) { yy--; d++; }
                            float f = Math.max(0.55f, 1.05f - d * 0.04f);
                            int alpha = Math.min(215, 120 + d * 12);   // quanto mais fundo, menos se enxerga o fundo
                            curCol = (alpha << 24) | (DioramaRenderer.scale(curCol, f) & 0xFFFFFF);
                            curSide = curCol;
                            pos.set(wx, y, wz);
                        }
                        curCanopy = fluid || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS);   // agua tambem deixa ver o leito por baixo
                    }
                }
                if (curKey != null && runs < MAXR) {
                    RTOP[i][runs] = curTop; RBOT[i][runs] = Math.max(minY, y + 1); RCOL[i][runs] = curCol; RSIDE[i][runs] = curSide; runs++;
                    if (!curCanopy) RGROUND[i] = true;
                }
                RC[i] = runs;
                if (RGROUND[i] && runs > 0) RBOT[i][runs - 1] = RTOP[i][runs - 1];
            }
        }
        DioramaRenderer.rebuild(R, N, RC, RTOP, RBOT, RCOL, RSIDE, RGROUND, px, py, pz);
    }

    // pet do jogador (companheiro do mod ou qualquer animal domado dele) nao aparece como ponto
    private static boolean isPet(Mob m, LocalPlayer p) {
        if (m instanceof com.bino.binosvanillaplus.entity.CompanionAllayEntity) return true;
        if (m instanceof net.minecraft.world.entity.OwnableEntity oe) {
            java.util.UUID id = oe.getOwnerUUID();
            return id != null && id.equals(p.getUUID());
        }
        return false;
    }

    // escala do texto pequeno do HUD: ~14 px reais de altura qualquer que seja a escala da interface
    static float smallScale(Minecraft mc) {
        double gs = mc.getWindow().getGuiScale();
        return (float) Math.max(0.5, Math.min(1.0, 2.0 / gs));
    }

    // ------------------------------------------------------------------ desenho

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || !com.bino.binosvanillaplus.map.MapGate.hasMap(p)) return;
        MapOptions.ensureLoaded();

        int a = (int) (255 * MapOptions.minimapOpacity / 100f) << 24;
        int x0 = g.guiWidth() - BOX - MARGIN_X, y0 = g.guiHeight() - BOX - MARGIN_Y;
        g.fill(x0 - 2, y0 - 2, x0 + BOX + 2, y0 + BOX + 2, a | 0x2E302F);   // moldura cinza escura (Figma)
        g.fill(x0 - 1, y0 - 1, x0 + BOX + 1, y0 + BOX + 1, a | 0x4A4D4B);
        g.fill(x0, y0, x0 + BOX, y0 + BOX, a | 0x0B0F14);
        if (!DioramaRenderer.ready()) return;

        float dist = DISTS[Math.max(0, Math.min(DISTS.length - 1, MapOptions.minimapZoom))];
        DioramaRenderer.draw(g, p, x0, y0, BOX, dist, dist * 0.62f);

        float cxm = x0 + BOX / 2f, cym = y0 + BOX / 2f;

        // mobs: hostis em vermelho, amigaveis em azul claro
        if (MapOptions.showMobs) {
            for (Mob m : mc.level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(R))) {
                if (!m.isAlive() || m.isInvisible() || isPet(m, p)) continue;
                float[] s = DioramaRenderer.project((float) (m.getX() - p.getX()), (float) (m.getY() - p.getY()) + 0.6f, (float) (m.getZ() - p.getZ()), x0, y0, BOX);
                if (s[2] == 0f || s[0] < x0 + 4 || s[0] > x0 + BOX - 4 || s[1] < y0 + 4 || s[1] > y0 + BOX - 4) continue;
                int col = m instanceof Enemy ? 0xFFFF3B30 : 0xFF8FD8FF;
                int sx = (int) s[0], sy = (int) s[1];
                g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000);
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, col);
            }
        }

        // marcacoes: icone pequeno acima do feixe de luz; fora da area, preso a borda
        if (MapOptions.minimapPins) {
            for (MapWaypoints.Waypoint w : MapWaypoints.list()) {
                if (w.hidden) continue;
                float dx = (float) (w.x + 0.5 - p.getX()), dy = (float) (w.y - p.getY()), dz = (float) (w.z + 0.5 - p.getZ());
                float[] s = DioramaRenderer.project(dx, dy + 5f, dz, x0, y0, BOX);
                float lim = BOX / 2f - 7f;
                float ox = s[0] - cxm, oy = s[1] - cym;
                boolean far = dx * dx + dz * dz > (R - 2) * (R - 2);
                boolean edge = far || s[2] == 0f || Math.abs(ox) > lim || Math.abs(oy) > lim;
                if (edge) {
                    // fora da maquete: vai para a borda, na direcao da marcacao vista de cima (a projecao em perspectiva
                    // falha para pontos atras da camera - era isso que jogava o icone em cima do ponteiro)
                    double yr = Math.toRadians(p.getYRot());
                    float c = (float) Math.cos(yr), sn = (float) Math.sin(yr);
                    ox = -dx * c - dz * sn;           // direita na tela
                    oy = -(-dx * sn + dz * c);        // para baixo na tela (frente do jogador = para cima)
                    float m = Math.max(Math.abs(ox), Math.abs(oy));
                    if (m < 0.001f) { ox = 0; oy = -lim; }
                    else { ox *= lim / m; oy *= lim / m; }
                }
                int sx = (int) (cxm + ox), sy = (int) (cym + oy);
                g.fill(sx - 6, sy - 6, sx + 6, sy + 6, edge ? 0xFFCCCCCC : 0xFF000000);
                WaypointIcons.badge(g, w.icon(), sx - 5, sy - 5, 10, w.color);
            }
        }
        double yaw = Math.toRadians(p.getYRot());
        float nr = (float) Math.sin(yaw), nf = (float) -Math.cos(yaw);
        int nx = (int) (x0 + BOX / 2 + nr * (BOX / 2 - 7)), ny = (int) (y0 + BOX / 2 - nf * (BOX / 2 - 7) * 0.9f);
        // "N" um pouco maior, branco com contorno preto
        g.pose().pushPose();
        g.pose().translate(nx, ny, 0);
        g.pose().scale(1.3f, 1.3f, 1f);
        int nw = mc.font.width("N");
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}})
            g.drawString(mc.font, "N", -nw / 2 + o[0], -4 + o[1], 0xFF000000, false);
        g.drawString(mc.font, "N", -nw / 2, -4, 0xFFFFFFFF, false);
        g.pose().popPose();
        if (cave) g.drawString(mc.font, "\u2193", x0 + 3, y0 + 3, 0xFFFFD84A, true);   // seta: modo corte de caverna

        // texto pequeno (tamanho fixo na tela, como no Figma): dia/hora em cima, coordenadas dentro embaixo
        float ts = smallScale(mc);
        String day = WorldMapScreen.dayTime(mc.level.getDayTime());
        float dw = mc.font.width(day) * ts;
        MapUi.text(g, mc.font, day, x0 + BOX / 2f - dw / 2f, y0 - 4 - 9 * ts, ts, 0xFFFFFFFF, true);
        String pos = p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ();
        float pw = mc.font.width(pos) * ts;
        MapUi.text(g, mc.font, pos, x0 + BOX / 2f - pw / 2f + 4, y0 + BOX - 3 - 8 * ts, ts, 0xFFFFFFFF, true);

        // icone do mapa no canto de baixo a direita, um pouco inclinado
        g.pose().pushPose();
        g.pose().translate(x0 + BOX - 5, y0 + BOX - 5, 150);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-18f));
        g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FILLED_MAP), -8, -8);
        g.pose().popPose();
    }
}

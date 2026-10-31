package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Vista 3D do mapa-mundi (estilo "3D view" do Diorama): o terreno ja explorado vira uma maquete 3D que voce gira e inclina.
// Cada chunk explorado e' uma malha na GPU (VertexBuffer) desenhada com o shader "diorama".
public class WorldMap3D {
    private static final float FOV = (float) Math.toRadians(50);
    private static final int BUILD_PER_FRAME = 14;

    public static float yaw = 0.0f, pitch = 0.95f, dist = 110f;
    public static Matrix4f view = new Matrix4f(), proj = new Matrix4f();
    public static double targetY = 64;                  // altura do alvo (suavizada)

    private static class ChunkMesh { VertexBuffer vb, wvb; int version = -1; boolean has, hasW; }
    private static final Map<Long, ChunkMesh> MESHES = new HashMap<>();

    public static void clearAll() {
        for (ChunkMesh m : MESHES.values()) { if (m.vb != null) m.vb.close(); if (m.wvb != null) m.wvb.close(); }
        MESHES.clear();
    }

    // desenha a maquete na area (x0,y0,w,h). tx,tz = ponto central no mundo.
    public static ShaderInstance depthShader;            // map_depth (passo de sombra)
    private static TextureTarget shadowTarget;
    private static final int SHADOW_RES = 2048;
    private static final float SHADOW_FAR = 1600f;

    // raio (em chunks) desenhado: cresce com o zoom para mostrar tudo o que ja foi explorado
    private static int viewChunks() { return Math.max(10, Math.min(48, (int) (dist * 2.2f / 16f) + 4)); }

    public static void render(GuiGraphics g, int x0, int y0, int w, int h, double tx, double tz, LocalPlayer player) {
        if (!DioramaRenderer.ready()) return;
        int th = ExploredMap.heightAt((int) Math.floor(tx), (int) Math.floor(tz));
        double goal = th == ExploredMap.UNKNOWN ? (player != null ? player.getY() : 64) : th;
        targetY += (goal - targetY) * 0.12;

        int vc = viewChunks();
        proj = new Matrix4f().perspective(FOV, w / (float) h, 0.5f, 4000f);
        view = new Matrix4f().translate(0, 0, -dist).rotateX(pitch).rotateY(yaw);

        // chunks explorados num CIRCULO em volta do alvo (monta as malhas que faltam, poucas por frame)
        int cxm = (int) Math.floor(tx / 16), czm = (int) Math.floor(tz / 16), built = 0;
        List<long[]> vis = new ArrayList<>();
        for (int dz = -vc; dz <= vc; dz++) {
            for (int dx = -vc; dx <= vc; dx++) {
                if (dx * dx + dz * dz > vc * vc) continue;
                int cx = cxm + dx, cz = czm + dz;
                ExploredMap.Tile t = ExploredMap.tile(cx, cz);
                if (t == null) continue;
                long key = ExploredMap.key(cx, cz);
                ChunkMesh m = MESHES.get(key);
                if (m == null) { m = new ChunkMesh(); MESHES.put(key, m); }
                if (m.version != t.version && built < BUILD_PER_FRAME) { build(m, t, cx, cz); built++; }
                if (m.has || m.hasW) vis.add(new long[]{cx, cz, dx * dx + dz * dz});
            }
        }

        g.flush();
        Minecraft mc = Minecraft.getInstance();
        Sun.update(mc);
        Matrix4f lightMat = new Matrix4f();
        float shadowHalf = Math.max(40f, Math.min(320f, dist * 1.15f + 24f));
        boolean shadowsOk = shadowPass(vis, tx, tz, shadowHalf, lightMat);

        DioramaRenderer.GlState st = DioramaRenderer.begin(g, x0, y0, w, h, proj);
        ShaderInstance sh = DioramaRenderer.shader;
        Sun.applyUniforms();
        sh.safeGetUniform("Exposure").set(1.0f);
        sh.safeGetUniform("Radius").set(1.0e6f);
        DioramaRenderer.drawSky(view, proj, tx, targetY, tz, 56f);   // nuvens mais baixas no mapa-mundi
        DioramaRenderer.applyFog(dist + vc * 16 * 0.45f, dist + vc * 16 * 0.92f, 0f, 0f);
        sh.safeGetUniform("Detail").set(1f);
        // qualidade "Complementary": sombra real, luz do sol/lua, agua com reflexo
        sh.safeGetUniform("Quality").set(1f);
        sh.safeGetUniform("ShadowOn").set(shadowsOk ? 1f : 0f);
        sh.safeGetUniform("LightMat").set(lightMat);
        sh.safeGetUniform("ShadowTexel").set(1f / SHADOW_RES);
        sh.safeGetUniform("ShadowBias").set(0.35f / SHADOW_FAR);
        sh.safeGetUniform("ShadowNormalOffset").set(2f * shadowHalf / SHADOW_RES * 1.5f);
        Vector3f cam = new Matrix4f(view).invert().transformPosition(new Vector3f());
        sh.safeGetUniform("CamRel").set(cam.x, cam.y, cam.z);
        float[] sky = MapAtmosphere.provider.skyColor();
        sh.safeGetUniform("SkyAmbient").set(sky[0], sky[1], sky[2]);
        if (shadowsOk) sh.setSampler("ShadowMap", shadowTarget.getDepthTextureId());

        for (long[] c : vis) {
            ChunkMesh m = MESHES.get(ExploredMap.key((int) c[0], (int) c[1]));
            if (m == null || !m.has) continue;
            drawChunk(m.vb, (int) c[0], (int) c[1], tx, tz);
        }
        VertexBuffer.unbind();

        // agua translucida por cima do terreno
        RenderSystem.depthMask(false);
        for (long[] c : vis) {
            ChunkMesh m = MESHES.get(ExploredMap.key((int) c[0], (int) c[1]));
            if (m == null || !m.hasW) continue;
            drawChunk(m.wvb, (int) c[0], (int) c[1], tx, tz);
        }
        VertexBuffer.unbind();
        RenderSystem.depthMask(true);
        sh.safeGetUniform("Quality").set(0f);
        sh.safeGetUniform("ShadowOn").set(0f);

        DioramaRenderer.drawBeams(view, proj, tx, targetY, tz, MapOptions.worldPins, Math.max(0.8f, dist / 80f), Math.max(60f, dist * 1.2f));

        // ponteiro do jogador (sempre visivel)
        if (player != null) {
            RenderSystem.clear(256, Minecraft.ON_OSX);
            float s = dist / 130f;   // ponteiro menor
            Matrix4f pmv = new Matrix4f(view)
                    .translate((float) (player.getX() - tx), (float) (player.getY() - targetY + 1.0), (float) (player.getZ() - tz))
                    .rotateY((float) (Math.PI - Math.toRadians(player.getYRot())))
                    .scale(s);
            DioramaRenderer.drawPointer(pmv, proj);
        }
        DioramaRenderer.end(g, st);
    }

    private static void drawChunk(VertexBuffer vb, int cx, int cz, double tx, double tz) {
        float ox = (float) (cx * 16 - tx), oy = (float) (64 - targetY), oz = (float) (cz * 16 - tz);
        Matrix4f mv = new Matrix4f(view).translate(ox, oy, oz);
        DioramaRenderer.shader.safeGetUniform("Origin").set((float) (cx * 16), 64f, (float) (cz * 16));
        DioramaRenderer.shader.safeGetUniform("ShadowOrigin").set(ox, oy, oz);
        vb.bind();
        vb.drawWithShader(mv, proj, DioramaRenderer.shader);
    }

    // Sombra real: desenha o terreno visto do sol/lua (so profundidade) num shadow map 2048x2048 em volta do alvo.
    // O shader do terreno compara cada ponto com esse mapa (com borda suave). Devolve false se nao deu para fazer.
    private static boolean shadowPass(List<long[]> vis, double tx, double tz, float half, Matrix4f outLightMat) {
        if (depthShader == null || !Sun.hasSky || Sun.dy < 0.03f) return false;
        try {
            if (shadowTarget == null) shadowTarget = new TextureTarget(SHADOW_RES, SHADOW_RES, true, Minecraft.ON_OSX);
            Vector3f L = new Vector3f(Sun.dx, Sun.dy, Sun.dz);
            Vector3f eye = new Vector3f(L).mul(SHADOW_FAR / 2f);
            Vector3f up = Math.abs(L.y) > 0.98f ? new Vector3f(0f, 0f, 1f) : new Vector3f(0f, 1f, 0f);
            Matrix4f lv = new Matrix4f().lookAt(eye.x, eye.y, eye.z, 0f, 0f, 0f, up.x, up.y, up.z);
            Matrix4f lp = new Matrix4f().ortho(-half, half, -half, half, 1f, SHADOW_FAR);
            outLightMat.set(lp).mul(lv);

            shadowTarget.setClearColor(1f, 1f, 1f, 1f);
            shadowTarget.clear(Minecraft.ON_OSX);
            shadowTarget.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            float reach = (half + 48f) / 16f;
            for (long[] c : vis) {
                if (c[2] > (long) (reach * reach)) continue;   // longe demais da area da sombra
                ChunkMesh m = MESHES.get(ExploredMap.key((int) c[0], (int) c[1]));
                if (m == null || !m.has) continue;
                Matrix4f mv = new Matrix4f(lv).translate((float) (c[0] * 16 - tx), (float) (64 - targetY), (float) (c[1] * 16 - tz));
                m.vb.bind();
                m.vb.drawWithShader(mv, lp, depthShader);
            }
            VertexBuffer.unbind();
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return true;
        } catch (Exception e) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            return false;
        }
    }

    // dados de uma coluna (de qualquer chunk explorado), ou null se desconhecida
    private record Col(int top, int ground, int cb, boolean canopy, boolean trunk, int topCol, int gcol, int tcol, boolean water, boolean fringe) {}

    private static Col col(int wx, int wz) {
        ExploredMap.Tile t = ExploredMap.tile(wx >> 4, wz >> 4);
        if (t == null) return null;
        int i = (wz & 15) * 16 + (wx & 15);
        if (t.abgr[i] == 0) return null;
        boolean canopy = (t.canopy[i] & 1) != 0;
        boolean water = (t.canopy[i] & 4) != 0;
        return new Col(t.h[i], canopy || water ? t.gh[i] : t.h[i], t.cb[i], canopy, (t.canopy[i] & 2) != 0, t.abgr[i], t.gcol[i], t.tcol[i], water, (t.canopy[i] & 8) != 0);
    }

    private static int argb(int abgr) { return 0xFF000000 | ((abgr & 0xFF) << 16) | (abgr & 0xFF00) | ((abgr >> 16) & 0xFF); }

    private static void build(ChunkMesh m, ExploredMap.Tile t, int cx, int cz) {
        m.version = t.version;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        BufferBuilder bw = new BufferBuilder(WBUF, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        int waterQuads = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] sp = new int[32], tmp = new int[32];
        int quads = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int wx = cx * 16 + x, wz = cz * 16 + z;
                Col c = col(wx, wz);
                if (c == null) continue;
                int sTop = c.ground() + 1;                       // topo do chao
                int topArgb = argb(c.canopy() || c.water() ? c.gcol() : c.topCol());
                quads += face(b, x, sTop - 64, z, topArgb);     // topo do chao (ou do terreno sem copa)
                if (c.water()) {                                 // agua: quadrado translucido na superficie; mais fundo = mais opaco
                    int depth = Math.max(1, c.top() - c.ground());
                    int alpha = Math.min(215, 120 + depth * 12);
                    face(bw, x, c.top() + 1 - 64 - 0.1f, z, (alpha << 24) | (argb(c.topCol()) & 0xFFFFFF));
                    waterQuads++;
                }

                for (int[] d : dirs) {
                    Col n = col(wx + d[0], wz + d[1]);
                    int nS = n == null ? sTop - 6 : n.ground() + 1;
                    if (nS < sTop) {                              // lateral do chao
                        float y0 = nS - 64, y1 = sTop - 64;
                        int sideArgb = !c.canopy() && !c.water() && c.tcol() != 0 ? argb(c.tcol()) : topArgb;
                        if (c.fringe()) {                         // grama: faixa verde no alto, o resto terra
                            float yf = Math.max(y0, y1 - 0.22f);
                            int g = DioramaRenderer.scale(topArgb, 0.82f);
                            side(b, d, x, z, yf, y1, g, g);
                            quads++;
                            y1 = yf;
                        }
                        if (y1 > y0) {
                            int cTop = DioramaRenderer.scale(sideArgb, 0.85f), cBot = DioramaRenderer.scale(sideArgb, 0.60f);
                            side(b, d, x, z, y0, y1, cTop, cBot);
                            quads++;
                        }
                    }
                    if (c.canopy()) {                             // lateral da copa de folhas
                        int cnt = 2;
                        sp[0] = c.cb(); sp[1] = c.top() + 1;
                        if (n == null) cnt = sub(sp, cnt, tmp, Integer.MIN_VALUE, c.top() + 1 - 6);
                        else {
                            cnt = sub(sp, cnt, tmp, Integer.MIN_VALUE, nS);
                            if (n.canopy()) cnt = sub(sp, cnt, tmp, n.cb(), n.top() + 1);
                        }
                        int lc = DioramaRenderer.scale(argb(c.topCol()), 0.80f);
                        for (int s = 0; s < cnt; s += 2) {
                            side(b, d, x, z, sp[s] - 64, sp[s + 1] - 64, lc, DioramaRenderer.scale(lc, 0.75f));
                            quads++;
                        }
                    }
                }
                if (c.canopy()) {
                    quads += face(b, x, c.top() + 1 - 64, z, argb(c.topCol()));                      // topo da copa
                    if (c.trunk() && c.cb() > sTop) {                                               // tronco: caixa fina
                        float lo = sTop - 64, hi = c.cb() - 64;
                        int tc = DioramaRenderer.scale(argb(c.tcol()), 1.25f), tb = DioramaRenderer.scale(argb(c.tcol()), 0.90f);   // troncos escuros (cerejeira, carvalho escuro) nao viram preto
                        float a = x + 0.30f, bb = x + 0.70f, e = z + 0.30f, f = z + 0.70f;
                        DioramaRenderer.quadC(b, bb, hi, e, bb, hi, f, bb, lo, f, bb, lo, e, tc, tc, tb, tb, 1, 0, 0);
                        DioramaRenderer.quadC(b, a, hi, f, a, hi, e, a, lo, e, a, lo, f, tc, tc, tb, tb, -1, 0, 0);
                        DioramaRenderer.quadC(b, bb, hi, f, a, hi, f, a, lo, f, bb, lo, f, tc, tc, tb, tb, 0, 0, 1);
                        DioramaRenderer.quadC(b, a, hi, e, bb, hi, e, bb, lo, e, a, lo, e, tc, tc, tb, tb, 0, 0, -1);
                        quads += 4;
                    }
                }
            }
        }
        MeshData md = b.build();
        if (m.vb == null) m.vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        m.vb.bind();
        if (md != null && quads > 0) { m.vb.upload(md); m.has = true; } else m.has = false;
        VertexBuffer.unbind();

        MeshData mw = bw.build();
        if (m.wvb == null) m.wvb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        m.wvb.bind();
        if (mw != null && waterQuads > 0) { m.wvb.upload(mw); m.hasW = true; } else { m.hasW = false; if (mw != null) mw.close(); }
        VertexBuffer.unbind();
    }

    private static final ByteBufferBuilder WBUF = new ByteBufferBuilder(1 << 16);

    private static int face(BufferBuilder b, int x, float y, int z, int col) {
        DioramaRenderer.quadC(b, x, y, z, x + 1, y, z, x + 1, y, z + 1, x, y, z + 1, col, col, col, col, 0, 1, 0);
        return 1;
    }

    private static void side(BufferBuilder b, int[] d, int x, int z, float y0, float y1, int cTop, int cBot) {
        if (d[0] == 1) DioramaRenderer.quadC(b, x + 1, y1, z, x + 1, y1, z + 1, x + 1, y0, z + 1, x + 1, y0, z, cTop, cTop, cBot, cBot, 1, 0, 0);
        else if (d[0] == -1) DioramaRenderer.quadC(b, x, y1, z + 1, x, y1, z, x, y0, z, x, y0, z + 1, cTop, cTop, cBot, cBot, -1, 0, 0);
        else if (d[1] == 1) DioramaRenderer.quadC(b, x + 1, y1, z + 1, x, y1, z + 1, x, y0, z + 1, x + 1, y0, z + 1, cTop, cTop, cBot, cBot, 0, 0, 1);
        else DioramaRenderer.quadC(b, x, y1, z, x + 1, y1, z, x + 1, y0, z, x, y0, z, cTop, cTop, cBot, cBot, 0, 0, -1);
    }

    private static int sub(int[] sp, int cnt, int[] tmp, int a, int b) {
        int out = 0;
        for (int s = 0; s < cnt; s += 2) {
            int lo = sp[s], hi = sp[s + 1];
            if (b <= lo || a >= hi) { tmp[out++] = lo; tmp[out++] = hi; continue; }
            if (lo < a) { tmp[out++] = lo; tmp[out++] = a; }
            if (b < hi) { tmp[out++] = b; tmp[out++] = hi; }
            if (out > 28) break;
        }
        System.arraycopy(tmp, 0, sp, 0, out);
        return out;
    }

    // ---------------------------------------------------------------- selecao com o mouse

    // coordenadas de mundo (x, z) sob o cursor, ou null se nao acertar o terreno explorado
    public static int[] pick(double mx, double my, int x0, int y0, int w, int h, double tx, double tz) {
        float nx = (float) ((mx - x0) / w * 2 - 1), ny = (float) (1 - (my - y0) / h * 2);
        Matrix4f inv = new Matrix4f(proj).mul(view).invert();
        Vector4f a = new Vector4f(nx, ny, -1f, 1f).mul(inv), c = new Vector4f(nx, ny, 1f, 1f).mul(inv);
        a.div(a.w); c.div(c.w);
        Vector3f o = new Vector3f(a.x, a.y, a.z), d = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
        float len = d.length();
        if (len < 1e-4f) return null;
        d.div(len);
        float step = 1.0f, tm = 0;
        float maxT = Math.min(len, dist * 6f);
        float lastT = 0;
        for (tm = 0; tm < maxT; tm += step) {
            double wx = tx + o.x + d.x * tm, wz = tz + o.z + d.z * tm, wy = targetY + o.y + d.y * tm;
            int hh = ExploredMap.heightAt((int) Math.floor(wx), (int) Math.floor(wz));
            if (hh != ExploredMap.UNKNOWN && wy <= hh + 1) {
                // refina entre lastT e tm
                float lo = lastT, hi = tm;
                for (int k = 0; k < 6; k++) {
                    float mid = (lo + hi) / 2;
                    double mx2 = tx + o.x + d.x * mid, mz2 = tz + o.z + d.z * mid, my2 = targetY + o.y + d.y * mid;
                    int h2 = ExploredMap.heightAt((int) Math.floor(mx2), (int) Math.floor(mz2));
                    if (h2 != ExploredMap.UNKNOWN && my2 <= h2 + 1) hi = mid; else lo = mid;
                }
                return new int[]{(int) Math.floor(tx + o.x + d.x * hi), (int) Math.floor(tz + o.z + d.z * hi)};
            }
            lastT = tm;
        }
        return null;
    }
}

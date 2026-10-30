package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

// Vista 3D do mapa-mundi (estilo "3D view" do Diorama): o terreno ja explorado vira uma maquete 3D que voce gira e inclina.
// Cada chunk explorado e' uma malha na GPU (VertexBuffer) desenhada com o shader "diorama".
public class WorldMap3D {
    private static final float FOV = (float) Math.toRadians(50);
    private static final int VIEW_CHUNKS = 10;          // raio de chunks desenhados em volta do alvo
    private static final int BUILD_PER_FRAME = 6;

    public static float yaw = 0.0f, pitch = 0.95f, dist = 110f;
    public static Matrix4f view = new Matrix4f(), proj = new Matrix4f();
    public static double targetY = 64;                  // altura do alvo (suavizada)

    private static class ChunkMesh { VertexBuffer vb; int version = -1; boolean has; }
    private static final Map<Long, ChunkMesh> MESHES = new HashMap<>();

    public static void clearAll() {
        for (ChunkMesh m : MESHES.values()) if (m.vb != null) m.vb.close();
        MESHES.clear();
    }

    // desenha a maquete na area (x0,y0,w,h). tx,tz = ponto central no mundo.
    public static void render(GuiGraphics g, int x0, int y0, int w, int h, double tx, double tz, LocalPlayer player) {
        if (!DioramaRenderer.ready()) return;
        int th = ExploredMap.heightAt((int) Math.floor(tx), (int) Math.floor(tz));
        double goal = th == ExploredMap.UNKNOWN ? (player != null ? player.getY() : 64) : th;
        targetY += (goal - targetY) * 0.12;

        proj = new Matrix4f().perspective(FOV, w / (float) h, 0.5f, 3000f);
        view = new Matrix4f().translate(0, 0, -dist).rotateX(pitch).rotateY(yaw);

        int cxm = (int) Math.floor(tx / 16), czm = (int) Math.floor(tz / 16), built = 0;
        DioramaRenderer.GlState st = DioramaRenderer.begin(g, x0, y0, w, h, proj);

        DioramaRenderer.shader.safeGetUniform("Radius").set(1.0e6f);
        DioramaRenderer.shader.safeGetUniform("Detail").set(1f);
        for (int dz = -VIEW_CHUNKS; dz <= VIEW_CHUNKS; dz++) {
            for (int dx = -VIEW_CHUNKS; dx <= VIEW_CHUNKS; dx++) {
                int cx = cxm + dx, cz = czm + dz;
                ExploredMap.Tile t = ExploredMap.tile(cx, cz);
                if (t == null) continue;
                long key = ExploredMap.key(cx, cz);
                ChunkMesh m = MESHES.get(key);
                if (m == null) { m = new ChunkMesh(); MESHES.put(key, m); }
                if (m.version != t.version && built < BUILD_PER_FRAME) { build(m, t, cx, cz); built++; }
                if (!m.has) continue;
                Matrix4f mv = new Matrix4f(view).translate((float) (cx * 16 - tx), (float) (64 - targetY), (float) (cz * 16 - tz));
                DioramaRenderer.shader.safeGetUniform("Origin").set((float) (cx * 16), 64f, (float) (cz * 16));
                m.vb.bind();
                m.vb.drawWithShader(mv, proj, DioramaRenderer.shader);
            }
        }
        VertexBuffer.unbind();

        // ponteiro do jogador (sempre visivel)
        if (player != null) {
            RenderSystem.clear(256, Minecraft.ON_OSX);
            float s = dist / 38f;
            Matrix4f pmv = new Matrix4f(view)
                    .translate((float) (player.getX() - tx), (float) (player.getY() - targetY + 1.0), (float) (player.getZ() - tz))
                    .rotateY((float) (Math.PI - Math.toRadians(player.getYRot())))
                    .scale(s);
            DioramaRenderer.drawPointer(pmv, proj);
        }
        DioramaRenderer.end(g, st);
    }

    private static void build(ChunkMesh m, ExploredMap.Tile t, int cx, int cz) {
        m.version = t.version;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int quads = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = z * 16 + x;
                if (t.abgr[i] == 0) continue;
                int hgt = t.h[i];
                int abgr = t.abgr[i];
                int col = 0xFF000000 | ((abgr & 0xFF) << 16) | (abgr & 0xFF00) | ((abgr >> 16) & 0xFF);
                float y = hgt + 1 - 64;
                DioramaRenderer.quadC(b, x, y, z, x + 1, y, z, x + 1, y, z + 1, x, y, z + 1, col, col, col, col, 0, 1, 0);
                quads++;
                for (int[] d : dirs) {
                    int nx = x + d[0], nz = z + d[1], nh;
                    if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                        int ni = nz * 16 + nx;
                        nh = t.abgr[ni] == 0 ? hgt - 6 : t.h[ni];
                    } else {
                        int w = ExploredMap.heightAt(cx * 16 + nx, cz * 16 + nz);
                        nh = w == ExploredMap.UNKNOWN ? hgt - 6 : w;
                    }
                    if (nh >= hgt) continue;
                    float y1 = hgt + 1 - 64, y0 = nh + 1 - 64;
                    int cTop = DioramaRenderer.scale(col, 0.78f), cBot = DioramaRenderer.scale(col, 0.55f);
                    if (d[0] == 1) DioramaRenderer.quadC(b, x + 1, y1, z, x + 1, y1, z + 1, x + 1, y0, z + 1, x + 1, y0, z, cTop, cTop, cBot, cBot, 1, 0, 0);
                    else if (d[0] == -1) DioramaRenderer.quadC(b, x, y1, z + 1, x, y1, z, x, y0, z, x, y0, z + 1, cTop, cTop, cBot, cBot, -1, 0, 0);
                    else if (d[1] == 1) DioramaRenderer.quadC(b, x + 1, y1, z + 1, x, y1, z + 1, x, y0, z + 1, x + 1, y0, z + 1, cTop, cTop, cBot, cBot, 0, 0, 1);
                    else DioramaRenderer.quadC(b, x, y1, z, x + 1, y1, z, x + 1, y0, z, x, y0, z, cTop, cTop, cBot, cBot, 0, 0, -1);
                    quads++;
                }
            }
        }
        MeshData md = b.build();
        if (m.vb == null) m.vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        m.vb.bind();
        if (md != null && quads > 0) { m.vb.upload(md); m.has = true; } else m.has = false;
        VertexBuffer.unbind();
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

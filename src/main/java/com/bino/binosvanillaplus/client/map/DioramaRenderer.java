package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector4f;

// Minimapa em 3D: a malha do terreno (cubos com faces visiveis, cor real do bloco, oclusao ambiente nos cantos) vai para a GPU
// num VertexBuffer e e' desenhada com o shader proprio "diorama" (perspectiva + depth buffer).
public class DioramaRenderer {
    public static ShaderInstance shader;          // preenchido por MapClientEvents (RegisterShadersEvent)

    private static final float TILT = (float) Math.toRadians(52);
    private static final float FOV = (float) Math.toRadians(45);

    private static VertexBuffer terrain, pointer;
    private static boolean hasTerrain = false;
    private static int ox0, oy0, oz0;             // origem (bloco) da malha
    public static Matrix4f view = new Matrix4f(), proj = new Matrix4f();

    public static boolean ready() { return shader != null; }

    // ------------------------------------------------------------------ malha do terreno (minimapa)

    static void rebuild(int R, int N, int[] RC, int[][] RTOP, int[][] RBOT, int[][] RCOL, int[][] RSIDE, boolean[] RGROUND,
                        int px, int py, int pz) {
        if (shader == null) return;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        int[] sp = new int[32], tmp = new int[32];
        final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int i = 0; i < N * N; i++) {
            int n = RC[i];
            if (n == 0) continue;
            int dx = i % N - R, dz = i / N - R;
            for (int r = 0; r < n; r++) {
                boolean ground = RGROUND[i] && r == n - 1;
                int top = RTOP[i][r] + 1;
                int bot = ground ? top - 9 : RBOT[i][r];
                int col = RCOL[i][r], sideCol = RSIDE[i][r];

                // topo, com oclusao ambiente por canto (so no bloco mais alto da coluna)
                boolean covered = r > 0 && RBOT[i][r - 1] == top;
                if (!covered) {
                    float y = top - py;
                    int[] c = new int[4];
                    int[][] corner = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                    boolean shadow = Sun.shadows && shadowed(RC, RTOP, RBOT, RGROUND, N, R, dx + 0.5f, top + 0.05f, dz + 0.5f);
                    for (int k = 0; k < 4; k++) {
                        float ao = shadow ? 0.60f : 1f;
                        if (r == 0) {
                            boolean s1 = higher(RC, RTOP, N, R, dx + corner[k][0], dz, top);
                            boolean s2 = higher(RC, RTOP, N, R, dx, dz + corner[k][1], top);
                            boolean cc = higher(RC, RTOP, N, R, dx + corner[k][0], dz + corner[k][1], top);
                            int a = (s1 && s2) ? 0 : 3 - ((s1 ? 1 : 0) + (s2 ? 1 : 0) + (cc ? 1 : 0));
                            ao *= 0.58f + 0.42f * a / 3f;
                        }
                        c[k] = scale(col, ao);
                    }
                    quadC(b, dx, y, dz, dx + 1, y, dz, dx + 1, y, dz + 1, dx, y, dz + 1, c[0], c[1], c[2], c[3], 0, 1, 0);
                }
                // laterais, descontando o que a coluna vizinha ja cobre
                for (int[] d : dirs) {
                    int cnt = 2;
                    sp[0] = bot; sp[1] = top;
                    int nx = dx + d[0], nz = dz + d[1];
                    boolean inside = Math.abs(nx) <= R && Math.abs(nz) <= R;
                    int ni = inside ? (nz + R) * N + (nx + R) : -1;
                    if (ni < 0 || RC[ni] == 0) {
                        cnt = subtract(sp, cnt, tmp, Integer.MIN_VALUE, top - 8);
                    } else {
                        for (int k = 0; k < RC[ni]; k++) {
                            boolean ng = RGROUND[ni] && k == RC[ni] - 1;
                            int a = ng ? Integer.MIN_VALUE : RBOT[ni][k];
                            cnt = subtract(sp, cnt, tmp, a, RTOP[ni][k] + 1);
                        }
                    }
                    for (int s = 0; s < cnt; s += 2) {
                        float y0 = sp[s] - py, y1 = sp[s + 1] - py;
                        float t = Math.max(0.0f, Math.min(1f, (sp[s + 1] - sp[s]) / 6f));
                        float sh = 1f;
                        if (Sun.shadows && d[0] * Sun.dx + d[1] * Sun.dz > 0.05f
                                && shadowed(RC, RTOP, RBOT, RGROUND, N, R, dx + 0.5f + d[0] * 0.55f, (sp[s] + sp[s + 1]) / 2f, dz + 0.5f + d[1] * 0.55f)) sh = 0.65f;
                        int cTop = scale(sideCol, 0.92f * sh), cBot = scale(sideCol, (0.92f - 0.22f * t) * sh);
                        if (d[0] == 1) quadC(b, dx + 1, y1, dz, dx + 1, y1, dz + 1, dx + 1, y0, dz + 1, dx + 1, y0, dz, cTop, cTop, cBot, cBot, 1, 0, 0);
                        else if (d[0] == -1) quadC(b, dx, y1, dz + 1, dx, y1, dz, dx, y0, dz, dx, y0, dz + 1, cTop, cTop, cBot, cBot, -1, 0, 0);
                        else if (d[1] == 1) quadC(b, dx + 1, y1, dz + 1, dx, y1, dz + 1, dx, y0, dz + 1, dx + 1, y0, dz + 1, cTop, cTop, cBot, cBot, 0, 0, 1);
                        else quadC(b, dx, y1, dz, dx + 1, y1, dz, dx + 1, y0, dz, dx, y0, dz, cTop, cTop, cBot, cBot, 0, 0, -1);
                    }
                }
            }
        }
        MeshData md = b.build();
        if (terrain == null) terrain = new VertexBuffer(VertexBuffer.Usage.STATIC);
        terrain.bind();
        if (md != null) { terrain.upload(md); hasTerrain = true; } else hasTerrain = false;
        VertexBuffer.unbind();
        ox0 = px; oy0 = py; oz0 = pz;
    }

    // o ponto (x,y,z) (x,z em blocos relativos ao jogador, y absoluto) esta na sombra? Marcha em direcao ao sol pela malha.
    private static boolean shadowed(int[] RC, int[][] RTOP, int[][] RBOT, boolean[] RGROUND, int N, int R, float x, float y, float z) {
        for (float t = 0.25f; t < 18f; t += 0.5f) {
            float px = x + Sun.dx * t, py = y + Sun.dy * t, pz = z + Sun.dz * t;
            int ix = (int) Math.floor(px), iz = (int) Math.floor(pz);
            if (Math.abs(ix) > R || Math.abs(iz) > R) return false;
            int i = (iz + R) * N + (ix + R);
            for (int r = 0; r < RC[i]; r++) {
                int yt = RTOP[i][r] + 1;
                boolean ground = RGROUND[i] && r == RC[i] - 1;
                if (ground ? py < yt : (py >= RBOT[i][r] && py < yt)) return true;
            }
        }
        return false;
    }

    private static boolean higher(int[] RC, int[][] RTOP, int N, int R, int nx, int nz, int top) {
        if (Math.abs(nx) > R || Math.abs(nz) > R) return false;
        int ni = (nz + R) * N + (nx + R);
        return RC[ni] > 0 && RTOP[ni][0] + 1 > top;
    }

    private static int subtract(int[] sp, int cnt, int[] tmp, int a, int b) {
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

    static int scale(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f)), g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f)),
                bl = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // quadrado com uma cor por vertice (usado tambem pelo mapa-mundi 3D)
    static void quadC(BufferBuilder b, float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      int c0, int c1, int c2, int c3, float nx, float ny, float nz) {
        b.addVertex(ax, ay, az).setColor(c0).setNormal(nx, ny, nz);
        b.addVertex(bx, by, bz).setColor(c1).setNormal(nx, ny, nz);
        b.addVertex(cx, cy, cz).setColor(c2).setNormal(nx, ny, nz);
        b.addVertex(dx, dy, dz).setColor(c3).setNormal(nx, ny, nz);
    }

    // ------------------------------------------------------------------ ponteiro do jogador (seta plana, aponta -Z)

    static VertexBuffer pointer() {
        if (pointer != null) return pointer;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        arrow(b, 0.95f, 0.30f, 0xFFFFFFFF);     // seta toda branca
        MeshData md = b.build();
        pointer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        pointer.bind();
        if (md != null) pointer.upload(md);
        VertexBuffer.unbind();
        return pointer;
    }

    // seta (chevron) deitada no plano XZ a altura y; centro da seta ~ origem; aponta para -Z
    private static void arrow(BufferBuilder b, float k, float y, int col) {
        float[] tip = {0f, -3.0f * k}, right = {2.1f * k, 2.0f * k}, notch = {0f, 0.7f * k}, left = {-2.1f * k, 2.0f * k};
        tri(b, tip, right, notch, y, col);
        tri(b, tip, notch, left, y, col);
    }

    private static void tri(BufferBuilder b, float[] p1, float[] p2, float[] p3, float y, int col) {
        b.addVertex(p1[0], y, p1[1]).setColor(col).setNormal(0, 1, 0);
        b.addVertex(p2[0], y, p2[1]).setColor(col).setNormal(0, 1, 0);
        b.addVertex(p3[0], y, p3[1]).setColor(col).setNormal(0, 1, 0);
    }

    // desenha o ponteiro com uma matriz modelview qualquer
    static void drawPointer(Matrix4f mv, Matrix4f projection) {
        VertexBuffer p = pointer();
        shader.safeGetUniform("Radius").set(100000f);
        shader.safeGetUniform("Detail").set(0f);
        shader.safeGetUniform("Origin").set(0f, 0f, 0f);
        p.bind();
        p.drawWithShader(mv, projection, shader);
        VertexBuffer.unbind();
    }

    // ------------------------------------------------------------------ estado de GL compartilhado (minimapa e mapa-mundi)

    static class GlState {
        Matrix4f oldProj;
        int vw, vx, vy, vh;
    }

    static GlState begin(GuiGraphics g, int x0, int y0, int w, int h, Matrix4f projection) {
        Minecraft mc = Minecraft.getInstance();
        Window win = mc.getWindow();
        double sc = win.getGuiScale();
        GlState s = new GlState();
        s.vx = (int) (x0 * sc); s.vw = (int) (w * sc); s.vh = (int) (h * sc); s.vy = (int) (win.getHeight() - (y0 + h) * sc);
        g.flush();
        s.oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        g.enableScissor(x0, y0, x0 + w, y0 + h);
        RenderSystem.viewport(s.vx, s.vy, s.vw, s.vh);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(256, Minecraft.ON_OSX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        return s;
    }

    static void end(GuiGraphics g, GlState s) {
        Window win = Minecraft.getInstance().getWindow();
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.viewport(0, 0, win.getWidth(), win.getHeight());
        RenderSystem.setProjectionMatrix(s.oldProj, VertexSorting.ORTHOGRAPHIC_Z);
        g.disableScissor();
    }

    // ------------------------------------------------------------------ desenho do minimapa

    public static void draw(GuiGraphics g, LocalPlayer p, int x0, int y0, int box, float dist, float fadeRadius) {
        if (shader == null) return;
        float yaw = (float) Math.toRadians(p.getYRot());
        view = new Matrix4f().translate(0, 0, -dist).rotateX(TILT).rotateY(yaw + (float) Math.PI);
        proj = new Matrix4f().perspective(FOV, 1f, 0.1f, 400f);
        GlState st = begin(g, x0, y0, box, box, proj);

        if (hasTerrain) {
            Matrix4f mv = new Matrix4f(view).translate((float) (ox0 - p.getX()), (float) (oy0 - p.getY()), (float) (oz0 - p.getZ()));
            Sun.applyUniforms();
            shader.safeGetUniform("Radius").set(fadeRadius);
            shader.safeGetUniform("Detail").set(1f);
            // Origin = canto da malha no mundo, para a textura procedural ficar presa aos blocos
            shader.safeGetUniform("Origin").set((float) ox0, (float) oy0, (float) oz0);
            terrain.bind();
            terrain.drawWithShader(mv, proj, shader);
            VertexBuffer.unbind();
        }
        // ponteiro do jogador por cima de tudo, deitado no plano do terreno, sempre apontando "para cima" no mapa
        RenderSystem.clear(256, Minecraft.ON_OSX);
        Matrix4f pmv = new Matrix4f().translate(0, 0, -dist).rotateX(TILT).scale(dist / 30f * 0.62f);
        drawPointer(pmv, proj);
        end(g, st);
    }

    // projeta um ponto (offset em blocos a partir da posicao exata do jogador) para pixels da tela: {sx, sy, visivel}
    public static float[] project(float dx, float dy, float dz, int x0, int y0, int box) {
        return projectWith(view, proj, dx, dy, dz, x0, y0, box, box);
    }

    public static float[] projectWith(Matrix4f v, Matrix4f pr, float dx, float dy, float dz, int x0, int y0, int w, int h) {
        Vector4f vec = new Vector4f(dx, dy, dz, 1f);
        v.transform(vec);
        pr.transform(vec);
        if (vec.w <= 0.0001f) return new float[]{x0 + w / 2f, y0 + h / 2f, 0};
        return new float[]{x0 + w / 2f + vec.x / vec.w * w / 2f, y0 + h / 2f - vec.y / vec.w * h / 2f, 1};
    }
}

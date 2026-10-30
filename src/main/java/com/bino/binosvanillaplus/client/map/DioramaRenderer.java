package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// Minimapa em 3D de verdade: a malha do terreno (cubos com faces visiveis) vai para a GPU num VertexBuffer e e' desenhada
// com o shader proprio "diorama" (assets/binosvanillaplus/shaders/core/diorama.*), com perspectiva e depth buffer.
// Se o shader nao carregar (shader == null), o MinimapHud usa o desenho 2D antigo.
public class DioramaRenderer {
    public static ShaderInstance shader;          // preenchido por MapClientEvents (RegisterShadersEvent)

    // camera
    private static final float TILT = (float) Math.toRadians(52);
    private static final float FOV = (float) Math.toRadians(45);
    private static final float DIST = 40f;

    private static VertexBuffer terrain, pin;
    private static boolean hasTerrain = false;
    private static int ox0, oy0, oz0;             // origem (bloco) da malha
    public static Matrix4f view = new Matrix4f(), proj = new Matrix4f();

    public static boolean ready() { return shader != null; }

    // ------------------------------------------------------------------ malha do terreno

    static void rebuild(int R, int N, int[] RC, int[][] RTOP, int[][] RBOT, int[][] RCOL, boolean[] RGROUND,
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
                int col = RCOL[i][r];

                // topo (escondido se houver outro bloco colado em cima)
                boolean covered = r > 0 && RBOT[i][r - 1] == top;
                if (!covered) {
                    int hv = (dx * 73856093 ^ dz * 19349663 ^ top * 83492791) & 3;
                    int tc = scale(col, 0.95f + 0.025f * hv);
                    float y = top - py;
                    quad(b, dx, y, dz, dx + 1, y, dz, dx + 1, y, dz + 1, dx, y, dz + 1, tc, 0, 1, 0);
                }
                // laterais, descontando o que a coluna vizinha ja cobre
                for (int[] d : dirs) {
                    int cnt = 2;
                    sp[0] = bot; sp[1] = top;
                    int nx = dx + d[0], nz = dz + d[1];
                    boolean inside = Math.abs(nx) <= R && Math.abs(nz) <= R;
                    int ni = inside ? (nz + R) * N + (nx + R) : -1;
                    if (ni < 0 || RC[ni] == 0) {
                        cnt = subtract(sp, cnt, tmp, Integer.MIN_VALUE, top - 8);   // borda: so um "corte" de 8 blocos
                    } else {
                        for (int k = 0; k < RC[ni]; k++) {
                            boolean ng = RGROUND[ni] && k == RC[ni] - 1;
                            int a = ng ? Integer.MIN_VALUE : RBOT[ni][k];
                            cnt = subtract(sp, cnt, tmp, a, RTOP[ni][k] + 1);
                        }
                    }
                    for (int s = 0; s < cnt; s += 2) {
                        float y0 = sp[s] - py, y1 = sp[s + 1] - py;
                        int sc = scale(col, 0.9f);
                        if (d[0] == 1) quad(b, dx + 1, y1, dz, dx + 1, y1, dz + 1, dx + 1, y0, dz + 1, dx + 1, y0, dz, sc, 1, 0, 0);
                        else if (d[0] == -1) quad(b, dx, y1, dz + 1, dx, y1, dz, dx, y0, dz, dx, y0, dz + 1, sc, -1, 0, 0);
                        else if (d[1] == 1) quad(b, dx + 1, y1, dz + 1, dx, y1, dz + 1, dx, y0, dz + 1, dx + 1, y0, dz + 1, sc, 0, 0, 1);
                        else quad(b, dx, y1, dz, dx + 1, y1, dz, dx + 1, y0, dz, dx, y0, dz, sc, 0, 0, -1);
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

    // remove o intervalo [a,b) da lista de trechos sp (pares lo,hi). Devolve a nova quantidade de ints.
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

    private static int scale(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f)), g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f)),
                bl = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static void quad(BufferBuilder b, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int col, float nx, float ny, float nz) {
        b.addVertex(ax, ay, az).setColor(col).setNormal(nx, ny, nz);
        b.addVertex(bx, by, bz).setColor(col).setNormal(nx, ny, nz);
        b.addVertex(cx, cy, cz).setColor(col).setNormal(nx, ny, nz);
        b.addVertex(dx, dy, dz).setColor(col).setNormal(nx, ny, nz);
    }

    // ------------------------------------------------------------------ o pin do jogador (aponta para onde ele olha)

    private static void buildPin() {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        final float cy = 3.2f;
        Vector3f tip = new Vector3f(0, cy, -2.4f), tail = new Vector3f(0, cy, 1.1f);
        Vector3f left = new Vector3f(-1.0f, cy, 0.2f), right = new Vector3f(1.0f, cy, 0.2f);
        Vector3f top = new Vector3f(0, cy + 0.7f, 0.2f), bottom = new Vector3f(0, cy - 0.7f, 0.2f);
        Vector3f center = new Vector3f(0, cy, 0);
        int head = 0xFFFFD84A, tipCol = 0xFFFF4A3A;
        // frente (ponta vermelha) e traseira (amarela)
        tri(b, tip, left, top, center, tipCol); tri(b, tip, top, right, center, tipCol);
        tri(b, tip, right, bottom, center, tipCol); tri(b, tip, bottom, left, center, tipCol);
        tri(b, tail, top, left, center, head); tri(b, tail, right, top, center, head);
        tri(b, tail, bottom, right, center, head); tri(b, tail, left, bottom, center, head);
        // agulha do pin
        float w = 0.16f;
        float[][] sides = {{-w, -w, w, -w}, {w, -w, w, w}, {w, w, -w, w}, {-w, w, -w, -w}};
        for (float[] s : sides) {
            Vector3f nrm = new Vector3f(s[3] - s[1], 0, -(s[2] - s[0])).normalize();
            quadN(b, s[0], cy - 0.7f, s[1], s[2], cy - 0.7f, s[3], s[2], 0f, s[3], s[0], 0f, s[1], 0xFFE8E8E8, nrm);
        }
        MeshData md = b.build();
        pin = new VertexBuffer(VertexBuffer.Usage.STATIC);
        pin.bind();
        if (md != null) pin.upload(md);
        VertexBuffer.unbind();
    }

    private static void quadN(BufferBuilder b, float ax, float ay, float az, float bx, float by, float bz,
                              float cx, float cy, float cz, float dx, float dy, float dz, int col, Vector3f n) {
        // quad como 2 triangulos nao e' necessario: o buffer do pin e' TRIANGLES, entao quebramos aqui
        vert(b, ax, ay, az, col, n); vert(b, bx, by, bz, col, n); vert(b, cx, cy, cz, col, n);
        vert(b, ax, ay, az, col, n); vert(b, cx, cy, cz, col, n); vert(b, dx, dy, dz, col, n);
    }

    private static void tri(BufferBuilder b, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f center, int col) {
        Vector3f n = new Vector3f(p2).sub(p1).cross(new Vector3f(p3).sub(p1)).normalize();
        Vector3f mid = new Vector3f(p1).add(p2).add(p3).div(3f).sub(center);
        if (n.dot(mid) < 0) n.negate();   // normal sempre para fora
        vert(b, p1.x, p1.y, p1.z, col, n); vert(b, p2.x, p2.y, p2.z, col, n); vert(b, p3.x, p3.y, p3.z, col, n);
    }

    private static void vert(BufferBuilder b, float x, float y, float z, int col, Vector3f n) {
        b.addVertex(x, y, z).setColor(col).setNormal(n.x, n.y, n.z);
    }

    // ------------------------------------------------------------------ desenho

    public static void draw(GuiGraphics g, LocalPlayer p, int x0, int y0, int box, float radius) {
        if (shader == null) return;
        if (pin == null) buildPin();
        Minecraft mc = Minecraft.getInstance();
        Window w = mc.getWindow();
        double sc = w.getGuiScale();
        int vx = (int) (x0 * sc), vw = (int) (box * sc), vy = (int) (w.getHeight() - (y0 + box) * sc);

        g.flush();
        float yaw = (float) Math.toRadians(p.getYRot());
        view = new Matrix4f().translate(0, 0, -DIST).rotateX(TILT).rotateY(yaw + (float) Math.PI);
        proj = new Matrix4f().perspective(FOV, 1f, 0.1f, 300f);

        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        g.enableScissor(x0, y0, x0 + box, y0 + box);
        RenderSystem.viewport(vx, vy, vw, vw);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(256, Minecraft.ON_OSX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (hasTerrain) {
            Matrix4f mv = new Matrix4f(view).translate((float) (ox0 - p.getX()), (float) (oy0 - p.getY()), (float) (oz0 - p.getZ()));
            shader.safeGetUniform("Radius").set(radius);
            terrain.bind();
            terrain.drawWithShader(mv, proj, shader);
            VertexBuffer.unbind();
        }
        // pin do jogador por cima de tudo
        RenderSystem.clear(256, Minecraft.ON_OSX);
        Matrix4f pinMv = new Matrix4f().translate(0, 0, -DIST).rotateX(TILT);
        shader.safeGetUniform("Radius").set(1000f);
        pin.bind();
        pin.drawWithShader(pinMv, proj, shader);
        VertexBuffer.unbind();

        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.viewport(0, 0, w.getWidth(), w.getHeight());
        RenderSystem.setProjectionMatrix(oldProj, VertexSorting.ORTHOGRAPHIC_Z);
        g.disableScissor();
    }

    // projeta um ponto (offset em blocos a partir da posicao exata do jogador) para pixels da tela
    // retorna {sx, sy, visivel(1/0)}
    public static float[] project(float dx, float dy, float dz, int x0, int y0, int box) {
        Vector4f v = new Vector4f(dx, dy, dz, 1f);
        view.transform(v);
        proj.transform(v);
        if (v.w <= 0.0001f) return new float[]{x0 + box / 2f, y0 + box / 2f, 0};
        float nx = v.x / v.w, ny = v.y / v.w;
        return new float[]{x0 + box / 2f + nx * box / 2f, y0 + box / 2f - ny * box / 2f, 1};
    }
}

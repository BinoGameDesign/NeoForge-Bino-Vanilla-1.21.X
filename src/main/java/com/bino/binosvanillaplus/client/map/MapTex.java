package com.bino.binosvanillaplus.client.map;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

// Faces texturizadas dos mapas (textura de verdade do bloco, em miniatura, vinda do atlas de blocos do jogo).
// Cor do vertice = tinta do bioma (se a camada for tingida) x luz/sombra/oclusao; alpha marca o tipo:
//   255 = camada de base (pixels transparentes viram cor escura, ex.: folhas sem buracos)
//   254 = camada por cima (pixels transparentes sao descartados, ex.: a borda verde da grama)
//   < 250 = agua (translucida)
// UV negativo = sem textura (so cor) - usado pelo ponteiro, feixes e mapas salvos antigos.
public final class MapTex {
    private MapTex() {}

    public static final VertexFormat FMT = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;

    static int color(BlockLooks.Layer l, int tint, float f, int alpha) {
        int base = l.tintIndex() >= 0 ? tint : 0xFFFFFF;
        int r = Math.min(255, (int) (((base >> 16) & 0xFF) * f)), g = Math.min(255, (int) (((base >> 8) & 0xFF) * f)),
                b = Math.min(255, (int) ((base & 0xFF) * f));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static int layerAlpha(int index, int baseAlpha) {
        if (baseAlpha < 250) return baseAlpha;     // agua
        return index == 0 ? 255 : 254;
    }

    // face de cima em (x..x+1, y, z..z+1); f[4] = luz por canto (0..1)
    static void top(BufferBuilder b, BlockLooks.Layer[] ls, int tint, float x, float y, float z, float[] f, int baseAlpha) {
        for (int li = 0; li < ls.length; li++) {
            BlockLooks.Layer l = ls[li];
            TextureAtlasSprite s = l.sprite();
            float yy = y + li * 0.003f;
            int a = layerAlpha(li, baseAlpha);
            float iu = (s.getU1() - s.getU0()) / 32f, iv = (s.getV1() - s.getV0()) / 32f;   // meio pixel para dentro: sem listras nas emendas
            float u0 = s.getU0() + iu, u1 = s.getU1() - iu, v0 = s.getV0() + iv, v1 = s.getV1() - iv;
            b.addVertex(x, yy, z).setUv(u0, v0).setColor(color(l, tint, f[0], a)).setNormal(0, 1, 0);
            b.addVertex(x + 1, yy, z).setUv(u1, v0).setColor(color(l, tint, f[1], a)).setNormal(0, 1, 0);
            b.addVertex(x + 1, yy, z + 1).setUv(u1, v1).setColor(color(l, tint, f[2], a)).setNormal(0, 1, 0);
            b.addVertex(x, yy, z + 1).setUv(u0, v1).setColor(color(l, tint, f[3], a)).setNormal(0, 1, 0);
        }
    }

    // lateral virada para d (+x, -x, +z, -z) do bloco (x,z), entre y0 e y1 (no maximo 1 bloco; textura alinhada pelo alto)
    static void side(BufferBuilder b, BlockLooks.Layer[] ls, int tint, int[] d, float x, float z, float y0, float y1,
                     float fTop, float fBot, int baseAlpha) {
        for (int li = 0; li < ls.length; li++) {
            BlockLooks.Layer l = ls[li];
            TextureAtlasSprite s = l.sprite();
            float o = li * 0.003f;
            int a = layerAlpha(li, baseAlpha);
            float iu = (s.getU1() - s.getU0()) / 32f, iv = (s.getV1() - s.getV0()) / 32f;   // meio pixel para dentro: sem listras nas emendas
            float u0 = s.getU0() + iu, u1 = s.getU1() - iu, v0 = s.getV0() + iv, v1 = s.getV1() - iv;
            float vb = v0 + (v1 - v0) * Math.min(1f, y1 - y0);
            int ct = color(l, tint, fTop, a), cb = color(l, tint, fBot, a);
            if (d[0] == 1) quad(b, x + 1 + o, y1, z, x + 1 + o, y1, z + 1, x + 1 + o, y0, z + 1, x + 1 + o, y0, z, u0, u1, v0, vb, ct, cb, 1, 0, 0);
            else if (d[0] == -1) quad(b, x - o, y1, z + 1, x - o, y1, z, x - o, y0, z, x - o, y0, z + 1, u0, u1, v0, vb, ct, cb, -1, 0, 0);
            else if (d[1] == 1) quad(b, x + 1, y1, z + 1 + o, x, y1, z + 1 + o, x, y0, z + 1 + o, x + 1, y0, z + 1 + o, u0, u1, v0, vb, ct, cb, 0, 0, 1);
            else quad(b, x, y1, z - o, x + 1, y1, z - o, x + 1, y0, z - o, x, y0, z - o, u0, u1, v0, vb, ct, cb, 0, 0, -1);
        }
    }

    // caixa fina (tronco) de x0..x1, z0..z1, y0..y1 com a textura lateral do bloco, 1 bloco por vez
    static void post(BufferBuilder b, BlockLooks.Layer[] ls, int tint, float x0, float x1, float z0, float z1, float y0, float y1, float fTop, float fBot) {
        if (ls.length == 0) return;
        BlockLooks.Layer l = ls[0];
        TextureAtlasSprite s = l.sprite();
        float iu = (s.getU1() - s.getU0()) / 32f, iv = (s.getV1() - s.getV0()) / 32f;
        float u0 = s.getU0() + iu, uw = (s.getU1() - s.getU0() - 2 * iu) * (x1 - x0), v0 = s.getV0() + iv;
        for (float y = y1; y > y0 + 0.001f; y -= 1f) {
            float ya = Math.max(y0, y - 1f);
            float vb = v0 + (s.getV1() - iv - v0) * (y - ya);
            int ct = color(l, tint, fTop, 255), cb = color(l, tint, fBot, 255);
            quad(b, x1, y, z0, x1, y, z1, x1, ya, z1, x1, ya, z0, u0, u0 + uw, v0, vb, ct, cb, 1, 0, 0);
            quad(b, x0, y, z1, x0, y, z0, x0, ya, z0, x0, ya, z1, u0, u0 + uw, v0, vb, ct, cb, -1, 0, 0);
            quad(b, x1, y, z1, x0, y, z1, x0, ya, z1, x1, ya, z1, u0, u0 + uw, v0, vb, ct, cb, 0, 0, 1);
            quad(b, x0, y, z0, x1, y, z0, x1, ya, z0, x0, ya, z0, u0, u0 + uw, v0, vb, ct, cb, 0, 0, -1);
        }
    }

    private static void quad(BufferBuilder b, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float u1, float v0, float vb, int ct, int cb, float nx, float ny, float nz) {
        b.addVertex(ax, ay, az).setUv(u0, v0).setColor(ct).setNormal(nx, ny, nz);
        b.addVertex(bx, by, bz).setUv(u1, v0).setColor(ct).setNormal(nx, ny, nz);
        b.addVertex(cx, cy, cz).setUv(u1, vb).setColor(cb).setNormal(nx, ny, nz);
        b.addVertex(dx, dy, dz).setUv(u0, vb).setColor(cb).setNormal(nx, ny, nz);
    }
}

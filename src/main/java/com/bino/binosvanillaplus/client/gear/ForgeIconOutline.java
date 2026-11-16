package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Icone 2D de item +10 (inventario, hotbar...): contorno de 1 pixel em degrade, no estilo da espada de safira que o
// Bino desenhou. E' desenhado antes do item (GuiGraphicsForgeOutlineMixin), entao fica por tras dele.
//  - Se existir textures/gui/forge_icon/<nome>.png (18x18, feito a mao), ele e' usado.
//  - Senao o contorno e' gerado a partir do sprite 2D: dois pontos de brilho (a ponta de cima-direita e o canto de
//    baixo-direita). Perto deles o contorno e' quase branco e vai escurecendo pela rampa clara; longe deles vira a
//    rampa saturada (o "fundo" do degrade). Cores tiradas do desenho do Bino e aplicadas na cor do material.
//  - Os pixels de brilho piscam de vez em quando (com uma cruzinha de luz).
public final class ForgeIconOutline {
    private ForgeIconOutline() {}

    private record Outline(ResourceLocation tex, int w, int h, int[] sparks) {}
    private static final Outline NONE = new Outline(null, 0, 0, new int[0]);
    private static final Map<Item, Outline> CACHE = new HashMap<>();

    public static void draw(GuiGraphics g, ItemStack stack, int x, int y) {
        if (ForgeGlow.tier(stack) < 4) return;
        Outline o = CACHE.computeIfAbsent(stack.getItem(), ForgeIconOutline::build);
        if (o == NONE) return;
        RenderSystem.enableBlend();
        g.blit(o.tex, x - 1, y - 1, 18, 18, 0, 0, o.w, o.h, o.w, o.h);
        // brilhos piscando (cada um no seu tempo)
        double t = Util.getMillis() / 1000.0;
        for (int i = 0; i + 1 < o.sparks.length; i += 2) {
            double ph = (t / 2.6 + i * 0.37) % 1.0;
            double f = ph < 0.18 ? Math.sin(ph / 0.18 * Math.PI) : 0;
            if (f <= 0.01) continue;
            int px = x - 1 + o.sparks[i] * 18 / o.w, py = y - 1 + o.sparks[i + 1] * 18 / o.h;
            g.fill(px, py, px + 1, py + 1, white(f));
            int arm = white(f * 0.45);
            g.fill(px - 1, py, px, py + 1, arm);
            g.fill(px + 1, py, px + 2, py + 1, arm);
            g.fill(px, py - 1, px + 1, py, arm);
            g.fill(px, py + 1, px + 1, py + 2, arm);
        }
    }

    private static int white(double a) {
        return ((int) Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | 0xFFFFFF;
    }

    public static void clearCache() { CACHE.clear(); }

    // ---------------------------------------------------------------- montar a textura
    private static Outline build(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        var rm = Minecraft.getInstance().getResourceManager();
        var res = rm.getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"));
        if (res.isEmpty()) return NONE;   // item sem sprite 2D proprio (blocos etc.): sem contorno
        try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            NativeImage out;
            int[] sparks;
            var hand = rm.getResource(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID,
                    "textures/gui/forge_icon/" + id.getPath() + ".png"));
            if (hand.isPresent()) {
                try (InputStream hin = hand.get().open()) { out = NativeImage.read(hin); }
                sparks = handSparks(out, img);
            } else {
                List<Integer> sp = new ArrayList<>();
                out = generate(img, ForgeGlow.color(item), sp);
                sparks = sp.stream().mapToInt(Integer::intValue).toArray();
            }
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID,
                    "forge_outline/" + id.getNamespace() + "/" + id.getPath());
            int w = out.getWidth(), h = out.getHeight();
            Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(out));
            return new Outline(tex, w, h, sparks);
        } catch (Exception e) {
            return NONE;
        }
    }

    // no desenho feito a mao, os brilhos sao os pixels do contorno quase brancos
    private static int[] handSparks(NativeImage hand, NativeImage sprite) {
        List<Integer> out = new ArrayList<>();
        int W = hand.getWidth(), H = hand.getHeight(), w = sprite.getWidth(), h = Math.min(sprite.getHeight(), w);
        float[] hsv = new float[3];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int c = hand.getPixelRGBA(x, y);
                if ((c >>> 24) < 128) continue;
                int sx = (x - 1) * w / Math.max(1, W - 2), sy = (y - 1) * h / Math.max(1, H - 2);
                boolean inside = x >= 1 && y >= 1 && x < W - 1 && y < H - 1 && (sprite.getPixelRGBA(sx, sy) >>> 24) > 16;
                if (inside) continue;
                hsv(c & 255, (c >> 8) & 255, (c >> 16) & 255, hsv);
                if (hsv[1] < 0.2f && hsv[2] > 0.92f) { out.add(x); out.add(y); }
            }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---------------------------------------------------------------- contorno gerado
    // rampa medida no desenho do Bino: {desvio de matiz rumo ao amarelo, saturacao, brilho}
    private static final float[] SP = {12, 0.12f, 1.00f}, L1 = {12, 0.35f, 0.93f}, L2 = {12, 0.48f, 0.88f},
            L3 = {12, 0.63f, 0.90f}, DK = {12, 0.74f, 0.82f}, S1 = {12, 0.93f, 1.00f}, S2 = {0, 1.00f, 1.00f},
            S3 = {0, 0.89f, 0.84f};
    private static final float REF_SAT = 0.63f;   // saturacao da cor do material no desenho original

    private static NativeImage generate(NativeImage img, int baseRgb, List<Integer> sparks) {
        int w = img.getWidth(), h = Math.min(img.getHeight(), w), W = w + 2, H = h + 2;
        boolean[] solid = new boolean[W * H];   // em coordenadas do contorno (sprite deslocado 1 pixel)
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) solid[(y + 1) * W + x + 1] = (img.getPixelRGBA(x, y) >>> 24) > 16;
        boolean[] border = new boolean[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                if (solid[y * W + x]) continue;
                border[y * W + x] = s(solid, W, H, x + 1, y) || s(solid, W, H, x - 1, y) || s(solid, W, H, x, y + 1) || s(solid, W, H, x, y - 1);
            }
        // cantos retos (ex.: canto da guarda): pixel so na diagonal onde duas bordas retas se encontram
        boolean[] corner = new boolean[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                if (border[y * W + x] || solid[y * W + x]) continue;
                for (int dx = -1; dx <= 1; dx += 2)
                    for (int dy = -1; dy <= 1; dy += 2) {
                        int ix = x + dx, iy = y + dy;
                        if (!(s(solid, W, H, ix, iy) && s(solid, W, H, ix + dx, iy) && s(solid, W, H, ix, iy + dy))) continue;
                        int a = run(border, W, H, x, y, dx, 0), b = run(border, W, H, x, y, 0, dy);
                        if (a >= 2 && b >= 2 && a + b >= 5) corner[y * W + x] = true;
                    }
            }
        for (int i = 0; i < border.length; i++) border[i] |= corner[i];

        // pontos de brilho: mais em cima-direita; e o mais embaixo-direita que esteja longe do primeiro
        int A = -1, B = -1;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                if (!border[y * W + x]) continue;
                if (A < 0 || x - y > A % W - A / W || x - y == A % W - A / W && y < A / W) A = y * W + x;
            }
        NativeImage out = new NativeImage(W, H, true);
        if (A < 0) return out;
        int[] dA = bfs(border, W, H, A);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int i = y * W + x;
                if (!border[i] || dA[i] < 10) continue;
                if (B < 0 || x + y > B % W + B / W || x + y == B % W + B / W && x > B % W) B = i;
            }
        int[] dB = B >= 0 ? bfs(border, W, H, B) : null;
        int[] md = new int[W * H];
        for (int i = 0; i < md.length; i++) md[i] = Math.min(dA[i], dB == null ? Integer.MAX_VALUE : dB[i]);

        float[] base = new float[3];
        hsv((baseRgb >> 16) & 255, (baseRgb >> 8) & 255, baseRgb & 255, base);
        float satK = Math.min(1f, base[1] / REF_SAT);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int i = y * W + x;
                if (!border[i]) continue;
                int d = md[i];
                float[] k;
                if (d == 0) { k = SP; sparks.add(x); sparks.add(y); }
                else if (d <= 2) k = L1;
                else if (d <= 4) k = L2;
                else if (d <= 7) k = dB != null && d >= 5 && localMax(border, md, W, H, x, y) ? DK : L3;
                else if (d <= 10) k = S1;
                else k = d % 3 == 0 ? S3 : S2;
                int rgb = hsvToRgb(towardYellow(base[0], k[0]), k[1] * satK, k[2]);
                out.setPixelRGBA(x, y, 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF));   // ABGR
            }
        return out;
    }

    private static boolean s(boolean[] a, int W, int H, int x, int y) {
        return x >= 0 && y >= 0 && x < W && y < H && a[y * W + x];
    }

    private static int run(boolean[] border, int W, int H, int x, int y, int dx, int dy) {
        int n = 0;
        while (s(border, W, H, x + dx * (n + 1), y + dy * (n + 1))) n++;
        return n;
    }

    // distancia andando pelo contorno (vizinhos nas 8 direcoes)
    private static int[] bfs(boolean[] border, int W, int H, int src) {
        int[] d = new int[W * H];
        java.util.Arrays.fill(d, Integer.MAX_VALUE);
        d[src] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(src);
        while (!q.isEmpty()) {
            int p = q.poll(), px = p % W, py = p / W;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = px + dx, ny = py + dy;
                    if ((dx | dy) == 0 || !s(border, W, H, nx, ny)) continue;
                    int n = ny * W + nx;
                    if (d[n] != Integer.MAX_VALUE) continue;
                    d[n] = d[p] + 1;
                    q.add(n);
                }
        }
        return d;
    }

    // o ponto mais longe dos dois brilhos (no meio do caminho entre eles) fica um pouco mais escuro
    private static boolean localMax(boolean[] border, int[] md, int W, int H, int x, int y) {
        int d = md[y * W + x];
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                if ((dx | dy) != 0 && s(border, W, H, x + dx, y + dy) && md[(y + dy) * W + x + dx] > d) return false;
        return true;
    }

    // ---------------------------------------------------------------- cores
    private static float towardYellow(float h, float deg) {
        float d = ((60 - h + 540) % 360) - 180;   // caminho mais curto ate o amarelo (60)
        float step = Math.max(-deg, Math.min(deg, d));
        return (h + step + 360) % 360;
    }

    private static void hsv(int r, int g, int b, float[] out) {
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * ((b - r) / d + 2f);
        else h = 60f * ((r - g) / d + 4f);
        if (h < 0) h += 360f;
        out[0] = h; out[1] = max == 0 ? 0 : d / max; out[2] = max / 255f;
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s, hp = (h % 360) / 60f, x = c * (1 - Math.abs(hp % 2 - 1)), m = v - c, r, g, b;
        if (hp < 1) { r = c; g = x; b = 0; }
        else if (hp < 2) { r = x; g = c; b = 0; }
        else if (hp < 3) { r = 0; g = c; b = x; }
        else if (hp < 4) { r = 0; g = x; b = c; }
        else if (hp < 5) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        int R = Math.round((r + m) * 255), G = Math.round((g + m) * 255), B = Math.round((b + m) * 255);
        return (R << 16) | (G << 8) | B;
    }
}

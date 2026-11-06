package com.bino.binosvanillaplus.client.faction;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

// Textura feita na hora a partir de outra (a do jogo ou a do pacote de recursos em uso, ex.: Fresh Animations):
// le a original e troca as cores pixel a pixel. Recarrega junto com os recursos (F3+T).
public class RecolorTexture extends SimpleTexture {
    public interface PixelMap { int map(int r, int g, int b, int x, int y); }   // devolve 0xRRGGBB

    private final ResourceLocation source;
    private final PixelMap fn;

    public RecolorTexture(ResourceLocation target, ResourceLocation source, PixelMap fn) {
        super(target);
        this.source = source;
        this.fn = fn;
    }

    @Override
    protected SimpleTexture.TextureImage getTextureImage(ResourceManager rm) {
        try (InputStream in = rm.open(source)) {
            NativeImage img = NativeImage.read(in);
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
                int abgr = img.getPixelRGBA(x, y);   // (o nome diz RGBA, mas vem em ABGR)
                int a = abgr >>> 24;
                if (a == 0) continue;
                int r = abgr & 255, g = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
                int rgb = fn.map(r, g, b, x, y);
                img.setPixelRGBA(x, y, (a << 24) | ((rgb & 255) << 16) | (((rgb >> 8) & 255) << 8) | ((rgb >> 16) & 255));
            }
            return new SimpleTexture.TextureImage(null, img);
        } catch (IOException e) {
            return new SimpleTexture.TextureImage(e);
        }
    }

    // ---- paletas

    private static int ramp(int c0, int c1, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c0 >> 16) & 255) + (((c1 >> 16) & 255) - ((c0 >> 16) & 255)) * t);
        int g = (int) (((c0 >> 8) & 255) + (((c1 >> 8) & 255) - ((c0 >> 8) & 255)) * t);
        int b = (int) ((c0 & 255) + ((c1 & 255) - (c0 & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static float lum(int r, int g, int b) { return (0.299f * r + 0.587f * g + 0.114f * b) / 255f; }

    // Vindicator -> Vingador Villager: pele salmao de villager, olhos verdes, casaco de couro, calca verde
    public static int villagerGuard(int r, int g, int b, int x, int y) {
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        float l = lum(r, g, b), sat = (mx - mn) / (float) Math.max(mx, 1);
        if (b > r + 18 && b >= g - 10 && l > 0.5f && y < 18) return 0x40963C;                       // olho
        if (sat < 0.16f && l > 0.42f) return ramp(0x7A4E3A, 0xDEAA8C, (l - 0.42f) / 0.5f);          // pele
        if (g > r + 12 && b > r + 12) return ramp(0x1E3A1C, 0x5C8C46, l / 0.5f);                    // calca
        if (r > g + 8 && l < 0.42f) return ramp(0x3A2414, 0x8C603C, l / 0.32f);                     // casaco
        if (sat < 0.2f && l <= 0.42f) return ramp(0x241A12, 0x705238, l / 0.42f);                   // escuros
        return (r << 16) | (g << 8) | b;
    }

    // Golem de ferro -> Golem Pillager com as cores do Ravager: couro cinza-escuro, metal azulado, detalhes laranja
    public static int pillagerGolem(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b);
        if (r > 120 && g < 70 && b < 70) return (r << 16) | (g << 8) | b;                           // olhos vermelhos
        if (g > r + 12 && g > b + 12) return ramp(0x465C5A, 0xA0BEBA, l / 0.6f);                    // cipos -> metal
        if (r > 170 && g > 140 && b < 100) return ramp(0x963A14, 0xE47030, (l - 0.5f) / 0.4f);      // flores -> laranja
        return ramp(0x282421, 0x746A60, (l - 0.30f) / 0.62f);                                       // ferro -> couro
    }
}

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

    private static float sat(int r, int g, int b) {
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        return (mx - mn) / (float) Math.max(mx, 1);
    }

    private static final int SALMON_0 = 0x7A4E3A, SALMON_1 = 0xDEAA8C;   // pele de villager
    private static final int GRAY_0 = 0x464C4C, GRAY_1 = 0xC4CAC8;       // pele de pillager

    // Evocador -> Mago Villager: pele salmao, manto esmeralda com o mesmo bordado dourado, camisa de couro
    public static int villagerMage(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b), s = sat(r, g, b);
        if (r > g && g > b && s > 0.4f) return (r << 16) | (g << 8) | b;                            // ouro
        if (b > r + 18 && l > 0.5f && y < 18) return 0x40963C;                                       // olho
        if (s < 0.14f && l > 0.38f) return ramp(SALMON_0, SALMON_1, (l - 0.38f) / 0.4f);             // pele
        if (b >= r && g > r && l > 0.15f) return ramp(0x3A2414, 0x8C603C, (l - 0.15f) / 0.2f);       // camisa
        return ramp(0x0E281A, 0x347446, l / 0.16f);                                                  // manto
    }

    // Vex -> Espirito Villager: azul de allay; na investida brilha verde-esmeralda
    public static int villagerSpirit(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b);
        if (r > b + 20) return ramp(0x146E3C, 0x78FFAA, (l - 0.2f) / 0.6f);
        return ramp(0x1246BE, 0xAAF5FF, (l - 0.3f) / 0.65f);
    }

    // Bruxa -> Bruxa Villager: pele salmao, vestido verde, chapeu de couro (a joia verde fica)
    public static int villagerWitch(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b), s = sat(r, g, b);
        if (b > g + 20 && r > g) return ramp(0x163016, 0x548C3A, l / 0.2f);                         // roxo
        if (y >= 64 && s < 0.25f && l < 0.2f) return ramp(0x281A10, 0x6E4C2E, l / 0.18f);           // chapeu
        if (y < 64 && s < 0.25f && l > 0.5f) return ramp(SALMON_0, SALMON_1, (l - 0.4f) / 0.4f);     // pele
        return (r << 16) | (g << 8) | b;
    }

    // Ravager -> Ravager Villager: couro marrom de boi, metal de latao, detalhes esmeralda
    public static int villagerRavager(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b);
        if (b > r + 15 && g > r + 15) return ramp(0x786028, 0xE6C86E, (l - 0.3f) / 0.45f);
        if (r > g + 40) return ramp(0x1A6028, 0x5AC85A, (l - 0.1f) / 0.4f);
        return ramp(0x281A10, 0xA27650, (l - 0.12f) / 0.32f);
    }

    // roupas de villager -> paleta pillager (carvao, vinho, petroleo)
    public static int pillagerize(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b), s = sat(r, g, b);
        if (s < 0.12f) return ramp(0x1A1A1C, 0x969698, l / 0.75f);
        if (r >= g && r >= b) {
            if (g > b + 25 && l > 0.5f) return ramp(0x5A5A54, 0xC8C4AA, (l - 0.5f) / 0.4f);         // palha -> pano claro
            return ramp(0x220E10, 0x8C3434, l / 0.55f);                                             // vinho
        }
        return ramp(0x0E2022, 0x467874, l / 0.6f);                                                  // petroleo
    }

    // corpo de villager -> Pillager civil: pele cinza de pillager, roupa na paleta pillager
    public static int pillagerSkin(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b), s = sat(r, g, b);
        if (g > r + 10 && g > b) return (r << 16) | (g << 8) | b;                                    // olho
        if (r > g && g > b && s > 0.18f && l > 0.35f) return ramp(GRAY_0, GRAY_1, (l - 0.3f) / 0.5f);
        return pillagerize(r, g, b, x, y);
    }

    // Mercador ambulante -> Contrabandista Pillager: pele cinza, manto carvao, faixa vinho
    public static int smuggler(int r, int g, int b, int x, int y) {
        float l = lum(r, g, b), s = sat(r, g, b);
        if (g > r + 10 && g > b && y < 18) return (r << 16) | (g << 8) | b;
        if (r > g && g > b && s > 0.18f && s < 0.5f && l > 0.42f) return ramp(GRAY_0, GRAY_1, (l - 0.3f) / 0.5f);
        if (b > r + 25) return ramp(0x14161A, 0x585A62, l / 0.5f);
        if (r > 120 && g < 40) return ramp(0x3C1014, 0x96282C, (l - 0.1f) / 0.3f);
        if (r > g && g > b && s > 0.6f) return (r << 16) | (g << 8) | b;
        return pillagerize(r, g, b, x, y);
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

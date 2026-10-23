package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Cria, na hora de jogar, uma cópia da textura do Allay com a cor principal trocada.
// Assim o mod não precisa trazer nenhuma imagem do jogo dentro dele.
public class AllayColorTextures {

    private static final ResourceLocation VANILLA =
            ResourceLocation.withDefaultNamespace("textures/entity/allay/allay.png");

    // Cor de cada corante, na ordem do jogo (DyeColor.getId()):
    // branco, laranja, magenta, azul-claro, amarelo, verde-limão, rosa, cinza,
    // cinza-claro, ciano, roxo, azul, marrom, verde, vermelho, preto
    private static final int[] DYE_RGB = {
            0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

    private static final Map<Integer, ResourceLocation> CACHE = new HashMap<>();

    // Devolve a textura da cor pedida (cria na primeira vez)
    public static ResourceLocation get(int colorIndex) {
        return CACHE.computeIfAbsent(colorIndex, AllayColorTextures::create);
    }

    // Esquece as texturas feitas (chamado quando os recursos recarregam)
    public static void clear() {
        CACHE.clear();
    }

    private static ResourceLocation create(int colorIndex) {
        Minecraft mc = Minecraft.getInstance();
        try (InputStream in = mc.getResourceManager().open(VANILLA)) {
            BufferedImage source = ImageIO.read(in);
            // O PNG do jogo usa uma paleta de cores limitada: pintar nele faria o Java "arredondar" cada cor
            // para a mais parecida da paleta (por isso o pet ficava cinza). Copiamos para uma imagem de cores completas.
            BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) image.setRGB(x, y, source.getRGB(x, y));
            }
            recolor(image, DYE_RGB[Math.floorMod(colorIndex, DYE_RGB.length)]);

            // Converte a imagem pintada para o formato do Minecraft
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "dynamic/allay_" + colorIndex);
            mc.getTextureManager().register(id, new DynamicTexture(nativeImage));
            return id;
        } catch (Exception e) {
            BinosVanillaMod.LOGGER.error("Não consegui pintar a textura do Allay", e);
            return VANILLA;   // se der erro, usa a textura original
        }
    }

    // Troca só os pixels AZUIS (o corpo). Olhos, branco e contornos ficam como estão.
    private static void recolor(BufferedImage img, int dyeRgb) {
        float[] dye = rgbToHsv(dyeRgb);

        // 1º passo: descobre a saturação e o brilho médios do azul, para manter o sombreado original
        double sumS = 0, sumV = 0; int count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) == 0) continue;
                float[] hsv = rgbToHsv(argb);
                if (isBody(hsv)) { sumS += hsv[1]; sumV += hsv[2]; count++; }
            }
        }
        if (count == 0) return;
        float meanS = (float) (sumS / count), meanV = (float) (sumV / count);

        // 2º passo: pinta cada pixel azul com a cor do corante, preservando as variações de claro/escuro
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) == 0) continue;
                float[] hsv = rgbToHsv(argb);
                if (!isBody(hsv)) continue;
                float s = clamp(dye[1] * (hsv[1] / meanS));
                float v = clamp(dye[2] * (hsv[2] / meanV));
                img.setRGB(x, y, (argb & 0xFF000000) | hsvToRgb(dye[0], s, v));
            }
        }
    }

    // "Corpo" = pixel azul com cor de verdade (matiz entre 170° e 250° e saturação acima de 20%)
    private static boolean isBody(float[] hsv) {
        return hsv[1] > 0.2f && hsv[0] >= 170f && hsv[0] <= 250f;
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    // Converte RGB (0xRRGGBB) em {matiz 0-360, saturação 0-1, brilho 0-1}
    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * (((b - r) / d) + 2f);
        else h = 60f * (((r - g) / d) + 4f);
        if (h < 0) h += 360f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    // Converte HSV de volta para 0xRRGGBB
    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c;
        float r, g, b;
        int sector = (int) (h / 60f) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}

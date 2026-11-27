package com.bino.binosvanillaplus.client.stats;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

// Numeros desenhados pelo Bino para o visor:
//  - hud_digits.png (tools/ui/digitos_hud.png): digitos 4x8 brancos com sombra cinza, mais "°", "C" e "F". Pintados
//    pela cor pedida (a sombra escurece junto). Usados na temperatura e no numero de barras de experiencia.
//  - level_digits.png (tools/ui/gen_nivel_numeros.py, escala 1): digitos 8x10 com volume, uma linha de cores por
//    dezena (cores do Bino) e a ultima para o 100. Usados no nivel do personagem.
public final class HudDigits {
    private HudDigits() {}

    private static final ResourceLocation SMALL = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/hud_digits.png");
    private static final ResourceLocation LEVEL = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/level_digits.png");
    public static final int GW = 4, GH = 8, GAP = 1;
    private static final int LW = 8, LH = 10;

    /** largura (sem escala) de um texto feito com os digitos pequenos */
    public static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += (s.charAt(i) == '-' ? 3 : GW) + (i > 0 ? GAP : 0);
        return w;
    }

    /** escreve com os digitos pequenos ('0'-'9', '°', 'C', 'F', '-'); x,y = canto de cima a esquerda (sem escala) */
    public static void draw(GuiGraphics g, String s, float x, float y, float scale, int rgb) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1f);
        int cx = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0) cx += GAP;
            if (c == '-') {   // menos: tracinho branco com sombra
                g.fill(cx, 3, cx + 3, 4, 0xFFFFFFFF);
                g.fill(cx, 4, cx + 3, 5, 0xFF6F8187);
                cx += 3;
                continue;
            }
            int idx = c >= '0' && c <= '9' ? c - '0' : c == '°' ? 10 : c == 'C' ? 11 : c == 'F' ? 12 : -1;
            if (idx >= 0) g.blit(SMALL, cx, 0, (idx % 5) * GW, (idx / 5) * GH, GW, GH, 20, 24);
            cx += GW;
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
    }

    /** nivel do personagem com os digitos grandes, na cor da dezena; centrado em cx, y = topo */
    public static void level(GuiGraphics g, int level, float cx, float y) {
        String s = String.valueOf(level);
        int row = level >= 100 ? 10 : Math.max(0, level / 10);
        float w = s.length() * (LW - 1) + 1;
        float x = cx - w / 2f;
        RenderSystem.enableBlend();
        for (int i = 0; i < s.length(); i++) {
            int d = s.charAt(i) - '0';
            g.blit(LEVEL, Math.round(x + i * (LW - 1)), Math.round(y), d * LW, row * LH, LW, LH, 80, 110);
        }
    }
}

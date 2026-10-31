package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Visual das telas do mapa (layout do Figma): painel escuro translucido, titulo grande, botoes cinza-claro,
// campos de texto cinza, botao X. Todas as telas do mapa usam estes helpers para ficarem iguais.
public final class MapUi {
    private MapUi() {}

    public static final int PANEL = 0xE01B1E1C;      // fundo do painel (deixa ver um pouco o jogo)
    public static final int LIST_BG = 0xF0191C19;    // area da lista
    public static final int FIELD = 0xFF7F8281;      // campo de texto
    public static final int BTN = 0xFFD9D9D9;        // botao claro
    public static final int BTN_HOVER = 0xFFEDEDED;
    public static final int BTN_TEXT = 0xFF4E4E4E;
    public static final int DARK_BTN = 0xFF484A49;   // botoes pequenos das linhas da lista
    public static final int DARK_BTN_HOVER = 0xFF5E615F;
    public static final int MENU_BG = 0xC81E211F;    // menus e paineis sobre o mapa

    public static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/map/" + name + ".png");
    }

    // retangulo com cantos arredondados (1 px)
    public static void round(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x + 1, y, x + w - 1, y + h, col);
        g.fill(x, y + 1, x + 1, y + h - 1, col);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) { round(g, x, y, w, h, PANEL); }

    // titulo em fonte 2x, centrado em cx; y = topo do texto
    public static void title(GuiGraphics g, Font font, String text, int cx, int y) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text), y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    // titulo 2x alinhado a esquerda
    public static void titleLeft(GuiGraphics g, Font font, String text, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    // texto em escala livre
    public static void text(GuiGraphics g, Font font, String text, float x, float y, float scale, int col, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, col, shadow);
        g.pose().popPose();
    }

    // fundo cinza de um campo de texto (o EditBox fica sem borda por cima)
    public static void field(GuiGraphics g, EditBox box) {
        round(g, box.getX() - 5, box.getY() - 4, box.getWidth() + 10, box.getHeight() + 4, FIELD);
    }

    // EditBox sem borda, texto branco; (x,y,w,h) = retangulo cinza inteiro
    public static EditBox editBox(Font font, int x, int y, int w, int h, String hint) {
        EditBox b = new EditBox(font, x + 5, y + 4, w - 10, h - 4, Component.literal(hint));
        b.setBordered(false);
        b.setTextColor(0xFFFFFFFF);
        if (!hint.isEmpty()) b.setHint(Component.literal(hint));
        return b;
    }

    // ------------------------------------------------------------------ botoes

    public enum Style { LIGHT, DARK }

    // botao plano (claro com texto cinza, ou escuro pequeno com simbolo); pode ter icone em vez de texto
    public static class FlatButton extends AbstractButton {
        private final Runnable action;
        private final Style style;
        private ResourceLocation icon;
        private int iconSize = 16;

        public FlatButton(int x, int y, int w, int h, String label, Style style, Runnable action) {
            super(x, y, w, h, Component.literal(label));
            this.action = action;
            this.style = style;
        }

        public FlatButton icon(ResourceLocation tex, int size) { this.icon = tex; this.iconSize = size; return this; }

        public void setLabel(String s) { setMessage(Component.literal(s)); }

        @Override
        public void onPress() { action.run(); }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean over = isHoveredOrFocused() && active;
            if (style == Style.LIGHT) {
                round(g, getX(), getY(), width, height, over ? BTN_HOVER : BTN);
            } else {
                round(g, getX(), getY(), width, height, 0xFF5B5E5C);
                round(g, getX() + 1, getY() + 1, width - 2, height - 2, over ? DARK_BTN_HOVER : DARK_BTN);
            }
            Font font = Minecraft.getInstance().font;
            if (icon != null) {
                int ix = getX() + (width - iconSize) / 2, iy = getY() + (height - iconSize) / 2;
                g.blit(icon, ix, iy, iconSize, iconSize, 0, 0, 17, 17, 17, 17);
            } else {
                int col = !active ? 0xFF9A9A9A : style == Style.LIGHT ? BTN_TEXT : 0xFFB8B8B8;
                String s = getMessage().getString();
                g.drawString(font, s, getX() + (width - font.width(s)) / 2 + (width % 2 == 0 ? 0 : 1), getY() + (height - 8) / 2 + 1, col, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }

    // botao X (fechar): quadrado claro com um x cinza
    public static FlatButton closeButton(int x, int y, int size, Runnable action) {
        return new FlatButton(x, y, size, size, "x", Style.LIGHT, action);
    }
}

package com.bino.binosvanillaplus.client.ui;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.WeakHashMap;

// Padrao visual do Bino's Vanilla+ (Figma): painel escuro translucido, titulo grande, botoes cinza-claro,
// campos cinza, X de fechar. Tudo com animacao de hover/foco/clique. Use em TODAS as telas novas.
public final class BinoUi {
    private BinoUi() {}

    // ---- paleta (mude aqui para mudar todas as telas) ----
    public static final int PANEL = 0xE01B1E1C;      // fundo do painel (deixa ver um pouco o jogo)
    public static final int LIST_BG = 0xF0191C19;    // area de lista
    public static final int FIELD = 0xFF7F8281;      // campo de texto
    public static final int FIELD_FOCUS = 0xFF8E9190;
    public static final int BTN = 0xFFD9D9D9;        // botao claro
    public static final int BTN_HOVER = 0xFFF2F2F2;
    public static final int BTN_PRESS = 0xFFB9B9B9;
    public static final int BTN_TEXT = 0xFF4E4E4E;
    public static final int DARK_BTN = 0xFF484A49;   // botoes pequenos escuros (linhas de lista)
    public static final int DARK_BTN_HOVER = 0xFF626563;
    public static final int MENU_BG = 0xD81E211F;    // menus e paineis flutuantes
    public static final int ACCENT = 0xFFFEC400;     // amarelo de destaque (selecao)
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF8A8A8A;

    public static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/" + path + ".png");
    }

    // retangulo com cantos arredondados (1 px)
    public static void round(GuiGraphics g, int x, int y, int w, int h, int col) {
        if (w <= 0 || h <= 0) return;
        g.fill(x + 1, y, x + w - 1, y + h, col);
        g.fill(x, y + 1, x + 1, y + h - 1, col);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    // contorno de 1 px com cantos arredondados
    public static void outline(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x + 1, y - 1, x + w - 1, y, col);
        g.fill(x + 1, y + h, x + w - 1, y + h + 1, col);
        g.fill(x - 1, y + 1, x, y + h - 1, col);
        g.fill(x + w, y + 1, x + w + 1, y + h - 1, col);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) { round(g, x, y, w, h, PANEL); }

    // titulo em fonte 2x centrado em cx (y = topo do texto)
    public static void title(GuiGraphics g, Font font, String text, int cx, int y) {
        text(g, font, text, cx - font.width(text), y, 2f, TEXT, false);
    }

    public static void titleLeft(GuiGraphics g, Font font, String text, int x, int y) {
        text(g, font, text, x, y, 2f, TEXT, false);
    }

    public static void text(GuiGraphics g, Font font, String text, float x, float y, float scale, int col, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, col, shadow);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ campos de texto

    private static final Map<EditBox, Tween> FIELD_FOCUS_T = new WeakHashMap<>();

    // EditBox sem borda e texto branco; (x,y,w,h) = retangulo cinza inteiro (desenhe o fundo com field())
    public static EditBox editBox(Font font, int x, int y, int w, int h, String hint) {
        EditBox b = new EditBox(font, x + 5, y + (h - 8) / 2, w - 10, h - (h - 8) / 2, Component.literal(hint));
        b.setBordered(false);
        b.setTextColor(TEXT);
        if (!hint.isEmpty()) b.setHint(Component.literal(hint).withColor(0xFFDADADA));
        return b;
    }

    // fundo do campo: clareia e ganha contorno quando focado (animado)
    public static void field(GuiGraphics g, EditBox box) {
        Tween t = FIELD_FOCUS_T.computeIfAbsent(box, k -> new Tween(0f, 60f));
        float f = t.get(box.isFocused() ? 1f : 0f);
        int x = box.getX() - 5, y = box.getY() - (16 - 8) / 2, w = box.getWidth() + 10, h = 16;
        round(g, x, y, w, h, Ease.mix(FIELD, FIELD_FOCUS, f));
        if (f > 0.01f) outline(g, x, y, w, h, Ease.alpha(0xFFE6E6E6, f));
    }

    // ------------------------------------------------------------------ botoes

    public enum Style { LIGHT, DARK }

    // Botao plano: hover clareia suavemente, clique "afunda" 1 px, foco pelo teclado mostra contorno.
    // Pode ter icone (PNG 17x17) em vez de texto.
    public static class Button extends AbstractButton {
        private final Runnable action;
        private final Style style;
        private ResourceLocation icon;
        private int iconSize = 16;
        private final Tween hover = new Tween(0f, 45f);
        private long pressedAt = -1000;

        public Button(int x, int y, int w, int h, String label, Style style, Runnable action) {
            super(x, y, w, h, Component.literal(label));
            this.action = action;
            this.style = style;
        }

        public Button icon(ResourceLocation tex, int size) { this.icon = tex; this.iconSize = size; return this; }

        public Button tooltip(String text) { setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(text))); return this; }

        public void setLabel(String s) { setMessage(Component.literal(s)); }

        @Override
        public void onPress() {
            pressedAt = Ease.now();
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            float h = hover.get(isHovered() && active ? 1f : 0f);
            float press = 1f - Ease.since(pressedAt, 140);         // 1 logo apos o clique -> 0
            int dy = press > 0.4f ? 1 : 0;
            int x = getX(), y = getY() + dy;
            if (style == Style.LIGHT) {
                int c = Ease.mix(BTN, BTN_HOVER, h);
                c = Ease.mix(c, BTN_PRESS, press * 0.8f);
                if (!active) c = 0xFF9C9C9C;
                round(g, x, y, width, height, c);
                if (dy == 0 && active) g.fill(x + 1, y + height - 1, x + width - 1, y + height, 0x22000000);   // sombra sutil embaixo
            } else {
                round(g, x, y, width, height, 0xFF5B5E5C);
                round(g, x + 1, y + 1, width - 2, height - 2, Ease.mix(Ease.mix(DARK_BTN, DARK_BTN_HOVER, h), 0xFF3A3C3B, press));
            }
            if (isFocused() && !isHovered()) outline(g, x, y, width, height, style == Style.LIGHT ? ACCENT : 0xFFE0E0E0);   // navegando com Tab
            Font font = Minecraft.getInstance().font;
            if (icon != null) {
                int ix = x + (width - iconSize) / 2, iy = y + (height - iconSize) / 2;
                g.blit(icon, ix, iy, iconSize, iconSize, 0, 0, 17, 17, 17, 17);
            } else {
                int col = !active ? 0xFF6A6A6A : style == Style.LIGHT ? BTN_TEXT : Ease.mix(0xFFB8B8B8, 0xFFFFFFFF, h);
                String s = getMessage().getString();
                g.drawString(font, s, x + (width - font.width(s) + 1) / 2, y + (height - 8) / 2 + 1, col, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }

    // X de fechar
    public static Button closeButton(int x, int y, int size, Runnable action) {
        return new Button(x, y, size, size, "x", Style.LIGHT, action);
    }

    // botao escuro pequeno desenhado "na mao" (para linhas de lista); 'hover' deve ser um Tween guardado por linha/botao
    public static void darkButton(GuiGraphics g, Font font, int x, int y, int w, int h, String t, float hover) {
        round(g, x, y, w, h, 0xFF5B5E5C);
        round(g, x + 1, y + 1, w - 2, h - 2, Ease.mix(DARK_BTN, DARK_BTN_HOVER, hover));
        g.drawString(font, t, x + (w - font.width(t) + 1) / 2, y + (h - 8) / 2 + 1, Ease.mix(0xFFB8B8B8, 0xFFFFFFFF, hover), false);
    }
}

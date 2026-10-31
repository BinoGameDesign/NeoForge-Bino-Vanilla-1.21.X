package com.bino.binosvanillaplus.client.map;

import com.bino.binosvanillaplus.client.ui.BinoScreen;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

// Opcoes do mapa (layout do Figma "Map Options"): botoes claros na largura toda + "ok" no canto.
public class MapOptionsScreen extends BinoScreen {
    private int L, T, W, H, rowH;

    public MapOptionsScreen(@Nullable Screen parent) {
        super("Map Options", parent);
    }

    @Override
    protected int[] panelBounds() { return new int[]{L, T, W, H}; }

    @Override
    protected void build() {
        MapOptions.ensureLoaded();
        W = Math.min(355, width - 24);
        H = Math.min(213, height - 34);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        rowH = Math.max(15, Math.min(19, (H - 18 - 22) / 9));
        int y = T + 18;
        toggle(y, "Marcações no minimapa", () -> MapOptions.minimapPins, v -> MapOptions.minimapPins = v); y += rowH;
        toggle(y, "Marcações no mapa-mundi", () -> MapOptions.worldPins, v -> MapOptions.worldPins = v); y += rowH;
        toggle(y, "Feixe de luz no mundo", () -> MapOptions.worldBeams, v -> MapOptions.worldBeams = v); y += rowH;
        toggle(y, "Nomes no mapa-mundi", () -> MapOptions.worldLabels, v -> MapOptions.worldLabels = v); y += rowH;
        toggle(y, "Distância ao lado do nome", () -> MapOptions.showDistance, v -> MapOptions.showDistance = v); y += rowH;
        toggle(y, "Mobs no minimapa", () -> MapOptions.showMobs, v -> MapOptions.showMobs = v); y += rowH;
        toggle(y, "Marcar local da morte", () -> MapOptions.deathPoint, v -> MapOptions.deathPoint = v); y += rowH;
        BinoUi.Button[] op = new BinoUi.Button[1];
        op[0] = addRenderableWidget(new BinoUi.Button(L + 13, y, W - 26, rowH - 3, opacityText(), BinoUi.Style.LIGHT, () -> {
            int[] steps = {40, 60, 80, 100};
            int cur = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] == MapOptions.minimapOpacity) cur = i;
            MapOptions.minimapOpacity = steps[(cur + 1) % steps.length];
            op[0].setLabel(opacityText());
            MapOptions.save();
        }));
        addRenderableWidget(new BinoUi.Button(L + W - 25, T + H - 9, 19, 19, "ok", BinoUi.Style.LIGHT, this::onClose));
    }

    private static String opacityText() { return "Opacidade do minimapa: " + MapOptions.minimapOpacity + "%"; }

    private void toggle(int y, String label, BooleanSupplier get, Consumer<Boolean> set) {
        BinoUi.Button[] b = new BinoUi.Button[1];
        b[0] = addRenderableWidget(new BinoUi.Button(L + 13, y, W - 26, rowH - 3, text(label, get.getAsBoolean()), BinoUi.Style.LIGHT, () -> {
            set.accept(!get.getAsBoolean());
            b[0].setLabel(text(label, get.getAsBoolean()));
            MapOptions.save();
        }));
    }

    private static String text(String label, boolean on) { return label + ": " + (on ? "Ligado" : "Desligado"); }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.panel(g, L, T, W, H);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.title(g, font, "Map Options", L + W / 2, T - 6);
    }

    @Override
    public void onClose() {
        MapOptions.save();
        super.onClose();
    }
}

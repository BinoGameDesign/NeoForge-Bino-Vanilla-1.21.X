package com.bino.binosvanillaplus.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

// Opcoes do mapa e das marcacoes
public class MapOptionsScreen extends Screen {
    @Nullable private final Screen parent;
    private static final int W = 260;
    private int left, top, H;

    public MapOptionsScreen(@Nullable Screen parent) {
        super(Component.literal("Opções do mapa"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapOptions.ensureLoaded();
        H = 36 + 9 * 22 + 30;
        left = (width - W) / 2;
        top = (height - H) / 2;
        int y = top + 26;
        toggle(y, "Marcações no minimapa", () -> MapOptions.minimapPins, v -> MapOptions.minimapPins = v); y += 22;
        toggle(y, "Marcações no mapa-mundi", () -> MapOptions.worldPins, v -> MapOptions.worldPins = v); y += 22;
        toggle(y, "Feixe de luz no mundo", () -> MapOptions.worldBeams, v -> MapOptions.worldBeams = v); y += 22;
        toggle(y, "Nomes no mapa-mundi", () -> MapOptions.worldLabels, v -> MapOptions.worldLabels = v); y += 22;
        toggle(y, "Distância ao lado do nome", () -> MapOptions.showDistance, v -> MapOptions.showDistance = v); y += 22;
        toggle(y, "Mobs no minimapa", () -> MapOptions.showMobs, v -> MapOptions.showMobs = v); y += 22;
        toggle(y, "Grade de chunks", () -> MapOptions.chunkGrid, v -> MapOptions.chunkGrid = v); y += 22;
        toggle(y, "Marcar local da morte", () -> MapOptions.deathPoint, v -> MapOptions.deathPoint = v); y += 22;
        addRenderableWidget(Button.builder(Component.literal("Opacidade do minimapa: " + MapOptions.minimapOpacity + "%"), b -> {
            int[] steps = {40, 60, 80, 100};
            int cur = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] == MapOptions.minimapOpacity) cur = i;
            MapOptions.minimapOpacity = steps[(cur + 1) % steps.length];
            b.setMessage(Component.literal("Opacidade do minimapa: " + MapOptions.minimapOpacity + "%"));
            MapOptions.save();
        }).bounds(left + 10, y, W - 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Concluído"), b -> onClose())
                .bounds(left + W / 2 - 50, top + H - 24, 100, 18).build());
    }

    private void toggle(int y, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(text(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(text(label, get.getAsBoolean()));
            MapOptions.save();
        }).bounds(left + 10, y, W - 20, 20).build());
    }

    private static Component text(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "Ligado" : "Desligado"));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF000000);
        g.fill(left, top, left + W, top + H, 0xFF2A2A38);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawString(font, "Opções do mapa", left + 10, top + 8, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

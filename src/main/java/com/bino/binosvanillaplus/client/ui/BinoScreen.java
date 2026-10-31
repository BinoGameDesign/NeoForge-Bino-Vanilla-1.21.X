package com.bino.binosvanillaplus.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

// Base de todas as telas do mod no padrao do Figma.
//  - Abre com fade + leve "pop" (escala 94% -> 100% com overshoot) e sobe 6 px; fecha com fade rapido.
//  - Esc/fechar anima e volta para a tela anterior (parent).
//  - Escurece o jogo atras (sem o blur do vanilla, que borrava as telas).
//  - Mostra os avisos (Toasts) e ignora cliques durante o fechamento.
// Como usar: estenda, implemente build() (cria widgets), panelBounds() (retangulo do painel)
// e desenhe em renderPanel() (antes dos widgets) e renderOverlay() (por cima dos widgets).
public abstract class BinoScreen extends Screen {
    public static final int OPEN_MS = 220, CLOSE_MS = 130;

    @Nullable protected final Screen parent;
    private long openAt = -1, closeAt = -1;
    @Nullable private Screen closeTarget;
    private boolean firstOpen = true;

    protected BinoScreen(String title, @Nullable Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ para as subclasses

    protected abstract void build();

    // {x, y, w, h} do painel principal (centro da animacao de escala)
    protected abstract int[] panelBounds();

    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {}

    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {}

    // telas com conteudo 3D (mapa) desligam a escala: o viewport do OpenGL nao acompanha a matriz da GUI
    protected boolean animateScale() { return true; }

    // quanto escurecer o jogo atras (0..255)
    protected int dimAlpha() { return 0x60; }

    // 0..1: visibilidade atual (para animar coisas proprias junto com a tela)
    protected float visibility() {
        float o = Ease.outCubic(Ease.since(openAt, firstOpen ? OPEN_MS : OPEN_MS / 2));
        if (closeAt < 0) return o;
        return o * (1f - Ease.inCubic(Ease.since(closeAt, CLOSE_MS)));
    }

    // tempo (ms) desde que a tela abriu; util para entradas em cascata (linhas de lista)
    protected long openAge() { return Ease.now() - openAt; }

    // progresso 0..1 da entrada do item 'index' de uma lista (cada item entra 30 ms depois do anterior)
    protected float stagger(int index) {
        return Ease.outCubic((openAge() - 60 - index * 30L) / 180f);
    }

    protected boolean isClosing() { return closeAt >= 0; }

    private long shakeAt = -1000;

    // balanca o painel (erro: campo invalido, acao nao permitida)
    protected void shake() {
        shakeAt = Ease.now();
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.2f, 0.4f));
    }

    private float shakeOffset() {
        float t = Ease.since(shakeAt, 320);
        if (t >= 1f) return 0f;
        return (float) Math.sin(t * Math.PI * 6) * 5f * (1f - t);
    }

    // fecha com animacao e abre 'next' (null = volta ao jogo)
    public void closeTo(@Nullable Screen next) {
        if (closeAt >= 0) return;
        closeAt = Ease.now();
        closeTarget = next;
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected final void init() {
        // reabrir (voltando de uma tela filha ou redimensionando) usa uma animacao mais curta
        if (openAt >= 0) firstOpen = false;
        openAt = Ease.now();
        closeAt = -1;
        build();
    }

    @Override
    public void onClose() { closeTo(parent); }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        // vazio de proposito: o fundo e o painel sao desenhados em render() (o vanilla aplicaria blur aqui)
    }

    @Override
    public final void render(GuiGraphics g, int mx, int my, float pt) {
        if (closeAt >= 0 && Ease.since(closeAt, CLOSE_MS) >= 1f) {
            minecraft.setScreen(closeTarget);
            return;
        }
        float v = visibility();
        g.fill(0, 0, width, height, (int) (dimAlpha() * v) << 24);

        int[] b = panelBounds();
        float cx = b[0] + b[2] / 2f, cy = b[1] + b[3] / 2f;
        float s = 1f;
        if (animateScale()) {
            s = closeAt >= 0 ? 0.97f + 0.03f * v : 0.94f + 0.06f * Ease.outBack(Ease.since(openAt, firstOpen ? OPEN_MS : OPEN_MS / 2));
        }
        float slide = (1f - v) * 6f;

        g.pose().pushPose();
        g.pose().translate(cx + shakeOffset(), cy + slide, 0);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-cx, -cy, 0);
        g.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, v);
        renderPanel(g, mx, my, pt);
        super.render(g, mx, my, pt);          // widgets
        renderOverlay(g, mx, my, pt);
        g.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.pose().popPose();

        Toasts.render(g, width, height, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closeAt >= 0) return true;
        if (button == 0 && Toasts.click(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closeAt >= 0) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

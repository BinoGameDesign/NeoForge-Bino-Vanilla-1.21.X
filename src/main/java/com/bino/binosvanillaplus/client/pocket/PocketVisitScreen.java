package com.bino.binosvanillaplus.client.pocket;

import com.bino.binosvanillaplus.client.ui.BinoScreen;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.network.PocketVisitReplyPayload;
import com.bino.binosvanillaplus.network.PocketVisitRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.UUID;

// Menu do DONO quando alguem tenta abrir a porta dele: "Fulano quer visitar a sua dimensao" -> Aceitar / Recusar.
// Segue o padrao de UI do mod (BinoScreen). Varios pedidos entram numa fila. Fechar/Esc = recusar.
// O convite vale 30 s (o servidor tambem expira; passado isso, a resposta e ignorada).
public class PocketVisitScreen extends BinoScreen {
    private static final long LIFE_MS = 30_000;
    private static final ArrayDeque<PocketVisitRequestPayload> QUEUE = new ArrayDeque<>();

    private final UUID visitor;
    private final String name;
    private final long born = System.currentTimeMillis();
    private boolean answered = false;
    private int L, T, W, H;

    private PocketVisitScreen(PocketVisitRequestPayload p) {
        super("Pedido de visita", null);
        this.visitor = p.visitor();
        this.name = p.name();
    }

    // chega do servidor
    public static void receive(PocketVisitRequestPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PocketVisitScreen) QUEUE.add(p);
        else mc.setScreen(new PocketVisitScreen(p));
    }

    private void decide(boolean accept) {
        if (answered) return;
        answered = true;
        PacketDistributor.sendToServer(new PocketVisitReplyPayload(visitor, accept));
        closeTo(null);
    }

    @Override
    protected int[] panelBounds() { return new int[]{L, T, W, H}; }

    @Override
    protected void build() {
        W = Math.min(260, width - 24);
        H = 96;
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        addRenderableWidget(BinoUi.closeButton(L + W - 30, T - 8, 19, () -> decide(false)));
        int bw = (W - 16 * 2 - 6) / 2;
        addRenderableWidget(new BinoUi.Button(L + 16, T + H - 34, bw, 18, "Aceitar", BinoUi.Style.LIGHT, () -> decide(true)));
        addRenderableWidget(new BinoUi.Button(L + 16 + bw + 6, T + H - 34, bw, 18, "Recusar", BinoUi.Style.LIGHT, () -> decide(false)));
    }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.panel(g, L, T, W, H);
        BinoUi.title(g, font, "Pedido de visita", L + W / 2, T - 6);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        String l1 = name + " quer visitar";
        String l2 = "a sua dimensão.";
        g.drawCenteredString(font, l1, L + W / 2, T + 20, 0xFFFFFFFF);
        g.drawCenteredString(font, l2, L + W / 2, T + 32, 0xFFFFFFFF);
        // barra de tempo restante
        float left = 1f - Math.min(1f, (System.currentTimeMillis() - born) / (float) LIFE_MS);
        int bx = L + 16, bw = W - 32, by = T + 50;
        g.fill(bx, by, bx + bw, by + 3, 0x40FFFFFF);
        g.fill(bx, by, bx + (int) (bw * left), by + 3, 0xFFFEC400);
        if (left <= 0f && !answered) { answered = true; closeTo(null); }   // expirou: o servidor ja descartou
    }

    @Override
    public void onClose() { decide(false); }

    // ao fechar, mostra o proximo pedido da fila
    @Override
    public void removed() {
        super.removed();
        if (!answered) PacketDistributor.sendToServer(new PocketVisitReplyPayload(visitor, false));
        PocketVisitRequestPayload next = QUEUE.poll();
        if (next != null) Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new PocketVisitScreen(next)));
    }
}

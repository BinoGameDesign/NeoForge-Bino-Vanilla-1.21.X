package com.bino.binosvanillaplus.client.journal;

import com.bino.binosvanillaplus.client.ui.*;
import com.bino.binosvanillaplus.journal.Fossils;
import com.bino.binosvanillaplus.network.DiaryDataPayload;
import com.bino.binosvanillaplus.network.DiaryRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// Diario de Exploracao: duas descobertas por pagina (icone pixelado, nome, descricao curta, coordenada e dia);
// depois das descobertas, as fotos livres (revelar = 1 papel -> item Foto para molduras).
public class DiaryScreen extends BinoScreen {
    private static final int PAPER = 0xFFE7D9B5, PAPER_DARK = 0xFFC9B48A, INK = 0xFF4A3B28, INK_SOFT = 0xFF7A6548;

    private DiaryDataPayload data;
    private int L, T, W, H, spread;
    private final Tween[] hover = {new Tween(0f, 60f), new Tween(0f, 60f), new Tween(0f, 60f), new Tween(0f, 60f), new Tween(0f, 60f), new Tween(0f, 60f)};

    // cada "pagina" tem ate dois itens; descobertas primeiro, fotos depois (comecam numa pagina nova)
    private record Slot(DiaryDataPayload.Entry entry, DiaryDataPayload.Pic pic, int picIndex) {}
    private final List<Slot[]> pages = new ArrayList<>();
    private int firstPhotoPage = -1;

    public DiaryScreen(DiaryDataPayload data) {
        super("Diário de Exploração", null);
        setData(data);
    }

    public static void receive(DiaryDataPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DiaryScreen d) d.setData(p);
        else mc.setScreen(new DiaryScreen(p));
    }

    private void setData(DiaryDataPayload d) {
        data = d;
        pages.clear();
        List<DiaryDataPayload.Entry> es = d.entries();
        for (int i = 0; i < Math.max(1, es.size()); i += 2) {
            pages.add(new Slot[]{i < es.size() ? new Slot(es.get(i), null, -1) : null, i + 1 < es.size() ? new Slot(es.get(i + 1), null, -1) : null});
        }
        firstPhotoPage = -1;
        if (!d.photos().isEmpty()) {
            if ((pages.size() & 1) == 1) pages.add(new Slot[]{null, null});   // fotos comecam numa pagina da esquerda
            firstPhotoPage = pages.size();
            for (int i = 0; i < d.photos().size(); i += 2) {
                pages.add(new Slot[]{new Slot(null, d.photos().get(i), i),
                        i + 1 < d.photos().size() ? new Slot(null, d.photos().get(i + 1), i + 1) : null});
            }
        }
        spread = Math.min(spread, maxSpread());
    }

    private int maxSpread() { return Math.max(0, (pages.size() - 1) / 2); }

    @Override
    protected int[] panelBounds() { return new int[]{L, T, W, H}; }

    @Override
    protected void build() {
        W = Math.min(355, width - 24);
        H = Math.min(213, height - 30);
        L = (width - W) / 2;
        T = (height - H) / 2 + 4;
        addRenderableWidget(BinoUi.closeButton(L + W - 30, T - 8, 19, this::onClose));
    }

    private int pageW() { return (W - 28 - 10) / 2; }
    private int pageH() { return H - 16 - 34; }
    private int pageX(int side) { return L + 14 + side * (pageW() + 10); }
    private int pageY() { return T + 16; }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.panel(g, L, T, W, H);
        for (int side = 0; side < 2; side++) {
            int x = pageX(side), y = pageY();
            BinoUi.round(g, x, y, pageW(), pageH(), PAPER_DARK);
            BinoUi.round(g, x + 1, y + 1, pageW() - 2, pageH() - 2, PAPER);
        }
        g.fill(L + W / 2 - 1, pageY(), L + W / 2 + 1, pageY() + pageH(), 0x40000000);   // dobra do livro
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.title(g, font, "Diário de Exploração", L + W / 2, T - 6);
        for (int side = 0; side < 2; side++) {
            int pi = spread * 2 + side;
            if (pi >= pages.size()) continue;
            int x = pageX(side), y = pageY();
            boolean photos = firstPhotoPage >= 0 && pi >= firstPhotoPage;
            String head = photos ? "Fotos" : "Descobertas";
            g.drawString(font, head, x + 6, y + 5, INK_SOFT, false);
            g.drawString(font, String.valueOf(pi + 1), x + pageW() - 6 - font.width(String.valueOf(pi + 1)), y + 5, INK_SOFT, false);
            Slot[] ss = pages.get(pi);
            int sh = (pageH() - 18) / 2;
            for (int k = 0; k < 2; k++) {
                Slot s = ss[k];
                int sy = y + 16 + k * sh;
                if (k == 1) g.fill(x + 8, sy - 1, x + pageW() - 8, sy, 0x30000000);
                if (s == null) {
                    if (pi == 0 && k == 0 && data.entries().isEmpty())
                        drawWrapped(g, "Nenhuma descoberta ainda. Fotografe minérios e fósseis com a câmera.", x + 8, sy + 6, pageW() - 16, INK_SOFT, 1f, 4);
                    continue;
                }
                if (s.entry() != null) drawEntry(g, s.entry(), x, sy, sh);
                else drawPhoto(g, s, x, sy, sh, mx, my, side * 2 + k);
            }
        }
        // esqueletos (quantas partes de cada especie ja foram fotografadas)
        StringBuilder sb = new StringBuilder();
        for (int sp = 0; sp < Fossils.SPECIES.size(); sp++) {
            int have = 0, total = Fossils.parts(sp).size();
            for (String part : Fossils.parts(sp)) if (hasKey("fossil:" + sp + ":" + part)) have++;
            if (sb.length() > 0) sb.append("   ");
            sb.append(Component.translatable(Fossils.speciesKey(sp)).getString()).append(' ').append(have).append('/').append(total);
        }
        String bottom = sb.toString();
        BinoUi.text(g, font, bottom, L + W / 2f - font.width(bottom) * 0.75f / 2f, T + H - 14, 0.75f, 0xFF8A8A8A, false);
        // setas
        arrow(g, L + 14, T + H - 28, "<", spread > 0, mx, my, 4);
        arrow(g, L + W - 14 - 22, T + H - 28, ">", spread < maxSpread(), mx, my, 5);
    }

    private boolean hasKey(String key) {
        for (DiaryDataPayload.Entry e : data.entries()) if (e.key().equals(key)) return true;
        return false;
    }

    private void drawEntry(GuiGraphics g, DiaryDataPayload.Entry e, int x, int y, int sh) {
        // icone pixelado (o item do bloco, em 2x)
        ResourceLocation id = ResourceLocation.tryParse(e.icon());
        ItemStack icon = id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
        BinoUi.round(g, x + 6, y + 4, 36, 36, 0x22000000);
        g.pose().pushPose();
        g.pose().translate(x + 8, y + 6, 0);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();
        int tx = x + 48, tw = pageW() - 54;
        String name = Component.translatable(e.name()).getString();
        drawWrapped(g, name, tx, y + 5, tw, INK, 1f, 2);
        String desc = Component.translatable(e.desc()).getString();
        if (desc.equals(e.desc())) desc = Component.translatable("discovery.binosvanillaplus.generic.desc").getString();
        drawWrapped(g, desc, tx, y + 24, tw, INK_SOFT, 0.75f, 4);
        String where = e.x() + ", " + e.y() + ", " + e.z() + "  ·  Dia " + e.day();
        BinoUi.text(g, font, where, x + 8, y + sh - 12, 0.75f, INK_SOFT, false);
    }

    private void drawPhoto(GuiGraphics g, Slot s, int x, int y, int sh, int mx, int my, int hv) {
        int size = Math.min(sh - 10, 56);
        g.fill(x + 6, y + 3, x + 8 + size, y + 5 + size, 0xFFF6EEDB);
        g.blit(PhotoTextures.get(s.pic().png()), x + 7, y + 4, size, size, 0, 0, 64, 64, 64, 64);
        int tx = x + 14 + size;
        g.drawString(font, "Dia " + s.pic().day(), tx, y + 6, INK, false);
        int bw = pageW() - (tx - x) - 8, bx = tx, by = y + size - 12;
        boolean over = mx >= bx && mx < bx + bw && my >= by && my < by + 14;
        BinoUi.darkButton(g, font, bx, by, bw, 14, "Revelar (1 papel)", hover[hv].get(over ? 1f : 0f));
    }

    private void arrow(GuiGraphics g, int x, int y, String t, boolean on, int mx, int my, int hv) {
        if (!on) return;
        boolean over = mx >= x && mx < x + 22 && my >= y && my < y + 14;
        BinoUi.darkButton(g, font, x, y, 22, 14, t, hover[hv].get(over ? 1f : 0f));
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int w, int color, float scale, int maxLines) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), (int) (w / scale));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.pose().pushPose();
            g.pose().translate(x, y + i * 10 * scale, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, lines.get(i), 0, 0, color, false);
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        if (spread > 0 && hit(mx, my, L + 14, T + H - 28, 22, 14)) { turn(-1); return true; }
        if (spread < maxSpread() && hit(mx, my, L + W - 36, T + H - 28, 22, 14)) { turn(1); return true; }
        int sh = (pageH() - 18) / 2;
        for (int side = 0; side < 2; side++) {
            int pi = spread * 2 + side;
            if (pi >= pages.size()) continue;
            for (int k = 0; k < 2; k++) {
                Slot s = pages.get(pi)[k];
                if (s == null || s.pic() == null) continue;
                int x = pageX(side), y = pageY() + 16 + k * sh, size = Math.min(sh - 10, 56);
                int tx = x + 14 + size, bw = pageW() - (tx - x) - 8, by = y + size - 12;
                if (hit(mx, my, tx, by, bw, 14)) {
                    UiSound.click();
                    PacketDistributor.sendToServer(new DiaryRequestPayload(s.picIndex()));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private void turn(int d) {
        spread = Math.max(0, Math.min(maxSpread(), spread + d));
        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 0.8f, 1f);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT) { if (spread > 0) turn(-1); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { if (spread < maxSpread()) turn(1); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy > 0 && spread > 0) turn(-1);
        else if (sy < 0 && spread < maxSpread()) turn(1);
        return true;
    }
}

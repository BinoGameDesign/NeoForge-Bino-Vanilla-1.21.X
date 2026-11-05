package com.bino.binosvanillaplus.client.inventory;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.recipes.RecipeLookupScreen;
import com.bino.binosvanillaplus.client.ui.Tween;
import com.bino.binosvanillaplus.client.ui.UiSound;
import com.bino.binosvanillaplus.inventory.BinoInventoryMenu;
import com.bino.binosvanillaplus.network.InventoryActionPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Inventario novo (Figma "Inventario"): tres abas — Inventario, Fabricacao e Armadura (Atributos vem depois).
// Todas as medidas sao as do Figma (tela de 3840x2160) multiplicadas por 's', que encaixa o desenho na tela.
// Os slots nao usam a posicao do jogo: cada um tem o seu retangulo do Figma (clique, hover e desenho).
public class BinoInventoryScreen extends AbstractContainerScreen<BinoInventoryMenu> {
    private static final int TAB_INV = BinoInventoryMenu.TAB_INV, TAB_CRAFT = BinoInventoryMenu.TAB_CRAFT, TAB_ARMOR = BinoInventoryMenu.TAB_ARMOR, TAB_ATTR = 8;
    private static final int PANEL = 0xA118281F;                // rgba(24,40,31,0.63)
    private static final int SLOT = 0xD6212121;                 // rgba(33,33,33,0.84)
    private static final int BUTTON = 0xFFD9D9D9, BUTTON_EDGE = 0xFFE0FEFF, BUTTON_INK = 0xFF7B7B7B;

    private float s, ox, oy;   // escala e origem do "quadro" do Figma na tela
    private final Map<Integer, float[]> rects = new HashMap<>();
    private record Btn(float x, float y, float size, float radius, String icon, String text, int action, Component tip) {}
    private final List<Btn> buttons = new ArrayList<>();
    private final Map<Btn, Tween> hover = new HashMap<>();

    public BinoInventoryScreen(BinoInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/inventory/" + n + ".png");
    }

    private static Component t(String k) { return Component.translatable("gui.binosvanillaplus.inv." + k); }

    // ---------------------------------------------------------------- layout (coordenadas do Figma)

    @Override
    protected void init() {
        s = Math.min(width / 3840f, height / 2160f);
        ox = (width - 3840 * s) / 2f;
        oy = (height - 2160 * s) / 2f;
        super.init();
        layout();
    }

    private float X(float d) { return ox + d * s; }
    private float Y(float d) { return oy + d * s; }

    private float[] panel() {
        int tab = BinoInventoryMenu.clientTab;
        if (tab == TAB_CRAFT) return new float[]{295, 337, 3231, 1414};
        if (tab == TAB_ARMOR) return new float[]{1366, 343, 1369, 1414};
        return new float[]{1138, 326, 2357, 1414};
    }

    private void layout() {
        rects.clear();
        buttons.clear();
        // inventario (27) + hotbar: mesmo lugar nas abas Inventario e Fabricacao
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            rects.put(BinoInventoryMenu.MAIN + r * 9 + c, new float[]{1386 + c * 216, 459 + r * 284, 196});
        for (int c = 0; c < 9; c++) rects.put(BinoInventoryMenu.HOTBAR + c, new float[]{1386 + c * 216, 1460, 196});
        // fabricacao 2x2 + resultado
        rects.put(BinoInventoryMenu.CRAFT, new float[]{499, 497, 345});
        rects.put(BinoInventoryMenu.CRAFT + 1, new float[]{864, 497, 345});
        rects.put(BinoInventoryMenu.CRAFT + 2, new float[]{499, 859, 345});
        rects.put(BinoInventoryMenu.CRAFT + 3, new float[]{864, 859, 345});
        rects.put(BinoInventoryMenu.RESULT, new float[]{634, 1257, 420});
        // armadura: capacete, peitoral, calca, botas (coluna da esquerda)
        rects.put(BinoInventoryMenu.ARMOR, new float[]{1700, 447, 252});
        rects.put(BinoInventoryMenu.ARMOR + 1, new float[]{1700, 746, 252});
        rects.put(BinoInventoryMenu.ARMOR + 2, new float[]{1700, 1051, 252});
        rects.put(BinoInventoryMenu.ARMOR + 3, new float[]{1700, 1355, 252});
        // extras: brincos, colar, anel / longo alcance, costas / mao secundaria (grande)
        rects.put(BinoInventoryMenu.EXTRA, new float[]{2022, 449, 176});
        rects.put(BinoInventoryMenu.EXTRA + 1, new float[]{2236, 449, 176});
        rects.put(BinoInventoryMenu.EXTRA + 2, new float[]{2449, 449, 176});
        rects.put(BinoInventoryMenu.EXTRA + 3, new float[]{2022, 675, 272});
        rects.put(BinoInventoryMenu.EXTRA + 4, new float[]{2349, 683, 272});
        rects.put(BinoInventoryMenu.OFFHAND, new float[]{2022, 1004, 603});

        int tab = BinoInventoryMenu.clientTab;
        if (tab == TAB_INV) {
            buttons.add(new Btn(3445, 359, 100, 19, null, "Z", InventoryActionPayload.SORT_NAME, t("sort_name")));
            buttons.add(new Btn(3445, 476, 100, 19, null, "=", InventoryActionPayload.SORT_COUNT, t("sort_count")));
            buttons.add(new Btn(3445, 593, 100, 19, null, "ID", InventoryActionPayload.SORT_ID, t("sort_id")));
            tabs(3424, 1027, new int[]{TAB_CRAFT, TAB_ARMOR, TAB_ATTR});
        } else if (tab == TAB_CRAFT) {
            buttons.add(new Btn(3445, 422, 196, 38, "recipes", null, -100, t("recipes")));
            tabs(3445, 1018, new int[]{TAB_INV, TAB_ARMOR, TAB_ATTR});
        } else {
            tabs(2682, 1027, new int[]{TAB_CRAFT, TAB_INV, TAB_ATTR});
        }
        // painel (para soltar item fora e para as janelas que perguntam o tamanho)
        float[] p = panel();
        leftPos = (int) X(p[0]);
        topPos = (int) Y(p[1]);
        imageWidth = (int) (p[2] * s);
        imageHeight = (int) (p[3] * s);
    }

    private void tabs(float x, float y, int[] order) {
        for (int i = 0; i < order.length; i++) {
            int t = order[i];
            String icon = t == TAB_CRAFT ? "tab_crafting" : t == TAB_INV ? "tab_inventory" : t == TAB_ARMOR ? "tab_armor" : "tab_attributes";
            String key = t == TAB_CRAFT ? "tab.crafting" : t == TAB_INV ? "tab.inventory" : t == TAB_ARMOR ? "tab.armor" : "tab.attributes_soon";
            buttons.add(new Btn(x, y + i * 221, 196, 38, icon, null, -t, t(key)));
        }
    }

    private float[] screenRect(int slotIndex) {
        float[] r = rects.get(slotIndex);
        if (r == null) return null;
        return new float[]{X(r[0]), Y(r[1]), r[2] * s};
    }

    // ---------------------------------------------------------------- hover e clique nos slots

    // o jogo pergunta "o mouse esta neste slot?" pela posicao do slot; aqui a posicao diz qual slot e' e respondemos pelo Figma
    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
        if (y != BinoInventoryMenu.FAKE_Y) return super.isHovering(x, y, w, h, mx, my);
        float[] r = screenRect(BinoInventoryMenu.indexFromFake(x));
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[2];
    }

    // retangulo de um slot na tela (usado pelo "new" dos itens recolhidos)
    public float[] slotRect(Slot slot) { return slot.isActive() ? screenRect(slot.index) : null; }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        float[] p = panel();
        boolean inPanel = mx >= X(p[0]) && mx < X(p[0] + p[2]) && my >= Y(p[1]) && my < Y(p[1] + p[3]);
        return !inPanel && buttonAt(mx, my) == null;
    }

    private Btn buttonAt(double mx, double my) {
        for (Btn b : buttons) {
            if (mx >= X(b.x()) && mx < X(b.x() + b.size()) && my >= Y(b.y()) && my < Y(b.y() + b.size())) return b;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Btn b = button == 0 ? buttonAt(mx, my) : null;
        if (b != null && menu.getCarried().isEmpty()) {
            if (b.action() == -100) {
                UiSound.click();
                minecraft.setScreen(new RecipeLookupScreen(this, RecipeLookupScreen.Mode.BROWSE, null));
            } else if (b.action() < 0) {
                int t = -b.action();
                if (t != TAB_ATTR) {
                    UiSound.click();
                    BinoInventoryMenu.clientTab = t;
                    layout();
                }
            } else {
                UiSound.click();
                PacketDistributor.sendToServer(new InventoryActionPayload(b.action()));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x30000000);   // o jogo continua aparecendo atras (como no Figma)
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) { }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int tab = BinoInventoryMenu.clientTab;
        float[] p = panel();
        fill(g, X(p[0]), Y(p[1]), p[2] * s, p[3] * s, PANEL);

        // personagem (atras do painel na aba Inventario, como no Figma)
        if (tab == TAB_INV) character(g, 0, 221, 1454, 1624, -28f, ActionPose.Pose.GUARD);
        if (tab == TAB_ARMOR) character(g, 275, 238, 1454, 1624, -24f, ActionPose.Pose.HERO);

        // titulo
        String title = t(tab == TAB_CRAFT ? "tab.crafting" : tab == TAB_ARMOR ? "tab.armor" : "tab.inventory").getString();
        float ts = 128 * s / 8.5f;
        float cx = tab == TAB_CRAFT ? 2121 : tab == TAB_ARMOR ? 2036 : 2249;
        float ty = tab == TAB_CRAFT ? 274 : tab == TAB_ARMOR ? 265 : 247;
        g.pose().pushPose();
        g.pose().translate(X(cx) - font.width(title) * ts / 2f, Y(ty) + 16 * s, 0);
        g.pose().scale(ts, ts, 1f);
        g.drawString(font, title, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();

        // fundos dos slots + silhuetas dos vazios
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            float[] r = screenRect(slot.index);
            if (r == null) continue;
            round(g, r[0], r[1], r[2], r[2], 9 * s * r[2] / (196 * s), SLOT);
            String sil = silhouette(slot.index);
            if (sil != null && !slot.hasItem()) icon(g, sil, r[0] + r[2] * 0.2f, r[1] + r[2] * 0.2f, r[2] * 0.6f, 0.22f);
            if (slot == hoveredSlot) round(g, r[0], r[1], r[2], r[2], 9 * s * r[2] / (196 * s), 0x30FFFFFF);
        }
        // linha da hotbar e item selecionado
        if (tab != TAB_ARMOR) {
            g.fill((int) X(1386), (int) Y(1366), (int) X(1386 + 1951), (int) Y(1366) + 1, 0x80FFFFFF);
            float[] r = screenRect(BinoInventoryMenu.HOTBAR + minecraft.player.getInventory().selected);
            if (r != null) icon(g, "select", r[0] - r[2] * 0.035f, r[1] - r[2] * 0.035f, r[2] * 1.07f, 1f);
        }
        if (tab == TAB_CRAFT) {   // moldura do resultado
            float[] r = screenRect(BinoInventoryMenu.RESULT);
            icon(g, "select", r[0] - r[2] * 0.03f, r[1] - r[2] * 0.03f, r[2] * 1.06f, 1f);
        }
        // botoes
        for (Btn b : buttons) {
            Tween tw = hover.computeIfAbsent(b, k -> new Tween(0f, 60f));
            boolean over = buttonAt(mx, my) == b;
            boolean disabled = b.action() == -TAB_ATTR;
            float h = tw.get(over && !disabled ? 1f : 0f);
            float bx = X(b.x()), by = Y(b.y()), bs = b.size() * s;
            float grow = h * 2f;
            round(g, bx - grow - 0.5f, by - grow - 0.5f, bs + grow * 2 + 1, bs + grow * 2 + 1, b.radius() * s + 0.5f, BUTTON_EDGE);
            round(g, bx - grow, by - grow, bs + grow * 2, bs + grow * 2, b.radius() * s, disabled ? 0xFFB5B5B5 : BUTTON);
            if (b.icon() != null) icon(g, b.icon(), bx + bs * 0.2f, by + bs * 0.2f, bs * 0.6f, disabled ? 0.35f : 0.67f);
            else if ("=".equals(b.text())) {   // icone de lista (3 barras)
                float lw = 57 * s, lh = Math.max(1, 11 * s);
                for (int i = 0; i < 3; i++) fill(g, bx + bs * 0.24f, by + bs * 0.28f + i * 17 * s, lw * (1 - i * 0.2f), lh, BUTTON_INK);
            } else {
                float k = 64 * s / 7f;
                g.pose().pushPose();
                g.pose().translate(bx + bs / 2f - font.width(b.text()) * k / 2f, by + bs / 2f - 3.5f * k, 0);
                g.pose().scale(k, k, 1f);
                g.drawString(font, b.text(), 0, 0, BUTTON_INK, false);
                g.pose().popPose();
            }
        }
    }

    private static String silhouette(int i) {
        return switch (i) {
            case BinoInventoryMenu.ARMOR -> "slot_helmet";
            case BinoInventoryMenu.ARMOR + 1 -> "slot_chest";
            case BinoInventoryMenu.ARMOR + 2 -> "slot_legs";
            case BinoInventoryMenu.ARMOR + 3 -> "slot_boots";
            case BinoInventoryMenu.EXTRA -> "slot_earring";
            case BinoInventoryMenu.EXTRA + 1 -> "slot_necklace";
            case BinoInventoryMenu.EXTRA + 2 -> "slot_ring";
            case BinoInventoryMenu.EXTRA + 3 -> "slot_ranged";
            case BinoInventoryMenu.EXTRA + 4 -> "slot_back";
            case BinoInventoryMenu.OFFHAND -> "slot_offhand";
            default -> null;
        };
    }

    // item dentro do slot, no tamanho do slot do Figma
    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        float[] r = screenRect(slot.index);
        if (r == null) return;
        float k = Math.min(3f, r[2] * 0.62f / 16f);
        g.pose().pushPose();
        g.pose().translate(r[0] - leftPos + (r[2] - 16 * k) / 2f, r[1] - topPos + (r[2] - 16 * k) / 2f, 0);
        g.pose().scale(k, k, 1f);
        g.pose().translate(-slot.x, -slot.y, 0);
        super.renderSlot(g, slot);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        Btn b = buttonAt(mx, my);
        if (b != null && menu.getCarried().isEmpty()) g.renderTooltip(font, b.tip(), mx, my);
    }

    // o proprio jogador (skin, armadura, item na mao) numa pose de acao
    private void character(GuiGraphics g, float x, float y, float w, float h, float yaw, ActionPose.Pose pose) {
        Player p = minecraft.player;
        if (p == null) return;
        float cx = X(x + w / 2f), cy = Y(y + h / 2f), scale = h * s * 0.92f / Math.max(1.8f, p.getBbHeight()) / p.getScale();
        float bodyRot = p.yBodyRot, yRot = p.getYRot(), xRot = p.getXRot(), headO = p.yHeadRotO, head = p.yHeadRot;
        p.yBodyRot = 180f + yaw;
        p.setYRot(180f + yaw);
        p.setXRot(0f);
        p.yHeadRot = p.getYRot();
        p.yHeadRotO = p.getYRot();
        Quaternionf q = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cam = new Quaternionf().rotateX((float) Math.toRadians(4));
        q.mul(cam);
        ActionPose.current = pose;
        try {
            InventoryScreen.renderEntityInInventory(g, cx, cy, scale, new Vector3f(0f, p.getBbHeight() / 2f, 0f), q, cam, p);
        } finally {
            ActionPose.current = null;
            p.yBodyRot = bodyRot;
            p.setYRot(yRot);
            p.setXRot(xRot);
            p.yHeadRotO = headO;
            p.yHeadRot = head;
        }
    }

    // ---------------------------------------------------------------- formas

    private static void fill(GuiGraphics g, float x, float y, float w, float h, int c) {
        g.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), c);
    }

    // retangulo de cantos arredondados (faixas horizontais)
    private static void round(GuiGraphics g, float x, float y, float w, float h, float r, int c) {
        int x0 = Math.round(x), y0 = Math.round(y), x1 = Math.round(x + w), y1 = Math.round(y + h);
        int rr = Math.max(0, Math.min(Math.round(r), Math.min(x1 - x0, y1 - y0) / 2));
        if (rr <= 0) { g.fill(x0, y0, x1, y1, c); return; }
        g.fill(x0, y0 + rr, x1, y1 - rr, c);
        for (int i = 0; i < rr; i++) {
            double dy = rr - i - 0.5;
            int inset = (int) Math.round(rr - Math.sqrt(rr * rr - dy * dy));
            g.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, c);
            g.fill(x0 + inset, y1 - i - 1, x1 - inset, y1 - i, c);
        }
    }

    private static void icon(GuiGraphics g, String name, float x, float y, float size, float alpha) {
        g.flush();
        float[] prev = RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha * prev[3]);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 16f, size / 16f, 1f);
        g.blit(tex(name), 0, 0, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();
        RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
    }
}

package com.bino.binosvanillaplus.client.inventory;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.recipes.RecipeLookupScreen;
import com.bino.binosvanillaplus.client.ui.Tween;
import com.bino.binosvanillaplus.client.ui.Ease;
import com.bino.binosvanillaplus.client.ui.Glass;
import com.bino.binosvanillaplus.client.ui.UiSound;
import com.bino.binosvanillaplus.inventory.BinoInventoryMenu;
import com.bino.binosvanillaplus.network.InventoryActionPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Inventario novo (Figma "Inventario"): tres abas — Inventario, Fabricacao e Armadura (Atributos vem depois).
// Todas as medidas sao as do Figma (tela de 3840x2160) multiplicadas por 's', que encaixa o desenho na tela.
// Os slots nao usam a posicao do jogo: cada um tem o seu retangulo do Figma (clique, hover e desenho).
public class BinoInventoryScreen extends AbstractContainerScreen<BinoInventoryMenu> {
    private static final int TAB_INV = BinoInventoryMenu.TAB_INV, TAB_CRAFT = BinoInventoryMenu.TAB_CRAFT, TAB_ARMOR = BinoInventoryMenu.TAB_ARMOR, TAB_ATTR = 8;
    private static final int PANEL = 0xA8222426;                // cinza escuro neutro (o verde do Figma ficava esverdeado demais a noite)
    private static final int SLOT = 0xD6212121;                 // rgba(33,33,33,0.84)
    private static final int BUTTON = 0xFFD9D9D9, BUTTON_EDGE = 0xFFE0FEFF, BUTTON_INK = 0xFF7B7B7B;

    private float s, ox, oy;   // escala e origem do "quadro" do Figma na tela
    private final Map<Integer, float[]> rects = new HashMap<>();
    private record Btn(float x, float y, float size, float radius, String icon, String text, int action, Component tip) {}
    private final List<Btn> buttons = new ArrayList<>();
    private final Map<Integer, Tween> hover = new HashMap<>();

    public BinoInventoryScreen(BinoInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/gui/inventory/" + n + ".png");
    }

    private static net.minecraft.network.chat.MutableComponent t(String k) { return Component.translatable("gui.binosvanillaplus.inv." + k); }

    // ---------------------------------------------------------------- layout (coordenadas do Figma)

    private static boolean hidHud, prevHideGui;

    // HUD escondido enquanto o inventario esta aberto (como o F1)
    @Override
    public void removed() {
        super.removed();
        if (hidHud) { minecraft.options.hideGui = prevHideGui; hidHud = false; }
    }

    @Override
    protected void init() {
        if (!hidHud) { prevHideGui = minecraft.options.hideGui; minecraft.options.hideGui = true; hidHud = true; }
        s = Math.min(width / 3840f, height / 2160f);
        ox = (width - 3840 * s) / 2f;
        oy = (height - 2160 * s) / 2f;
        super.init();
        layout();
        syncAnim(true);
    }

    private float X(float d) { return ox + d * s; }
    private float Y(float d) { return oy + d * s; }

    private float[] panel() {
        int tab = BinoInventoryMenu.clientTab;
        if (tab == TAB_CRAFT) return new float[]{295, 337, 3231, 1414};
        // lado esquerdo esticado: o personagem fica por cima, cobrindo o canto
        if (tab == TAB_ARMOR) return new float[]{960, 343, 1775, 1414};
        return new float[]{700, 326, 2795, 1414};
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
            tabs(3424, 806);
        } else if (tab == TAB_CRAFT) {
            buttons.add(new Btn(3445, 422, 196, 38, "recipes", null, -100, t("recipes")));
            tabs(3445, 806);
        } else {
            tabs(2682, 806);
        }
        // painel (para soltar item fora e para as janelas que perguntam o tamanho)
        float[] p = panel();
        leftPos = (int) X(p[0]);
        topPos = (int) Y(p[1]);
        imageWidth = (int) (p[2] * s);
        imageHeight = (int) (p[3] * s);
    }

    // as 4 abas, sempre na mesma ordem; a aba aberta aparece selecionada
    private static final int[] TAB_ORDER = {TAB_INV, TAB_CRAFT, TAB_ARMOR, TAB_ATTR};

    private void tabs(float x, float y) {
        int[] order = TAB_ORDER;
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
        return !inPanel && !inPopup(mx, my) && buttonAt(mx, my) == null;
    }

    private Btn buttonAt(double mx, double my) {
        for (Btn b : buttons) {
            if (mx >= X(b.x()) && mx < X(b.x() + b.size()) && my >= Y(b.y()) && my < Y(b.y() + b.size())) return b;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && clickPopup(mx, my)) return true;
        if (button == 0 && clickEquipSlot(mx, my)) return true;
        Btn b = button == 0 ? buttonAt(mx, my) : null;
        if (b != null && menu.getCarried().isEmpty()) {
            if (b.action() != -TAB_ATTR) pressAt.put(b.action(), Ease.now());
            if (b.action() == -100) {
                UiSound.click();
                minecraft.setScreen(new RecipeLookupScreen(this, RecipeLookupScreen.Mode.BROWSE, null));
            } else if (b.action() < 0) {
                int t = -b.action();
                if (t != TAB_ATTR && t != BinoInventoryMenu.clientTab) {
                    pinned = -1;
                    UiSound.click();
                    BinoInventoryMenu.clientTab = t;
                    layout();
                    syncAnim(false);
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
        Glass.capture();                             // copia e desfoca o mundo atras do menu (o vidro usa)
        g.fill(0, 0, width, height, Glass.available() ? 0x48000000 : 0x90000000);   // mundo levemente escurecido
        renderBg(g, pt, mx, my);                    // no 1.21 o fundo da janela e' desenhado aqui (painel, slots, botoes, personagem)
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) { }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int tab = BinoInventoryMenu.clientTab;
        float[] p = panel();
        // o painel "escorre" para o tamanho da aba nova (em vez de pular)
        float pxx = pX.get(X(p[0])), pyy = pY.get(Y(p[1])), pww = pW.get(p[2] * s), phh = pH.get(p[3] * s);
        float pr = 70 * s;
        Glass.shadow(g, pxx, pyy, pww, phh, pr, 5f, 9f, 0.45f);
        Glass.panel(g, pxx, pyy, pww, phh, pr, Glass.PANEL, PANEL);

        // personagem (atras do painel na aba Inventario, como no Figma)
        // o personagem fica por cima do canto esquerdo do painel, como se fizesse parte do menu
        if (tab == TAB_INV) character(820, 1000, 1500, 32f, ActionPose.Pose.GUARD, g);
        if (tab == TAB_ARMOR) character(1060, 1000, 1500, 30f, ActionPose.Pose.HERO, g);

        // titulo
        String title = t(tab == TAB_CRAFT ? "tab.crafting" : tab == TAB_ARMOR ? "tab.armor" : "tab.inventory").getString();
        float ts = 128 * s / 8.5f;
        float cx = tab == TAB_CRAFT ? 2121 : tab == TAB_ARMOR ? 2036 : 2249;
        float ty = tab == TAB_CRAFT ? 274 : tab == TAB_ARMOR ? 265 : 247;
        g.pose().pushPose();
        g.pose().translate(X(cx) - font.width(title) * ts / 2f, Y(ty) + 16 * s, 0);
        g.pose().scale(ts, ts, 1f);
        g.drawString(font, title, 0, 0, 0xFFFFFFFF, true);   // com a sombra cinza do jogo por baixo
        g.pose().popPose();

        // fundos dos slots + silhuetas dos vazios
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            float[] r = screenRect(slot.index);
            if (r == null) continue;
            float hv = slotHover.computeIfAbsent(slot.index, k -> new Tween(0f, 45f)).get(slot == hoveredSlot ? 1f : 0f);
            float grow = hv * Math.max(0.6f, r[2] * 0.03f);   // o slot "incha" um pouco no hover
            Glass.panel(g, r[0] - grow, r[1] - grow, r[2] + grow * 2, r[2] + grow * 2, r[2] * 0.17f,
                    Glass.SLOT.withMilk(0.07f * hv).withRim(0.55f + 0.6f * hv), SLOT);
            if (!Glass.available() && slot == hoveredSlot) round(g, r[0], r[1], r[2], r[2], 9 * s * r[2] / (196 * s), 0x30FFFFFF);
            String sil = silhouette(slot.index);
            if (sil != null && !slot.hasItem()) icon(g, sil, r[0] + r[2] * 0.2f, r[1] + r[2] * 0.2f, r[2] * 0.6f, 0.22f);
        }
        // linha da hotbar e item selecionado
        if (tab != TAB_ARMOR) {
            g.fill((int) X(1386), (int) Y(1366), (int) X(1386 + 1951), (int) Y(1366) + 1, 0x55FFFFFF);
            float[] r = screenRect(BinoInventoryMenu.HOTBAR + minecraft.player.getInventory().selected);
            if (r != null) icon(g, "select", r[0] - r[2] * 0.035f, r[1] - r[2] * 0.035f, r[2] * 1.07f, 1f);
        }
        if (tab == TAB_CRAFT) {   // moldura do resultado
            float[] r = screenRect(BinoInventoryMenu.RESULT);
            icon(g, "select", r[0] - r[2] * 0.03f, r[1] - r[2] * 0.03f, r[2] * 1.06f, 1f);
        }
        // botoes: vidro claro (como os botoes claros do Figma). A aba aberta tem uma gota de vidro mais clara que
        // desliza ate a aba nova quando troca. Hover: cresce e brilha. Clique: aperta e volta (mola).
        Btn selBtn = null;
        for (Btn b : buttons) if (b.action() == -tab) selBtn = b;
        float tabDx = tabX.get(0f);
        if (selBtn != null) selY.get(Y(selBtn.y()));
        for (int pass = 0; pass < 2; pass++) {
            for (Btn b : buttons) {
                boolean isTab = b.action() < 0 && b.action() != -100;
                Tween tw = hover.computeIfAbsent(b.action(), k -> new Tween(0f, 55f));
                boolean over = buttonAt(mx, my) == b;
                boolean disabled = b.action() == -TAB_ATTR;
                boolean selected = b.action() == -tab;
                float h = pass == 0 ? tw.get(over && !disabled ? 1f : 0f) : tw.get();
                float press = pressAt.containsKey(b.action()) ? Ease.since(pressAt.get(b.action()), 180) : 1f;
                float squish = 1f - 0.10f * (float) Math.sin(Math.PI * press);
                float bs0 = b.size() * s, grow = h * 1.6f;
                float bs = (bs0 + grow * 2) * squish;
                float bx = X(b.x()) + (isTab ? tabDx : 0) + bs0 / 2f - bs / 2f, by = Y(b.y()) + bs0 / 2f - bs / 2f;
                float br = b.radius() * s * bs / bs0;
                if (pass == 0) {
                    Glass.shadow(g, bx, by, bs, bs, br, 2f, 3.5f, 0.30f);
                    Glass.Style st = disabled ? Glass.BUTTON.withTint(0.55f).withMilk(-0.06f) : Glass.BUTTON.withMilk(0.02f + 0.06f * h).withRim(0.9f + 0.6f * h);
                    if (Glass.available()) Glass.panel(g, bx, by, bs, bs, br, st, BUTTON);
                    else {
                        round(g, bx - 0.5f, by - 0.5f, bs + 1, bs + 1, br + 0.5f, BUTTON_EDGE);
                        round(g, bx, by, bs, bs, br, selected ? 0xFFF4F6F6 : disabled ? 0xFFB5B5B5 : BUTTON);
                    }
                    continue;
                }
                // segunda passada: a gota da aba aberta (deslizando) por baixo do icone dela
                if (selected && selBtn != null && Glass.available()) {
                    float gy = selY.get(), gs = bs0 + 3f;
                    Glass.panel(g, bx - (gs - bs) / 2f, gy + bs0 / 2f - gs / 2f, gs, gs, b.radius() * s * gs / bs0,
                            Glass.BUTTON.withTint(0.80f).withMilk(0.10f).withRim(1.7f), 0xFFF4F6F6);
                }
                if (b.icon() != null) icon(g, b.icon(), bx + bs * 0.2f, by + bs * 0.2f, bs * 0.6f, disabled ? 0.35f : selected ? 0.9f : 0.67f);
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
    }

    // animacoes do menu
    private final Tween pX = new Tween(0, 55f), pY = new Tween(0, 55f), pW = new Tween(0, 55f), pH = new Tween(0, 55f);
    private final Tween tabX = new Tween(0, 60f), selY = new Tween(0, 50f);
    private final Map<Integer, Tween> slotHover = new HashMap<>();
    private final Map<Integer, Long> pressAt = new HashMap<>();
    private boolean animInit;
    private float lastTabColumn = Float.NaN;

    // posicao inicial das animacoes (sem animar ao abrir) e coluna de abas: ao trocar de aba, as abas deslizam da coluna antiga
    private void syncAnim(boolean snap) {
        float[] p = panel();
        if (snap) { pX.snap(X(p[0])); pY.snap(Y(p[1])); pW.snap(p[2] * s); pH.snap(p[3] * s); }
        float col = Float.NaN;
        for (Btn b : buttons) if (b.action() < 0 && b.action() != -100) { col = X(b.x()); break; }
        if (!snap && !Float.isNaN(lastTabColumn) && !Float.isNaN(col)) tabX.snap(lastTabColumn - col).to(0f);
        else tabX.snap(0f);
        for (Btn b : buttons) if (b.action() == -BinoInventoryMenu.clientTab && snap) selY.snap(Y(b.y()));
        lastTabColumn = col;
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
        int over = drawPopup(g, mx, my);
        if (over >= 0) compareTooltip(g, menu.slots.get(over).getItem(), menu.slots.get(popupSlot).getItem(), popupSlot);
        else if (popupRect != null && hoveredSlot != null && hoveredSlot.index == popupSlot) {
            if (hoveredSlot.hasItem()) {   // o item equipado: tooltip do outro lado, sem cobrir a lista
                float[] r = screenRect(popupSlot);
                List<Component> l = getTooltipFromContainerItem(hoveredSlot.getItem());
                tipAt(g, l, r[0] - 6 - tipWidth(l), r[1]);
            }
        } else renderTooltip(g, mx, my);
        Btn b = buttonAt(mx, my);
        if (b != null && menu.getCarried().isEmpty()) g.renderTooltip(font, b.tip(), mx, my);
    }

    // ---------------------------------------------------------------- aba Armadura: itens do inventario que servem no slot

    private int popupSlot = -1;
    private float[] popupRect;
    private final List<int[]> popupCells = new ArrayList<>();   // {indice do slot do inventario, x, y}

    private static boolean equipSlot(int i) {
        return (i >= BinoInventoryMenu.ARMOR && i < BinoInventoryMenu.ARMOR + 4) || i == BinoInventoryMenu.OFFHAND
                || (i >= BinoInventoryMenu.EXTRA && i < BinoInventoryMenu.END);
    }

    private List<Integer> candidates(int target) {
        Slot t = menu.slots.get(target);
        List<Integer> out = new ArrayList<>();
        for (int i = BinoInventoryMenu.MAIN; i < BinoInventoryMenu.OFFHAND; i++) {
            net.minecraft.world.item.ItemStack st = menu.slots.get(i).getItem();
            if (st.isEmpty()) continue;
            if (target == BinoInventoryMenu.OFFHAND) {   // mao secundaria: so o que faz sentido nela
                boolean ok = st.getItem() instanceof net.minecraft.world.item.ShieldItem || st.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)
                        || st.getItem() instanceof net.minecraft.world.item.FireworkRocketItem || st.is(net.minecraft.tags.ItemTags.ARROWS)
                        || st.getItem() instanceof net.minecraft.world.item.MapItem || st.is(net.minecraft.world.item.Items.TORCH);
                if (!ok) continue;
            } else if (!t.mayPlace(st)) continue;
            out.add(i);
        }
        return out;
    }

    // desenha a lista ao lado do slot em que o mouse esta; devolve o slot do inventario sob o mouse (ou -1)
    private int drawPopup(GuiGraphics g, int mx, int my) {
        popupCells.clear();
        if (BinoInventoryMenu.clientTab != TAB_ARMOR || !menu.getCarried().isEmpty()) { popupSlot = -1; pinned = -1; popupRect = null; return -1; }
        boolean inPopup = inPopup(mx, my);
        if (pinned >= 0) popupSlot = pinned;                                   // clicou no slot: a lista fica aberta
        else if (hoveredSlot != null && equipSlot(hoveredSlot.index)) popupSlot = hoveredSlot.index;
        else if (!inPopup) popupSlot = -1;
        if (popupSlot < 0) { popupRect = null; popupShown = -1; return -1; }
        List<Integer> list = candidates(popupSlot);
        if (list.isEmpty()) { popupRect = null; popupShown = -1; return -1; }
        float[] r = screenRect(popupSlot);
        int cell = 20, pad = 4, cols = Math.min(6, list.size()), rows = (list.size() + cols - 1) / cols;
        int w = pad * 2 + cols * cell, h = pad * 2 + rows * cell;
        float px = r[0] + r[2] + 4, py = r[1];
        if (px + w > width - 4) px = r[0] - w - 4;
        if (py + h > height - 4) py = height - 4 - h;
        popupRect = new float[]{px - 4, py, w + 8, h};   // folga para o mouse passar do slot para a lista
        if (popupSlot != popupShown) { popupShown = popupSlot; popupAt = Ease.now(); }
        float bloom = outBack(Ease.since(popupAt, 190));        // a lista "brota" do lado do slot, com um leve repique
        float ax = px < r[0] ? px + w : px, ay = py;      // ponto de onde ela cresce (encostado no slot)
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (pinned >= 0) round(g, r[0] - 1, r[1] - 1, r[2] + 2, r[2] + 2, 3, 0x60FFFFFF);   // slot com a lista aberta
        float gx = ax + (px - ax) * bloom, gw = w * bloom, gh = h * bloom;
        Glass.shadow(g, gx, ay, gw, gh, 5, 2f, 4f, 0.4f * Math.min(1f, bloom));
        Glass.panel(g, gx, ay, gw, gh, 5, Glass.POPUP, 0xF01E211F);
        g.pose().translate(ax, ay, 0);
        g.pose().scale(bloom, bloom, 1f);
        g.pose().translate(-ax, -ay, 0);
        int over = -1;
        for (int k = 0; k < list.size(); k++) {
            int cx = (int) px + pad + (k % cols) * cell, cy = (int) py + pad + (k / cols) * cell;
            boolean hov = mx >= cx && mx < cx + cell && my >= cy && my < cy + cell;
            if (Glass.available()) Glass.panel(g, ax + (cx + 1 - ax) * bloom, ay + (cy + 1 - ay) * bloom, (cell - 2) * bloom, (cell - 2) * bloom, 3 * bloom,
                    Glass.SLOT.withMilk(hov ? 0.10f : 0f).withRim(hov ? 1.2f : 0.55f), 0xFF2A2D2B);
            else round(g, cx + 1, cy + 1, cell - 2, cell - 2, 2, hov ? 0xFF5B5E5C : 0xFF2A2D2B);
            net.minecraft.world.item.ItemStack st = menu.slots.get(list.get(k)).getItem();
            g.renderItem(st, cx + 2, cy + 2);
            g.renderItemDecorations(font, st, cx + 2, cy + 2);
            popupCells.add(new int[]{list.get(k), cx, cy});
            if (hov) over = list.get(k);
        }
        g.pose().popPose();
        return over;
    }

    private int pinned = -1;
    private int popupShown = -1;
    private long popupAt;

    private static float outBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    private boolean inPopup(double mx, double my) {
        return popupRect != null && mx >= popupRect[0] && mx < popupRect[0] + popupRect[2] && my >= popupRect[1] && my < popupRect[1] + popupRect[3];
    }

    // 1o clique num slot da aba Armadura abre (e prende) a lista; clicar de novo no mesmo slot pega o item, como sempre
    private boolean clickEquipSlot(double mx, double my) {
        if (BinoInventoryMenu.clientTab != TAB_ARMOR || !menu.getCarried().isEmpty() || hasShiftDown()) return false;
        if (hoveredSlot != null && equipSlot(hoveredSlot.index)) {
            int i = hoveredSlot.index;
            if (pinned != i && !candidates(i).isEmpty()) { pinned = i; UiSound.click(); return true; }
            pinned = -1;
            return false;
        }
        if (!inPopup(mx, my)) pinned = -1;
        return false;
    }

    // ---- tooltip de comparacao: o item da lista + o equipado, lado a lado, ao lado da lista (sem cobrir)

    private int tipWidth(List<Component> l) {
        int w = 0;
        for (Component c : l) w = Math.max(w, Math.min(220, font.width(c)));
        return w + 8;
    }

    private void tipAt(GuiGraphics g, List<Component> lines, float x, float y) {
        int tx = (int) x, ty = (int) y;
        List<net.minecraft.util.FormattedCharSequence> seq = new ArrayList<>();
        for (Component c : lines) {   // linhas longas quebram (a dica da lanterna era uma linha so enorme)
            if (font.width(c) <= 220) seq.add(c.getVisualOrderText());
            else seq.addAll(font.split(c, 220));
        }
        g.renderTooltip(font, seq,
                (sw, sh, mx, my, tw, th) -> new org.joml.Vector2i(Math.max(4, Math.min(sw - tw - 4, tx + 4)), Math.max(4, Math.min(sh - th - 4, ty + 4))),
                tx, ty);
    }

    private static net.minecraft.world.entity.EquipmentSlot equipmentOf(int slot) {
        return switch (slot) {
            case BinoInventoryMenu.ARMOR -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case BinoInventoryMenu.ARMOR + 1 -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case BinoInventoryMenu.ARMOR + 2 -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case BinoInventoryMenu.ARMOR + 3 -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case BinoInventoryMenu.OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            default -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        };
    }

    private static Map<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, Double> stats(
            net.minecraft.world.item.ItemStack st, net.minecraft.world.entity.EquipmentSlot es) {
        Map<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, Double> m = new java.util.LinkedHashMap<>();
        if (st.isEmpty()) return m;
        st.forEachModifier(es, (attr, mod) -> {
            if (mod.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
                m.merge(attr, mod.amount(), Double::sum);
        });
        return m;
    }

    private static String num(double d) {
        return Math.abs(d - Math.round(d)) < 0.01 ? String.valueOf(Math.round(d)) : String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    private void compareTooltip(GuiGraphics g, net.minecraft.world.item.ItemStack cand, net.minecraft.world.item.ItemStack cur, int slot) {
        List<Component> a = new ArrayList<>(getTooltipFromContainerItem(cand));
        if (!cur.isEmpty()) {
            var es = equipmentOf(slot);
            var sa = stats(cand, es);
            var sb = stats(cur, es);
            java.util.Set<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> keys = new java.util.LinkedHashSet<>(sa.keySet());
            keys.addAll(sb.keySet());
            List<Component> diff = new ArrayList<>();
            for (var k : keys) {
                double va = sa.getOrDefault(k, 0.0), vb = sb.getOrDefault(k, 0.0), d = va - vb;
                if (Math.abs(d) < 0.001) continue;
                diff.add(Component.literal((d > 0 ? "+" : "") + num(d) + " ").append(Component.translatable(k.value().getDescriptionId()))
                        .withStyle(d > 0 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            }
            if (cand.isDamageableItem() && cur.isDamageableItem()) {
                int da = cand.getMaxDamage() - cand.getDamageValue(), db = cur.getMaxDamage() - cur.getDamageValue();
                if (da != db) diff.add(Component.literal((da > db ? "+" : "") + (da - db) + " ").append(t("durability"))
                        .withStyle(da > db ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            }
            a.add(Component.empty());
            a.add(t("compare").withStyle(net.minecraft.ChatFormatting.GRAY));
            a.add(Component.literal(" ").append(cur.getHoverName()).withStyle(net.minecraft.ChatFormatting.GOLD));
            if (diff.isEmpty()) a.add(t("compare_same").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            else for (Component c : diff) a.add(Component.literal(" ").append(c));
        }
        // uma caixa so: a direita da lista; sem espaco, a esquerda do slot; nunca por cima da lista
        int wa = tipWidth(a);
        float x = popupRect[0] + popupRect[2] + 8, y = popupRect[1];
        if (x + wa > width - 4) x = Math.max(4, screenRect(slot)[0] - wa - 10);
        tipAt(g, a, x, y);
    }

    // clique num item da lista: troca com o que esta equipado (os mesmos cliques que o jogador faria)
    private boolean clickPopup(double mx, double my) {
        if (popupSlot < 0) return false;
        for (int[] c : popupCells) {
            if (mx >= c[1] && mx < c[1] + 20 && my >= c[2] && my < c[2] + 20) {
                var gm = minecraft.gameMode;
                int id = menu.containerId;
                gm.handleInventoryMouseClick(id, c[0], 0, net.minecraft.world.inventory.ClickType.PICKUP, minecraft.player);
                gm.handleInventoryMouseClick(id, popupSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, minecraft.player);
                if (!menu.getCarried().isEmpty()) gm.handleInventoryMouseClick(id, c[0], 0, net.minecraft.world.inventory.ClickType.PICKUP, minecraft.player);
                UiSound.click();
                return true;
            }
        }
        return false;
    }

    // o proprio jogador (skin, armadura, itens nas maos) numa pose de acao, com cotovelos e joelhos
    private void character(float cx, float cy, float h, float yaw, ActionPose.Pose pose, GuiGraphics g) {
        if (minecraft.player == null) return;
        BendyPlayer.render(g, minecraft.player, X(cx), Y(cy), h * s / 2.0f, yaw, pose);
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

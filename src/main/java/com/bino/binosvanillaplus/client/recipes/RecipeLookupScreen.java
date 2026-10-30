package com.bino.binosvanillaplus.client.recipes;

import com.bino.binosvanillaplus.client.recipes.RecipeEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

// Navegador de receitas estilo JEI.
//  RESULT = "como fazer o item", USAGE = "onde o item e' usado", BROWSE = todos os itens (com busca), FAVORITES = favoritos.
//  Clique esquerdo num item: como fazer. Clique direito: onde e' usado. Teclas R / U / B (configuraveis) fazem o mesmo
//  e B marca/desmarca favorito. Backspace volta. O botao "+" de cada receita preenche a grade (se houver bancada/fornalha aberta).
public class RecipeLookupScreen extends Screen {
    public enum Mode { RESULT, USAGE, BROWSE, FAVORITES }

    private record State(Mode mode, @Nullable Item item, String search) {}
    private record Hit(int x, int y, int w, int h, ItemStack stack) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private record FillHit(int x, int y, int w, int h, RecipeEntry entry) {}

    @Nullable private final Screen parent;
    private Mode mode;
    @Nullable private Item focus;
    private String search = "";
    private final Deque<State> history = new ArrayDeque<>();

    private List<RecipeEntry> entries = List.of();
    private List<RecipeCategory> categories = List.of();
    private int catIndex = 0;
    private List<ItemStack> grid = List.of();
    private int page = 0;

    private EditBox searchBox;
    private Button prevButton, nextButton, backButton, browseButton, favButton;

    private final List<Hit> hits = new ArrayList<>();
    private final List<FillHit> fillHits = new ArrayList<>();
    private int x0, y0, panelW, panelH;
    @Nullable private ItemStack hovered = null;

    public RecipeLookupScreen(@Nullable Screen parent, Mode mode, @Nullable Item item) {
        super(Component.translatableWithFallback("recipes.binosvanillaplus.title", "Receitas"));
        this.parent = parent;
        this.mode = mode;
        this.focus = item;
        rebuild();
    }

    private static Component t(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("recipes.binosvanillaplus." + key, fallback, args);
    }

    // ---------------------------------------------------------------- dados

    private void rebuild() {
        page = 0;
        catIndex = 0;
        RecipeIndex index = RecipeIndex.get();
        switch (mode) {
            case RESULT, USAGE -> {
                entries = focus == null ? List.of()
                        : (mode == Mode.RESULT ? index.producing(focus) : index.using(focus));
                EnumSet<RecipeCategory> set = EnumSet.noneOf(RecipeCategory.class);
                for (RecipeEntry e : entries) set.add(e.category());
                categories = new ArrayList<>(set);
            }
            case BROWSE -> grid = filter(RecipeIndex.allItems(), search);
            case FAVORITES -> {
                List<ItemStack> list = new ArrayList<>();
                for (Item it : FavoritesStore.items()) list.add(new ItemStack(it));
                grid = list;
            }
        }
    }

    private static List<ItemStack> filter(List<ItemStack> all, String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all;
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : all) {
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
            boolean ok = true;
            for (String word : q.split("\\s+")) {
                if (word.startsWith("@")) ok &= id.getNamespace().contains(word.substring(1));
                else ok &= s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(word);
            }
            if (ok) out.add(s);
        }
        return out;
    }

    private void go(Mode newMode, @Nullable Item item) {
        history.push(new State(mode, focus, search));
        mode = newMode;
        focus = item;
        if (newMode != Mode.BROWSE) search = "";
        rebuild();
        syncWidgets();
    }

    private void back() {
        if (history.isEmpty()) { onClose(); return; }
        State s = history.pop();
        mode = s.mode();
        focus = s.item();
        search = s.search();
        rebuild();
        syncWidgets();
    }

    // ---------------------------------------------------------------- widgets

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 340);
        panelH = Math.min(height - 16, 240);
        x0 = (width - panelW) / 2;
        y0 = (height - panelH) / 2;

        backButton = addRenderableWidget(Button.builder(Component.literal("\u2190"), b -> back())
                .bounds(x0 + panelW - 96, y0 + 4, 20, 16).build());
        browseButton = addRenderableWidget(Button.builder(t("browse", "Itens"), b -> { if (mode != Mode.BROWSE) go(Mode.BROWSE, null); })
                .bounds(x0 + panelW - 74, y0 + 4, 36, 16).build());
        favButton = addRenderableWidget(Button.builder(Component.literal("\u2605"), b -> { if (mode != Mode.FAVORITES) go(Mode.FAVORITES, null); })
                .bounds(x0 + panelW - 36, y0 + 4, 20, 16).build());

        searchBox = new EditBox(font, x0 + 8, y0 + 26, panelW - 16, 16, t("search", "Buscar"));
        searchBox.setHint(t("search_hint", "Buscar item (@mod para filtrar por mod)"));
        searchBox.setValue(search);
        searchBox.setResponder(s -> { search = s; grid = filter(RecipeIndex.allItems(), s); page = 0; });
        addRenderableWidget(searchBox);

        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                .bounds(x0 + panelW / 2 - 60, y0 + panelH - 22, 20, 16).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> page = Math.min(pageCount() - 1, page + 1))
                .bounds(x0 + panelW / 2 + 40, y0 + panelH - 22, 20, 16).build());
        syncWidgets();
    }

    private void syncWidgets() {
        if (searchBox == null) return;
        searchBox.visible = mode == Mode.BROWSE;
        searchBox.setEditable(mode == Mode.BROWSE);
        if (mode == Mode.BROWSE) searchBox.setValue(search);
        if (mode != Mode.BROWSE) searchBox.setFocused(false);
    }

    // ---------------------------------------------------------------- layout

    private int gridCols() { return Math.max(1, (panelW - 16) / 18); }
    private int gridTop() { return mode == Mode.BROWSE ? y0 + 48 : y0 + 28; }
    private int gridRows() { return Math.max(1, (y0 + panelH - 28 - gridTop()) / 18); }

    private List<RecipeEntry> currentRecipes() {
        if (categories.isEmpty()) return List.of();
        RecipeCategory cat = categories.get(Math.min(catIndex, categories.size() - 1));
        List<RecipeEntry> out = new ArrayList<>();
        for (RecipeEntry e : entries) if (e.category() == cat) out.add(e);
        return out;
    }

    private static int cardHeight(RecipeCategory c) { return c == RecipeCategory.CRAFTING ? 64 : 30; }

    private int recipesTop() { return y0 + 54; }
    private int recipesBottom() { return y0 + panelH - 28; }

    private int perPage() {
        if (categories.isEmpty()) return 1;
        RecipeCategory cat = categories.get(Math.min(catIndex, categories.size() - 1));
        return Math.max(1, (recipesBottom() - recipesTop()) / cardHeight(cat));
    }

    private int pageCount() {
        int total = (mode == Mode.RESULT || mode == Mode.USAGE) ? currentRecipes().size() : grid.size();
        int per = (mode == Mode.RESULT || mode == Mode.USAGE) ? perPage() : gridCols() * gridRows();
        return Math.max(1, (total + per - 1) / per);
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderTransparentBackground(g);
        g.fill(x0 - 1, y0 - 1, x0 + panelW + 1, y0 + panelH + 1, 0xFF000000);
        g.fill(x0, y0, x0 + panelW, y0 + panelH, 0xF0222233);

        hits.clear();
        fillHits.clear();
        hovered = null;

        g.drawString(font, headerText(), x0 + 8, y0 + 8, 0xFFFFFFFF, true);

        if (mode == Mode.RESULT || mode == Mode.USAGE) renderRecipes(g, mx, my);
        else renderGrid(g, mx, my);

        // paginacao
        page = Math.min(page, pageCount() - 1);
        prevButton.active = page > 0;
        nextButton.active = page < pageCount() - 1;
        g.drawCenteredString(font, (page + 1) + " / " + pageCount(), x0 + panelW / 2, y0 + panelH - 18, 0xFFBBBBBB);
        backButton.active = !history.isEmpty() || parent != null;

        super.render(g, mx, my, partial);

        if (hovered != null) {
            g.renderTooltip(font, hovered, mx, my);
        }
    }

    private Component headerText() {
        return switch (mode) {
            case RESULT -> t("header_result", "Como fazer: %s", focus == null ? "" : new ItemStack(focus).getHoverName());
            case USAGE -> t("header_usage", "Usos de: %s", focus == null ? "" : new ItemStack(focus).getHoverName());
            case BROWSE -> t("header_browse", "Todos os itens (%s)", grid.size());
            case FAVORITES -> t("header_fav", "Favoritos");
        };
    }

    private void renderGrid(GuiGraphics g, int mx, int my) {
        int cols = gridCols(), rows = gridRows(), per = cols * rows;
        int left = x0 + (panelW - cols * 18) / 2, top = gridTop();
        if (grid.isEmpty()) {
            g.drawCenteredString(font, t("empty_items", "Nenhum item encontrado."), x0 + panelW / 2, top + 20, 0xFFAAAAAA);
            return;
        }
        int start = page * per;
        for (int i = 0; i < per && start + i < grid.size(); i++) {
            int x = left + (i % cols) * 18, y = top + (i / cols) * 18;
            drawStack(g, grid.get(start + i), x, y, mx, my);
        }
    }

    private void renderRecipes(GuiGraphics g, int mx, int my) {
        if (categories.isEmpty()) {
            g.drawCenteredString(font, t("empty_recipes", "Nenhuma receita encontrada."), x0 + panelW / 2, y0 + 60, 0xFFAAAAAA);
            return;
        }
        // abas das categorias
        int tx = x0 + 8;
        for (int i = 0; i < categories.size(); i++) {
            RecipeCategory c = categories.get(i);
            boolean sel = i == catIndex;
            g.fill(tx, y0 + 24, tx + 24, y0 + 48, sel ? 0xFF6B6B8A : 0xFF33334A);
            g.renderItem(c.icon(), tx + 4, y0 + 28);
            if (mx >= tx && mx < tx + 24 && my >= y0 + 24 && my < y0 + 48) {
                g.renderTooltip(font, c.title(), mx, my);
            }
            tx += 26;
        }
        RecipeCategory cat = categories.get(Math.min(catIndex, categories.size() - 1));
        g.drawString(font, cat.title(), x0 + panelW - 8 - font.width(cat.title()), y0 + 34, 0xFFDDDDDD, true);

        List<RecipeEntry> recipes = currentRecipes();
        int per = perPage(), ch = cardHeight(cat);
        int start = page * per;
        for (int i = 0; i < per && start + i < recipes.size(); i++) {
            drawCard(g, recipes.get(start + i), x0 + 8, recipesTop() + i * ch, panelW - 16, ch - 2, mx, my);
        }
    }

    private void drawCard(GuiGraphics g, RecipeEntry e, int cx, int cy, int cw, int chh, int mx, int my) {
        g.fill(cx, cy, cx + cw, cy + chh, 0x40FFFFFF);
        int ox = cx + 8, oy = cy + 4;
        if (e.category() == RecipeCategory.CRAFTING) {
            for (int i = 0; i < 9; i++) {
                int sx = ox + (i % 3) * 18, sy = oy + (i / 3) * 18;
                drawSlot(g, pick(e.inputs().get(i)), sx, sy, mx, my);
            }
            g.drawString(font, "\u2192", ox + 60, oy + 22, 0xFFFFFFFF, true);
            drawSlot(g, e.output(), ox + 78, oy + 18, mx, my);
            drawFill(g, e, cx + cw - 18, cy + chh - 14, mx, my);
        } else {
            int sx = ox;
            for (List<ItemStack> slot : e.inputs()) {
                drawSlot(g, pick(slot), sx, oy, mx, my);
                sx += 20;
            }
            g.drawString(font, "\u2192", sx + 2, oy + 5, 0xFFFFFFFF, true);
            drawSlot(g, e.output(), sx + 18, oy, mx, my);
            if (e.cookTime() > 0) {
                String info = String.format(Locale.ROOT, "%.1fs  +%.1f XP", e.cookTime() / 20f, e.xp());
                g.drawString(font, info, sx + 42, oy + 5, 0xFFAAAAAA, false);
            }
            drawFill(g, e, cx + cw - 18, cy + chh - 14, mx, my);
        }
    }

    private static ItemStack pick(List<ItemStack> alts) {
        if (alts.isEmpty()) return ItemStack.EMPTY;
        return alts.get((int) ((System.currentTimeMillis() / 1000) % alts.size()));   // alterna entre as opcoes (tags)
    }

    private void drawSlot(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my) {
        g.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"), x - 1, y - 1, 18, 18);
        if (!stack.isEmpty()) drawStackOnly(g, stack, x, y, mx, my);
    }

    private void drawStack(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my) {
        g.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"), x - 1, y - 1, 18, 18);
        drawStackOnly(g, stack, x, y, mx, my);
    }

    private void drawStackOnly(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my) {
        boolean over = mx >= x && mx < x + 16 && my >= y && my < y + 16;
        if (over) {
            g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            hovered = stack;
        }
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
        if (FavoritesStore.isFavorite(stack.getItem())) g.drawString(font, "*", x + 11, y - 1, 0xFFFFD84A, true);
        hits.add(new Hit(x, y, 16, 16, stack));
    }

    private void drawFill(GuiGraphics g, RecipeEntry e, int x, int y, int mx, int my) {
        boolean ok = canFill(e);
        boolean over = mx >= x && mx < x + 14 && my >= y && my < y + 12;
        g.fill(x, y, x + 14, y + 12, ok ? (over ? 0xFF55CC55 : 0xFF2E8B2E) : 0xFF444444);
        g.drawCenteredString(font, "+", x + 7, y + 2, ok ? 0xFFFFFFFF : 0xFF888888);
        if (over) g.renderTooltip(font, ok ? t("fill", "Preencher a grade") : t("fill_off", "Abra uma bancada ou fornalha compat\u00edvel para preencher"), mx, my);
        fillHits.add(new FillHit(x, y, 14, 12, e));
    }

    // ---------------------------------------------------------------- preencher a grade

    private boolean canFill(RecipeEntry e) {
        if (e.holder() == null || !(parent instanceof AbstractContainerScreen<?> cs)) return false;
        AbstractContainerMenu m = cs.getMenu();
        if (!(m instanceof RecipeBookMenu<?, ?> rbm)) return false;
        RecipeBookType want = switch (e.category()) {
            case CRAFTING -> RecipeBookType.CRAFTING;
            case SMELTING -> RecipeBookType.FURNACE;
            case BLASTING -> RecipeBookType.BLAST_FURNACE;
            case SMOKING -> RecipeBookType.SMOKER;
            default -> null;
        };
        if (want == null || rbm.getRecipeBookType() != want) return false;
        if (e.category() == RecipeCategory.CRAFTING && e.holder().value() instanceof CraftingRecipe cr)
            return cr.canCraftInDimensions(rbm.getGridWidth(), rbm.getGridHeight());
        return true;
    }

    private void fill(RecipeEntry e) {
        if (!canFill(e) || parent == null) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractContainerMenu m = ((AbstractContainerScreen<?>) parent).getMenu();
        if (mc.getConnection() != null)
            mc.getConnection().send(new ServerboundPlaceRecipePacket(m.containerId, (RecipeHolder<?>) e.holder(), hasShiftDown()));
        mc.setScreen(parent);
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        if (mode == Mode.RESULT || mode == Mode.USAGE) {
            int tx = x0 + 8;
            for (int i = 0; i < categories.size(); i++) {
                if (mx >= tx && mx < tx + 24 && my >= y0 + 24 && my < y0 + 48) { catIndex = i; page = 0; return true; }
                tx += 26;
            }
        }
        for (FillHit f : fillHits) {
            if (mx >= f.x() && mx < f.x() + f.w() && my >= f.y() && my < f.y() + f.h()) { fill(f.entry()); return true; }
        }
        for (Hit h : hits) {
            if (h.contains(mx, my)) {
                if (button == 1) go(Mode.USAGE, h.stack().getItem());
                else go(Mode.RESULT, h.stack().getItem());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) {
            page = Math.max(0, Math.min(pageCount() - 1, page - (int) Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean typing = searchBox != null && searchBox.visible && searchBox.isFocused();
        if (!typing) {
            if (hovered != null) {
                if (LookupKeys.SHOW_RECIPE.matches(key, scan)) { go(Mode.RESULT, hovered.getItem()); return true; }
                if (LookupKeys.SHOW_USES.matches(key, scan)) { go(Mode.USAGE, hovered.getItem()); return true; }
                if (LookupKeys.BOOKMARK.matches(key, scan)) { FavoritesStore.toggle(hovered.getItem()); if (mode == Mode.FAVORITES) rebuild(); return true; }
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) { back(); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // Sem isto o Screen.render do vanilla aplica o BLUR do fundo por cima de tudo que ja foi desenhado (era o que deixava a tela borrada)
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

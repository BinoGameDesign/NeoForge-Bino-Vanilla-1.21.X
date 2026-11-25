package com.bino.binoscombat.client.combo;

import com.bino.binoscombat.BinosCombat;
import com.bino.binoscombat.combat.CombatMove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

// LISTA DE COMBOS (menu do jogo, regras de 23/11), como a lista de golpes dos jogos de luta: so mostra como se faz cada
// combo (nome, teclas e uma linha explicando). Rola com a roda do mouse.
@EventBusSubscriber(modid = BinosCombat.MOD_ID, value = Dist.CLIENT)
public class ComboListScreen extends Screen {
    private final Screen parent;

    private record Row(String section, String name, String keys, CombatMove[] moves) {}
    private static CombatMove[] m(CombatMove... ms) { return ms; }
    private static final CombatMove L1 = CombatMove.LIGHT_1, L2 = CombatMove.LIGHT_2, L3 = CombatMove.LIGHT_3, L4 = CombatMove.LIGHT_4;

    private static final List<Row> ROWS = List.of(
            new Row("ground", "base", "L L L L L", m(L1, L2, L3, L4, CombatMove.LIGHT_5)),
            new Row("ground", "tormenta", "L L L L P", m(L1, L2, L3, L4, CombatMove.TORMENTA)),
            new Row("ground", "judgment", "L L L P", m(L1, L2, L3, CombatMove.JUDGMENT)),
            new Row("ground", "guard_break", "L L P", m(L1, L2, CombatMove.GUARD_BREAK)),
            new Row("ground", "surprise", "L P", m(L1, CombatMove.SURPRISE)),
            new Row("air", "meteor", "L ... P", m(CombatMove.AIR_1, CombatMove.AIR_2, CombatMove.AIR_3, CombatMove.METEOR)),
            new Row("crouch", "launcher", "Shift + L ... P", m(L1, L2, CombatMove.LAUNCHER)),
            new Row("context", "dash", "W W + L / P", m(CombatMove.DASH, CombatMove.DASH_SLASH)),
            new Row("context", "pierce", "W W + P", m(CombatMove.DASH, CombatMove.PIERCE)),
            new Row("context", "roll", "Q + L / P", m(CombatMove.ROLL_SLASH, CombatMove.ROLL_THRUST)),
            new Row("context", "forward", "Ctrl + L / P", m(CombatMove.FORWARD)),
            new Row("defense", "parry", "R", m(CombatMove.PARRY)),
            new Row("defense", "issen", "R (azul) L L L", m(CombatMove.PARRY, CombatMove.ISSEN, CombatMove.ISSEN)),
            new Row("defense", "backstab", "Shift + L", m(CombatMove.BACKSTAB)),
            new Row("defense", "critical", "P", m(CombatMove.CRITICAL)));


    public ComboListScreen(Screen parent) {
        super(Component.translatable("gui.binoscombat.combos.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }



    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        // como nos jogos de luta: nome do golpe a esquerda, os botoes (teclas) a direita e uma linha explicando embaixo
        int x0 = width / 2 - 170, x1 = width / 2 + 170;
        int y = 34 - scroll;
        String last = "";
        for (Row r : ROWS) {
            if (!r.section().equals(last)) {
                if (!last.isEmpty()) y += 6;
                if (y > 22 && y < height - 40) g.drawString(font, Component.translatable("gui.binoscombat.combos.section." + r.section()), x0, y, 0xF2C14E);
                y += 12;
                last = r.section();
            }
            List<net.minecraft.util.FormattedCharSequence> desc = font.split(Component.translatable("gui.binoscombat.combos." + r.name() + ".desc"), x1 - x0 - 8);
            int h = 14 + desc.size() * 9;
            if (y + h > 22 && y < height - 36) {
                g.fill(x0 - 4, y - 3, x1 + 4, y + h - 1, 0x40000000);
                g.drawString(font, Component.translatable("gui.binoscombat.combos." + r.name()), x0, y, 0xFFFFFF);
                keys(g, r.keys(), x1, y - 1);
                for (int i = 0; i < desc.size(); i++) g.drawString(font, desc.get(i), x0 + 4, y + 12 + i * 9, 0xB8B8B8);
            }
            y += h + 3;
        }
        contentH = y + scroll - 34;
    }

    // as teclas como botoes (tampinhas), alinhadas a direita: cada palavra separada por espaco e' uma tecla
    private void keys(GuiGraphics g, String keys, int right, int y) {
        String[] parts = keys.split(" ");
        int x = right;
        for (int i = parts.length - 1; i >= 0; i--) {
            String k = parts[i];
            if (k.isEmpty()) continue;
            boolean word = k.equals("+") || k.equals("/") || k.equals("...") || k.startsWith("(");
            boolean mouse = k.equals("L") || k.equals("P");
            int w = mouse ? 9 : font.width(k) + (word ? 2 : 8);
            x -= w;
            if (mouse) mouse(g, x, y - 1, k.equals("L"));
            else if (!word) {
                g.fill(x, y, x + w, y + 11, 0xFF2A2C30);
                g.fill(x, y, x + w, y + 1, 0xFF5A5E66);
                g.fill(x, y + 10, x + w, y + 11, 0xFF15161A);
                int col = k.equals("P") ? 0xFFFF9A5A : k.equals("L") ? 0xFF9AD8FF : k.equals("R") ? 0xFFB8FF8A : 0xFFFFFFFF;
                g.drawString(font, k, x + 4, y + 2, col, false);
            } else g.drawString(font, k, x + 1, y + 2, 0xA0A0A0, false);
            x -= 2;
        }
    }

    // mouse em pixel art (9x13): botao esquerdo ou direito aceso
    private static void mouse(GuiGraphics g, int x, int y, boolean left) {
        int edge = 0xFF15161A, body = 0xFF3A3D44, lit = left ? 0xFF6EC8FF : 0xFFFF9A5A;
        g.fill(x + 1, y, x + 8, y + 13, edge);
        g.fill(x, y + 1, x + 9, y + 12, edge);
        g.fill(x + 1, y + 1, x + 8, y + 12, body);
        // botoes (metade de cima), separados no meio
        g.fill(left ? x + 1 : x + 5, y + 1, left ? x + 4 : x + 8, y + 5, lit);
        g.fill(x + 4, y + 1, x + 5, y + 6, edge);
        g.fill(x + 1, y + 5, x + 8, y + 6, edge);
        g.fill(x + 2, y + 9, x + 7, y + 10, 0xFF4E525A);   // brilho embaixo
    }

    private int scroll, contentH;

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, contentH - (height - 80));
        scroll = Math.max(0, Math.min(max, scroll - (int) (dy * 14)));
        return true;
    }

    // botao "Lista de combos" no menu do jogo (canto de cima, a esquerda)
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof PauseScreen ps) || !ps.showsPauseMenu()) return;
        e.addListener(Button.builder(Component.translatable("gui.binoscombat.combos.button"),
                b -> Minecraft.getInstance().setScreen(new ComboListScreen(ps))).bounds(6, 6, 110, 20).build());
    }
}

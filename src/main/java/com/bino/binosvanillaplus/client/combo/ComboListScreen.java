package com.bino.binosvanillaplus.client.combo;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.combat.CombatMoves;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.CombatMove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

// LISTA DE COMBOS (menu do jogo, regras de 23/11): so para ver e ler. Cada combo com as teclas e a explicacao; o
// personagem mostra os combos um depois do outro, sozinho (precisa de espada ou machado na mao). Nada aqui e' clicavel.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
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
            new Row("context", "dash", "W W  L / P", m(CombatMove.DASH, CombatMove.DASH_SLASH)),
            new Row("context", "pierce", "W W  P", m(CombatMove.DASH, CombatMove.PIERCE)),
            new Row("context", "roll", "Q  L / P", m(CombatMove.ROLL_SLASH, CombatMove.ROLL_THRUST)),
            new Row("context", "forward", "Ctrl  L / P", m(CombatMove.FORWARD)),
            new Row("defense", "parry", "R", m(CombatMove.PARRY)),
            new Row("defense", "issen", "R (azul)  L L L", m(CombatMove.PARRY, CombatMove.ISSEN, CombatMove.ISSEN)),
            new Row("defense", "backstab", "Shift + L", m(CombatMove.BACKSTAB)),
            new Row("defense", "critical", "P", m(CombatMove.CRITICAL)));

    private int selected = 0, step = 0;
    private long nextAt;

    public ComboListScreen(Screen parent) {
        super(Component.translatable("gui.binosvanillaplus.combos.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
        nextAt = 0;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }

    private int rowY(int i) { return 36 + i * 13 + sectionIndex(i) * 6; }
    private static int sectionIndex(int i) {
        int n = 0;
        for (int k = 1; k <= i; k++) if (!ROWS.get(k).section().equals(ROWS.get(k - 1).section())) n++;
        return n;
    }


    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int x0 = width / 2 - 200;
        String last = "";
        for (int i = 0; i < ROWS.size(); i++) {
            Row r = ROWS.get(i);
            int y = rowY(i);
            if (!r.section().equals(last)) {
                g.drawString(font, Component.translatable("gui.binosvanillaplus.combos.section." + r.section()), x0, y - 8 - 2, 0xF2C14E);
                last = r.section();
            }
            if (i == selected) g.fill(x0 - 2, y - 2, x0 + 232, y + 10, 0x50FFFFFF);   // o que o personagem esta mostrando agora
            g.drawString(font, Component.translatable("gui.binosvanillaplus.combos." + r.name()), x0 + 2, y, 0xFFFFFF);
            g.drawString(font, r.keys(), x0 + 228 - font.width(r.keys()), y, 0xA0D8FF);
        }
        // explicacao do selecionado
        Row r = ROWS.get(selected);
        int px = width / 2 + 50, pw = 150;
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("gui.binosvanillaplus.combos." + r.name() + ".desc"), pw);
        int ty = height - 40 - lines.size() * 10;
        for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), px, ty + i * 10, 0xDDDDDD);
        // o personagem fazendo o combo
        var p = minecraft.player;
        if (p == null) return;
        boolean armed = CombatEvents.weapon(p.getMainHandItem()) != CombatEvents.Weapon.NONE;
        if (!armed) {
            List<net.minecraft.util.FormattedCharSequence> tip = font.split(Component.translatable("gui.binosvanillaplus.combos.need_weapon"), pw);
            for (int i = 0; i < tip.size(); i++) g.drawString(font, tip.get(i), px, 60 + i * 10, 0xFF8080);
        } else {
            long now = System.currentTimeMillis();
            if (now >= nextAt) {
                CombatMove mv = r.moves()[step % r.moves().length];
                CombatMoves.playLocal(mv);
                boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
                int len = com.bino.binosvanillaplus.combat.MoveTiming.lengthMs(mv, false, axe, p.getCurrentItemAttackStrengthDelay());
                step++;
                nextAt = now + (int) (len * 0.92f);
                if (step >= r.moves().length) {   // acabou este combo: pausa e passa para o proximo da lista
                    nextAt += 900;
                    step = 0;
                    selected = (selected + 1) % ROWS.size();
                }
            }
        }
        int bx = px, by = 34, bw = pw, bh = Math.max(80, ty - 44);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx, by, bx + bw, by + bh, (int) (bh * 0.36f), 0.0625f, bx + bw * 0.75f, by + bh * 0.35f, p);
    }

    // botao "Lista de combos" no menu do jogo (canto de cima, a esquerda)
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof PauseScreen ps) || !ps.showsPauseMenu()) return;
        e.addListener(Button.builder(Component.translatable("gui.binosvanillaplus.combos.button"),
                b -> Minecraft.getInstance().setScreen(new ComboListScreen(ps))).bounds(6, 6, 110, 20).build());
    }
}

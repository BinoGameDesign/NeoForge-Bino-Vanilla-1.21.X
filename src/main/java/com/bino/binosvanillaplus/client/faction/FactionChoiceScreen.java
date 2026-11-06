package com.bino.binosvanillaplus.client.faction;

import com.bino.binosvanillaplus.client.ui.BinoScreen;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.client.ui.Ease;
import com.bino.binosvanillaplus.client.ui.Tween;
import com.bino.binosvanillaplus.client.ui.UiSound;
import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.network.FactionChoosePayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

// Escolha da faccao na primeira entrada: tres cartoes (Villagers, Pillagers, Errante). Clique escolhe e o botao confirma.
// Nao fecha com Esc: precisa escolher (trocar depois so pelo ritual).
public class FactionChoiceScreen extends BinoScreen {
    private static final Faction[] OPTIONS = {Faction.VILLAGER, Faction.PILLAGER, Faction.WANDERER};
    private static final ItemStack[] ICONS = {new ItemStack(Items.BELL), new ItemStack(Items.CROSSBOW), new ItemStack(Items.COMPASS)};
    private static final int W = 355, H = 213, CARD_W = 103, CARD_H = 134;

    private int selected = -1;
    private final Tween[] hover = {new Tween(0, 50f), new Tween(0, 50f), new Tween(0, 50f)};
    private final Tween sel = new Tween(0, 55f);
    private BinoUi.Button confirm;

    public FactionChoiceScreen() {
        super("Faccao", null);
    }

    private int L() { return (width - W) / 2; }
    private int T() { return (height - H) / 2 + 4; }
    private int cardX(int i) { return L() + 16 + i * (CARD_W + 7); }
    private int cardY() { return T() + 22; }

    @Override
    protected void build() {
        confirm = new BinoUi.Button(L() + W / 2 - 50, T() + H - 30, 100, 16, Component.translatable("faction.binosvanillaplus.confirm").getString(),
                BinoUi.Style.LIGHT, () -> {
                    if (selected < 0) { shake(); return; }
                    PacketDistributor.sendToServer(new FactionChoosePayload(OPTIONS[selected].ordinal()));
                    closeTo(null);
                });
        confirm.active = false;
        addRenderableWidget(confirm);
        if (selected >= 0) sel.snap(cardX(selected));
    }

    @Override
    protected int[] panelBounds() { return new int[]{L(), T(), W, H}; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void onClose() { }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 3; i++) {
            int x = cardX(i), y = cardY();
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) {
                if (selected < 0) sel.snap(x);
                selected = i;
                confirm.active = true;
                UiSound.click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderPanel(GuiGraphics g, int mx, int my, float pt) {
        BinoUi.panel(g, L(), T(), W, H);
        BinoUi.title(g, font, Component.translatable("faction.binosvanillaplus.choose_title").getString(), L() + W / 2, T() - 6);
        // a moldura de selecao desliza entre os cartoes
        if (selected >= 0) {
            int sx = Math.round(sel.get(cardX(selected)));
            BinoUi.round(g, sx - 2, cardY() - 2, CARD_W + 4, CARD_H + 4, 0xFFFFFFFF);
        }
        for (int i = 0; i < 3; i++) {
            Faction f = OPTIONS[i];
            int x = cardX(i), y = cardY();
            boolean over = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;
            float h = hover[i].get(over ? 1f : 0f);
            int lift = Math.round(h * 2);
            float in = stagger(i);
            int dy = Math.round((1f - in) * 8) - lift;
            BinoUi.round(g, x, y + dy, CARD_W, CARD_H, Ease.mix(0xFF262A27, 0xFF323733, h));
            // icone grande
            g.pose().pushPose();
            g.pose().translate(x + CARD_W / 2f - 16, y + dy + 10, 0);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(ICONS[i], 0, 0);
            g.pose().popPose();
            Component name = Component.translatable(f.langKey()).withStyle(f.color);
            g.drawString(font, name, x + (CARD_W - font.width(name)) / 2, y + dy + 48, 0xFFFFFFFF, true);
            List<FormattedCharSequence> lines = font.split(Component.translatable("faction.binosvanillaplus." + f.id + ".desc"), CARD_W - 12);
            int ly = y + dy + 62;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, x + 6, ly, 0xFFB8B8B8, false);
                ly += 10;
            }
        }
        g.drawCenteredString(font, Component.translatable("faction.binosvanillaplus.choose_hint"), L() + W / 2, T() + H - 46, 0xFF8A8A8A);
    }
}

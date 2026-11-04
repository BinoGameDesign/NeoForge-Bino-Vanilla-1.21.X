package com.bino.binosvanillaplus.client.campfire;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.network.CampfireListPayload;
import com.bino.binosvanillaplus.network.RespawnChoicePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

// Tela de morte: alem do "Renascer" do jogo (cama ou spawn), um botao para cada fogueira registrada.
// Fogueira apagada, quebrada ou sem espaco do lado aparece desativada.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class RespawnChoices {
    private RespawnChoices() {}

    private static List<CampfireListPayload.Entry> points = new ArrayList<>();

    public static void receive(CampfireListPayload p) {
        points = new ArrayList<>(p.points());
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DeathScreen d) d.init(mc, d.width, d.height);   // a tela ja abriu: refaz os botoes
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen screen) || points.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getLevelData().isHardcore()) return;
        int w = 200, x = screen.width / 2 - w / 2, y = screen.height / 4 + 124;
        List<BinoUi.Button> mine = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            CampfireListPayload.Entry e = points.get(i);
            String dim = e.dim().endsWith("the_nether") ? " (Nether)" : e.dim().endsWith("the_end") ? " (End)" : "";
            String label = (e.valid() ? "Fogueira " : "Fogueira apagada ") + (i + 1) + ": "
                    + e.pos().getX() + ", " + e.pos().getY() + ", " + e.pos().getZ() + dim;
            final int idx = i;
            BinoUi.Button b = new BinoUi.Button(x, y + i * 20, w, 16, label, BinoUi.Style.LIGHT, () -> {
                if (mc.player == null) return;
                PacketDistributor.sendToServer(new RespawnChoicePayload(idx));
                mc.player.respawn();
                for (BinoUi.Button o : mine) o.active = false;
            });
            b.active = e.valid();
            mine.add(b);
            event.addListener(b);
        }
    }
}

package com.bino.binosvanillaplus.client.pickup;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.map.MinimapHud;
import com.bino.binosvanillaplus.client.ui.BinoUi;
import com.bino.binosvanillaplus.client.ui.Ease;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Avisos de itens coletados (no estilo do Pick Up Notifier): "+3 Pedregulho" com o icone, empilhados logo ACIMA do
// "Day N, hh:mm" do minimapa. Itens que o jogador nunca tinha tido ganham "NEW" no canto superior direito do aviso.
// Detecta pelo aumento da quantidade no inventario; ignora mudancas com um inventario/bau aberto (mexer nos itens) e os
// primeiros segundos depois de entrar no mundo/renascer (o inventario ainda esta chegando do servidor).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class PickupNotifier {
    private PickupNotifier() {}

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pickup_notifier");
    private static final long LIFE_MS = 3500, IN_MS = 220, OUT_MS = 350;
    private static final int MAX_ROWS = 6, ROW_H = 14, GAP = 2;

    private static final class Note {
        final Item item;
        final ItemStack icon;
        int count;
        boolean isNew;
        long born, last;
        float y = Float.NaN;       // posicao animada (em unidades de interface)
        Note(Item item, int count, boolean isNew) {
            this.item = item;
            this.icon = new ItemStack(item);
            this.count = count;
            this.isNew = isNew;
            this.born = this.last = System.currentTimeMillis();
        }
    }

    private static final List<Note> NOTES = new ArrayList<>();   // do mais antigo (em cima) ao mais novo (embaixo)
    private static Map<Item, Integer> before = new HashMap<>();
    private static LocalPlayer lastPlayer;
    private static int settle = 0;
    private static final Set<String> SEEN = new HashSet<>();
    private static UUID seenOwner;

    // ---------------------------------------------------------------- deteccao

    private static Map<Item, Integer> snapshot(Inventory inv) {
        Map<Item, Integer> m = new HashMap<>();
        for (ItemStack s : inv.items) add(m, s);
        for (ItemStack s : inv.armor) add(m, s);
        for (ItemStack s : inv.offhand) add(m, s);
        return m;
    }

    private static void add(Map<Item, Integer> m, ItemStack s) {
        if (!s.isEmpty()) m.merge(s.getItem(), s.getCount(), Integer::sum);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) { lastPlayer = null; NOTES.clear(); return; }

        if (p != lastPlayer) {                 // entrou no mundo / renasceu / trocou de dimensao: recomeca em silencio
            lastPlayer = p;
            settle = 40;
            before = snapshot(p.getInventory());
            NOTES.clear();
            loadSeen(p.getUUID());
            return;
        }
        Map<Item, Integer> now = snapshot(p.getInventory());
        if (settle > 0) {                      // inventario ainda chegando: so atualiza a base e conta como ja visto
            settle--;
            before = now;
            for (Item it : now.keySet()) markSeen(it);
            return;
        }
        boolean quiet = mc.screen instanceof AbstractContainerScreen<?>;   // mexendo em itens: nao e coleta
        if (!quiet) {
            for (Map.Entry<Item, Integer> e : now.entrySet()) {
                int delta = e.getValue() - before.getOrDefault(e.getKey(), 0);
                if (delta > 0) notify(e.getKey(), delta);
            }
        }
        before = now;
        for (Item it : now.keySet()) markSeen(it);   // tambem marca o que veio com a tela aberta
    }

    private static void notify(Item item, int delta) {
        boolean isNew = !SEEN.contains(key(item));
        for (Note n : NOTES) {
            if (n.item == item && System.currentTimeMillis() - n.last < LIFE_MS - OUT_MS) {   // mesmo item: soma no mesmo aviso
                n.count += delta;
                n.isNew |= isNew;
                n.last = System.currentTimeMillis();
                return;
            }
        }
        NOTES.add(new Note(item, delta, isNew));
        while (NOTES.size() > MAX_ROWS) NOTES.remove(0);
    }

    // ---------------------------------------------------------------- "ja vi este item" (salvo por jogador)

    private static String key(Item it) { return BuiltInRegistries.ITEM.getKey(it).toString(); }

    private static Path file(UUID id) { return FMLPaths.GAMEDIR.get().resolve("binos_map").resolve("seen_items_" + id + ".txt"); }

    private static void loadSeen(UUID id) {
        if (id.equals(seenOwner)) return;
        seenOwner = id;
        SEEN.clear();
        try {
            Path f = file(id);
            if (Files.exists(f)) SEEN.addAll(Files.readAllLines(f));
        } catch (Exception ignored) {
        }
    }

    private static void markSeen(Item it) {
        if (SEEN.add(key(it))) {
            try {
                Path f = file(seenOwner);
                Files.createDirectories(f.getParent());
                Files.writeString(f, key(it) + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {
            }
        }
    }

    // ---------------------------------------------------------------- desenho

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ID, PickupNotifier::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (NOTES.isEmpty() || mc.player == null || mc.options.hideGui || mc.screen != null) return;
        long now = System.currentTimeMillis();
        NOTES.removeIf(n -> now - n.last > LIFE_MS);

        float s = MinimapHud.smallScale(mc);
        int[] anchor = MinimapHud.notifyAnchor(g.guiWidth(), g.guiHeight(), mc);
        float rowH = ROW_H * s, step = (ROW_H + GAP) * s;
        int count = NOTES.size();
        for (int i = 0; i < count; i++) {
            Note n = NOTES.get(i);
            float targetY = anchor[1] - rowH - (count - 1 - i) * step;
            n.y = Float.isNaN(n.y) ? targetY : n.y + (targetY - n.y) * 0.25f;   // desliza para o lugar

            String num = "+" + n.count + " ";
            String name = n.icon.getHoverName().getString();
            int textW = mc.font.width(num) + mc.font.width(name);
            int w = 3 + 12 + 3 + textW + 5;

            float in = Ease.outCubic((now - n.born) / (float) IN_MS);
            float out = Ease.inCubic((now - n.last - (LIFE_MS - OUT_MS)) / (float) OUT_MS);
            float vis = Math.max(0f, in * (1f - out));
            if (vis < 0.03f) continue;
            float x = anchor[0] - w * s + (1f - vis) * (w * s + 8);

            g.pose().pushPose();
            g.pose().translate(x, n.y, 0);
            g.pose().scale(s, s, 1f);
            int a = Math.max(8, (int) (255 * vis));
            BinoUi.round(g, 0, 0, w, ROW_H, (Math.max(8, (int) (0xD8 * vis)) << 24) | 0x1E211F);
            g.pose().pushPose();
            g.pose().translate(3, 1, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.renderItem(n.icon, 0, 0);
            g.pose().popPose();
            g.drawString(mc.font, num, 18, 3, (a << 24) | 0x9BE37A, true);
            g.drawString(mc.font, name, 18 + mc.font.width(num), 3, (a << 24) | 0xFFFFFF, true);
            if (n.isNew) {   // etiqueta NEW no canto superior direito
                int tw = (int) Math.ceil(mc.font.width("NEW") * 0.6f);
                int bx = w - tw - 6, by = -4;
                BinoUi.round(g, bx, by, tw + 5, 8, (a << 24) | 0xFEC400);
                g.pose().pushPose();
                g.pose().translate(bx + 2.5f, by + 1.6f, 0);
                g.pose().scale(0.6f, 0.6f, 1f);
                g.drawString(mc.font, "NEW", 0, 0, (a << 24) | 0x1B1E1C, false);
                g.pose().popPose();
            }
            g.pose().popPose();
        }
    }
}

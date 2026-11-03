package com.bino.binosvanillaplus.client.pickup;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.HashSet;
import java.util.Set;

// "new" pequeno e branco nos itens recolhidos (como o mod Item Highlighter). Some quando o mouse passa por cima do item
// ou quando o jogador fecha o inventario. Marca por TIPO de item (todas as pilhas dele ganham o "new").
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class InventoryNewLabels {
    private InventoryNewLabels() {}

    private static final Set<Item> NEW_ITEMS = new HashSet<>();

    static void mark(Item item) { NEW_ITEMS.add(item); }

    static void clear() { NEW_ITEMS.clear(); }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (NEW_ITEMS.isEmpty() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Minecraft mc = Minecraft.getInstance();
        Slot hovered = screen.getSlotUnderMouse();
        if (hovered != null && !hovered.getItem().isEmpty()) NEW_ITEMS.remove(hovered.getItem().getItem());   // passou o mouse: some

        GuiGraphics g = event.getGuiGraphics();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.getItem().isEmpty() || !NEW_ITEMS.contains(slot.getItem().getItem())) continue;
            g.pose().pushPose();
            g.pose().translate(screen.getGuiLeft() + slot.x + 1, screen.getGuiTop() + slot.y + 1, 400);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.drawString(mc.font, "new", 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) clear();   // fechou o inventario: todos os "new" somem
    }
}

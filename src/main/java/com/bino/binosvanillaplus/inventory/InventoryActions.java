package com.bino.binosvanillaplus.inventory;

import com.bino.binosvanillaplus.network.InventoryActionPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.*;

// Servidor: abrir o inventario novo e organizar os 27 slots (a hotbar nao muda)
public final class InventoryActions {
    private InventoryActions() {}

    public static void handle(ServerPlayer p, int action) {
        if (action == InventoryActionPayload.OPEN) {
            if (p.isCreative() || p.isSpectator()) return;
            if (p.containerMenu != p.inventoryMenu) return;   // ja tem outra janela aberta
            ExtraSlots extra = BackSlot.slots(p);
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BinoInventoryMenu(id, inv, extra),
                    Component.translatable("container.binosvanillaplus.inventory")));
            return;
        }
        sort(p, action);
    }

    private static void sort(ServerPlayer p, int mode) {
        Inventory inv = p.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            // junta pilhas iguais
            for (ItemStack t : items) {
                if (s.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(s, t) && t.getCount() < t.getMaxStackSize()) {
                    int m = Math.min(s.getCount(), t.getMaxStackSize() - t.getCount());
                    t.grow(m);
                    s.shrink(m);
                }
            }
            if (!s.isEmpty()) items.add(s.copy());
        }
        Comparator<ItemStack> byName = Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
        Comparator<ItemStack> byId = Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
        Comparator<ItemStack> c = switch (mode) {
            case InventoryActionPayload.SORT_COUNT -> Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed().thenComparing(byName);
            case InventoryActionPayload.SORT_ID -> byId.thenComparing(byName);
            default -> byName.thenComparing(byId);
        };
        items.sort(c);
        for (int i = 9; i < 36; i++) inv.setItem(i, i - 9 < items.size() ? items.get(i - 9) : ItemStack.EMPTY);
        p.containerMenu.broadcastChanges();
    }
}

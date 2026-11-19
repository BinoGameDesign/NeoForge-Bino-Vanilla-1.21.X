package com.bino.binosvanillaplus.client.inventory;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Atalhos do mouse nas janelas de inventario (mod Menus). Comportamento inspirado no Mouse Tweaks (BSD-3), codigo
// proprio. Vale para toda janela com slots (inventario, baus, fornalha...), menos o inventario do criativo.
//  1) Botao DIREITO segurado com um item na mao, arrastando: poe 1 item em cada slot por onde passar.
//  2) Botao ESQUERDO segurado com um item na mao, arrastando: junta na mao os itens iguais dos slots por onde passar.
//     (Clique duplo continua juntando tudo, como no jogo.)
//  3) SHIFT + botao esquerdo segurado, arrastando: manda cada slot por onde passar para o outro lado (igual shift-clique).
//  4) RODINHA em cima de um item: para baixo manda 1 item para o outro lado; para cima puxa 1 item igual do outro lado.
//     "Outro lado" = bau <-> inventario; no inventario do jogador, barra de atalhos <-> resto do inventario.
// Tudo vira cliques normais do menu (o servidor confere), entao funciona em servidor sem o mod.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class MouseSlotTweaks {
    private MouseSlotTweaks() {}

    private enum Mode { NONE, SPREAD, GATHER, SHIFT_MOVE }

    private static Mode mode = Mode.NONE;
    private static final Set<Integer> visited = new HashSet<>();
    private static long lastLeftAt;
    private static int lastLeftSlot = -1;

    @Nullable
    private static AbstractContainerScreen<?> screen(Screen s) {
        return s instanceof AbstractContainerScreen<?> a && !(s instanceof CreativeModeInventoryScreen) ? a : null;
    }

    // ---------------------------------------------------------------- botoes
    @SubscribeEvent
    public static void onPress(ScreenEvent.MouseButtonPressed.Pre e) {
        AbstractContainerScreen<?> scr = screen(e.getScreen());
        if (scr == null) return;
        Slot slot = scr.getSlotUnderMouse();
        mode = Mode.NONE;
        visited.clear();
        if (slot == null) return;
        AbstractContainerMenu menu = scr.getMenu();
        ItemStack carried = menu.getCarried();
        boolean shift = Screen.hasShiftDown();
        int b = e.getButton();

        if (b == 1 && !carried.isEmpty() && !shift) {
            mode = Mode.SPREAD;
            visited.add(slot.index);
            if (canTakeOne(slot, carried)) click(menu, slot, 1, ClickType.PICKUP);
            e.setCanceled(true);
        } else if (b == 0 && shift && carried.isEmpty()) {
            mode = Mode.SHIFT_MOVE;
            visited.add(slot.index);
            if (slot.hasItem()) click(menu, slot, 0, ClickType.QUICK_MOVE);
            e.setCanceled(true);
        } else if (b == 0 && !carried.isEmpty() && !shift) {
            long now = Util.getMillis();
            if (now - lastLeftAt < 250 && lastLeftSlot == slot.index) {   // clique duplo: juntar tudo (como no jogo)
                click(menu, slot, 0, ClickType.PICKUP_ALL);
                lastLeftAt = 0;
                e.setCanceled(true);
                return;
            }
            lastLeftAt = now;
            lastLeftSlot = slot.index;
            mode = Mode.GATHER;
            visited.add(slot.index);
            if (!gather(menu, slot, carried)) click(menu, slot, 0, ClickType.PICKUP);   // item diferente/vazio: clique normal
            e.setCanceled(true);
        } else if (b == 0) {
            lastLeftAt = Util.getMillis();
            lastLeftSlot = slot.index;
        }
    }

    @SubscribeEvent
    public static void onDrag(ScreenEvent.MouseDragged.Pre e) {
        if (mode == Mode.NONE) return;
        AbstractContainerScreen<?> scr = screen(e.getScreen());
        if (scr == null) { mode = Mode.NONE; return; }
        e.setCanceled(true);   // o arrastar do jogo (dividir pilha) fica desligado enquanto o atalho esta ativo
        Slot slot = scr.getSlotUnderMouse();
        if (slot == null || !visited.add(slot.index)) return;
        AbstractContainerMenu menu = scr.getMenu();
        ItemStack carried = menu.getCarried();
        switch (mode) {
            case SPREAD -> { if (!carried.isEmpty() && canTakeOne(slot, carried)) click(menu, slot, 1, ClickType.PICKUP); }
            case GATHER -> gather(menu, slot, carried);
            case SHIFT_MOVE -> { if (Screen.hasShiftDown() && slot.hasItem()) click(menu, slot, 0, ClickType.QUICK_MOVE); }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre e) {
        if (mode == Mode.NONE) return;
        mode = Mode.NONE;
        visited.clear();
        if (screen(e.getScreen()) != null) e.setCanceled(true);
    }

    // junta na mao os itens iguais de um slot (se couber tudo na pilha da mao)
    private static boolean gather(AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        ItemStack there = slot.getItem();
        if (carried.isEmpty() || there.isEmpty() || slot instanceof ResultSlot || !ItemStack.isSameItemSameComponents(there, carried)) return false;
        if (!slot.mayPickup(player()) || !slot.mayPlace(carried)) return false;
        if (carried.getCount() + there.getCount() > carried.getMaxStackSize()) return false;
        click(menu, slot, 0, ClickType.PICKUP);   // junta a mao no slot...
        click(menu, slot, 0, ClickType.PICKUP);   // ...e pega tudo de volta
        return true;
    }

    // ---------------------------------------------------------------- rodinha
    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        AbstractContainerScreen<?> scr = screen(e.getScreen());
        if (scr == null || e.getScrollDeltaY() == 0) return;
        Slot slot = scr.getSlotUnderMouse();
        AbstractContainerMenu menu = scr.getMenu();
        if (slot == null || !slot.hasItem() || !menu.getCarried().isEmpty() || slot instanceof ResultSlot || !inSection(slot)) return;
        e.setCanceled(true);
        List<Slot> other = otherSide(menu, slot);
        ItemStack here = slot.getItem();
        if (e.getScrollDeltaY() < 0) {   // para baixo: manda 1 para o outro lado
            if (!slot.mayPickup(player())) return;
            Slot target = null;
            for (Slot s : other) if (s.hasItem() && ItemStack.isSameItemSameComponents(s.getItem(), here) && s.getItem().getCount() < s.getMaxStackSize(here) && s.mayPlace(here)) { target = s; break; }
            if (target == null) for (Slot s : other) if (!s.hasItem() && s.mayPlace(here)) { target = s; break; }
            if (target == null) return;
            click(menu, slot, 0, ClickType.PICKUP);      // pega a pilha
            click(menu, target, 1, ClickType.PICKUP);    // solta 1 no outro lado
            if (!menu.getCarried().isEmpty()) click(menu, slot, 0, ClickType.PICKUP);   // devolve o resto
        } else {                          // para cima: puxa 1 igual do outro lado
            if (here.getCount() >= slot.getMaxStackSize(here)) return;
            Slot source = null;
            for (Slot s : other) if (s.hasItem() && ItemStack.isSameItemSameComponents(s.getItem(), here) && s.mayPickup(player())) { source = s; break; }
            if (source == null) return;
            click(menu, source, 0, ClickType.PICKUP);
            click(menu, slot, 1, ClickType.PICKUP);
            if (!menu.getCarried().isEmpty()) click(menu, source, 0, ClickType.PICKUP);
        }
    }

    // ---------------------------------------------------------------- lados do menu
    private static boolean isPlayer(Slot s) { return s.container instanceof Inventory; }

    // slots que participam (fora armadura/mao secundaria do jogador e resultados)
    private static boolean inSection(Slot s) {
        if (s instanceof ResultSlot) return false;
        return !isPlayer(s) || s.getContainerSlot() < 36;
    }

    private static List<Slot> otherSide(AbstractContainerMenu menu, Slot from) {
        List<Slot> container = new ArrayList<>(), hotbar = new ArrayList<>(), main = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (!inSection(s)) continue;
            if (!isPlayer(s)) container.add(s);
            else if (s.getContainerSlot() < 9) hotbar.add(s);
            else main.add(s);
        }
        boolean hasStorage = container.size() >= 9;   // bau, barril... (grades de fabricacao pequenas nao contam)
        if (!isPlayer(from)) { List<Slot> l = new ArrayList<>(hotbar); l.addAll(main); return l; }
        if (hasStorage) return container;
        return from.getContainerSlot() < 9 ? main : hotbar;
    }

    // ---------------------------------------------------------------- ajudas
    private static boolean canTakeOne(Slot slot, ItemStack carried) {
        if (slot instanceof ResultSlot || !slot.mayPlace(carried)) return false;
        ItemStack there = slot.getItem();
        if (there.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(there, carried) && there.getCount() < slot.getMaxStackSize(there);
    }

    @Nullable
    private static LocalPlayer player() { return Minecraft.getInstance().player; }

    private static void click(AbstractContainerMenu menu, Slot slot, int button, ClickType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, button, type, mc.player);
    }
}

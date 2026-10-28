package com.bino.binosvanillaplus.menu;

import com.bino.binosvanillaplus.entity.SpiritEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Janela do Espirito: 6 fileiras de itens (so dá pra RETIRAR) + 3 botoes (tratados no servidor)
public class SpiritMenu extends AbstractContainerMenu {
    public static final int BUTTON_TAKE_ALL = 0;
    public static final int BUTTON_TAKE_XP = 1;
    public static final int BUTTON_DISSIPATE = 2;
    private static final int ROWS = 6;
    private static final int SPIRIT_SLOTS = ROWS * 9;

    private final Container container;
    private final ContainerData data;       // data 0 = xp guardada (sincroniza com o cliente)
    private final SpiritEntity spirit;      // null no cliente
    private boolean confirmDissipate;

    // Cliente
    public SpiritMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SPIRIT_SLOTS), new SimpleContainerData(1), null);
    }

    // Servidor
    public SpiritMenu(int id, Inventory inventory, SpiritEntity spirit) {
        this(id, inventory, spirit.getItems(), new ContainerData() {
            @Override public int get(int index) { return Math.min(spirit.getExperience(), Short.MAX_VALUE); }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 1; }
        }, spirit);
    }

    private SpiritMenu(int id, Inventory inventory, Container container, ContainerData data, SpiritEntity spirit) {
        super(ModMenuTypes.SPIRIT.get(), id);
        checkContainerSize(container, SPIRIT_SLOTS);
        this.container = container;
        this.data = data;
        this.spirit = spirit;
        container.startOpen(inventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }   // so retirar
                });
            }
        }
        int y = 18 + ROWS * 18 + 14;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inventory, col, 8 + col * 18, y + 58));

        this.addDataSlots(data);
    }

    public int getExperience() { return data.get(0); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (spirit == null || player.level().isClientSide) return false;
        if (id != BUTTON_DISSIPATE) confirmDissipate = false;
        switch (id) {
            case BUTTON_TAKE_ALL -> {
                boolean leftover = false;
                for (int i = 0; i < SPIRIT_SLOTS; i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;
                    player.getInventory().add(stack);          // o que nao couber fica no espirito
                    if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY); else leftover = true;
                }
                container.setChanged();
                takeExperience(player);
                if (leftover) player.displayClientMessage(Component.translatable("message.binosvanillaplus.spirit.inventory_full"), true);
                return true;
            }
            case BUTTON_TAKE_XP -> { takeExperience(player); return true; }
            case BUTTON_DISSIPATE -> {
                // Se ainda ha itens ou xp, pede confirmacao (segundo clique)
                if (!spirit.isEmptyOfLoot() && !confirmDissipate) {
                    confirmDissipate = true;
                    player.displayClientMessage(Component.translatable("message.binosvanillaplus.spirit.confirm"), true);
                    return true;
                }
                spirit.dissipate();
                player.closeContainer();
                return true;
            }
            default -> { return false; }
        }
    }

    private void takeExperience(Player player) {
        int xp = spirit.getExperience();
        if (xp > 0) {
            spirit.setExperience(0);
            player.giveExperiencePoints(xp);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < SPIRIT_SLOTS) {                          // do espirito para o inventario
                if (!this.moveItemStackTo(stack, SPIRIT_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;                            // nao da pra guardar itens no espirito
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (spirit == null) return true;
        return spirit.isAlive() && player.distanceToSqr(spirit) < 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}

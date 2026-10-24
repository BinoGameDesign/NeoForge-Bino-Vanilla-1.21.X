package com.bino.binosvanillaplus.menu;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Menu da Mesa de Lapidação: slot 0 = minério bruto, slot 1 = sílex, slot 2 = resultado
public class LapidaryMenu extends AbstractContainerMenu {

    private static final int RESULT_SLOT = 2;

    private final ContainerLevelAccess access;

    // Os dois espaços de entrada. Sempre que mudam, o resultado é recalculado.
    private final Container input = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            LapidaryMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();

    // Usado pelo cliente (quando o servidor abre o menu)
    public LapidaryMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public LapidaryMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenuTypes.LAPIDARY.get(), containerId);
        this.access = access;

        // Posições iguais às da bigorna: 27, 76 e 134 (altura 47)
        this.addSlot(new Slot(this.input, 0, 27, 47) {
            @Override public boolean mayPlace(ItemStack stack) { return gemFor(stack) != null; }
        });
        this.addSlot(new Slot(this.input, 1, 76, 47) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.FLINT); }
        });
        this.addSlot(new Slot(this.result, 0, 134, 47) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }   // ninguém coloca nada no resultado

            // Ao pegar o resultado: gasta 1 minério e 1 sílex
            @Override
            public void onTake(Player player, ItemStack stack) {
                LapidaryMenu.this.input.removeItem(0, 1);
                LapidaryMenu.this.input.removeItem(1, 1);
                LapidaryMenu.this.access.execute((level, pos) ->
                        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.2f));
                super.onTake(player, stack);
            }
        });

        // Inventário do jogador (3 linhas) e barra de itens
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    // Qual gema sai de cada minério bruto (nulo = item não aceito)
    public static Item gemFor(ItemStack raw) {
        if (raw.is(ModItems.RAW_DIAMOND)) return Items.DIAMOND;
        if (raw.is(ModItems.RAW_RUBI)) return ModItems.RUBI.get();
        if (raw.is(ModItems.RAW_SAPPHIRE)) return ModItems.SAPPHIRE.get();
        if (raw.is(ModItems.RAW_JADE)) return ModItems.JADE.get();
        return null;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.input) {
            Item gem = gemFor(this.input.getItem(0));
            boolean ready = gem != null && this.input.getItem(1).is(Items.FLINT);
            this.result.setItem(0, ready ? new ItemStack(gem) : ItemStack.EMPTY);
            this.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.LAPIDARY_TABLE.get());
    }

    // Ao fechar o menu, devolve ao jogador o que ficou nos espaços de entrada
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.input));
    }

    // Shift + clique
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, original);
            } else if (index < RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, 3, 39, false)) return ItemStack.EMPTY;
            } else if (gemFor(stack) != null) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(Items.FLINT)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (index < 30) {
                if (!this.moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 3, 30, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return original;
    }
}

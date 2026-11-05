package com.bino.binosvanillaplus.inventory;

import com.bino.binosvanillaplus.menu.ModMenuTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

// Inventario novo (abas Inventario / Fabricacao / Armadura). Um menu so com tudo; a tela mostra cada grupo na sua aba.
// Ordem dos slots: 0 resultado, 1-4 grade 2x2, 5-8 armadura (cabeca..pes), 9-35 inventario, 36-44 hotbar,
// 45 mao secundaria, 46-50 extras (brincos, colar, anel, longo alcance, costas).
public class BinoInventoryMenu extends AbstractContainerMenu {
    public static final int RESULT = 0, CRAFT = 1, ARMOR = 5, MAIN = 9, HOTBAR = 36, OFFHAND = 45, EXTRA = 46, END = 51;
    public static final int TAB_INV = 1, TAB_CRAFT = 2, TAB_ARMOR = 4;

    // aba aberta no cliente (as outras abas escondem os seus slots)
    public static int clientTab = TAB_INV;

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Player player;
    private final boolean client;
    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();

    // cliente
    public BinoInventoryMenu(int id, Inventory inv) { this(id, inv, new ItemStackHandler(ExtraSlots.SIZE)); }

    // servidor
    public BinoInventoryMenu(int id, Inventory inv, IItemHandler extra) {
        super(ModMenuTypes.BINO_INVENTORY.get(), id);
        this.player = inv.player;
        this.client = inv.player.level().isClientSide;

        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, fakeX(), FAKE_Y) {
            @Override public boolean isActive() { return shown(TAB_CRAFT); }
            @Override public boolean isHighlightable() { return false; }
        });
        for (int i = 0; i < 4; i++) addSlot(new TabSlot(craftSlots, i, TAB_CRAFT));
        for (int i = 0; i < 4; i++) {
            EquipmentSlot es = ARMOR_SLOTS[i];
            addSlot(new TabSlot(inv, 39 - i, TAB_ARMOR) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack s) { return s.canEquip(es, player); }
                @Override public boolean mayPickup(Player p) {
                    ItemStack s = getItem();
                    return (s.isEmpty() || p.isCreative() || !EnchantmentHelper.has(s, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(p);
                }
                @Override public void setByPlayer(ItemStack n, ItemStack old) { player.onEquipItem(es, old, n); super.setByPlayer(n, old); }
                @Override public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() { return null; }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new TabSlot(inv, 9 + r * 9 + c, TAB_INV | TAB_CRAFT));
        for (int c = 0; c < 9; c++) addSlot(new TabSlot(inv, c, TAB_INV | TAB_CRAFT));
        addSlot(new TabSlot(inv, 40, TAB_ARMOR) {
            @Override public void setByPlayer(ItemStack n, ItemStack old) { player.onEquipItem(EquipmentSlot.OFFHAND, old, n); super.setByPlayer(n, old); }
        });
        for (int i = 0; i < ExtraSlots.SIZE; i++) {
            final int idx = i;
            addSlot(new SlotItemHandler(extra, i, fakeX(), FAKE_Y) {
                @Override public boolean isActive() { return shown(TAB_ARMOR); }
                @Override public boolean isHighlightable() { return false; }
                @Override public boolean mayPlace(ItemStack s) { return ExtraSlots.accepts(idx, s); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
    }

    // A tela desenha cada slot no lugar do Figma. O x do jogo so serve para a tela saber QUAL slot e' (x = -10000 + 20*indice).
    public static final int FAKE_Y = -10000;
    private int fakeX() { return -10000 + slots.size() * 20; }
    public static int indexFromFake(int x) { return (x + 10000) / 20; }

    private boolean shown(int tab) { return !client || (clientTab & tab) != 0; }

    // slot que so aparece em algumas abas
    private class TabSlot extends Slot {
        private final int tabs;
        TabSlot(Container c, int index, int tabs) { super(c, index, fakeX(), FAKE_Y); this.tabs = tabs; }
        @Override public boolean isActive() { return shown(tabs); }
        @Override public boolean isHighlightable() { return false; }
    }

    // ---------------------------------------------------------------- fabricacao 2x2

    @Override
    public void slotsChanged(Container container) {
        if (container != craftSlots || !(player instanceof ServerPlayer sp)) return;
        Level level = sp.level();
        CraftingInput in = craftSlots.asCraftInput();
        ItemStack out = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> r = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, level);
        if (r.isPresent() && resultSlots.setRecipeUsed(level, sp, r.get())) {
            ItemStack a = r.get().value().assemble(in, level.registryAccess());
            if (a.isItemEnabled(level.enabledFeatures())) out = a;
        }
        resultSlots.setItem(0, out);
        setRemoteSlot(RESULT, out);
        sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, out));
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        resultSlots.clearContent();
        if (!p.level().isClientSide) clearContainer(p, craftSlots);
    }

    @Override
    public boolean stillValid(Player p) { return true; }

    // ---------------------------------------------------------------- shift + clique

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index == RESULT) {
            stack.getItem().onCraftedBy(stack, p.level(), p);
            if (!moveItemStackTo(stack, MAIN, OFFHAND, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index < MAIN || index == OFFHAND || index >= EXTRA) {
            if (!moveItemStackTo(stack, MAIN, OFFHAND, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            EquipmentSlot es = p.getEquipmentSlotForItem(stack);
            if (es.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                int a = ARMOR + (3 - es.getIndex());
                if (!slots.get(a).hasItem()) moved = moveItemStackTo(stack, a, a + 1, false);
            }
            if (!moved) {
                for (int i = 0; i < ExtraSlots.SIZE && !moved; i++) {
                    Slot e = slots.get(EXTRA + i);
                    if (!e.hasItem() && e.mayPlace(stack)) moved = moveItemStackTo(stack, EXTRA + i, EXTRA + i + 1, false);
                }
            }
            if (!moved) {
                if (index < HOTBAR) moved = moveItemStackTo(stack, HOTBAR, OFFHAND, false);
                else moved = moveItemStackTo(stack, MAIN, HOTBAR, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, copy); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        if (index == RESULT) p.drop(stack, false);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}

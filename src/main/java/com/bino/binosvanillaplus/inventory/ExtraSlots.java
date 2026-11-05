package com.bino.binosvanillaplus.inventory;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.items.ItemStackHandler;

// Slots extras do jogador (aba Armadura): brincos, colar, anel, arma de longo alcance e costas.
// Guardados no jogador (data attachment), caem ao morrer como o resto do inventario.
public class ExtraSlots extends ItemStackHandler {
    public static final int EARRING = 0, NECKLACE = 1, RING = 2, RANGED = 3, BACK = 4, SIZE = 5;

    // acessorios futuros: basta colocar os itens nestas tags
    public static final TagKey<Item> EARRINGS = tag("accessories/earring"), NECKLACES = tag("accessories/necklace"), RINGS = tag("accessories/ring");

    private static TagKey<Item> tag(String p) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, p)); }

    private final IAttachmentHolder holder;

    public ExtraSlots(IAttachmentHolder holder) {
        super(SIZE);
        this.holder = holder;
    }

    public static boolean accepts(int slot, ItemStack s) {
        if (s.isEmpty()) return true;
        return switch (slot) {
            case EARRING -> s.is(EARRINGS);
            case NECKLACE -> s.is(NECKLACES);
            case RING -> s.is(RINGS);
            case RANGED -> s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem || s.getItem() instanceof TridentItem;
            case BACK -> s.getItem() instanceof ElytraItem;
            default -> false;
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }

    @Override
    public int getSlotLimit(int slot) { return 1; }

    @Override
    protected void onContentsChanged(int slot) {
        if (slot == BACK && holder instanceof ServerPlayer p) BackSlot.broadcast(p);
    }
}

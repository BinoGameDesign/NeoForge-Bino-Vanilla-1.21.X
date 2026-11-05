package com.bino.binosvanillaplus.inventory;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import com.bino.binosvanillaplus.network.BackItemPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

// Elytra no slot das costas: voa mesmo com peitoral no corpo. Os mixins de voo e de desenho perguntam aqui qual item
// usar no lugar do peitoral. O cliente recebe o item das costas de cada jogador (para voar e para desenhar a elytra).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class BackSlot {
    private BackSlot() {}

    // cliente: item nas costas de cada jogador visivel (id da entidade -> item)
    public static final Map<Integer, ItemStack> CLIENT = new HashMap<>();

    public static ExtraSlots slots(Player p) { return p.getData(ModAttachments.EXTRA_SLOTS.get()); }

    public static ItemStack back(LivingEntity e) {
        if (!(e instanceof Player p)) return ItemStack.EMPTY;
        if (e.level().isClientSide) return CLIENT.getOrDefault(e.getId(), ItemStack.EMPTY);
        return slots(p).getStackInSlot(ExtraSlots.BACK);
    }

    // usado no lugar de getItemBySlot(CHEST) no voo e no desenho da elytra
    public static ItemStack flightStack(LivingEntity e, EquipmentSlot slot, ItemStack original) {
        if (slot != EquipmentSlot.CHEST || original.canElytraFly(e)) return original;
        ItemStack b = back(e);
        return !b.isEmpty() && b.canElytraFly(e) ? b : original;
    }

    public static void broadcast(ServerPlayer p) {
        BackItemPayload msg = new BackItemPayload(p.getId(), slots(p).getStackInSlot(ExtraSlots.BACK).copy());
        PacketDistributor.sendToPlayer(p, msg);
        PacketDistributor.sendToPlayersTrackingEntity(p, msg);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer p) broadcast(p); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer p) broadcast(p); }

    @SubscribeEvent
    public static void onDim(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer p) broadcast(p); }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer other && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new BackItemPayload(other.getId(), slots(other).getStackInSlot(ExtraSlots.BACK).copy()));
        }
    }

    // morreu: os slots extras caem junto com o resto (ou vao para o tumulo). HIGH = antes do tumulo recolher os drops.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) return;
        ExtraSlots s = slots(p);
        for (int i = 0; i < ExtraSlots.SIZE; i++) {
            ItemStack st = s.getStackInSlot(i);
            if (st.isEmpty()) continue;
            net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getEyeY() - 0.3, p.getZ(), st.copy());
            ie.setPickUpDelay(40);
            event.getDrops().add(ie);
            s.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}

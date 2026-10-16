package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.custom.CyalumeStickItem;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class CyalumeEvents {

    // Golpe em qualquer entidade (mobs, jogadores, quadros de armadura...)
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!player.getMainHandItem().is(ModItems.CYALUME_STICK.get())) return;

        Entity target = event.getTarget();
        CyalumeStickItem.glowBurst((ServerLevel) player.level(),
                target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ());
    }

    // Golpe em bloco (começo do clique esquerdo, funciona também no criativo)
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!event.getItemStack().is(ModItems.CYALUME_STICK.get())) return;

        // Solta as partículas na face do bloco que foi golpeada
        Direction face = event.getFace() != null ? event.getFace() : Direction.UP;
        Vec3 pos = Vec3.atCenterOf(event.getPos())
                .add(face.getStepX() * 0.55, face.getStepY() * 0.55, face.getStepZ() * 0.55);
        CyalumeStickItem.glowBurst((ServerLevel) player.level(), pos.x, pos.y, pos.z);
    }
}
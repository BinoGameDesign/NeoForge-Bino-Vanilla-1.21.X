package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

// Deixa a corda mais "de escada": W sobe (a corda fica solta no ar, entao nao ha parede para empurrar), e no topo
// ela da um impulso para o jogador conseguir subir no parapeito.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class RopeClimbEvents {
    private static final double POP_SPEED = 0.42;   // impulso para cima ao chegar no topo da corda
    private static int popCooldown = 0;

    private static boolean onRope(Player p) {
        return p.onClimbable() && p.level().getBlockState(p.blockPosition()).is(ModBlocks.ROPE.get());
    }

    // Andar para frente na corda = subir (igual a escada, onde voce empurra a parede)
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player p = event.getEntity();
        if (event.getInput().forwardImpulse > 0 && onRope(p)) event.getInput().jumping = true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { popCooldown = 0; return; }
        if (popCooldown > 0) popCooldown--;
        if (popCooldown > 0 || !onRope(p)) return;

        BlockPos feet = p.blockPosition();
        if (p.level().getBlockState(feet.above()).is(ModBlocks.ROPE.get())) return;   // so no segmento do topo
        if (!(mc.options.keyUp.isDown() || mc.options.keyJump.isDown())) return;
        if (p.getY() - feet.getY() < 0.5) return;                                       // so na parte alta do bloco

        Vec3 look = p.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        fwd = fwd.lengthSqr() > 1.0E-4 ? fwd.normalize().scale(0.12) : Vec3.ZERO;
        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x + fwd.x, POP_SPEED, dm.z + fwd.z);
        popCooldown = 12;
    }
}

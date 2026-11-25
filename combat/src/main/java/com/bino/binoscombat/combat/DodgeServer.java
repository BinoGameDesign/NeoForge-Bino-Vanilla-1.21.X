package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
import com.bino.binoscombat.CombatSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Rolamento (mod Combat), servidor: quadros de invencibilidade (i-frames) como no Elden Ring. Durante o comeco do
// rolamento os golpes, flechas e explosoes passam "por voce" (aparece "Miss"). Queda, fogo, afogamento etc. pegam.
// Rolar bem em cima do golpe de um mob = ESQUIVA PERFEITA (o mob fica lento e abre a Rajada).
@EventBusSubscriber(modid = BinosCombat.MOD_ID)
public final class DodgeServer {
    private DodgeServer() {}

    // ticks: invencivel de I_FROM ate I_TO depois do inicio; recarga entre rolamentos
    public static final int I_FROM = 0, I_TO = 7, I_TO_BACK = 4, COOLDOWN = 11;

    private record Roll(long tick, boolean back) {}
    private static final Map<UUID, Roll> ROLLS = new HashMap<>();

    public static void onRoll(ServerPlayer p, RollPayload m) {
        long now = p.level().getGameTime();
        Roll last = ROLLS.get(p.getUUID());
        if (last != null && now - last.tick() < COOLDOWN - 1) return;   // rapido demais (cliente ja segura a recarga)
        if (p.isPassenger() || p.isFallFlying() || p.getAbilities().flying) return;
        ROLLS.put(p.getUUID(), new Roll(now, m.back()));
        p.setSprinting(false);
        p.level().playSound(p, p.getX(), p.getY() + 0.5, p.getZ(), CombatSounds.SWORD_SWING.get(), SoundSource.PLAYERS, 0.5f, 0.55f);
        PacketDistributor.sendToPlayersTrackingEntity(p, new RollSeenPayload(p.getId(), m.yaw(), m.back()));
    }

    static boolean invulnerable(ServerPlayer p) {
        Roll r = ROLLS.get(p.getUUID());
        if (r == null) return false;
        long dt = p.level().getGameTime() - r.tick();
        return dt >= I_FROM && dt <= (r.back() ? I_TO_BACK : I_TO);
    }

    private static boolean dodgeable(DamageSource s) {
        if (s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return s.getEntity() != null || s.getDirectEntity() != null || s.is(DamageTypeTags.IS_PROJECTILE) || s.is(DamageTypeTags.IS_EXPLOSION);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !dodgeable(e.getSource()) || !invulnerable(p)) return;
        e.setCanceled(true);
        CombatEvents.miss(p);
        // esquiva perfeita: rolou nos ultimos ~0,15 s antes do golpe de um mob -> Rajada
        Roll r = ROLLS.get(p.getUUID());
        if (r != null && !r.back() && p.level().getGameTime() - r.tick() <= 3
                && e.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity atk && !(atk instanceof net.minecraft.world.entity.player.Player))
            CombatActionsServer.perfectDodge(p, atk);
    }
}

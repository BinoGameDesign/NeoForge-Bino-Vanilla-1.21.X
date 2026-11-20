package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Servidor dos golpes do hack and slash (mod Combat):
//  - golpe LEVE: o ataque normal do jogo continua acontecendo; aqui so entra o bonus do tipo (investida, no ar, final
//    do combo) para o golpe que chegar logo depois do aviso do cliente;
//  - golpe PESADO: tem preparacao (a arma sobe) e o servidor acerta todo mundo num arco a frente (ou em volta, no
//    golpe pesado pulando, que cai como uma pancada no chao), com dano maior e empurrao forte. Tem recarga.
//  - avisa os outros jogadores para eles verem a animacao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class CombatMovesServer {
    private CombatMovesServer() {}

    private record Recent(CombatMove move, boolean heavy, long tick, int window) {}
    private record Pending(CombatMove move, boolean axe, long atTick) {}

    private static final Map<UUID, Recent> RECENT = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> HEAVY_READY = new HashMap<>();
    private static boolean ownHit;   // o proprio golpe pesado esta batendo (nao aplicar o bonus do leve por cima)

    public static void onMove(ServerPlayer p, MovePayload m) {
        CombatEvents.Weapon w = CombatEvents.weapon(p.getMainHandItem());
        if (w == CombatEvents.Weapon.NONE) return;
        boolean axe = w == CombatEvents.Weapon.AXE;
        CombatMove move = CombatMove.of(m.move());
        float delay = p.getCurrentItemAttackStrengthDelay();
        long now = p.level().getGameTime();
        if (m.heavy()) {
            if (now < HEAVY_READY.getOrDefault(p.getUUID(), 0L)) return;   // ainda recarregando
            int windup = MoveTiming.windupTicks(move, true, axe, delay);
            PENDING.put(p.getUUID(), new Pending(move, axe, now + windup));
            HEAVY_READY.put(p.getUUID(), now + MoveTiming.lengthTicks(move, true, axe, delay) - 2);
        }
        RECENT.put(p.getUUID(), new Recent(move, m.heavy(), now, MoveTiming.windupTicks(move, m.heavy(), axe, delay) + 4));
        PacketDistributor.sendToPlayersTrackingEntity(p, new MoveBroadcastPayload(p.getId(), m.move(), m.heavy(),
                MoveTiming.lengthMs(move, m.heavy(), axe, delay)));
    }

    // bonus do golpe leve que acabou de ser anunciado (investida, no ar, final do combo)
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (ownHit || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p) return;
        Recent r = RECENT.get(p.getUUID());
        if (r == null || r.heavy() || p.level().getGameTime() - r.tick() > r.window()) return;
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        e.setAmount(e.getAmount() * r.move().damage(false, axe));
    }

    // golpe pesado: na hora do impacto, acerta quem estiver no arco a frente (ou em volta, caindo do pulo)
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Pending pd = PENDING.get(p.getUUID());
        if (pd == null || p.level().getGameTime() < pd.atTick()) return;
        PENDING.remove(p.getUUID());
        if (!p.isAlive() || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return;
        ServerLevel level = p.serverLevel();
        boolean slam = pd.move() == CombatMove.JUMP;
        double reach = pd.move() == CombatMove.FORWARD ? 3.9 : pd.move() == CombatMove.FINISHER ? 3.6 : 3.2;
        double cone = pd.move() == CombatMove.FINISHER ? -0.1 : 0.35;   // cos do meio-angulo (finalizador quase em volta)
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * pd.move().damage(true, pd.axe());
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        boolean hitAny = false;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(reach + 1),
                t -> t != p && t.isAlive() && !t.isAlliedTo(p) && t.isPickable())) {
            Vec3 to = t.position().subtract(p.position());
            double d = to.horizontalDistance();
            if (d > reach + t.getBbWidth() * 0.5 || Math.abs(to.y) > 2.5) continue;
            if (!slam && d > 0.6 && to.multiply(1, 0, 1).normalize().dot(look) < cone) continue;
            if (!p.hasLineOfSight(t)) continue;
            ownHit = true;
            boolean ok = t.hurt(p.damageSources().playerAttack(p), dmg);
            ownHit = false;
            if (ok) {
                hitAny = true;
                Vec3 push = slam ? to.multiply(1, 0, 1).normalize() : look;
                t.knockback(pd.axe() ? 0.9 : 0.7, -push.x, -push.z);
                if (slam) t.setDeltaMovement(t.getDeltaMovement().add(0, 0.25, 0));
            }
        }
        if (hitAny) p.getMainHandItem().hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        if (slam) level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                level.getBlockState(p.blockPosition().below())), p.getX(), p.getY() + 0.1, p.getZ(), 30, 1.0, 0.05, 1.0, 0.15);
        level.playSound(p, p.getX(), p.getEyeY(), p.getZ(), pd.axe() ? ModSounds.AXE_SWING.get() : ModSounds.SWORD_SWING.get(),
                SoundSource.PLAYERS, 1.0f, 0.75f);
        p.resetAttackStrengthTicker();
    }
}

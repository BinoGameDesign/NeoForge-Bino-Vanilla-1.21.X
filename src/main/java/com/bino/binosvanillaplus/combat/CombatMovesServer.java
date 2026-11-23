package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Servidor dos golpes do hack and slash (mod Combat):
//  - golpe LEVE (ataque normal do jogo, no impacto da animacao): aqui so entra o bonus do golpe anunciado
//    (CombatMove.damage) e quanto ele enche a postura do alvo.
//  - golpe do SERVIDOR (pesado, Juizo, Quebra-Guarda, saida de rolamento pesada, pulo/investida pesados): tem
//    preparacao e acerta todo mundo num arco a frente (ou em volta, nas pancadas no chao), com empurrao forte.
//  - avisa os outros jogadores para eles verem a animacao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class CombatMovesServer {
    private CombatMovesServer() {}

    private record Recent(CombatMove move, boolean heavy, long tick, int window) {}
    private record Pending(CombatMove move, boolean axe, long atTick, int charge, int step) {}

    private static final Map<UUID, Recent> RECENT = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> HEAVY_READY = new HashMap<>();
    static boolean ownHit;   // golpe feito aqui (pesado, Issen, critico...): sem o bonus do leve por cima
    private static CombatMove hitting;   // golpe do servidor que esta batendo agora (para a postura)
    private static float hittingCharge = 1f;   // carga do pesado que esta batendo (multiplica a postura)
    static final Map<UUID, Long> STAB = new HashMap<>();   // mob -> tick em que levou uma estocada (morte "com stab")
    private static final Map<UUID, Long> JUGGLE = new HashMap<>();   // mob no malabarismo: sem empurrao do golpe
    private record Hover(LivingEntity mob, long until, double targetY) {}
    private static final Map<UUID, Hover> HOVER = new HashMap<>();   // mob no malabarismo subindo ate a altura alvo

    // o mob no malabarismo esta "congelado" (Posture.freeze, sem IA nem gravidade); aqui ele sobe suave ate a altura
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        if (HOVER.isEmpty()) return;
        for (java.util.Iterator<Hover> it = HOVER.values().iterator(); it.hasNext(); ) {
            Hover h = it.next();
            LivingEntity m = h.mob();
            if (!m.isAlive() || m.isRemoved() || m.level().getGameTime() > h.until()) { it.remove(); continue; }
            double dy = net.minecraft.util.Mth.clamp((h.targetY() - m.getY()) * 0.35, -0.04, 0.3);
            if (Math.abs(dy) > 1e-3) m.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, dy, 0));
        }
    }

    @SubscribeEvent
    public static void onKnockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent e) {
        Long until = JUGGLE.get(e.getEntity().getUUID());
        if (until == null) return;
        if (e.getEntity().level().getGameTime() <= until) e.setCanceled(true);
        else JUGGLE.remove(e.getEntity().getUUID());
        if (JUGGLE.size() > 256) JUGGLE.clear();
    }

    static boolean stabMove(CombatMove m, boolean axe) {
        return switch (m) {
            case FORWARD, ROLL_THRUST -> true;
            case PIERCE, GUARD_BREAK, JUDGMENT, CRITICAL -> !axe;
            default -> false;
        };
    }

    public static void onMove(ServerPlayer p, MovePayload m) {
        CombatEvents.Weapon w = CombatEvents.weapon(p.getMainHandItem());
        if (w == CombatEvents.Weapon.NONE) return;
        boolean axe = w == CombatEvents.Weapon.AXE;
        CombatMove move = CombatMove.of(m.move());
        float delay = p.getCurrentItemAttackStrengthDelay();
        if (move.visualOnly()) {   // so animacao (avanco); parry, Issen etc. vem por CombatActionPayload
            PacketDistributor.sendToPlayersTrackingEntity(p, new MoveBroadcastPayload(p.getId(), m.move(), m.heavy(),
                    MoveTiming.lengthMs(move, m.heavy(), axe, delay)));
            return;
        }
        long now = p.level().getGameTime();
        CombatAI.playerAttack(p, move.serverHit(m.heavy()));   // mobs ageis por perto podem esquivar
        PvpClash.swing(p);
        if (move.serverHit(m.heavy())) {
            if (now < HEAVY_READY.getOrDefault(p.getUUID(), 0L)) return;   // ainda recarregando
            // passo do combo nos 4 bits de baixo, nivel de carga do pesado nos bits 4-5
            PENDING.put(p.getUUID(), new Pending(move, axe, now + MoveTiming.windupTicks(move, m.heavy(), axe, delay), (m.step() >> 4) & 3, m.step() & 15));
            HEAVY_READY.put(p.getUUID(), now + MoveTiming.lengthTicks(move, m.heavy(), axe, delay) - 3);
        }
        RECENT.put(p.getUUID(), new Recent(move, m.heavy(), now, MoveTiming.windupTicks(move, m.heavy(), axe, delay) + 5));
        PacketDistributor.sendToPlayersTrackingEntity(p, new MoveBroadcastPayload(p.getId(), m.move(), m.heavy(),
                MoveTiming.lengthMs(move, m.heavy(), axe, delay)));
    }

    // so a animacao para quem esta vendo (parry, Issen, Rajada, critico)
    static void broadcast(ServerPlayer p, CombatMove move, boolean heavy) {
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        PacketDistributor.sendToPlayersTrackingEntity(p, new MoveBroadcastPayload(p.getId(), (byte) move.ordinal(), heavy,
                MoveTiming.lengthMs(move, heavy, axe, p.getCurrentItemAttackStrengthDelay())));
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        RECENT.remove(id); PENDING.remove(id); HEAVY_READY.remove(id);
    }

    private static Recent recentLight(ServerPlayer p) {
        Recent r = RECENT.get(p.getUUID());
        if (r == null || r.heavy() || !r.move().vanillaHit(false) || p.level().getGameTime() - r.tick() > r.window()) return null;
        return r;
    }

    // bonus do golpe leve que acabou de ser anunciado
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (ownHit || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p) return;
        Recent r = recentLight(p);
        if (r == null) return;
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        e.setAmount(e.getAmount() * r.move().damage(false, axe));
    }

    // postura: todo golpe corpo a corpo de jogador enche a barra do alvo (golpes fortes enchem mais)
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity v = e.getEntity();
        if (v instanceof Player || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p) return;
        if (e.getNewDamage() <= 0) return;
        float mult = 1f;
        if (hitting != null) mult = hitting.posture() * hittingCharge;
        else if (!ownHit) { Recent r = recentLight(p); if (r != null) mult = r.move().posture(); }
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        // arma mais pesada (machado) enche mais a postura; Forca tambem (leve: o pesado ja vem com isso em hittingCharge)
        Posture.add(v, e.getNewDamage() * mult * (axe ? 1.3f : 1f) * (hitting == null ? com.bino.binosvanillaplus.leveling.PlayerStats.postureMult(p) : 1f));
        CombatMove cur = hitting != null ? hitting : RECENT.containsKey(p.getUUID()) ? RECENT.get(p.getUUID()).move() : null;
        if (cur != null && stabMove(cur, axe) && (hitting != null || recentLight(p) != null)) STAB.put(v.getUUID(), v.level().getGameTime());
        // malabarismo (estilo Kingdom Hearts / Devil May Cry): mob atingido NO AR, ou atingido pelo combo aereo, fica no
        // ar sem ser empurrado e SOBE um pouco a cada golpe, acompanhando a altura do jogador (o combo continua pegando)
        boolean airCombo = !p.onGround() && cur != null && (cur.aerial() || cur == CombatMove.LAUNCHER);
        if ((!v.onGround() || airCombo) && !v.isInWater() && !v.onClimbable() && !(v instanceof net.minecraft.world.entity.FlyingMob)) {
            v.setDeltaMovement(v.getDeltaMovement().x * 0.2, 0, v.getDeltaMovement().z * 0.2);
            v.hurtMarked = true;
            Posture.freeze(v, 9);
            JUGGLE.put(v.getUUID(), v.level().getGameTime() + 2);
            double ty = Math.max(v.getY() + 0.4, p.getY() + 0.3);
            HOVER.put(v.getUUID(), new Hover(v, v.level().getGameTime() + 9, Math.min(ty, v.getY() + 1.3)));
        }
    }

    // morte de mob por jogador: avisa o cliente qual animacao de morte tocar (normal ou "com stab") e de que lado veio
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        Long st = STAB.remove(v.getUUID());
        if (STAB.size() > 256) STAB.clear();
        if (v instanceof Player || v.level().isClientSide) return;
        var src = e.getSource();
        // tipo de morte: 0 normal (cai para tras do golpe), 1 com stab, 2 queimado, 3 explosao, 4 afogado/sufocado, 5 outras
        int kind;
        if (src.getEntity() instanceof Player) kind = st != null && v.level().getGameTime() - st <= 3 ? 1 : 0;
        else if (src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) kind = 3;
        else if (src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || src.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)) kind = 2;
        else if (src.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING) || src.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) kind = 4;
        else if (src.getEntity() != null || src.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) kind = 0;
        else kind = 5;
        // de que lado veio: quem bateu, ou o ponto da explosao/flecha; sem nada, cai para tras do proprio olhar
        Vec3 from = src.getSourcePosition() != null ? src.getSourcePosition() : src.getEntity() != null ? src.getEntity().position() : null;
        Vec3 d = from != null ? v.position().subtract(from) : v.getLookAngle().reverse();
        if (d.horizontalDistanceSqr() < 1e-4) d = v.getLookAngle().reverse();
        float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x));
        PacketDistributor.sendToPlayersTrackingEntity(v, new CombatFxPayload(CombatFxPayload.DEATH, v.getId(), kind, yaw));
    }

    // golpe do servidor: na hora do impacto, acerta quem estiver no arco a frente (ou em volta, nas pancadas no chao)
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Pending pd = PENDING.get(p.getUUID());
        if (pd == null || p.level().getGameTime() < pd.atTick()) return;
        PENDING.remove(p.getUUID());
        if (!p.isAlive() || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return;
        ServerLevel level = p.serverLevel();
        CombatMove mv = pd.move();
        boolean slam = mv == CombatMove.JUMP || mv == CombatMove.METEOR || (mv == CombatMove.JUDGMENT && pd.axe()) || (mv == CombatMove.TORMENTA && pd.axe());
        boolean around = slam || mv == CombatMove.DANCE || (mv == CombatMove.TORMENTA && !pd.axe());
        double reach = switch (mv) {
            case FORWARD -> 3.9; case ROLL_THRUST -> 4.0; case JUDGMENT -> 3.8; case GUARD_BREAK, LAUNCHER -> 3.0;
            case PIERCE -> 4.6; case METEOR, DANCE -> 3.4; case TORMENTA -> 3.6; case SURPRISE -> 4.0; default -> 3.2;
        };
        double cone = switch (mv) {
            case JUDGMENT -> -0.2; case ROLL_THRUST, FORWARD, SURPRISE -> 0.6; case PIERCE -> 0.85; case LAUNCHER -> 0.45; default -> 0.35;
        };
        float charge = CombatMove.chargeMult(pd.charge());
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * mv.damage(true, pd.axe()) * charge;   // Forca ja vem no atributo
        boolean launched = false;
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        boolean hitAny = false;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(reach + 1),
                t -> t != p && t.isAlive() && !t.isAlliedTo(p) && t.isPickable())) {
            Vec3 to = t.position().subtract(p.position());
            double d = to.horizontalDistance();
            if (d > reach + t.getBbWidth() * 0.5 || Math.abs(to.y) > 2.5) continue;
            if (!around && d > 0.6 && to.multiply(1, 0, 1).normalize().dot(look) < cone) continue;
            if (!p.hasLineOfSight(t)) continue;
            ownHit = true; hitting = mv; hittingCharge = charge * com.bino.binosvanillaplus.leveling.PlayerStats.postureMult(p);
            boolean ok = t.hurt(p.damageSources().playerAttack(p), dmg);
            ownHit = false; hitting = null; hittingCharge = 1f;
            if (ok) {
                hitAny = true;
                Vec3 out = to.multiply(1, 0, 1);
                Vec3 push = around && out.lengthSqr() > 1e-4 ? out.normalize() : look;
                if (mv == CombatMove.LAUNCHER) {   // Lanca-Ceu: joga o mob para cima (o jogador pula atras)
                    t.setDeltaMovement(push.x * 0.15, 0.78, push.z * 0.15);
                    t.hurtMarked = true;
                    launched = true;
                } else if (mv == CombatMove.GUARD_BREAK) {
                    if (pd.axe()) { t.setDeltaMovement(push.x * 0.1, 0.7, push.z * 0.1); t.hurtMarked = true; }   // cabo no queixo: levanta
                    else Posture.freeze(t, 10);   // estocada com o ombro: atordoa (o resto do combo pega)
                } else if (mv == CombatMove.PIERCE && pd.axe()) {   // ombrada: derruba
                    t.knockback(1.2, -push.x, -push.z);
                    Posture.freeze(t, 12);
                } else t.knockback(pd.axe() ? 0.9 : 0.7, -push.x, -push.z);
                if (slam) t.setDeltaMovement(t.getDeltaMovement().add(0, 0.25, 0));
                Vec3 c = t.getBoundingBox().getCenter();
                level.sendParticles(ModParticles.IMPACT_STAR.get(), c.x, c.y, c.z, 0, 1.4, 0, 0, 1.0);
                // finalizador: anel da carga (fraco branco, medio dourado, forte vermelho) e estrelas pelo ponto do combo
                if (pd.charge() > 0)
                    level.sendParticles(ModParticles.IMPACT_RING.get(), c.x, c.y, c.z, 0, 0.9 + 0.35 * pd.charge(), pd.charge() == 1 ? 0 : pd.charge() == 2 ? 1 : 2, 0, 1.0);
                for (int i = 1; i < Math.min(5, pd.step()); i++) {
                    double ang = i * Math.PI * 2 / Math.max(1, pd.step() - 1);
                    level.sendParticles(ModParticles.IMPACT_STAR.get(), c.x + Math.cos(ang) * 0.6, c.y + 0.4, c.z + Math.sin(ang) * 0.6, 0, 0.6, 1, 0, 1.0);
                }
            }
        }
        if (launched) CombatFxPayload.stamina(p, 40);   // levantar o inimigo devolve estamina
        if (hitAny) p.getMainHandItem().hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        if (slam) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(p.blockPosition().below())),
                    p.getX(), p.getY() + 0.1, p.getZ(), 40, 1.2, 0.05, 1.2, 0.18);
            level.sendParticles(ModParticles.IMPACT_RING.get(), p.getX(), p.getY() + 0.15, p.getZ(), 0, 2.4, 0, 0, 1.0);
        }
        level.playSound(p, p.getX(), p.getEyeY(), p.getZ(), pd.axe() ? ModSounds.AXE_SWING.get() : ModSounds.SWORD_SWING.get(),
                SoundSource.PLAYERS, 1.0f, 0.75f);
        p.resetAttackStrengthTicker();
    }
}

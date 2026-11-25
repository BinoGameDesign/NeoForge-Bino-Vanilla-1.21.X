package com.bino.binoscombat.combat;

import com.bino.binoscombat.BinosCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// IA de combate dos mobs corpo a corpo do jogo (zumbi, aranha, vindicador, enderman...) contra JOGADORES, com ideias de
// jogos de acao:
//  - FICHAS DE ATAQUE (Batman Arkham, Assassin's Creed, Doom 2016): no maximo 2 mobs atacam o mesmo jogador ao mesmo
//    tempo. Os outros nao ficam em fila colados: rodeiam a uns 4 blocos, de frente para o jogador, esperando a vez.
//  - RECUO DEPOIS DO GOLPE (Elden Ring, Dark Souls): quem acabou de atacar da uns passos para tras e anda de lado
//    um tempo (0,8 a 1,6 s) antes de voltar, em vez de grudar e bater sem parar. Abre espaco para o contra-ataque.
//  - CERCO: cada mob rodeando escolhe um lado e um angulo proprio, entao o grupo se espalha em volta (flanqueia),
//    troca de direcao de vez em quando e se bater numa parede.
//  - SEQUENCIA (Elden Ring): mobs fortes as vezes emendam um 2o golpe rapido logo depois do 1o (cada um com aviso).
//  - ESQUIVA (leitura do comando, como os chefes de Souls): quando o jogador comeca um golpe perto, mobs ageis
//    (aranha, rato, vindicador, esqueleto...) as vezes pulam de lado/para tras. Mobs grandes nao esquivam.
//  - QUEM ATACA DE LONGE (esqueleto, pillager, bruxa, mago villager, blaze...): com o jogador colado, da um pulo para
//    tras para recuperar distancia; flechas e virotes MIRAM ADIANTE (onde o jogador vai estar, pela velocidade dele).
//  - ALERTA DO GRUPO: bater num mob chama os da mesma especie que estao por perto (ate 10 blocos) e sem alvo.
// Vale para os mobs do jogo e os do mod (rato gigante, guarda/golem/bruxa/mago villager...), sempre contra jogador
// em sobrevivencia. O golpe em si continua o do jogo, com o aviso do parry (MeleeAttackGoalTelegraphMixin pede a ficha).
@EventBusSubscriber(modid = BinosCombat.MOD_ID)
public final class CombatAI {
    private CombatAI() {}

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final int MAX_ATTACKERS = 2;        // mobs atacando o mesmo jogador ao mesmo tempo
    public static final double CIRCLE_RADIUS = 4.2;   // distancia de quem espera a vez
    public static final double ENGAGE = 8.0;          // a partir de que distancia do jogador comeca a rodear

    // jogador -> (mob -> tick em que a ficha vence)
    private static final Map<UUID, Map<Integer, Long>> TOKENS = new HashMap<>();
    // mob -> tick ate quando esta recuando depois de atacar
    private static final Map<Integer, Long> RECOVER = new HashMap<>();
    // mob -> tick ate quando fica rodeando ("estudando" o jogador) antes de entrar para o golpe
    private static final Map<Integer, Long> STALK = new HashMap<>();

    private static Map<Integer, Long> tokens(ServerPlayer p) {
        Map<Integer, Long> m = TOKENS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        long now = p.level().getGameTime();
        for (Iterator<Map.Entry<Integer, Long>> it = m.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> en = it.next();
            if (en.getValue() < now || !(p.level().getEntity(en.getKey()) instanceof Mob mob) || !mob.isAlive() || mob.getTarget() != p)
                it.remove();
        }
        return m;
    }

    // tem ficha livre para este mob? (ou ele ja tem uma)
    public static boolean free(ServerPlayer p, Mob mob) {
        Map<Integer, Long> m = tokens(p);
        return m.containsKey(mob.getId()) || m.size() < MAX_ATTACKERS;
    }

    // pega a ficha para preparar um golpe (vale ate o golpe + recuo; no maximo 3 s)
    public static boolean take(ServerPlayer p, Mob mob, int windupTicks) {
        if (!free(p, mob)) return false;
        tokens(p).put(mob.getId(), p.level().getGameTime() + Math.min(60, windupTicks + 8));
        return true;
    }

    // golpe terminou (acertou, foi aparado ou errou): recua um tempo e devolve a ficha no fim do recuo
    public static void attacked(Mob mob, ServerPlayer p) {
        long now = mob.level().getGameTime();
        long until = now + 16 + mob.getRandom().nextInt(17);
        RECOVER.put(mob.getId(), until);
        STALK.remove(mob.getId());   // depois do recuo, rodeia de novo antes do proximo golpe
        tokens(p).computeIfPresent(mob.getId(), (k, v) -> Math.min(v, until));
        if (RECOVER.size() > 512) RECOVER.values().removeIf(t -> t < now);
    }

    static boolean recovering(Mob mob) {
        Long t = RECOVER.get(mob.getId());
        return t != null && t >= mob.level().getGameTime();
    }

    static boolean fighter(net.minecraft.world.entity.Entity e) {
        return e instanceof Mob && (e instanceof Enemy || com.bino.binoscombat.CombatHooks.extraFighter(e))
                && !(e instanceof net.minecraft.world.entity.NeutralMob);
    }

    // ---------------------------------------------------------------- sequencia de golpes
    // depois de um golpe que entrou: emenda outro? (mobs fortes: nivel 12+, 40+ de vida, ou de luta: vindicador, devastador...)
    public static boolean chain(Mob mob, int done) {
        if (done >= 1) return false;   // no maximo 2 golpes seguidos
        boolean strong = com.bino.binoscombat.CombatHooks.mobLevel(mob) >= 12 || mob.getMaxHealth() >= 40
                || mob instanceof net.minecraft.world.entity.monster.Vindicator || mob instanceof net.minecraft.world.entity.monster.Ravager
                || mob instanceof net.minecraft.world.entity.animal.IronGolem;
        return strong && mob.getRandom().nextFloat() < 0.4f;
    }

    // ---------------------------------------------------------------- esquiva
    private record Dodge(Mob mob, ServerPlayer from, long at) {}
    private static final java.util.List<Dodge> DODGES = new java.util.ArrayList<>();
    private static final Map<Integer, Long> DODGE_READY = new HashMap<>();

    // o jogador comecou um golpe (CombatMovesServer.onMove)
    public static void playerAttack(ServerPlayer p, boolean heavy) {
        if (!Telegraphs.eligible(p)) return;
        long now = p.level().getGameTime();
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(4.5), m -> fighter(m) && m.getTarget() == p)) {
            float agile = agility(m);
            if (agile <= 0 || !m.onGround() || Posture.busy(m) || now < DODGE_READY.getOrDefault(m.getId(), 0L)) continue;
            if (tokens(p).containsKey(m.getId()) && !recovering(m)) continue;   // ja comprometido com o proprio golpe
            if (m.getRandom().nextFloat() >= (heavy ? 0.35f : 0.15f) * agile) continue;
            DODGE_READY.put(m.getId(), now + 60);
            DODGES.add(new Dodge(m, p, now + 2 + m.getRandom().nextInt(3)));   // tempo de reacao
        }
    }

    // mobs grandes/pesados nao esquivam; os ageis esquivam mais
    private static float agility(Mob m) {
        if (m.getBbWidth() > 1.2f || m instanceof net.minecraft.world.entity.animal.IronGolem) return 0f;
        if (m instanceof net.minecraft.world.entity.monster.Spider || com.bino.binoscombat.CombatHooks.agileMob(m)
                || m instanceof net.minecraft.world.entity.monster.AbstractSkeleton || m instanceof net.minecraft.world.entity.monster.Vindicator
                || m instanceof net.minecraft.world.entity.monster.Pillager) return 1.4f;
        if (m instanceof net.minecraft.world.entity.monster.Zombie) return 0.6f;
        return 1f;
    }

    private static void leap(Mob m, Vec3 dir, double h, double up) {
        m.setDeltaMovement(dir.x * h, up, dir.z * h);
        m.hurtMarked = true;
        m.getNavigation().stop();
    }

    // ---------------------------------------------------------------- longe: recuo e mira adiante
    private static final Map<Integer, Long> HOP_READY = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>(), VEL = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        // velocidade dos jogadores (o servidor nao tem a do cliente): media da mudanca de posicao por tick
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            Vec3 last = LAST_POS.put(p.getUUID(), p.position());
            if (last == null || last.distanceToSqr(p.position()) > 16) { VEL.put(p.getUUID(), Vec3.ZERO); continue; }
            Vec3 v = p.position().subtract(last);
            VEL.put(p.getUUID(), VEL.getOrDefault(p.getUUID(), Vec3.ZERO).scale(0.6).add(v.scale(0.4)));
        }
        if (!DODGES.isEmpty()) {
            for (Iterator<Dodge> it = DODGES.iterator(); it.hasNext(); ) {
                Dodge d = it.next();
                Mob m = d.mob();
                if (!m.isAlive() || m.isRemoved()) { it.remove(); continue; }
                if (m.level().getGameTime() < d.at()) continue;
                it.remove();
                if (!m.onGround() || Posture.busy(m)) continue;
                Vec3 away = m.position().subtract(d.from().position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 1e-4) continue;
                away = away.normalize();
                Vec3 side = new Vec3(-away.z, 0, away.x).scale(m.getRandom().nextBoolean() ? 1 : -1);
                leap(m, side.scale(0.8).add(away.scale(0.6)).normalize(), 0.55, 0.28);
            }
        }
        if (e.getServer().getTickCount() % 600 == 0) {
            long now = e.getServer().overworld().getGameTime();
            DODGE_READY.values().removeIf(t -> t < now - 1200);
            HOP_READY.values().removeIf(t -> t < now - 1200);
            if (RECOVER.size() > 64) RECOVER.values().removeIf(t -> t < now);
            if (STALK.size() > 64) STALK.values().removeIf(t -> t < now - 600);
        }
    }

    // quem ataca de longe, com o jogador colado: pulo para tras (no maximo a cada 2,5 s)
    @SubscribeEvent
    public static void onMobTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide || (m.tickCount + m.getId()) % 10 != 0 || !fighter(m)) return;
        if (!(m instanceof net.minecraft.world.entity.monster.RangedAttackMob) && !(m instanceof net.minecraft.world.entity.monster.SpellcasterIllager)) return;
        if (!(m.getTarget() instanceof ServerPlayer p) || !Telegraphs.eligible(p) || !m.onGround() || Posture.busy(m) || m.isPassenger()) return;
        long now = m.level().getGameTime();
        if (now < HOP_READY.getOrDefault(m.getId(), 0L) || m.distanceToSqr(p) > 3.6 * 3.6 || m.getRandom().nextFloat() > 0.45f) return;
        Vec3 away = m.position().subtract(p.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) return;
        away = away.normalize();
        // nao pula para dentro de um buraco/lava: confere o chao uns 2,5 blocos atras
        net.minecraft.core.BlockPos land = net.minecraft.core.BlockPos.containing(m.position().add(away.scale(2.5)));
        boolean ground = false;
        for (int dy = 0; dy >= -3 && !ground; dy--) ground = m.level().getBlockState(land.offset(0, dy - 1, 0)).isFaceSturdy(m.level(), land.offset(0, dy - 1, 0), net.minecraft.core.Direction.UP);
        if (!ground || !m.level().getFluidState(land).isEmpty() || !m.level().getFluidState(land.below()).isEmpty()) return;
        HOP_READY.put(m.getId(), now + 50);
        leap(m, away, 0.62, 0.36);
    }

    // flecha/virote de um mob mirando um jogador: mira onde o jogador VAI estar (60% das vezes)
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow a)) return;
        if (!(a.getOwner() instanceof Mob m) || !fighter(m) || !(m.getTarget() instanceof ServerPlayer p) || !Telegraphs.eligible(p)) return;
        if (a.tickCount > 0 || m.getRandom().nextFloat() > 0.6f) return;
        Vec3 v = a.getDeltaMovement();
        double speed = v.length();
        if (speed < 0.5) return;
        Vec3 vel = VEL.getOrDefault(p.getUUID(), Vec3.ZERO);
        Vec3 aim = p.position().add(0, p.getBbHeight() * 0.333, 0);
        double dist = aim.distanceTo(a.position());
        if (dist > 40) return;
        double t = dist / speed;
        Vec3 pred = aim.add(vel.x * t * 0.9, 0, vel.z * t * 0.9);
        double dx = pred.x - a.getX(), dz = pred.z - a.getZ(), dy = pred.y - a.getY();
        double h = Math.sqrt(dx * dx + dz * dz);
        float inacc = (float) Math.max(1, 14 - m.level().getDifficulty().getId() * 4);
        a.shoot(dx, dy + h * 0.2, dz, (float) speed, inacc);
    }

    // ---------------------------------------------------------------- alerta do grupo
    @SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof Mob v) || !fighter(v) || !(e.getSource().getEntity() instanceof ServerPlayer p) || !Telegraphs.eligible(p)) return;
        for (Mob o : v.level().getEntitiesOfClass(Mob.class, v.getBoundingBox().inflate(10),
                o -> o != v && o.getType() == v.getType() && o.getTarget() == null && o.isAlive() && !o.isNoAi())) {
            if (o.hasLineOfSight(v) || o.hasLineOfSight(p)) o.setTarget(p);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof PathfinderMob mob) || !fighter(mob)) return;
        boolean melee = false;
        for (WrappedGoal g : mob.goalSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof CircleGoal) return;   // ja tem
            if (g.getGoal() instanceof MeleeAttackGoal) melee = true;
        }
        if (melee) mob.goalSelector.addGoal(1, new CircleGoal(mob));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        TOKENS.remove(e.getEntity().getUUID()); LAST_POS.remove(e.getEntity().getUUID()); VEL.remove(e.getEntity().getUUID());
    }

    // rodeia o jogador: antes de cada golpe (estudando, 0,75 a 2,3 s; as vezes entra direto), esperando a vez (sem ficha)
    // ou recuando depois de atacar. Anda em volta do jogador pelo caminho do jogo (nao cai de barranco), olhando para ele.
    static final class CircleGoal extends Goal {
        private final PathfinderMob mob;
        private float side;
        private int switchIn, time;

        CircleGoal(PathfinderMob mob) {
            this.mob = mob;
            this.side = mob.getRandom().nextBoolean() ? 1f : -1f;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private ServerPlayer target() {
            return mob.getTarget() instanceof ServerPlayer p && Telegraphs.eligible(p) && p.isAlive() ? p : null;
        }

        private boolean wanted() {
            ServerPlayer p = target();
            if (p == null || Posture.busy(mob) || mob.isPassenger() || mob.isInWater()) return false;
            if (mob.distanceToSqr(p) > ENGAGE * ENGAGE || !mob.hasLineOfSight(p)) return false;
            if (recovering(mob)) return true;
            if (tokens(p).containsKey(mob.getId())) return false;   // ja esta no golpe
            if (!free(p, mob)) return true;                          // sem ficha: espera a vez
            long now = mob.level().getGameTime();
            Long st = STALK.get(mob.getId());
            if (st == null) {   // chegou perto: estuda um pouco antes de entrar (25% entra direto)
                st = mob.getRandom().nextFloat() < 0.25f ? now : now + 15 + mob.getRandom().nextInt(31);
                STALK.put(mob.getId(), st);
            }
            return now < st;
        }

        @Override public boolean canUse() { return wanted(); }
        @Override public boolean canContinueToUse() { return wanted(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            time = 0;
            switchIn = 30 + mob.getRandom().nextInt(40);
        }

        @Override public void stop() { mob.getNavigation().stop(); }

        @Override public void tick() {
            ServerPlayer p = target();
            if (p == null) return;
            mob.getLookControl().setLookAt(p, 30f, 30f);
            if (--switchIn <= 0) { side = -side; switchIn = 30 + mob.getRandom().nextInt(50); }
            if (time++ % 5 != 0 && !mob.getNavigation().isDone()) return;
            boolean back = recovering(mob);
            Vec3 rel = mob.position().subtract(p.position()).multiply(1, 0, 1);
            if (rel.lengthSqr() < 1e-4) rel = new Vec3(1, 0, 0);
            double ang = Math.atan2(rel.z, rel.x);
            // espalha: anda para o lado contrario do aliado mais perto (o grupo se distribui em volta)
            Mob ally = nearestAlly(p);
            if (ally != null) {
                Vec3 ar = ally.position().subtract(p.position());
                double diff = Mth.wrapDegrees(Math.toDegrees(ang - Math.atan2(ar.z, ar.x)));
                if (Math.abs(diff) < 60) side = diff >= 0 ? 1f : -1f;
            }
            double r = back ? 3.4 : CIRCLE_RADIUS;
            Vec3 to;
            if (back && time < 10) to = mob.position().add(rel.normalize().scale(2.5));   // passos para tras logo depois do golpe
            else {
                double a = ang + side * 0.55;
                to = p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            }
            if (!mob.getNavigation().moveTo(to.x, p.getY(), to.z, back ? 1.0 : 0.85)) side = -side;   // sem caminho: outro lado
        }

        private Mob nearestAlly(ServerPlayer p) {
            Mob near = null;
            double best = 4.0 * 4.0;
            for (Mob o : mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(4.0), o -> o != mob && o.getTarget() == p)) {
                double d2 = o.distanceToSqr(mob);
                if (d2 < best) { best = d2; near = o; }
            }
            return near;
        }
    }
}

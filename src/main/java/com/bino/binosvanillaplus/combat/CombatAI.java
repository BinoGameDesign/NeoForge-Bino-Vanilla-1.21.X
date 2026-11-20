package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
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
// O golpe em si continua o do jogo, com o aviso do parry (MeleeAttackGoalTelegraphMixin pede a ficha antes de preparar).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
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
        tokens(p).computeIfPresent(mob.getId(), (k, v) -> Math.min(v, until));
        if (RECOVER.size() > 512) RECOVER.values().removeIf(t -> t < now);
    }

    static boolean recovering(Mob mob) {
        Long t = RECOVER.get(mob.getId());
        return t != null && t >= mob.level().getGameTime();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof PathfinderMob mob) || !(mob instanceof Enemy)) return;
        boolean melee = false;
        for (WrappedGoal g : mob.goalSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof CircleGoal) return;   // ja tem
            if (g.getGoal() instanceof MeleeAttackGoal) melee = true;
        }
        if (melee) mob.goalSelector.addGoal(1, new CircleGoal(mob));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { TOKENS.remove(e.getEntity().getUUID()); }

    // rodeia o jogador (esperando a vez) ou recua andando de lado (depois de atacar)
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
            double d2 = mob.distanceToSqr(p);
            if (d2 > ENGAGE * ENGAGE) return false;
            if (recovering(mob)) return true;
            return !free(p, mob);   // sem ficha: espera a vez rodeando
        }

        @Override public boolean canUse() { return wanted(); }
        @Override public boolean canContinueToUse() { return wanted(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            time = 0;
            switchIn = 30 + mob.getRandom().nextInt(40);
            mob.getNavigation().stop();
        }

        @Override public void stop() {
            mob.getMoveControl().strafe(0, 0);
            mob.setXxa(0); mob.setZza(0);
        }

        @Override public void tick() {
            ServerPlayer p = target();
            if (p == null) return;
            time++;
            mob.getLookControl().setLookAt(p, 30f, 30f);
            mob.lookAt(p, 30f, 30f);
            double d = mob.distanceTo(p);
            boolean back = recovering(mob);
            double want = back ? 3.2 : CIRCLE_RADIUS;
            float fwd;
            if (back && time < 8) fwd = -0.7f;                // passos para tras logo depois do golpe
            else if (d < want - 0.6) fwd = -0.5f;
            else if (d > want + 0.9) fwd = 0.55f;
            else fwd = 0f;
            if (--switchIn <= 0 || mob.horizontalCollision) {   // troca de lado de vez em quando ou ao bater em algo
                side = -side;
                switchIn = 30 + mob.getRandom().nextInt(50);
            }
            // os que esperam se espalham: quem esta perto de outro mob anda para longe dele
            float s = side * (back ? 0.45f : 0.5f);
            Vec3 away = spread(p);
            if (away != null) s += (float) Mth.clamp(away.dot(right(p)), -0.4, 0.4);
            mob.getMoveControl().strafe(fwd, Mth.clamp(s, -0.7f, 0.7f));
        }

        // direita do mob olhando para o jogador
        private Vec3 right(ServerPlayer p) {
            Vec3 to = p.position().subtract(mob.position()).multiply(1, 0, 1);
            if (to.lengthSqr() < 1e-4) return Vec3.ZERO;
            to = to.normalize();
            return new Vec3(-to.z, 0, to.x);
        }

        // empurrao para longe do mob aliado mais perto (a menos de 2,5 blocos)
        private Vec3 spread(ServerPlayer p) {
            Mob near = null;
            double best = 2.5 * 2.5;
            for (Mob o : mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(2.5), o -> o != mob && o.getTarget() == p)) {
                double d2 = o.distanceToSqr(mob);
                if (d2 < best) { best = d2; near = o; }
            }
            if (near == null) return null;
            Vec3 a = mob.position().subtract(near.position()).multiply(1, 0, 1);
            return a.lengthSqr() < 1e-4 ? null : a.normalize();
        }
    }
}

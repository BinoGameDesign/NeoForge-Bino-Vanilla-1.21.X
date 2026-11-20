package com.bino.binosvanillaplus.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// Ajudas de "furtividade" para a IA dos mobs do mod: achar um lugar ESCURO e FORA DA VISTA de alguem, para fugir,
// se esconder ou vigiar de longe. Tudo por amostragem (sorteia varios pontos alcancaveis e da nota para cada um),
// leve o bastante para rodar de vez em quando no servidor.
public final class StealthAI {
    private StealthAI() {}

    // luz do lugar (bloco e ceu; de dia ao ar livre conta como claro)
    public static int light(Level level, BlockPos p) {
        return Math.max(level.getBrightness(LightLayer.BLOCK, p), level.getBrightness(LightLayer.SKY, p) - level.getSkyDarken());
    }

    // alguem com olhos em 'eye' enxerga o ponto? (so blocos atrapalham)
    public static boolean canSee(Level level, Vec3 eye, Vec3 point, Entity viewer) {
        HitResult hit = level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(point) < 0.25;
    }

    // o 'watcher' esta olhando na direcao do mob (cone de ~70 graus) e com linha de visao?
    public static boolean isWatchedBy(LivingEntity mob, LivingEntity watcher) {
        if (mob.isInvisible()) return false;
        Vec3 to = mob.getEyePosition().subtract(watcher.getEyePosition());
        if (to.lengthSqr() < 1.0e-4) return true;
        return watcher.getViewVector(1.0f).normalize().dot(to.normalize()) > 0.34 && watcher.hasLineOfSight(mob);
    }

    // Lugar para FUGIR de 'from': longe, escuro e, se der, fora da vista de quem assusta.
    @Nullable
    public static Vec3 fleeSpot(PathfinderMob mob, Vec3 from, @Nullable LivingEntity threat, int range) {
        Vec3 best = null;
        double bestScore = -1e9;
        for (int i = 0; i < 10; i++) {
            Vec3 v = DefaultRandomPos.getPosAway(mob, range, 7, from);
            if (v == null) continue;
            BlockPos bp = BlockPos.containing(v);
            double score = Math.sqrt(v.distanceToSqr(from)) * 1.0 - light(mob.level(), bp) * 1.6;
            if (threat != null && !canSee(mob.level(), threat.getEyePosition(), v.add(0, 0.5, 0), threat)) score += 6;
            if (score > bestScore) { bestScore = score; best = v; }
        }
        return best;
    }

    // Esconderijo para VIGIAR 'watched': a uma distancia boa dele (min..max), no escuro e de preferencia fora da vista
    // dele, perto de onde o mob ja esta (para nao atravessar o mapa).
    @Nullable
    public static Vec3 watchSpot(PathfinderMob mob, LivingEntity watched, double min, double max) {
        Vec3 best = null;
        double bestScore = -1e9;
        Vec3 eye = watched.getEyePosition();
        for (int i = 0; i < 14; i++) {
            Vec3 v = LandRandomPos.getPos(mob, 12, 5);
            if (v == null) continue;
            double d = Math.sqrt(v.distanceToSqr(watched.position()));
            if (d < min || d > max + 4) continue;
            BlockPos bp = BlockPos.containing(v);
            double score = -light(mob.level(), bp) * 2.0 - Math.abs(d - (min + max) / 2) * 0.6 - Math.sqrt(v.distanceToSqr(mob.position())) * 0.2;
            if (!canSee(mob.level(), eye, v.add(0, 0.6, 0), watched)) score += 7;   // escondido
            if (score > bestScore) { bestScore = score; best = v; }
        }
        return best;
    }

    // Um passo para o lado em volta do alvo (andar em circulo, mantendo distancia)
    @Nullable
    public static Vec3 circleStep(PathfinderMob mob, LivingEntity target, double dist, int dir) {
        Vec3 c = target.position();
        Vec3 off = mob.position().subtract(c);
        double ang = Math.atan2(off.z, off.x) + dir * 0.6;
        Vec3 p = c.add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
        BlockPos bp = BlockPos.containing(p);
        // ajusta a altura para o chao mais proximo
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos q = bp.above(dy);
            if (mob.level().getBlockState(q.below()).isSolidRender(mob.level(), q.below()) && mob.level().getBlockState(q).isAir())
                return Vec3.atBottomCenterOf(q);
        }
        return null;
    }
}

package com.bino.binosvanillaplus.gear;

import com.bino.binosvanillaplus.network.WhistleSignalPayload;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

// Apito de explorador: som de longo alcance (maior em caverna). Quem estiver no alcance ve por alguns segundos a
// silhueta branca de quem apitou (mesmo atras de paredes). Criaturas hostis vao investigar a origem; as passivas fogem;
// o Warden fica com mais raiva e mais forte.
public final class Whistle {
    private Whistle() {}

    public static final double RANGE = 96, CAVE_RANGE = 160;   // alcance do som (blocos)
    private static final double MOB_RANGE = 28, CAVE_MOB_RANGE = 40;

    public static boolean inCave(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockPos b = p.blockPosition();
        if (l.dimensionType().hasCeiling()) return true;
        return !l.canSeeSky(b.above()) && b.getY() < l.getHeight(Heightmap.Types.WORLD_SURFACE, b.getX(), b.getZ()) - 6;
    }

    public static void blow(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        boolean cave = inCave(p);
        double range = cave ? CAVE_RANGE : RANGE;
        level.playSound(null, p.getX(), p.getEyeY(), p.getZ(), ModSounds.WHISTLE.get(), SoundSource.PLAYERS,
                (float) (range / 16.0), 0.95f + level.getRandom().nextFloat() * 0.1f);
        level.gameEvent(p, GameEvent.INSTRUMENT_PLAY, p.position());   // sensores de sculk e o Warden "ouvem"

        // silhueta para os outros jogadores no alcance
        WhistleSignalPayload sig = new WhistleSignalPayload(p.getId(), p.getX(), p.getY(), p.getZ());
        for (ServerPlayer o : level.players()) {
            if (o != p && o.distanceToSqr(p) <= range * range) PacketDistributor.sendToPlayer(o, sig);
        }

        double mr = cave ? CAVE_MOB_RANGE : MOB_RANGE;
        Vec3 src = p.position();
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(mr))) {
            if (!m.isAlive() || m.isNoAi()) continue;
            if (m instanceof Warden w) {
                w.increaseAngerAt(p, 120, true);
                w.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 1));
                w.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 0));
                continue;
            }
            if (m instanceof Enemy) {
                // perto: ja vai atras do jogador; longe: vai investigar de onde veio o som
                if (m.distanceToSqr(p) < 16 * 16) m.setTarget(p);
                m.getNavigation().moveTo(src.x, src.y, src.z, 1.1);
            } else if (m instanceof PathfinderMob pm && !(m instanceof TamableAnimal t && t.isTame())) {
                Vec3 away = DefaultRandomPos.getPosAway(pm, 16, 7, src);
                if (away != null) pm.getNavigation().moveTo(away.x, away.y, away.z, 1.6);   // assusta e foge
            }
        }
    }
}

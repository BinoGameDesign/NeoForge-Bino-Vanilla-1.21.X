package com.bino.binoscombat.client;

import com.bino.binoscombat.BinosCombat;
import com.bino.binoscombat.CombatParticles;
import com.bino.binoscombat.client.combat.CombatMoves;
import com.bino.binoscombat.client.combat.Crosshair;
import com.bino.binoscombat.client.combat.DeathPuffParticle;
import com.bino.binoscombat.client.combat.DodgeRoll;
import com.bino.binoscombat.client.combat.HitSparkParticle;
import com.bino.binoscombat.client.combat.ImpactParticle;
import com.bino.binosmovement.client.MovementHooks;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

// Inicio do Combat no cliente: particulas e a ligacao com o motor de animacao/camera do Movement
@EventBusSubscriber(modid = BinosCombat.MOD_ID, value = Dist.CLIENT)
public final class CombatClient {
    private CombatClient() {}

    @SubscribeEvent
    public static void onSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // pose dos golpes e do rolamento por cima da vida no corpo (nessa ordem: o rolamento manda por ultimo)
            MovementHooks.POSERS.add(CombatMoves::posePlayer);
            MovementHooks.POSERS.add(DodgeRoll::posePlayer);
            MovementHooks.stanceFn = CombatMoves::stance;
            MovementHooks.bodyBusyFn = p -> CombatMoves.moving(p) || DodgeRoll.rolling(p);
            // camera: golpes do Blockbench levam a camera junto (antes das animacoes do dia a dia do Movement)
            MovementHooks.BODY_SOURCES.add(0, CombatMoves::bodyFrame);
            MovementHooks.CINEMA.add(CombatMoves::cinemaCam);
            MovementHooks.NUDGES.add((p, pt) -> new float[]{DodgeRoll.cameraDrop(p) + CombatMoves.cameraDrop(p), CombatMoves.cameraForward(p)});
            MovementHooks.crosshair = (g, x, y) -> Crosshair.draw(g, x, y, true);
        });
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CombatParticles.HIT_SPARK.get(), HitSparkParticle.Provider::new);
        event.registerSpriteSet(CombatParticles.PARRY_SPARK.get(), HitSparkParticle.GoldProvider::new);
        event.registerSpriteSet(CombatParticles.IMPACT_STAR.get(), ImpactParticle.StarProvider::new);
        event.registerSpriteSet(CombatParticles.IMPACT_RING.get(), ImpactParticle.RingProvider::new);
        event.registerSpriteSet(CombatParticles.DEATH_PUFF.get(), DeathPuffParticle.Provider::new);
        // tira os coracoes escuros que o jogo solta quando voce bate (DAMAGE_INDICATOR); ficam so as faiscas
        event.registerSpecial(ParticleTypes.DAMAGE_INDICATOR, (type, level, x, y, z, vx, vy, vz) -> null);
    }
}

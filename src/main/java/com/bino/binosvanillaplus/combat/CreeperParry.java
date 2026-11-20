package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.mixin.CreeperAccessor;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// Aparar a EXPLOSAO do creeper (mod Combat). Quando o creeper comeca a inchar em voce aparece o brilho do parry,
// com a hora certa = a hora em que ele explodiria. Na hora:
//  - branco: voce segura a explosao com a arma (ela acontece, mas voce leva so 30% do dano);
//  - amarelo: desarma o creeper (o pavio volta a zero), ele e' empurrado e fica atordoado 1,5 s;
//  - azul: rebate o creeper para longe e ele explode LA (longe de voce, pegando quem estiver perto dele).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class CreeperParry {
    private CreeperParry() {}

    private static final Map<Creeper, Integer> LAST_DIR = new WeakHashMap<>();
    private static final Map<Creeper, Boolean> RETURNED = new WeakHashMap<>();
    private static final Map<UUID, Long> BRACED = new HashMap<>();

    // comecou a inchar em um jogador: aviso com o tempo que falta para explodir
    @SubscribeEvent
    public static void onTickPost(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof Creeper c) || c.level().isClientSide || !c.isAlive()) return;
        int dir = c.getSwellDir();
        Integer last = LAST_DIR.put(c, dir);
        if (dir <= 0 || last != null && last > 0 || RETURNED.containsKey(c)) return;
        if (!(c.getTarget() instanceof ServerPlayer p) || !Telegraphs.eligible(p)) return;
        CreeperAccessor a = (CreeperAccessor) c;
        int left = Math.max(1, a.binosvanillaplus$getMaxSwell() - a.binosvanillaplus$getSwell());
        Telegraphs.startCustom(c, p, left);
    }

    // vai explodir neste tick: o jogador aparou?
    @SubscribeEvent
    public static void onTickPre(EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof Creeper c) || c.level().isClientSide || !c.isAlive() || Posture.busy(c)) return;
        CreeperAccessor a = (CreeperAccessor) c;
        int dir = c.getSwellDir();
        if (dir <= 0 || RETURNED.containsKey(c) || a.binosvanillaplus$getSwell() + dir < a.binosvanillaplus$getMaxSwell()) return;
        if (c.getTarget() != null) Telegraphs.parriedCustom(c, c.getTarget());   // aparado -> CombatActionsServer.parried -> parried() abaixo
    }

    static void parried(ServerPlayer p, Creeper c, int zone) {
        ServerLevel level = p.serverLevel();
        CreeperAccessor a = (CreeperAccessor) c;
        Vec3 mid = p.getEyePosition().add(c.getEyePosition()).scale(0.5);
        Vec3 away = c.position().subtract(p.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? p.getLookAngle().multiply(1, 0, 1).normalize() : away.normalize();
        switch (zone) {
            case 2 -> {   // azul: rebate para longe e explode la
                RETURNED.put(c, true);
                a.binosvanillaplus$setSwell(Math.max(0, a.binosvanillaplus$getMaxSwell() - 15));
                c.ignite();   // pavio aceso de vez (nao apaga por estar longe do alvo)
                c.setDeltaMovement(away.x * 1.9, 0.6, away.z * 1.9);
                c.hurtMarked = true;
                level.sendParticles(ModParticles.PARRY_SPARK.get(), mid.x, mid.y, mid.z, 30, 0.05, 0.05, 0.05, 0.5);
                level.sendParticles(ModParticles.IMPACT_RING.get(), mid.x, mid.y, mid.z, 0, 1.4, 4, 0, 1.0);
                level.playSound(null, mid.x, mid.y, mid.z, ModSounds.PARRY_PERFECT.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            }
            case 1 -> {   // amarelo: desarma e atordoa
                a.binosvanillaplus$setSwell(0);
                c.setSwellDir(-1);
                c.knockback(1.0, -away.x, -away.z);
                Posture.add(c, Posture.max(c) * 0.45f);
                Posture.freeze(c, 30);
                level.sendParticles(ModParticles.PARRY_SPARK.get(), mid.x, mid.y, mid.z, 20, 0.05, 0.05, 0.05, 0.45);
                level.sendParticles(ModParticles.IMPACT_STAR.get(), mid.x, mid.y, mid.z, 0, 1.1, 1, 0, 1.0);
                level.playSound(null, mid.x, mid.y, mid.z, ModSounds.PARRY.get(), SoundSource.PLAYERS, 1.2f, 1.05f);
            }
            default -> {   // branco: segura a explosao com a arma
                BRACED.put(p.getUUID(), level.getGameTime() + 2);
                level.sendParticles(ModParticles.HIT_SPARK.get(), mid.x, mid.y, mid.z, 10, 0.05, 0.05, 0.05, 0.35);
                level.playSound(null, mid.x, mid.y, mid.z, ModSounds.SWORD_HIT_HARD.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
            }
        }
        // 3 = efeito de "devolvido" no cliente (flash e zoom do perfeito, sem o Issen)
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new CombatFxPayload(CombatFxPayload.PARRY, p.getId(), c.getId(), zone == 2 ? 3f : zone));
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Long until = BRACED.get(p.getUUID());
        if (until == null) return;
        if (p.level().getGameTime() > until) { BRACED.remove(p.getUUID()); return; }
        if (e.getSource().is(DamageTypeTags.IS_EXPLOSION)) e.setAmount(e.getAmount() * 0.3f);
    }
}

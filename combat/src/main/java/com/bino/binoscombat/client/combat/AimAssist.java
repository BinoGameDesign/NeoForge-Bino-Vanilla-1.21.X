package com.bino.binoscombat.client.combat;

import com.bino.binoscombat.BinosCombat;
import com.bino.binosmovement.client.ShoulderCam;
import com.bino.binoscombat.combat.CombatEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

// Mira assistida de combate (mod Combat), como a "magnetizacao" dos jogos de acao no controle: com ESPADA ou MACHADO na
// mao, em primeira pessoa ou na camera do ombro, a visao e' puxada DEVAGAR para o monstro mais perto do centro da
// mira (ate 15 graus e 6,5 blocos, com visao livre). Puxa um pouco mais durante o golpe. Perto do centro nao mexe
// (zona morta). Enquanto VOCE mexe o mouse ela desliga (e fica desligada 0,45 s depois): o mouse manda.
@EventBusSubscriber(modid = BinosCombat.MOD_ID, value = Dist.CLIENT)
public final class AimAssist {
    private AimAssist() {}

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final double RANGE = 6.5;
    public static final float CONE = 15f, KEEP_CONE = 20f;   // graus para pegar / para continuar no mesmo alvo
    public static final float PULL = 1.0f, PULL_ATTACK = 2.4f;   // forca (por segundo)
    public static final float MAX_SPEED = 30f;               // graus por segundo, no maximo
    public static final float DEAD = 2.0f;                   // graus: zona morta no centro
    public static final long MOUSE_PAUSE = 450;              // ms sem puxar depois que voce mexe o mouse

    private static int target = -1;
    private static long lastMs, mouseAt = -100000;
    private static float setYaw = Float.NaN, setPitch;

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        long now = Util.getMillis();
        float dt = lastMs == 0 ? 0f : Math.min(0.05f, (now - lastMs) / 1000f);
        lastMs = now;
        if (p == null || mc.screen != null || mc.isPaused() || p.isSpectator() || p.isPassenger() || p.isFallFlying()
                || !(mc.options.getCameraType().isFirstPerson() || ShoulderCam.active())
                || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) { target = -1; setYaw = Float.NaN; return; }

        // voce mexeu o mouse desde o ultimo quadro? (o giro nao bate com o que a assistencia deixou)
        if (!Float.isNaN(setYaw) && (Math.abs(Mth.wrapDegrees(p.getYRot() - setYaw)) > 0.15f || Math.abs(p.getXRot() - setPitch) > 0.15f)) mouseAt = now;
        setYaw = p.getYRot(); setPitch = p.getXRot();
        if (now - mouseAt < MOUSE_PAUSE) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 eye = p.getEyePosition(pt);
        LivingEntity best = null;
        float bestAng = 999;
        if (p.level().getEntity(target) instanceof LivingEntity cur && valid(p, cur) && angle(p, eye, cur, pt) < KEEP_CONE) {
            best = cur; bestAng = angle(p, eye, cur, pt);
        } else {
            for (LivingEntity le : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE), le -> valid(p, le))) {
                float a = angle(p, eye, le, pt);
                if (a < CONE && a < bestAng) { best = le; bestAng = a; }
            }
        }
        if (best == null) { target = -1; setYaw = Float.NaN; return; }
        target = best.getId();

        Vec3 aim = aimPoint(best, pt).subtract(eye);
        float wantYaw = (float) (Mth.atan2(aim.z, aim.x) * Mth.RAD_TO_DEG) - 90f;
        float wantPitch = (float) -(Mth.atan2(aim.y, aim.horizontalDistance()) * Mth.RAD_TO_DEG);
        float dyaw = Mth.wrapDegrees(wantYaw - p.getYRot()), dpitch = wantPitch - p.getXRot();
        if (Math.abs(dyaw) < DEAD) dyaw = 0;
        if (Math.abs(dpitch) < DEAD * 3) dpitch = 0;   // na vertical puxa menos (so se estiver bem fora)
        if (dyaw == 0 && dpitch == 0) return;
        float k = 1f - (float) Math.exp(-(CombatMoves.moving(p) ? PULL_ATTACK : PULL) * dt);
        float sy = Mth.clamp(dyaw * k, -MAX_SPEED * dt, MAX_SPEED * dt), sp = Mth.clamp(dpitch * k * 0.4f, -MAX_SPEED * 0.4f * dt, MAX_SPEED * 0.4f * dt);
        p.turn(sy / 0.15, sp / 0.15);   // igual ao mouse (mexe tambem no "antes", sem tremer)
        setYaw = p.getYRot(); setPitch = p.getXRot();
    }

    private static boolean valid(LocalPlayer p, LivingEntity le) {
        return le != p && le.isAlive() && le.deathTime <= 0 && le instanceof Enemy && !le.isInvisible()
                && le.distanceToSqr(p) < RANGE * RANGE && p.hasLineOfSight(le);
    }

    private static Vec3 aimPoint(LivingEntity le, float pt) {
        return le.getPosition(pt).add(0, le.getBbHeight() * 0.6, 0);
    }

    private static float angle(LocalPlayer p, Vec3 eye, LivingEntity le, float pt) {
        Vec3 to = aimPoint(le, pt).subtract(eye).normalize();
        double dot = Mth.clamp(to.dot(p.getViewVector(pt)), -1, 1);
        return (float) Math.toDegrees(Math.acos(dot));
    }
}

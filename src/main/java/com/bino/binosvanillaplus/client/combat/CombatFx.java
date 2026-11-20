package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatFxPayload;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Efeitos e estado do combate no cliente (mod Combat):
//  - AVISO de ataque dos mobs (brilho dourado + "shing"); guarda a hora prevista do golpe para o parry.
//  - PARRY / ISSEN / ESQUIVA PERFEITA / RAJADA / CRITICO: flash, vinheta azul, barras de cinema, linha do corte,
//    janelas de contra-ataque (quanto tempo o clique ainda vale).
//  - POSTURA dos mobs: barrinha acima da cabeca (aparece de 50% para cima) e, quebrada, o mob ajoelha com estrelinhas.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class CombatFx {
    private CombatFx() {}

    record Incoming(int target, long hitAt, boolean red) {}
    record Window(int mobId, long until) {}
    record Post(float frac, boolean broken, long at) {}

    private static final Map<Integer, Incoming> INCOMING = new HashMap<>();
    private static final Map<Integer, Post> POSTURE = new HashMap<>();
    private static final Set<Integer> KNEELING = new HashSet<>();
    private static final Map<Integer, Long> GLINT2 = new HashMap<>();   // segundo brilho, logo antes do golpe
    // estado da morte (fisica simples no cliente: o corpo tomba com o impulso do golpe, quica e escorrega)
    static final class Death {
        final int kind; final float dx, dz;
        float ang, angV, slide, slideV, spin, spinV, y, vy, tumble, tumbleV;
        boolean landed; long last;
        Death(int kind, float dx, float dz, java.util.Random r) {
            this.kind = kind; this.dx = dx; this.dz = dz;
            this.spinV = (r.nextFloat() - 0.5f) * 70f;
            switch (kind) {
                case 0 -> { angV = 120f + r.nextFloat() * 60f; slideV = 2.4f; }
                case 1 -> { angV = 0f; slideV = 0f; spinV *= 0.3f; }
                case 3 -> { vy = 7f; slideV = 5f; tumbleV = 520f + r.nextFloat() * 200f; spinV *= 3f; }
                default -> { }
            }
        }
        int kind() { return kind; }
        float dx() { return dx; }
        float dz() { return dz; }
    }
    private static final Map<Integer, Death> DEATHS = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();
    private static Window issen, rush;
    private static long flashAt = -100000, dodgeAt = -100000, letterboxAt = -100000, slashAt = -100000, zoomAt = -100000;
    private static float flashPower;

    private static long now() { return System.currentTimeMillis(); }

    // ------------------------------------------------------------------ rede

    public static void receive(CombatFxPayload m) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (mc.level == null || me == null) return;
        long now = now();
        boolean mine = m.a() == me.getId();
        switch (m.kind()) {
            case CombatFxPayload.TELEGRAPH -> {
                boolean red = m.f() < 0;   // vermelho: nao da parry, so esquiva
                INCOMING.put(m.a(), new Incoming(m.b(), now + (long) Math.abs(m.f()), red));
                Entity mob = mc.level.getEntity(m.a());
                if (mob != null) {
                    Vec3 at = attackPoint(mc, mob);
                    mc.level.addParticle(ModParticles.IMPACT_STAR.get(), at.x, at.y, at.z, red ? 0.85 : 0.65, red ? 2 : 1, 0);   // 1 dourada, 2 vermelha
                    GLINT2.put(m.a(), now + Math.max(0, (long) Math.abs(m.f()) - 140));
                    if (m.b() == me.getId())
                        mc.level.playLocalSound(at.x, at.y, at.z, ModSounds.SWORD_SWING.get(), SoundSource.HOSTILE, red ? 0.6f : 0.35f, red ? 0.7f : 2.0f, false);
                }
            }
            case CombatFxPayload.PARRY -> {
                INCOMING.remove(m.b());
                if (!mine) return;
                boolean perfect = m.f() > 0.5f;
                HitFx.kick(perfect ? 1.2f : 0.7f);
                flashAt = now; flashPower = perfect ? 1f : 0.45f;
                if (perfect) {
                    issen = new Window(m.b(), now + 520);
                    zoomAt = now;
                    Stamina.give(25);
                }
            }
            case CombatFxPayload.ISSEN -> {
                if (!mine) return;
                issen = null;
                slashAt = now; flashAt = now; flashPower = 0.8f; zoomAt = now;
                HitFx.kick(1.6f);
            }
            case CombatFxPayload.PERFECT_DODGE -> {
                if (!mine) return;
                rush = new Window(m.b(), now + 1450);
                dodgeAt = now;
                me.level().playLocalSound(me.getX(), me.getEyeY(), me.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.4f, false);
            }
            case CombatFxPayload.RUSH -> { if (mine) { rush = null; HitFx.kick(0.9f); } }
            case CombatFxPayload.POSTURE -> {
                boolean broken = m.b() == 1;
                if (m.f() <= 0.001f && !broken) POSTURE.remove(m.a());
                else POSTURE.put(m.a(), new Post(m.f(), broken, now));
                if (broken) KNEELING.add(m.a()); else KNEELING.remove(m.a());
            }
            case CombatFxPayload.DEATH -> {
                KNEELING.remove(m.a()); POSTURE.remove(m.a()); INCOMING.remove(m.a());
                float r = m.f() * Mth.DEG_TO_RAD;
                DEATHS.put(m.a(), new Death(m.b(), Mth.cos(r), Mth.sin(r), RANDOM));
            }
            case CombatFxPayload.CRITICAL -> {
                POSTURE.remove(m.b()); KNEELING.remove(m.b());
                if (!mine) return;
                letterboxAt = now; zoomAt = now; flashAt = now; flashPower = 0.6f;
                HitFx.kick(1.8f);
            }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        INCOMING.clear(); POSTURE.clear(); KNEELING.clear(); GLINT2.clear(); DEATHS.clear(); issen = null; rush = null;
    }

    // a parte do mob que vai bater: a mao (mobs com bracos, como zumbi e esqueleto) ou a boca/garras (aranha, lobo...)
    static Vec3 attackPoint(Minecraft mc, Entity mob) {
        float yaw = (mob instanceof LivingEntity le ? le.yBodyRot : mob.getYRot()) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        float w = mob.getBbWidth(), h = mob.getBbHeight();
        boolean arms = mc.getEntityRenderDispatcher().getRenderer(mob) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> lr
                && lr.getModel() instanceof net.minecraft.client.model.ArmedModel;
        if (arms) return mob.position().add(0, h * 0.68, 0).add(fwd.scale(w * 0.5 + 0.42)).add(right.scale(w * 0.38));
        return mob.getEyePosition().add(fwd.scale(w * 0.5 + 0.12)).add(0, -0.12, 0);
    }

    // bracos e cabeca na morte (HitReact): 0..1 e o tipo; os bracos se debatem no comeco e amolecem no fim
    public static float deathLimbs(LivingEntity e) {
        Death d = DEATHS.get(e.getId());
        if (d == null || e.deathTime <= 0) return 0f;
        float t = Mth.clamp(e.deathTime / 40f, 0f, 1f);
        return t < 0.1f ? t / 0.1f : t < 0.55f ? 1f : Math.max(0f, 1f - (t - 0.55f) / 0.3f) * 0.6f + 0.25f;
    }

    public static int deathKind(LivingEntity e) { Death d = DEATHS.get(e.getId()); return d == null ? -1 : d.kind(); }

    // morte com animacao propria? (LivingEntityRendererDeathMixin tira a queda de lado do jogo)
    public static boolean customDeath(LivingEntity e) { return e.deathTime > 0 && DEATHS.containsKey(e.getId()); }

    // barras de cinema por um tempo (facada pelas costas)
    private static long letterboxUntil;
    static void letterbox(long ms) { letterboxUntil = now() + ms; letterboxAt = now(); }

    // ------------------------------------------------------------------ consultas do CombatMoves

    // golpe vindo em mim agora? (mob id, ou -1). perfeito = bem na hora
    static int parryTarget(LocalPlayer p, long now) { return parryTarget(p, now, 240); }

    // window = quanto antes do golpe ainda vale (escudo tem janela maior)
    static int parryTarget(LocalPlayer p, long now, long window) {
        int best = -1; long bestD = Long.MAX_VALUE;
        for (Map.Entry<Integer, Incoming> en : INCOMING.entrySet()) {
            Incoming in = en.getValue();
            if (in.target() != p.getId() || in.red()) continue;
            long d = in.hitAt() - now;
            if (d < -70 || d > window) continue;
            if (Math.abs(d) < bestD) { bestD = Math.abs(d); best = en.getKey(); }
        }
        return best;
    }

    static boolean perfect(int mobId, long now) {
        Incoming in = INCOMING.get(mobId);
        if (in == null) return false;
        long d = in.hitAt() - now;
        return d >= -70 && d <= 110;
    }

    static void consumeIncoming(int mobId) { INCOMING.remove(mobId); }

    static int issenTarget(long now) { return issen != null && now <= issen.until() ? issen.mobId() : -1; }
    static int rushTarget(long now) { return rush != null && now <= rush.until() ? rush.mobId() : -1; }
    static void consumeIssen() { issen = null; }
    static void consumeRush() { rush = null; }

    // mob de guarda quebrada perto e na frente (para o golpe critico)
    static LivingEntity brokenTarget(LocalPlayer p) {
        LivingEntity best = null; double bestD = 3.6 * 3.6;
        Vec3 look = p.getLookAngle();
        for (int id : KNEELING) {
            if (!(p.level().getEntity(id) instanceof LivingEntity le) || !le.isAlive()) continue;
            double d = le.distanceToSqr(p);
            Vec3 to = le.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
            if (d < bestD && to.dot(look) > 0.35) { best = le; bestD = d; }
        }
        return best;
    }

    // ------------------------------------------------------------------ mundo: estrelinhas, barra de postura, ajoelhar

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = now();
        INCOMING.values().removeIf(in -> now - in.hitAt() > 600);
        for (java.util.Iterator<Map.Entry<Integer, Long>> it = GLINT2.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> en = it.next();
            if (now < en.getValue()) continue;
            it.remove();
            Incoming in = INCOMING.get(en.getKey());
            Entity mob = mc.level.getEntity(en.getKey());
            if (in == null || mob == null) continue;
            Vec3 at = attackPoint(mc, mob);
            mc.level.addParticle(ModParticles.IMPACT_STAR.get(), at.x, at.y, at.z, in.red() ? 1.0 : 0.8, in.red() ? 2 : 1, 0);
        }
        DEATHS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        POSTURE.entrySet().removeIf(en -> !en.getValue().broken() && now - en.getValue().at() > 20000);
        if (mc.level.getGameTime() % 3 != 0) return;
        for (int id : KNEELING) {
            Entity ent = mc.level.getEntity(id);
            if (ent == null) continue;
            double ang = (now / 180.0) % (Math.PI * 2);
            double r = ent.getBbWidth() * 0.5 + 0.15;
            mc.level.addParticle(ModParticles.IMPACT_STAR.get(), ent.getX() + Math.cos(ang) * r, ent.getY() + ent.getBbHeight() + 0.25,
                    ent.getZ() + Math.sin(ang) * r, 0.35, 1, 0);
        }
    }

    private static float smooth(float a) { a = Mth.clamp(a, 0f, 1f); return a * a * (3 - 2 * a); }

    // queda com quiquezinho no fim: 0 em pe -> 90 deitado
    private static float fall(float a) {
        a = Mth.clamp(a, 0f, 1f);
        if (a < 0.8f) { float k = a / 0.8f; return 90f * k * k; }               // acelera caindo (gravidade)
        return 90f - 9f * Mth.sin(Mth.PI * (a - 0.8f) / 0.2f);                 // bate no chao e quica um pouco
    }

    // Morte (mod Combat): o mob fica 2 s (LivingEntityDeathMixin) e some com o "puf" e almas subindo.
    // O corpo gira em volta da BORDA de baixo do lado para onde cai (assim deita em cima do chao, sem atravessar).
    //  0 golpe/flecha/mob: fisica - o impulso do golpe tomba o corpo, ele acelera caindo, quica no chao e escorrega
    //  1 stab: treme espetado, a lamina sai (tranco), cai de joelhos e entao tomba para a frente (fisica)
    //  2 queimado: se debate, encolhe e desaba de lado
    //  3 explosao: arremessado girando no ar (fisica), cai e deita
    //  4 afogado/sufocado: amolece devagar e deita de lado
    //  5 outras: cambaleia e cai de lado
    private static void step(Death d, float h, float tSec) {
        long now = now();
        float dt = d.last == 0 ? 0f : Mth.clamp((now - d.last) / 1000f, 0f, 0.05f);
        d.last = now;
        d.spin += d.spinV * dt;
        d.spinV *= (float) Math.exp(-3 * dt);
        if (d.kind == 3 && !d.landed) {   // no ar
            d.vy -= 26f * dt;
            d.y += d.vy * dt;
            d.slide += d.slideV * dt;
            d.tumble += d.tumbleV * dt;
            if (d.y <= 0 && d.vy < 0) { d.y = 0; d.landed = true; d.ang = 80f; d.angV = 60f; d.slideV *= 0.4f; }
            return;
        }
        boolean fallNow = d.kind == 0 || d.kind == 3 || d.kind == 1 && tSec > 0.95f;
        if (d.kind == 1 && tSec > 0.95f && d.angV == 0f && d.ang < 1f) d.angV = 50f;
        if (!fallNow) return;
        if (d.ang < 90f) {
            d.angV += (700f * Mth.sin(d.ang * Mth.DEG_TO_RAD) + 40f) * dt;   // gravidade derruba mais rapido quanto mais inclinado
            d.ang += d.angV * dt;
            if (d.ang >= 90f) {   // bateu no chao: quica um pouco
                d.ang = 90f;
                d.angV = Math.abs(d.angV) > 60f ? -d.angV * 0.28f : 0f;
                d.slideV *= 0.5f;
            }
        } else if (d.angV != 0f) {
            d.angV += 700f * dt;
            d.ang += d.angV * dt;
            if (d.ang >= 90f) { d.ang = 90f; d.angV = Math.abs(d.angV) > 40f ? -d.angV * 0.25f : 0f; }
        }
        d.slide += d.slideV * dt;
        d.slideV *= (float) Math.exp(-(d.ang > 80f ? 7f : 2.5f) * dt);   // deitado escorrega pouco
    }

    // gira o corpo em volta da borda de baixo do lado (fx, fz) para onde ele cai
    private static void fallAbout(PoseStack ps, float deg, float fx, float fz, float half) {
        ps.translate(fx * half, 0, fz * half);
        rot(ps, deg, fz, -fx);
        ps.translate(-fx * half, 0, -fz * half);
    }

    private static boolean deathPose(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity le = e.getEntity();
        Death d = DEATHS.get(le.getId());
        if (d == null || le.deathTime <= 0) return false;
        float t = Mth.clamp((le.deathTime - 1 + e.getPartialTick()) / 40f, 0f, 1f);
        float tSec = (le.deathTime - 1 + e.getPartialTick()) / 20f;
        float tm = (le.tickCount + e.getPartialTick());
        float h = le.getBbHeight(), half = le.getBbWidth() * 0.5f;
        step(d, h, tSec);
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        float sx = -d.dz(), sz = d.dx();          // de lado
        ps.translate(d.dx() * d.slide, d.y, d.dz() * d.slide);
        ps.mulPose(Axis.YP.rotationDegrees(d.spin));
        switch (d.kind()) {
            case 1 -> {   // stab
                float lurch = smooth(t / 0.06f) * (1 - smooth((t - 0.06f) / 0.15f));
                float shake = t > 0.05f && t < 0.32f ? 1f - (t - 0.05f) / 0.27f : 0f;
                float pull = smooth((t - 0.3f) / 0.05f) * (1 - smooth((t - 0.35f) / 0.08f));
                float knees = smooth((t - 0.38f) / 0.1f);
                ps.translate(d.dx() * -0.1f * lurch, 0.06f * lurch - h * 0.12f * knees * (1 - smooth(d.ang / 60f)), d.dz() * -0.1f * lurch);
                rot(ps, Mth.sin(tm * 2.8f) * 4f * shake, sx, sz);
                // tomba para a frente (para quem matou), em volta da borda da frente
                fallAbout(ps, 14f * lurch - 10f * pull + 20f * knees * (1 - smooth(d.ang / 40f)) + d.ang, -d.dx(), -d.dz(), half);
            }
            case 2 -> {   // queimado
                float writhe = t < 0.5f ? 1f - t / 0.5f : 0f;
                rot(ps, Mth.sin(tm * 1.9f) * 14f * writhe, sx, sz);
                float melt = smooth((t - 0.35f) / 0.5f);
                ps.scale(1f + 0.15f * melt, 1f - 0.45f * melt, 1f + 0.15f * melt);
                fallAbout(ps, fall((t - 0.55f) / 0.35f) * 0.9f, sx, sz, half);
            }
            case 3 -> {   // explosao
                if (!d.landed) {
                    ps.translate(0, h * 0.5f, 0);
                    rot(ps, d.tumble, d.dz(), -d.dx());
                    ps.translate(0, -h * 0.5f, 0);
                } else fallAbout(ps, d.ang, d.dx(), d.dz(), half);
            }
            case 4 -> {   // afogado
                float k = smooth(t / 0.8f);
                if (le.isInWater()) ps.translate(0, -0.25f * k, 0);
                fallAbout(ps, 90f * k + Mth.sin(tm * 0.5f) * 4f * (1 - k), sx, sz, half);
            }
            case 5 -> {   // outras
                float wob = t < 0.4f ? Mth.sin(tm * 0.9f) * 11f * (1 - t / 0.4f) : 0f;
                rot(ps, wob, sx, sz);
                fallAbout(ps, fall((t - 0.3f) / 0.45f), sx, sz, half);
            }
            default -> fallAbout(ps, d.ang, d.dx(), d.dz(), half);   // fisica
        }
        return true;
    }

    private static void rot(PoseStack ps, float deg, float ax, float az) {
        ps.mulPose(new org.joml.Quaternionf().rotationAxis(deg * Mth.DEG_TO_RAD, ax, 0, az));
    }

    // quebrado: cai de joelhos (abaixa e inclina para a frente)
    @SubscribeEvent
    public static void onLivingPre(RenderLivingEvent.Pre<?, ?> e) {
        if (deathPose(e)) return;
        if (!KNEELING.contains(e.getEntity().getId())) return;
        LivingEntity le = e.getEntity();
        float yaw = Mth.rotLerp(e.getPartialTick(), le.yBodyRotO, le.yBodyRot) * Mth.DEG_TO_RAD;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(0, -le.getBbHeight() * 0.12f, 0);
        ps.mulPose(new org.joml.Quaternionf().rotationAxis(18f * Mth.DEG_TO_RAD, Mth.cos(yaw), 0, Mth.sin(yaw)));
    }

    @SubscribeEvent
    public static void onLivingPost(RenderLivingEvent.Post<?, ?> e) {
        LivingEntity le = e.getEntity();
        if (DEATHS.containsKey(le.getId()) && le.deathTime > 0 || KNEELING.contains(le.getId())) e.getPoseStack().popPose();
    }

    @SubscribeEvent
    public static void onWorld(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || POSTURE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = now();
        for (Map.Entry<Integer, Post> en : POSTURE.entrySet()) {
            Post post = en.getValue();
            if (!post.broken() && post.frac() < 0.5f) continue;
            Entity ent = mc.level.getEntity(en.getKey());
            if (ent == null || ent.distanceToSqr(cp) > 32 * 32) continue;
            // embaixo do mob, um pouco para o lado da camera (nao some dentro do chao nem atras dele)
            Vec3 feet = ent.getPosition(pt);
            Vec3 toCam = cp.subtract(feet).multiply(1, 0, 1);
            toCam = toCam.lengthSqr() < 1e-4 ? Vec3.ZERO : toCam.normalize().scale(ent.getBbWidth() * 0.5 + 0.2);
            Vec3 at = feet.add(toCam).add(0, 0.12, 0);
            ps.pushPose();
            ps.translate(at.x - cp.x, at.y - cp.y, at.z - cp.z);
            ps.mulPose(cam.rotation());
            Matrix4f mat = ps.last().pose();
            float w = 0.45f, h = 0.035f;
            int bg = 0xB0101010;
            int fg = post.broken() ? ((int) (180 + 75 * Math.sin(now / 90.0)) << 24 | 0xFFD54A) : 0xF0F2F2F2;
            quad(vc, mat, -w - 0.012f, -h - 0.012f, w + 0.012f, h + 0.012f, bg);
            float f = post.broken() ? 1f : post.frac();
            // a barra enche das pontas para o meio (como no Sekiro/Elden Ring)
            quad(vc, mat, -w * f, -h, w * f, h, fg);
            ps.popPose();
        }
        buf.endBatch(RenderType.debugQuads());
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int argb) {
        vc.addVertex(m, x0, y0, 0).setColor(argb);
        vc.addVertex(m, x1, y0, 0).setColor(argb);
        vc.addVertex(m, x1, y1, 0).setColor(argb);
        vc.addVertex(m, x0, y1, 0).setColor(argb);
        vc.addVertex(m, x0, y1, 0).setColor(argb);
        vc.addVertex(m, x1, y1, 0).setColor(argb);
        vc.addVertex(m, x1, y0, 0).setColor(argb);
        vc.addVertex(m, x0, y0, 0).setColor(argb);
    }

    // ------------------------------------------------------------------ tela: flash, vinheta, cinema, corte, avisos

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        long dt = now() - zoomAt;
        if (dt > 420) return;
        float k = dt < 80 ? dt / 80f : 1f - (dt - 80) / 340f;
        e.setFOV(e.getFOV() * (1 - 0.08f * k));
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        long now = now();
        // vinheta azul da esquiva perfeita (o mundo "desacelera")
        long dd = now - dodgeAt;
        if (dd < 1500) {
            float k = dd < 120 ? dd / 120f : dd > 1200 ? (1500 - dd) / 300f : 1f;
            int a = (int) (k * 120);
            int edge = (a << 24) | 0x2A5CFF, clear = 0x002A5CFF;
            int band = h / 4;
            g.fillGradient(0, 0, w, band, edge, clear);
            g.fillGradient(0, h - band, w, h, clear, edge);
            g.fill(0, 0, w, h, ((int) (k * 35) << 24) | 0x103060);
        }
        // barras de cinema do golpe critico
        long dl = now - letterboxAt, total = Math.max(950, letterboxUntil - letterboxAt);
        if (dl < total) {
            float k = dl < 150 ? dl / 150f : dl > total - 200 ? (total - dl) / 200f : 1f;
            int bar = (int) (h * 0.11f * k);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
        // linha do Issen: um risco branco que corta a tela e some
        long ds = now - slashAt;
        if (ds < 320) {
            float k = 1f - ds / 320f;
            int len = (int) (w * Math.min(1f, ds / 70f));
            int y = h / 2 - 6;
            g.pose().pushPose();
            g.pose().translate(w / 2f, y, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(-12));
            g.fill(-len / 2, -1, len / 2, 2, ((int) (k * 255) << 24) | 0xFFFFFF);
            g.fill(-len / 2, -4, len / 2, 5, ((int) (k * 70) << 24) | 0xBFE3FF);
            g.pose().popPose();
        }
        // flash do parry / Issen / critico
        long df = now - flashAt;
        if (df < 130) g.fill(0, 0, w, h, ((int) ((1 - df / 130f) * 90 * flashPower) << 24) | 0xE8F4FF);
        // avisos de contra-ataque
        String tip = null; int col = 0xFFFFFF;
        if (issenTarget(now) != -1) { tip = Component.translatableWithFallback("combat.binosvanillaplus.issen", "Issen!").getString(); col = 0xBFE3FF; }
        else if (rushTarget(now) != -1) { tip = Component.translatableWithFallback("combat.binosvanillaplus.rush", "Rajada!").getString(); col = 0x8FB8FF; }
        if (tip != null) {
            float pulse = 1f + 0.08f * (float) Math.sin(now / 60.0);
            g.pose().pushPose();
            g.pose().translate(w / 2f, h / 2f + 22, 0);
            g.pose().scale(pulse, pulse, 1);
            g.drawString(mc.font, tip, -mc.font.width(tip) / 2, 0, 0xFF000000 | col, true);
            g.pose().popPose();
        }
    }
}

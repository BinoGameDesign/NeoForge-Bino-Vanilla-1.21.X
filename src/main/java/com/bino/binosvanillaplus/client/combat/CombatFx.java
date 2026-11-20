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
    record Death(int kind, float dx, float dz) {}
    private static final Map<Integer, Death> DEATHS = new HashMap<>();
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
                DEATHS.put(m.a(), new Death(m.b(), Mth.cos(r), Mth.sin(r)));
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

    // morte com animacao propria? (LivingEntityRendererDeathMixin tira a queda de lado do jogo)
    public static boolean customDeath(LivingEntity e) { return e.deathTime > 0 && DEATHS.containsKey(e.getId()); }

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

    // Morte (mod Combat): o mob fica 2 s (LivingEntityDeathMixin) e some com o "puf". Animacoes do corpo inteiro:
    //  0 normal (golpe, flecha, mob): cambaleia para tras, na direcao do golpe, e cai de costas quicando
    //  1 com stab (morto por estocada): treme espetado, cai de joelhos e tomba para a frente, em cima de quem matou
    //  2 queimado (fogo, lava, raio): se debate, encolhe e desaba no lugar
    //  3 explosao: e' arremessado girando e cai longe
    //  4 afogado/sufocado: amolece devagar e deita de lado, afundando um pouco
    //  5 outras (queda, fome, veneno...): cambaleia de um lado para o outro e cai de lado
    private static boolean deathPose(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity le = e.getEntity();
        Death d = DEATHS.get(le.getId());
        if (d == null || le.deathTime <= 0) return false;
        float t = Mth.clamp((le.deathTime - 1 + e.getPartialTick()) / 40f, 0f, 1f);
        float tm = (le.tickCount + e.getPartialTick());
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        float h = le.getBbHeight();
        float ax = d.dz(), az = -d.dx();          // eixo: cair NA direcao do golpe
        float sx = -d.dz(), sz = d.dx();          // de lado
        switch (d.kind()) {
            case 1 -> {   // stab
                float shake = t < 0.25f ? (1 - t / 0.25f) : 0f;
                float j = Mth.sin(tm * 2.6f) * 3f * shake;
                float knees = smooth((t - 0.2f) / 0.2f);
                ps.translate(0, 0.06f * shake - h * 0.18f * knees, 0);
                float ang = -(18f * knees + fall((t - 0.42f) / 0.35f) * 0.8f);
                rot(ps, ang + j, ax, az);
            }
            case 2 -> {   // queimado: se debate, encolhe e desaba
                float writhe = t < 0.5f ? 1f - t / 0.5f : 0f;
                rot(ps, Mth.sin(tm * 1.9f) * 14f * writhe, sx, sz);
                rot(ps, Mth.sin(tm * 1.3f) * 8f * writhe, ax, az);
                float melt = smooth((t - 0.35f) / 0.5f);
                ps.scale(1f + 0.15f * melt, 1f - 0.55f * melt, 1f + 0.15f * melt);
                rot(ps, fall((t - 0.55f) / 0.35f) * 0.9f, sx, sz);
            }
            case 3 -> {   // explosao: arremessado girando
                float fly = smooth(t / 0.45f);
                float arc = Mth.sin(Mth.PI * Math.min(1f, t / 0.45f));
                ps.translate(d.dx() * 2.2f * fly, 1.1f * arc, d.dz() * 2.2f * fly);
                float spin = t < 0.45f ? 540f * smooth(t / 0.45f) : 540f;
                ps.translate(0, h * 0.5f, 0);
                rot(ps, spin, ax, az);
                ps.translate(0, -h * 0.5f, 0);
                if (t >= 0.45f) rot(ps, 90f - 10f * Mth.sin(Mth.PI * Mth.clamp((t - 0.45f) / 0.15f, 0f, 1f)), ax, az);
            }
            case 4 -> {   // afogado: amolece e deita de lado devagar
                float k = smooth(t / 0.8f);
                ps.translate(0, -0.25f * k, 0);
                rot(ps, 90f * k + Mth.sin(tm * 0.5f) * 4f * (1 - k), sx, sz);
            }
            case 5 -> {   // outras: cambaleia e cai de lado
                float wob = t < 0.4f ? Mth.sin(tm * 0.9f) * 12f * (1 - t / 0.4f) : 0f;
                rot(ps, wob, sx, sz);
                rot(ps, fall((t - 0.3f) / 0.45f), sx, sz);
            }
            default -> {  // normal: cambaleia para tras e cai de costas
                float back = smooth(t / 0.25f);
                ps.translate(d.dx() * 0.45f * back, 0, d.dz() * 0.45f * back);
                rot(ps, Mth.sin(tm * 1.2f) * 5f * (1 - back), sx, sz);
                rot(ps, fall((t - 0.12f) / 0.4f), ax, az);
            }
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
        long dl = now - letterboxAt;
        if (dl < 950) {
            float k = dl < 150 ? dl / 150f : dl > 750 ? (950 - dl) / 200f : 1f;
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

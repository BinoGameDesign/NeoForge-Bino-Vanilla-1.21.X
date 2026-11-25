package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosmining.client.HeldToolModel;
import com.bino.binosmining.client.lapidary.LapidaryHands;
import com.bino.binosvanillaplus.combat.CombatActionPayload;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.CombatMove;
import com.bino.binosvanillaplus.combat.MoveBroadcastPayload;
import com.bino.binosvanillaplus.combat.MovePayload;
import com.bino.binosvanillaplus.combat.MoveTiming;
import com.bino.binosvanillaplus.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Hack and slash (mod Combat), lado do cliente. So com ESPADA ou MACHADO (uma mao) na mao principal.
//
// CONTROLES
//  - esquerdo = golpe leve; direito EM COMBATE = golpe pesado (gasta estamina). O dano do leve sai no IMPACTO da
//    animacao (ataque normal do jogo, carga cheia). Clicar durante um golpe guarda o proximo.
//  - ARVORE DE COMBOS (ver CombatMove): L L L L L = Tempestade/Lenhador, L L L L P = Juizo, L L P L L = Quebra-Guarda.
//    No meio do combo: no ar = golpe pulando, correndo = investida, logo apos rolar = Contra-Rolamento (L) /
//    Saida de Cacador (P). Esses contam como continuacao.
//  - direito bem na hora do golpe de um mob com aviso dourado = PARRY (perfeito se for no finzinho). Parry perfeito
//    abre o ISSEN: esquerdo em ~0,5 s.
//  - esquiva perfeita (rolar em cima do golpe) abre a RAJADA: esquerdo em ~1,4 s.
//  - mob de guarda quebrada perto = direito faz o GOLPE CRITICO.
//  - em combate = bateu ou apanhou nos ultimos 6 s, ou tem monstro a ate 12 blocos.
//
// VISUAL: corpo inteiro (MoveAnims) em terceira pessoa (braco, tronco, pernas, agachar, pulos, rodopios,
// cambalhotas); em primeira pessoa a mao + braco da skin, rastro da lamina e a camera seguindo o quadril.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class CombatMoves {
    private CombatMoves() {}

    private static final long COMBAT_MS = 6000, COMBO_GAP_MS = 500, BLEND_MS = 110, TRAIL_MS = 110, EQUIP_MS = 260;

    static final class Active {
        final CombatMove move; final boolean heavy, axe; final long start; final int length; final MoveAnims.Key[] keys;
        final BbAnims.Anim bb;                    // animacao do Blockbench (terceira pessoa), se ja existir para o golpe
        boolean hitDone;
        float[] from = MoveAnims.REST;            // pose de onde este golpe sai (o fim do golpe anterior)
        long pauseStart, pauseEnd;                // pausa no acerto (a pose congela um instante)
        Active(CombatMove move, boolean heavy, boolean axe, long start, int length) {
            this.move = move; this.heavy = heavy; this.axe = axe; this.start = start; this.length = length;
            this.keys = MoveAnims.keys(move, axe);
            this.bb = BbAnims.forMove(move, axe);
        }
        float bbTime(long now) { return u(now) * bb.length(); }
        BbAnims.Sample fromBb;                    // pose em que o golpe anterior estava (o combo sai dela, sem voltar ao descanso)
        static final float CHAIN_BLEND_MS = 120f;
        BbAnims.Sample src(long now) {
            BbAnims.Sample s = BbAnims.at(bb, bbTime(now));
            if (fromBb == null) return s;
            float k = Mth.clamp((t(now) - start) / CHAIN_BLEND_MS, 0f, 1f);
            return k >= 1f ? s : BbAnims.mix(fromBb, s, k * k * (3 - 2 * k));
        }
        long t(long now) { return now < pauseEnd ? Math.min(now, pauseStart) : now - (pauseEnd - pauseStart); }
        float u(long now) { return Mth.clamp((t(now) - start) / (float) length, 0f, 1f); }
        boolean done(long now) { return t(now) - start >= length; }
        void pause(long now, long ms) { if (pauseEnd == 0) { pauseStart = now; pauseEnd = now + ms; } }
    }

    // novo golpe: sai da pose em que o anterior estava (sem voltar ao descanso no meio do combo)
    private static final Map<Integer, Long> LAST_MOVE = new HashMap<>();

    // postura de combate (PlayerLife): voce em combate; os outros jogadores, se golpearam nos ultimos 6 s
    public static boolean stance(Player p) {
        if (CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return false;
        if (p instanceof LocalPlayer lp && lp == Minecraft.getInstance().player) return inCombat(lp, now());
        Long t = LAST_MOVE.get(p.getId());
        return t != null && now() - t < COMBAT_MS;
    }

    public static boolean moving(Player p) { return activeFor(p) != null; }

    private static void put(int id, Active a) {
        Active prev = ACTIVE.get(id);
        long now = now();
        LAST_MOVE.put(id, now);
        if (LAST_MOVE.size() > 64) LAST_MOVE.clear();
        if (prev != null && !prev.done(now)) {
            a.from = pose(prev, now);
            if (prev.bb != null && a.bb != null) a.fromBb = frozen(prev, now);
        }
        ACTIVE.put(id, a);
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();
    private static Boolean queued;            // proximo golpe guardado (true = pesado)
    private static String seq = "";           // entradas do combo atual ("LLP"...)
    private static long lastMoveEnd, lastCombat, heavyReady;
    private static int hudStep; private static long hudAt; private static String hudName;
    private static long punchAt = -100000; private static float punchPower;
    private static ItemStack lastHeld = ItemStack.EMPTY; private static long equipAt = -100000;
    // fase 4: avanco (W W), Lanca-Ceu (P segurado), combo aereo
    private static final double[] DASH_SPEED = {0.70, 0.78, 0.74, 0.60, 0.40, 0.20};
    private static long dashStart = -100000, dashReady, lastFwdTap = -100000, airStart, launchAt = -100000;
    private static Vec3 dashDir = Vec3.ZERO;
    private static boolean wasFwd;
    private static long cinemaRestore;
    private static net.minecraft.client.CameraType savedCamera;

    // projetil vindo em voce e quase chegando: {id, nivel} ou null. Nivel 2 (devolve ao atirador) so mirando
    // bem em cima do projetil (a mira no projetil); senao 0 (so apara: ele cai)
    static int[] arrowIncoming(LocalPlayer p) {
        int best = -1; double bestEta = com.bino.binosvanillaplus.combat.ParryZones.WINDOW_MS + 1;
        Vec3 c = p.getBoundingBox().getCenter();
        for (net.minecraft.world.entity.projectile.Projectile pr : p.level().getEntitiesOfClass(
                net.minecraft.world.entity.projectile.Projectile.class, p.getBoundingBox().inflate(12), pr -> pr.getOwner() != p && pr.isAlive())) {
            Vec3 v = pr.getDeltaMovement();
            double sp = v.length();
            if (sp < 0.3) continue;   // parado (flecha cravada)
            Vec3 to = c.subtract(pr.position());
            double d = to.length();
            if (d < 1e-3 || v.dot(to) / (sp * d) < 0.85) continue;
            double eta = d / sp * 50;   // ms ate chegar
            if (eta < bestEta) { bestEta = eta; best = pr.getId(); }
        }
        if (best == -1 || bestEta > com.bino.binosvanillaplus.combat.ParryZones.WINDOW_MS) return null;
        return new int[]{best, p.level().getEntity(best) instanceof net.minecraft.world.entity.projectile.Projectile pr && aimedAt(p, pr) ? 2 : 0};
    }

    // a mira esta em cima do projetil? (distancia do projetil ate a linha da mira: ~0,4 bloco perto, um pouco mais longe)
    public static boolean aimedAt(Player p, Entity pr) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 to = pr.getBoundingBox().getCenter().subtract(eye);
        double along = to.dot(look);
        if (along <= 0.2) return false;
        double off = to.subtract(look.scale(along)).length();
        return off < 0.32 + 0.035 * along;
    }

    // camera de cinema da facada pelas costas: de lado, mostrando voce e o mob, girando devagar
    private static int cinemaTarget = -1;
    private static long cinemaStart;

    public static float[] cinemaCam(Player p, float pt) {
        if (cinemaRestore == 0 || cinemaTarget == -1 || p != Minecraft.getInstance().player) return null;
        Entity t = p.level().getEntity(cinemaTarget);
        if (t == null) return null;
        Vec3 a = p.getPosition(pt), b = t.getPosition(pt);
        Vec3 dir = b.subtract(a).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = p.getLookAngle().multiply(1, 0, 1);
        dir = dir.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        float k = Mth.clamp((now() - cinemaStart) / 1400f, 0f, 1f);
        double ang = -0.55 + 0.7 * k;   // gira de leve durante a cena
        Vec3 mid = a.add(b).scale(0.5).add(0, 1.0, 0);
        Vec3 cam = mid.add(side.scale(3.3 * Math.cos(ang))).add(dir.scale(3.3 * Math.sin(ang))).add(0, 0.7, 0);
        Vec3 d = mid.subtract(cam);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[]{(float) cam.x, (float) cam.y, (float) cam.z, yaw, pitch};
    }

    // mob de costas para voce (voce fora do campo de visao dele)
    static boolean behind(Player p, LivingEntity t) {
        double y = Math.toRadians(t.yBodyRot);
        Vec3 face = new Vec3(-Math.sin(y), 0, Math.cos(y));
        Vec3 to = p.position().subtract(t.position()).multiply(1, 0, 1);
        return to.lengthSqr() > 1e-4 && face.dot(to.normalize()) < -0.35;
    }

    // durante a facada pelas costas e o golpe critico voce nao anda
    @SubscribeEvent
    public static void onMoveInput(net.neoforged.neoforge.client.event.MovementInputUpdateEvent e) {
        Active a = ACTIVE.get(e.getEntity().getId());
        if (a == null || a.done(now()) || a.move != CombatMove.BACKSTAB && a.move != CombatMove.CRITICAL) return;
        var in = e.getInput();
        in.forwardImpulse = 0; in.leftImpulse = 0; in.up = in.down = in.left = in.right = false; in.jumping = false;
    }
    private static long useDownAt;   // segurando o direito (o jogo repete o clique: ignorar a repeticao)
    private static int airHits;
    private static final Map<Integer, Deque<TrailPt>> TP_TRAIL = new HashMap<>();

    private record TrailPt(Vector3f base, Vector3f tip, long t) {}
    // rastro desenhado como "entidade translucida que brilha" (com shaders o tipo de raio do jogo virava placas gigantes)
    private static final java.util.function.Supplier<RenderType> TRAIL_TYPE = () -> RenderType.entityTranslucentEmissive(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "textures/particle/hit_spark.png"));

    private static void vtx(VertexConsumer vc, Matrix4f m, Vector3f p, int r, int g, int b, int a, float u, float v) {
        if (m != null) vc.addVertex(m, p.x, p.y, p.z); else vc.addVertex(p.x, p.y, p.z);
        vc.setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
    }
    private static final Deque<TrailPt> TRAIL = new ArrayDeque<>();

    private static long now() { return System.currentTimeMillis(); }

    // ================================================================== entrada

    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || e.getHand() != InteractionHand.MAIN_HAND) return;
        // escudo na outra mao (agachado, ou sem arma): o escudo sobe normal e, se um golpe com aviso estiver chegando,
        // vira parry (janela maior que a da arma, sem Issen)
        if (e.isUseItem() && p.getOffhandItem().getItem() instanceof ShieldItem
                && CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) {
            int mob = CombatFx.parryTarget(p, now());
            if (mob != -1) {   // escudo: so bloqueia (branco)
                CombatFx.consumeIncoming(mob);
                PacketDistributor.sendToServer(new CombatActionPayload(CombatActionPayload.PARRY, mob, CombatActionPayload.WHITE));
            }
            return;
        }
        if (CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return;
        if (!e.isAttack() && !e.isUseItem()) return;
        if (e.isUseItem() && useDownAt > 0 && now() - useDownAt > 150
                && !(mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)) { cancel(e); return; }   // repeticao de quem segura
        if (DodgeRoll.rolling(p) || Defense.stunned()) { e.setCanceled(true); e.setSwingHand(false); return; }   // rolando / guarda quebrada
        long now = now();
        Active cur = ACTIVE.get(p.getId());
        boolean busy = cur != null && !cur.done(now);
        // grama, flores, folhas etc. nao seguram o golpe em combate (a lamina passa por elas)
        boolean onBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK && !(soft(mc) && inCombat(p, now));
        if (e.isAttack()) {
            if (onBlock && !busy && CombatFx.issenTarget(now) == -1 && CombatFx.rushTarget(now) == -1) return;   // minerando
            e.setCanceled(true);
            e.setSwingHand(false);
            int t;
            if ((t = CombatFx.issenTarget(now)) != -1) { CombatFx.consumeIssen(); special(p, CombatMove.ISSEN, CombatActionPayload.ISSEN, t, (byte) 0, now); return; }
            if ((t = CombatFx.rushTarget(now)) != -1) { CombatFx.consumeRush(); special(p, CombatMove.RUSH, CombatActionPayload.RUSH, t, (byte) 0, now); return; }
            // agachado e por tras do mob: facada pelas costas (cena em terceira pessoa)
            if (p.isShiftKeyDown() && mc.hitResult instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le
                    && !(le instanceof Player) && le.isAlive() && behind(p, le)
                    && !(le instanceof net.minecraft.world.entity.Mob mb && mb.getTarget() == p)) {   // so se ele nao te viu
                special(p, CombatMove.BACKSTAB, CombatActionPayload.BACKSTAB, le.getId(), (byte) 0, now);
                return;
            }
            request(p, false, cur, now);
        } else {
            // golpe critico na guarda quebrada
            LivingEntity broken = CombatFx.brokenTarget(p);
            if (broken != null) { cancel(e); special(p, CombatMove.CRITICAL, CombatActionPayload.CRITICAL, broken.getId(), (byte) 0, now); return; }
            // (parry e aparar flecha agora sao na tecla Defender: Defense)
            if (onBlock || !inCombat(p, now)) return;   // descascar tronco etc.
            cancel(e);
            lastCombat = now;
            request(p, true, cur, now);
        }
    }

    private static void cancel(InputEvent.InteractionKeyMappingTriggered e) { e.setCanceled(true); e.setSwingHand(false); }

    // golpes especiais (parry, Issen, Rajada, critico): interrompem o golpe atual
    static void special(LocalPlayer p, CombatMove m, byte action, int target, byte level, long now) {
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        int len = MoveTiming.lengthMs(m, false, axe, p.getCurrentItemAttackStrengthDelay());
        put(p.getId(), new Active(m, false, axe, now, len));
        queued = null;
        TRAIL.clear();
        lastCombat = now;
        lastMoveEnd = now + len;
        if (m == CombatMove.CRITICAL) step(p, 0.12, 0);
        if (m == CombatMove.BACKSTAB) {   // mostra a cena de lado e volta para a camera de antes
            Minecraft mc = Minecraft.getInstance();
            cinemaTarget = target;
            cinemaStart = now;
            if (cinemaRestore == 0) savedCamera = mc.options.getCameraType();
            if (mc.options.getCameraType().isFirstPerson()) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            cinemaRestore = now + len + 150;
            CombatFx.letterbox(len + 150);
        }
        if (m == CombatMove.PARRY) step(p, -0.06, 0);
        PacketDistributor.sendToServer(new CombatActionPayload(action, target, level));
    }

    private static void request(LocalPlayer p, boolean heavy, Active cur, long now) {
        if (chargeMove != null) return;   // carregando um pesado
        if (cur == null || cur.done(now) || cur.move == CombatMove.DASH) begin(p, heavy, now);
        else if (cur.u(now) >= 0.25f) queued = heavy;   // guarda o proximo (encadeia quando este acabar)
    }

    // arvore dos combos (null = fora da arvore: recomeca). O pesado so fecha combo: "P" sozinho nao existe.
    private static CombatMove tree(String s) {
        return switch (s) {
            case "L" -> CombatMove.LIGHT_1;
            case "LL" -> CombatMove.LIGHT_2;
            case "LLL" -> CombatMove.LIGHT_3;
            case "LLLL" -> CombatMove.LIGHT_4;
            case "LLLLL" -> CombatMove.LIGHT_5;
            case "LP" -> CombatMove.SURPRISE;
            case "LLP" -> CombatMove.GUARD_BREAK;
            case "LLLP" -> CombatMove.JUDGMENT;
            case "LLLLP" -> CombatMove.TORMENTA;
            default -> null;
        };
    }

    private static CombatMove route(LocalPlayer p, String s, boolean heavy, long now) {
        boolean fresh = s.length() == 1;   // comecando agora (nao e' continuacao de combo)
        // golpes de contexto (fora da lista): logo depois do avanco ou do rolamento, ou correndo
        if (fresh) {
            if (now - dashStart < DASH_SPEED.length * 50L + 450) return heavy ? CombatMove.PIERCE : CombatMove.DASH_SLASH;
            if (DodgeRoll.justRolled(p, now)) return heavy ? CombatMove.ROLL_THRUST : CombatMove.ROLL_SLASH;
            if (p.isSprinting() && p.onGround()) return CombatMove.FORWARD;
        }
        if (!p.onGround() && !p.isInWater() && !p.onClimbable()) {   // no ar: mesmos botoes; o pesado que fecha e' o Meteoro
            if (heavy) return fresh ? null : CombatMove.METEOR;
            return switch (s.length()) { case 1, 4 -> CombatMove.AIR_1; case 2 -> CombatMove.AIR_2; default -> CombatMove.AIR_3; };
        }
        CombatMove t = tree(s);
        if (t != null && heavy && p.isShiftKeyDown()) return CombatMove.LAUNCHER;   // abaixado: o pesado levanta o inimigo
        return t;
    }

    private static void begin(LocalPlayer p, boolean heavy, long now) {
        CombatEvents.Weapon w = CombatEvents.weapon(p.getMainHandItem());
        if (w == CombatEvents.Weapon.NONE) return;
        if (heavy && now < heavyReady) return;
        boolean axe = w == CombatEvents.Weapon.AXE;
        char c = heavy ? 'P' : 'L';
        boolean cont = now <= lastMoveEnd + COMBO_GAP_MS && seq.length() < CombatMove.MAX_COMBO;
        String s = (cont ? seq : "") + c;
        CombatMove m = route(p, s, heavy, now);
        if (m == null) { s = String.valueOf(c); m = route(p, s, heavy, now); }
        if (m == null) return;
        if (heavy) {   // pesado: comeca a carregar; sai ao soltar o botao (ou sozinho no nivel forte)
            if (Stamina.combat() <= 0) { Stamina.denied(); return; }
            chargeMove = m; chargeSeq = s; chargeAxe = axe; chargeAt = now; chargeShown = 0;
            return;
        }
        start(p, false, axe, s, m, now, 0);
    }

    // pesado carregando (null = nenhum)
    private static CombatMove chargeMove;
    private static String chargeSeq = "";
    private static boolean chargeAxe;
    private static long chargeAt;
    private static int chargeShown;

    public static boolean charging() { return chargeMove != null; }
    public static float chargeProgress(long now) { return chargeMove == null ? 0f : Math.min(1f, (now - chargeAt) / (float) CombatMove.CHARGE_MS[3]); }

    private static void chargeTick(Minecraft mc, LocalPlayer p, long now) {
        if (chargeMove == null) return;
        long held = now - chargeAt;
        int lv = CombatMove.chargeLevel(held);
        if (lv > chargeShown) {   // subiu de nivel: brilho na arma e um "tim"
            chargeShown = lv;
            Vec3 h = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.35, 0);
            p.level().addParticle(com.bino.binosvanillaplus.entity.ModParticles.IMPACT_RING.get(), h.x, h.y, h.z, 0.35 + 0.15 * lv, lv == 1 ? 0 : lv == 2 ? 1 : 2, 1);
            p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 0.8f + 0.25f * lv, false);
        }
        boolean held2 = mc.options.keyUse.isDown();
        if (!held2 || held >= CombatMove.CHARGE_MS[3] || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) {
            CombatMove m = chargeMove;
            chargeMove = null;
            if (CombatEvents.weapon(p.getMainHandItem()) != CombatEvents.Weapon.NONE) start(p, true, chargeAxe, chargeSeq, m, now, lv);
        }
    }

    private static void start(LocalPlayer p, boolean heavy, boolean axe, String s, CombatMove m, long now, int charge) {
        boolean serverHit = m.serverHit(heavy);
        if (!Stamina.use(heavy ? Stamina.HEAVY : Stamina.LIGHT)) return;   // sem folego
        int len = MoveTiming.lengthMs(m, heavy, axe, p.getCurrentItemAttackStrengthDelay());
        put(p.getId(), new Active(m, heavy, axe, now, len));
        lastMoveEnd = now + len;
        if (serverHit) heavyReady = now + len - 80;
        seq = m.closesCombo() ? "" : s;
        hudStep = s.length(); hudAt = now;
        hudName = m.closesCombo() ? comboName(m, axe) : null;
        if (m.aerial()) {
            airHits++;
            if (airStart == 0) airStart = now;
            Vec3 d = p.getDeltaMovement();
            p.setDeltaMovement(d.x, Math.max(d.y, 0.22), d.z);   // um respiro no ar (sobe um pouco a cada golpe)
        }
        if (m == CombatMove.METEOR) { Vec3 d = p.getDeltaMovement(); p.setDeltaMovement(d.x, Math.max(d.y, 0.2), d.z); }
        impulse(p, m, heavy, axe);
        PacketDistributor.sendToServer(new MovePayload((byte) m.ordinal(), heavy, (byte) (Math.min(15, s.length()) | (charge << 4))));
    }

    private static String comboName(CombatMove m, boolean axe) {
        return switch (m) {
            case TORMENTA -> Component.translatableWithFallback("combat.binosvanillaplus.combo.tormenta", "Tormenta!").getString();
            case SURPRISE -> Component.translatableWithFallback("combat.binosvanillaplus.combo.surprise", "Surpresa!").getString();
            case LIGHT_5 -> Component.translatableWithFallback("combat.binosvanillaplus.combo.base", "Base!").getString();
            case JUDGMENT -> Component.translatableWithFallback("combat.binosvanillaplus.combo.judgment", "Juizo!").getString();
            case GUARD_BREAK -> Component.translatableWithFallback("combat.binosvanillaplus.combo.guard_break", "Quebra-Guarda!").getString();
            case LAUNCHER -> Component.translatableWithFallback("combat.binosvanillaplus.combo.launcher", "Lanca-Ceu!").getString();
            case AIR_3 -> Component.translatableWithFallback("combat.binosvanillaplus.combo.aerial", "Combo Aereo!").getString();
            case METEOR -> Component.translatableWithFallback("combat.binosvanillaplus.combo.meteor", "Meteoro!").getString();
            case DANCE -> Component.translatableWithFallback("combat.binosvanillaplus.combo.dance", "Danca!").getString();
            case DASH_SLASH -> Component.translatableWithFallback("combat.binosvanillaplus.combo.dash", "Avanco!").getString();
            case PIERCE -> Component.translatableWithFallback("combat.binosvanillaplus.combo.pierce", "Perfurador!").getString();
            default -> null;
        };
    }

    // deslocamento real do golpe (o corpo anda/pula junto com a animacao)
    private static void impulse(LocalPlayer p, CombatMove m, boolean heavy, boolean axe) {
        double fwd = 0, up = 0;
        switch (m) {
            case FORWARD -> fwd = heavy ? 0.65 : 0.4;
            case ROLL_SLASH -> fwd = 0.15;
            case ROLL_THRUST -> fwd = 0.5;
            case GUARD_BREAK -> fwd = 0.25;
            case TEMPEST, TORMENTA -> { fwd = axe ? 0.25 : 0.35; up = axe ? 0 : 0.42; }
            case SURPRISE -> fwd = axe ? 0.45 : 0.55;
            case LIGHT_5 -> fwd = 0.15;
            case JUDGMENT -> { fwd = axe ? 0.2 : -0.35; up = axe ? 0.55 : 0.45; }
            case BREAK_1, BREAK_2 -> { fwd = 0.1; up = axe ? 0.35 : 0; }
            case JUMP, AIR_1, AIR_2, AIR_3, METEOR -> fwd = 0;
            case DASH_SLASH -> fwd = 0.35;
            case PIERCE -> fwd = axe ? 0.6 : 0.7;
            case DANCE, LAUNCHER -> fwd = 0.1;
            default -> fwd = p.onGround() ? (heavy ? 0.12 : 0.06) : 0;
        }
        step(p, fwd, up);
    }

    private static void step(LocalPlayer p, double fwd, double up) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) return;
        flat = flat.normalize();
        Vec3 d = p.getDeltaMovement().add(flat.scale(fwd));
        if (up > 0 && p.onGround()) d = new Vec3(d.x, Math.max(d.y, up), d.z);
        p.setDeltaMovement(d);
    }

    static boolean inCombat(LocalPlayer p, long now) {
        if (p.hurtTime > 0) lastCombat = now;
        if (now - lastCombat < COMBAT_MS) return true;
        return !p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12),
                le -> le instanceof Enemy && le.isAlive()).isEmpty();
    }

    // anima um golpe so aqui (o servidor ja resolveu: ex. Issen do parry azul)
    public static void playLocal(CombatMove m) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        long now = now();
        int len = MoveTiming.lengthMs(m, false, axe, p.getCurrentItemAttackStrengthDelay());
        put(p.getId(), new Active(m, false, axe, now, len));
        queued = null;
        lastMoveEnd = now + len;
    }

    static void holdCombo(long until) { lastMoveEnd = Math.max(lastMoveEnd, until); }

    // rolamento cancela o golpe que estava saindo
    static void cancel(Player p) {
        ACTIVE.remove(p.getId());
        if (p == Minecraft.getInstance().player) { queued = null; TRAIL.clear(); }
    }

    // a cada quadro: impacto do golpe, fim do golpe e o proximo guardado
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        long now = now();
        if (cinemaRestore > 0 && now >= cinemaRestore) {
            cinemaRestore = 0;
            cinemaTarget = -1;
            if (savedCamera != null && mc.options.getCameraType() != savedCamera) mc.options.setCameraType(savedCamera);
            savedCamera = null;
        }
        if (p.onGround() && now - launchAt > 350 && (airHits > 0 || airStart > 0)
                && (ACTIVE.get(p.getId()) == null || !ACTIVE.get(p.getId()).move.aerial())) { airHits = 0; airStart = 0; }
        chargeTick(mc, p, now);
        Active a = ACTIVE.get(p.getId());
        if (a == null) { queued = null; return; }
        if (!a.hitDone && a.u(now) >= MoveTiming.impact(a.move, a.axe)) { a.hitDone = true; impact(mc, p, a); }
        boolean canChain = a.u(now) >= MoveTiming.chain(a.move, a.heavy, a.axe);
        if (queued != null && canChain) { boolean h = queued; queued = null; begin(p, h, now); }
        else if (a.done(now)) ACTIVE.remove(p.getId());
    }

    private static void impact(Minecraft mc, LocalPlayer p, Active a) {
        if (mc.screen instanceof com.bino.binosvanillaplus.client.combo.ComboListScreen) return;   // so a demonstracao da lista de combos
        if (a.move.visualOnly()) return;   // o servidor resolve (parry, Issen, Rajada, critico)
        if (a.move == CombatMove.LAUNCHER) {   // pula atras do mob (um pouco mais alto que ele)
            Vec3 d = p.getDeltaMovement();
            p.setDeltaMovement(d.x, 0.86, d.z);
            airHits = 0; airStart = now(); launchAt = now();
        }
        if (a.move == CombatMove.METEOR) { Vec3 d = p.getDeltaMovement(); p.setDeltaMovement(d.x * 0.3, -1.1, d.z * 0.3); }   // mergulho
        else if (a.move.aerial() && !p.onGround()) { Vec3 d = p.getDeltaMovement(); p.setDeltaMovement(d.x, Math.max(d.y, 0.16), d.z); }   // sobe junto com o mob
        if (a.move.serverHit(a.heavy)) {   // o servidor faz o dano; aqui so o peso
            boolean big = a.move == CombatMove.JUMP || a.move == CombatMove.JUDGMENT || a.move == CombatMove.METEOR;
            HitFx.kick(big ? 1.4f : 1.1f);
            a.pause(now(), big ? 100 : 75);
            lastMoveEnd += big ? 100 : 75;
            punchAt = now(); punchPower = big ? 1f : 0.7f;
            p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), a.axe ? ModSounds.AXE_SWING.get() : ModSounds.SWORD_SWING.get(),
                    SoundSource.PLAYERS, 1f, 0.72f, false);
            lastCombat = now();
            return;
        }
        if (a.move.closesCombo()) { punchAt = now(); punchPower = 0.6f; }
        Entity target = mc.hitResult instanceof EntityHitResult eh ? eh.getEntity() : soft(mc) ? pickThrough(p) : null;
        if (target == null) target = assist(p);
        if (target != null && mc.gameMode != null) {
            lastCombat = now();
            mc.gameMode.attack(p, target);   // o ataque normal do jogo (dano, critico, varrida, encantamentos)
            a.pause(now(), a.move.closesCombo() ? 90 : 55);   // a pose segura um instante no impacto (peso)
            lastMoveEnd += a.move.closesCombo() ? 90 : 55;
        } else HitFx.swingAir(p);
    }

    // bloco "mole" na mira: plantas, grama, flores, folhas (tudo sem colisao, mais as folhas)
    private static boolean soft(Minecraft mc) {
        if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK || mc.level == null) return false;
        var st = mc.level.getBlockState(bh.getBlockPos());
        return st.is(net.minecraft.tags.BlockTags.LEAVES) || st.getCollisionShape(mc.level, bh.getBlockPos()).isEmpty();
    }

    // quem esta na mira atras da planta (a mira do jogo para na planta)
    private static Entity pickThrough(LocalPlayer p) {
        double reach = p.entityInteractionRange();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(reach));
        EntityHitResult r = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p, eye, end,
                p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1), en -> !en.isSpectator() && en.isPickable(), reach * reach);
        return r != null ? r.getEntity() : null;
    }

    // ajuda de mira: monstro bem na frente, dentro do alcance
    private static Entity assist(LocalPlayer p) {
        double reach = p.entityInteractionRange();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Entity best = null; double bestD = Double.MAX_VALUE;
        for (LivingEntity le : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach + 1),
                le -> le instanceof Enemy && le.isAlive() && le.isPickable())) {
            Vec3 c = le.getBoundingBox().getCenter();
            Vec3 to = c.subtract(eye);
            double d = to.length() - le.getBbWidth() * 0.5;
            if (d > reach || d >= bestD || to.normalize().dot(look) < 0.82 || !p.hasLineOfSight(le)) continue;
            best = le; bestD = d;
        }
        return best;
    }

    // outro jogador comecou um golpe
    public static void receive(MoveBroadcastPayload m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(m.entityId()) instanceof Player pl)) return;
        boolean axe = CombatEvents.weapon(pl.getMainHandItem()) == CombatEvents.Weapon.AXE;
        put(m.entityId(), new Active(CombatMove.of(m.move()), m.heavy(), axe, now(), Math.max(100, m.lengthMs())));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ACTIVE.clear(); PUSHED.clear(); TRAIL.clear(); TP_TRAIL.clear(); seq = ""; queued = null; chargeMove = null; airHits = 0; airStart = 0;
    }

    // ================================================================== avanco (W W) e flutuar no combo aereo

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        boolean fwd = mc.options.keyUp.isDown();
        long now = now();
        if (fwd && !wasFwd && mc.screen == null) {
            if (now - lastFwdTap < 260) tryDash(p, now);
            lastFwdTap = now;
        }
        wasFwd = fwd;
        if (mc.options.keyUse.isDown()) { if (useDownAt == 0) useDownAt = now; } else useDownAt = 0;
    }

    private static void tryDash(LocalPlayer p, long now) {
        if (now < dashReady || !p.onGround() || p.isShiftKeyDown() || p.isInWater() || p.isPassenger() || p.isFallFlying()
                || DodgeRoll.rolling(p) || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE || !inCombat(p, now)) return;
        if (!Stamina.use(Stamina.DASH)) return;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) return;
        dashDir = flat.normalize();
        dashStart = now;
        dashReady = now + 650;
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        int len = MoveTiming.lengthMs(CombatMove.DASH, false, axe, p.getCurrentItemAttackStrengthDelay());
        put(p.getId(), new Active(CombatMove.DASH, false, axe, now, len));
        queued = null;
        holdCombo(now + len);
        p.level().playLocalSound(p.getX(), p.getY() + 0.5, p.getZ(), ModSounds.SWORD_SWING.get(), SoundSource.PLAYERS, 0.5f, 0.6f, false);
        var st = p.level().getBlockState(p.blockPosition().below());
        if (!st.isAir()) for (int i = 0; i < 6; i++)
            p.level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, st),
                    p.getX(), p.getY() + 0.05, p.getZ(), -dashDir.x * 0.2, 0.1, -dashDir.z * 0.2);
        PacketDistributor.sendToServer(new MovePayload((byte) CombatMove.DASH.ordinal(), false, (byte) 0));
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e) {
        if (!(e.getEntity() instanceof LocalPlayer p) || p != Minecraft.getInstance().player) return;
        long now = now();
        int i = (int) ((now - dashStart) / 50);
        if (i >= 0 && i < DASH_SPEED.length) {
            Vec3 d = p.getDeltaMovement();
            p.setDeltaMovement(dashDir.x * DASH_SPEED[i], d.y, dashDir.z * DASH_SPEED[i]);
        }
        // combo aereo: cai bem devagar enquanto golpeia (no maximo ~1,3 s no ar)
        Active a = ACTIVE.get(p.getId());
        if (a != null && !a.done(now) && !p.onGround() && airStart > 0 && now - airStart < 1500
                && (a.move.aerial() || a.move == CombatMove.LAUNCHER && a.hitDone)) {
            Vec3 d = p.getDeltaMovement();
            if (d.y < -0.03) p.setDeltaMovement(d.x, -0.03, d.z);
            p.fallDistance = 0;
        }
    }

    // ================================================================== pose

    private static Active activeFor(Player p) {
        Active a = ACTIVE.get(p.getId());
        if (a == null) return null;
        if (a.done(now()) || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) {
            if (p != Minecraft.getInstance().player) ACTIVE.remove(p.getId());
            return null;
        }
        return a;
    }

    // pose agora; entra suave a partir do descanso (rodopio e cambalhota nao, sao voltas inteiras)
    private static float[] pose(Active a, long now) {
        float[] v = a.bb != null ? BbAnims.firstPersonPose(a.src(now), tpWeight(a, now)) : MoveAnims.sample(a.keys, a.u(now));
        float in = Mth.clamp((a.t(now) - a.start) / (float) BLEND_MS, 0f, 1f);
        in = in * in * (3 - 2 * in);
        for (int i = 0; i < MoveAnims.N; i++)
            if (i != MoveAnims.SP && i != MoveAnims.FL) v[i] = a.from[i] + (v[i] - a.from[i]) * in;
        return v;
    }

    // terceira pessoa entra/sai da pose do jogo base
    // pose do golpe anterior congelada no instante em que o novo comecou
    private static BbAnims.Sample frozen(Active prev, long now) {
        BbAnims.Sample s = prev.src(now);
        String[] bones = {"root", "tumble", "head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "item"};
        Map<String, float[][]> snap = new HashMap<>();
        for (String b : bones) snap.put(b, new float[][]{s.rot(b), s.pos(b), s.scale(b)});
        float w = tpWeight(prev, now);   // se o anterior ja estava voltando para a pose do jogo, sai dali
        return new BbAnims.Sample() {
            public float[] rot(String b) { float[] v = snap.get(b)[0].clone(); for (int i = 0; i < 3; i++) v[i] *= w; return v; }
            public float[] pos(String b) { float[] v = snap.get(b)[1].clone(); for (int i = 0; i < 3; i++) v[i] *= w; return v; }
            public float[] scale(String b) { float[] v = snap.get(b)[2].clone(); for (int i = 0; i < 3; i++) v[i] = 1 + (v[i] - 1) * w; return v; }
        };
    }

    private static float tpWeight(Active a, long now) {
        float u = a.u(now);
        float w = (a.fromBb != null ? 1f : Math.min(1f, u / 0.1f)) * Math.min(1f, (1f - u) / 0.22f);
        w = Mth.clamp(w, 0f, 1f);
        return w * w * (3 - 2 * w);
    }

    private static Vector3f bladeDir(float ay, float ap) {
        return new Quaternionf().rotateY((float) Math.toRadians(-ay)).rotateX((float) Math.toRadians(-ap)).transform(new Vector3f(0, 1, 0));
    }

    // ------------------------------------------------------------------ primeira pessoa

    // mola da mao em primeira pessoa: segue a pose com um pouquinho de atraso (da peso e suaviza as viradas)
    private static final float[] SPRING = MoveAnims.REST.clone(), SPRING_V = new float[6];
    private static long springAt;

    private static void spring(float[] v, long now) {
        float dt = (now - springAt) / 1000f;
        springAt = now;
        boolean bad = false;
        for (int i = 0; i < 6; i++) bad |= !Float.isFinite(SPRING[i]) || !Float.isFinite(SPRING_V[i]);
        if (bad || dt > 0.25f) {   // voltou de outra camera / travou: comeca colado na pose
            System.arraycopy(v, 0, SPRING, 0, 6);
            java.util.Arrays.fill(SPRING_V, 0f);
            return;
        }
        if (dt <= 0f) { System.arraycopy(SPRING, 0, v, 0, 6); return; }
        float w = 34f;   // rigidez (maior = segue mais grudado)
        float e = (float) Math.exp(-w * dt);
        for (int i = 0; i < 6; i++) {   // amortecimento critico, conta exata: estavel com qualquer FPS
            float x0 = SPRING[i] - v[i], v0 = SPRING_V[i];
            float c = v0 + w * x0;
            float x = (x0 + c * dt) * e;
            SPRING_V[i] = (v0 - w * c * dt) * e;
            SPRING[i] = v[i] + x;
            v[i] = SPRING[i];
        }
    }

    private static void springReset() {
        System.arraycopy(MoveAnims.REST, 0, SPRING, 0, 6);
        java.util.Arrays.fill(SPRING_V, 0f);
        springAt = now();
    }

    private static Quaternionf rot(float[] v, float side) {
        return new Quaternionf().rotateY((float) Math.toRadians(-v[MoveAnims.AY] * side))
                .rotateX((float) Math.toRadians(-v[MoveAnims.AP])).rotateY((float) Math.toRadians(v[MoveAnims.ER] * side));
    }

    // Primeira pessoa (espada/machado): o braco da skin segura a arma, e a arma e' desenhada do MESMO jeito que o jogo
    // desenha (mesma posicao e o "display" do modelo, 2D ou 3D), so que movida e girada pela pose do golpe a partir do
    // descanso; por isso aponta para o lado certo, nao "pula" ao comecar/terminar e fica do tamanho certo.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHand(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        long now = now();
        Active a = activeFor(p);
        // golpe com animacao do Blockbench: braços 3D do proprio boneco vistos dos olhos (como o Better Combat)
        if (a != null && a.bb != null) {
            e.setCanceled(true);
            if (e.getHand() == InteractionHand.MAIN_HAND) { arms3D(e, mc, p, a, now); springReset(); }
            return;
        }
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = e.getItemStack();
        if (CombatEvents.weapon(stack) == CombatEvents.Weapon.NONE || p.isUsingItem() && p.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            springReset();
            return;   // sem espada/machado: o jogo desenha
        }
        float dip = DodgeRoll.handDip(p);
        e.setCanceled(true);
        if (!ItemStack.isSameItem(stack, lastHeld) && a != null) equipAt = -100000;

        float[] v = a != null ? pose(a, now) : MoveAnims.REST.clone();
        if (a == null) {
            float sw = e.getSwingProgress();   // minerando
            if (sw > 0) {
                float f = Mth.sin(Mth.sqrt(sw) * Mth.PI);
                v[MoveAnims.AP] += 55 * f; v[MoveAnims.HY] -= 0.08f * f; v[MoveAnims.HZ] -= 0.1f * f; v[MoveAnims.HX] -= 0.08f * f;
            }
            if (!ItemStack.isSameItem(stack, lastHeld)) equipAt = now;   // trocou de item: sobe de baixo
            float eq = 1f - Mth.clamp((now - equipAt) / (float) EQUIP_MS, 0f, 1f);
            v[MoveAnims.HY] -= 0.6f * eq * eq;
            v[MoveAnims.HY] += Mth.sin(now / 650f) * 0.006f;   // respiracao
        }
        v[MoveAnims.HY] -= 0.55f * dip; v[MoveAnims.AP] += 35f * dip; v[MoveAnims.HX] += 0.08f * dip;
        // nunca encosta na camera (senao a arma vira uma placa enorme na tela)
        v[MoveAnims.HZ] = Math.min(v[MoveAnims.HZ], -0.52f);
        v[MoveAnims.HY] = Math.min(v[MoveAnims.HY], 0.08f);
        v[MoveAnims.HX] = Mth.clamp(v[MoveAnims.HX], -0.62f, 0.72f);
        spring(v, now);

        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        PoseStack ps = e.getPoseStack();
        MultiBufferSource buf = e.getMultiBufferSource();
        int light = e.getPackedLight();
        Quaternionf abs = rot(v, side), rest = rot(MoveAnims.REST, side);

        // braco da skin: do ombro (fora da tela, embaixo) ate o cabo da arma
        LapidaryHands.arm(ps, buf, Vec3.ZERO, new Vec3(v[MoveAnims.HX] * side, v[MoveAnims.HY] - 0.04, v[MoveAnims.HZ] + 0.06),
                new Vec3(0.40 * side, -0.98, 0.18), side > 0, light, new Vec3(0, 0, -1), p, 0.62f);

        ps.pushPose();
        ps.translate(v[MoveAnims.HX] * side, v[MoveAnims.HY], v[MoveAnims.HZ]);
        if (a != null && MoveAnims.striking(a.keys, a.u(now))) {   // rastro: base e ponta da lamina pela direcao absoluta
            ps.pushPose();
            ps.mulPose(abs);
            Matrix4f m = ps.last().pose();
            float len = a.axe ? 0.72f : 0.84f;
            TRAIL.addLast(new TrailPt(m.transformPosition(new Vector3f(0, len * 0.3f, 0)), m.transformPosition(new Vector3f(0, len, 0)), now));
            ps.popPose();
        }
        ps.mulPose(new Quaternionf(abs).mul(new Quaternionf(rest).conjugate()));   // so a diferenca para o descanso
        boolean right = side > 0;
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, stack,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !right, ps, buf, light);
        ps.popPose();
        renderTrail(buf, now, a != null && a.move.serverHit(a.heavy), a != null && a.axe);
        lastHeld = stack.copy();
    }

    // Primeira pessoa igual a terceira (ideia do Better Combat, codigo proprio): o boneco do jogador recebe a pose do golpe
    // (o mesmo setupAnim da terceira pessoa) e so os dois braços (pele, manga da camisa, manga da armadura) e a arma
    // sao desenhados, a partir dos olhos. Barato: um setupAnim e 4 partes do modelo + o item, so durante o golpe.
    // O corpo inteiro (root) entra so como deslocamento; giros/cambalhotas ficam com a camera (onCamera).
    private static void arms3D(RenderHandEvent e, Minecraft mc, LocalPlayer p, Active a, long now) {
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr)) return;
        var model = pr.getModel();
        float pt = e.getPartialTick();
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        BbAnims.Sample src = a.src(now);
        float w = tpWeight(a, now);
        model.attackTime = 0f;
        model.crouching = p.isCrouching();
        model.riding = false;
        model.young = false;
        model.rightArmPose = right ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = right ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        model.setupAnim(p, 0f, 0f, p.tickCount + pt, 0f, 0f);   // passa pelo HumanoidModelCombatMixin (pose do golpe)

        PoseStack ps = e.getPoseStack();
        MultiBufferSource buf = e.getMultiBufferSource();
        int light = e.getPackedLight();
        var cam = mc.gameRenderer.getMainCamera();
        Matrix4f eyeSpace = new Matrix4f(ps.last().pose());   // espaco da mao normal (para a transicao da arma)
        ps.pushPose();
        ps.translate(0f, -0.06f, 0f);                                      // um pouco abaixo dos olhos: braço nao cobre a tela
        ps.mulPose(new Quaternionf(cam.rotation()).conjugate());          // mundo -> olhos
        ps.mulPose(halfPitch(cam));                                        // os braços acompanham metade da inclinacao da camera
        Vec3 d = p.getPosition(pt).subtract(cam.getPosition());
        ps.translate(d.x, d.y, d.z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - p.getViewYRot(pt)));     // o boneco olha para onde a camera olha
        BbAnims.rootShift(ps, src, w, right);
        ps.scale(-1f, -1f, 1f);
        ps.scale(0.9375f, 0.9375f, 0.9375f);
        ps.translate(0f, -1.501f, 0f);

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(p.getSkin().texture()));
        int ov = OverlayTexture.NO_OVERLAY;
        model.rightArm.visible = model.leftArm.visible = true;
        model.rightSleeve.visible = p.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        model.leftSleeve.visible = p.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
        ModelPart[] arms = {model.rightArm, model.rightSleeve, model.leftArm, model.leftSleeve};
        for (ModelPart part : arms) { part.xScale = part.zScale = ARM_THIN; part.render(ps, vc, light, ov); }   // mais finos: nao tampam a tela
        for (ModelPart part : arms) part.xScale = part.zScale = 1f;
        com.bino.binosmining.client.gear.HandArmor.render(ps, buf, light, p, true, model);
        com.bino.binosmining.client.gear.HandArmor.render(ps, buf, light, p, false, model);

        ItemStack stack = p.getMainHandItem();
        if (!stack.isEmpty()) {   // a arma: igual a camada do item na mao da terceira pessoa
            HumanoidArm arm = right ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
            ps.pushPose();
            model.translateToHand(arm, ps);
            poseItem(p, arm, ps);
            ps.mulPose(Axis.XP.rotationDegrees(-90f));
            ps.mulPose(Axis.YP.rotationDegrees(180f));
            ps.translate((right ? 1f : -1f) / 16f, 0.125f, -0.625f);
            float k = switchBlend(a, now);
            if (k >= 1f) {
                mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, stack,
                        right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, !right, ps, buf, light);
            } else {   // comeco/fim do golpe: a arma desliza de onde a mao normal segura ate a mao do boneco (sem pulo)
                var ir = mc.getItemRenderer();
                var bm = ir.getModel(stack, p.level(), p, p.getId());
                PoseStack fpS = new PoseStack();
                fpS.last().pose().set(eyeSpace).translate(MoveAnims.REST[MoveAnims.HX] * (right ? 1f : -1f), MoveAnims.REST[MoveAnims.HY], MoveAnims.REST[MoveAnims.HZ]);
                bm.applyTransform(right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, fpS, !right);
                PoseStack tpS = new PoseStack();
                tpS.last().pose().set(ps.last().pose());
                bm.applyTransform(right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, tpS, !right);
                Matrix4f m = blend(fpS.last().pose(), tpS.last().pose(), k);
                PoseStack out = new PoseStack();
                out.last().pose().set(m);
                out.last().normal().set(m.normal(new org.joml.Matrix3f()));
                ir.render(stack, ItemDisplayContext.NONE, !right, out, buf, light, OverlayTexture.NO_OVERLAY, bm);
            }
            ps.popPose();
        }
        ps.popPose();
        lastHeld = stack.copy();
    }

    private static final float ARM_THIN = 0.82f, SWITCH_MS = 110f;

    // 0..1: quanto a arma ja passou da mao normal para a mao do boneco (no comeco e no fim do golpe)
    private static float switchBlend(Active a, long now) {
        float since = a.fromBb != null ? SWITCH_MS : a.t(now) - a.start, left = a.length - (a.t(now) - a.start);
        float k = Mth.clamp(Math.min(since, left) / SWITCH_MS, 0f, 1f);
        return k * k * (3 - 2 * k);
    }

    // mistura duas matrizes (posicao, giro e tamanho) sem deformar
    private static Matrix4f blend(Matrix4f a, Matrix4f b, float k) {
        Vector3f ta = a.getTranslation(new Vector3f()), tb = b.getTranslation(new Vector3f());
        Vector3f sa = a.getScale(new Vector3f()), sb = b.getScale(new Vector3f());
        Quaternionf qa = a.getNormalizedRotation(new Quaternionf()), qb = b.getNormalizedRotation(new Quaternionf());
        return new Matrix4f().translationRotateScale(ta.lerp(tb, k), qa.slerp(qb, k), sa.lerp(sb, k));
    }

    // giro (no mundo, em volta dos olhos) que devolve metade da inclinacao da camera aos braços
    private static Quaternionf halfPitch(net.minecraft.client.Camera cam) {
        float yaw = (float) Math.PI - cam.getYRot() * Mth.DEG_TO_RAD, pitch = cam.getXRot() * Mth.DEG_TO_RAD;
        return new Quaternionf().rotationYXZ(yaw, -pitch * 0.5f, 0f).mul(new Quaternionf().rotationY(yaw).conjugate());
    }

    // rastro solido e curto (estilo Zelda): espada branco-azulado, machado branco-alaranjado, pesado dourado
    private static void renderTrail(MultiBufferSource buf, long now, boolean heavy, boolean axe) {
        for (Iterator<TrailPt> it = TRAIL.iterator(); it.hasNext(); ) if (now - it.next().t() > TRAIL_MS) it.remove();
        if (TRAIL.size() < 2) return;
        VertexConsumer vc = buf.getBuffer(TRAIL_TYPE.get());
        int r = 255, g = heavy ? 225 : axe ? 230 : 240, b = heavy ? 150 : axe ? 200 : 255;
        if (!heavy && !axe) r = 225;
        TrailPt prev = null;
        for (TrailPt pt : TRAIL) {
            if (prev != null && prev.tip().distanceSquared(pt.tip()) > 1e-6) {
                int a0 = (int) (120 * (1 - (now - prev.t()) / (float) TRAIL_MS)), a1 = (int) (120 * (1 - (now - pt.t()) / (float) TRAIL_MS));
                quad(vc, prev.base(), prev.tip(), pt.tip(), pt.base(), a0, a1, r, g, b);
            }
            prev = pt;
        }
    }

    private static void quad(VertexConsumer vc, Vector3f b0, Vector3f t0, Vector3f t1, Vector3f b1, int a0, int a1, int r, int g, int b) {
        quadM(vc, null, b0, t0, t1, b1, a0, a1, r, g, b);
    }

    private static void quadM(VertexConsumer vc, Matrix4f m, Vector3f b0, Vector3f t0, Vector3f t1, Vector3f b1, int a0, int a1, int r, int g, int b) {
        if (t0.distanceSquared(t1) > 0.3f || b0.distanceSquared(b1) > 0.3f) return;   // salto grande (FPS baixo/shaders): nao estica uma placa
        vtx(vc, m, b0, r, g, b, 0, 0, 1); vtx(vc, m, t0, r, g, b, a0, 0, 0); vtx(vc, m, t1, r, g, b, a1, 1, 0); vtx(vc, m, b1, r, g, b, 0, 1, 1);
        vtx(vc, m, b1, r, g, b, 0, 1, 1); vtx(vc, m, t1, r, g, b, a1, 1, 0); vtx(vc, m, t0, r, g, b, a0, 0, 0); vtx(vc, m, b0, r, g, b, 0, 0, 1);
    }

    // ------------------------------------------------------------------ rastro da lamina em terceira pessoa

    // base e ponta da lamina no mundo, a partir da mesma pose (mao vista "dos olhos", no sentido do corpo)
    private static Vec3[] bladeWorld(Player p, float[] v, float pt) {
        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        float yaw = (Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot) + v[MoveAnims.SP] % 360f) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)), up = new Vec3(0, 1, 0);
        Vec3 eye = p.getPosition(pt).add(0, p.getBbHeight() * 0.83 + v[MoveAnims.HOP] - v[MoveAnims.CR] * 0.28, 0).add(fwd.scale(v[MoveAnims.DX]));
        Vec3 hand = eye.add(right.scale(v[MoveAnims.HX] * side)).add(up.scale(v[MoveAnims.HY])).add(fwd.scale(-v[MoveAnims.HZ]));
        Vector3f bd = bladeDir(v[MoveAnims.AY] * side, v[MoveAnims.AP]);
        Vec3 blade = right.scale(bd.x).add(up.scale(bd.y)).add(fwd.scale(-bd.z));
        return new Vec3[]{hand.add(blade.scale(0.3)), hand.add(blade.scale(1.0))};
    }

    @SubscribeEvent
    public static void onWorld(net.neoforged.neoforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = now();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean fp = mc.options.getCameraType().isFirstPerson();
        for (Map.Entry<Integer, Active> en : ACTIVE.entrySet()) {
            Active a = en.getValue();
            if (a.done(now) || mc.player != null && en.getKey() == mc.player.getId() && fp && a.bb == null) continue;
            if (!(mc.level.getEntity(en.getKey()) instanceof Player pl)) continue;
            if (a.bb != null) {   // animacao do Blockbench: a lamina pelas contas do proprio modelo
                float bt = a.bbTime(now);
                if (!a.bb.striking(bt)) continue;
                BbAnims.Sample src = a.src(now);
                boolean mineFp = fp && pl == mc.player;   // primeira pessoa: o boneco segue a camera (arms3D)
                Vec3[] bw = BbAnims.blade(src, pl.getMainArm() == HumanoidArm.RIGHT,
                        mineFp ? pl.getViewYRot(pt) : Mth.rotLerp(pt, pl.yBodyRotO, pl.yBodyRot), pl.getPosition(pt));
                if (mineFp) {   // primeira pessoa: os braços estao 0,06 abaixo e com metade da inclinacao (arms3D)
                    var cam = e.getCamera();
                    Vec3 eye = cam.getPosition();
                    Quaternionf hp = halfPitch(cam);
                    for (int i = 0; i < 2; i++) {
                        Vector3f r = hp.transform(new Vector3f((float) (bw[i].x - eye.x), (float) (bw[i].y - eye.y), (float) (bw[i].z - eye.z)));
                        Vector3f down = new Quaternionf(cam.rotation()).transform(new Vector3f(0f, -0.06f, 0f));
                        bw[i] = eye.add(r.x + down.x, r.y + down.y, r.z + down.z);
                    }
                }
                TP_TRAIL.computeIfAbsent(en.getKey(), k -> new ArrayDeque<>()).addLast(new TrailPt(bw[0].toVector3f(), bw[1].toVector3f(), now));
                continue;
            }
            if (!MoveAnims.striking(a.keys, a.u(now))) continue;
            float[] v = pose(a, now);
            if (Math.abs(v[MoveAnims.FL] % 360f) > 1 || Math.abs(v[MoveAnims.RL]) > 1) continue;   // de cabeca para baixo: sem rastro
            Vec3[] bt = bladeWorld(pl, v, pt);
            TP_TRAIL.computeIfAbsent(en.getKey(), k -> new ArrayDeque<>())
                    .addLast(new TrailPt(bt[0].toVector3f(), bt[1].toVector3f(), now));
        }
        if (TP_TRAIL.isEmpty()) return;
        var cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(TRAIL_TYPE.get());
        for (Iterator<Map.Entry<Integer, Deque<TrailPt>>> it = TP_TRAIL.entrySet().iterator(); it.hasNext(); ) {
            Deque<TrailPt> d = it.next().getValue();
            d.removeIf(t -> now - t.t() > TRAIL_MS);
            if (d.isEmpty()) { it.remove(); continue; }
            TrailPt prev = null;
            for (TrailPt t : d) {
                if (prev != null && prev.tip().distanceSquared(t.tip()) > 1e-6) {
                    int a0 = (int) (150 * (1 - (now - prev.t()) / (float) TRAIL_MS)), a1 = (int) (150 * (1 - (now - t.t()) / (float) TRAIL_MS));
                    quadW(vc, m, prev.base(), prev.tip(), t.tip(), t.base(), a0, a1);
                }
                prev = t;
            }
        }
        ps.popPose();
        buf.endBatch(TRAIL_TYPE.get());
    }

    private static void quadW(VertexConsumer vc, Matrix4f m, Vector3f b0, Vector3f t0, Vector3f t1, Vector3f b1, int a0, int a1) {
        quadM(vc, m, b0, t0, t1, b1, a0, a1, 240, 245, 255);
    }

    // camera segue o quadril: torce/tomba (roll), inclina, e acompanha cambalhotas de leve (sem enjoar)
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        Active a = activeFor(mc.player);
        if (a == null) return;
        float[] v = pose(a, now());
        float side = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        e.setRoll(e.getRoll() - v[MoveAnims.TW] * 0.07f * side + v[MoveAnims.RL] * 0.35f);
        e.setPitch(e.getPitch() + v[MoveAnims.LN] * 0.06f + Mth.sin(v[MoveAnims.FL] * Mth.DEG_TO_RAD) * 14f);
        e.setYaw(e.getYaw() + v[MoveAnims.TW] * 0.04f * side);
    }

    // quanto a camera vai a frente (o corpo entra na estocada/investida), para CameraRollMixin
    public static float cameraForward(Player p) {
        Active a = activeFor(p);
        if (a == null) return 0f;
        return pose(a, now())[MoveAnims.DX] * 0.35f;
    }

    // quanto a camera abaixa (agachando no golpe), para CameraRollMixin
    public static float cameraDrop(Player p) {
        Active a = activeFor(p);
        if (a == null) return 0f;
        return pose(a, now())[MoveAnims.CR] * 0.32f;
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        long dt = now() - punchAt;
        if (dt > 260) return;
        float k = 1f - dt / 260f;
        e.setFOV(e.getFOV() * (1 + 0.05f * punchPower * k * k));
    }

    // ------------------------------------------------------------------ contador do combo (do lado da mira)

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || hudStep <= 0) return;
        long now = now(), end = Math.max(lastMoveEnd + COMBO_GAP_MS, hudAt + (hudName != null ? 1100 : 600));
        if (now > end + 350) return;
        float a = now > end ? 1f - (now - end) / 350f : 1f;
        GuiGraphics g = e.getGuiGraphics();
        int cx = g.guiWidth() / 2 + 14, cy = g.guiHeight() / 2 + 8;
        float pop = 1f + 0.45f * Math.max(0f, 1f - (now - hudAt) / 160f);
        boolean fin = hudName != null;
        int col = fin ? 0xFFD54A : 0xFFFFFF;
        int alpha = (int) (a * 255) << 24;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(pop, pop, 1);
        String s = fin ? hudName : "x" + hudStep;
        g.drawString(mc.font, s, 0, -4, alpha | col, true);
        g.pose().popPose();
        for (int i = 0; i < CombatMove.MAX_COMBO; i++) {
            int x = cx + i * 6, y = cy + 7;
            g.fill(x, y, x + 4, y + 2, i < hudStep ? (alpha | col) : ((int) (a * 90) << 24) | 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ terceira pessoa

    // braco, lamina, tronco e pernas (chamado no fim do setupAnim do HumanoidModel)
    public static void posePlayer(HumanoidModel<?> m, Player p) {
        Active a = activeFor(p);
        if (a == null) return;
        long now = now();
        float w = tpWeight(a, now);
        if (w <= 0.001f) return;
        if (a.bb != null) { BbAnims.poseModel(m, a.src(now), w, p.getMainArm() == HumanoidArm.RIGHT); return; }
        float[] v = pose(a, now);
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        float side = right ? 1f : -1f;
        ModelPart main = right ? m.rightArm : m.leftArm, other = right ? m.leftArm : m.rightArm;

        // braco: do ombro ate a mao da animacao (espaco da camera -> espaco do modelo: x e y invertidos)
        Vector3f d = new Vector3f(v[MoveAnims.HX] * side - 0.30f * side, v[MoveAnims.HY] + 0.45f, v[MoveAnims.HZ]);
        Vector3f arm = new Vector3f(-d.x, -d.y, d.z).normalize();
        Vector3f bd = bladeDir(v[MoveAnims.AY] * side, v[MoveAnims.AP]);
        Vector3f blade = new Vector3f(-bd.x, -bd.y, bd.z);
        Quaternionf q = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), arm);
        Vector3f s0 = q.transform(new Vector3f(0, 0, -1));   // para onde a arma aponta com essa pose
        Vector3f t = new Vector3f(blade).sub(new Vector3f(arm).mul(arm.dot(blade)));
        if (t.lengthSquared() > 1e-5f) {
            t.normalize();
            float roll = (float) Math.atan2(new Vector3f(s0).cross(t).dot(arm), s0.dot(t));
            q = new Quaternionf().rotationAxis(roll, arm).mul(q);
        }
        Vector3f eu = q.getEulerAnglesZYX(new Vector3f());
        if (!Float.isFinite(eu.x) || !Float.isFinite(eu.y) || !Float.isFinite(eu.z)) return;   // conta degenerada: nao mexe

        float tw = (float) Math.toRadians(v[MoveAnims.TW]) * side;
        m.body.yRot = Mth.lerp(w, m.body.yRot, tw);
        float cr = v[MoveAnims.CR] * w;   // (o tronco nao inclina sozinho: no modelo do jogo os bracos e a cabeca nao acompanham)
        float by = m.body.yRot;
        m.rightArm.z = Mth.sin(by) * 5f; m.rightArm.x = -Mth.cos(by) * 5f;
        m.leftArm.z = -Mth.sin(by) * 5f; m.leftArm.x = Mth.cos(by) * 5f;

        main.xRot = Mth.lerp(w, main.xRot, eu.x);
        main.yRot = Mth.lerp(w, main.yRot, eu.y);
        main.zRot = Mth.lerp(w, main.zRot, eu.z);
        // a outra mao equilibra: abre para o lado, oposta ao golpe
        other.xRot = Mth.lerp(w, other.xRot, -0.35f - (float) Math.toRadians(v[MoveAnims.LN]) * 0.6f + tw * 0.4f);
        other.yRot = Mth.lerp(w, other.yRot, by);
        other.zRot = Mth.lerp(w, other.zRot, -(0.45f + cr * 0.4f) * side);
        m.head.yRot -= by * 0.5f * w;   // a cabeca segura o olhar enquanto o tronco torce

        ModelPart front = right ? m.rightLeg : m.leftLeg, back = right ? m.leftLeg : m.rightLeg;
        float st = v[MoveAnims.ST];
        if (p.onGround()) {
            front.xRot = Mth.lerp(w, front.xRot, -st * 0.42f - cr * 0.25f);
            back.xRot = Mth.lerp(w, back.xRot, st * 0.32f + cr * 0.15f);
            front.zRot = Mth.lerp(w, front.zRot, 0.06f * side);
            back.zRot = Mth.lerp(w, back.zRot, -0.06f * side);
        } else {   // no ar: joelhos para cima (encolhido)
            front.xRot = Mth.lerp(w, front.xRot, -0.55f);
            back.xRot = Mth.lerp(w, back.xRot, 0.2f);
        }
    }

    // a arma na mao (ItemInHandLayerCombatMixin): o osso "item" da animacao do Blockbench (gira e estica)
    public static void poseItem(net.minecraft.world.entity.LivingEntity e, HumanoidArm arm, PoseStack ps) {
        if (!(e instanceof Player p) || arm != p.getMainArm()) return;
        Active a = activeFor(p);
        if (a == null || a.bb == null) return;
        long now = now();
        float w = tpWeight(a, now);
        if (w > 0.001f) BbAnims.poseItem(ps, a.src(now), w, arm == HumanoidArm.RIGHT);
    }

    // corpo inteiro: agacha, pulinho, rodopio, tombo, cambalhota, inclinacao e o corpo indo a frente
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        Active a = activeFor(p);
        if (a == null || DodgeRoll.rolling(p)) return;
        long now = now();
        float w = tpWeight(a, now);
        float yaw = Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot);
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(p.getId());
        if (a.bb != null) { BbAnims.poseRoot(ps, a.src(now), w, p.getMainArm() == HumanoidArm.RIGHT, yaw); return; }
        float[] v = pose(a, now);
        ps.translate(0, (v[MoveAnims.HOP] - v[MoveAnims.CR] * 0.12f) * w, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.translate(0, 0, v[MoveAnims.DX] * w);                  // corpo vai a frente
        ps.mulPose(Axis.YP.rotationDegrees(-(v[MoveAnims.SP] % 360f)));
        ps.mulPose(Axis.XP.rotationDegrees(v[MoveAnims.LN] * 0.3f * w));   // inclina a partir dos pes (o corpo todo junto)
        float flip = v[MoveAnims.FL] % 360f, roll = v[MoveAnims.RL] * w;
        if (Math.abs(flip) > 0.01f || Math.abs(roll) > 0.01f) {   // cambalhota/tombo em volta do meio do corpo
            ps.translate(0, 0.9f, 0);
            ps.mulPose(Axis.XP.rotationDegrees(flip));
            ps.mulPose(Axis.ZP.rotationDegrees(roll));
            ps.translate(0, -0.9f, 0);
        }
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post e) {
        if (PUSHED.remove(e.getEntity().getId())) e.getPoseStack().popPose();
    }
}

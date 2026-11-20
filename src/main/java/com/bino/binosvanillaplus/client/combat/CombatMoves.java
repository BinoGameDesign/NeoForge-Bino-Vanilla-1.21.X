package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.client.HeldToolModel;
import com.bino.binosvanillaplus.client.lapidary.LapidaryHands;
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

    private static final long COMBAT_MS = 6000, COMBO_GAP_MS = 500, BLEND_MS = 60, TRAIL_MS = 110, EQUIP_MS = 260;

    static final class Active {
        final CombatMove move; final boolean heavy, axe; final long start; final int length; final MoveAnims.Key[] keys;
        boolean hitDone;
        Active(CombatMove move, boolean heavy, boolean axe, long start, int length) {
            this.move = move; this.heavy = heavy; this.axe = axe; this.start = start; this.length = length;
            this.keys = MoveAnims.keys(move, axe);
        }
        float u(long now) { return Mth.clamp((now - start) / (float) length, 0f, 1f); }
        boolean done(long now) { return now - start >= length; }
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
    private static long dashStart = -100000, dashReady, lastFwdTap = -100000, pendingLLP, airStart;
    private static Vec3 dashDir = Vec3.ZERO;
    private static boolean wasFwd;
    private static long useDownAt;   // segurando o direito (o jogo repete o clique: ignorar a repeticao)
    private static int airHits;
    private static final Map<Integer, Deque<TrailPt>> TP_TRAIL = new HashMap<>();

    private record TrailPt(Vector3f base, Vector3f tip, long t) {}
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
                && (p.isShiftKeyDown() || CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE)) {
            int mob = CombatFx.parryTarget(p, now(), 320);
            if (mob != -1) {
                CombatFx.consumeIncoming(mob);
                PacketDistributor.sendToServer(new CombatActionPayload(CombatActionPayload.PARRY, mob, false));
            }
            return;
        }
        if (CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.NONE) return;
        if (!e.isAttack() && !e.isUseItem()) return;
        if (e.isUseItem() && useDownAt > 0 && now() - useDownAt > 150
                && !(mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)) { cancel(e); return; }   // repeticao de quem segura
        if (DodgeRoll.rolling(p)) { e.setCanceled(true); e.setSwingHand(false); return; }
        long now = now();
        Active cur = ACTIVE.get(p.getId());
        boolean busy = cur != null && !cur.done(now);
        boolean onBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
        if (e.isAttack()) {
            if (onBlock && !busy && CombatFx.issenTarget(now) == -1 && CombatFx.rushTarget(now) == -1) return;   // minerando
            e.setCanceled(true);
            e.setSwingHand(false);
            int t;
            if ((t = CombatFx.issenTarget(now)) != -1) { CombatFx.consumeIssen(); special(p, CombatMove.ISSEN, CombatActionPayload.ISSEN, t, false, now); return; }
            if ((t = CombatFx.rushTarget(now)) != -1) { CombatFx.consumeRush(); special(p, CombatMove.RUSH, CombatActionPayload.RUSH, t, false, now); return; }
            request(p, false, cur, now);
        } else {
            // golpe critico na guarda quebrada
            LivingEntity broken = CombatFx.brokenTarget(p);
            if (broken != null) { cancel(e); special(p, CombatMove.CRITICAL, CombatActionPayload.CRITICAL, broken.getId(), false, now); return; }
            // parry: golpe com aviso vindo em mim agora
            int mob = CombatFx.parryTarget(p, now);
            if (mob != -1) {
                cancel(e);
                boolean perfect = CombatFx.perfect(mob, now);
                CombatFx.consumeIncoming(mob);
                special(p, CombatMove.PARRY, CombatActionPayload.PARRY, mob, perfect, now);
                return;
            }
            if (onBlock || !inCombat(p, now)) return;   // descascar tronco etc.
            if (p.isShiftKeyDown() && p.getOffhandItem().getItem() instanceof ShieldItem) return;   // defender com escudo
            cancel(e);
            lastCombat = now;
            request(p, true, cur, now);
        }
    }

    private static void cancel(InputEvent.InteractionKeyMappingTriggered e) { e.setCanceled(true); e.setSwingHand(false); }

    // golpes especiais (parry, Issen, Rajada, critico): interrompem o golpe atual
    private static void special(LocalPlayer p, CombatMove m, byte action, int target, boolean flag, long now) {
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        int len = MoveTiming.lengthMs(m, false, axe, p.getCurrentItemAttackStrengthDelay());
        ACTIVE.put(p.getId(), new Active(m, false, axe, now, len));
        queued = null;
        TRAIL.clear();
        lastCombat = now;
        lastMoveEnd = now + len;
        if (m == CombatMove.CRITICAL) step(p, 0.12, 0);
        if (m == CombatMove.PARRY) step(p, -0.06, 0);
        PacketDistributor.sendToServer(new CombatActionPayload(action, target, flag));
    }

    private static void request(LocalPlayer p, boolean heavy, Active cur, long now) {
        if (pendingLLP > 0) return;   // decidindo Quebra-Guarda x Lanca-Ceu
        if (cur == null || cur.done(now) || cur.move == CombatMove.DASH) begin(p, heavy, now);
        else if (cur.u(now) >= 0.25f) queued = heavy;   // guarda o proximo (encadeia quando este acabar)
    }

    // arvore dos combos (null = fora da arvore: recomeca)
    private static CombatMove tree(String s) {
        return switch (s) {
            case "L" -> CombatMove.LIGHT_1;
            case "LL" -> CombatMove.LIGHT_2;
            case "LLL" -> CombatMove.LIGHT_3;
            case "LLLL" -> CombatMove.LIGHT_4;
            case "LLLLL" -> CombatMove.TEMPEST;
            case "LLLLP" -> CombatMove.JUDGMENT;
            case "LLP" -> CombatMove.GUARD_BREAK;
            case "LLPL" -> CombatMove.BREAK_1;
            case "LLPLL" -> CombatMove.BREAK_2;
            case "P" -> CombatMove.HEAVY;
            default -> null;
        };
    }

    private static CombatMove route(LocalPlayer p, String s, boolean heavy, long now) {
        CombatMove t = tree(s);
        boolean signature = t != null && (t.closesCombo() || t == CombatMove.GUARD_BREAK);   // golpes com nome mandam
        if (!signature) {
            if (now - dashStart < DASH_SPEED.length * 50L + 450) return heavy ? CombatMove.PIERCE : CombatMove.DASH_SLASH;
            if (DodgeRoll.justRolled(p, now))
                return heavy ? (DodgeRoll.chain() >= 2 ? CombatMove.DANCE : CombatMove.ROLL_THRUST) : CombatMove.ROLL_SLASH;
            if (!p.onGround() && !p.isInWater() && !p.onClimbable()) {   // no ar: combo aereo / Meteoro
                if (heavy) return airHits >= 1 ? CombatMove.METEOR : CombatMove.JUMP;
                return airHits == 0 ? CombatMove.AIR_1 : airHits == 1 ? CombatMove.AIR_2 : airHits == 2 ? CombatMove.AIR_3 : CombatMove.JUMP;
            }
            if (p.isSprinting()) return CombatMove.FORWARD;
        }
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
        if (m == CombatMove.GUARD_BREAK) { pendingLLP = now; return; }   // segurou o direito? (onFrame decide)
        start(p, heavy, axe, s, m, now);
    }

    private static void start(LocalPlayer p, boolean heavy, boolean axe, String s, CombatMove m, long now) {
        boolean serverHit = m.serverHit(heavy);
        if (serverHit && !Stamina.use(Stamina.HEAVY)) return;   // sem folego para o pesado
        int len = MoveTiming.lengthMs(m, heavy, axe, p.getCurrentItemAttackStrengthDelay());
        ACTIVE.put(p.getId(), new Active(m, heavy, axe, now, len));
        lastMoveEnd = now + len;
        if (serverHit) heavyReady = now + len - 80;
        seq = m.closesCombo() ? "" : s;
        hudStep = s.length(); hudAt = now;
        hudName = m.closesCombo() || m == CombatMove.GUARD_BREAK ? comboName(m, axe) : null;
        if (m.aerial()) {
            airHits++;
            if (airStart == 0) airStart = now;
            Vec3 d = p.getDeltaMovement();
            p.setDeltaMovement(d.x, Math.max(d.y, 0.12), d.z);   // um respiro no ar
        }
        if (m == CombatMove.METEOR) { Vec3 d = p.getDeltaMovement(); p.setDeltaMovement(d.x, Math.max(d.y, 0.2), d.z); }
        impulse(p, m, heavy, axe);
        PacketDistributor.sendToServer(new MovePayload((byte) m.ordinal(), heavy, (byte) s.length()));
    }

    private static String comboName(CombatMove m, boolean axe) {
        return switch (m) {
            case TEMPEST -> axe ? Component.translatableWithFallback("combat.binosvanillaplus.combo.lumberjack", "Lenhador!").getString()
                    : Component.translatableWithFallback("combat.binosvanillaplus.combo.tempest", "Tempestade!").getString();
            case JUDGMENT -> Component.translatableWithFallback("combat.binosvanillaplus.combo.judgment", "Juizo!").getString();
            case GUARD_BREAK, BREAK_2 -> Component.translatableWithFallback("combat.binosvanillaplus.combo.guard_break", "Quebra-Guarda!").getString();
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
            case TEMPEST -> { fwd = axe ? 0.25 : 0.35; up = axe ? 0 : 0.42; }
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
        if (p.onGround() && (airHits > 0 || airStart > 0) && (ACTIVE.get(p.getId()) == null || !ACTIVE.get(p.getId()).move.aerial())) { airHits = 0; airStart = 0; }
        if (pendingLLP > 0) {   // L L P: soltou = Quebra-Guarda; segurou 0,2 s = Lanca-Ceu
            boolean held = mc.options.keyUse.isDown();
            if (!held || now - pendingLLP >= 200) {
                pendingLLP = 0;
                boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
                if (CombatEvents.weapon(p.getMainHandItem()) != CombatEvents.Weapon.NONE)
                    start(p, true, axe, "LLP", held ? CombatMove.LAUNCHER : CombatMove.GUARD_BREAK, now);
            }
        }
        Active a = ACTIVE.get(p.getId());
        if (a == null) { queued = null; return; }
        if (!a.hitDone && a.u(now) >= MoveTiming.impact(a.move)) { a.hitDone = true; impact(mc, p, a); }
        boolean canChain = a.u(now) >= (a.move.serverHit(a.heavy) ? 0.9f : 0.84f);
        if (queued != null && canChain) { boolean h = queued; queued = null; begin(p, h, now); }
        else if (a.done(now)) ACTIVE.remove(p.getId());
    }

    private static void impact(Minecraft mc, LocalPlayer p, Active a) {
        if (a.move.visualOnly()) return;   // o servidor resolve (parry, Issen, Rajada, critico)
        if (a.move == CombatMove.LAUNCHER) {   // pula atras do mob
            Vec3 d = p.getDeltaMovement();
            p.setDeltaMovement(d.x, 0.62, d.z);
            airHits = 0; airStart = now();
        }
        if (a.move == CombatMove.METEOR) { Vec3 d = p.getDeltaMovement(); p.setDeltaMovement(d.x * 0.3, -1.1, d.z * 0.3); }   // mergulho
        if (a.move.serverHit(a.heavy)) {   // o servidor faz o dano; aqui so o peso
            boolean big = a.move == CombatMove.JUMP || a.move == CombatMove.JUDGMENT || a.move == CombatMove.METEOR;
            HitFx.kick(big ? 1.4f : 1.1f);
            punchAt = now(); punchPower = big ? 1f : 0.7f;
            p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), a.axe ? ModSounds.AXE_SWING.get() : ModSounds.SWORD_SWING.get(),
                    SoundSource.PLAYERS, 1f, 0.72f, false);
            lastCombat = now();
            return;
        }
        if (a.move.closesCombo()) { punchAt = now(); punchPower = 0.6f; }
        Entity target = mc.hitResult instanceof EntityHitResult eh ? eh.getEntity() : assist(p);
        if (target != null && mc.gameMode != null) {
            lastCombat = now();
            mc.gameMode.attack(p, target);   // o ataque normal do jogo (dano, critico, varrida, encantamentos)
        } else HitFx.swingAir(p);
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
        ACTIVE.put(m.entityId(), new Active(CombatMove.of(m.move()), m.heavy(), axe, now(), Math.max(100, m.lengthMs())));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ACTIVE.clear(); PUSHED.clear(); TRAIL.clear(); TP_TRAIL.clear(); seq = ""; queued = null; pendingLLP = 0; airHits = 0; airStart = 0;
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
        if (!Stamina.use(15)) return;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) return;
        dashDir = flat.normalize();
        dashStart = now;
        dashReady = now + 650;
        boolean axe = CombatEvents.weapon(p.getMainHandItem()) == CombatEvents.Weapon.AXE;
        int len = MoveTiming.lengthMs(CombatMove.DASH, false, axe, p.getCurrentItemAttackStrengthDelay());
        ACTIVE.put(p.getId(), new Active(CombatMove.DASH, false, axe, now, len));
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
        if (a != null && !a.done(now) && !p.onGround() && airStart > 0 && now - airStart < 1300
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
        float[] v = MoveAnims.sample(a.keys, a.u(now));
        float in = Mth.clamp((now - a.start) / (float) BLEND_MS, 0f, 1f);
        in = in * in * (3 - 2 * in);
        for (int i = 0; i < MoveAnims.N; i++)
            if (i != MoveAnims.SP && i != MoveAnims.FL) v[i] = MoveAnims.REST[i] + (v[i] - MoveAnims.REST[i]) * in;
        return v;
    }

    // terceira pessoa entra/sai da pose do jogo base
    private static float tpWeight(Active a, long now) {
        float u = a.u(now);
        float w = Math.min(1f, u / 0.1f) * Math.min(1f, (1f - u) / 0.22f);
        w = Mth.clamp(w, 0f, 1f);
        return w * w * (3 - 2 * w);
    }

    private static Vector3f bladeDir(float ay, float ap) {
        return new Quaternionf().rotateY((float) Math.toRadians(-ay)).rotateX((float) Math.toRadians(-ap)).transform(new Vector3f(0, 1, 0));
    }

    // ------------------------------------------------------------------ primeira pessoa

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        ItemStack stack = e.getItemStack();
        if (CombatEvents.weapon(stack) == CombatEvents.Weapon.NONE || p.isUsingItem() && p.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            lastHeld = stack.copy();
            return;
        }
        e.setCanceled(true);
        long now = now();
        if (!ItemStack.isSameItem(stack, lastHeld)) equipAt = now;
        lastHeld = stack.copy();

        Active a = activeFor(p);
        float[] v = a != null ? pose(a, now) : MoveAnims.REST.clone();
        if (a == null) {
            float sw = e.getSwingProgress();   // minerando
            if (sw > 0) {
                float f = Mth.sin(Mth.sqrt(sw) * Mth.PI);
                v[MoveAnims.AP] += 60 * f; v[MoveAnims.HY] -= 0.10f * f; v[MoveAnims.HZ] -= 0.12f * f; v[MoveAnims.HX] -= 0.08f * f;
            }
            v[MoveAnims.HY] += Mth.sin(now / 650f) * 0.006f;   // respiracao
        }
        float dip = DodgeRoll.handDip(p);   // rolando: a arma desce com o corpo
        v[MoveAnims.HY] -= 0.55f * dip; v[MoveAnims.AP] += 35f * dip; v[MoveAnims.HX] += 0.08f * dip;
        float eq = 1f - Mth.clamp((now - equipAt) / (float) EQUIP_MS, 0f, 1f);
        v[MoveAnims.HY] -= 0.6f * eq * eq;

        float side = p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f;
        PoseStack ps = e.getPoseStack();
        MultiBufferSource buf = e.getMultiBufferSource();
        int light = e.getPackedLight();
        Vector3f hand = new Vector3f(v[MoveAnims.HX] * side, v[MoveAnims.HY], v[MoveAnims.HZ]);

        Vec3 shoulder = new Vec3(0.40 * side, -0.86, 0.12);
        LapidaryHands.arm(ps, buf, Vec3.ZERO, new Vec3(hand.x, hand.y, hand.z), shoulder, side > 0, light, new Vec3(0, 0, -1), p, 0.85f);

        ps.pushPose();
        ps.translate(hand.x, hand.y, hand.z);
        ps.mulPose(Axis.YP.rotationDegrees(-v[MoveAnims.AY] * side));
        ps.mulPose(Axis.XP.rotationDegrees(-v[MoveAnims.AP]));
        ps.mulPose(Axis.YP.rotationDegrees(v[MoveAnims.ER] * side));
        if (a != null && MoveAnims.striking(a.keys, a.u(now))) {
            Matrix4f m = ps.last().pose();
            float len = a.axe ? 0.72f : 0.84f;
            TRAIL.addLast(new TrailPt(m.transformPosition(new Vector3f(0, len * 0.3f, 0)), m.transformPosition(new Vector3f(0, len, 0)), now));
        }
        renderWeapon(mc, p, stack, ps, buf, light);
        ps.popPose();
        renderTrail(buf, now, a != null && a.move.serverHit(a.heavy), a != null && a.axe);
    }

    private static void renderWeapon(Minecraft mc, LocalPlayer p, ItemStack stack, PoseStack ps, MultiBufferSource buf, int light) {
        BakedModel model = mc.getItemRenderer().getModel(stack, p.level(), p, p.getId());
        ps.pushPose();
        if (model instanceof HeldToolModel h) {   // modelo 3D do mod (em pe, cabo embaixo)
            ps.scale(0.55f, 0.55f, 0.55f);
            ps.translate(0, 0.36f, 0);
            if (CombatEvents.weapon(stack) == CombatEvents.Weapon.SWORD) ps.mulPose(Axis.YP.rotationDegrees(90));
            mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buf, light, OverlayTexture.NO_OVERLAY, h.solid());
        } else {                                   // icone 2D (diagonal: cabo embaixo-esquerda, ponta em cima-direita)
            ps.scale(0.72f, 0.72f, 0.72f);
            ps.translate(0, 0.42f, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(45));
            mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buf, light, OverlayTexture.NO_OVERLAY, model);
        }
        ps.popPose();
    }

    // rastro solido e curto (estilo Zelda): espada branco-azulado, machado branco-alaranjado, pesado dourado
    private static void renderTrail(MultiBufferSource buf, long now, boolean heavy, boolean axe) {
        for (Iterator<TrailPt> it = TRAIL.iterator(); it.hasNext(); ) if (now - it.next().t() > TRAIL_MS) it.remove();
        if (TRAIL.size() < 2) return;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        int r = 255, g = heavy ? 225 : axe ? 230 : 240, b = heavy ? 150 : axe ? 200 : 255;
        if (!heavy && !axe) r = 225;
        TrailPt prev = null;
        for (TrailPt pt : TRAIL) {
            if (prev != null && prev.tip().distanceSquared(pt.tip()) > 1e-6) {
                int a0 = (int) (170 * (1 - (now - prev.t()) / (float) TRAIL_MS)), a1 = (int) (170 * (1 - (now - pt.t()) / (float) TRAIL_MS));
                quad(vc, prev.base(), prev.tip(), pt.tip(), pt.base(), a0, a1, r, g, b);
            }
            prev = pt;
        }
    }

    private static void quad(VertexConsumer vc, Vector3f b0, Vector3f t0, Vector3f t1, Vector3f b1, int a0, int a1, int r, int g, int b) {
        vc.addVertex(b0.x, b0.y, b0.z).setColor(r, g, b, 0);
        vc.addVertex(t0.x, t0.y, t0.z).setColor(r, g, b, a0);
        vc.addVertex(t1.x, t1.y, t1.z).setColor(r, g, b, a1);
        vc.addVertex(b1.x, b1.y, b1.z).setColor(r, g, b, 0);
        vc.addVertex(b1.x, b1.y, b1.z).setColor(r, g, b, 0);
        vc.addVertex(t1.x, t1.y, t1.z).setColor(r, g, b, a1);
        vc.addVertex(t0.x, t0.y, t0.z).setColor(r, g, b, a0);
        vc.addVertex(b0.x, b0.y, b0.z).setColor(r, g, b, 0);
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
            if (a.done(now) || mc.player != null && en.getKey() == mc.player.getId() && fp) continue;
            if (!(mc.level.getEntity(en.getKey()) instanceof Player pl) || !MoveAnims.striking(a.keys, a.u(now))) continue;
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
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
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
        buf.endBatch(RenderType.lightning());
    }

    private static void quadW(VertexConsumer vc, Matrix4f m, Vector3f b0, Vector3f t0, Vector3f t1, Vector3f b1, int a0, int a1) {
        vc.addVertex(m, b0.x, b0.y, b0.z).setColor(240, 245, 255, 0);
        vc.addVertex(m, t0.x, t0.y, t0.z).setColor(240, 245, 255, a0);
        vc.addVertex(m, t1.x, t1.y, t1.z).setColor(240, 245, 255, a1);
        vc.addVertex(m, b1.x, b1.y, b1.z).setColor(240, 245, 255, 0);
        vc.addVertex(m, b1.x, b1.y, b1.z).setColor(240, 245, 255, 0);
        vc.addVertex(m, t1.x, t1.y, t1.z).setColor(240, 245, 255, a1);
        vc.addVertex(m, t0.x, t0.y, t0.z).setColor(240, 245, 255, a0);
        vc.addVertex(m, b0.x, b0.y, b0.z).setColor(240, 245, 255, 0);
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

        float tw = (float) Math.toRadians(v[MoveAnims.TW]) * side;
        m.body.yRot = Mth.lerp(w, m.body.yRot, tw);
        float cr = v[MoveAnims.CR] * w;
        m.body.xRot = Mth.lerp(w, m.body.xRot, cr * 0.3f);
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
            front.xRot = Mth.lerp(w, front.xRot, -st * 0.75f - cr * 0.55f);
            back.xRot = Mth.lerp(w, back.xRot, st * 0.55f + cr * 0.3f);
            front.zRot = Mth.lerp(w, front.zRot, 0.06f * side);
            back.zRot = Mth.lerp(w, back.zRot, -0.06f * side);
        } else {   // no ar: joelhos para cima (encolhido)
            front.xRot = Mth.lerp(w, front.xRot, -0.9f);
            back.xRot = Mth.lerp(w, back.xRot, -0.4f);
        }
    }

    // corpo inteiro: agacha, pulinho, rodopio, tombo, cambalhota, inclinacao e o corpo indo a frente
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        Active a = activeFor(p);
        if (a == null || DodgeRoll.rolling(p)) return;
        long now = now();
        float[] v = pose(a, now);
        float w = tpWeight(a, now);
        float yaw = Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot);
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(p.getId());
        ps.translate(0, (v[MoveAnims.HOP] - v[MoveAnims.CR] * 0.28f) * w, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.translate(0, 0, v[MoveAnims.DX] * w);                  // corpo vai a frente
        ps.mulPose(Axis.YP.rotationDegrees(-(v[MoveAnims.SP] % 360f)));
        ps.mulPose(Axis.XP.rotationDegrees(v[MoveAnims.LN] * 0.5f * w));   // inclina a partir dos pes
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

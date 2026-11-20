package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.RollPayload;
import com.bino.binosvanillaplus.combat.RollSeenPayload;
import com.bino.binosvanillaplus.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Rolamento estilo Elden Ring (mod Combat), lado do cliente.
//  - Q em combate (ou com espada/machado na mao) = rola na direcao que voce esta andando (WASD). Parado = passo
//    para tras (backstep). Fora disso o Q continua soltando o item; Ctrl+Q sempre solta a pilha.
//  - So no chao. Cancela o golpe que estava saindo. Da para emendar rolamentos (recarga curta).
//  - Invencivel no comeco do rolamento (o servidor decide: DodgeServer); golpes passam e aparece "Miss".
// Animacao:
//  - terceira pessoa: o corpo vira para a direcao do rolamento, se encolhe (joelhos no peito, bracos abracando,
//    queixo para baixo) e da uma cambalhota de 360 rente ao chao; poeira na saida e na chegada.
//  - primeira pessoa: a camera abaixa ate perto do chao, gira para a frente/para o lado conforme a direcao, o FOV
//    abre um pouco e a arma desce junto com o corpo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class DodgeRoll {
    private DodgeRoll() {}

    static final int ROLL_MS = 560, BACK_MS = 380;
    // quanto anda em cada tick (blocos)
    private static final double[] ROLL_SPEED = {0.50, 0.58, 0.58, 0.54, 0.48, 0.40, 0.32, 0.24, 0.16, 0.10, 0.06};
    private static final double[] BACK_SPEED = {0.46, 0.42, 0.34, 0.24, 0.14, 0.07, 0.03};

    record Roll(long start, float yaw, boolean back, int length, Vec3 dir, boolean[] landed) {
        float u(long now) { return Mth.clamp((now - start) / (float) length, 0f, 1f); }
        boolean done(long now) { return now - start >= length; }
    }

    private static final Map<Integer, Roll> ROLLS = new HashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();
    private static long readyAt;

    private static long now() { return System.currentTimeMillis(); }

    private static Roll get(Player p) {
        Roll r = ROLLS.get(p.getId());
        if (r != null && r.done(now())) { ROLLS.remove(p.getId()); return null; }
        return r;
    }

    public static boolean rolling(Player p) { return get(p) != null; }

    // ------------------------------------------------------------------ curvas

    private static float smooth(float a) { a = Mth.clamp(a, 0f, 1f); return a * a * (3 - 2 * a); }

    // angulo da cambalhota (graus)
    private static float flip(float u) { return 360f * smooth((u - 0.06f) / 0.62f); }

    // encolhido (0..1): fecha rapido, abre no fim
    static float tuck(Roll r, long now) {
        float u = r.u(now);
        float k = Math.min(smooth(u / 0.14f), 1f - smooth((u - 0.62f) / 0.3f));
        return r.back ? k * 0.35f : k;
    }

    // ------------------------------------------------------------------ entrada (Q)

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) return;
        if (Screen.hasControlDown()) return;   // Ctrl+Q: soltar a pilha, normal
        long now = now();
        boolean wants = CombatEvents.weapon(p.getMainHandItem()) != CombatEvents.Weapon.NONE || CombatMoves.inCombat(p, now);
        if (!wants) return;
        boolean pressed = false;
        while (mc.options.keyDrop.consumeClick()) pressed = true;   // tira o clique antes do jogo soltar o item
        if (pressed) tryRoll(p, now);
    }

    private static void tryRoll(LocalPlayer p, long now) {
        if (now < readyAt || !p.onGround() || p.isInWater() || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying
                || p.onClimbable() || p.isUsingItem()) return;
        float fwd = p.input.forwardImpulse, left = p.input.leftImpulse;
        boolean back = fwd * fwd + left * left < 0.01f;
        float yawRad = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 dir;
        if (back) dir = new Vec3(Mth.sin(yawRad), 0, -Mth.cos(yawRad));   // para tras do olhar
        else {
            float f = Mth.sin(yawRad), g = Mth.cos(yawRad);
            dir = new Vec3(left * g - fwd * f, 0, fwd * g + left * f).normalize();
        }
        float yaw = back ? p.getYRot() : (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        int len = back ? BACK_MS : ROLL_MS;
        ROLLS.put(p.getId(), new Roll(now, yaw, back, len, dir, new boolean[1]));
        readyAt = now + len - 60;
        p.setSprinting(false);
        CombatMoves.cancel(p);
        p.level().playLocalSound(p.getX(), p.getY() + 0.5, p.getZ(), ModSounds.SWORD_SWING.get(), SoundSource.PLAYERS, 0.5f, 0.55f, false);
        dust(p, dir, 6);
        PacketDistributor.sendToServer(new RollPayload(yaw, back));
    }

    public static void receive(RollSeenPayload m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(m.entityId()) instanceof Player pl)) return;
        float r = m.yaw() * Mth.DEG_TO_RAD;
        Vec3 dir = m.back() ? new Vec3(Mth.sin(r), 0, -Mth.cos(r)) : new Vec3(-Mth.sin(r), 0, Mth.cos(r));
        ROLLS.put(m.entityId(), new Roll(now(), m.yaw(), m.back(), m.back() ? BACK_MS : ROLL_MS, dir, new boolean[1]));
        CombatMoves.cancel(pl);
        dust(pl, dir, 6);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { ROLLS.clear(); PUSHED.clear(); }

    // ------------------------------------------------------------------ movimento

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre e) {
        if (!(e.getEntity() instanceof LocalPlayer p) || p != Minecraft.getInstance().player) return;
        Roll r = get(p);
        if (r == null) return;
        long now = now();
        int i = (int) ((now - r.start) / 50);
        double[] sp = r.back ? BACK_SPEED : ROLL_SPEED;
        double s = i < sp.length ? sp[i] : 0;
        Vec3 d = p.getDeltaMovement();
        p.setDeltaMovement(r.dir.x * s, d.y, r.dir.z * s);
        p.setSprinting(false);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!rolling(e.getEntity())) return;
        var in = e.getInput();
        in.forwardImpulse = 0; in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false; in.shiftKeyDown = false;
    }

    // chegada (poeira e som de passo) — conferido a cada quadro, para todos os jogadores visiveis
    private static void landCheck(Player p, Roll r, long now) {
        if (r.landed[0] || r.u(now) < (r.back ? 0.5f : 0.66f)) return;
        r.landed[0] = true;
        dust(p, r.dir, r.back ? 5 : 10);
        BlockState st = p.level().getBlockState(p.blockPosition().below());
        SoundType t = st.getSoundType();
        p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), t.getStepSound(), SoundSource.PLAYERS, t.getVolume() * 0.9f, t.getPitch() * 0.85f, false);
    }

    private static void dust(Player p, Vec3 dir, int n) {
        BlockPos below = p.blockPosition().below();
        BlockState st = p.level().getBlockState(below);
        if (st.isAir()) return;
        var rnd = p.getRandom();
        for (int i = 0; i < n; i++)
            p.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st),
                    p.getX() + (rnd.nextDouble() - 0.5) * 0.6, p.getY() + 0.05, p.getZ() + (rnd.nextDouble() - 0.5) * 0.6,
                    -dir.x * 0.15 + (rnd.nextDouble() - 0.5) * 0.1, 0.08 + rnd.nextDouble() * 0.1, -dir.z * 0.15 + (rnd.nextDouble() - 0.5) * 0.1);
    }

    // ------------------------------------------------------------------ primeira pessoa

    // quanto a arma desce (para CombatMoves.onHand)
    static float handDip(Player p) {
        Roll r = get(p);
        return r == null ? 0f : tuck(r, now());
    }

    // camera: quanto abaixa (CameraRollMixin)
    public static float cameraDrop(Player p) {
        Roll r = get(p);
        if (r == null) return 0f;
        long now = now();
        landCheck(p, r, now);
        return r.back ? 0.18f * Mth.sin(Mth.PI * r.u(now)) : 0.95f * tuck(r, now);
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        Roll r = get(mc.player);
        if (r == null) return;
        float s = Mth.sin(Mth.PI * r.u(now()));
        float vy = (float) e.getYaw() * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(vy), 0, Mth.cos(vy)), right = new Vec3(-Mth.cos(vy), 0, -Mth.sin(vy));
        float f = (float) r.dir.dot(fwd), l = (float) r.dir.dot(right);
        if (r.back) { e.setPitch(e.getPitch() - 7f * s); return; }
        e.setPitch(e.getPitch() + 32f * f * s);
        e.setRoll(e.getRoll() + 18f * l * s);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Roll r = get(mc.player);
        if (r == null) return;
        e.setFOV(e.getFOV() * (1 + 0.07f * Mth.sin(Mth.PI * r.u(now()))));
    }

    // ------------------------------------------------------------------ terceira pessoa

    // corpo encolhido (chamado no fim do setupAnim do HumanoidModel, depois dos golpes)
    public static void posePlayer(HumanoidModel<?> m, Player p) {
        Roll r = get(p);
        if (r == null) return;
        float k = tuck(r, now());
        if (k <= 0.001f) return;
        m.head.xRot = Mth.lerp(k, m.head.xRot, 0.85f);
        m.head.yRot = Mth.lerp(k, m.head.yRot, 0f);
        m.body.xRot = Mth.lerp(k, m.body.xRot, 0.55f);
        m.body.yRot = Mth.lerp(k, m.body.yRot, 0f);
        m.rightArm.x = Mth.lerp(k, m.rightArm.x, -5f); m.rightArm.z = Mth.lerp(k, m.rightArm.z, 0f);
        m.leftArm.x = Mth.lerp(k, m.leftArm.x, 5f); m.leftArm.z = Mth.lerp(k, m.leftArm.z, 0f);
        // bracos abracando os joelhos
        m.rightArm.xRot = Mth.lerp(k, m.rightArm.xRot, -1.75f); m.rightArm.yRot = Mth.lerp(k, m.rightArm.yRot, 0.2f); m.rightArm.zRot = Mth.lerp(k, m.rightArm.zRot, -0.25f);
        m.leftArm.xRot = Mth.lerp(k, m.leftArm.xRot, -1.75f); m.leftArm.yRot = Mth.lerp(k, m.leftArm.yRot, -0.2f); m.leftArm.zRot = Mth.lerp(k, m.leftArm.zRot, 0.25f);
        // joelhos no peito
        m.rightLeg.xRot = Mth.lerp(k, m.rightLeg.xRot, -1.55f); m.rightLeg.yRot = Mth.lerp(k, m.rightLeg.yRot, 0f); m.rightLeg.zRot = Mth.lerp(k, m.rightLeg.zRot, 0.05f);
        m.leftLeg.xRot = Mth.lerp(k, m.leftLeg.xRot, -1.35f); m.leftLeg.yRot = Mth.lerp(k, m.leftLeg.yRot, 0f); m.leftLeg.zRot = Mth.lerp(k, m.leftLeg.zRot, -0.05f);
    }

    // cambalhota: vira para a direcao do rolamento e gira 360 em volta do meio do corpo encolhido, rente ao chao
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        Roll r = get(p);
        if (r == null) return;
        long now = now();
        landCheck(p, r, now);
        float u = r.u(now), k = tuck(r, now);
        float bodyYaw = Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot);
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(p.getId());
        if (r.back) {   // passo para tras: pulinho e corpo jogado para tras
            float s = Mth.sin(Mth.PI * u);
            ps.translate(0, 0.16f * s, 0);
            float br = bodyYaw * Mth.DEG_TO_RAD;
            ps.mulPose(new Quaternionf().rotationAxis(-18f * s * Mth.DEG_TO_RAD, Mth.cos(br), 0, Mth.sin(br)));
            return;
        }
        float pivot = 0.8f;
        ps.translate(0, -0.32f * k, 0);   // encolhido fica mais baixo
        ps.translate(0, pivot, 0);
        ps.mulPose(new Quaternionf().rotationAxis(flip(u) * Mth.DEG_TO_RAD, (float) r.dir.z, 0, (float) -r.dir.x));
        ps.translate(0, -pivot, 0);
        ps.mulPose(Axis.YP.rotationDegrees(bodyYaw - r.yaw));   // olha para onde rola (junta com o giro do proprio jogo)
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post e) {
        if (PUSHED.remove(e.getEntity().getId())) e.getPoseStack().popPose();
    }

    static float side(Player p) { return p.getMainArm() == HumanoidArm.RIGHT ? 1f : -1f; }
}

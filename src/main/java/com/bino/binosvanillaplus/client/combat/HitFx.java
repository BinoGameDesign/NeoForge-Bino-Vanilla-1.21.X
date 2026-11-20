package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.SwingPayload;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

// Mod COMBAT (cliente): o "peso" do golpe.
//  1) FLASH: quem toma dano fica todo branco por ~90 ms no instante do impacto (depois vem o vermelho normal).
//  4) RECUO: o corpo de quem apanhou da um tranco para tras (na direcao do golpe) e volta, em ~0,25 s.
//  5) CAMERA: quando VOCE acerta alguem, a camera trava ~50 ms (hitstop) e da uma tremidinha.
//  2) SONS do golpe no ar/na agua (os de acerto e de bloco vem do servidor, CombatEvents).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class HitFx {
    private HitFx() {}

    public static final long FLASH_MS = 90, RECOIL_MS = 260, STOP_MS = 50, SHAKE_MS = 170;
    public static final double RECOIL = 0.16;   // blocos

    private record Hit(long at, double dx, double dz) {}
    private static final Map<Integer, Hit> HITS = new HashMap<>();

    // chamado quando o cliente recebe o dano de uma entidade (mixin em LivingEntity.handleDamageEvent)
    public static void onHurt(LivingEntity e, DamageSource src) {
        Vec3 from = src.getSourcePosition() != null ? src.getSourcePosition() : src.getEntity() != null ? src.getEntity().position() : null;
        double dx = 0, dz = 0;
        if (from != null) {
            Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
            if (d.lengthSqr() > 1.0e-4) { d = d.normalize(); dx = d.x; dz = d.z; }
        }
        HITS.put(e.getId(), new Hit(Util.getMillis(), dx, dz));
        if (HITS.size() > 256) HITS.entrySet().removeIf(en -> Util.getMillis() - en.getValue().at() > 2000);
    }

    // flash branco agora?
    public static boolean flashing(LivingEntity e) {
        Hit h = HITS.get(e.getId());
        return h != null && Util.getMillis() - h.at() < FLASH_MS;
    }

    // ---------------------------------------------------------------- recuo (desenho deslocado, o corpo de verdade nao)
    // LOWEST: so roda se ninguem cancelou o desenho (senao o Post nao vem e a pilha ficaria torta)
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> e) {
        Hit h = HITS.get(e.getEntity().getId());
        double k = 0;
        if (h != null) {
            double u = (Util.getMillis() - h.at()) / (double) RECOIL_MS;
            if (u < 1) k = u < 0.25 ? u / 0.25 : Math.pow(1 - (u - 0.25) / 0.75, 2);   // vai rapido e volta devagar
        }
        e.getPoseStack().pushPose();
        if (k > 0) e.getPoseStack().translate(h.dx() * RECOIL * k, 0.02 * k, h.dz() * RECOIL * k);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> e) { e.getPoseStack().popPose(); }

    // ---------------------------------------------------------------- camera (hitstop + tremida)
    private static long kickAt = -100000;
    private static float kickYaw, kickPitch, kickPower;

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getEntity() != mc.player || !(e.getTarget() instanceof LivingEntity)) return;
        float charge = mc.player.getAttackStrengthScale(0.5f);
        kickAt = Util.getMillis();
        kickYaw = mc.player.getYRot();
        kickPitch = mc.player.getXRot();
        kickPower = 0.4f + 0.6f * charge;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        long dt = Util.getMillis() - kickAt;
        if (dt > SHAKE_MS + STOP_MS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        if (dt < STOP_MS) {   // travada: segura a camera onde estava no impacto
            e.setYaw(e.getYaw() + (kickYaw - mc.player.getYRot()));
            e.setPitch(e.getPitch() + (kickPitch - mc.player.getXRot()));
        }
        double k = Math.max(0, 1 - (dt) / (double) (SHAKE_MS + STOP_MS)) * kickPower;
        k *= k;
        double s = dt / 1000.0;
        e.setYaw((float) (e.getYaw() + Math.sin(s * 95) * 0.55 * k));
        e.setPitch((float) (e.getPitch() + Math.cos(s * 83) * 0.45 * k));
        e.setRoll((float) (e.getRoll() + Math.sin(s * 70 + 1) * 0.7 * k));
    }

    // ---------------------------------------------------------------- golpe no ar ou na agua
    @SubscribeEvent
    public static void onSwingEmpty(PlayerInteractEvent.LeftClickEmpty e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || e.getEntity() != p) return;
        CombatEvents.Weapon w = CombatEvents.weapon(p.getMainHandItem());
        if (w == CombatEvents.Weapon.NONE || p.getAttackStrengthScale(0.5f) < 0.3f) return;
        // a mira bate na agua dentro do alcance?
        double reach = p.entityInteractionRange();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(reach));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, p));
        boolean water = hit.getType() == HitResult.Type.BLOCK && p.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER);
        if (water) {
            Vec3 at = hit.getLocation();
            p.level().playLocalSound(at.x, at.y, at.z, ModSounds.HIT_WATER.get(), SoundSource.PLAYERS, 0.9f, 0.9f + p.getRandom().nextFloat() * 0.2f, false);
            for (int i = 0; i < 12; i++)
                p.level().addParticle(ParticleTypes.SPLASH, at.x + (p.getRandom().nextDouble() - 0.5) * 0.5, at.y + 0.1, at.z + (p.getRandom().nextDouble() - 0.5) * 0.5, 0, 0.2, 0);
            PacketDistributor.sendToServer(new SwingPayload(true, at.x, at.y, at.z));
        } else {
            p.level().playLocalSound(p.getX(), p.getEyeY(), p.getZ(), w == CombatEvents.Weapon.SWORD ? ModSounds.SWORD_SWING.get() : ModSounds.AXE_SWING.get(),
                    SoundSource.PLAYERS, 0.8f, 0.9f + p.getRandom().nextFloat() * 0.2f, false);
            PacketDistributor.sendToServer(new SwingPayload(false, 0, 0, 0));
        }
    }

}

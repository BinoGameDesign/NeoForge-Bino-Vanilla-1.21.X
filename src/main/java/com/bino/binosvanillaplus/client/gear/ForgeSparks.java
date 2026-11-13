package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Faiscas da forja +10 (junto com o brilho do ForgeGlow): faisquinhas na cor do item saindo da arma na mao
// e das pecas de armadura vestidas. So no cliente; aparecem em todos os jogadores por perto.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ForgeSparks {
    private ForgeSparks() {}

    private static final float HAND_CHANCE = 0.22f;    // por tick, para cada mao com item +10 (~4 faiscas/s)
    private static final float ARMOR_CHANCE = 0.08f;   // por tick, para cada peca +10 vestida

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        RandomSource rnd = mc.level.random;
        for (Player p : mc.level.players()) {
            if (p.isInvisible() || p.distanceToSqr(mc.player) > 48 * 48) continue;
            boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson();
            hand(mc, p, p.getMainHandItem(), p.getMainArm(), firstPerson, rnd);
            hand(mc, p, p.getOffhandItem(), p.getMainArm().getOpposite(), firstPerson, rnd);
            if (firstPerson) continue;   // a propria armadura em primeira pessoa nao aparece
            armor(mc, p, EquipmentSlot.HEAD, 1.45, 1.95, rnd);
            armor(mc, p, EquipmentSlot.CHEST, 0.75, 1.45, rnd);
            armor(mc, p, EquipmentSlot.LEGS, 0.35, 0.95, rnd);
            armor(mc, p, EquipmentSlot.FEET, 0.0, 0.4, rnd);
        }
    }

    private static boolean isTen(ItemStack s) { return ForgeGlow.tier(s) >= 4; }

    // ao longo da arma: a mao fica na lateral, na altura do quadril, e a arma aponta para a frente
    private static void hand(Minecraft mc, Player p, ItemStack stack, HumanoidArm arm, boolean firstPerson, RandomSource rnd) {
        if (!isTen(stack) || rnd.nextFloat() > HAND_CHANCE) return;
        Vec3 pos;
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        if (firstPerson) {
            // na frente da camera, no canto da mao
            Vec3 look = p.getViewVector(1f);
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize().scale(side);
            Vec3 up = right.cross(look).normalize().scale(side);
            double t = 0.4 + rnd.nextDouble() * 0.5;
            pos = p.getEyePosition().add(look.scale(0.55 + t * 0.4)).add(right.scale(0.42 - t * 0.12)).add(up.scale(-0.32 + t * 0.18));
        } else {
            float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(side);
            double t = rnd.nextDouble() * 0.8;
            pos = p.position().add(0, p.isCrouching() ? 0.55 : 0.7, 0).add(right.scale(0.38)).add(fwd.scale(0.1 + t))
                    .add((rnd.nextDouble() - 0.5) * 0.12, (rnd.nextDouble() - 0.5) * 0.12, (rnd.nextDouble() - 0.5) * 0.12);
        }
        spark(mc, stack, pos, rnd, firstPerson ? 0.35f : 0.6f);
    }

    private static void armor(Minecraft mc, Player p, EquipmentSlot slot, double y0, double y1, RandomSource rnd) {
        ItemStack stack = p.getItemBySlot(slot);
        if (!isTen(stack) || rnd.nextFloat() > ARMOR_CHANCE) return;
        double scale = p.getScale();
        Vec3 pos = p.position().add((rnd.nextDouble() - 0.5) * 0.7 * scale, (y0 + rnd.nextDouble() * (y1 - y0)) * scale,
                (rnd.nextDouble() - 0.5) * 0.7 * scale);
        spark(mc, stack, pos, rnd, 0.55f);
    }

    private static void spark(Minecraft mc, ItemStack stack, Vec3 pos, RandomSource rnd, float size) {
        Particle part = mc.particleEngine.createParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                (rnd.nextDouble() - 0.5) * 0.04, 0.02 + rnd.nextDouble() * 0.04, (rnd.nextDouble() - 0.5) * 0.04);
        if (part == null) return;
        int rgb = ForgeGlow.color(stack.getItem());
        part.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        part.setLifetime(8 + rnd.nextInt(8));
        part.scale(size);
    }
}

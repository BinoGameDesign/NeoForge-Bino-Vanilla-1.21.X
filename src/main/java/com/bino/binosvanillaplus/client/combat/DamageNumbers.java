package com.bino.binosvanillaplus.client.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.combat.CombatEvents;
import com.bino.binosvanillaplus.combat.DamageNumberPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Numeros de dano (mod Combat). Cada numero da um "pop", sobe fazendo um arco para um lado sorteado e some em fade.
// Tamanho pelo dano (com limites). Cores: fisico branco; fogo e raio amarelos; critico vermelho claro com "Critical" (branco)
// em cima; afogamento azul; veneno verde claro; "Block" e "Miss" cinza. Golpes muito rapidos no mesmo alvo SOMAM no
// numero que ja esta no ar (ele cresce e da outro pop).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class DamageNumbers {
    private DamageNumbers() {}

    private static final long LIFE_MS = 950, STACK_MS = 380, POP_MS = 140;
    private static final float MIN_SCALE = 0.020f, MAX_SCALE = 0.046f;
    private static final RandomSource RND = RandomSource.create();

    private static final class Num {
        final int entityId; final byte kind;
        boolean gray;
        float amount; long born, popAt;
        final Vec3 origin; final double side, up;
        Num(int id, byte kind, float amount, Vec3 origin) {
            this.entityId = id; this.kind = kind; this.amount = amount; this.origin = origin;
            this.born = this.popAt = Util.getMillis();
            this.side = (RND.nextDouble() - 0.5) * 1.6;   // para que lado faz o arco
            this.up = 0.75 + RND.nextDouble() * 0.35;
        }
    }

    private static final List<Num> NUMS = new ArrayList<>();

    public static void receive(DamageNumberPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p.entityId());
        if (e == null) return;
        long now = Util.getMillis();
        boolean stackable = p.kind() != CombatEvents.KIND_BLOCK && p.kind() != CombatEvents.KIND_MISS;
        if (stackable) {   // soma no numero recente do mesmo alvo e do mesmo tipo
            for (int i = NUMS.size() - 1; i >= 0; i--) {
                Num n = NUMS.get(i);
                if (n.entityId == p.entityId() && n.kind == p.kind() && now - n.popAt < STACK_MS) {
                    n.amount += p.amount();
                    n.popAt = now;
                    n.born = Math.max(n.born, now - 150);   // ganha mais um tempinho de vida
                    return;
                }
            }
        }
        // nasce acima da cabeca (antes ficava dentro do mob)
        Vec3 at = e.position().add((RND.nextDouble() - 0.5) * e.getBbWidth() * 0.5, e.getBbHeight() + 0.35, (RND.nextDouble() - 0.5) * e.getBbWidth() * 0.5);
        Num n = new Num(p.entityId(), p.kind(), p.amount(), at);
        // lugar branco (neve, gelo, quartzo...): o numero branco vira cinza para aparecer
        if (p.kind() == CombatEvents.KIND_PHYSICAL && whiteAround(mc.level, e.blockPosition())) n.gray = true;
        NUMS.add(n);
        if (NUMS.size() > 80) NUMS.remove(0);
    }

    private static boolean whiteAround(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level.getBiome(pos).value().coldEnoughToSnow(pos) && level.canSeeSky(pos)) return true;   // bioma de neve
        for (int dy = 0; dy >= -2; dy--) {
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(pos.offset(0, dy, 0));
            if (st.isAir()) continue;
            return st.is(net.minecraft.tags.BlockTags.SNOW) || st.is(net.minecraft.tags.BlockTags.ICE)
                    || st.is(net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK) || st.is(net.minecraft.world.level.block.Blocks.CALCITE)
                    || st.is(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE) || st.is(net.minecraft.world.level.block.Blocks.WHITE_WOOL)
                    || st.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW);
        }
        return false;
    }

    private static int color(byte k) {
        return switch (k) {
            case CombatEvents.KIND_FIRE -> 0xFFD447;
            case CombatEvents.KIND_LIGHTNING -> 0xFFF36B;
            case CombatEvents.KIND_CRIT -> 0xFF8A8A;
            case CombatEvents.KIND_DROWN -> 0x5AA9FF;
            case CombatEvents.KIND_POISON -> 0x9CFF7A;
            case CombatEvents.KIND_BLOCK, CombatEvents.KIND_MISS -> 0xA8A8A8;
            default -> 0xFFFFFF;
        };
    }

    private static String text(Num n) {
        if (n.kind == CombatEvents.KIND_BLOCK) return "Block";
        if (n.kind == CombatEvents.KIND_MISS) return "Miss";
        float a = n.amount;
        if (a >= 10 || Math.abs(a - Math.round(a)) < 0.05f) return String.valueOf(Math.round(a));
        return String.format(java.util.Locale.ROOT, "%.1f", a);
    }

    // tamanho pelo dano: cresce devagar (log) e fica entre os limites
    private static float scale(Num n) {
        if (n.kind == CombatEvents.KIND_BLOCK || n.kind == CombatEvents.KIND_MISS) return MIN_SCALE * 1.1f;
        float s = 0.019f + 0.0065f * (float) Math.log1p(n.amount);
        if (n.kind == CombatEvents.KIND_CRIT) s *= 1.15f;
        return Mth.clamp(s, MIN_SCALE, MAX_SCALE);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || NUMS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        long now = Util.getMillis();
        Vec3 right = new Vec3(cam.getLeftVector()).scale(-1);
        for (Iterator<Num> it = NUMS.iterator(); it.hasNext(); ) {
            Num n = it.next();
            double u = (now - n.born) / (double) LIFE_MS;
            if (u >= 1) { it.remove(); continue; }
            // arco: sobe e cai um pouco, indo para o lado
            double t = u * 1.6;
            Vec3 p = n.origin.add(right.scale(n.side * 0.6 * u)).add(0, n.up * t - 0.55 * t * t, 0);
            double pop = Mth.clamp((now - n.popAt) / (double) POP_MS, 0, 1);
            float popK = (float) (1 + 0.7 * Math.sin(Math.PI * Math.min(1, pop * 1.2)) * (1 - pop) + (pop < 1 ? 0.25 * (1 - pop) : 0));
            float alpha = u < 0.6 ? 1f : (float) (1 - (u - 0.6) / 0.4);
            int a = Mth.clamp((int) (alpha * 255), 8, 255);
            float s = scale(n) * popK;

            ps.pushPose();
            ps.translate(p.x - cp.x, p.y - cp.y, p.z - cp.z);
            ps.mulPose(cam.rotation());
            ps.scale(s, -s, s);   // igual a placa de nome do jogo (Y invertido)
            String str = text(n);
            int col = (a << 24) | (n.gray ? 0x7C7C7C : color(n.kind));
            float x = -font.width(str) / 2f;
            // sem contorno (so o texto, brilhando sozinho)
            font.drawInBatch(str, x, 0, col, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            if (n.kind == CombatEvents.KIND_CRIT) {   // "Critical" pequeno logo acima
                ps.pushPose();
                ps.translate(0, -7, 0);
                ps.scale(0.6f, 0.6f, 0.6f);
                String c = "Critical";
                font.drawInBatch(c, -font.width(c) / 2f, 0, (a << 24) | 0xFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                ps.popPose();
            }
            ps.popPose();
        }
        buf.endBatch();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { NUMS.clear(); }
}

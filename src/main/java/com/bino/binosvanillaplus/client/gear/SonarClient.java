package com.bino.binosvanillaplus.client.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import com.bino.binosvanillaplus.entity.GraveAllayEntity;
import com.bino.binosvanillaplus.gear.Dynamite;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.*;

// Sonar (lado do jogador): o pulso sai do jogador e cresce ate o alcance em ~1,2 s; tudo que a onda alcanca ganha
// contorno branco por alguns segundos, visivel atraves das paredes: minerios e fosseis (contorno so das faces de fora
// do veio) e criaturas (silhueta branca do jogo). O anel do pulso tambem e' desenhado no chao.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class SonarClient {
    private SonarClient() {}

    public static final int BLOCK_RANGE = 20, MOB_RANGE = 32;
    private static final long WAVE_MS = 1200, SHOW_MS = 6000;
    private static final int MAX_BLOCKS = 600;
    // blocos que o sonar acha (minerios + fosseis); editavel por datapack
    public static final TagKey<Block> DETECTABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "sonar_detectable"));

    private record Hit(BlockPos pos, double dist) {}

    private static Vec3 origin;
    private static long start = -1;
    private static List<Hit> hits = new ArrayList<>();
    private static Set<BlockPos> found = new HashSet<>();
    private static final List<int[]> pendingMobs = new ArrayList<>();   // {entityId, distancia}

    public static void pulse(Player player) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        origin = player.getEyePosition();
        start = Util.getMillis();
        BlockPos c = player.blockPosition();
        List<Hit> list = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r = BLOCK_RANGE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r * r) continue;
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir()) continue;
                    if (s.is(DETECTABLE) || Dynamite.isOre(s)) list.add(new Hit(m.immutable(), Math.sqrt(d2)));
                }
            }
        }
        list.sort(Comparator.comparingDouble(Hit::dist));
        if (list.size() > MAX_BLOCKS) list = new ArrayList<>(list.subList(0, MAX_BLOCKS));
        hits = list;
        found = new HashSet<>();
        for (Hit h : list) found.add(h.pos());

        pendingMobs.clear();
        int mobs = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MOB_RANGE))) {
            if (e == player || !e.isAlive() || e instanceof CompanionAllayEntity || e instanceof GraveAllayEntity) continue;
            pendingMobs.add(new int[]{e.getId(), (int) e.distanceTo(player)});
            mobs++;
        }
        int veins = countVeins(found);
        player.displayClientMessage(Component.translatable("message.binosvanillaplus.sonar.result", veins, mobs).withStyle(ChatFormatting.GRAY), true);
    }

    // quantos veios (grupos de blocos encostados)
    private static int countVeins(Set<BlockPos> set) {
        Set<BlockPos> seen = new HashSet<>();
        int n = 0;
        for (BlockPos p : set) {
            if (!seen.add(p)) continue;
            n++;
            Deque<BlockPos> q = new ArrayDeque<>();
            q.add(p);
            while (!q.isEmpty()) {
                BlockPos a = q.poll();
                for (Direction d : Direction.values()) {
                    BlockPos b = a.relative(d);
                    if (set.contains(b) && seen.add(b)) q.add(b);
                }
            }
        }
        return n;
    }

    private static float waveRadius(long now) {
        return Math.min(1f, (now - start) / (float) WAVE_MS) * MOB_RANGE;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || start < 0) return;
        long now = Util.getMillis();
        long age = now - start;
        if (age > WAVE_MS + SHOW_MS) { start = -1; return; }
        float wave = waveRadius(now);
        // criaturas: ganham a silhueta quando a onda chega nelas
        for (Iterator<int[]> it = pendingMobs.iterator(); it.hasNext(); ) {
            int[] e = it.next();
            if (e[1] <= wave) { ClientGlow.add(e[0], SHOW_MS - Math.max(0, age - WAVE_MS)); it.remove(); }
        }
        float fade = age < WAVE_MS + SHOW_MS - 1200 ? 1f : (WAVE_MS + SHOW_MS - age) / 1200f;
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f pose = event.getPoseStack().last().pose();

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        int a = (int) (230 * fade);
        boolean any = false;
        for (Hit h : hits) {
            if (h.dist() > wave * BLOCK_RANGE / (float) MOB_RANGE + 0.5f) break;   // a onda ainda nao chegou
            any |= faces(bb, pose, h.pos(), cam, a);
        }
        // anel do pulso (enquanto a onda cresce)
        if (age < WAVE_MS && origin != null) {
            int ra = (int) (200 * (1f - age / (float) WAVE_MS));
            float rr = wave;
            int seg = 64;
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                float x0 = (float) (origin.x + Math.cos(a0) * rr - cam.x), z0 = (float) (origin.z + Math.sin(a0) * rr - cam.z);
                float x1 = (float) (origin.x + Math.cos(a1) * rr - cam.x), z1 = (float) (origin.z + Math.sin(a1) * rr - cam.z);
                float y = (float) (origin.y - 1.4 - cam.y);
                line(bb, pose, x0, y, z0, x1, y, z1, ra);
            }
            any = true;
        }
        MeshData mesh = bb.build();
        if (!any || mesh == null) { if (mesh != null) mesh.close(); return; }
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(2.5f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();   // atraves das paredes
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(mesh);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1f);
    }

    // arestas das faces que dao para fora do veio (silhueta do veio)
    private static boolean faces(BufferBuilder bb, Matrix4f pose, BlockPos p, Vec3 cam, int a) {
        float x = (float) (p.getX() - cam.x), y = (float) (p.getY() - cam.y), z = (float) (p.getZ() - cam.z);
        boolean any = false;
        for (Direction d : Direction.values()) {
            if (found.contains(p.relative(d))) continue;
            any = true;
            float[][] q = quad(d, x, y, z);
            for (int i = 0; i < 4; i++) {
                float[] s = q[i], e = q[(i + 1) % 4];
                line(bb, pose, s[0], s[1], s[2], e[0], e[1], e[2], a);
            }
        }
        return any;
    }

    private static float[][] quad(Direction d, float x, float y, float z) {
        float X = x + 1, Y = y + 1, Z = z + 1;
        return switch (d) {
            case DOWN -> new float[][]{{x, y, z}, {X, y, z}, {X, y, Z}, {x, y, Z}};
            case UP -> new float[][]{{x, Y, z}, {X, Y, z}, {X, Y, Z}, {x, Y, Z}};
            case NORTH -> new float[][]{{x, y, z}, {X, y, z}, {X, Y, z}, {x, Y, z}};
            case SOUTH -> new float[][]{{x, y, Z}, {X, y, Z}, {X, Y, Z}, {x, Y, Z}};
            case WEST -> new float[][]{{x, y, z}, {x, y, Z}, {x, Y, Z}, {x, Y, z}};
            case EAST -> new float[][]{{X, y, z}, {X, y, Z}, {X, Y, Z}, {X, Y, z}};
        };
    }

    private static void line(BufferBuilder bb, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, int a) {
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-5f) return;
        nx /= len; ny /= len; nz /= len;
        bb.addVertex(pose, x0, y0, z0).setColor(255, 255, 255, a).setNormal(nx, ny, nz);
        bb.addVertex(pose, x1, y1, z1).setColor(255, 255, 255, a).setNormal(nx, ny, nz);
    }
}

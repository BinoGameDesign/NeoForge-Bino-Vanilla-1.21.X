package com.bino.binosvanillaplus.gear;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.FlashlightLightBlock;
import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.custom.FlashlightItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

// Luz da lanterna: a cada 3 ticks, raios saem dos olhos do jogador na direcao do olhar (um no centro e quatro um pouco
// abertos, para o feixe ser ligeiramente largo). Onde cada raio bate entra uma luz invisivel; no raio do centro tambem
// entram luzes mais fracas pelo caminho (o "corredor" iluminado). Nada acende atras ou do lado do jogador.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class FlashlightManager {
    private FlashlightManager() {}

    private static final double RANGE = 30.0;     // alcance do feixe, em blocos
    private static final double SPREAD = 0.17;    // abertura dos raios laterais (~10 graus)

    private record Lit(ResourceKey<Level> dim, Set<BlockPos> positions) {}

    private static final Map<UUID, Lit> ACTIVE = new HashMap<>();

    // alguma lanterna ainda ilumina este bloco?
    public static boolean wanted(ServerLevel level, BlockPos pos) {
        for (Lit l : ACTIVE.values()) if (l.dim() == level.dimension() && l.positions().contains(pos)) return true;
        return false;
    }

    public static boolean lampOn(ServerPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof FlashlightItem && FlashlightItem.isOn(s)) return true;
        }
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        return head.getOrDefault(ModDataComponents.HELMET_LAMP.get(), false) && head.getOrDefault(ModDataComponents.FLASHLIGHT_ON.get(), false);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 3 != 0) return;
        if (!p.isAlive() || p.isSpectator() || !lampOn(p)) { ACTIVE.remove(p.getUUID()); return; }
        update(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { ACTIVE.remove(event.getEntity().getUUID()); }

    private static void update(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();

        Map<BlockPos, Integer> want = new HashMap<>();
        cast(level, p, eye, look, 15, true, want);
        double[][] offs = {{SPREAD, 0}, {-SPREAD, 0}, {0, SPREAD * 0.8}, {0, -SPREAD * 0.8}};
        for (double[] o : offs) cast(level, p, eye, look.add(right.scale(o[0])).add(up.scale(o[1])).normalize(), 12, false, want);

        for (Map.Entry<BlockPos, Integer> e : want.entrySet()) place(level, e.getKey(), e.getValue());
        ACTIVE.put(p.getUUID(), new Lit(level.dimension(), new HashSet<>(want.keySet())));
    }

    private static void cast(ServerLevel level, ServerPlayer p, Vec3 eye, Vec3 dir, int light, boolean center, Map<BlockPos, Integer> want) {
        Vec3 end = eye.add(dir.scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
        double len = hit.getType() == HitResult.Type.MISS ? RANGE : hit.getLocation().distanceTo(eye) - 0.6;
        if (len < 1.2) return;   // colado na parede: o bloco do lado ja fica claro pelo vizinho
        put(level, eye, dir, len, light, want);
        if (center) {   // luz mais fraca pelo caminho, para o feixe iluminar o corredor
            for (double d = 7.0; d < len - 4.0; d += 7.0) put(level, eye, dir, d, 9, want);
        }
    }

    // acha um bloco livre perto do ponto (recuando pelo raio se cair dentro de planta/neve) e pede luz nele
    private static void put(ServerLevel level, Vec3 eye, Vec3 dir, double dist, int light, Map<BlockPos, Integer> want) {
        for (double back = 0; back <= 2.0 && dist - back > 0.8; back += 0.5) {
            BlockPos pos = BlockPos.containing(eye.add(dir.scale(dist - back)));
            if (pos.equals(BlockPos.containing(eye))) return;
            if (free(level.getBlockState(pos))) { want.merge(pos, light, Math::max); return; }
        }
    }

    private static boolean free(BlockState s) {
        return s.isAir() || s.getBlock() instanceof FlashlightLightBlock
                || (s.is(Blocks.WATER) && s.getFluidState().isSource());
    }

    private static void place(ServerLevel level, BlockPos pos, int light) {
        BlockState cur = level.getBlockState(pos);
        BlockState lamp = ModBlocks.FLASHLIGHT_LIGHT.get().defaultBlockState();
        if (cur.getBlock() instanceof FlashlightLightBlock) {
            if (cur.getValue(LightBlock.LEVEL) == light) return;
            level.setBlock(pos, cur.setValue(LightBlock.LEVEL, light), 3);
            return;
        }
        boolean water = cur.is(Blocks.WATER) && cur.getFluidState().is(Fluids.WATER);
        if (!cur.isAir() && !water) return;
        level.setBlock(pos, lamp.setValue(LightBlock.LEVEL, light).setValue(LightBlock.WATERLOGGED, water), 3);
    }
}

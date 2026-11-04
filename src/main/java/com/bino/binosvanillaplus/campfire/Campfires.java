package com.bino.binosvanillaplus.campfire;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.network.CampfireListPayload;
import com.bino.binosvanillaplus.pocket.PocketDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

// Fogueiras como ponto de retorno: agachar + botao direito numa fogueira acesa registra (ou tira) ela.
// Ao morrer, a tela de morte mostra as fogueiras para escolher onde renascer (alem da cama/spawn).
// Perto de uma fogueira acesa, longe de monstros que estejam atras de voce, a vida volta aos poucos.
// Nao funciona na Dimensao Portatil.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class Campfires {
    private Campfires() {}

    private static final int HEAL_RADIUS = 5, AGGRO_RADIUS = 16, HEAL_EVERY = 60;   // 1 coracao a cada 3 s
    private static final Map<UUID, Integer> CHOICE = new HashMap<>();             // jogador -> fogueira escolhida

    public static boolean isLitCampfire(BlockState s) {
        return s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT);
    }

    // ---------------------------------------------------------------- registrar

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        ServerLevel level = p.serverLevel();
        BlockPos pos = event.getPos();
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof CampfireBlock)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.dimension().equals(PocketDimension.LEVEL)) {
            p.displayClientMessage(Component.translatable("message.binosvanillaplus.campfire.pocket").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!s.getValue(CampfireBlock.LIT)) {
            p.displayClientMessage(Component.translatable("message.binosvanillaplus.campfire.unlit").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        CampfireData data = CampfireData.get(p.server);
        List<GlobalPos> list = data.of(p.getUUID());
        GlobalPos g = GlobalPos.of(level.dimension(), pos.immutable());
        if (list.remove(g)) {
            p.displayClientMessage(Component.translatable("message.binosvanillaplus.campfire.removed").withStyle(ChatFormatting.GRAY), true);
        } else {
            list.add(g);
            while (list.size() > CampfireData.MAX) list.remove(0);   // a mais antiga sai
            p.displayClientMessage(Component.translatable("message.binosvanillaplus.campfire.set", list.size(), CampfireData.MAX)
                    .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
        data.setDirty();
    }

    // ---------------------------------------------------------------- morrer / renascer

    // fogueira ainda serve? (existe, acesa, com lugar livre do lado)
    private static BlockPos spot(ServerPlayer p, GlobalPos g) {
        ServerLevel level = p.server.getLevel(g.dimension());
        if (level == null || !isLitCampfire(level.getBlockState(g.pos()))) return null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos f = g.pos().relative(d).above(dy);
                if (level.getBlockState(f).getCollisionShape(level, f).isEmpty()
                        && level.getBlockState(f.above()).getCollisionShape(level, f.above()).isEmpty()
                        && level.getBlockState(f.below()).isFaceSturdy(level, f.below(), Direction.UP)) return f;
            }
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isCanceled()) return;
        CHOICE.remove(p.getUUID());
        List<CampfireListPayload.Entry> out = new ArrayList<>();
        for (GlobalPos g : CampfireData.get(p.server).of(p.getUUID())) {
            out.add(new CampfireListPayload.Entry(g.dimension().location().toString(), g.pos(), spot(p, g) != null));
        }
        PacketDistributor.sendToPlayer(p, new CampfireListPayload(out));
    }

    public static void choose(ServerPlayer p, int index) {
        if (p.isAlive()) return;
        CHOICE.put(p.getUUID(), index);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isEndConquered()) return;
        Integer idx = CHOICE.remove(p.getUUID());
        if (idx == null || idx < 0) return;
        List<GlobalPos> list = CampfireData.get(p.server).of(p.getUUID());
        if (idx >= list.size()) return;
        GlobalPos g = list.get(idx);
        BlockPos at = spot(p, g);
        ServerLevel level = p.server.getLevel(g.dimension());
        if (at == null || level == null) {
            p.displayClientMessage(Component.translatable("message.binosvanillaplus.campfire.gone").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(g.pos().getX() - at.getX()), g.pos().getZ() - at.getZ()));   // olhando para a fogueira
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 10f);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { CHOICE.remove(event.getEntity().getUUID()); }

    // ---------------------------------------------------------------- cura perto da fogueira

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % HEAL_EVERY != 0) return;
        if (!p.isAlive() || p.getHealth() >= p.getMaxHealth() || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        if (level.dimension().equals(PocketDimension.LEVEL)) return;
        BlockPos c = p.blockPosition();
        BlockPos fire = null;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-HEAL_RADIUS, -2, -HEAL_RADIUS), c.offset(HEAL_RADIUS, 2, HEAL_RADIUS))) {
            if (isLitCampfire(level.getBlockState(q))) { fire = q.immutable(); break; }
        }
        if (fire == null) return;
        // monstro atras de voce = sem descanso
        List<Mob> hunters = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(AGGRO_RADIUS),
                m -> m instanceof Enemy && m.isAlive() && m.getTarget() == p);
        if (!hunters.isEmpty()) return;
        p.heal(2.0f);
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.9, p.getZ(), 1, 0.2, 0.1, 0.2, 0);
    }
}

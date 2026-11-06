package com.bino.binosvanillaplus.faction;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.entity.faction.PillagerGolem;
import com.bino.binosvanillaplus.entity.faction.VillagerGuard;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Aliancas e hostilidades das faccoes:
//  - ninguem ataca um aliado (mob do mesmo lado ou jogador da mesma faccao)
//  - golens de ferro atacam jogadores Pillager; illagers/ravagers atacam os guardas villager
//  - quem bate num jogador (ou e' atacado por ele) vira alvo dos aliados dele por perto
//  - villagers nao negociam com jogador Pillager
//  - no Posto Villager, o golem de ferro preso vira Golem Pillager
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class FactionAI {
    private FactionAI() {}

    public static final ResourceKey<Structure> VILLAGER_OUTPOST =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "villager_outpost"));

    private record Pending(ServerLevel level, IronGolem golem) {}
    private static final Deque<Pending> CAGED = new ArrayDeque<>();

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        // (nada de ler o mundo aqui: o evento acontece no meio do carregamento do chunk)
        if (ent instanceof IronGolem g && !(g instanceof PillagerGolem)) {
            g.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(g, Player.class, 10, true, false,
                    t -> t instanceof Player p && FactionData.get(p) == Faction.PILLAGER));
            g.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(g, PillagerGolem.class, true));
            if (!g.isPlayerCreated()) CAGED.add(new Pending((ServerLevel) e.getLevel(), g));
        }
        if ((ent instanceof AbstractIllager || ent instanceof Ravager) && !(ent instanceof VillagerGuard)) {
            Mob m = (Mob) ent;
            m.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(m, VillagerGuard.class, true));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        for (int i = 0; i < 32 && !CAGED.isEmpty(); i++) {
            Pending p = CAGED.poll();
            IronGolem g = p.golem();
            if (!g.isAlive() || g.isRemoved()) continue;
            ServerLevel level = p.level();
            if (!level.hasChunkAt(g.blockPosition())) continue;
            if (!level.structureManager().getStructureWithPieceAt(g.blockPosition(), h -> h.is(VILLAGER_OUTPOST)).isValid()) continue;
            PillagerGolem pg = ModEntities.PILLAGER_GOLEM.get().create(level);
            if (pg == null) continue;
            pg.moveTo(g.getX(), g.getY(), g.getZ(), g.getYRot(), g.getXRot());
            pg.setPersistenceRequired();
            g.discard();
            level.addFreshEntity(pg);
        }
    }

    // ninguem mira num aliado
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        LivingEntity t = e.getNewAboutToBeSetTarget();
        if (t == null) return;
        if (FactionData.allied(e.getEntity(), t)) e.setCanceled(true);
    }

    // aliados defendem e ajudam: quem machuca um jogador de faccao (ou apanha dele) vira alvo dos aliados por perto
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        Entity src = e.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker) || attacker == victim) return;
        if (victim instanceof ServerPlayer p) call(level, p, attacker);
        if (attacker instanceof ServerPlayer p) call(level, p, victim);
    }

    private static void call(ServerLevel level, ServerPlayer p, LivingEntity enemy) {
        Faction f = FactionData.get(p);
        if (f != Faction.VILLAGER && f != Faction.PILLAGER) return;
        if (FactionData.allied(p, enemy)) return;   // briga entre aliados: ninguem toma partido
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(p.blockPosition()).inflate(16),
                m -> m != enemy && m.isAlive() && FactionData.side(m) == f && !(m instanceof AbstractVillager))) {
            if (m.getTarget() == null || !m.getTarget().isAlive()) m.setTarget(enemy);
        }
    }

    // villagers (e o mercador) nao negociam com jogador Pillager
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof AbstractVillager v)) return;
        if (FactionData.get(e.getEntity()) != Faction.PILLAGER) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(e.getLevel().isClientSide()));
        if (!e.getLevel().isClientSide()) {
            if (v instanceof Villager villager) villager.setUnhappyCounter(40);
            v.playSound(SoundEvents.VILLAGER_NO, 1f, v.getVoicePitch());
        }
    }
}

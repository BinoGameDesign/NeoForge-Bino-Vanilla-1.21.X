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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import java.util.ArrayList;
import com.bino.binosvanillaplus.entity.faction.PillagerSmuggler;
import com.bino.binosvanillaplus.entity.faction.VillagerMage;
import com.bino.binosvanillaplus.entity.faction.VillagerVex;
import com.bino.binosvanillaplus.network.CivilianSyncPayload;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

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
    private static final Deque<Pending> BUILT = new ArrayDeque<>();
    private record Pumpkin(ServerLevel level, BlockPos pos, Faction faction, long time) {}
    private static final List<Pumpkin> PUMPKINS = new ArrayList<>();
    private record Outpost(ServerLevel level, long key, BoundingBox box) {}
    private static final Deque<Outpost> OUTPOSTS = new ArrayDeque<>();
    // mobs que nasceram com a estrutura: conferir se estao numa estrutura espelhada (no proximo tick)
    private record Born(ServerLevel level, Mob mob) {}
    private static final Deque<Born> BORN = new ArrayDeque<>();
    public static final net.minecraft.tags.TagKey<Structure> PILLAGER_VILLAGES =
            net.minecraft.tags.TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "pillager_villages"));
    public static final ResourceKey<Structure> VILLAGER_MANSION =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "villager_mansion"));
    public static final ResourceKey<Structure> VILLAGER_WITCH_HUT =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "villager_witch_hut"));
    private record Spirit(ServerLevel level, Vec3 pos, VillagerMage owner, BlockPos origin) {}
    private static final Deque<Spirit> SPIRITS = new ArrayDeque<>();

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
            else BUILT.add(new Pending((ServerLevel) e.getLevel(), g));   // golem feito por jogador: ve quem colocou a abobora
        }
        // villager novo (ainda nao conferido), evocador/vingador/bruxa do jogo: ver se nasceram numa estrutura espelhada
        if ((ent instanceof Villager v && !v.getData(com.bino.binosvanillaplus.leveling.ModAttachments.FACTION_CHECKED))
                || ent.getType() == EntityType.EVOKER || ent.getType() == EntityType.VINDICATOR || ent.getType() == EntityType.WITCH)
            BORN.add(new Born((ServerLevel) e.getLevel(), (Mob) ent));
        // vex invocado pelo Mago Villager: vira Espirito Villager (troca no proximo tick)
        if (ent.getType() == EntityType.VEX && ent instanceof Vex v && v.getOwner() instanceof VillagerMage mage) {
            e.setCanceled(true);
            SPIRITS.add(new Spirit((ServerLevel) e.getLevel(), v.position(), mage, v.getBoundOrigin()));
            return;
        }
        // illagers, ravagers, bruxas e vex do jogo tambem atacam os lutadores novos do lado villager
        if ((ent instanceof Raider || ent instanceof Vex) && !(ent instanceof com.bino.binosvanillaplus.faction.FactionMob)) {
            Mob m = (Mob) ent;
            m.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(m, LivingEntity.class, 10, true, false, FactionData::villagerFighter));
        }
    }

    // o cliente precisa saber quais villagers sao Pillagers civis (textura)
    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking e) {
        if (FactionData.isCivilian(e.getTarget()) && e.getEntity() instanceof ServerPlayer p)
            PacketDistributor.sendToPlayer(p, new CivilianSyncPayload(e.getTarget().getId(), true));
    }

    // villager civil que vira zumbi (e volta curado) continua Pillager civil
    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Post e) {
        if (!e.getEntity().getData(com.bino.binosvanillaplus.leveling.ModAttachments.PILLAGER_CIVILIAN)) return;
        if (e.getOutcome() instanceof AbstractVillager) FactionData.setCivilian(e.getOutcome(), true);
        else e.getOutcome().setData(com.bino.binosvanillaplus.leveling.ModAttachments.PILLAGER_CIVILIAN, true);   // o zumbi guarda a marca
    }

    // quem colocou a abobora (para saber se o golem construido e' do jogador Pillager)
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        BlockState st = e.getPlacedBlock();
        if (st.is(Blocks.CARVED_PUMPKIN) || st.is(Blocks.JACK_O_LANTERN))
            PUMPKINS.add(new Pumpkin((ServerLevel) p.level(), e.getPos().immutable(), FactionData.get(p), p.level().getGameTime()));
    }

    // chunk com o inicio de um Posto Villager: arruma o posto (bandeiras e guardas) no proximo tick
    @SubscribeEvent
    public static void onChunk(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(VILLAGER_OUTPOST);
        if (s == null) return;
        StructureStart start = chunk.getStartForStructure(s);
        if (start != null && start.isValid()) OUTPOSTS.add(new Outpost(level, chunk.getPos().toLong(), start.getBoundingBox()));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        // golens construidos: virou Golem Pillager se a abobora foi colocada por um jogador Pillager
        for (int i = 0; i < 8 && !BUILT.isEmpty(); i++) {
            Pending p = BUILT.poll();
            IronGolem g = p.golem();
            if (!g.isAlive() || g instanceof PillagerGolem) continue;
            Pumpkin best = null;
            for (Pumpkin pk : PUMPKINS)
                if (pk.level() == p.level() && pk.pos().distSqr(g.blockPosition()) <= 16 && p.level().getGameTime() - pk.time() <= 3) best = pk;
            if (best == null || best.faction() != Faction.PILLAGER) continue;
            PillagerGolem pg = ModEntities.PILLAGER_GOLEM.get().create(p.level());
            if (pg == null) continue;
            pg.moveTo(g.getX(), g.getY(), g.getZ(), g.getYRot(), g.getXRot());
            pg.setPlayerCreated(true);
            g.discard();
            p.level().addFreshEntity(pg);
        }
        PUMPKINS.removeIf(pk -> pk.level().getGameTime() - pk.time() > 5);
        // estruturas espelhadas: Vila Pillager (villagers viram Pillagers civis), Mansao Villager (evocador -> Mago,
        // vingador -> Vingador Villager) e Cabana da Bruxa Villager (bruxa -> Bruxa Villager)
        for (int i = 0; i < 64 && !BORN.isEmpty(); i++) {
            Born b = BORN.poll();
            Mob m = b.mob();
            ServerLevel level = b.level();
            if (!m.isAlive() || m.isRemoved() || !level.hasChunkAt(m.blockPosition())) continue;
            var sm = level.structureManager();
            BlockPos pos = m.blockPosition();
            if (m instanceof Villager v) {
                v.setData(com.bino.binosvanillaplus.leveling.ModAttachments.FACTION_CHECKED, true);
                if (!FactionData.isCivilian(v) && sm.getStructureWithPieceAt(pos, h -> h.is(PILLAGER_VILLAGES)).isValid())
                    FactionData.setCivilian(v, true);
            } else if (m.getType() == EntityType.WITCH) {
                if (sm.getStructureWithPieceAt(pos, h -> h.is(VILLAGER_WITCH_HUT)).isValid()) swap(level, m, ModEntities.VILLAGER_WITCH.get());
            } else if (sm.getStructureWithPieceAt(pos, h -> h.is(VILLAGER_MANSION)).isValid()) {
                swap(level, m, m.getType() == EntityType.EVOKER ? ModEntities.VILLAGER_MAGE.get() : ModEntities.VILLAGER_GUARD.get());
            }
        }
        // espiritos do Mago Villager
        while (!SPIRITS.isEmpty()) {
            Spirit sp = SPIRITS.poll();
            if (!sp.owner().isAlive()) continue;
            VillagerVex vx = ModEntities.VILLAGER_VEX.get().create(sp.level());
            if (vx == null) continue;
            vx.moveTo(sp.pos().x, sp.pos().y, sp.pos().z, 0f, 0f);
            vx.finalizeSpawn(sp.level(), sp.level().getCurrentDifficultyAt(BlockPos.containing(sp.pos())), MobSpawnType.MOB_SUMMONED, null);
            vx.setOwner(sp.owner());
            vx.setBoundOrigin(sp.origin());
            vx.setLimitedLife(20 * (30 + sp.level().random.nextInt(90)));
            sp.level().addFreshEntity(vx);
        }
        // Contrabandista Pillager: de tempos em tempos perto de um jogador Pillager
        ServerLevel ow = e.getServer().overworld();
        if (ow.getGameTime() % 6000 == 0 && ow.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING))
            for (ServerPlayer pl : ow.players())
                if (!pl.isSpectator() && FactionData.get(pl) == Faction.PILLAGER && ow.random.nextFloat() < 0.2f) spawnSmuggler(ow, pl);
        // postos villager: uma vez por posto, quando todos os chunks dele estiverem carregados
        for (int i = 0, n = Math.min(4, OUTPOSTS.size()); i < n; i++) {
            Outpost o = OUTPOSTS.poll();
            FactionStructures data = FactionStructures.get(o.level());
            if (data.done.contains(o.key())) continue;
            BoundingBox b = o.box();
            if (!o.level().hasChunksAt(b.minX(), b.minZ(), b.maxX(), b.maxZ())) { OUTPOSTS.add(o); continue; }
            setupOutpost(o.level(), b);
            data.mark(o.key());
        }
        for (int i = 0; i < 32 && !CAGED.isEmpty(); i++) {
            Pending p = CAGED.poll();
            IronGolem g = p.golem();
            if (!g.isAlive() || g.isRemoved()) continue;
            ServerLevel level = p.level();
            if (!level.hasChunkAt(g.blockPosition())) continue;
            boolean caged = level.structureManager().getStructureWithPieceAt(g.blockPosition(), h -> h.is(VILLAGER_OUTPOST)).isValid();
            if (!caged && !level.structureManager().getStructureWithPieceAt(g.blockPosition(), h -> h.is(PILLAGER_VILLAGES)).isValid()
                    && !civilianTown(level, g)) continue;
            PillagerGolem pg = ModEntities.PILLAGER_GOLEM.get().create(level);
            if (pg == null) continue;
            pg.moveTo(g.getX(), g.getY(), g.getZ(), g.getYRot(), g.getXRot());
            pg.setPersistenceRequired();
            g.discard();
            level.addFreshEntity(pg);
        }
    }

    // troca um mob do jogo pela versao espelhada, no mesmo lugar (fica permanente, como os da estrutura)
    private static void swap(ServerLevel level, Mob old, EntityType<? extends Mob> type) {
        Mob n = type.create(level);
        if (n == null) return;
        n.moveTo(old.getX(), old.getY(), old.getZ(), old.getYRot(), old.getXRot());
        n.finalizeSpawn(level, level.getCurrentDifficultyAt(old.blockPosition()), MobSpawnType.STRUCTURE, null);
        n.setPersistenceRequired();
        old.discard();
        level.addFreshEntity(n);
    }

    // golem nascido no meio de Pillagers civis (a vila deles chamou um golem): vira Golem Pillager
    private static boolean civilianTown(ServerLevel level, IronGolem g) {
        int civ = 0, vil = 0;
        for (Villager v : level.getEntitiesOfClass(Villager.class, g.getBoundingBox().inflate(16)))
            if (FactionData.isCivilian(v)) civ++; else vil++;
        return civ > vil;
    }

    private static void spawnSmuggler(ServerLevel level, ServerPlayer p) {
        if (!level.getEntitiesOfClass(PillagerSmuggler.class, p.getBoundingBox().inflate(64)).isEmpty()) return;
        for (int tries = 0; tries < 10; tries++) {
            int x = p.getBlockX() + (level.random.nextBoolean() ? 1 : -1) * (10 + level.random.nextInt(14));
            int z = p.getBlockZ() + (level.random.nextBoolean() ? 1 : -1) * (10 + level.random.nextInt(14));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isSolid() || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
            PillagerSmuggler sm = ModEntities.PILLAGER_SMUGGLER.get().create(level);
            if (sm == null) return;
            sm.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0f);
            sm.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            sm.setDespawnDelay(48000);
            sm.setWanderTarget(p.blockPosition());
            sm.restrictTo(p.blockPosition(), 16);
            level.addFreshEntity(sm);
            return;
        }
    }

    // bandeiras com as cores da vila e alguns guardas
    private static void setupOutpost(ServerLevel level, BoundingBox b) {
        BannerPatternLayers layers = villagerBanner(level);
        for (BlockPos pos : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            BlockState old = level.getBlockState(pos);
            BlockState ns;
            if (old.getBlock() instanceof WallBannerBlock) ns = Blocks.BROWN_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, old.getValue(WallBannerBlock.FACING));
            else if (old.getBlock() instanceof BannerBlock) ns = Blocks.BROWN_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, old.getValue(BannerBlock.ROTATION));
            else continue;
            BlockPos at = pos.immutable();
            level.setBlock(at, ns, 2);
            if (level.getBlockEntity(at) instanceof BannerBlockEntity be) {
                ItemStack st = new ItemStack(Items.BROWN_BANNER);
                st.set(DataComponents.BANNER_PATTERNS, layers);
                be.fromItem(st, DyeColor.BROWN);
                be.setChanged();
                level.sendBlockUpdated(at, ns, ns, 3);
            }
        }
        int count = 3 + level.random.nextInt(3);
        for (int i = 0, tries = 0; i < count && tries < 40; tries++) {
            int x = b.minX() + level.random.nextInt(Math.max(1, b.getXSpan()));
            int z = b.minZ() + level.random.nextInt(Math.max(1, b.getZSpan()));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isSolid() || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
            VillagerGuard guard = ModEntities.VILLAGER_GUARD.get().create(level);
            if (guard == null) continue;
            guard.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0f);
            guard.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            guard.setPersistenceRequired();
            level.addFreshEntity(guard);
            i++;
        }
    }

    // bandeira da vila: marrom com losango verde-limao e centro verde (cores dos villagers e da esmeralda)
    private static BannerPatternLayers villagerBanner(ServerLevel level) {
        var reg = level.registryAccess().registryOrThrow(Registries.BANNER_PATTERN);
        return new BannerPatternLayers.Builder()
                .add(reg.getHolderOrThrow(BannerPatterns.STRIPE_BOTTOM), DyeColor.GREEN)
                .add(reg.getHolderOrThrow(BannerPatterns.RHOMBUS_MIDDLE), DyeColor.LIME)
                .add(reg.getHolderOrThrow(BannerPatterns.CIRCLE_MIDDLE), DyeColor.GREEN)
                .add(reg.getHolderOrThrow(BannerPatterns.BORDER), DyeColor.BROWN)
                .build();
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
        // villager nao negocia com Pillager; Pillager civil e contrabandista nao negociam com Villager
        if (!FactionData.get(e.getEntity()).rival(FactionData.side(v))) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(e.getLevel().isClientSide()));
        if (!e.getLevel().isClientSide()) {
            if (v instanceof Villager villager) villager.setUnhappyCounter(40);
            v.playSound(SoundEvents.VILLAGER_NO, 1f, v.getVoicePitch());
        }
    }
}

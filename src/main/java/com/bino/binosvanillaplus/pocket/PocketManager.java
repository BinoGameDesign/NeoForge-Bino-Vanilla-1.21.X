package com.bino.binosvanillaplus.pocket;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.DimensionalDoorBlock;
import com.bino.binosvanillaplus.item.custom.DimensionalKeyItem;
import com.bino.binosvanillaplus.network.PocketVisitRequestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Cerebro da Dimensao Portatil (so no servidor):
//  - a chave cria a porta; a porta leva o dono para a sala dele; outros jogadores PEDEM e o dono aceita ou recusa;
//  - quem esta dentro nunca fica preso: porta quebrada, dono desconectado, morte, reinicio do servidor ou porta
//    sumida mandam o jogador de volta (conferencia a cada 1 s e ao entrar no jogo).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class PocketManager {
    private PocketManager() {}

    private static final long REQUEST_TICKS = 20L * 30L;   // o convite vale 30 s

    private record Pending(UUID visitor, UUID owner, PocketData.Return ret, long expireTick) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();   // visitante -> pedido

    private static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("message.binosvanillaplus.pocket_" + key, args), true);
    }

    @Nullable
    private static ServerPlayer player(MinecraftServer server, UUID id) { return server.getPlayerList().getPlayer(id); }

    // ================================================================ chave: cria a porta

    public static InteractionResult placeDoor(ServerPlayer player, ServerLevel level, BlockPos clicked, Direction face, ItemStack key) {
        if (PocketDimension.is(level)) { msg(player, "key_inside"); return InteractionResult.FAIL; }

        BlockPos lower = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        Direction facing = player.getDirection().getOpposite();    // a frente da porta olha para o jogador
        BlockPos front = lower.relative(facing);
        boolean ok = level.isInWorldBounds(lower) && lower.getY() + 1 < level.getMaxBuildHeight()
                && level.getBlockState(lower).canBeReplaced() && level.getBlockState(lower.above()).canBeReplaced()
                && level.getBlockState(lower.below()).isFaceSturdy(level, lower.below(), Direction.UP)
                && level.getBlockState(front).getCollisionShape(level, front).isEmpty()
                && level.getBlockState(front.above()).getCollisionShape(level, front.above()).isEmpty();
        if (!ok) { msg(player, "no_space"); return InteractionResult.FAIL; }

        MinecraftServer server = level.getServer();
        PocketData data = PocketData.get(server);
        PocketData.Plot plot = data.plotOrCreate(player.getUUID());
        int keyLevel = DimensionalKeyItem.level(key);
        if (keyLevel > plot.level) plot.level = keyLevel;           // a sala cresce com a chave

        // so existe UMA porta por dono: a antiga some (e quem estava dentro volta para o mundo)
        removeDoorBlocks(server, plot);

        DimensionalDoorBlock.placeAt(level, lower, facing);
        plot.doorDim = level.dimension();
        plot.doorPos = lower.immutable();
        data.setDirty();

        level.playSound(null, lower, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6f, 1.3f);
        level.sendParticles(ParticleTypes.PORTAL, lower.getX() + 0.5, lower.getY() + 1.0, lower.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.3);
        msg(player, "door_placed", PocketDimension.sizeFor(plot.level), PocketDimension.sizeFor(plot.level));
        return InteractionResult.SUCCESS;
    }

    private static void removeDoorBlocks(MinecraftServer server, PocketData.Plot plot) {
        if (plot.doorPos == null || plot.doorDim == null) return;
        ServerLevel old = server.getLevel(plot.doorDim);
        if (old == null) return;
        if (old.getBlockState(plot.doorPos).getBlock() instanceof DimensionalDoorBlock) {
            old.removeBlock(plot.doorPos, false);       // onRemove -> onDoorRemoved manda os de dentro para fora
            old.removeBlock(plot.doorPos.above(), false);
        }
        plot.doorDim = null;
        plot.doorPos = null;
    }

    // sincroniza o nivel do dono quando ele fabrica um upgrade da chave
    public static void keyCrafted(ServerPlayer player, ItemStack key) {
        int lvl = DimensionalKeyItem.level(key);
        if (lvl <= 0) return;
        PocketData data = PocketData.get(player.server);
        PocketData.Plot plot = data.plotOrCreate(player.getUUID());
        if (lvl > plot.level) { plot.level = lvl; data.setDirty(); }
    }

    // ================================================================ porta: entrar / pedir / sair

    public static void useDoor(ServerPlayer player, ServerLevel level, BlockPos lower, BlockState lowerState) {
        if (PocketDimension.is(level)) { exit(player, null); return; }     // porta de saida

        PocketData data = PocketData.get(level.getServer());
        PocketData.Plot plot = data.plotByDoor(level.dimension(), lower);
        if (plot == null) { msg(player, "orphan"); return; }

        Direction facing = lowerState.getBlock() instanceof DimensionalDoorBlock
                ? lowerState.getValue(DimensionalDoorBlock.FACING) : Direction.NORTH;
        BlockPos front = lower.relative(facing);
        boolean frontFree = level.getBlockState(front).getCollisionShape(level, front).isEmpty();
        BlockPos at = frontFree ? front : lower;
        PocketData.Return ret = new PocketData.Return(level.dimension(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, facing.toYRot());

        if (plot.owner.equals(player.getUUID())) enter(player, plot, ret);
        else request(player, plot, ret);
    }

    private static void enter(ServerPlayer player, PocketData.Plot plot, PocketData.Return ret) {
        MinecraftServer server = player.server;
        ServerLevel pocket = PocketDimension.get(server);
        if (pocket == null) { msg(player, "unavailable"); return; }
        PocketData data = PocketData.get(server);
        PocketRooms.ensure(pocket, plot);
        data.setReturn(player.getUUID(), ret);
        data.setDirty();
        Vec3 e = PocketDimension.entrance(plot.index, plot.level);
        teleport(player, pocket, e, 0f);
        pocket.playSound(null, e.x, e.y, e.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    // sai da dimensao para onde entrou. reason = chave de mensagem (null = saida normal)
    public static void exit(ServerPlayer player, @Nullable String reason) {
        MinecraftServer server = player.server;
        PocketData data = PocketData.get(server);
        PocketData.Return r = data.removeReturn(player.getUUID());
        ServerLevel to = r == null ? null : server.getLevel(r.dim());
        Vec3 pos;
        float yaw;
        if (r != null && to != null) {
            pos = new Vec3(r.x(), r.y(), r.z());
            yaw = r.yaw();
        } else {
            to = server.overworld();                                   // sem retorno guardado: spawn do mundo
            BlockPos sp = to.getSharedSpawnPos();
            BlockPos top = to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, sp);
            pos = Vec3.atBottomCenterOf(top);
            yaw = player.getYRot();
        }
        teleport(player, to, pos, yaw);
        to.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.2f);
        if (reason != null) msg(player, reason);
    }

    private static void teleport(ServerPlayer p, ServerLevel to, Vec3 pos, float yaw) {
        if (p.serverLevel() == to) {
            p.connection.teleport(pos.x, pos.y, pos.z, yaw, 0f);
        } else {
            p.changeDimension(new DimensionTransition(to, pos, Vec3.ZERO, yaw, 0f, DimensionTransition.DO_NOTHING));
        }
        p.fallDistance = 0f;
    }

    // ================================================================ convites

    private static void request(ServerPlayer visitor, PocketData.Plot plot, PocketData.Return ret) {
        MinecraftServer server = visitor.server;
        ServerPlayer owner = player(server, plot.owner);
        if (owner == null) { msg(visitor, "owner_offline"); return; }
        long now = server.overworld().getGameTime();
        Pending old = PENDING.get(visitor.getUUID());
        if (old != null && old.expireTick() > now) { msg(visitor, "request_pending", owner.getName()); return; }
        PENDING.put(visitor.getUUID(), new Pending(visitor.getUUID(), plot.owner, ret, now + REQUEST_TICKS));
        PacketDistributor.sendToPlayer(owner, new PocketVisitRequestPayload(visitor.getUUID(), visitor.getName().getString()));
        msg(visitor, "request_sent", owner.getName());
    }

    // resposta do dono ao menu
    public static void reply(ServerPlayer owner, UUID visitorId, boolean accept) {
        MinecraftServer server = owner.server;
        Pending pd = PENDING.get(visitorId);
        if (pd == null || !pd.owner().equals(owner.getUUID())) return;   // sem pedido (ou nao e para este dono)
        PENDING.remove(visitorId);
        ServerPlayer visitor = player(server, visitorId);
        if (visitor == null || !visitor.isAlive()) return;
        if (!accept) { msg(visitor, "denied", owner.getName()); return; }
        if (server.overworld().getGameTime() > pd.expireTick()) { msg(visitor, "expired"); return; }
        PocketData data = PocketData.get(server);
        PocketData.Plot plot = data.plotOf(owner.getUUID());
        if (plot == null || plot.doorPos == null) { msg(visitor, "orphan"); return; }
        enter(visitor, plot, pd.ret());
        msg(owner, "visitor_in", visitor.getName());
    }

    // ================================================================ porta removida

    public static void onDoorRemoved(ServerLevel level, BlockPos lower) {
        if (PocketDimension.is(level)) return;       // a porta de saida e reposta na conferencia de 1 s
        MinecraftServer server = level.getServer();
        PocketData data = PocketData.get(server);
        PocketData.Plot plot = data.plotByDoor(level.dimension(), lower);
        if (plot == null) return;
        plot.doorDim = null;
        plot.doorPos = null;
        data.setDirty();
        PENDING.values().removeIf(p -> p.owner().equals(plot.owner));
        ejectAll(server, plot, "door_broken", null);
    }

    // manda para fora todos que estao na sala deste lote (menos 'except')
    private static void ejectAll(MinecraftServer server, PocketData.Plot plot, String reason, @Nullable UUID except) {
        ServerLevel pocket = PocketDimension.get(server);
        if (pocket == null) return;
        for (ServerPlayer sp : new ArrayList<>(pocket.players())) {
            if (except != null && sp.getUUID().equals(except)) continue;
            if (PocketDimension.indexAt(sp.getX()) == plot.index) exit(sp, reason);
        }
    }

    // ================================================================ conferencia: ninguem fica preso

    private static boolean doorExists(MinecraftServer server, PocketData.Plot plot, boolean load) {
        if (plot.doorPos == null || plot.doorDim == null) return false;
        ServerLevel dl = server.getLevel(plot.doorDim);
        if (dl == null) return false;
        if (!load && !dl.isLoaded(plot.doorPos)) return true;       // longe e descarregado: nao da para saber, segue
        return dl.getBlockState(plot.doorPos).getBlock() instanceof DimensionalDoorBlock;
    }

    // confere um jogador que esta na dimensao; manda para fora se nao puder ficar
    private static void validate(ServerPlayer p, boolean load) {
        MinecraftServer server = p.server;
        ServerLevel pocket = PocketDimension.get(server);
        if (pocket == null || !PocketDimension.is(p.level())) return;
        PocketData data = PocketData.get(server);
        PocketData.Plot plot = data.plotByIndex(PocketDimension.indexAt(p.getX()));
        if (plot == null) { exit(p, "door_broken"); return; }
        if (!doorExists(server, plot, load)) {
            plot.doorDim = null;
            plot.doorPos = null;
            data.setDirty();
            exit(p, "door_broken");
            return;
        }
        // visitante so fica enquanto o dono estiver conectado
        if (!plot.owner.equals(p.getUUID()) && player(server, plot.owner) == null) { exit(p, "owner_left"); return; }
        if (PocketRooms.ensure(pocket, plot)) data.setDirty();   // repoe a porta de saida e cresce a sala se a chave subiu de nivel
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel pocket = PocketDimension.get(server);
        if (pocket != null) {
            for (ServerPlayer sp : new ArrayList<>(pocket.players())) validate(sp, false);
        }
        if (!PENDING.isEmpty()) {
            long now = server.overworld().getGameTime();
            Iterator<Pending> it = PENDING.values().iterator();
            while (it.hasNext()) {
                Pending pd = it.next();
                if (now > pd.expireTick() + 20) {
                    it.remove();
                    ServerPlayer v = player(server, pd.visitor());
                    if (v != null) msg(v, "expired");
                }
            }
        }
    }

    // entrou no jogo (inclui depois de reiniciar o servidor): se estava dentro, confere tudo; se nao, limpa o retorno antigo
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (PocketDimension.is(sp.level())) validate(sp, true);
        else PocketData.get(sp.server).removeReturn(sp.getUUID());
    }

    // renasceu: se nao foi parar na dimensao, esquece o retorno; se foi (cama la dentro), confere
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (PocketDimension.is(sp.level())) validate(sp, true);
        else PocketData.get(sp.server).removeReturn(sp.getUUID());
    }

    // o dono desconectou: os visitantes saem (so entram com ele conectado, para poder aceitar)
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PENDING.remove(sp.getUUID());
        PocketData data = PocketData.get(sp.server);
        PocketData.Plot plot = data.plotOf(sp.getUUID());
        if (plot != null) ejectAll(sp.server, plot, "owner_left", sp.getUUID());
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && event.getCrafting().getItem() instanceof DimensionalKeyItem) {
            keyCrafted(sp, event.getCrafting());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        PENDING.clear();
    }
}

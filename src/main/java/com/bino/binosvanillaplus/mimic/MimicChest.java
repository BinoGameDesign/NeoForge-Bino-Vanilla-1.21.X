package com.bino.binosvanillaplus.mimic;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import com.bino.binosvanillaplus.mixin.CompoundContainerAccessor;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mimico: um bau comum do jogo com uma marca ESCONDIDA (ModAttachments.MIMIC, so no servidor; o bau nao manda nada
// disso para o cliente, entao nenhum mod/Jade consegue ver). Vira mimico quando alguem guarda o Ovo de Mimico nele e
// fecha (bau duplo -> mimico duplo; bau ender nao conta). Se revela quando alguem tenta PUXAR um item (ou quebra o
// bau): a janela fecha, o bau some e no lugar nasce a criatura (MimicEntity), que abre a boca, morde e persegue.
// Pista: perto de jogadores, de vez em quando a tampa "respira" (evento de bloco EVENT_BREATH).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class MimicChest {
    private MimicChest() {}

    public static final int EVENT_BREATH = 70;
    public static final float BREATH_CHANCE = 0.12f;   // por segundo, por mimico a ate 6 blocos de alguem

    public static boolean isChristmas() {
        LocalDate d = LocalDate.now();
        return d.getMonth() == Month.DECEMBER && d.getDayOfMonth() >= 24 && d.getDayOfMonth() <= 26;
    }

    public static boolean isMimic(BlockEntity be) {
        return be instanceof ChestBlockEntity && be.hasData(ModAttachments.MIMIC) && be.getData(ModAttachments.MIMIC);
    }

    // as metades de bau por tras do menu (1 ou 2)
    public static List<ChestBlockEntity> halves(Container c) {
        List<ChestBlockEntity> out = new ArrayList<>();
        if (c instanceof ChestBlockEntity cb) out.add(cb);
        else if (c instanceof CompoundContainer cc) {
            CompoundContainerAccessor a = (CompoundContainerAccessor) cc;
            if (a.binosvanillaplus$first() instanceof ChestBlockEntity c1) out.add(c1);
            if (a.binosvanillaplus$second() instanceof ChestBlockEntity c2) out.add(c2);
        }
        return out;
    }

    // ---------------------------------------------------------------- virar mimico (guardou o ovo e fechou)
    @SubscribeEvent
    public static void onClose(PlayerContainerEvent.Close e) {
        if (!(e.getContainer() instanceof ChestMenu menu) || e.getEntity().level().isClientSide) return;
        Container c = menu.getContainer();
        List<ChestBlockEntity> hs = halves(c);
        if (hs.isEmpty() || hs.stream().anyMatch(MimicChest::isMimic)) return;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.is(ModItems.MIMIC_SPAWN_EGG.get())) {
                s.shrink(1);
                c.setChanged();
                for (ChestBlockEntity h : hs) { h.setData(ModAttachments.MIMIC, true); h.setChanged(); }
                return;
            }
        }
    }

    // ---------------------------------------------------------------- revelar
    // chamado pelo mixin do clique (tentou puxar um item) e por quem tenta quebrar o bau
    public static void reveal(ServerPlayer player, List<ChestBlockEntity> hs) {
        if (hs.isEmpty()) return;
        ServerLevel level = (ServerLevel) hs.get(0).getLevel();
        if (level == null) return;
        player.closeContainer();
        List<ItemStack> loot = new ArrayList<>();
        BlockState st = hs.get(0).getBlockState();
        Direction facing = st.getValue(ChestBlock.FACING);
        boolean trapped = st.getBlock() instanceof TrappedChestBlock;
        double x = 0, y = 0, z = 0;
        for (ChestBlockEntity h : hs) {
            for (int i = 0; i < h.getContainerSize(); i++) { ItemStack s = h.getItem(i); if (!s.isEmpty()) loot.add(s.copy()); }
            h.clearContent();
            h.removeData(ModAttachments.MIMIC);
            BlockPos p = h.getBlockPos();
            x += p.getX() + 0.5; y += p.getY(); z += p.getZ() + 0.5;
        }
        x /= hs.size(); y /= hs.size(); z /= hs.size();
        for (ChestBlockEntity h : hs) level.removeBlock(h.getBlockPos(), false);
        MimicEntity m = (hs.size() > 1 ? ModEntities.MIMIC_DOUBLE : ModEntities.MIMIC).get().create(level);
        if (m == null) return;
        m.moveTo(x, y, z, facing.toYRot(), 0);
        m.setYHeadRot(facing.toYRot()); m.setYBodyRot(facing.toYRot());
        m.setup(loot, trapped, player);
        level.addFreshEntity(m);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp) || sp.isCreative()) return;
        BlockEntity be = e.getLevel().getBlockEntity(e.getPos());
        if (!isMimic(be)) return;
        e.setCanceled(true);
        List<ChestBlockEntity> hs = new ArrayList<>();
        hs.add((ChestBlockEntity) be);
        BlockState st = be.getBlockState();
        if (st.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && e.getLevel().getBlockEntity(e.getPos().relative(ChestBlock.getConnectedDirection(st))) instanceof ChestBlockEntity other)
            hs.add(other);
        reveal(sp, hs);
    }

    // ---------------------------------------------------------------- pista: respiracao
    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel sl) || sl.getGameTime() % 20 != 0) return;
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator()) continue;
            int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (!sl.hasChunk(cx + dx, cz + dz)) continue;
                    LevelChunk ch = sl.getChunk(cx + dx, cz + dz);
                    for (BlockEntity be : ch.getBlockEntities().values()) {
                        if (!isMimic(be) || be.getBlockPos().distToCenterSqr(p.position()) > 36) continue;
                        if (sl.random.nextFloat() < BREATH_CHANCE) sl.blockEvent(be.getBlockPos(), be.getBlockState().getBlock(), EVENT_BREATH, 0);
                    }
                }
        }
    }

    // cliente: quando cada bau comecou a respirar (ms); o renderizador do bau usa
    private static final Map<BlockPos, Long> BREATH = new HashMap<>();
    public static final long BREATH_MS = 2600;

    public static void startBreath(BlockPos pos) { BREATH.put(pos.immutable(), Util.getMillis()); }

    // inclinacao extra da tampa (radianos, para abrir um tiquinho) agora
    public static float breath(BlockPos pos) {
        Long t0 = BREATH.get(pos);
        if (t0 == null) return 0;
        long dt = Util.getMillis() - t0;
        if (dt > BREATH_MS) { BREATH.remove(pos); return 0; }
        float k = (float) Math.sin(Math.PI * dt / BREATH_MS);
        return k * k * (float) Math.toRadians(2.5);
    }
}

package com.bino.binosvanillaplus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Ratoeira: guarda quantas iscas (pedacos de queijo) ainda tem (ate 16). Cada rato pego gasta 1.
// No cliente guarda a hora em que o arco comecou a fechar (para o renderer animar).
public class RatTrapBlockEntity extends BlockEntity {
    public static final int MAX_BAIT = 16;

    // ratoeiras com isca carregadas (servidor), para os ratos acharem sem varrer blocos
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BAITED = new HashMap<>();
    public static Set<BlockPos> baited(Level level) { return BAITED.computeIfAbsent(level.dimension(), k -> new HashSet<>()); }

    private int bait;
    public long snapStart = Long.MIN_VALUE;   // cliente: quando a animacao de fechar comecou

    public RatTrapBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.RAT_TRAP.get(), pos, state); }

    public int getBait() { return bait; }

    public void setBait(int b) {
        bait = Math.max(0, Math.min(MAX_BAIT, b));
        setChanged();
        track();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void track() {
        if (level == null || level.isClientSide) return;
        if (bait > 0 && !getBlockState().getValue(com.bino.binosvanillaplus.block.custom.RatTrapBlock.SPRUNG)) baited(level).add(worldPosition);
        else baited(level).remove(worldPosition);
    }

    @Override public void onLoad() { super.onLoad(); track(); }
    @Override public void setRemoved() { if (level != null && !level.isClientSide) baited(level).remove(worldPosition); super.setRemoved(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { super.saveAdditional(tag, r); tag.putInt("Bait", bait); }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { super.loadAdditional(tag, r); bait = tag.getInt("Bait"); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider r) { CompoundTag t = new CompoundTag(); t.putInt("Bait", bait); return t; }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public void refreshTracking() { track(); }
}

package com.bino.binosvanillaplus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Guarda quem jogou a corda solta (a corda amarrada no gancho nao tem dono). O dono nao consegue escalar a propria corda.
public class RopeBlockEntity extends BlockEntity {
    @Nullable private UUID owner;

    public RopeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROPE.get(), pos, state);
    }

    @Nullable
    public UUID getOwner() { return owner; }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    // Manda o dono tambem para o cliente (ele precisa saber para impedir a escalada)
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

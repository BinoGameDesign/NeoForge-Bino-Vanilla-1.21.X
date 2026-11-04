package com.bino.binosvanillaplus.block.entity;

import com.bino.binosvanillaplus.gear.Dynamite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Guarda quem colocou a dinamite (o detonador so explode as dinamites do proprio dono)
public class DynamiteBlockEntity extends BlockEntity {
    @Nullable private UUID owner;

    public DynamiteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNAMITE.get(), pos, state);
    }

    @Nullable public UUID getOwner() { return owner; }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
        if (level instanceof ServerLevel sl && owner != null) Dynamite.track(owner, sl, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl && owner != null) Dynamite.track(owner, sl, worldPosition);
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
}

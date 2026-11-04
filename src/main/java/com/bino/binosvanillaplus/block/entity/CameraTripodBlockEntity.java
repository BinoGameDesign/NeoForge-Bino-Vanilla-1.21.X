package com.bino.binosvanillaplus.block.entity;

import com.bino.binosvanillaplus.block.custom.CameraTripodBlock;
import com.bino.binosvanillaplus.network.TripodShotPayload;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

// Tripe: guarda a camera colocada nele e conta o timer da foto (5 s, com "tic" a cada segundo)
public class CameraTripodBlockEntity extends BlockEntity {
    public static final int TIMER = 100;
    private ItemStack camera = ItemStack.EMPTY;
    private int timer = -1;
    private UUID shooter;

    public CameraTripodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMERA_TRIPOD.get(), pos, state);
    }

    public ItemStack getCamera() { return camera; }
    public void setCamera(ItemStack s) { camera = s; setChanged(); }
    public boolean counting() { return timer >= 0; }

    public void start(ServerPlayer p) {
        shooter = p.getUUID();
        timer = TIMER;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CameraTripodBlockEntity be) {
        if (be.timer < 0 || !(level instanceof ServerLevel sl)) return;
        if (be.camera.isEmpty()) { be.timer = -1; return; }
        if (be.timer % 20 == 0 && be.timer > 0) {
            sl.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.6f, be.timer <= 40 ? 2.0f : 1.5f);
        }
        if (be.timer-- > 0) return;
        be.timer = -1;
        sl.playSound(null, pos, ModSounds.CAMERA_SHUTTER.get(), SoundSource.BLOCKS, 1f, 1f);
        float yaw = state.getValue(CameraTripodBlock.FACING).toYRot();
        sl.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        ServerPlayer p = be.shooter == null ? null : sl.getServer().getPlayerList().getPlayer(be.shooter);
        if (p != null && p.level() == sl && p.distanceToSqr(pos.getCenter()) < 96 * 96) PacketDistributor.sendToPlayer(p, new TripodShotPayload(pos, yaw));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!camera.isEmpty()) tag.put("Camera", camera.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        camera = tag.contains("Camera") ? ItemStack.parse(registries, tag.getCompound("Camera")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}

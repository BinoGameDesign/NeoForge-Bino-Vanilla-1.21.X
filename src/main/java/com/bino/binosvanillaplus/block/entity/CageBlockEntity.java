package com.bino.binosvanillaplus.block.entity;

import com.bino.binosvanillaplus.block.custom.CageBlock;
import com.bino.binosvanillaplus.entity.rat.RatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

// Gaiola: guarda a isca (queijo = ratos; sementes = galinhas e papagaios) e o bicho preso (tipo + dados do mob).
// No cliente guarda a hora em que a porta comecou a se mexer e um "boneco" do bicho para desenhar la dentro.
public class CageBlockEntity extends BlockEntity {
    private ItemStack bait = ItemStack.EMPTY;
    @Nullable private CompoundTag creature;          // mob preso (com "id")
    public long doorStart = Long.MIN_VALUE;          // cliente: inicio da animacao da porta
    @Nullable public Entity dummy;                   // cliente: boneco do bicho preso

    public CageBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.CAGE.get(), pos, state); }

    public ItemStack getBait() { return bait; }
    public boolean hasCreature() { return creature != null; }
    @Nullable public CompoundTag getCreature() { return creature; }

    public static boolean isCheese(ItemStack s) {
        return s.is(com.bino.binosvanillaplus.item.ModItems.CHEESE_SLICE.get()) || s.is(com.bino.binosvanillaplus.item.ModItems.CHEESE.get());
    }
    public static boolean isSeed(ItemStack s) { return s.is(ItemTags.CHICKEN_FOOD) || s.is(ItemTags.PARROT_FOOD); }

    // esse bicho cai nessa isca?
    public boolean lures(Entity e) {
        if (bait.isEmpty()) return false;
        if (e instanceof RatEntity && !(e instanceof com.bino.binosvanillaplus.entity.rat.DireRatEntity)) return isCheese(bait);   // Rato Atroz nao cabe
        if (e instanceof Chicken || e instanceof Parrot) return isSeed(bait);
        return false;
    }

    public void setBait(ItemStack s) { bait = s; changed(); }

    public void capture(LivingEntity e) {
        CompoundTag t = new CompoundTag();
        if (!e.save(t)) return;              // save() grava o "id" do tipo
        creature = t;
        bait = ItemStack.EMPTY;
        e.discard();
        changed();
    }

    // solta o bicho na frente da gaiola
    public void release(Level level, BlockPos at) {
        if (creature == null || !(level instanceof ServerLevel sl)) return;
        EntityType.create(creature, sl).ifPresent(e -> {
            e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, e.getYRot(), 0);
            sl.addFreshEntity(e);
        });
        creature = null;
        changed();
    }

    private void changed() {
        setChanged();
        dummy = null;
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // servidor: a cada 2 s puxa os bichos que gostam da isca para dentro
    public static void serverTick(Level level, BlockPos pos, BlockState state, CageBlockEntity be) {
        if (state.getValue(CageBlock.CLOSED) || be.bait.isEmpty()) return;
        if (level.getGameTime() % 5 == 0) {   // chegou na porta: entra e a porta fecha
            net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.atBottomCenterOf(pos);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.0), be::lures)) {
                double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
                double reach = e instanceof Parrot ? 1.1 : 0.45;
                if (dx * dx + dz * dz < reach * reach && dy > -0.3 && dy < (e instanceof Parrot ? 1.6 : 0.6)) {
                    be.capture(e);
                    CageBlock.close(level, pos, state);
                    return;
                }
            }
        }
        if (level.getGameTime() % 40 != (pos.asLong() & 31)) return;
        for (PathfinderMob m : level.getEntitiesOfClass(PathfinderMob.class, new AABB(pos).inflate(12), be::lures))
            m.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r);
        if (!bait.isEmpty()) tag.put("Bait", bait.save(r));
        if (creature != null) tag.put("Creature", creature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r);
        bait = tag.contains("Bait") ? ItemStack.parseOptional(r, tag.getCompound("Bait")) : ItemStack.EMPTY;
        creature = tag.contains("Creature") ? tag.getCompound("Creature") : null;
        dummy = null;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { CompoundTag t = new CompoundTag(); saveAdditional(t, r); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

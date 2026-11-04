package com.bino.binosvanillaplus.entity;

import com.bino.binosvanillaplus.block.entity.GraveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Allay fantasma (cinza e branco) parado no ar em cima do tumulo, com o nome do pet do jogador morto.
// Clicar nele abre o tumulo. Cerebro desligado: nao anda, nao pega itens, nao dorme.
public class GraveAllayEntity extends Allay {
    private BlockPos gravePos;   // so no servidor

    public GraveAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);
    }

    public void setGrave(BlockPos pos, Component name) {
        this.gravePos = pos.immutable();
        this.setCustomName(name);
    }

    @Override protected void customServerAiStep() { }   // sem cerebro do Allay

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        setDeltaMovement(Vec3.ZERO);
        // o tumulo sumiu (pegou tudo / dissipou / foi removido): o fantasma vai embora junto
        if (gravePos == null || !(level().getBlockEntity(gravePos) instanceof GraveBlockEntity)) discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.sidedSuccess(true);
        if (gravePos != null && player instanceof ServerPlayer sp && level().getBlockEntity(gravePos) instanceof GraveBlockEntity be) {
            be.tryOpen(sp);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
}

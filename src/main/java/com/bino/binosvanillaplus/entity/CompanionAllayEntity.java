package com.bino.binosvanillaplus.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// Extende o Allay do jogo (modelo e animação originais), mas com o cérebro dele desligado
public class CompanionAllayEntity extends Allay {

    // ---------- Valores que você pode ajustar ----------
    private static final double BACK = 1.1;               // blocos ATRÁS do jogador
    private static final double SIDE = 0.9;               // blocos à DIREITA do jogador
    private static final double HEIGHT = 1.7;             // altura acima dos pés do jogador
    private static final double FOLLOW_MAX_SPEED = 0.34;  // velocidade máxima seguindo (blocos por tick)
    private static final double FETCH_MAX_SPEED = 0.30;   // velocidade máxima buscando/entregando
    private static final int LIGHT_LEVEL = 6;             // luz que ele emite (0 a 15; tocha = 14)
    private static final double FETCH_RANGE_SQ = 32.0 * 32.0;   // só busca drops a até 32 blocos do dono
    private static final double CLUSTER_RADIUS_SQ = 4.0 * 4.0;  // pega tudo que estiver a até 4 blocos do drop

    private UUID ownerId;                                  // dono do pet
    private final List<ItemStack> carriedStacks = new ArrayList<>();   // itens que está levando
    private int carriedXp = 0;                             // XP que está levando
    private float anchorYaw;                               // direção de referência (só muda quando o jogador anda)
    private double lastOwnerX, lastOwnerZ;                 // posição anterior do dono (detecta movimento)
    private int farTicks;                                  // tempo longe do dono sem chegar perto
    private BlockPos lightPos = null;                      // onde está o bloco de luz invisível

    public CompanionAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);                   // o nome fica sempre aparecendo acima dele
    }

    public void setOwner(Player owner) {
        this.ownerId = owner.getUUID();
        this.anchorYaw = owner.getYRot();
        this.lastOwnerX = owner.getX();
        this.lastOwnerZ = owner.getZ();
    }

    // ---------- Posição de descanso: atrás e à direita da direção de referência ----------

    private Vec3 slot(Player owner) {
        double rad = Math.toRadians(this.anchorYaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);    // vetor "frente"
        double rx = -Math.cos(rad), rz = -Math.sin(rad);   // vetor "direita"
        return new Vec3(owner.getX() - fx * BACK + rx * SIDE,
                owner.getY() + HEIGHT,
                owner.getZ() - fz * BACK + rz * SIDE);
    }

    // Direção "direita" (usada para soltar itens ao lado do jogador)
    private Vec3 rightVec() {
        double rad = Math.toRadians(this.anchorYaw);
        return new Vec3(-Math.cos(rad), 0.0, -Math.sin(rad));
    }

    // A referência só gira quando o jogador ANDA. Só mexer a câmera não muda a posição do pet.
    private void updateAnchor(Player owner) {
        double dx = owner.getX() - this.lastOwnerX, dz = owner.getZ() - this.lastOwnerZ;
        if (dx * dx + dz * dz > 0.0004) {
            this.anchorYaw = Mth.rotLerp(0.1f, this.anchorYaw, owner.getYRot());
        }
        this.lastOwnerX = owner.getX();
        this.lastOwnerZ = owner.getZ();
    }

    // ---------- Imunidades e interação ----------

    @Override public boolean isAttackable() { return false; }            // golpes do jogador não o atingem
    @Override public boolean canBeHitByProjectile() { return false; }    // flechas, tridentes etc. atravessam
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeSeenAsEnemy() { return false; }        // mobs não o escolhem como alvo
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean shouldBeSaved() { return false; }

    // Só /kill e o vazio conseguem matá-lo (o gerenciador cria outro em 1 segundo)
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    // Desliga as interações do Allay (dar item, dançar...). A etiqueta de nome continua funcionando,
    // porque o jogo a trata antes deste método.
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    // ---------- Lógica principal (a cada tick, só no servidor) ----------

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel server)) return;

        Player owner = this.ownerId == null ? null : server.getPlayerByUUID(this.ownerId);
        if (owner == null || !owner.isAlive() || owner.level() != server) {
            this.discard();
            return;
        }

        updateAnchor(owner);
        Vec3 slot = slot(owner);

        boolean carrying = !this.carriedStacks.isEmpty() || this.carriedXp > 0;
        Vec3 target = carrying ? null : findTarget(server, owner);

        Vec3 goal;
        double maxSpeed;
        if (carrying) {
            // Levando coisas até o jogador
            goal = owner.position().add(0, 1.3, 0);
            maxSpeed = FETCH_MAX_SPEED;
            if (this.distanceToSqr(goal) < 2.0) deliver(owner);
        } else if (target != null) {
            // Indo buscar um drop ou XP
            goal = target.add(0, 0.5, 0);
            maxSpeed = FETCH_MAX_SPEED;
            if (this.distanceToSqr(goal) < 1.5) collectCluster(server, owner, target);
        } else {
            // Parado ao lado do jogador, flutuando com uma oscilação leve
            goal = slot.add(0, Math.sin(this.tickCount * 0.08) * 0.06, 0);
            maxSpeed = FOLLOW_MAX_SPEED;
        }

        // Teletransporte se o jogador se afastou muito
        boolean busy = carrying || target != null;
        double dist = this.distanceTo(owner);
        this.farTicks = (!busy && dist > 6.0) ? this.farTicks + 1 : 0;
        if (dist > (busy ? 48.0 : 12.0) || this.farTicks > 100) {
            this.moveTo(slot.x, slot.y, slot.z, this.getYRot(), this.getXRot());
            this.setDeltaMovement(Vec3.ZERO);
            this.farTicks = 0;
            return;
        }

        steerTo(goal, maxSpeed);
        faceCamera(owner, target);   // antes: faceCamera(owner, carrying);
        updateLight(server);

        // A cada 1 segundo: guarda o nome dado com a etiqueta (ignora o nome padrão traduzido)
        if (this.tickCount % 20 == 0 && this.hasCustomName()
                && !(this.getCustomName().getContents() instanceof TranslatableContents)) {
            CompanionAllayManager.setName(owner, this.getCustomName().getString());
        }
        // Brilhinhos ocasionais
        if (this.tickCount % 15 == 0) {
            server.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.2, getZ(), 1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    // Aproxima o pet do destino de forma suave: freia perto do alvo, então não fica orbitando
    private void steerTo(Vec3 goal, double maxSpeed) {
        Vec3 offset = goal.subtract(this.position());
        double dist = offset.length();
        Vec3 desired = dist < 0.05 ? Vec3.ZERO : offset.scale(Math.min(maxSpeed, dist * 0.15) / dist);
        this.setDeltaMovement(this.getDeltaMovement().lerp(desired, 0.2));
    }

    // Indo buscar: olha para o item (de costas para o jogador). Voltando com itens: olha para o jogador.
// Parado: câmera frontal copia a sua cabeça; nos outros modos olha para o jogador.
    private void faceCamera(Player owner, Vec3 fetchTarget) {
        boolean carrying = !this.carriedStacks.isEmpty() || this.carriedXp > 0;

        Vec3 lookAt;   // ponto para onde ele olha (null = copiar a cabeça do jogador)
        if (carrying) {
            lookAt = owner.getEyePosition();             // voltando: de frente para o jogador
        } else if (fetchTarget != null) {
            lookAt = fetchTarget;                        // indo: olhando para o item
        } else if (CompanionAllayManager.isFrontCamera(owner.getUUID())) {
            lookAt = null;                               // parado na câmera frontal: copia a cabeça
        } else {
            lookAt = owner.getEyePosition();             // parado nos outros modos: olha para o jogador
        }

        float targetYaw, targetPitch;
        if (lookAt == null) {
            targetYaw = owner.getYHeadRot();
            targetPitch = owner.getXRot();
        } else {
            double dx = lookAt.x - getX(), dy = lookAt.y - getEyeY(), dz = lookAt.z - getZ();
            targetYaw = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90.0f;
            targetPitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29577951308232);
        }

        // Gira suave (0.5 = metade do caminho por tick), para não dar tranco ao trocar de modo
        float yaw = Mth.rotLerp(0.5f, this.getYRot(), targetYaw);
        float pitch = Mth.lerp(0.5f, this.getXRot(), targetPitch);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

        float targetYaw, targetPitch;
        if (lookAt == null) {
            targetYaw = owner.getYHeadRot();
            targetPitch = owner.getXRot();
        } else {
            double dx = lookAt.x - getX(), dy = lookAt.y - getEyeY(), dz = lookAt.z - getZ();
            targetYaw = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90.0f;
            targetPitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29577951308232);
        }

        // Gira suave (0.5 = metade do caminho por tick), para não dar tranco ao trocar de modo
        float yaw = Mth.rotLerp(0.5f, this.getYRot(), targetYaw);
        float pitch = Mth.lerp(0.5f, this.getXRot(), targetPitch);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    // ---------- Coleta ----------

    // Ponto mais próximo para buscar (drop ou XP). XP que ficou longe demais é entregue direto ao jogador.
    private Vec3 findTarget(ServerLevel server, Player owner) {
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        long now = server.getGameTime();

        Iterator<CompanionAllayManager.TrackedDrop> it = CompanionAllayManager.drops(owner.getUUID()).iterator();
        while (it.hasNext()) {
            CompanionAllayManager.TrackedDrop drop = it.next();
            if (server.getEntity(drop.itemId()) instanceof ItemEntity item && item.isAlive()) {
                if (item.distanceToSqr(owner) > FETCH_RANGE_SQ) { it.remove(); continue; }
                double d = this.distanceToSqr(item);
                if (d < bestDist) { bestDist = d; best = item.position(); }
            } else if (now - drop.createdTick() > 100) {
                it.remove();
            }
        }

        Iterator<CompanionAllayManager.TrackedXp> xi = CompanionAllayManager.xp(owner.getUUID()).iterator();
        while (xi.hasNext()) {
            CompanionAllayManager.TrackedXp xp = xi.next();
            if (xp.pos().distanceToSqr(owner.position()) > FETCH_RANGE_SQ || now - xp.createdTick() > 600) {
                ExperienceOrb.award(server, owner.position(), xp.amount());   // não perde a XP
                xi.remove();
                continue;
            }
            double d = this.position().distanceToSqr(xp.pos());
            if (d < bestDist) { bestDist = d; best = xp.pos(); }
        }
        return best;
    }

    // Pega TUDO que estiver perto do ponto (todos os drops e XP do mesmo mob) de uma vez só
    private void collectCluster(ServerLevel server, Player owner, Vec3 center) {
        Iterator<CompanionAllayManager.TrackedDrop> it = CompanionAllayManager.drops(owner.getUUID()).iterator();
        while (it.hasNext()) {
            CompanionAllayManager.TrackedDrop drop = it.next();
            if (server.getEntity(drop.itemId()) instanceof ItemEntity item && item.isAlive()
                    && item.position().distanceToSqr(center) <= CLUSTER_RADIUS_SQ) {
                this.carriedStacks.add(item.getItem().copy());
                item.discard();     // o item some do chão e passa a ser mostrado na mão do Allay
                it.remove();
            }
        }

        Iterator<CompanionAllayManager.TrackedXp> xi = CompanionAllayManager.xp(owner.getUUID()).iterator();
        while (xi.hasNext()) {
            CompanionAllayManager.TrackedXp xp = xi.next();
            if (xp.pos().distanceToSqr(center) <= CLUSTER_RADIUS_SQ) {
                this.carriedXp += xp.amount();
                xi.remove();
            }
        }

        // Mostra o primeiro item na mão do Allay
        if (!this.carriedStacks.isEmpty()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, this.carriedStacks.get(0).copy());
        }
    }

    // Entrega tudo de uma vez: itens no inventário (o que não couber cai ao lado) e XP em esferas
    private void deliver(Player owner) {
        boolean overflow = false;
        for (ItemStack stack : this.carriedStacks) {
            ItemStack rest = stack.copy();
            owner.getInventory().add(rest);           // o que não couber continua em "rest"
            if (!rest.isEmpty()) {
                overflow = true;
                dropBeside(owner, rest);
            }
        }

        if (!this.carriedStacks.isEmpty()) {
            if (overflow) {
                // Avisa só na primeira vez, não a cada entrega
                if (CompanionAllayManager.markFull(owner.getUUID())) {
                    owner.displayClientMessage(
                            Component.translatable("message.binosvanillaplus.pet_inventory_full"), true);
                }
            } else {
                CompanionAllayManager.clearFull(owner.getUUID());
                this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.6f);
            }
        }

        if (this.carriedXp > 0) {
            ExperienceOrb.award((ServerLevel) this.level(), owner.position(), this.carriedXp);
        }

        this.carriedStacks.clear();
        this.carriedXp = 0;
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);   // mão vazia de novo
    }

    private void dropBeside(Player owner, ItemStack stack) {
        Vec3 side = rightVec().scale(0.7);
        ItemEntity drop = new ItemEntity(this.level(), owner.getX() + side.x, owner.getY() + 0.3,
                owner.getZ() + side.z, stack);
        drop.setDeltaMovement(Vec3.ZERO);
        drop.setDefaultPickUpDelay();
        this.level().addFreshEntity(drop);
    }

    // ---------- Luz (bloco invisível que acompanha o pet) ----------

    private void updateLight(ServerLevel server) {
        BlockPos pos = this.blockPosition();
        if (pos.equals(this.lightPos)) return;
        clearLight();
        if (server.getBlockState(pos).isAir()) {
            // UPDATE_CLIENTS = não dispara atualização de vizinhos (não ativa observadores)
            server.setBlock(pos, Blocks.LIGHT.defaultBlockState()
                    .setValue(BlockStateProperties.LEVEL, LIGHT_LEVEL), Block.UPDATE_CLIENTS);
            this.lightPos = pos;
        }
    }

    private void clearLight() {
        if (this.lightPos != null) {
            BlockState state = this.level().getBlockState(this.lightPos);
            if (state.is(Blocks.LIGHT) && state.getValue(BlockStateProperties.LEVEL) == LIGHT_LEVEL) {
                this.level().setBlock(this.lightPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            this.lightPos = null;
        }
    }

    // Quando o pet some: devolve o que estava levando e apaga a luz
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            for (ItemStack stack : this.carriedStacks) this.spawnAtLocation(stack);
            if (this.carriedXp > 0 && this.level() instanceof ServerLevel server) {
                ExperienceOrb.award(server, this.position(), this.carriedXp);
            }
            this.carriedStacks.clear();
            this.carriedXp = 0;
            clearLight();
        }
        super.remove(reason);
    }
}
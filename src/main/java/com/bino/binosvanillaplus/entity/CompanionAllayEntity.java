package com.bino.binosvanillaplus.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.UUID;

// Extende o Allay do jogo (modelo e animação originais), mas com o cérebro dele desligado
public class CompanionAllayEntity extends Allay {

    private static final double MAX_FETCH_DISTANCE_SQ = 32.0 * 32.0;  // só busca itens a até 32 blocos do dono

    private UUID ownerId;          // quem é o dono deste pet
    private ItemEntity carried;    // item que o pet está levando até o jogador (ou null)
    private int farTicks;          // há quanto tempo o pet está longe sem chegar perto

    public CompanionAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);        // golpes, flechas e explosões não o ferem
        this.setPersistenceRequired();     // nunca some sozinho
    }

    public void setOwner(Player owner) {
        this.ownerId = owner.getUUID();
    }

    // Direção "direita" do jogador no plano horizontal (baseada para onde o corpo dele aponta)
    public static Vec3 rightOf(Player player) {
        double yaw = Math.toRadians(player.yBodyRot);
        return new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
    }

    // Posição onde o pet fica parado: acima e à direita do jogador
    public static Vec3 followSlot(Player player) {
        Vec3 right = rightOf(player);
        return new Vec3(player.getX() + right.x * 0.9, player.getY() + 1.9, player.getZ() + right.z * 0.9);
    }

    // ---------- Imunidades ----------

    @Override
    public boolean isPickable() { return false; }            // flechas e golpes atravessam

    @Override
    public boolean isPushable() { return false; }            // ninguém empurra o pet

    @Override
    public boolean canBeSeenAsEnemy() { return false; }      // mobs não o escolhem como alvo

    @Override
    public boolean canPickUpLoot() { return false; }         // não pega itens sozinho como o Allay normal

    @Override
    public boolean shouldBeSaved() { return false; }         // não é salvo no mundo

    // ---------- Lógica principal (roda a cada tick, só no servidor) ----------

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel server)) return;

        // Se o dono sumiu, morreu ou está em outra dimensão, o pet desaparece (o gerenciador cria outro)
        Player owner = this.ownerId == null ? null : server.getPlayerByUUID(this.ownerId);
        if (owner == null || !owner.isAlive() || owner.level() != server) {
            this.discard();
            return;
        }

        Vec3 slot = followSlot(owner);

        // O item que ele levava sumiu (outro jogador pegou, despawn...): esquece
        if (this.carried != null && !this.carried.isAlive()) this.carried = null;

        // Sem nada nas "mãos", procura o drop mais próximo
        ItemEntity target = this.carried == null ? findTarget(server, owner) : null;

        Vec3 goal;       // para onde voar
        double speed;    // multiplicador de velocidade

        if (this.carried != null) {
            // Levando um item até o jogador
            goal = owner.position().add(0, 1.2, 0);
            speed = 2.25;
            this.carried.setPos(this.getX(), this.getY() - 0.2, this.getZ());   // o item acompanha o pet
            this.carried.setDeltaMovement(Vec3.ZERO);
            if (this.distanceToSqr(goal) < 2.25) deliver(owner);                // chegou perto: entrega
        } else if (target != null) {
            // Indo buscar um drop
            goal = target.position().add(0, 0.4, 0);
            speed = 2.0;
            if (this.distanceToSqr(goal) < 1.0) grab(target, owner);            // encostou: pega
        } else {
            // Seguindo o jogador: quanto mais longe, mais rápido, para alcançá-lo
            goal = slot;
            speed = 1.0 + Math.min(Math.sqrt(this.distanceToSqr(slot)), 12.0) * 0.15;
        }

        // Teletransporta se o jogador se afastou muito (ou se ficou preso longe por 5 segundos)
        boolean busy = this.carried != null || target != null;
        double dist = this.distanceTo(owner);
        this.farTicks = (!busy && dist > 6.0) ? this.farTicks + 1 : 0;
        if (dist > (busy ? 48.0 : 12.0) || this.farTicks > 100) {
            this.moveTo(slot.x, slot.y, slot.z, this.getYRot(), this.getXRot());
            this.setDeltaMovement(Vec3.ZERO);
            this.farTicks = 0;
            return;
        }

        this.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, speed);
        this.getLookControl().setLookAt(owner);
    }

    // Acha o drop rastreado mais próximo do pet, descartando os que ficaram longe demais
    private ItemEntity findTarget(ServerLevel server, Player owner) {
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        Iterator<CompanionAllayManager.TrackedDrop> it = CompanionAllayManager.drops(owner.getUUID()).iterator();
        while (it.hasNext()) {
            CompanionAllayManager.TrackedDrop drop = it.next();
            if (server.getEntity(drop.itemId()) instanceof ItemEntity item && item.isAlive()) {
                if (item.distanceToSqr(owner) > MAX_FETCH_DISTANCE_SQ) { it.remove(); continue; }
                double d = this.distanceToSqr(item);
                if (d < bestDist) { bestDist = d; best = item; }
            } else if (server.getGameTime() - drop.createdTick() > 100) {
                it.remove();   // o drop nunca apareceu ou já sumiu
            }
        }
        return best;
    }

    // Pega o item: ele passa a acompanhar o pet e ninguém mais consegue pegá-lo
    private void grab(ItemEntity item, Player owner) {
        CompanionAllayManager.untrack(owner.getUUID(), item.getUUID());
        this.carried = item;
        item.setNeverPickUp();
        item.setNoGravity(true);
    }

    // Entrega o item no inventário do jogador; o que não couber cai ao lado dele
    private void deliver(Player owner) {
        ItemStack stack = this.carried.getItem().copy();
        owner.getInventory().add(stack);   // o que sobrar continua dentro de "stack"

        if (stack.isEmpty()) {
            // Coube tudo
            owner.take(this.carried, this.carried.getItem().getCount());   // animação de pegar item
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.6f);
            this.carried.discard();
            CompanionAllayManager.clearFull(owner.getUUID());   // tem espaço de novo: pode avisar outra vez
        } else {
            // Inventário cheio: solta o resto ao lado do jogador
            Vec3 side = rightOf(owner).scale(0.7);
            this.carried.setItem(stack);
            this.carried.setPos(owner.getX() + side.x, owner.getY() + 0.3, owner.getZ() + side.z);
            this.carried.setDeltaMovement(Vec3.ZERO);
            this.carried.setNoGravity(false);
            this.carried.setDefaultPickUpDelay();

            // Avisa só na primeira vez, não a cada item
            if (CompanionAllayManager.markFull(owner.getUUID())) {
                owner.displayClientMessage(
                        Component.translatable("message.binosvanillaplus.pet_inventory_full"), true);
            }
        }
        this.carried = null;
    }

    // Se o pet some levando um item, o item volta a ser um drop normal (nada se perde)
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.carried != null && this.carried.isAlive()) {
            this.carried.setNoGravity(false);
            this.carried.setDefaultPickUpDelay();
        }
        this.carried = null;
        super.remove(reason);
    }
}
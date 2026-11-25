package com.bino.binosvanillaplus.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

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

    // Cor do pet: -1 = cor original; 0 a 15 = número do corante (DyeColor). É sincronizada com o cliente,
    // que precisa saber a cor para desenhar a textura certa.
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(CompanionAllayEntity.class, EntityDataSerializers.INT);

    // true enquanto o pet está buscando ou carregando itens. Nesse tempo ele não pode ser "alvo" do cursor,
    // senão fica na frente do bloco que você está quebrando e atrapalha a mineração.
    private static final EntityDataAccessor<Boolean> DATA_BUSY =
            SynchedEntityData.defineId(CompanionAllayEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int PET_COOLDOWN_TICKS = 100;   // 5 segundos entre um carinho e outro (20 ticks = 1 segundo)
    private long nextPetTick = 0;                        // a partir de que momento o próximo carinho é permitido

    private UUID ownerId;                                  // dono do pet
    private final List<ItemStack> carriedStacks = new ArrayList<>();   // itens que está levando
    private int carriedXp = 0;                             // XP que está levando
    private float anchorYaw;                               // direção de referência (só muda quando o jogador anda)
    private double lastOwnerX, lastOwnerZ;                 // posição anterior do dono (detecta movimento)
    private int farTicks;                                  // tempo longe do dono sem chegar perto
    private BlockPos lightPos = null;                      // onde está o bloco de luz invisível
    private int lightLevel = LIGHT_LEVEL;                  // nivel da luz colocada (tocha deixa mais forte)
    private BlockPos taskPos = null;                       // bloco que o dono pediu para apertar (Alt + botão direito)
    private net.minecraft.core.Direction taskFace = net.minecraft.core.Direction.UP;
    private int taskTicks = 0;
    private int settle = 0;                               // pausa curta depois de entregar/apertar, antes de voltar
    private int returnRamp = 99;                           // 0..35: acelera aos poucos na volta (99 = ja normal)
    private int pressTimer = -1;                           // chegou: espera a animação de apertar antes de acionar

    // ---------- Animações extras (o servidor avisa o cliente por "evento de entidade") ----------
    public static final byte EV_PET = 70, EV_PRESS = 71, EV_COLLECT = 72, EV_DELIVER = 73;
    // no cliente: em que tickCount cada animação começou (-1000 = nunca)
    public final int[] animStart = {-1000, -1000, -1000, -1000};

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EV_PET && id <= EV_DELIVER) {
            this.animStart[id - EV_PET] = this.tickCount;
            return;
        }
        super.handleEntityEvent(id);
    }

    // 0..1 do andamento da animação (ou -1 se não está tocando)
    public float animProgress(int index, float partialTick, float lengthTicks) {
        float t = (this.tickCount - this.animStart[index] + partialTick) / lengthTicks;
        return t >= 0f && t <= 1f ? t : -1f;
    }

    public CompanionAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);                   // o nome fica sempre aparecendo acima dele
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, -1);          // começa sem cor (azul original)
        builder.define(DATA_BUSY, false);
    }

    // Quando está ocupado, o cursor do jogador atravessa o pet (dá para quebrar blocos atrás dele)
    @Override
    public boolean isPickable() {
        return !this.entityData.get(DATA_BUSY);
    }

    public int getColorIndex() { return this.entityData.get(DATA_COLOR); }

    public void setColorIndex(int index) { this.entityData.set(DATA_COLOR, index); }

    public void setOwner(Player owner) {
        this.ownerId = owner.getUUID();
        this.anchorYaw = owner.getYRot();
        this.lastOwnerX = owner.getX();
        this.lastOwnerZ = owner.getZ();
    }

    // ---------- Tarefa: ir até um bloco (botão, alavanca, porta...) e acioná-lo ----------

    public void startTask(BlockPos pos, net.minecraft.core.Direction face) {
        this.taskPos = pos.immutable();
        this.taskFace = face;
        this.taskTicks = 0;
        this.pressTimer = -1;
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 0.6f, 1.4f);
    }

    // ponto um pouco à frente da face clicada (para o pet não entrar no bloco)
    private Vec3 taskGoal() {
        Vec3 c = Vec3.atCenterOf(this.taskPos);
        return c.add(this.taskFace.getStepX() * 0.85, this.taskFace.getStepY() * 0.85 - 0.3, this.taskFace.getStepZ() * 0.85);
    }

    private void doTask(ServerLevel server, Player owner) {
        BlockState st = server.getBlockState(this.taskPos);
        if (PetInteractables.allowed(st)) {
            Vec3 hit = Vec3.atCenterOf(this.taskPos).add(this.taskFace.getStepX() * 0.5, this.taskFace.getStepY() * 0.5, this.taskFace.getStepZ() * 0.5);
            st.useWithoutItem(server, owner, new net.minecraft.world.phys.BlockHitResult(hit, this.taskFace, this.taskPos, false));
            this.swing(InteractionHand.MAIN_HAND);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 0.7f, 1.5f);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, hit.x, hit.y, hit.z, 4, 0.15, 0.15, 0.15, 0.0);
        }
        this.taskPos = null;
        this.settle = 10;
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

    public UUID getOwnerId() { return ownerId; }

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

    // Desliga as interações do Allay (dar item, dançar...), menos uma: usar um CORANTE no pet muda a cor dele.
    // A etiqueta de nome continua funcionando, porque o jogo a trata antes deste método.
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ----- PEGAR DE VOLTA o item equipado: botão direito com as duas mãos vazias (no lugar do carinho) -----
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty() && !this.tool.isEmpty()) {
            if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);
            if (!player.getUUID().equals(this.ownerId)) return InteractionResult.PASS;
            ItemStack back = this.tool.copy();
            setTool(ItemStack.EMPTY);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            CompanionAllayManager.setTool(player, ItemStack.EMPTY);
            if (!player.getInventory().add(back)) player.drop(back, false);
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 1.0f, 1.0f);
            return InteractionResult.sidedSuccess(false);
        }

        // ----- ENTREGAR um item que o pet sabe usar (tocha, lanterna) -----
        // balde de água só agachado (sem agachar, o balde lava o pet: veja mais abaixo)
        if (usable(stack) && (!stack.is(Items.WATER_BUCKET) || player.isShiftKeyDown())) {
            if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);
            if (!player.getUUID().equals(this.ownerId)) return InteractionResult.PASS;
            ItemStack old = this.tool;
            ItemStack give = isFood(stack) ? stack.copy() : stack.copyWithCount(1);   // comida: a pilha toda
            if (give.getItem() instanceof com.bino.binosvanillaplus.item.custom.FlashlightItem)
                give.set(com.bino.binosvanillaplus.item.ModDataComponents.FLASHLIGHT_ON.get(), true);   // ja chega ligada
            if (!player.getAbilities().instabuild) stack.shrink(give.getCount());
            setTool(give);
            CompanionAllayManager.setTool(player, give);
            if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);   // troca: devolve o anterior
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 1.0f, 1.0f);
            return InteractionResult.sidedSuccess(false);
        }

        // ----- CARINHO: botão direito com as DUAS mãos vazias -----
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
            if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);   // só o balanço do braço

            if (this.level() instanceof ServerLevel server && server.getGameTime() >= this.nextPetTick) {
                this.nextPetTick = server.getGameTime() + PET_COOLDOWN_TICKS;   // começa a espera de 5 segundos

                // Barulhinho fofo (tom mais agudo que o normal do Allay). Troque o som aqui se quiser outro.
                server.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1.0f, 1.6f);
                server.broadcastEntityEvent(this, EV_PET);   // animação de carinho

                // Corações rosa: 7 partículas espalhadas ao redor do pet (como no breed)
                server.sendParticles(ModParticles.PINK_HEART.get(), getX(), getY() + 0.5, getZ(),
                        3, 0.25, 0.25, 0.25, 0.02);   // só 3 corações
            }
            return InteractionResult.sidedSuccess(false);   // mesmo na espera, o clique é "consumido"
        }

        // ----- BALDE DE ÁGUA (sem agachar): lava o pet e volta à cor original (o balde NÃO é gasto) -----
        if (stack.is(Items.WATER_BUCKET) && getColorIndex() >= 0) {
            if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);
            if (!player.getUUID().equals(this.ownerId)) return InteractionResult.PASS;

            setColorIndex(-1);                                   // -1 = azul original
            CompanionAllayManager.setColor(player, -1);          // esquece a cor guardada
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.8f, 1.2f);
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 10, 0.2, 0.2, 0.2, 0.1);
            }
            return InteractionResult.sidedSuccess(false);
        }

        // ----- CORANTE: muda a cor do pet -----
        if (!(stack.getItem() instanceof DyeItem dye)) return InteractionResult.PASS;

        // No cliente só mostra o balanço do braço; quem decide é o servidor
        if (this.level().isClientSide) return InteractionResult.sidedSuccess(true);

        // Só o dono pode tingir o pet
        if (!player.getUUID().equals(this.ownerId)) return InteractionResult.PASS;

        int color = dye.getDyeColor().getId();
        if (color != getColorIndex()) {                       // se já é essa cor, não gasta o corante
            setColorIndex(color);
            CompanionAllayManager.setColor(player, color);    // lembra a cor (o pet é recriado ao entrar no jogo)
            if (!player.getAbilities().instabuild) stack.shrink(1);   // no criativo o corante não acaba
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.DYE_USE, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess(false);
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

        hideWith(owner);
        updateAnchor(owner);
        Vec3 slot = slot(owner);
        Vec3 guide = guidePoint(owner);     // com bussola: vai na frente, mostrando o caminho
        if (guide != null) slot = guide;
        guiding = guide == null ? null : guide.add(guide.subtract(owner.position().add(0, 1.6, 0)).normalize().scale(6)).add(0, -0.6, 0);

        boolean carrying = !this.carriedStacks.isEmpty() || this.carriedXp > 0;
        // tarefa pedida pelo dono (aperta botao/alavanca) tem prioridade sobre buscar drops
        if (this.taskPos != null && (++this.taskTicks > 200 || this.taskPos.distSqr(owner.blockPosition()) > 40 * 40)) this.taskPos = null;
        boolean tasking = this.taskPos != null && !carrying;
        // com um item equipado (tocha, lanterna) ele nao busca drops
        Vec3 target = carrying || tasking || !this.tool.isEmpty() ? null : findTarget(server, owner);

        Vec3 goal;
        double maxSpeed;
        double smooth = 0.2;      // quanto a velocidade acompanha o alvo (menor = mais suave)
        if (tasking) {
            goal = taskGoal();
            maxSpeed = FETCH_MAX_SPEED;
            if (this.pressTimer < 0 && this.distanceToSqr(goal) < 0.5) {
                this.pressTimer = 6;                                  // a mão vai no bloco no meio da animação
                server.broadcastEntityEvent(this, EV_PRESS);
            } else if (this.pressTimer > 0 && --this.pressTimer == 0) {
                doTask(server, owner);
                this.pressTimer = -1;
            }
        } else if (carrying) {
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
            if (this.settle > 0) {
                // acabou de entregar/apertar: fica um instante no lugar, flutuando, antes de voltar
                this.settle--;
                this.returnRamp = 0;
                goal = this.position().add(0, Math.sin(this.tickCount * 0.15) * 0.04, 0);
                maxSpeed = FOLLOW_MAX_SPEED;
                smooth = 0.12;
            } else {
                // Volta ao lado do jogador acelerando e freando suavemente, flutuando com uma oscilação leve
                goal = slot.add(0, Math.sin(this.tickCount * 0.08) * 0.06, 0);
                double e = Math.min(1.0, this.returnRamp / 35.0);
                this.returnRamp++;
                e = e * e * (3.0 - 2.0 * e);
                maxSpeed = FOLLOW_MAX_SPEED * (0.2 + 0.8 * e);
                smooth = 0.06 + 0.14 * e;
            }
        }

        // Teletransporte se o jogador se afastou muito
        boolean busy = carrying || target != null || tasking;
        this.entityData.set(DATA_BUSY, busy);      // avisa o cliente (sincronizado)
        double dist = this.distanceTo(owner);
        this.farTicks = (!busy && dist > 6.0) ? this.farTicks + 1 : 0;
        if (dist > (busy ? 48.0 : 12.0) || this.farTicks > 100) {
            this.moveTo(slot.x, slot.y, slot.z, this.getYRot(), this.getXRot());
            this.setDeltaMovement(Vec3.ZERO);
            this.farTicks = 0;
            return;
        }

        steerTo(goal, maxSpeed, smooth);
        faceCamera(owner, tasking ? Vec3.atCenterOf(this.taskPos == null ? this.blockPosition() : this.taskPos) : target);
        if (hidden) clearLight(); else updateLight(server);   // dono invisivel: o pet nao ilumina
        useTool(server, owner, carrying);
        toolEffects(server, owner);

        // A cada 1 segundo: guarda o nome dado com a etiqueta (ignora o nome padrão traduzido)
        if (this.tickCount % 20 == 0 && this.hasCustomName()
                && !(this.getCustomName().getContents() instanceof TranslatableContents)) {
            CompanionAllayManager.setName(owner, this.getCustomName().getString());
        }
    }

    // Aproxima o pet do destino de forma suave: freia perto do alvo, então não fica orbitando
    private void steerTo(Vec3 goal, double maxSpeed, double smooth) {
        Vec3 offset = goal.subtract(this.position());
        double dist = offset.length();
        Vec3 desired = dist < 0.05 ? Vec3.ZERO : offset.scale(Math.min(maxSpeed, dist * 0.15) / dist);
        this.setDeltaMovement(this.getDeltaMovement().lerp(desired, smooth));
    }

    // Indo buscar: olha para o item (de costas para o jogador). Voltando com itens: olha para o jogador.
// Parado: câmera frontal copia a sua cabeça; nos outros modos olha para o jogador.
    private void faceCamera(Player owner, Vec3 fetchTarget) {
        boolean carrying = !this.carriedStacks.isEmpty() || this.carriedXp > 0;

        Vec3 lookAt;   // ponto para onde ele olha (null = copiar a cabeça do jogador)
        if (carrying) {
            lookAt = owner.getEyePosition();
        } else if (fetchTarget != null) {
            lookAt = fetchTarget;
        } else if (guiding != null) {
            lookAt = guiding;   // com a bussola: olha para o caminho, nao para o jogador
        } else if (CompanionAllayManager.isFrontCamera(owner.getUUID())) {
            lookAt = null;
        } else {
            lookAt = owner.getEyePosition();
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
        if (!this.carriedStacks.isEmpty() || this.carriedXp > 0) server.broadcastEntityEvent(this, EV_COLLECT);   // animação de pegar
        if (!this.carriedStacks.isEmpty()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, this.carriedStacks.get(0).copy());
        }
    }

    // Entrega tudo de uma vez: itens no inventário (o que não couber cai ao lado) e XP em esferas
    private void deliver(Player owner) {
        this.level().broadcastEntityEvent(this, EV_DELIVER);   // animação de entregar
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
        this.settle = 12;                                                  // 0,6 s no lugar antes de voltar
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

    // ---------- Invisibilidade junto com o dono ----------
    // Qualquer invisibilidade do dono (pocao, Anel da Invisibilidade, comando...) vale para o pet: ele fica invisivel,
    // sem som e sem luz, para nao denunciar onde o jogador esta. A invisibilidade do pet e' curta e renovada a cada
    // tick enquanto o dono estiver invisivel; quando o dono aparece, o pet aparece junto.
    private boolean hidden;

    private void hideWith(Player owner) {
        boolean hide = owner.isInvisible() || owner.hasEffect(MobEffects.INVISIBILITY);
        if (hide) {
            MobEffectInstance cur = this.getEffect(MobEffects.INVISIBILITY);
            if (cur == null || cur.getDuration() < 20)
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, false));
        } else if (this.hidden) {
            this.removeEffect(MobEffects.INVISIBILITY);
        }
        this.hidden = hide;
    }

    // ---------- Item entregue pelo dono ----------
    // Tocha/lanterna (o bloco) = ilumina forte em volta. Lanterna de mao (a do mod) = aponta o feixe para onde o dono
    // esta olhando. Com item equipado o pet nao busca drops. O item fica guardado nos dados do jogador
    // (CompanionAllayManager), porque o pet e' recriado ao entrar no jogo.
    private ItemStack tool = ItemStack.EMPTY;

    public ItemStack getTool() { return tool; }

    public void setTool(ItemStack t) {
        this.tool = t.copy();
        if (this.carriedStacks.isEmpty()) this.setItemInHand(InteractionHand.MAIN_HAND, this.tool.copy());
    }

    // o que da para entregar
    public static boolean usable(ItemStack s) {
        return s.is(Items.TORCH) || s.is(Items.SOUL_TORCH) || s.is(Items.LANTERN) || s.is(Items.SOUL_LANTERN)
                || s.getItem() instanceof com.bino.binosvanillaplus.item.custom.FlashlightItem
                || s.is(Items.TOTEM_OF_UNDYING) || isFood(s) || s.is(Items.WATER_BUCKET)
                || s.is(com.bino.binosvanillaplus.item.ModItems.CYALUME_STICK.get())
                || s.is(com.bino.binosvanillaplus.item.ModItems.EXPLORER_WHISTLE.get()) || s.is(Items.BELL)
                || s.is(Items.COMPASS) || s.is(Items.RECOVERY_COMPASS) || s.is(Items.FEATHER);
    }

    // comida: o pet recebe a pilha inteira; o resto, um so
    public static boolean isFood(ItemStack s) { return s.has(net.minecraft.core.component.DataComponents.FOOD); }

    // gasta uma unidade do item equipado (comida) e guarda nos dados do dono
    private void useUp(Player owner) {
        ItemStack t = this.tool.copy();
        t.shrink(1);
        setTool(t);
        if (t.isEmpty()) this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CompanionAllayManager.setTool(owner, t);
    }

    @Nullable private Vec3 guiding;   // ponto a frente, na direcao do destino da bussola (para onde ele olha)

    // ---- usos de cada item (chamado a cada tick, no servidor)
    private int toolCd;
    @Nullable private BlockPos mlgWater;
    private long mlgUntil;
    private final java.util.Map<UUID, Long> warned = new java.util.HashMap<>();

    private void toolEffects(ServerLevel server, Player owner) {
        if (toolCd > 0) toolCd--;
        long now = server.getGameTime();
        // agua da MLG: some depois de 1,5 s
        if (mlgWater != null && now > mlgUntil) {
            if (server.getBlockState(mlgWater).is(Blocks.WATER) && server.getFluidState(mlgWater).isSource())
                server.setBlock(mlgWater, Blocks.AIR.defaultBlockState(), 3);
            mlgWater = null;
        }
        if (tool.isEmpty()) return;
        boolean falling = owner.getDeltaMovement().y < -0.5 && !owner.onGround() && !owner.isFallFlying() && !owner.getAbilities().flying;

        if (isFood(tool)) {   // te alimenta quando a fome cai (faltando 3 coxinhas ou mais)
            if (toolCd == 0 && owner.getFoodData().getFoodLevel() <= 14) {
                var food = tool.get(net.minecraft.core.component.DataComponents.FOOD);
                if (food != null) {
                    owner.getFoodData().eat(food);
                    for (var pe : food.effects()) if (server.random.nextFloat() < pe.probability()) owner.addEffect(pe.effect());
                    server.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.2f);
                    server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, tool.copyWithCount(1)),
                            owner.getX(), owner.getEyeY() - 0.2, owner.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
                    useUp(owner);
                    toolCd = 60;
                }
            }
        } else if (tool.is(Items.WATER_BUCKET)) {
            if (owner.isOnFire() && toolCd == 0) {   // apaga o fogo
                owner.clearFire();
                server.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 1.4f);
                server.sendParticles(ParticleTypes.SPLASH, owner.getX(), owner.getY() + 1, owner.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                toolCd = 40;
            }
            // MLG: caindo de alto e o chao chegando, joga agua onde voce vai cair (nao no Nether)
            if (falling && owner.fallDistance > 6 && mlgWater == null && !server.dimensionType().ultraWarm()) {
                var hit = server.clip(new net.minecraft.world.level.ClipContext(owner.position(), owner.position().add(0, -4, 0),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.ANY, owner));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos land = hit.getBlockPos().above();
                    if (server.getBlockState(land).isAir() && server.getFluidState(hit.getBlockPos()).isEmpty()) {
                        server.setBlock(land, Blocks.WATER.defaultBlockState(), 3);
                        server.playSound(null, land, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1f, 1f);
                        mlgWater = land;
                        mlgUntil = now + 30;
                    }
                }
            }
        } else if (tool.is(Items.FEATHER)) {   // queda alta: segura voce (queda lenta)
            if (falling && owner.fallDistance > 4 && !owner.hasEffect(MobEffects.SLOW_FALLING)) {
                owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 0.8f, 1.5f);
            }
        } else if (tool.is(com.bino.binosvanillaplus.item.ModItems.CYALUME_STICK.get())) {   // pulso que revela os mobs por perto
            if (toolCd == 0 && !hidden) {
                com.bino.binosvanillaplus.item.custom.CyalumeStickItem.glowBurst(server, getX(), getY() + 0.3, getZ());
                for (Mob m : server.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(20), m -> m instanceof net.minecraft.world.entity.monster.Enemy))
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
                toolCd = 200;
            }
        } else if (tool.is(com.bino.binosvanillaplus.item.ModItems.EXPLORER_WHISTLE.get()) || tool.is(Items.BELL)) {   // vigia
            if (server.getGameTime() % 10 == 0) {
                for (Mob m : server.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(14),
                        m -> m instanceof net.minecraft.world.entity.monster.Enemy && m.isAlive() && (m.getTarget() == owner || m.distanceTo(owner) < 9))) {
                    Long last = warned.get(m.getUUID());
                    if (last != null && now - last < 600) continue;
                    warned.put(m.getUUID(), now);
                    server.playSound(null, owner.getX(), owner.getY(), owner.getZ(), tool.is(Items.BELL) ? SoundEvents.BELL_BLOCK : SoundEvents.NOTE_BLOCK_BELL.value(),
                            SoundSource.NEUTRAL, 0.9f, 1.6f);
                    owner.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.binosvanillaplus.pet_warn", m.getDisplayName())
                            .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                    this.getLookControl().setLookAt(m);
                    break;
                }
                if (warned.size() > 64) warned.entrySet().removeIf(e -> now - e.getValue() > 600);
            }
        } else if ((tool.is(Items.COMPASS) || tool.is(Items.RECOVERY_COMPASS)) && server.getGameTime() % 8 == 0) {
            Vec3 dest = compassDest(owner);
            if (dest != null && dest.distanceToSqr(owner.position()) > 64)   // trilha de luzinhas na direcao
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    // destino da bussola: recuperacao = onde voce morreu por ultimo (o tumulo); bussola com magnetita = a magnetita;
    // bussola comum = o spawn do mundo. So na mesma dimensao.
    @Nullable
    private Vec3 compassDest(Player owner) {
        if (tool.is(Items.RECOVERY_COMPASS)) {
            var g = owner.getLastDeathLocation();
            return g.isPresent() && g.get().dimension() == owner.level().dimension() ? Vec3.atCenterOf(g.get().pos()) : null;
        }
        var lode = tool.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
        if (lode != null) {
            if (lode.target().isEmpty()) return null;
            var g = lode.target().get();
            return g.dimension() == owner.level().dimension() ? Vec3.atCenterOf(g.pos()) : null;
        }
        if (owner.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) return null;
        return Vec3.atCenterOf(((ServerLevel) owner.level()).getSharedSpawnPos());
    }

    // com bussola: o pet fica ~3 blocos a frente do dono, na direcao do destino
    @Nullable
    private Vec3 guidePoint(Player owner) {
        if (!(tool.is(Items.COMPASS) || tool.is(Items.RECOVERY_COMPASS)) || hidden) return null;
        Vec3 dest = compassDest(owner);
        if (dest == null) return null;
        Vec3 d = dest.subtract(owner.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 64) return null;   // chegou perto: volta para o lado do dono
        return owner.position().add(d.normalize().scale(3.0)).add(0, 1.6, 0);
    }

    // totem: chamado quando o dono ia morrer (PetToolEvents); true = salvou
    public boolean saveOwner(Player owner) {
        if (!tool.is(Items.TOTEM_OF_UNDYING)) return false;
        useUp(owner);
        owner.setHealth(1.0f);
        owner.removeAllEffects();
        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        owner.level().broadcastEntityEvent(owner, (byte) 35);   // animacao e som do totem
        return true;
    }

    private int toolLight() {
        if (tool.is(Items.TORCH) || tool.is(Items.LANTERN)) return 14;
        if (tool.is(Items.SOUL_TORCH) || tool.is(Items.SOUL_LANTERN)) return 10;
        return LIGHT_LEVEL;
    }

    private boolean holdsFlashlight() { return tool.getItem() instanceof com.bino.binosvanillaplus.item.custom.FlashlightItem; }

    private void useTool(ServerLevel server, Player owner, boolean carrying) {
        if (!tool.isEmpty() && !carrying && !ItemStack.matches(getMainHandItem(), tool)) setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
        if (!holdsFlashlight() || hidden) { com.bino.binosvanillaplus.gear.FlashlightManager.stop(getUUID()); return; }
        if (this.tickCount % 3 != 0) return;
        // aponta para onde o dono esta olhando (o ponto que o olhar do dono acerta, ate 30 blocos)
        Vec3 eye = owner.getEyePosition(), end = eye.add(owner.getLookAngle().scale(30));
        var hit = server.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.VISUAL,
                net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
        Vec3 from = this.position().add(0, 0.35, 0);
        Vec3 dir = hit.getLocation().subtract(from);
        if (dir.lengthSqr() < 1.0e-3) return;
        com.bino.binosvanillaplus.gear.FlashlightManager.beam(getUUID(), server, this, from, dir.normalize());
    }

    // ---------- Luz (bloco invisível que acompanha o pet) ----------

    private void updateLight(ServerLevel server) {
        BlockPos pos = this.blockPosition();
        if (pos.equals(this.lightPos) && this.lightLevel == toolLight()) return;
        clearLight();
        if (server.getBlockState(pos).isAir()) {
            // UPDATE_CLIENTS = não dispara atualização de vizinhos (não ativa observadores)
            this.lightLevel = toolLight();
            server.setBlock(pos, Blocks.LIGHT.defaultBlockState()
                    .setValue(BlockStateProperties.LEVEL, this.lightLevel), Block.UPDATE_CLIENTS);
            this.lightPos = pos;
        }
    }

    private void clearLight() {
        if (this.lightPos != null) {
            BlockState state = this.level().getBlockState(this.lightPos);
            if (state.is(Blocks.LIGHT) && state.getValue(BlockStateProperties.LEVEL) == this.lightLevel) {
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
            com.bino.binosvanillaplus.gear.FlashlightManager.stop(getUUID());
        }
        super.remove(reason);
    }
}
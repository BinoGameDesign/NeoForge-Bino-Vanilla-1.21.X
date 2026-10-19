package com.bino.binosvanillaplus.entity;

import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.custom.CyalumeStickItem;
import com.bino.binosvanillaplus.network.CyalumeStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class CyalumeStickEntity extends ThrowableItemProjectile {

    private static final int SIGNAL_TICKS = 240;   // 12 segundos (20 ticks = 1 segundo)
    private static final int LIGHT_LEVEL = 10;     // mesma luz da tocha de almas (a azul)

    private boolean landed = false;     // já tocou o chão?
    private int landedTicks = 0;        // há quantos ticks está no chão
    private double startY;              // altura de onde foi solto
    private boolean dropsOnLiquid = true;
    private BlockPos lightPos = null;   // onde está o bloco de luz invisível (se houver)

    public CyalumeStickEntity(EntityType<? extends CyalumeStickEntity> type, Level level) {
        super(type, level);
    }

    public CyalumeStickEntity(Level level, LivingEntity owner) {
        super(ModEntities.CYALUME_STICK.get(), owner, level);
        this.startY = this.getY();
    }

    public void setDropsOnLiquid(boolean value) {
        this.dropsOnLiquid = value;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CYALUME_STICK.get();
    }

    // O bastão passa através de mobs e jogadores, só colide com blocos
    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    // Não salva a entidade ao descarregar o mundo: ela some em vez de ficar presa no ar
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        if (landed) {
            landedTicks++;
            if (!level().isClientSide()) {
                tickLanded();
            }
            return;
        }

        super.tick();

        if (!level().isClientSide() && (isInWater() || isInLava())) {
            splash();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide() || landed) return;

        // Bateu numa parede ou no teto: quica e continua caindo
        if (hit.getDirection() != Direction.UP) {
            Vec3 v = getDeltaMovement();
            switch (hit.getDirection().getAxis()) {
                case X -> setDeltaMovement(-v.x * 0.4, v.y, v.z);
                case Y -> setDeltaMovement(v.x, -v.y * 0.4, v.z);
                case Z -> setDeltaMovement(v.x, v.y, -v.z * 0.4);
            }
            Vec3 loc = hit.getLocation();
            setPos(loc.x + hit.getDirection().getStepX() * 0.15,
                    loc.y + hit.getDirection().getStepY() * 0.15,
                    loc.z + hit.getDirection().getStepZ() * 0.15);
            return;
        }

        // Tocou o chão: para tudo e começa a sinalizar
        landed = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        Vec3 loc = hit.getLocation();
        setPos(loc.x, loc.y + 0.15, loc.z);

        float height = (float) Math.max(0.0, startY - 1.52 - loc.y);

        // Som de impacto (o dono ouve de qualquer distância)
        playSoundForAll(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);

        // Acende a luz azul, só se o espaço estiver vazio (não troca grama alta, neve etc.)
        lightPos = blockPosition();
        if (level().getBlockState(lightPos).isAir()) {
            level().setBlock(lightPos,
                    Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, LIGHT_LEVEL), 3);
        } else {
            lightPos = null;
        }

        notifyOwner(CyalumeStatePayload.LAND, height);
    }

    private void tickLanded() {
        if (landedTicks >= SIGNAL_TICKS) {
            discard();   // 12 segundos acabaram: a entidade some (e a luz também, veja remove())
            return;
        }

        if (landedTicks % 20 < 10 && landedTicks % 2 == 0) {
            ServerLevel server = (ServerLevel) level();
            server.sendParticles(CyalumeStickItem.GLOW_DUST,
                    getX(), getY() + 0.1, getZ(), 6, 0.15, 0.15, 0.15, 0.0);
            if (landedTicks % 10 == 0) {
                server.sendParticles(ParticleTypes.GLOW_SQUID_INK,
                        getX(), getY() + 0.1, getZ(), 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private void splash() {
        playSoundForAll(SoundEvents.GENERIC_SPLASH, 1.0f, 1.0f);
        ((ServerLevel) level()).sendParticles(ParticleTypes.SPLASH,
                getX(), getY(), getZ(), 8, 0.2, 0.1, 0.2, 0.1);

        if (dropsOnLiquid) {
            spawnAtLocation(new ItemStack(ModItems.CYALUME_STICK.get()));
        }
        notifyOwner(CyalumeStatePayload.CANCEL, 0.0f);
        discard();
    }

    // Toca um som: quem soltou o bastão ouve em qualquer distância; os outros ouvem pela posição do bastão
    private void playSoundForAll(SoundEvent sound, float volume, float pitch) {
        ServerPlayer owner = getOwner() instanceof ServerPlayer sp ? sp : null;
        // Todos menos o dono ouvem normalmente (o som some com a distância)
        level().playSound(owner, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
        // O dono ouve como se o som tocasse ao lado dele, não importa quão longe o bastão caiu
        if (owner != null) {
            owner.playNotifySound(sound, SoundSource.NEUTRAL, volume, pitch);
        }
    }

    private void notifyOwner(int action, float height) {
        if (getOwner() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new CyalumeStatePayload(action, height));
        }
    }

    // Apaga o bloco de luz invisível, se ele ainda existir
    private void clearLight() {
        if (lightPos != null && level().getBlockState(lightPos).is(Blocks.LIGHT)) {
            level().setBlock(lightPos, Blocks.AIR.defaultBlockState(), 3);
        }
        lightPos = null;
    }

    // Toda vez que a entidade some (fim dos 12 s, descarregar chunk, etc.), a luz some junto
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide()) {
            clearLight();
        }
        super.remove(reason);
    }
}
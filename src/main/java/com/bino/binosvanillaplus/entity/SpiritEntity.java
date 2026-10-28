package com.bino.binosvanillaplus.entity;

import com.bino.binosvanillaplus.menu.SpiritMenu;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

// Espirito: aparece onde o jogador morreu e guarda os itens e a experiencia dele.
public class SpiritEntity extends Entity {
    public static final int SLOTS = 54;                    // 6 fileiras, como um bau duplo
    // Depois de quanto tempo (com o dono online) o espirito pergunta se deve esperar ou dissipar.
    public static final int PROMPT_AFTER_TICKS = 20 * 60 * 10;   // 10 minutos

    private final SimpleContainer items = new SimpleContainer(SLOTS);
    private int experience;
    private UUID ownerId;
    private int waitTicks;          // quanto tempo desde a ultima pergunta
    private boolean hadLoot;        // ja recebeu itens/xp (um espirito que nunca recebeu nada some sozinho)

    // So existe no cliente: controla a animacao de "flutuar"
    public final AnimationState idleAnimationState = new AnimationState();

    public SpiritEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    // ---------- dados ----------
    public SimpleContainer getItems() { return items; }
    public int getExperience() { return experience; }
    public void setExperience(int xp) { this.experience = Math.max(0, xp); }
    public void addExperience(int xp) { if (xp > 0) { this.experience += xp; this.hadLoot = true; } }
    public void markHadLoot() { this.hadLoot = true; }
    public boolean hadLoot() { return hadLoot; }
    public UUID getOwnerId() { return ownerId; }

    public void setOwner(ServerPlayer player) {
        this.ownerId = player.getUUID();
        this.setCustomName(Component.translatable("entity.binosvanillaplus.spirit_of", player.getName()));
        this.setCustomNameVisible(true);
    }

    public boolean canAccess(Player player) {
        return ownerId == null || player.getUUID().equals(ownerId) || player.hasPermissions(2);
    }

    public boolean isEmptyOfLoot() {
        return experience <= 0 && items.isEmpty();
    }

    // ---------- comportamento ----------
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) { }

    @Override public boolean isPickable() { return true; }          // da pra clicar nele
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }  // nao bloqueia o caminho
    @Override public boolean hurt(DamageSource source, float amount) { return false; }   // imortal

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            idleAnimationState.startIfStopped(this.tickCount);
            return;
        }
        if (!hadLoot && tickCount > 40) { discard(); return; }   // morreu sem nada: nao deixa um espirito vazio

        // Aviso "esperar ou dissipar": so conta o tempo enquanto o dono esta online
        ServerPlayer owner = ownerId == null ? null : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null && owner.isAlive()) {
            if (++waitTicks >= PROMPT_AFTER_TICKS) {
                waitTicks = 0;
                sendPrompt(owner);
            }
        }
    }

    private void sendPrompt(ServerPlayer owner) {
        String id = getUUID().toString();
        Component wait = Component.translatable("message.binosvanillaplus.spirit.wait_button")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/binosspirit wait " + id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.binosvanillaplus.spirit.wait_hover"))));
        Component dissipate = Component.translatable("message.binosvanillaplus.spirit.dissipate_button")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/binosspirit dissipate " + id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("message.binosvanillaplus.spirit.dissipate_hover"))));
        owner.sendSystemMessage(Component.translatable("message.binosvanillaplus.spirit.prompt",
                (int) getX(), (int) getY(), (int) getZ(), level().dimension().location().toString())
                .withStyle(ChatFormatting.AQUA));
        owner.sendSystemMessage(Component.literal("  ").append(wait).append(Component.literal("   ")).append(dissipate));
    }

    // O jogador respondeu "esperar": recomeca a contagem
    public void keepWaiting() { this.waitTicks = 0; }

    // Some, levando tudo junto. (Aqui entra a animacao no futuro.)
    public void dissipate() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 24, 0.3, 0.6, 0.3, 0.02);
        }
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.sidedSuccess(true);
        if (!canAccess(player)) {
            player.displayClientMessage(Component.translatable("message.binosvanillaplus.spirit.not_yours"), true);
            return InteractionResult.FAIL;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SpiritMenu(id, inv, this), getDisplayName()));
        return InteractionResult.CONSUME;
    }

    // ---------- salvar / carregar ----------
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Xp", experience);
        tag.putInt("WaitTicks", waitTicks);
        tag.putBoolean("HadLoot", hadLoot);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) list.set(i, items.getItem(i));
        ContainerHelper.saveAllItems(tag, list, registryAccess());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        experience = tag.getInt("Xp");
        waitTicks = tag.getInt("WaitTicks");
        hadLoot = tag.getBoolean("HadLoot");
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registryAccess());
        for (int i = 0; i < SLOTS; i++) items.setItem(i, list.get(i));
    }
}

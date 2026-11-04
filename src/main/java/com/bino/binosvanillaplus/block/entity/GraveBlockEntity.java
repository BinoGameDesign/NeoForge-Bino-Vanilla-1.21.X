package com.bino.binosvanillaplus.block.entity;

import com.bino.binosvanillaplus.entity.CompanionAllayManager;
import com.bino.binosvanillaplus.entity.GraveAllayEntity;
import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.menu.SpiritMenu;
import com.bino.binosvanillaplus.network.GraveMarkPayload;
import com.bino.binosvanillaplus.block.custom.GraveBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Tumulo: guarda os itens e o xp de quem morreu. Um Allay fantasma fica parado no ar em cima dele.
public class GraveBlockEntity extends BlockEntity {
    public static final int SLOTS = 54;                          // 6 fileiras, como um bau duplo
    public static final int PROMPT_AFTER_TICKS = 20 * 60 * 10;   // 10 min (so conta com o dono online): "esperar ou dissipar?"

    // tumulos conhecidos (para os botoes clicaveis do aviso): id -> onde esta
    public static final Map<UUID, GlobalPos> REGISTRY = new HashMap<>();

    private final SimpleContainer items = new SimpleContainer(SLOTS);
    private int experience;
    private UUID ownerId;
    private String ownerName = "";
    private String petName = "";
    private UUID graveId = UUID.randomUUID();
    private boolean hadLoot;
    private boolean dissipated;
    private int waitTicks;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVE.get(), pos, state);
        items.addListener(c -> setChanged());
    }

    // ---------- dados ----------
    public SimpleContainer getItems() { return items; }
    public int getExperience() { return experience; }
    public void setExperience(int xp) { this.experience = Math.max(0, xp); setChanged(); }
    public void addExperience(int xp) { if (xp > 0) { this.experience += xp; this.hadLoot = true; setChanged(); } }
    public void markHadLoot() { this.hadLoot = true; setChanged(); }
    public boolean hadLoot() { return hadLoot; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getGraveId() { return graveId; }
    public boolean isEmptyOfLoot() { return experience <= 0 && items.isEmpty(); }
    public boolean wasDissipated() { return dissipated; }

    public Component petDisplayName() {
        return petName == null || petName.isEmpty()
                ? Component.translatable("entity.binosvanillaplus.companion_allay") : Component.literal(petName);
    }

    public void setOwner(ServerPlayer player) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        String n = CompanionAllayManager.getName(player);
        this.petName = n == null ? "" : n;
        setChanged();
    }

    public boolean canAccess(Player player) {
        return ownerId == null || player.getUUID().equals(ownerId) || player.hasPermissions(2);
    }

    // ---------- abrir / dissipar ----------
    public void tryOpen(ServerPlayer player) {
        if (!canAccess(player)) {
            player.displayClientMessage(Component.translatable("message.binosvanillaplus.spirit.not_yours"), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SpiritMenu(id, inv, this),
                Component.translatable("block.binosvanillaplus.grave_of", ownerName)));
    }

    public void keepWaiting() { this.waitTicks = 0; }

    // Some, levando tudo junto
    public void dissipate() {
        if (!(level instanceof ServerLevel sl)) return;
        dissipated = true;
        items.clearContent();
        experience = 0;
        sl.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 24, 0.3, 0.6, 0.3, 0.02);
        sl.playSound(null, worldPosition, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
        for (GraveAllayEntity g : ghosts(sl)) g.discard();
        if (ownerId != null) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) PacketDistributor.sendToPlayer(owner, new GraveMarkPayload(worldPosition, GraveMarkPayload.CLEAR));
        }
        sl.removeBlock(worldPosition, false);
    }

    private List<GraveAllayEntity> ghosts(ServerLevel sl) {
        return sl.getEntitiesOfClass(GraveAllayEntity.class, new AABB(worldPosition).inflate(1.5, 3.0, 1.5));
    }

    // ---------- tick (servidor) ----------
    public static void serverTick(Level level, BlockPos pos, BlockState state, GraveBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        long t = sl.getGameTime();
        if (!REGISTRY.containsKey(be.graveId)) REGISTRY.put(be.graveId, GlobalPos.of(sl.dimension(), pos));
        if (t % 20 == 0) be.ensureGhost(sl);
        if (t % 50 == 0) {
            sl.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0.25, 0.3, 0.25, 0.005);
        }
        // aviso "esperar ou dissipar": so conta com o dono online
        ServerPlayer owner = be.ownerId == null ? null : sl.getServer().getPlayerList().getPlayer(be.ownerId);
        if (owner != null && owner.isAlive() && ++be.waitTicks >= PROMPT_AFTER_TICKS) {
            be.waitTicks = 0;
            be.sendPrompt(owner, sl);
        }
    }

    private void ensureGhost(ServerLevel sl) {
        if (dissipated || !sl.isPositionEntityTicking(worldPosition)) return;
        List<GraveAllayEntity> list = ghosts(sl);
        for (int i = 1; i < list.size(); i++) list.get(i).discard();   // nunca mais de um
        if (!list.isEmpty()) return;
        GraveAllayEntity g = ModEntities.GRAVE_ALLAY.get().create(sl);
        if (g == null) return;
        float yaw = getBlockState().getValue(GraveBlock.FACING).toYRot();
        g.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.55, worldPosition.getZ() + 0.5, yaw, 0f);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        g.setGrave(worldPosition, petDisplayName());
        sl.addFreshEntity(g);
    }

    private void sendPrompt(ServerPlayer owner, ServerLevel sl) {
        String id = graveId.toString();
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
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), sl.dimension().location().toString())
                .withStyle(ChatFormatting.AQUA));
        owner.sendSystemMessage(Component.literal("  ").append(wait).append(Component.literal("   ")).append(dissipate));
    }

    // ---------- salvar / carregar ----------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putString("OwnerName", ownerName);
        tag.putString("PetName", petName);
        tag.putUUID("GraveId", graveId);
        tag.putInt("Xp", experience);
        tag.putInt("WaitTicks", waitTicks);
        tag.putBoolean("HadLoot", hadLoot);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) list.set(i, items.getItem(i));
        ContainerHelper.saveAllItems(tag, list, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        petName = tag.getString("PetName");
        if (tag.hasUUID("GraveId")) graveId = tag.getUUID("GraveId");
        experience = tag.getInt("Xp");
        waitTicks = tag.getInt("WaitTicks");
        hadLoot = tag.getBoolean("HadLoot");
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registries);
        for (int i = 0; i < SLOTS; i++) items.setItem(i, list.get(i));
    }
}

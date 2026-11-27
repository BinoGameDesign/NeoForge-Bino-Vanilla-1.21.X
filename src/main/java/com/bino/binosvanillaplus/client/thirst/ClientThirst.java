package com.bino.binosvanillaplus.client.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.thirst.DrinkWaterPayload;
import com.bino.binosvanillaplus.thirst.Thirst;
import com.bino.binosvanillaplus.thirst.ThirstSyncPayload;
import com.bino.binosvanillaplus.thirst.WaterBottles;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Sede no cliente: a barra de gotas (desenhos do Bino) logo acima da fome, as bolhas de ar sobem para cima dela;
// sem correr com 3 gotas ou menos; beber direto da agua (agachado, mao vazia); aviso na garrafa de agua do mundo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientThirst {
    private ClientThirst() {}

    private static int level = (int) Thirst.MAX;

    public static void receive(ThirstSyncPayload m) { level = m.level(); }
    public static int level() { return level; }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { level = (int) Thirst.MAX; }

    // ---------------------------------------------------------------- barra
    private static ResourceLocation sprite(String n) { return ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "hud/" + n); }
    private static final ResourceLocation FULL = sprite("thirst_full"), HALF = sprite("thirst_half"), EMPTY = sprite("thirst_empty");
    private static final ResourceLocation FULL_N = sprite("thirst_full_nausea"), HALF_N = sprite("thirst_half_nausea"), EMPTY_N = sprite("thirst_empty_nausea");

    // logo depois da fome: usa a mesma pilha da direita, entao o ar (que vem depois) fica acima da sede
    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.FOOD_LEVEL, ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "thirst"), ClientThirst::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        if (!(mc.getCameraEntity() instanceof Player p)) return;
        if (p.getVehicle() instanceof LivingEntity v && v.showVehicleHealth()) return;   // no lugar da fome fica a vida da montaria
        Gui gui = mc.gui;
        int right = g.guiWidth() / 2 + 91;
        int y = g.guiHeight() - gui.rightHeight;
        gui.rightHeight += 10;
        boolean sick = p.hasEffect(MobEffects.CONFUSION);
        int ticks = gui.getGuiTicks();
        var rnd = net.minecraft.util.RandomSource.create(ticks * 312871L);
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int yy = y;
            if (level <= 4 && ticks % (level * 3 + 1) == 0) yy += rnd.nextInt(3) - 1;   // treme quando esta quase sem agua
            g.blitSprite(sick ? EMPTY_N : EMPTY, x, yy, 9, 9);
            if (i * 2 + 1 < level) g.blitSprite(sick ? FULL_N : FULL, x, yy, 9, 9);
            else if (i * 2 + 1 == level) g.blitSprite(sick ? HALF_N : HALF, x, yy, 9, 9);
        }
    }

    // ---------------------------------------------------------------- sem correr com pouca agua
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        var p = Minecraft.getInstance().player;
        if (p == null || p.getAbilities().invulnerable) return;
        if (level <= Thirst.NO_SPRINT_AT && p.isSprinting()) p.setSprinting(false);
    }

    // ---------------------------------------------------------------- beber direto da agua
    private static boolean lookingAtWater(Player p) {
        HitResult hit = p.pick(p.blockInteractionRange(), 1f, true);
        return hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK
                && p.level().getFluidState(bh.getBlockPos()).is(FluidTags.WATER);
    }

    private static boolean trySip(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !p.isShiftKeyDown() || !p.getMainHandItem().isEmpty()) return false;
        if (level >= Thirst.MAX || !lookingAtWater(p)) return false;
        PacketDistributor.sendToServer(DrinkWaterPayload.INSTANCE);
        return true;
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty e) {
        trySip(e.getEntity(), e.getHand());
    }

    // olhando a agua com o fundo (ou a margem) ao alcance: o clique iria no bloco; se a agua esta na frente, bebe
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!e.getLevel().isClientSide()) return;
        if (trySip(e.getEntity(), e.getHand())) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- avisos nos itens
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        if (WaterBottles.is(e.getItemStack())) {
            e.getToolTip().add(Component.translatable("tooltip.binosvanillaplus.water.dirty").withStyle(ChatFormatting.GOLD));
            e.getToolTip().add(Component.translatable("tooltip.binosvanillaplus.water.boil").withStyle(ChatFormatting.GRAY));
        } else if (e.getItemStack().is(com.bino.binosvanillaplus.item.ModItems.BOILED_WATER.get())) {
            e.getToolTip().add(Component.translatable("tooltip.binosvanillaplus.water.boiled").withStyle(ChatFormatting.AQUA));
        }
    }
}

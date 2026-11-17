package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.inventory.ExtraSlots;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// Anel da Invisibilidade (slot de anel): deixa o jogador invisivel. Atacar ou atirar qualquer projetil (flecha, tridente,
// bola de neve...) corta a invisibilidade por alguns segundos. A durabilidade so gasta enquanto a invisibilidade esta ativa.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class InvisibleRingEvents {

    // ====== VALORES QUE VOCE PODE AJUSTAR ======
    public static final int OFF_AFTER_ACTION = 100;   // 5 s sem invisibilidade depois de atacar/atirar
    public static final int WEAR_EVERY = 20;          // 1 de durabilidade a cada 20 ticks (1 s) invisivel

    private static final String KEY = "binosvanillaplus_ring_off_until";
    private static final int EFFECT_TICKS = 60;       // o efeito e renovado sempre; curto para sumir logo se tirar o anel

    private static boolean wearing(ServerPlayer p) {
        return p.getData(ModAttachments.EXTRA_SLOTS.get()).getStackInSlot(ExtraSlots.RING).is(ModItems.INVISIBLE_RING.get());
    }

    // so mexe na invisibilidade que o anel deu (marcada como "ambient"); pocao de invisibilidade fica intacta
    private static void removeRingEffect(ServerPlayer p) {
        MobEffectInstance cur = p.getEffect(MobEffects.INVISIBILITY);
        if (cur != null && cur.isAmbient() && cur.getDuration() <= EFFECT_TICKS) p.removeEffect(MobEffects.INVISIBILITY);
    }

    private static void suppress(ServerPlayer p) {
        if (!wearing(p)) return;
        p.getPersistentData().putLong(KEY, p.level().getGameTime() + OFF_AFTER_ACTION);
        removeRingEffect(p);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ExtraSlots slots = p.getData(ModAttachments.EXTRA_SLOTS.get());
        ItemStack ring = slots.getStackInSlot(ExtraSlots.RING);
        if (!ring.is(ModItems.INVISIBLE_RING.get())) return;
        if (p.level().getGameTime() < p.getPersistentData().getLong(KEY)) { removeRingEffect(p); return; }

        MobEffectInstance cur = p.getEffect(MobEffects.INVISIBILITY);
        boolean ours = cur == null || (cur.isAmbient() && cur.getDuration() <= EFFECT_TICKS);
        if (!ours) return;   // ja esta invisivel por pocao: o anel nao gasta
        if (cur == null || cur.getDuration() < EFFECT_TICKS - 20)
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS, 0, true, false, true));

        if (!p.isCreative() && p.tickCount % WEAR_EVERY == 0) {
            ItemStack worn = ring.copy();
            worn.setDamageValue(worn.getDamageValue() + 1);
            if (worn.getDamageValue() >= worn.getMaxDamage()) {
                slots.setStackInSlot(ExtraSlots.RING, ItemStack.EMPTY);
                p.level().playSound(null, p.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                removeRingEffect(p);
            } else slots.setStackInSlot(ExtraSlots.RING, worn);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) suppress(p);
    }

    @SubscribeEvent
    public static void onProjectile(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && e.getEntity() instanceof Projectile pr && pr.getOwner() instanceof ServerPlayer p
                && pr.tickCount == 0 && !e.loadedFromDisk()) suppress(p);
    }
}

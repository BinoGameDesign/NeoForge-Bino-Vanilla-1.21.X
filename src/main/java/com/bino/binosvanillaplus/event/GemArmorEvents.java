package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModArmorMaterials;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

// Bônus dos sets COMPLETOS (as 4 peças): Rubi = esquiva, Jade = peso-pena, Safira = magnético
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class GemArmorEvents {

    // ====== VALORES QUE VOCÊ PODE AJUSTAR ======
    public static final float RUBI_DODGE_CHANCE = 0.10f;      // 10% de chance de ignorar um dano
    public static final float JADE_FALL_MULTIPLIER = 0.52f;   // dano de queda x0.52 (igual a Peso-pena IV, -48%). Use 0 para nunca tomar dano de queda
    public static final double SAPPHIRE_MAGNET_RANGE = 6.0;   // alcance do imã, em blocos (o normal do jogo é 1)
    private static final double SAPPHIRE_MAGNET_SPEED = 0.25;  // velocidade com que os itens vêm até você

    // Conta quantas peças (0 a 4) do material o jogador está vestindo
    public static int countPieces(Player player, DeferredHolder<ArmorMaterial, ArmorMaterial> material) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).getItem() instanceof ArmorItem armor
                    && armor.getMaterial().is(material.getId())) {
                count++;
            }
        }
        return count;
    }

    // Diz se o jogador veste as 4 peças feitas com o material indicado
    private static boolean hasFullSet(Player player, DeferredHolder<ArmorMaterial, ArmorMaterial> material) {
        return countPieces(player, material) == 4;
    }

    // ---------- RUBI: 10% de chance de desviar de um dano ----------
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Danos que "atravessam tudo" (void, /kill) nunca são desviados
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        if (hasFullSet(player, ModArmorMaterials.RUBI) && player.getRandom().nextFloat() < RUBI_DODGE_CHANCE) {
            event.setCanceled(true);   // cancelar = o dano não acontece

            // Um som e uma nuvenzinha para você perceber que desviou
            ServerLevel level = player.serverLevel();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.6f);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                    6, 0.3, 0.4, 0.3, 0.02);
        }
    }

    // ---------- JADE: peso-pena (menos dano de queda) ----------
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && hasFullSet(player, ModArmorMaterials.JADE)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * JADE_FALL_MULTIPLIER);
        }
    }

    // ---------- SAFIRA: magnético (puxa itens do chão de mais longe) ----------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isSpectator() || !player.isAlive()) return;
        if (!hasFullSet(player, ModArmorMaterials.SAPPHIRE)) return;

        // Todos os itens no chão dentro do alcance
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(SAPPHIRE_MAGNET_RANGE));

        Vec3 target = player.position().add(0, 0.5, 0);   // puxa para a altura do peito
        for (ItemEntity item : items) {
            // Itens recém-largados (por você ou pelo pet) ainda têm "atraso de coleta": deixa em paz
            if (item.hasPickUpDelay() || !item.isAlive()) continue;

            Vec3 pull = target.subtract(item.position());
            if (pull.lengthSqr() < 0.25) continue;   // já está colado em você, o jogo pega sozinho

            item.setDeltaMovement(pull.normalize().scale(SAPPHIRE_MAGNET_SPEED));
            item.hurtMarked = true;                  // avisa o cliente do movimento novo
        }
    }
}

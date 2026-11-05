package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.entity.CompanionAllayEntity;
import com.bino.binosvanillaplus.entity.GraveAllayEntity;
import com.bino.binosvanillaplus.network.MobLevelPayload;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Niveis dos mobs (1 a 100): cada mob recebe um nivel ao entrar no mundo pela primeira vez (nivel do lugar +-2),
// que fica gravado nele. O nivel aumenta vida, dano e resistencia, e o XP que ele solta.
// Curvas pensadas junto com as pedras de forja (+10 = status x2): no nivel 100 a vida e' ~x5 e o dano corpo a corpo ~x2,8.
// Mobs que atacam de outro jeito: explosao (creeper etc.) escala a parte; projeteis (flecha, tridente, bola de fogo)
// ganham um dano EXTRA fixo por nivel, somado por fora do dano normal do projetil.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class MobLevels {
    private MobLevels() {}

    public static final double HEALTH_PER_LEVEL = 0.04;      // vida: +4% por nivel (x4,96 no 100)
    public static final double MELEE_PER_LEVEL = 0.018;      // corpo a corpo: +1,8% por nivel (x2,78 no 100)
    public static final double EXPLOSION_PER_LEVEL = 0.012;  // explosao: +1,2% por nivel (x2,19 no 100)
    public static final double PROJECTILE_EXTRA = 0.07;      // projetil: +0,07 de dano fixo por nivel (+6,9 no 100)
    public static final double RESIST_PER_LEVEL = 0.0025;    // resistencia: -0,25% de dano recebido por nivel
    public static final double RESIST_MAX = 0.25;
    public static final double XP_PER_LEVEL = 0.01;

    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "mob_level_health");

    public static int get(Entity e) { return e.getData(ModAttachments.MOB_LEVEL.get()); }

    // quem pode ter nivel: criaturas (hostis e passivas). Fora: jogadores, aldeoes/comerciantes, chefes, pets do jogador
    public static boolean eligible(Entity e) {
        if (!(e instanceof Mob m)) return false;
        if (e instanceof Npc || e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden || e instanceof ElderGuardian) return false;
        if (e instanceof CompanionAllayEntity || e instanceof GraveAllayEntity) return false;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;   // domados
        return true;
    }

    // ---------------------------------------------------------------- dar o nivel

    // O evento de entrar no mundo acontece no MEIO do carregamento do chunk: ler altura/bioma ali fazia o servidor
    // esperar o proprio chunk (mundo sem carregar e engasgos). Agora o mob entra numa fila e recebe o nivel no tick seguinte.
    private static final java.util.ArrayDeque<Mob> QUEUE = new java.util.ArrayDeque<>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (eligible(e) && get(e) == 0) QUEUE.add((Mob) e);
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (int n = 0; n < 200 && !QUEUE.isEmpty(); n++) {
            Mob m = QUEUE.poll();
            if (m.isRemoved() || get(m) > 0 || !(m.level() instanceof ServerLevel level)) continue;
            if (!level.isLoaded(m.blockPosition())) continue;
            assign(level, m);
        }
    }

    private static void assign(ServerLevel level, Mob e) {
        double base = RegionDifficulty.levelAt(level, e.blockPosition());
        if (base < 0) return;
        var r = level.getRandom();
        int lv = (int) Math.round(base + (r.nextFloat() + r.nextFloat() - 1f) * 2.5f);   // +-2,5, mais comum perto do centro
        set(e, Math.max(RegionDifficulty.MIN, Math.min(RegionDifficulty.MAX, lv)));
    }

    public static void set(Mob m, int lv) {
        m.setData(ModAttachments.MOB_LEVEL.get(), lv);
        applyAttributes(m, lv);
        if (m.level() instanceof ServerLevel) PacketDistributor.sendToPlayersTrackingEntity(m, new MobLevelPayload(m.getId(), lv));
    }

    private static void applyAttributes(LivingEntity e, int lv) {
        float ratio = e.getMaxHealth() > 0 ? e.getHealth() / e.getMaxHealth() : 1f;
        modify(e, Attributes.MAX_HEALTH, HEALTH_ID, HEALTH_PER_LEVEL * (lv - 1));
        e.setHealth(Math.max(1f, ratio * e.getMaxHealth()));
    }

    private static void modify(LivingEntity e, Holder<Attribute> attr, ResourceLocation id, double amount) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // virou outro mob (zumbi -> afogado, aldeao -> bruxa, porco -> piglin zumbi...): mantem o nivel
    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Post event) {
        int lv = get(event.getEntity());
        if (lv > 0 && event.getOutcome() instanceof Mob m && eligible(m)) set(m, lv);
    }

    // ---------------------------------------------------------------- combate

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        float amount = event.getAmount();
        Entity attacker = src.getEntity();
        if (attacker instanceof LivingEntity a && !(attacker instanceof ServerPlayer)) {
            int lv = get(a);
            if (lv > 1) {
                if (src.is(DamageTypeTags.IS_EXPLOSION)) amount *= (float) (1 + EXPLOSION_PER_LEVEL * (lv - 1));
                else if (src.getDirectEntity() instanceof Projectile) amount += (float) (PROJECTILE_EXTRA * (lv - 1));
                else amount *= (float) (1 + MELEE_PER_LEVEL * (lv - 1));
            }
        }
        int tl = get(event.getEntity());
        if (tl > 1) amount *= (float) (1 - Math.min(RESIST_MAX, RESIST_PER_LEVEL * (tl - 1)));
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        int lv = get(event.getEntity());
        if (lv > 1) event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * (1 + XP_PER_LEVEL * (lv - 1))));
    }

    // o jogador passou a ver o mob: manda o nivel para o painel de inspecao
    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        int lv = get(event.getTarget());
        if (lv > 0 && event.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p, new MobLevelPayload(event.getTarget().getId(), lv));
    }
}

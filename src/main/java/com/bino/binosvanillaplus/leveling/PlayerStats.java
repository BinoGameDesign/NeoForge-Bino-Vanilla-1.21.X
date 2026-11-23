package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

// NIVEL DO PERSONAGEM (regras de 23/11, estilo almas do Dark Souls): a experiencia do jogo e' a moeda. Cada nivel
// custa barras de experiencia (o "nivel" do jogo = barras completas): nivel 2 custa 1 barra e o custo sobe 1 barra a
// cada 4 niveis (nivel 20 = 5 barras). Maximo 100. Cada nivel da 1 ponto para distribuir:
//   FORCA       +3% de dano fisico e +2% da postura que voce causa, por ponto
//   VITALIDADE  +1 de vida (meio coracao) e +3 de postura sua, por ponto
//   AGILIDADE   +2 de estamina de combate, +1% de velocidade de ataque e +0,5% de evasao (max 25%), por ponto
//   INTELIGENCIA / VONTADE: reservados para a magia (travados, nao recebem pontos ainda)
// Os dados ficam no jogador (sobrevivem a morte) e vao para o cliente (visor e aba Atributos).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class PlayerStats {
    private PlayerStats() {}

    public static final int LEVEL = 0, STR = 1, VIT = 2, AGI = 3, INT = 4, WIL = 5;
    public static final int MAX_LEVEL = 100;

    public static List<Integer> data(Player p) {
        List<Integer> d = p.getData(ModAttachments.CHAR_STATS.get());
        if (d == null || d.size() < 6) return List.of(1, 0, 0, 0, 0, 0);
        return d;
    }

    public static int get(Player p, int i) { return data(p).get(i); }
    public static int level(Player p) { return Math.max(1, get(p, LEVEL)); }
    public static int vitality(Player p) { return get(p, VIT); }
    public static int freePoints(Player p) { return Math.max(0, level(p) - 1 - get(p, STR) - get(p, VIT) - get(p, AGI) - get(p, INT) - get(p, WIL)); }

    // custo (em barras de experiencia) para chegar ao nivel 'to'
    public static int cost(int to) { return 1 + Math.max(0, to - 2) / 4; }

    public static float postureMult(Player p) { return 1f + 0.02f * get(p, STR); }
    public static float evasion(Player p) { return Math.min(0.25f, 0.005f * get(p, AGI)); }

    private static void set(ServerPlayer p, int i, int v) {
        List<Integer> d = new ArrayList<>(data(p));
        d.set(i, v);
        p.setData(ModAttachments.CHAR_STATS.get(), List.copyOf(d));
        apply(p);
        sync(p);
    }

    // ---------------------------------------------------------------- acoes (aba Atributos)
    public static final byte ACT_LEVEL_UP = 0, ACT_ADD = 1;

    public static void onAction(ServerPlayer p, StatsActionPayload m) {
        if (m.kind() == ACT_LEVEL_UP) {
            int lv = level(p);
            if (lv >= MAX_LEVEL) return;
            int c = cost(lv + 1);
            if (!p.isCreative()) {
                if (p.experienceLevel < c) return;
                p.giveExperienceLevels(-c);
            }
            set(p, LEVEL, lv + 1);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
        } else if (m.kind() == ACT_ADD) {
            int st = m.stat();
            if (st != STR && st != VIT && st != AGI) return;   // Inteligencia e Vontade: so com a magia
            if (freePoints(p) <= 0) return;
            set(p, st, get(p, st) + 1);
        }
    }

    // Essencia do Recomeco: devolve todos os pontos (o nivel fica)
    public static void respec(ServerPlayer p) {
        p.setData(ModAttachments.CHAR_STATS.get(), List.of(level(p), 0, 0, 0, 0, 0));
        apply(p);
        sync(p);
    }

    // ---------------------------------------------------------------- efeitos
    private static final ResourceLocation HEALTH = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "stats_vitality");
    private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "stats_agility");
    private static final ResourceLocation DAMAGE = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "stats_strength");

    public static void apply(ServerPlayer p) {
        modify(p, Attributes.MAX_HEALTH, HEALTH, get(p, VIT), AttributeModifier.Operation.ADD_VALUE);
        modify(p, Attributes.ATTACK_SPEED, SPEED, 0.01 * get(p, AGI), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modify(p, Attributes.ATTACK_DAMAGE, DAMAGE, 0.03 * get(p, STR), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void modify(Player p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    public static void sync(ServerPlayer p) {
        List<Integer> d = data(p);
        PacketDistributor.sendToPlayer(p, new StatsSyncPayload(d.get(0), d.get(1), d.get(2), d.get(3), d.get(4), d.get(5)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) { apply(p); sync(p); } }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) { apply(p); sync(p); } }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }

    // evasao (Agilidade): chance de nao levar o golpe de um mob
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getSource().getEntity() instanceof net.minecraft.world.entity.Mob)) return;
        float ev = evasion(p);
        if (ev > 0 && p.getRandom().nextFloat() < ev) {
            e.setCanceled(true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }

    // correr gasta fome pela metade (a corrida agora tambem gasta a estamina normal; regras de 23/11)
    private static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> LAST = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var last = LAST.put(p.getUUID(), p.position());
        if (last == null || !p.isSprinting() || !p.onGround()) return;
        double dist = Math.sqrt(p.position().distanceToSqr(last.x, p.getY(), last.z));
        if (dist <= 0 || dist > 2) return;
        float refund = (float) (0.05 * dist);   // o jogo gasta 0,1 por bloco correndo
        var food = p.getFoodData();
        if (food.getExhaustionLevel() >= refund) food.addExhaustion(-refund);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); }
}

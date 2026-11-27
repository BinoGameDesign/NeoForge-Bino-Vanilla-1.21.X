package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// SEDE (regras de 26/11, ritmo refeito em 27/11): barra de 10 gotas (0..20 pontos) acima da fome; o ar fica acima dela.
//  - Enche com umas 2-3 garrafas: garrafa de agua e agua fervida = 8 pontos (4 gotas); frascos multiplicam (Vessels).
//  - Agua do mundo (garrafa enchida no rio/mar ou beber direto, agachado e com a mao vazia) NAO e' fervida: mata a
//    sede, mas pode dar nausea. Ferver a garrafa (fornalha, defumador ou fogueira) da a Agua Fervida, sem risco.
//    Na primeira vez aparece uma dica; a garrafa de agua tambem avisa na descricao.
//  - Comidas aquosas dao um pouco (tabela WATERY): melancia, sopas e ensopados, leite, pocoes, frutas.
//  - Gasto NO RITMO DA FOME: o mesmo cansaco da fome (correr, pular, nadar, lutar, curar) e, como a saturacao da fome,
//    uma reserva escondida ("hidratacao") que gasta antes das gotas. Calor (deserto, savana, badlands, Nether)
//    multiplica o gasto e ainda seca um pouco parado; frio gasta menos.
//  - VONTADE: cada ponto +2% de fome e de sede (100 pontos = 3x): a barra continua com 10 itens, cada um vale mais
//    (o que se come/bebe enche proporcionalmente menos e cada item dura proporcionalmente mais). Fome: FoodDataWillMixin.
//  - Igual a fome: com 3 gotas ou menos nao da para correr; zerada, para de regenerar e perde vida a cada 4 s (ate 5
//    coracoes no facil, meio coracao no normal, ate morrer no dificil). No pacifico volta sozinha. Criativo nao gasta.
//  - Futuro: a mana vai usar a sede como combustivel das magias.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class Thirst {
    private Thirst() {}

    public static final float MAX = 20f;
    public static final float NO_SPRINT_AT = 6f;        // igual a fome: 3 gotas ou menos
    public static final float POINT = 4f;               // cansaco que gasta 1 ponto (como a fome)
    public static final float HYDRATION = 0.6f;         // reserva que cada gole deixa (fracao do que deu), como a saturacao
    public static final float HEAT_IDLE_PER_TICK = POINT / 2400f;   // calor: parado, 1 ponto a cada 2 min por "grau" de calor
    public static final float BOTTLE = 8f, SIP = 2f;
    public static final float BOTTLE_NAUSEA = 0.5f, SIP_NAUSEA = 0.2f;
    public static final int NAUSEA_TICKS = 200;
    public static final int DAMAGE_EVERY = 80;

    public static final ResourceKey<DamageType> DEHYDRATION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "dehydration"));

    // o que mata a sede (pontos; 2 = 1 gota). Agua (garrafa) e Agua Fervida ficam no BOTTLE.
    private static final Map<Item, Integer> WATERY = new HashMap<>();
    static {
        WATERY.put(Items.MILK_BUCKET, 6);
        WATERY.put(Items.MUSHROOM_STEW, 5);
        WATERY.put(Items.BEETROOT_SOUP, 5);
        WATERY.put(Items.RABBIT_STEW, 5);
        WATERY.put(Items.SUSPICIOUS_STEW, 5);
        WATERY.put(Items.MELON_SLICE, 3);
        WATERY.put(Items.HONEY_BOTTLE, 2);
        WATERY.put(Items.APPLE, 2);
        WATERY.put(Items.GOLDEN_APPLE, 2);
        WATERY.put(Items.ENCHANTED_GOLDEN_APPLE, 4);
        WATERY.put(Items.SWEET_BERRIES, 1);
        WATERY.put(Items.GLOW_BERRIES, 1);
        WATERY.put(Items.CHORUS_FRUIT, 1);
        WATERY.put(Items.POTION, 4);   // pocoes de efeito (a de agua e' tratada a parte)
    }

    /** sede em pontos visiveis (0..20, com fracao) */
    public static float get(Player p) { return p.getData(ModAttachments.THIRST.get()); }

    /** quanto cada ponto da barra vale com a Vontade (1 = normal, 3 = 100 pontos) */
    public static float will(Player p) { return com.bino.binosvanillaplus.leveling.PlayerStats.willScale(p); }

    public static void set(ServerPlayer p, float v) {
        v = Math.max(0f, Math.min(MAX, v));
        float old = get(p);
        p.setData(ModAttachments.THIRST.get(), v);
        if (shown(old) != shown(v)) sync(p);
        float hyd = p.getData(ModAttachments.THIRST_HYD.get());
        if (hyd > v) p.setData(ModAttachments.THIRST_HYD.get(), v);   // a reserva nunca passa das gotas (como a saturacao)
    }

    /** bebeu/comeu: "pontos de garrafa" (a Vontade faz cada ponto da barra valer mais, entao enche menos) */
    public static void add(ServerPlayer p, float v) {
        float gain = v / will(p);
        set(p, get(p) + gain);
        float hyd = Math.min(get(p), p.getData(ModAttachments.THIRST_HYD.get()) + gain * HYDRATION);
        p.setData(ModAttachments.THIRST_HYD.get(), hyd);
    }

    /** meias gotas mostradas (0..20) */
    public static int shown(float v) { return (int) Math.ceil(v - 1e-4f); }

    public static boolean isWaterBottle(ItemStack s) { return WaterBottles.is(s); }

    public static void sync(ServerPlayer p) { PacketDistributor.sendToPlayer(p, new ThirstSyncPayload(shown(get(p)))); }

    // ---------------------------------------------------------------- gasto e efeitos
    private static final Map<UUID, Float> LAST_FOOD_EXH = new HashMap<>();
    private static final Map<UUID, Integer> DAMAGE_TIMER = new HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        if (p.getAbilities().invulnerable) return;   // criativo / espectador
        ServerLevel lvl = p.serverLevel();
        if (lvl.getDifficulty() == Difficulty.PEACEFUL) {
            if (p.tickCount % 20 == 0) add(p, 1);
            return;
        }
        // esforco: o mesmo que o cansaco da fome subiu neste tick (ele volta 4 quando gasta comida). A Vontade ja vem
        // descontada nele (FoodDataWillMixin).
        float fe = p.getFoodData().getExhaustionLevel();
        Float prev = LAST_FOOD_EXH.put(p.getUUID(), fe);
        float exertion = 0;
        if (prev != null) { exertion = fe - prev; if (exertion < -1f) exertion += POINT; if (exertion < 0) exertion = 0; }
        float heat = heat(p);
        float exh = p.getData(ModAttachments.THIRST_EXH.get()) + exertion * heat + Math.max(0f, heat - 1f) * HEAT_IDLE_PER_TICK / will(p);
        if (exh >= POINT) {
            exh -= POINT;
            float hyd = p.getData(ModAttachments.THIRST_HYD.get());
            if (hyd > 0f) p.setData(ModAttachments.THIRST_HYD.get(), Math.max(0f, hyd - 1f));   // gasta a reserva antes
            else set(p, get(p) - 1f);
        }
        p.setData(ModAttachments.THIRST_EXH.get(), exh);

        if (get(p) <= NO_SPRINT_AT && p.isSprinting()) p.setSprinting(false);

        // zerada: perde vida aos poucos, como a fome
        if (get(p) <= 0f) {
            int t = DAMAGE_TIMER.merge(p.getUUID(), 1, Integer::sum);
            if (t >= DAMAGE_EVERY) {
                DAMAGE_TIMER.put(p.getUUID(), 0);
                Difficulty d = lvl.getDifficulty();
                if (p.getHealth() > 10f || d == Difficulty.HARD || (p.getHealth() > 1f && d == Difficulty.NORMAL))
                    p.hurt(new DamageSource(lvl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DEHYDRATION)), 1f);
            }
        } else DAMAGE_TIMER.remove(p.getUUID());
    }

    // calor: quanto a temperatura (Temperature) multiplica o gasto de sede (1 = nada; etapas de calor 1,25 e 1,5)
    public static float heat(Player p) { return Temperature.thirstFactor(p); }

    // sem agua nenhuma nao regenera sozinho (efeito Regeneracao e cura forte, como a pocao, continuam valendo)
    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getAbilities().invulnerable) return;
        if (get(p) <= 0f && e.getAmount() <= 1.0f && !p.hasEffect(MobEffects.REGENERATION)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- beber
    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = e.getItem();
        if (s.getItem() instanceof BoiledWaterItem) { add(p, BOTTLE); Temperature.refresh(p, true, 60, 15f); return; }
        if (isWaterBottle(s)) {
            add(p, BOTTLE);
            Temperature.refresh(p, true, 60, 15f);
            dirty(p, BOTTLE_NAUSEA);
            return;
        }
        Integer v = WATERY.get(s.getItem());
        if (v != null) add(p, v);
        // temperatura: sopas aquecem, melancia refresca
        if (s.is(Items.MUSHROOM_STEW) || s.is(Items.BEETROOT_SOUP) || s.is(Items.RABBIT_STEW) || s.is(Items.SUSPICIOUS_STEW))
            Temperature.refresh(p, false, 60, 10f);
        else if (s.is(Items.MELON_SLICE)) Temperature.refresh(p, true, 30, 5f);
    }

    // agua do mundo: pode dar nausea; a primeira vez mostra a dica
    public static void dirty(ServerPlayer p, float chance) {
        if (p.getRandom().nextFloat() < chance) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0));
        if (!p.getData(ModAttachments.THIRST_HINT.get())) {
            p.setData(ModAttachments.THIRST_HINT.get(), true);
            p.sendSystemMessage(Component.translatable("message.binosvanillaplus.thirst.dirty_hint").withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
    }

    // beber direto da agua: agachado, mao vazia, olhando para agua ao alcance (o cliente pede, o servidor confere)
    private static final Map<UUID, Long> LAST_SIP = new HashMap<>();

    public static void onSip(ServerPlayer p) {
        if (!p.isShiftKeyDown() || !p.getMainHandItem().isEmpty() || get(p) >= MAX) return;
        long now = p.level().getGameTime();
        Long last = LAST_SIP.get(p.getUUID());
        if (last != null && now - last < 12) return;
        HitResult hit = p.pick(p.blockInteractionRange(), 1f, true);
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bh.getBlockPos();
        if (!p.level().getFluidState(pos).is(FluidTags.WATER)) return;
        LAST_SIP.put(p.getUUID(), now);
        add(p, SIP);
        Temperature.refresh(p, true, 20, 5f);
        dirty(p, SIP_NAUSEA);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.1f + p.getRandom().nextFloat() * 0.2f);
        var v = bh.getLocation();
        p.serverLevel().sendParticles(ParticleTypes.SPLASH, v.x, v.y + 0.1, v.z, 6, 0.15, 0.02, 0.15, 0.1);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    // ---------------------------------------------------------------- sincronizar
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        LAST_FOOD_EXH.remove(id); DAMAGE_TIMER.remove(id); LAST_SIP.remove(id);
    }
}

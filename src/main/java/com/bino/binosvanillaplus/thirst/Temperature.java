package com.bino.binosvanillaplus.thirst;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.leveling.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TEMPERATURA (27/11, balanceada para ser "so um pouco mais dificil"): o HUD mostra a temperatura do AMBIENTE (°C).
//  - Ambiente: bioma (dia/noite), altura, cavernas (puxam para 16 °C), chuva/neve, molhado, abrigo (teto em cima) e
//    fontes por perto (fogueira +7, lava, fogo, fornalha, tocha; gelo e neve em po esfriam).
//  - Faixas: azul <= 0 congelante, ciano 1-14 frio, amarelo 15-27 agradavel, laranja 28-37 quente, vermelho >= 38.
//  - Exposicao escondida (-100 frio .. +100 calor): enche devagar fora da faixa agradavel e esvazia mais rapido nela.
//    Etapas em 30 / 60 / 90; a etapa 3 (dano) so chega em extremos de verdade (<= -5 °C ou >= 42 °C, ja com protecao).
//      frio 1: fome gasta um pouco mais   | calor 1: sede gasta 25% mais (Thirst)
//      frio 2: + ataque/mineracao -10%, tremedeira leve | calor 2: sede 50% mais, borda quente na tela
//      frio 3: + borda de gelo, meio coracao a cada 6 s | calor 3: + 10% mais lento, meio coracao a cada 6 s
//    (como a fome: no facil para em 5 coracoes, no normal em meio coracao; no dificil pode matar)
//  - Protecao: forro na armadura (la = +3 °C de frio por peca; leve = +3 °C de calor por peca), encantamento Isolamento
//    (+1 °C para os dois lados por nivel em cada peca), beber agua (refresca 60 s e tira calor), cha quente e sopas
//    (aquecem), melancia e Refresco (refrescam). Fogueira por perto tira o frio.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class Temperature {
    private Temperature() {}

    public static final float COMFORT_LOW = 15f, COMFORT_HIGH = 27f;
    public static final float EXTREME_COLD = -5f, EXTREME_HOT = 42f;
    public static final float FILL_PER_DEGREE = 0.03f;   // exposicao por segundo para cada grau fora da faixa
    public static final float RECOVER = 1.0f;            // exposicao que volta por segundo na faixa agradavel
    public static final float STAGE_1 = 30f, STAGE_2 = 60f, STAGE_3 = 90f;
    public static final float LINING = 3f, INSULATION = 1f;
    public static final int DAMAGE_EVERY = 120;

    public static final ResourceKey<DamageType> HEATSTROKE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "heatstroke"));
    public static final ResourceKey<Enchantment> INSULATION_KEY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "insulation"));

    public static final int LINING_NONE = 0, LINING_WOOL = 1, LINING_LIGHT = 2;

    // ---------------------------------------------------------------- ambiente
    /** temperatura do lugar (°C) para este jogador agora */
    public static float ambient(Player p) {
        Level level = p.level();
        BlockPos pos = p.blockPosition();
        if (level.dimension() == Level.NETHER) return 34f + sources(level, pos) * 0.5f;
        if (level.dimension() == Level.END) return 0f + sources(level, pos);
        Holder<Biome> biome = level.getBiome(pos);
        float[] dn = dayNight(biome);
        long t = level.getDayTime() % 24000L;
        float day = 0.5f + 0.5f * Mth.cos((float) ((t - 6000L) / 24000.0 * Math.PI * 2));   // meio-dia 1, meia-noite 0
        float temp = Mth.lerp(day, dn[1], dn[0]);
        int y = pos.getY();
        if (y > 100) temp -= (y - 100) / 15f;                                       // montanha: esfria com a altura
        boolean sky = level.canSeeSky(BlockPos.containing(p.getEyePosition()));
        if (!sky && y < 50) temp = Mth.lerp(Mth.clamp((50 - y) / 10f, 0f, 1f), temp, 16f);   // caverna: estavel
        else if (!sky) temp = 20f + (temp - 20f) * 0.6f;                            // abrigo: teto corta sol/frio
        if (sky && level.isRaining()) {
            Biome.Precipitation pr = biome.value().getPrecipitationAt(pos);
            if (pr == Biome.Precipitation.RAIN) temp -= 3f;
            else if (pr == Biome.Precipitation.SNOW) temp -= 4f;
        }
        if (wet(p)) temp -= 3f;
        return temp + sources(level, pos);
    }

    // {dia, noite} de cada bioma; os outros pela temperatura do bioma no jogo
    private static float[] dayNight(Holder<Biome> b) {
        if (b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BADLANDS)) return new float[]{36f, 12f};
        if (b.is(BiomeTags.IS_SAVANNA)) return new float[]{30f, 20f};
        if (b.is(BiomeTags.IS_JUNGLE)) return new float[]{28f, 22f};
        if (b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return new float[]{24f, 18f};
        float bt = b.value().getBaseTemperature();
        if (b.is(BiomeTags.IS_OCEAN) && bt >= 0.5f) return new float[]{19f, 15f};
        float d = 4f + 20f * bt;    // planicie (0,8) 20; floresta (0,7) 18; taiga (0,25) 9; neve (0) 4
        return new float[]{d, d - 8f};
    }

    // fogueira, lava, fogo, fornalha e tocha aquecem; gelo e neve em po esfriam (o mais forte de cada lado vale)
    private static float sources(Level level, BlockPos at) {
        float heat = 0f, cold = 0f;
        for (BlockPos b : BlockPos.betweenClosed(at.offset(-4, -2, -4), at.offset(4, 2, 4))) {
            BlockState s = level.getBlockState(b);
            if (s.isAir()) continue;
            double d = Math.sqrt(b.distSqr(at));
            float h = 0f, c = 0f;
            if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) h = fall(7f, d, 4.5);
            else if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) h = fall(5f, d, 3.5);
            else if (s.getFluidState().is(FluidTags.LAVA)) h = fall(10f, d, 4.5);
            else if (s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(AbstractFurnaceBlock.LIT)) h = fall(4f, d, 3.5);
            else if (s.is(Blocks.MAGMA_BLOCK)) h = fall(2f, d, 2.5);
            else if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_TORCH)
                    || s.is(Blocks.SOUL_WALL_TORCH) || s.is(Blocks.SOUL_LANTERN)) h = fall(1f, d, 2.5);
            else if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE) || s.is(Blocks.POWDER_SNOW)) c = fall(3f, d, 2.5);
            heat = Math.max(heat, h);
            cold = Math.max(cold, c);
        }
        return heat - cold;
    }

    private static float fall(float v, double d, double range) { return d >= range ? 0f : v * (float) (1.0 - d / range); }

    // ---------------------------------------------------------------- protecao
    /** quantos graus a mais de frio [0] e de calor [1] este jogador aguenta (forros, Isolamento, bebidas) */
    public static float[] tolerance(ServerPlayer p) {
        float cold = 0f, hot = 0f;
        Holder<Enchantment> ins = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(INSULATION_KEY).orElse(null);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(slot);
            if (s.isEmpty()) continue;
            int lining = s.getOrDefault(ModDataComponents.LINING.get(), LINING_NONE);
            if (lining == LINING_WOOL) cold += LINING;
            else if (lining == LINING_LIGHT) hot += LINING;
            if (ins != null) {
                int lv = EnchantmentHelper.getItemEnchantmentLevel(ins, s);
                cold += lv * INSULATION; hot += lv * INSULATION;
            }
        }
        long now = p.level().getGameTime();
        if (p.getData(ModAttachments.TEMP_COOL_UNTIL.get()) > now) hot += 5f;
        if (p.getData(ModAttachments.TEMP_WARM_UNTIL.get()) > now) cold += 5f;
        return new float[]{cold, hot};
    }

    /** bebeu/comeu algo que refresca (cool) ou aquece: protecao por alguns segundos e tira um pouco da exposicao */
    public static void refresh(ServerPlayer p, boolean cool, int seconds, float exposureRelief) {
        long until = p.level().getGameTime() + seconds * 20L;
        var key = cool ? ModAttachments.TEMP_COOL_UNTIL.get() : ModAttachments.TEMP_WARM_UNTIL.get();
        p.setData(key, Math.max(p.getData(key), until));
        float e = p.getData(ModAttachments.TEMP_EXPOSURE.get());
        if (cool && e > 0) e = Math.max(0f, e - exposureRelief);
        if (!cool && e < 0) e = Math.min(0f, e + exposureRelief);
        p.setData(ModAttachments.TEMP_EXPOSURE.get(), e);
    }

    // ---------------------------------------------------------------- etapas
    public static float exposure(Player p) { return p.getData(ModAttachments.TEMP_EXPOSURE.get()); }

    /** etapa de calor (0..3) */
    public static int hotStage(Player p) { float e = exposure(p); return e >= STAGE_3 ? 3 : e >= STAGE_2 ? 2 : e >= STAGE_1 ? 1 : 0; }
    /** etapa de frio (0..3) */
    public static int coldStage(Player p) { float e = -exposure(p); return e >= STAGE_3 ? 3 : e >= STAGE_2 ? 2 : e >= STAGE_1 ? 1 : 0; }

    /** quanto o calor multiplica o gasto de sede (Thirst) */
    public static float thirstFactor(Player p) { int s = hotStage(p); return s == 0 ? 1f : s == 1 ? 1.25f : 1.5f; }

    // ---------------------------------------------------------------- a cada tick
    private static final Map<UUID, Integer> WET = new HashMap<>();
    private static final Map<UUID, Float> AMBIENT = new HashMap<>();
    private static final Map<UUID, Integer> DAMAGE_TIMER = new HashMap<>();
    private static final Map<UUID, int[]> SENT = new HashMap<>();

    private static boolean wet(Player p) { return WET.getOrDefault(p.getUUID(), 0) > 0; }

    private static final ResourceLocation SLOW_ATTACK = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "cold_attack");
    private static final ResourceLocation SLOW_MINE = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "cold_mining");
    private static final ResourceLocation SLOW_MOVE = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "heat_speed");

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        UUID id = p.getUUID();
        if (p.isInWaterRainOrBubble()) WET.put(id, 400); else WET.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
        ServerLevel level = p.serverLevel();
        boolean safe = p.getAbilities().invulnerable || level.getDifficulty() == Difficulty.PEACEFUL;
        if (p.tickCount % 20 == 0) {   // uma vez por segundo
            float amb = ambient(p);
            AMBIENT.put(id, amb);
            float[] tol = tolerance(p);
            float low = COMFORT_LOW - tol[0], high = COMFORT_HIGH + tol[1];
            float ex = exposure(p);
            if (safe) ex = 0f;
            else if (amb < low) {
                float cap = amb <= EXTREME_COLD - tol[0] ? 100f : STAGE_3 - 1f;   // dano so em frio extremo
                ex = Math.max(-cap, Math.min(ex, ex - (low - amb) * FILL_PER_DEGREE * (ex > 0 ? 3f : 1f)));
                if (ex > 0) ex = Math.max(0f, ex - RECOVER * 2f);
            } else if (amb > high) {
                float cap = amb >= EXTREME_HOT + tol[1] ? 100f : STAGE_3 - 1f;
                ex = Math.min(cap, Math.max(ex, ex + (amb - high) * FILL_PER_DEGREE * (ex < 0 ? 3f : 1f)));
                if (ex < 0) ex = Math.min(0f, ex + RECOVER * 2f);
            } else {
                ex = ex > 0 ? Math.max(0f, ex - RECOVER) : Math.min(0f, ex + RECOVER);
            }
            p.setData(ModAttachments.TEMP_EXPOSURE.get(), ex);
            effects(p);
            sync(p, amb);
        }
        if (safe) return;
        int cold = coldStage(p), hot = hotStage(p);
        if (cold >= 1) p.causeFoodExhaustion(cold >= 2 ? 0.002f : 0.001f);   // frio: o corpo queima mais comida
        if (cold >= 3) p.setTicksFrozen(Math.max(p.getTicksFrozen(), (int) (p.getTicksRequiredToFreeze() * 0.7f)));   // borda de gelo
        if (cold >= 3 || hot >= 3) {
            int t = DAMAGE_TIMER.merge(id, 1, Integer::sum);
            if (t >= DAMAGE_EVERY) {
                DAMAGE_TIMER.put(id, 0);
                Difficulty d = level.getDifficulty();
                if (p.getHealth() > 10f || d == Difficulty.HARD || (p.getHealth() > 1f && d == Difficulty.NORMAL)) {
                    DamageSource src = cold >= 3 ? p.damageSources().freeze()
                            : new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HEATSTROKE));
                    p.hurt(src, 1f);
                }
            }
        } else DAMAGE_TIMER.remove(id);
    }

    // modificadores de atributo das etapas 2 e 3
    private static void effects(ServerPlayer p) {
        int cold = coldStage(p), hot = hotStage(p);
        float slow = cold >= 3 ? -0.2f : cold >= 2 ? -0.1f : 0f;
        modify(p, Attributes.ATTACK_SPEED, SLOW_ATTACK, slow);
        modify(p, Attributes.BLOCK_BREAK_SPEED, SLOW_MINE, slow);
        modify(p, Attributes.MOVEMENT_SPEED, SLOW_MOVE, hot >= 3 ? -0.1f : 0f);
    }

    private static void modify(Player p, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, float amount) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && Math.abs(cur.amount() - amount) < 1e-4) return;
        inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void sync(ServerPlayer p, float amb) {
        int a = Math.round(amb), ex = Math.round(exposure(p));
        int[] last = SENT.get(p.getUUID());
        if (last != null && last[0] == a && last[1] == ex) return;
        SENT.put(p.getUUID(), new int[]{a, ex});
        PacketDistributor.sendToPlayer(p, new TemperatureSyncPayload(a, ex));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        WET.remove(id); AMBIENT.remove(id); DAMAGE_TIMER.remove(id); SENT.remove(id);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { SENT.remove(e.getEntity().getUUID()); }
}

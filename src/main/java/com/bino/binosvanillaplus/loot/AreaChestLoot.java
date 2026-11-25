package com.bino.binosvanillaplus.loot;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.leveling.RegionDifficulty;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// Loot de bau melhora com o NIVEL DA AREA (RegionDifficulty.levelAt no lugar do bau, 1 a 100).
// Vale para toda tabela "chests/..." (do jogo, do mod e de outros mods), na hora em que o bau e' aberto pela 1a vez.
//  - Quantidade: pilhas crescem ate x2 no nivel 100 (sem passar do maximo da pilha).
//  - Itens extras: 1 a cada 20 niveis (0 a 5), sorteados de faixas que vao abrindo com o nivel.
//  - Ferramentas, armas e armaduras do loot podem vir encantadas (chance e forca sobem com o nivel).
public class AreaChestLoot extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BinosVanillaMod.MOD_ID);

    public static final MapCodec<AreaChestLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, AreaChestLoot::new));
    public static final Supplier<MapCodec<AreaChestLoot>> AREA_CHEST_LOOT = SERIALIZERS.register("area_chest_loot", () -> CODEC);

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }

    public AreaChestLoot(LootItemCondition[] conditions) { super(conditions); }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    // faixas de itens extras: {nivel minimo, id, quantidade minima, maxima, peso}
    private record Bonus(int minLevel, String id, int min, int max, int weight) {}
    private static final List<Bonus> BONUS = List.of(
            new Bonus(1, "minecraft:torch", 4, 12, 30),
            new Bonus(1, "minecraft:bread", 2, 5, 25),
            new Bonus(1, "minecraft:iron_ingot", 1, 4, 25),
            new Bonus(1, "minecraft:coal", 3, 10, 20),
            new Bonus(15, "minecraft:gold_ingot", 2, 5, 20),
            new Bonus(15, "minecraft:redstone", 4, 12, 15),
            new Bonus(15, "minecraft:lapis_lazuli", 4, 10, 15),
            new Bonus(15, "binosmining:raw_rubi", 1, 3, 10),
            new Bonus(15, "binosmining:raw_sapphire", 1, 3, 10),
            new Bonus(15, "binosmining:raw_jade", 1, 3, 10),
            new Bonus(30, "minecraft:diamond", 1, 2, 10),
            new Bonus(30, "minecraft:golden_apple", 1, 2, 8),
            new Bonus(30, "binosmining:rubi", 1, 2, 8),
            new Bonus(30, "binosmining:sapphire", 1, 2, 8),
            new Bonus(30, "binosmining:jade", 1, 2, 8),
            new Bonus(30, "binosmining:forge_stone_1", 1, 2, 8),
            new Bonus(30, "binosmining:forge_stone_2", 1, 1, 6),
            new Bonus(45, "minecraft:book", 1, 1, 10),            // vira livro encantado
            new Bonus(45, "minecraft:emerald", 2, 6, 10),
            new Bonus(45, "binosmining:forge_stone_3", 1, 1, 6),
            new Bonus(45, "binosmining:forge_stone_4", 1, 1, 4),
            new Bonus(60, "minecraft:diamond", 2, 4, 10),
            new Bonus(60, "binosmining:forge_stone_5", 1, 1, 4),
            new Bonus(60, "binosmining:forge_stone_6", 1, 1, 3),
            new Bonus(60, "binosmining:philosopher_fragment", 1, 1, 3),
            new Bonus(75, "minecraft:netherite_scrap", 1, 2, 5),
            new Bonus(75, "binosmining:forge_stone_7", 1, 1, 2),
            new Bonus(75, "binosmining:forge_stone_8", 1, 1, 1),
            new Bonus(90, "minecraft:enchanted_golden_apple", 1, 1, 1),
            new Bonus(90, "binosmining:forge_stone_9", 1, 1, 1));

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        ResourceLocation table = ctx.getQueriedLootTableId();
        if (table == null || !table.getPath().startsWith("chests/")) return loot;
        Vec3 origin = ctx.getParamOrNull(LootContextParams.ORIGIN);
        ServerLevel level = ctx.getLevel();
        if (origin == null) return loot;
        double lv = RegionDifficulty.levelAt(level, BlockPos.containing(origin));
        if (lv < 1) return loot;   // lugar sem nivel (Dimensao Portatil)
        RandomSource rnd = ctx.getRandom();

        // 1) quantidade
        double mult = 1 + (lv - 1) / 99.0;
        for (ItemStack s : loot)
            if (s.isStackable() && s.getCount() > 0)
                s.setCount(Mth.clamp((int) Math.round(s.getCount() * mult), 1, s.getMaxStackSize()));

        // 2) itens extras
        int extra = (int) (lv / 20);
        List<Bonus> open = BONUS.stream().filter(b -> lv >= b.minLevel()).toList();
        int total = open.stream().mapToInt(Bonus::weight).sum();
        for (int i = 0; i < extra && total > 0; i++) {
            int r = rnd.nextInt(total);
            for (Bonus b : open) {
                if ((r -= b.weight()) >= 0) continue;
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(b.id()));
                if (item != Items.AIR) loot.add(new ItemStack(item, Mth.nextInt(rnd, b.min(), b.max())));
                break;
            }
        }

        // 3) encantar equipamento (e livros)
        float chance = (float) Math.min(0.9, lv / 100.0);
        int power = Mth.clamp((int) (5 + lv * 0.3), 5, 30);
        for (int i = 0; i < loot.size(); i++) {
            ItemStack s = loot.get(i);
            if (s.isEnchanted() || !s.isEnchantable() || rnd.nextFloat() >= (s.is(Items.BOOK) ? 1f : chance)) continue;
            loot.set(i, EnchantmentHelper.enchantItem(rnd, s, power, level.registryAccess(), Optional.empty()));
        }
        return loot;
    }
}

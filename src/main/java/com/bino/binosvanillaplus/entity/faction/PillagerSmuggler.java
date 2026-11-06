package com.bino.binosvanillaplus.entity.faction;

import com.bino.binosvanillaplus.faction.Faction;
import com.bino.binosvanillaplus.faction.FactionMob;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

// Contrabandista Pillager: o mercador ambulante do lado pillager. Aparece perto de jogadores Pillager
// (FactionAI) e vende "contrabando". Jogadores Villager ele nem atende.
public class PillagerSmuggler extends WanderingTrader implements FactionMob {
    public PillagerSmuggler(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // nao foge dos illagers (sao do lado dele); foge dos guardas da vila, golens de ferro e zumbis
        goalSelector.removeAllGoals(g -> g instanceof AvoidEntityGoal<?>);
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zoglin.class, 10f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, VillagerGuard.class, 10f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, IronGolem.class, t -> !(t instanceof PillagerGolem), 10f, 0.5, 0.5, t -> true));
    }

    @Override
    public Faction faction() { return Faction.PILLAGER; }

    private static MerchantOffer sell(int emeralds, Item item, int count, int uses) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), uses, 1, 0.05f);
    }

    private static MerchantOffer buy(Item item, int count, int emeralds, int uses) {
        return new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD, emeralds), uses, 1, 0.05f);
    }

    @Override
    protected void updateTrades() {
        List<MerchantOffer> common = new ArrayList<>(List.of(
                sell(2, Items.CROSSBOW, 1, 3),
                sell(1, Items.ARROW, 16, 8),
                sell(1, Items.GUNPOWDER, 4, 8),
                sell(3, Items.SADDLE, 1, 2),
                sell(4, Items.TNT, 1, 4),
                sell(2, Items.LEATHER, 4, 6),
                sell(5, Items.IRON_AXE, 1, 2),
                sell(1, Items.DARK_OAK_LOG, 8, 6),
                buy(Items.STRING, 14, 1, 8),
                buy(Items.IRON_INGOT, 4, 1, 8)));
        List<MerchantOffer> rare = new ArrayList<>(List.of(
                sell(16, Items.TOTEM_OF_UNDYING, 1, 1),
                sell(10, Items.OMINOUS_BOTTLE, 1, 1),
                sell(8, Items.NAME_TAG, 1, 1)));
        MerchantOffers offers = getOffers();
        for (int i = 0; i < 6 && !common.isEmpty(); i++) offers.add(common.remove(random.nextInt(common.size())));
        offers.add(rare.get(random.nextInt(rare.size())));
    }
}

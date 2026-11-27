package com.bino.binosvanillaplus;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.entity.ModEntities;
import com.bino.binosvanillaplus.entity.ModParticles;
import com.bino.binosvanillaplus.item.ModCreativeModeTabs;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BinosVanillaMod.MOD_ID)
public class BinosVanillaMod {

    public static final String MOD_ID = "binosvanillaplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public BinosVanillaMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModEntities.register(modEventBus);
        ModParticles.register(modEventBus);

        com.bino.binosvanillaplus.item.ModDataComponents.register(modEventBus);   // nível de forja dos itens
        com.bino.binosvanillaplus.menu.ModMenuTypes.register(modEventBus);   // janelas (menu da Mesa de Lapidação)
        com.bino.binosvanillaplus.recipe.ModRecipes.register(modEventBus);   // receita de upgrade da Chave Dimensional
        com.bino.binosvanillaplus.sound.ModSounds.register(modEventBus);   // sons (apito, sonar, camera)
        com.bino.binosvanillaplus.leveling.ModAttachments.register(modEventBus);   // nivel dos mobs
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        com.bino.binosvanillaplus.worldgen.ModFeatures.register(modEventBus);   // fosseis gerados no mundo
        com.bino.binosvanillaplus.loot.AreaChestLoot.register(modEventBus);   // loot de bau melhora com o nivel da area
        com.bino.binosvanillaplus.block.entity.ModBlockEntities.register(modEventBus);   // dono da corda solta

        // ganchos do Mining: nivel dos mobs na curva da armadura e o "Miss" do desvio do conjunto de rubi
        com.bino.binosmining.MiningHooks.mobLevel = le -> com.bino.binosvanillaplus.leveling.MobLevels.eligible(le)
                ? Math.max(1, com.bino.binosvanillaplus.leveling.MobLevels.get(le)) : 0;
        com.bino.binosmining.MiningHooks.dodged = com.bino.binoscombat.combat.CombatEvents::miss;
        // Combat (mod separado): niveis, atributos e faccoes do Vanilla+ entram no combate
        com.bino.binoscombat.CombatHooks.mobLevelFn = com.bino.binosvanillaplus.leveling.MobLevels::get;
        com.bino.binoscombat.CombatHooks.extraFighterFn = e -> e instanceof com.bino.binosvanillaplus.faction.FactionMob;
        com.bino.binoscombat.CombatHooks.agileMobFn = m -> m instanceof com.bino.binosvanillaplus.entity.rat.RatEntity;
        com.bino.binoscombat.CombatHooks.vitalityFn = com.bino.binosvanillaplus.leveling.PlayerStats::vitality;
        com.bino.binoscombat.CombatHooks.postureMultFn = com.bino.binosvanillaplus.leveling.PlayerStats::postureMult;
        com.bino.binoscombat.CombatHooks.critChanceFn = com.bino.binosvanillaplus.leveling.PlayerStats::critChance;
        com.bino.binoscombat.CombatHooks.critDamageFn = com.bino.binosvanillaplus.leveling.PlayerStats::critDamage;
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);   // °C ou °F
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.bino.binosvanillaplus.block.custom.MilkCauldronBlock::registerVanillaInteraction);   // leite no caldeirao

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // O NeoForge detecta sozinho o bus correto (MOD ou GAME) pelo tipo do evento
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // botao Receitas+ na cena da bancada do Mining
            com.bino.binosmining.client.MiningClientHooks.recipeBrowser = s -> new com.bino.binosvanillaplus.client.recipes.RecipeLookupScreen(s,
                    com.bino.binosvanillaplus.client.recipes.RecipeLookupScreen.Mode.BROWSE, null);
            com.bino.binosmining.client.MiningClientHooks.isRecipeBrowser = s -> s instanceof com.bino.binosvanillaplus.client.recipes.RecipeLookupScreen;
            // a textura da Chave Dimensional muda com o nivel (ver models/item/dimensional_key.json)
            event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(
                    com.bino.binosvanillaplus.item.ModItems.DIMENSIONAL_KEY.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "key_level"),
                    (stack, level, entity, seed) -> com.bino.binosvanillaplus.item.custom.DimensionalKeyItem.level(stack)));
        }
    }
}
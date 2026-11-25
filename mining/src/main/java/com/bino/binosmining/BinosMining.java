package com.bino.binosmining;

import com.bino.binosmining.block.MiningBlocks;
import com.bino.binosmining.block.entity.MiningBlockEntities;
import com.bino.binosmining.item.MiningComponents;
import com.bino.binosmining.item.MiningCreativeTab;
import com.bino.binosmining.item.MiningItems;
import com.bino.binosmining.item.ModArmorMaterials;
import com.bino.binosmining.menu.MiningMenus;
import com.bino.binosmining.sound.MiningSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

// Bino's Vanilla+ Mining: gemas (rubi, safira, jade), ferramentas e armaduras de gema, pedras de forja, Pedra Filosofal,
// Mesa de Lapidacao e as cenas da bancada de trabalho e da bigorna. Funciona sozinho; com o Bino's Vanilla+ ganha
// extras (ver MiningHooks).
@Mod(BinosMining.MOD_ID)
public class BinosMining {
    public static final String MOD_ID = "binosmining";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BinosMining(IEventBus modEventBus, ModContainer modContainer) {
        MiningCreativeTab.register(modEventBus);
        ModArmorMaterials.register(modEventBus);   // materiais das armaduras de gema
        MiningComponents.register(modEventBus);    // nivel de forja dos itens
        MiningMenus.register(modEventBus);         // janela da Mesa de Lapidacao
        MiningSounds.register(modEventBus);        // sons da Mesa de Lapidacao
        MiningItems.register(modEventBus);
        MiningBlocks.register(modEventBus);
        MiningBlockEntities.register(modEventBus);

        // Durabilidade x1.25 das ferramentas de diamante (evento do barramento do mod)
        modEventBus.addListener(com.bino.binosmining.event.DiamondBuffEvents::onModifyDefaultComponents);
        modEventBus.addListener(BinosMining::addCreative);
    }

    // rubi e seus blocos tambem nas abas do jogo
    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(MiningItems.RUBI);
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(MiningBlocks.RUBI_ORE);
            event.accept(MiningBlocks.RUBI_DEEPSLATE_ORE);
            event.accept(MiningBlocks.RUBI_BLOCK);
        }
    }
}

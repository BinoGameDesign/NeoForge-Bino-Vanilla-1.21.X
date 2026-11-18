package com.bino.binosvanillaplus.compat.jade;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.custom.MilkCauldronBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

// Integracao opcional com o Jade: so e carregada se o Jade estiver instalado.
@WailaPlugin
public class BinosJadePlugin implements IWailaPlugin {
    public static final ResourceLocation MILK_CAULDRON = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "milk_cauldron");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MilkCauldronProvider.INSTANCE, MilkCauldronBlock.class);
    }

    enum MilkCauldronProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            for (Component line : MilkCauldronBlock.describe(accessor.getBlockState(), accessor.getLevel(), accessor.getPosition()))
                tooltip.add(line);
        }

        @Override
        public ResourceLocation getUid() { return MILK_CAULDRON; }
    }
}

package com.bino.binosvanillaplus.compat.jade;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.custom.MilkCauldronBlock;
import com.bino.binosvanillaplus.leveling.MobLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

// Integracao opcional com o Jade: so e carregada se o Jade estiver instalado.
// Com o Jade presente, o painel proprio do mod (LookAtHud) se esconde e as informacoes vem para ca.
@WailaPlugin
public class BinosJadePlugin implements IWailaPlugin {
    public static final ResourceLocation MILK_CAULDRON = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "milk_cauldron");
    public static final ResourceLocation MOB_LEVEL = ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "mob_level");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MilkCauldronProvider.INSTANCE, MilkCauldronBlock.class);
        registration.registerEntityComponent(MobLevelProvider.INSTANCE, LivingEntity.class);
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

    enum MobLevelProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            int lv = MobLevels.get(accessor.getEntity());
            if (lv > 0) tooltip.add(Component.translatable("lookat.binosvanillaplus.mob_level", lv).withStyle(ChatFormatting.GREEN));
        }

        @Override
        public ResourceLocation getUid() { return MOB_LEVEL; }
    }
}

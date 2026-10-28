package com.bino.binosvanillaplus.client.lookat;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

// Informacoes basicas. Cada classe interna e' um provider independente.
public class BasicProviders {

    // Ferramenta ideal + se a que voce segura serve + dureza
    public static class Tool implements LookAtProvider {
        @Override
        public void appendBlock(LookAtInfo info, Level level, BlockPos pos, BlockState state, LocalPlayer player) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) {
                info.line(Component.translatable("lookat.binosvanillaplus.unbreakable").withStyle(ChatFormatting.RED));
                return;
            }
            String tool = null;
            if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) tool = "pickaxe";
            else if (state.is(BlockTags.MINEABLE_WITH_AXE)) tool = "axe";
            else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) tool = "shovel";
            else if (state.is(BlockTags.MINEABLE_WITH_HOE)) tool = "hoe";
            if (tool != null) {
                boolean ok = !state.requiresCorrectToolForDrops() || player.getMainHandItem().isCorrectToolForDrops(state);
                info.line(Component.translatable("lookat.binosvanillaplus.tool",
                                Component.translatable("lookat.binosvanillaplus.tool." + tool))
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            info.line(Component.translatable("lookat.binosvanillaplus.hardness",
                    String.format(Locale.ROOT, "%.1f", hardness)).withStyle(ChatFormatting.GRAY));
        }
    }

    // Plantacoes: usa a propriedade "age" do bloco
    public static class Crop implements LookAtProvider {
        @Override
        public void appendBlock(LookAtInfo info, Level level, BlockPos pos, BlockState state, LocalPlayer player) {
            for (Property<?> p : state.getProperties()) {
                if (p instanceof IntegerProperty ip && ip.getName().equals("age")) {
                    int max = ip.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(1);
                    int pct = Math.round(100f * state.getValue(ip) / max);
                    info.line(Component.translatable("lookat.binosvanillaplus.growth", pct)
                            .withStyle(pct >= 100 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                    return;
                }
            }
        }
    }

    // Vida de seres vivos
    public static class Health implements LookAtProvider {
        @Override
        public void appendEntity(LookAtInfo info, Level level, Entity entity, LocalPlayer player) {
            if (entity instanceof LivingEntity living) {
                info.health = living.getHealth();
                info.maxHealth = living.getMaxHealth();
                info.line(Component.literal(String.format(Locale.ROOT, "%.1f / %.1f",
                        info.health, info.maxHealth)).withStyle(ChatFormatting.RED));
            }
        }
    }
}

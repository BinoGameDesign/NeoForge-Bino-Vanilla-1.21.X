package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.pocket.PocketDimension;
import com.bino.binosvanillaplus.pocket.PocketManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

// Chave Dimensional. Clicar numa superficie cria a Porta Dimensional (uma por dono). O nivel da chave (quantos
// upgrades de minerio ja recebeu) define o tamanho da sala: 9x9 + 2 por nivel. Os minerios ja usados ficam
// guardados na chave e nao podem ser usados de novo.
public class DimensionalKeyItem extends Item {
    public DimensionalKeyItem(Properties properties) { super(properties); }

    public static int level(ItemStack stack) { return stack.getOrDefault(ModDataComponents.KEY_LEVEL.get(), 0); }

    public static List<ResourceLocation> usedOres(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.KEY_ORES.get(), List.of());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        return PocketManager.placeDoor(sp, sl, ctx.getClickedPos(), ctx.getClickedFace(), ctx.getItemInHand());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int size = PocketDimension.sizeFor(level(stack));
            player.displayClientMessage(Component.translatable("message.binosvanillaplus.pocket_key_info", level(stack), size, size), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return level(stack) > 0; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        int lvl = level(stack);
        int size = PocketDimension.sizeFor(lvl);
        tips.add(Component.translatable("tooltip.binosvanillaplus.key_level", lvl, size, size).withStyle(ChatFormatting.LIGHT_PURPLE));
        List<ResourceLocation> used = usedOres(stack);
        if (!used.isEmpty()) {
            net.minecraft.network.chat.MutableComponent joined = Component.empty();
            for (int i = 0; i < used.size(); i++) {
                if (i > 0) joined.append(", ");
                joined.append(new ItemStack(BuiltInRegistries.ITEM.get(used.get(i))).getHoverName());
            }
            tips.add(Component.translatable("tooltip.binosvanillaplus.key_used", joined).withStyle(ChatFormatting.GRAY));
        }
        tips.add(Component.translatable("tooltip.binosvanillaplus.key_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

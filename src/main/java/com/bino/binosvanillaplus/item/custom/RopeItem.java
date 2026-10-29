package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.block.custom.RopeBlock;
import com.bino.binosvanillaplus.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Corda: botao direito num bloco joga a corda dali para baixo (corda SOLTA: quem joga nao consegue escalar nela,
// mas um amigo consegue). Para subir/descer sozinho, use o Gancho (ele ja traz a propria corda).
// O tamanho e' a quantidade de cordas que voce tem (limitada por MAX_LENGTH). Botao direito na corda recolhe.
public class RopeItem extends Item {
    public static final int MAX_LENGTH = 64;
    // true = conta todas as cordas do inventario; false = so a pilha na mao
    public static final boolean USE_WHOLE_INVENTORY = true;

    public RopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos clicked = ctx.getClickedPos();
        BlockState cs = level.getBlockState(clicked);

        if (cs.is(ModBlocks.ROPE.get())) {
            if (cs.getValue(RopeBlock.ANCHORED)) return InteractionResult.PASS;
            if (!level.isClientSide) RopeBlock.pull(level, clicked, cs, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) hangLoose(level, clicked.relative(ctx.getClickedFace()), player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Joga a corda solta a partir de "start" (precisa haver espaco livre embaixo para ela pender). So no servidor.
    public static boolean hangLoose(Level level, BlockPos start, Player player) {
        boolean creative = player.hasInfiniteMaterials();
        int available = creative ? MAX_LENGTH : Math.min(MAX_LENGTH, count(player));
        if (available <= 0) {
            player.displayClientMessage(Component.translatable("message.binosvanillaplus.rope_none"), true);
            return false;
        }
        if (!RopeBlock.isFree(level.getBlockState(start)) || !RopeBlock.isFree(level.getBlockState(start.below()))) {
            player.displayClientMessage(Component.translatable("message.binosvanillaplus.rope_no_space"), true);
            return false;
        }
        int placed = RopeBlock.hang(level, start, available, false, player.getUUID());
        if (placed <= 0) return false;
        if (!creative) consume(player, placed);
        level.playSound(null, start, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    private static boolean isRope(ItemStack s) {
        return s.is(ModItems.ROPE.get());
    }

    private static int count(Player player) {
        if (!USE_WHOLE_INVENTORY) {
            ItemStack main = player.getMainHandItem();
            return isRope(main) ? main.getCount() : (isRope(player.getOffhandItem()) ? player.getOffhandItem().getCount() : 0);
        }
        Inventory inv = player.getInventory();
        int n = 0;
        for (ItemStack s : inv.items) if (isRope(s)) n += s.getCount();
        for (ItemStack s : inv.offhand) if (isRope(s)) n += s.getCount();
        return n;
    }

    private static void consume(Player player, int amount) {
        int left = amount;
        if (!USE_WHOLE_INVENTORY) {
            ItemStack h = isRope(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
            h.shrink(left);
            return;
        }
        // primeiro o que esta na mao, depois o resto do inventario
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (left > 0 && isRope(s)) { int t = Math.min(left, s.getCount()); s.shrink(t); left -= t; }
        }
        for (ItemStack s : player.getInventory().items) {
            if (left > 0 && isRope(s)) { int t = Math.min(left, s.getCount()); s.shrink(t); left -= t; }
        }
    }
}

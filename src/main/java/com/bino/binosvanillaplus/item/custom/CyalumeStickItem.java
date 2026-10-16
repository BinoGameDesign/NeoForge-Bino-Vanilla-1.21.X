package com.bino.binosvanillaplus.item.custom;

import com.bino.binosvanillaplus.entity.CyalumeStickEntity;
import com.bino.binosvanillaplus.network.CyalumeStatePayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class CyalumeStickItem extends Item {

    // Cor azul-esverdeada (parecida com a tinta de lula brilhante), usada nas partículas de "poeira"
    public static final DustParticleOptions GLOW_DUST =
            new DustParticleOptions(new Vector3f(0.15f, 0.95f, 0.75f), 1.2f);

    public CyalumeStickItem(Item.Properties properties) {
        super(properties);
    }

    // Roda quando o jogador clica com o botão direito segurando o item
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Só o servidor cria a entidade (o cliente só recebe o resultado)
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CyalumeStickEntity stick = new CyalumeStickEntity(level, player);

            // Se não está no criativo, o item é consumido; se cair na água, ele volta como drop
            stick.setDropsOnLiquid(!player.hasInfiniteMaterials());

            // Sai da mão com um leve impulso para frente (0.5 = força do impulso)
            stick.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 0.5f, 0.0f);
            level.addFreshEntity(stick);

            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }

            // Avisa o cliente para começar o cronômetro no centro da tela
            PacketDistributor.sendToPlayer(serverPlayer,
                    new CyalumeStatePayload(CyalumeStatePayload.START, 0.0f));
        }

        // Pequena espera para não soltar vários bastões no mesmo instante
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // Efeito de brilho ao golpear algo: partículas azul-esverdeadas no ponto do golpe
    public static void glowBurst(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.GLOW_SQUID_INK, x, y, z, 10, 0.15, 0.15, 0.15, 0.05);
        level.sendParticles(GLOW_DUST, x, y, z, 8, 0.2, 0.2, 0.2, 0.0);
    }
}
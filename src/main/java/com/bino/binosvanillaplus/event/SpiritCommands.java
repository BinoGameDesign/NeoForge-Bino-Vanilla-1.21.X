package com.bino.binosvanillaplus.event;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.block.entity.GraveBlockEntity;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Comando usado pelos botoes clicaveis do aviso do espirito: /binosspirit wait|dissipate <id>
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public class SpiritCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("binosspirit")
                .then(Commands.literal("wait")
                        .then(Commands.argument("id", UuidArgument.uuid()).executes(ctx -> run(ctx, false))))
                .then(Commands.literal("dissipate")
                        .then(Commands.argument("id", UuidArgument.uuid()).executes(ctx -> run(ctx, true)))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, boolean dissipate) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID id = UuidArgument.getUuid(ctx, "id");

        GraveBlockEntity spirit = null;
        net.minecraft.core.GlobalPos where = GraveBlockEntity.REGISTRY.get(id);
        if (where != null) {
            ServerLevel level = ctx.getSource().getServer().getLevel(where.dimension());
            if (level != null && level.getBlockEntity(where.pos()) instanceof GraveBlockEntity g) spirit = g;
        }
        if (spirit == null) {
            player.sendSystemMessage(Component.translatable("message.binosvanillaplus.spirit.not_found"));
            return 0;
        }
        if (!spirit.canAccess(player)) {
            player.sendSystemMessage(Component.translatable("message.binosvanillaplus.spirit.not_yours"));
            return 0;
        }
        if (dissipate) {
            spirit.dissipate();
            player.sendSystemMessage(Component.translatable("message.binosvanillaplus.spirit.dissipated"));
        } else {
            spirit.keepWaiting();
            player.sendSystemMessage(Component.translatable("message.binosvanillaplus.spirit.waiting"));
        }
        return 1;
    }
}

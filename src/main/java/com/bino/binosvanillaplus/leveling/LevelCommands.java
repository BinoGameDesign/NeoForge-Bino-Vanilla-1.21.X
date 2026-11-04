package com.bino.binosvanillaplus.leveling;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// /binoslevel here  -> nivel do lugar onde voce esta
// /binoslevel set <mobs> <nivel>  -> muda o nivel de mobs (teste de balanceamento)
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class LevelCommands {
    private LevelCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("binoslevel").requires(s -> s.hasPermission(2))
                .then(Commands.literal("here").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    BlockPos pos = BlockPos.containing(src.getPosition());
                    double surf = RegionDifficulty.surfaceLevel(src.getLevel(), pos.getX(), pos.getZ());
                    double here = RegionDifficulty.levelAt(src.getLevel(), pos);
                    if (here < 0) { src.sendSuccess(() -> Component.literal("Sem níveis nesta dimensão."), false); return 1; }
                    int band = RegionDifficulty.band(surf);
                    src.sendSuccess(() -> Component.literal(String.format("Nível aqui: %.1f (superfície %.1f, faixa %d-%d)",
                            here, surf, band * 5 - 4, band * 5)), false);
                    return 1;
                }))
                .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                            int lv = IntegerArgumentType.getInteger(ctx, "level"), n = 0;
                            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                                if (e instanceof Mob m && MobLevels.eligible(m)) { MobLevels.set(m, lv); n++; }
                            }
                            int count = n;
                            ctx.getSource().sendSuccess(() -> Component.literal(count + " mob(s) agora no nível " + lv), true);
                            return count;
                        })))));
    }
}

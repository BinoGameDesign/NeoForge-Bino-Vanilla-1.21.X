package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.brigadier.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Medidor de desempenho do mod: quanto tempo cada parte gasta (no processador).
// No jogo: /binosperf  -> mostra o resultado desde a ultima vez e zera a contagem.
//   "ms/s" = milissegundos gastos por segundo (1000 ms/s = o jogo inteiro); "pico" = a chamada mais lenta (causa engasgos)
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class Perf {
    private Perf() {}

    private static final class Stat { long total, max, count; }

    private static final Map<String, Stat> STATS = new LinkedHashMap<>();
    private static long since = System.nanoTime();

    public static long start() { return System.nanoTime(); }

    public static void end(String name, long t0) {
        long d = System.nanoTime() - t0;
        Stat s = STATS.computeIfAbsent(name, k -> new Stat());
        s.total += d;
        s.count++;
        if (d > s.max) s.max = d;
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("binosperf").executes(ctx -> { report(); return Command.SINGLE_SUCCESS; }));
    }

    private static void report() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double secs = Math.max(0.001, (System.nanoTime() - since) / 1e9);
        List<Map.Entry<String, Stat>> list = new ArrayList<>(STATS.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue().total, a.getValue().total));
        mc.player.displayClientMessage(Component.literal(String.format("Bino's Vanilla+: desempenho nos ultimos %.0f s", secs))
                .withStyle(ChatFormatting.AQUA), false);
        double sum = 0;
        for (Map.Entry<String, Stat> e : list) {
            Stat s = e.getValue();
            double perSec = s.total / 1e6 / secs, peak = s.max / 1e6;
            sum += perSec;
            ChatFormatting c = perSec > 50 || peak > 16 ? ChatFormatting.RED : perSec > 15 || peak > 6 ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
            mc.player.displayClientMessage(Component.literal(String.format("  %s: %.1f ms/s, pico %.1f ms", e.getKey(), perSec, peak))
                    .withStyle(c), false);
        }
        mc.player.displayClientMessage(Component.literal(String.format("  Total do mod: %.1f ms/s (%.1f%% do tempo)", sum, sum / 10.0))
                .withStyle(ChatFormatting.WHITE), false);
        STATS.clear();
        since = System.nanoTime();
    }
}

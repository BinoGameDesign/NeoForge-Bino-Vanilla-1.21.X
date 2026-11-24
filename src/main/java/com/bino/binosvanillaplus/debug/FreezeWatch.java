package com.bino.binosvanillaplus.debug;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Vigia de congelamento: se o mundo (thread do servidor) ficar mais de 8 s sem terminar um tick, escreve no log
// (run/logs/latest.log) ONDE ele esta preso, procurando por "CONGELOU". Repete a cada 15 s enquanto continuar preso.
// So le; nao muda nada no jogo.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID)
public final class FreezeWatch {
    private FreezeWatch() {}

    private static volatile long lastTick;
    private static volatile Thread serverThread;
    private static volatile net.minecraft.server.MinecraftServer server;
    private static Thread watcher;

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        server = e.getServer();
        serverThread = e.getServer().getRunningThread();
        lastTick = System.currentTimeMillis();
        if (watcher != null) return;
        watcher = new Thread(FreezeWatch::run, "Bino FreezeWatch");
        watcher.setDaemon(true);
        watcher.start();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { serverThread = null; server = null; }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) { beat(); }

    // o servidor terminou uma volta (no mundo de um jogador tambem quando pausado: IntegratedServerHeartbeatMixin)
    public static void beat() { lastTick = System.currentTimeMillis(); }

    private static void run() {
        long reported = 0;
        while (true) {
            try { Thread.sleep(2000); } catch (InterruptedException ex) { return; }
            Thread t = serverThread;
            if (t == null) continue;
            long now = System.currentTimeMillis(), stuck = now - lastTick;
            if (stuck < 8000) { reported = 0; continue; }
            if (reported != 0 && now - reported < 15000) continue;
            reported = now;
            StringBuilder sb = new StringBuilder("CONGELOU: o mundo esta parado ha ").append(stuck / 1000).append(" s. Onde a thread do servidor esta:\n");
            for (StackTraceElement el : t.getStackTrace()) sb.append("    at ").append(el).append('\n');
            BinosVanillaMod.LOGGER.error(sb.toString());
        }
    }
}

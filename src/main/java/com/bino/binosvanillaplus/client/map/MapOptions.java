package com.bino.binosvanillaplus.client.map;

import com.google.gson.Gson;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

// Opcoes do mapa (valem para todos os mundos): binos_map/options.json
public class MapOptions {
    public static boolean minimapPins = true;      // marcacoes no minimapa
    public static boolean worldPins = true;        // marcacoes no mapa-mundi
    public static boolean worldLabels = true;      // nomes ao lado das marcacoes no mapa-mundi
    public static boolean minimapLabels = false;   // letra/nome nas marcacoes do minimapa
    public static boolean chunkGrid = false;       // grade de chunks no mapa-mundi
    public static boolean deathPoint = true;       // cria marcacao automatica ao morrer
    public static boolean showDistance = true;     // distancia no nome das marcacoes

    private static boolean loaded = false;

    private static class Data {
        boolean minimapPins = true, worldPins = true, worldLabels = true, minimapLabels = false,
                chunkGrid = false, deathPoint = true, showDistance = true;
    }

    private static Path file() { return FMLPaths.GAMEDIR.get().resolve("binos_map").resolve("options.json"); }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Data d = new Gson().fromJson(r, Data.class);
            if (d == null) return;
            minimapPins = d.minimapPins; worldPins = d.worldPins; worldLabels = d.worldLabels; minimapLabels = d.minimapLabels;
            chunkGrid = d.chunkGrid; deathPoint = d.deathPoint; showDistance = d.showDistance;
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        Data d = new Data();
        d.minimapPins = minimapPins; d.worldPins = worldPins; d.worldLabels = worldLabels; d.minimapLabels = minimapLabels;
        d.chunkGrid = chunkGrid; d.deathPoint = deathPoint; d.showDistance = showDistance;
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) { new Gson().toJson(d, w); }
        } catch (Exception ignored) {
        }
    }
}

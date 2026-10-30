package com.bino.binosvanillaplus.client.map;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Marcacoes do mapa-mundi, salvas por mundo e por dimensao (binos_map/<mundo>/<dimensao>_waypoints.json)
public class MapWaypoints {
    public static class Waypoint {
        public String name;
        public int x, y, z;
        public int color;   // ARGB
    }

    private static final int[] PALETTE = {0xFFE04040, 0xFF40C0E0, 0xFF50D060, 0xFFF0D030, 0xFFC060E0, 0xFFF08030, 0xFFFFFFFF};
    private static final List<Waypoint> LIST = new ArrayList<>();
    private static Path file;
    private static int counter = 0;

    static void load(Path dir, String dim) {
        LIST.clear();
        file = dir.resolve(dim + "_waypoints.json");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Waypoint[] arr = new Gson().fromJson(r, Waypoint[].class);
            if (arr != null) for (Waypoint w : arr) LIST.add(w);
        } catch (Exception ignored) {
        }
        counter = LIST.size();
    }

    static void clear() {
        LIST.clear();
        file = null;
    }

    public static List<Waypoint> list() { return LIST; }

    public static void add(int x, int y, int z) {
        Waypoint w = new Waypoint();
        w.name = "Marcador " + (++counter);
        w.x = x; w.y = y; w.z = z;
        w.color = PALETTE[counter % PALETTE.length];
        LIST.add(w);
        save();
    }

    public static void remove(Waypoint w) {
        LIST.remove(w);
        save();
    }

    private static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                new Gson().toJson(LIST.toArray(new Waypoint[0]), w);
            }
        } catch (Exception ignored) {
        }
    }
}

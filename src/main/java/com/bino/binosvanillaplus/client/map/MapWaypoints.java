package com.bino.binosvanillaplus.client.map;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Marcacoes do mapa, salvas por mundo e por dimensao (binos_map/<mundo>/<dimensao>_waypoints.json)
public class MapWaypoints {
    public static final String DEFAULT_SET = "Padrão";
    public static final String DEATH_SET = "Mortes";

    public static class Waypoint {
        public String name;
        public int x, y, z;
        public int color;          // ARGB
        public String symbol;      // id do icone (casa, mina, perigo...), ver WaypointIcons
        public String set;         // grupo (ex.: "Casa", "Minas")
        public boolean hidden;     // oculto = nao aparece em nenhum mapa

        public String icon() { return WaypointIcons.valid(symbol); }

        public String setName() { return set == null || set.isEmpty() ? DEFAULT_SET : set; }
    }

    public static final int[] PALETTE = {
            0xFFE04040, 0xFFF08030, 0xFFFEC400, 0xFF80D040, 0xFF40C060, 0xFF40C0C0, 0xFF40A0F0, 0xFF4060E0,
            0xFF9060E0, 0xFFD060D0, 0xFFF080A0, 0xFFFFFFFF, 0xFFB0B0B0, 0xFF707070, 0xFF303030, 0xFF8B5A2B};

    private static final List<Waypoint> LIST = new ArrayList<>();
    private static Path file;
    private static int counter = 0;

    static void load(Path dir, String dim) {
        LIST.clear();
        file = dir.resolve(dim + "_waypoints.json");
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                Waypoint[] arr = new Gson().fromJson(r, Waypoint[].class);
                if (arr != null) for (Waypoint w : arr) LIST.add(w);
            } catch (Exception ignored) {
            }
        }
        counter = LIST.size();
    }

    static void clear() {
        LIST.clear();
        file = null;
    }

    public static List<Waypoint> list() { return LIST; }

    public static Set<String> sets() {
        Set<String> s = new LinkedHashSet<>();
        s.add(DEFAULT_SET);
        for (Waypoint w : LIST) s.add(w.setName());
        return s;
    }

    public static Waypoint create(int x, int y, int z) {
        Waypoint w = new Waypoint();
        w.name = "Marcador " + (++counter);
        w.x = x; w.y = y; w.z = z;
        w.color = PALETTE[counter % PALETTE.length];
        w.set = DEFAULT_SET;
        return w;
    }

    public static void add(Waypoint w) {
        if (!LIST.contains(w)) LIST.add(w);
        save();
    }

    public static void add(int x, int y, int z) { add(create(x, y, z)); }

    public static void remove(Waypoint w) {
        LIST.remove(w);
        save();
    }

    // ponto de morte: so existe um, o mais recente
    public static void setDeathPoint(int x, int y, int z) {
        LIST.removeIf(w -> DEATH_SET.equals(w.set));
        Waypoint w = new Waypoint();
        w.name = "Ponto de morte";
        w.x = x; w.y = y; w.z = z;
        w.color = PALETTE[0];
        w.symbol = "skull";
        w.set = DEATH_SET;
        LIST.add(w);
        save();
    }

    public static void save() {
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

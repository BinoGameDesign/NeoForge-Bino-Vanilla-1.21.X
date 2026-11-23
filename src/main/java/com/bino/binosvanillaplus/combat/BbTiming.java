package com.bino.binosvanillaplus.combat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Tempos das animacoes do Blockbench (duracao, acertos e quando o proximo golpe pode entrar), lidos do mesmo JSON que o
// cliente toca (assets/binosvanillaplus/combat_anims/<arma>.json, dentro do proprio mod; vale no servidor tambem).
// Assim o momento do acerto vem do MARCADOR da linha do tempo do Blockbench, sem numero escrito a mao no codigo.
public final class BbTiming {
    private BbTiming() {}

    // nome da animacao no Blockbench para cada golpe
    public static String name(CombatMove m) {
        return switch (m) {
            case LIGHT_1 -> "base_1"; case LIGHT_2 -> "base_2"; case LIGHT_3 -> "base_3"; case LIGHT_4 -> "base_4"; case LIGHT_5 -> "base_5";
            case SURPRISE -> "surpresa"; case GUARD_BREAK -> "quebra_guarda"; case JUDGMENT -> "juizo"; case TORMENTA -> "tormenta";
            case LAUNCHER -> "lanca_ceu"; case AIR_1 -> "aereo_1"; case AIR_2 -> "aereo_2"; case AIR_3 -> "aereo_3"; case METEOR -> "meteoro";
            case FORWARD -> "investida"; case DASH -> "avanco"; case DASH_SLASH -> "avanco_corte"; case PIERCE -> "perfurador";
            case ROLL_SLASH -> "rolamento_corte"; case ROLL_THRUST -> "rolamento_estocada"; case PARRY -> "aparar"; case ISSEN -> "issen";
            case RUSH -> "rajada"; case CRITICAL -> "critico"; case BACKSTAB -> "pelas_costas";
            default -> null;
        };
    }

    // length (s), hit = primeiro acerto (s), chain = a partir de quando o proximo golpe pode sair (s)
    public record Times(float length, float hit, float chain) {}

    private static final Map<String, Map<String, Times>> CACHE = new HashMap<>();

    public static Times get(CombatMove m, boolean axe) {
        String n = name(m);
        return n == null ? null : load(axe ? "axe" : "sword").get(n);
    }

    private static synchronized Map<String, Times> load(String weapon) {
        Map<String, Times> got = CACHE.get(weapon);
        if (got != null) return got;
        Map<String, Times> out = new HashMap<>();
        String path = "/assets/" + BinosVanillaMod.MOD_ID + "/combat_anims/" + weapon + ".json";
        try (InputStream in = BbTiming.class.getResourceAsStream(path)) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> en : root.entrySet()) {
                    JsonObject o = en.getValue().getAsJsonObject();
                    float len = o.get("length").getAsFloat();
                    var hits = o.getAsJsonArray("hits");
                    float hit = hits.size() > 0 ? hits.get(0).getAsFloat() : len * 0.35f;
                    float last = hits.size() > 0 ? hits.get(hits.size() - 1).getAsFloat() : hit;
                    float chain = o.has("chain") ? o.get("chain").getAsFloat() : Math.min(len, last + 0.10f);
                    out.put(en.getKey(), new Times(len, hit, chain));
                }
            }
        } catch (Exception ex) {
            BinosVanillaMod.LOGGER.error("Tempos das animacoes do Blockbench ({}) com erro: {}", path, ex.toString());
        }
        CACHE.put(weapon, out);
        return out;
    }
}

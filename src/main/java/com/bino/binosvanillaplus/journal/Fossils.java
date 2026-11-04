package com.bino.binosvanillaplus.journal;

import java.util.List;

// As tres especies de fosseis gigantes e as partes de cada uma (cada parte fotografada vira uma descoberta;
// todas as partes de uma especie = esqueleto completo no diario)
public final class Fossils {
    private Fossils() {}

    public static final List<String> SPECIES = List.of("beast", "serpent", "leviathan");

    public static List<String> parts(int species) {
        return species == 1 ? List.of("skull", "vertebra", "rib") : List.of("skull", "vertebra", "rib", "limb");
    }

    public static String speciesKey(int species) {
        return "discovery.binosvanillaplus.species." + SPECIES.get(Math.floorMod(species, SPECIES.size()));
    }

    public static String partKey(int species, String part) {
        return "discovery.binosvanillaplus.fossil." + SPECIES.get(Math.floorMod(species, SPECIES.size())) + "." + part;
    }
}

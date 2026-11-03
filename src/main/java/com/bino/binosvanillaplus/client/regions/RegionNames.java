package com.bino.binosvanillaplus.client.regions;

import java.util.Random;
import java.util.Set;

// Nomes de regiao estilo World of Warcraft, em portugues: "Planice do Luar", "Montanhar de Forjanegra".
// Sorteados com a semente da regiao (o mesmo mundo sempre da os mesmos nomes) e nunca repetidos no mesmo mundo.
final class RegionNames {
    private RegionNames() {}

    // [palavras-base]; o primeiro caractere de cada grupo diz o genero: m/f (define "do/da")
    private static final String[][] KINDS = {
            {"plains", "Planície", "Campina", "Pradaria", "Savana"},
            {"meadow", "Campina", "Prado", "Várzea"},
            {"flower", "Jardim", "Campina", "Vale"},
            {"sunflower", "Campina", "Vale", "Pradaria"},
            {"savanna", "Savana", "Pradaria", "Cerrado"},
            {"forest", "Floresta", "Bosque", "Mata", "Arvoredo"},
            {"birch", "Bosque", "Mata", "Arvoredo"},
            {"grove", "Bosque", "Pinhal", "Alameda"},
            {"taiga", "Taiga", "Pinhal", "Mata Gelada"},
            {"jungle", "Selva", "Matagal", "Trópico"},
            {"bamboo", "Bambuzal", "Selva", "Matagal"},
            {"swamp", "Pântano", "Brejo", "Charneca"},
            {"mangrove", "Mangue", "Brejo", "Pântano"},
            {"desert", "Deserto", "Areal", "Ermo"},
            {"badlands", "Planalto", "Mesa", "Ermo"},
            {"mountain", "Montanhar", "Serra", "Cordilheira", "Cume"},
            {"peaks", "Cordilheira", "Cume", "Pico"},
            {"hills", "Colinas", "Outeiros", "Morros"},
            {"windswept", "Colinas", "Serra", "Penhasco"},
            {"snowy", "Tundra", "Geleira", "Gelo"},
            {"ice", "Geleira", "Tundra", "Gelo"},
            {"frozen", "Geleira", "Tundra", "Mar Gelado"},
            {"cherry", "Cerejal", "Bosque", "Jardim"},
            {"beach", "Praia", "Costa", "Enseada"},
            {"shore", "Costa", "Praia", "Enseada"},
            {"ocean", "Mar", "Oceano", "Golfo"},
            {"river", "Rio", "Corredeira", "Caudal"},
            {"cave", "Gruta", "Caverna", "Abismo"},
            {"lush", "Gruta", "Jardim Oculto", "Caverna"},
            {"dripstone", "Gruta", "Caverna", "Salão"},
            {"deep_dark", "Abismo", "Breu", "Silêncio"},
            {"nether", "Ermo", "Chamas", "Brasa"},
            {"basalt", "Ermo", "Cinzas", "Brasa"},
            {"soul", "Vale", "Ermo", "Limbo"},
            {"crimson", "Floresta", "Bosque", "Ermo"},
            {"warped", "Floresta", "Bosque", "Ermo"},
            {"end", "Vazio", "Ermo", "Ilhas"},
    };
    private static final String[] FALLBACK = {"Terras", "Ermo", "Vale", "Reino"};

    // epiteto com o artigo ja pronto: "do Luar", "da Aurora", "das Brumas", "de Forjanegra"
    private static final String[] EPITHETS = {
            "do Luar", "da Aurora", "do Crepúsculo", "das Brumas", "dos Sussurros", "dos Corvos", "do Lobo Cinzento",
            "da Lua Cinzenta", "do Rei Esquecido", "da Rainha Pálida", "dos Antigos", "das Sombras", "da Tempestade",
            "do Trovão", "do Dragão Adormecido", "da Estrela Cadente", "do Eco", "das Cinzas", "do Orvalho",
            "dos Mil Ventos", "da Última Luz", "dos Reis Caídos", "do Velho Carvalho", "da Fênix", "do Silêncio",
            "do Pôr do Sol", "das Lágrimas", "da Geada Eterna", "do Guardião", "das Almas Perdidas", "do Falcão",
            "da Serpente", "dos Gigantes", "do Ferro Velho", "da Bruxa", "do Caçador"
    };
    private static final String[] PREFIX = {"Forja", "Sombra", "Lume", "Aurora", "Neve", "Espinho", "Corvo", "Bruma", "Ferro",
            "Alva", "Pedra", "Vento", "Brasa", "Gelo", "Trovão", "Musgo", "Lua", "Estrela", "Cinza", "Ouro", "Cristal", "Lobo"};
    private static final String[] SUFFIX = {"negra", "dourada", "vil", "alta", "cinza", "vento", "lume", "fria", "branca",
            "funda", "rubra", "velha", "serena", "ardente", "perdida", "sombria", "áspera", "eterna"};

    static String generate(String biomeKey, Random rnd, Set<String> used) {
        String path = biomeKey.contains(":") ? biomeKey.substring(biomeKey.indexOf(':') + 1) : biomeKey;
        String[] words = FALLBACK;
        for (String[] k : KINDS) {
            if (path.contains(k[0])) {
                words = new String[k.length - 1];
                System.arraycopy(k, 1, words, 0, words.length);
                break;
            }
        }
        String name = "";
        for (int tries = 0; tries < 24; tries++) {
            String base = words[rnd.nextInt(words.length)];
            String epi;
            if (rnd.nextInt(3) == 0) {   // nome composto inventado: "de Forjanegra"
                epi = "de " + PREFIX[rnd.nextInt(PREFIX.length)] + SUFFIX[rnd.nextInt(SUFFIX.length)];
            } else {
                epi = EPITHETS[rnd.nextInt(EPITHETS.length)];
            }
            name = base + " " + epi;
            if (!used.contains(name)) break;
        }
        return name;
    }
}

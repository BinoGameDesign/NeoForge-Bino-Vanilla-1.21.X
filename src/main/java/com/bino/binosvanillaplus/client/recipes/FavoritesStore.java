package com.bino.binosvanillaplus.client.recipes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Itens favoritos do jogador, guardados em config/binosvanillaplus_favoritos.json (vale para todos os mundos)
public class FavoritesStore {
    private static final Set<ResourceLocation> FAVS = new LinkedHashSet<>();
    private static boolean loaded = false;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("binosvanillaplus_favoritos.json");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            JsonArray arr = new Gson().fromJson(r, JsonArray.class);
            if (arr != null) for (JsonElement e : arr) {
                ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                if (id != null) FAVS.add(id);
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        JsonArray arr = new JsonArray();
        for (ResourceLocation id : FAVS) arr.add(id.toString());
        try (Writer w = Files.newBufferedWriter(file())) {
            new Gson().toJson(arr, w);
        } catch (IOException ignored) {
        }
    }

    public static boolean isFavorite(Item item) {
        load();
        return FAVS.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static void toggle(Item item) {
        load();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!FAVS.remove(id)) FAVS.add(id);
        save();
    }

    public static List<Item> items() {
        load();
        List<Item> list = new ArrayList<>();
        for (ResourceLocation id : FAVS) {
            Item it = BuiltInRegistries.ITEM.get(id);
            if (it != Items.AIR) list.add(it);
        }
        return list;
    }
}

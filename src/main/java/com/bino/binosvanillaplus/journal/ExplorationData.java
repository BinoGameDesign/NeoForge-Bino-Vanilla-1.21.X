package com.bino.binosvanillaplus.journal;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

// Diario de exploracao de cada jogador (salvo no mundo): descobertas fotografadas e fotos livres (decoracao)
public class ExplorationData extends SavedData {
    private static final String ID = "binosvanillaplus_journal";
    public static final int MAX_PHOTOS = 60;

    public record Discovery(String key, String name, String desc, String icon, int x, int y, int z, long day) {}
    public record Photo(byte[] png, long day, int x, int y, int z) {}

    public static final class Journal {
        public final List<Discovery> discoveries = new ArrayList<>();
        public final List<Photo> photos = new ArrayList<>();

        public boolean has(String key) {
            for (Discovery d : discoveries) if (d.key().equals(key)) return true;
            return false;
        }
    }

    private final Map<UUID, Journal> journals = new HashMap<>();

    public static ExplorationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ExplorationData::new, ExplorationData::load, DataFixTypes.LEVEL), ID);
    }

    public Journal journal(UUID player) { return journals.computeIfAbsent(player, k -> new Journal()); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag all = new ListTag();
        for (Map.Entry<UUID, Journal> e : journals.entrySet()) {
            CompoundTag j = new CompoundTag();
            j.putUUID("Player", e.getKey());
            ListTag ds = new ListTag();
            for (Discovery d : e.getValue().discoveries) {
                CompoundTag t = new CompoundTag();
                t.putString("Key", d.key()); t.putString("Name", d.name()); t.putString("Desc", d.desc()); t.putString("Icon", d.icon());
                t.putInt("X", d.x()); t.putInt("Y", d.y()); t.putInt("Z", d.z()); t.putLong("Day", d.day());
                ds.add(t);
            }
            j.put("Discoveries", ds);
            ListTag ps = new ListTag();
            for (Photo p : e.getValue().photos) {
                CompoundTag t = new CompoundTag();
                t.putByteArray("Png", p.png()); t.putLong("Day", p.day());
                t.putInt("X", p.x()); t.putInt("Y", p.y()); t.putInt("Z", p.z());
                ps.add(t);
            }
            j.put("Photos", ps);
            all.add(j);
        }
        tag.put("Journals", all);
        return tag;
    }

    private static ExplorationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ExplorationData data = new ExplorationData();
        for (Tag raw : tag.getList("Journals", Tag.TAG_COMPOUND)) {
            CompoundTag j = (CompoundTag) raw;
            if (!j.hasUUID("Player")) continue;
            Journal jr = data.journal(j.getUUID("Player"));
            for (Tag t0 : j.getList("Discoveries", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) t0;
                jr.discoveries.add(new Discovery(t.getString("Key"), t.getString("Name"), t.getString("Desc"), t.getString("Icon"),
                        t.getInt("X"), t.getInt("Y"), t.getInt("Z"), t.getLong("Day")));
            }
            for (Tag t0 : j.getList("Photos", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) t0;
                jr.photos.add(new Photo(t.getByteArray("Png"), t.getLong("Day"), t.getInt("X"), t.getInt("Y"), t.getInt("Z")));
            }
        }
        return data;
    }
}

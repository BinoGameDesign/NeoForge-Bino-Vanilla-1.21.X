package com.bino.binosvanillaplus.campfire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

// Fogueiras registradas por cada jogador como ponto de retorno (ate 3), salvas no mundo
public class CampfireData extends SavedData {
    private static final String ID = "binosvanillaplus_campfires";
    public static final int MAX = 3;

    private final Map<UUID, List<GlobalPos>> points = new HashMap<>();

    public static CampfireData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CampfireData::new, CampfireData::load, DataFixTypes.LEVEL), ID);
    }

    public List<GlobalPos> of(UUID player) { return points.computeIfAbsent(player, k -> new ArrayList<>()); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag all = new ListTag();
        for (Map.Entry<UUID, List<GlobalPos>> e : points.entrySet()) {
            CompoundTag p = new CompoundTag();
            p.putUUID("Player", e.getKey());
            ListTag l = new ListTag();
            for (GlobalPos g : e.getValue()) {
                CompoundTag t = new CompoundTag();
                t.putString("Dim", g.dimension().location().toString());
                t.putLong("Pos", g.pos().asLong());
                l.add(t);
            }
            p.put("Points", l);
            all.add(p);
        }
        tag.put("Players", all);
        return tag;
    }

    private static CampfireData load(CompoundTag tag, HolderLookup.Provider registries) {
        CampfireData d = new CampfireData();
        for (Tag raw : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag p = (CompoundTag) raw;
            if (!p.hasUUID("Player")) continue;
            List<GlobalPos> l = d.of(p.getUUID("Player"));
            for (Tag t0 : p.getList("Points", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) t0;
                ResourceLocation dim = ResourceLocation.tryParse(t.getString("Dim"));
                if (dim != null) l.add(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLong("Pos"))));
            }
        }
        return d;
    }
}

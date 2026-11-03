package com.bino.binosvanillaplus.pocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Dados salvos do mundo (sobrevivem a reinicios do servidor):
//  - os lotes: dono, posicao na grade, nivel (tamanho), onde esta a porta de fora;
//  - os "retornos": para onde cada jogador que esta DENTRO da dimensao volta ao sair.
public class PocketData extends SavedData {
    private static final String ID = "binosvanillaplus_pocket";

    public static final class Plot {
        public final UUID owner;
        public final int index;
        public int level = 0;
        public int builtLevel = -1;                  // nivel com que a sala foi construida (-1 = ainda nao)
        @Nullable public ResourceKey<Level> doorDim; // porta de fora (null = sem porta: ninguem entra)
        @Nullable public BlockPos doorPos;           // metade de baixo da porta

        Plot(UUID owner, int index) { this.owner = owner; this.index = index; }
    }

    public record Return(ResourceKey<Level> dim, double x, double y, double z, float yaw) {}

    private final Map<UUID, Plot> plots = new HashMap<>();
    private final Map<UUID, Return> returns = new HashMap<>();
    private int nextIndex = 0;

    public static PocketData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PocketData::new, PocketData::load, DataFixTypes.LEVEL), ID);
    }

    // ---------------------------------------------------------------- lotes

    @Nullable
    public Plot plotOf(UUID owner) { return plots.get(owner); }

    public Plot plotOrCreate(UUID owner) {
        Plot p = plots.get(owner);
        if (p == null) {
            p = new Plot(owner, nextIndex++);
            plots.put(owner, p);
            setDirty();
        }
        return p;
    }

    @Nullable
    public Plot plotByIndex(int index) {
        for (Plot p : plots.values()) if (p.index == index) return p;
        return null;
    }

    @Nullable
    public Plot plotByDoor(ResourceKey<Level> dim, BlockPos lower) {
        for (Plot p : plots.values()) {
            if (p.doorPos != null && p.doorPos.equals(lower) && dim.equals(p.doorDim)) return p;
        }
        return null;
    }

    // ---------------------------------------------------------------- retornos

    @Nullable
    public Return getReturn(UUID player) { return returns.get(player); }

    public void setReturn(UUID player, Return r) { returns.put(player, r); setDirty(); }

    @Nullable
    public Return removeReturn(UUID player) {
        Return r = returns.remove(player);
        if (r != null) setDirty();
        return r;
    }

    // ---------------------------------------------------------------- salvar / carregar

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next", nextIndex);
        ListTag pl = new ListTag();
        for (Plot p : plots.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("owner", p.owner);
            t.putInt("index", p.index);
            t.putInt("level", p.level);
            t.putInt("built", p.builtLevel);
            if (p.doorDim != null && p.doorPos != null) {
                t.putString("doorDim", p.doorDim.location().toString());
                t.putLong("doorPos", p.doorPos.asLong());
            }
            pl.add(t);
        }
        tag.put("plots", pl);
        ListTag rl = new ListTag();
        for (Map.Entry<UUID, Return> e : returns.entrySet()) {
            CompoundTag t = new CompoundTag();
            Return r = e.getValue();
            t.putUUID("player", e.getKey());
            t.putString("dim", r.dim().location().toString());
            t.putDouble("x", r.x());
            t.putDouble("y", r.y());
            t.putDouble("z", r.z());
            t.putFloat("yaw", r.yaw());
            rl.add(t);
        }
        tag.put("returns", rl);
        return tag;
    }

    public static PocketData load(CompoundTag tag, HolderLookup.Provider registries) {
        PocketData d = new PocketData();
        d.nextIndex = tag.getInt("next");
        for (Tag raw : tag.getList("plots", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            Plot p = new Plot(t.getUUID("owner"), t.getInt("index"));
            p.level = t.getInt("level");
            p.builtLevel = t.contains("built") ? t.getInt("built") : -1;
            if (t.contains("doorDim") && t.contains("doorPos")) {
                p.doorDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("doorDim")));
                p.doorPos = BlockPos.of(t.getLong("doorPos"));
            }
            d.plots.put(p.owner, p);
            d.nextIndex = Math.max(d.nextIndex, p.index + 1);
        }
        for (Tag raw : tag.getList("returns", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            d.returns.put(t.getUUID("player"), new Return(
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim"))),
                    t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw")));
        }
        return d;
    }
}

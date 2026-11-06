package com.bino.binosvanillaplus.faction;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

// Estruturas das faccoes ja "arrumadas" (bandeiras trocadas e guardas colocados), pela posicao do chunk de inicio
public class FactionStructures extends SavedData {
    final LongOpenHashSet done = new LongOpenHashSet();

    public static FactionStructures get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(FactionStructures::new, FactionStructures::load, null),
                "binosvanillaplus_faction_structures");
    }

    private static FactionStructures load(CompoundTag tag, HolderLookup.Provider p) {
        FactionStructures d = new FactionStructures();
        for (long l : tag.getLongArray("done")) d.done.add(l);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.putLongArray("done", done.toLongArray());
        return tag;
    }

    public boolean mark(long key) {
        boolean added = done.add(key);
        if (added) setDirty();
        return added;
    }
}

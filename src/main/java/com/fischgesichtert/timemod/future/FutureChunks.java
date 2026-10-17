package com.fischgesichtert.timemod.future;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Das Gedächtnis einer Zukunfts-Epoche: aus welchem Stand der Gegenwart jeder Chunk abgeleitet wurde,
 * in welchen Chunks Spieler in der Zukunft selbst gebaut haben (die bleiben, wie sie sind), und wo die
 * Nachkommen der alten Tiere schon ausgesetzt wurden.
 */
public class FutureChunks extends SavedData {
    private static final String NAME = "timemod_future_chunks";

    private final Long2LongOpenHashMap derived = new Long2LongOpenHashMap();
    private final LongOpenHashSet claimed = new LongOpenHashSet();
    private final LongOpenHashSet populated = new LongOpenHashSet();

    public static FutureChunks get(ServerLevel future) {
        return future.getDataStorage().computeIfAbsent(FutureChunks::load, FutureChunks::new, NAME);
    }

    /** Muss neu abgeleitet werden: die Gegenwart ist neuer, und hier hat in der Zukunft niemand gebaut. */
    public boolean needs(long chunk, long presentRevision) {
        return !claimed.contains(chunk) && derived.get(chunk) < presentRevision;
    }

    public boolean isDerived(long chunk) {
        return derived.containsKey(chunk);
    }

    public boolean isClaimed(long chunk) {
        return claimed.contains(chunk);
    }

    void derived(long chunk, long presentRevision) {
        derived.put(chunk, presentRevision);
        setDirty();
    }

    /** In der Zukunft gebaut: Dieser Chunk wird nie mehr aus der Gegenwart überschrieben. */
    public void claim(long chunk) {
        if (claimed.add(chunk)) {
            setDirty();
        }
    }

    /** Ob die Nachkommen hier schon leben; markiert es zugleich. */
    boolean populate(long chunk) {
        boolean first = populated.add(chunk);
        if (first) {
            setDirty();
        }
        return first;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] keys = derived.keySet().toLongArray();
        long[] values = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = derived.get(keys[i]);
        }
        tag.putLongArray("Derived", keys);
        tag.putLongArray("Revisions", values);
        tag.putLongArray("Claimed", claimed.toLongArray());
        tag.putLongArray("Populated", populated.toLongArray());
        return tag;
    }

    private static FutureChunks load(CompoundTag tag) {
        FutureChunks chunks = new FutureChunks();
        long[] keys = tag.getLongArray("Derived");
        long[] values = tag.getLongArray("Revisions");
        for (int i = 0; i < Math.min(keys.length, values.length); i++) {
            chunks.derived.put(keys[i], values[i]);
        }
        for (long key : tag.getLongArray("Claimed")) {
            chunks.claimed.add(key);
        }
        for (long key : tag.getLongArray("Populated")) {
            chunks.populated.add(key);
        }
        return chunks;
    }
}

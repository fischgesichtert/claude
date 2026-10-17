package com.fischgesichtert.timemod.era;

import com.fischgesichtert.timemod.safety.Safety;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Die Zeitlinie: Was in der Vergangenheit absichtlich hinterlassen wurde – Zeitkapseln und Zeitsaat –,
 * wartet hier auf seinen Auftritt in der Gegenwart. Es erscheint an derselben Stelle, sobald diese
 * geladen ist (oder sofort, wenn sie es schon ist).
 */
public class Timeline extends SavedData {
    private static final String NAME = "timemod_timeline";

    public enum Kind {
        CAPSULE, TREE
    }

    /** Ein Gruß aus der Vergangenheit: was, wo, wie viele Jahre her, und die Einzelheiten. */
    public record Entry(Kind kind, BlockPos pos, long years, CompoundTag data) {
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Kind", kind.name());
            tag.put("Pos", NbtUtils.writeBlockPos(pos));
            tag.putLong("Years", years);
            tag.put("Data", data);
            return tag;
        }

        static Entry load(CompoundTag tag) {
            return new Entry(Kind.valueOf(tag.getString("Kind")), NbtUtils.readBlockPos(tag.getCompound("Pos")), tag.getLong("Years"),
                tag.getCompound("Data"));
        }
    }

    private final Map<Long, List<Entry>> byChunk = new HashMap<>();
    /** Geladene Chunks, deren Grüße am Ende des Ticks erscheinen (nie mitten im Laden eines Chunks). */
    private final Set<Long> due = new LinkedHashSet<>();

    public static Timeline get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Timeline::load, Timeline::new, NAME);
    }

    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> Safety.run("Zeitlinie: Chunk geladen", () -> {
            if (level.dimension() != Level.OVERWORLD) {
                return;
            }
            Timeline timeline = get(level.getServer());
            long pos = chunk.getPos().toLong();
            if (timeline.byChunk.containsKey(pos)) {
                timeline.due.add(pos);
            }
        }));
        ServerTickEvents.END_SERVER_TICK.register(server -> Safety.run("Zeitlinie", () -> {
            Timeline timeline = get(server);
            if (!timeline.due.isEmpty()) {
                timeline.deliverDue(server.overworld());
            }
        }));
    }

    /** Merkt einen Gruß aus der Vergangenheit vor; ist die Stelle in der Gegenwart geladen, erscheint er noch in diesem Tick. */
    public void add(MinecraftServer server, Entry entry) {
        ChunkPos chunk = new ChunkPos(entry.pos());
        byChunk.computeIfAbsent(chunk.toLong(), key -> new ArrayList<>()).add(entry);
        setDirty();
        if (server.overworld().getChunkSource().getChunkNow(chunk.x, chunk.z) != null) {
            due.add(chunk.toLong());
        }
    }

    /** Ob an dieser Stelle schon etwas Bestimmtes auf die Gegenwart wartet. */
    public boolean has(Kind kind, BlockPos pos) {
        List<Entry> entries = byChunk.get(new ChunkPos(pos).toLong());
        return entries != null && entries.stream().anyMatch(entry -> entry.kind() == kind && entry.pos().equals(pos));
    }

    /**
     * Lässt erscheinen, was in geladenen Chunks wartet. Ein großer Baum kann dabei Nachbarchunks laden;
     * deren Grüße kommen dann im nächsten Tick an die Reihe.
     */
    private void deliverDue(ServerLevel present) {
        List<Long> chunks = new ArrayList<>(due);
        due.clear();
        for (long key : chunks) {
            ChunkPos chunk = new ChunkPos(key);
            if (present.getChunkSource().getChunkNow(chunk.x, chunk.z) == null) {
                continue;
            }
            List<Entry> entries = byChunk.remove(key);
            if (entries == null) {
                continue;
            }
            setDirty();
            for (Entry entry : entries) {
                switch (entry.kind()) {
                    case CAPSULE -> TimeCapsules.deliver(present, entry);
                    case TREE -> AncientTrees.grow(present, entry);
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (List<Entry> entries : byChunk.values()) {
            for (Entry entry : entries) {
                list.add(entry.save());
            }
        }
        tag.put("Entries", list);
        return tag;
    }

    private static Timeline load(CompoundTag tag) {
        Timeline timeline = new Timeline();
        for (Tag element : tag.getList("Entries", Tag.TAG_COMPOUND)) {
            Entry entry = Entry.load((CompoundTag) element);
            timeline.byChunk.computeIfAbsent(new ChunkPos(entry.pos()).toLong(), key -> new ArrayList<>()).add(entry);
        }
        return timeline;
    }
}

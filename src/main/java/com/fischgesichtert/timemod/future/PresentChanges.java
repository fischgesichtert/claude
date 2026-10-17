package com.fischgesichtert.timemod.future;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Was Spieler in der Gegenwart verändert haben: je Chunk der Zeitpunkt der letzten Änderung. Die Zukunft
 * leitet sich aus genau diesen Chunks ab – und rechnet neu, sobald sich hier etwas tut.
 *
 * <p>Dazu eine Zählung der gehaltenen Tiere (eingezäunt, angeleint, mit Namen): In der Zukunft bleiben
 * von ihnen Knochen, ihre Nachkommen leben verwildert in der Nähe.
 */
public class PresentChanges extends SavedData {
    private static final String NAME = "timemod_present_changes";
    private static final int MAX_ANIMALS = 24;

    /** Ein gehaltenes Tier bei der letzten Zählung. */
    public record Animal(ResourceLocation type, BlockPos pos) {
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", type.toString());
            tag.put("Pos", NbtUtils.writeBlockPos(pos));
            return tag;
        }

        static Animal load(CompoundTag tag) {
            return new Animal(new ResourceLocation(tag.getString("Type")), NbtUtils.readBlockPos(tag.getCompound("Pos")));
        }
    }

    private final Long2LongOpenHashMap revisions = new Long2LongOpenHashMap();
    private final Long2ObjectOpenHashMap<List<Animal>> animals = new Long2ObjectOpenHashMap<>();

    public static PresentChanges get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PresentChanges::load, PresentChanges::new, NAME);
    }

    /** Wann sich der Chunk zuletzt geändert hat (Spielzeit); 0 = nie. */
    public long revision(long chunk) {
        return revisions.get(chunk);
    }

    public void mark(ChunkPos chunk, long gameTime) {
        long key = chunk.toLong();
        if (revisions.get(key) < gameTime) {
            revisions.put(key, Math.max(1, gameTime));
            setDirty();
        }
    }

    public List<Animal> animals(long chunk) {
        return animals.getOrDefault(chunk, List.of());
    }

    /** Neue Zählung für einen Chunk: ersetzt die alte. */
    public void count(long chunk, List<Animal> found) {
        List<Animal> previous = animals.get(chunk);
        List<Animal> kept = found.size() > MAX_ANIMALS ? List.copyOf(found.subList(0, MAX_ANIMALS)) : List.copyOf(found);
        if (kept.isEmpty() ? previous != null : !kept.equals(previous)) {
            if (kept.isEmpty()) {
                animals.remove(chunk);
            } else {
                animals.put(chunk, kept);
            }
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag chunks = new ListTag();
        for (Long2LongOpenHashMap.Entry entry : revisions.long2LongEntrySet()) {
            CompoundTag chunk = new CompoundTag();
            chunk.putLong("Chunk", entry.getLongKey());
            chunk.putLong("Revision", entry.getLongValue());
            List<Animal> counted = animals.get(entry.getLongKey());
            if (counted != null) {
                ListTag list = new ListTag();
                counted.forEach(animal -> list.add(animal.save()));
                chunk.put("Animals", list);
            }
            chunks.add(chunk);
        }
        tag.put("Chunks", chunks);
        return tag;
    }

    private static PresentChanges load(CompoundTag tag) {
        PresentChanges changes = new PresentChanges();
        for (Tag element : tag.getList("Chunks", Tag.TAG_COMPOUND)) {
            CompoundTag chunk = (CompoundTag) element;
            long key = chunk.getLong("Chunk");
            changes.revisions.put(key, chunk.getLong("Revision"));
            if (chunk.contains("Animals", Tag.TAG_LIST)) {
                List<Animal> list = new ArrayList<>();
                for (Tag animal : chunk.getList("Animals", Tag.TAG_COMPOUND)) {
                    Animal loaded = Animal.load((CompoundTag) animal);
                    if (BuiltInRegistries.ENTITY_TYPE.containsKey(loaded.type())) {
                        list.add(loaded);
                    }
                }
                changes.animals.put(key, list);
            }
        }
        return changes;
    }
}

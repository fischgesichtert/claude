package com.fischgesichtert.timemod.era;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Wer in der tiefen Vergangenheit spawnen darf: Kühe, Zombies und Dorfbewohner jedenfalls nicht.
 * Wesen anderer Mods bleiben erlaubt; die eigenen Urzeit-Wesen bringen eigene Spawn-Regeln mit.
 */
public final class EraSpawns {
    /** Gefilterte Listen je Ausgangsliste – die Listen der Biome bleiben dieselben Objekte. */
    private static final Map<WeightedRandomList<MobSpawnSettings.SpawnerData>, WeightedRandomList<MobSpawnSettings.SpawnerData>> FILTERED =
        Collections.synchronizedMap(new WeakHashMap<>());

    private EraSpawns() {
    }

    /** Ob in dieser Dimension Vanilla-Wesen von selbst entstehen dürfen. */
    public static boolean allowsVanillaLife(ResourceKey<Level> level) {
        Era era = Eras.byLevel(level);
        return era == null || era.vanillaLife();
    }

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> filter(ResourceKey<Level> level,
                                                                       WeightedRandomList<MobSpawnSettings.SpawnerData> spawns) {
        if (spawns.isEmpty() || allowsVanillaLife(level)) {
            return spawns;
        }
        return FILTERED.computeIfAbsent(spawns, list -> WeightedRandomList.create(list.unwrap().stream()
            .filter(data -> !BuiltInRegistries.ENTITY_TYPE.getKey(data.type).getNamespace().equals("minecraft"))
            .toList()));
    }
}

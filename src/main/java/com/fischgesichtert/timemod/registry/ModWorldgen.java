package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Chronit-Erz tief im Tiefenschiefer (häufiger im Deep Dark), Zeitsand unter den Wüsten. */
public final class ModWorldgen {
    public static final ResourceKey<PlacedFeature> CHRONITE_ORE = key("chronite_ore");
    public static final ResourceKey<PlacedFeature> CHRONITE_ORE_DEEP_DARK = key("chronite_ore_deep_dark");
    public static final ResourceKey<PlacedFeature> TIME_SAND = key("time_sand");

    private ModWorldgen() {
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, TimeMod.id(name));
    }

    public static void init() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, CHRONITE_ORE);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.DEEP_DARK), GenerationStep.Decoration.UNDERGROUND_ORES, CHRONITE_ORE_DEEP_DARK);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.DESERT), GenerationStep.Decoration.UNDERGROUND_ORES, TIME_SAND);
    }
}

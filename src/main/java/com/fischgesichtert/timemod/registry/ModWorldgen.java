package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.future.WeatheringFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Chronit-Erz tief im Tiefenschiefer (häufiger im Deep Dark), Zeitsand unter den Wüsten, Verwitterung der Zukunft. */
public final class ModWorldgen {
    public static final ResourceKey<PlacedFeature> CHRONITE_ORE = key("chronite_ore");
    public static final ResourceKey<PlacedFeature> CHRONITE_ORE_DEEP_DARK = key("chronite_ore_deep_dark");
    public static final ResourceKey<PlacedFeature> TIME_SAND = key("time_sand");
    /** Verwitterung der Bauwerke in Zukunfts-Epochen (läuft als letzter Schritt jeder Dekoration). */
    public static final Feature<NoneFeatureConfiguration> WEATHERING_FEATURE = Registry.register(BuiltInRegistries.FEATURE, TimeMod.id("weathering"),
        new WeatheringFeature());
    public static final ResourceKey<PlacedFeature> WEATHERING = key("weathering");

    private ModWorldgen() {
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, TimeMod.id(name));
    }

    public static void init() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, CHRONITE_ORE);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.DEEP_DARK), GenerationStep.Decoration.UNDERGROUND_ORES, CHRONITE_ORE_DEEP_DARK);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.DESERT), GenerationStep.Decoration.UNDERGROUND_ORES, TIME_SAND);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, WEATHERING);
    }
}

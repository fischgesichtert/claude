package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Auch was die Welt selbst gebaut hat, altert in der Zukunft: Läuft als letzter Schritt der Dekoration in
 * jedem Chunk, der ein Bauwerk enthält (Tempel, Minen, Ruinen …) – nur in Zukunfts-Epochen.
 */
public class WeatheringFeature extends Feature<NoneFeatureConfiguration> {
    public WeatheringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Era era = Eras.byLevel(level.getLevel().dimension());
        if (era == null || !era.isFuture()) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        ChunkAccess access = level.getChunk(chunk.x, chunk.z);
        if (access.getAllReferences().isEmpty() && access.getAllStarts().isEmpty()) {
            return false;
        }
        Weathering.apply(new RegionCanvas(level, chunk), era.years(), WeatheringRules.get());
        return true;
    }
}

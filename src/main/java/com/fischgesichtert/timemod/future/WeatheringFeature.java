package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.town.RuinLife;
import com.fischgesichtert.timemod.future.town.Settlements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Auch was die Welt selbst gebaut hat, altert in der Zukunft: Läuft als letzter Schritt der Dekoration in
 * jedem Chunk, der ein Bauwerk enthält (Tempel, Minen, Ruinen …) – nur in Zukunfts-Epochen. Ausgenommen sind
 * Dörfer, die noch leben: Dort wird gepflegt statt verfallen (ab Stufe 2 sind die alten Wege gepflastert),
 * und Lager, in denen heute Plünderer hausen. In verlassenen Dörfern ziehen Ruinenwächter und Moosbären ein.
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
        Settlements.Presence presence = Settlements.presence(level, access, era.years());
        if (presence.living()) {
            Settlements.renovate(level, chunk, presence);
            return true;
        }
        if (presence.abandoned()) {
            // In verlassenen Dörfern wohnt niemand mehr – nur ihre Wächter sind geblieben
            RuinLife.populate(level, access, presence, era.years());
        }
        Weathering.apply(new RegionCanvas(level, chunk, presence.camps()), presence.decay(era.years()), WeatheringRules.get());
        return true;
    }
}

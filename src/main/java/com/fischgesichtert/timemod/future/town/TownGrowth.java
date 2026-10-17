package com.fischgesichtert.timemod.future.town;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

/**
 * Lässt ein Dorf der Zukunft wachsen, sobald Minecraft es angelegt hat: Die neuen Häuser kommen zum Dorf
 * (sie sitzen wie dessen Häuser im Gelände), Straßen, Mauer, Felder, Markt oder Plünderer-Lager bekommen
 * einen eigenen Start der Struktur {@code timemod:town} im selben Chunk.
 */
public final class TownGrowth {
    private TownGrowth() {
    }

    /** Das gewachsene Dorf und (falls nötig) der Start der Stadt mit Straßen, Mauer, Feldern, Markt oder Lager. */
    public record Growth(StructureStart village, @org.jetbrains.annotations.Nullable StructureStart town) {
    }

    public static StructureStart grow(StructureStart start, Structure structure, int years, RegistryAccess access, ChunkGenerator generator,
                                      RandomState randomState, LevelHeightAccessor heights, long seed, StructureManager manager, ChunkAccess chunk,
                                      SectionPos section) {
        Growth growth = plan(start, structure, years, access, generator, randomState, heights, seed);
        if (growth.village() != start || growth.town() != null) {
            com.fischgesichtert.timemod.TimeMod.LOGGER.debug("Dorf {} in +{}: {} Teile im Kern, {} neue Häuser, {} Stadtteile", start.getChunkPos(), years,
                start.getPieces().size(), growth.village().getPieces().size() - start.getPieces().size(),
                growth.town() == null ? 0 : growth.town().getPieces().size());
        }
        if (growth.town() != null) {
            manager.setStartForStructure(section, growth.town().getStructure(), growth.town(), chunk);
        }
        return growth.village();
    }

    public static Growth plan(StructureStart start, Structure structure, int years, RegistryAccess access, ChunkGenerator generator,
                              RandomState randomState, LevelHeightAccessor heights, long seed) {
        Registry<Structure> registry = access.registryOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = registry.wrapAsHolder(structure);
        if (!holder.is(StructureTags.VILLAGE) || start.getPieces().isEmpty()) {
            return new Growth(start, null);
        }
        ChunkPos pos = start.getChunkPos();
        Settlements.Outlook outlook = Settlements.outlook(seed, pos, years);
        if (outlook.stage() == 0 && outlook.fate() != Settlements.Fate.RAIDED) {
            return new Growth(start, null);
        }
        ResourceLocation id = registry.getKey(structure);
        TownStyle style = TownStyle.of(TownStyle.typeOf(id == null ? "" : id.toString()), Math.max(1, outlook.stage()));
        TownPlanner.Plan plan = TownPlanner.plan(start.getPieces(), pos, outlook, style, generator, randomState, heights,
            seed ^ pos.toLong() * 0x2545F4914F6CDD1DL);
        Structure townStructure = registry.get(TownPieces.TOWN);
        StructureStart town = townStructure == null || plan.town().isEmpty() ? null
            : new StructureStart(townStructure, pos, 0, new PiecesContainer(plan.town()));
        if (plan.buildings().isEmpty()) {
            return new Growth(start, town);
        }
        List<StructurePiece> pieces = new ArrayList<>(start.getPieces());
        pieces.addAll(plan.buildings());
        return new Growth(new StructureStart(structure, pos, start.getReferences(), new PiecesContainer(pieces)), town);
    }
}

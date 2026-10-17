package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.TimeMod;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Bauteile der gewachsenen Siedlungen und die Struktur, die Straßen, Mauer, Felder und Markt trägt. Sie
 * entsteht nie von selbst – nur als Begleiter eines Dorfes in einer Zukunfts-Epoche – und passt das Gelände
 * nicht an (sonst würde eine Ringstraße ganze Landstriche einebnen). Die Häuser selbst gehören zum Dorf und
 * sitzen wie dessen Häuser im Gelände.
 */
public final class TownPieces {
    public static final StructurePieceType BUILDING = piece("town_building", BuildingPiece::new);
    public static final StructurePieceType ROAD = piece("town_road", RoadPiece::new);
    public static final StructurePieceType WALL = piece("town_wall", WallPiece::new);
    public static final StructurePieceType FIELD = piece("town_field", FieldPiece::new);
    public static final StructurePieceType PLAZA = piece("town_plaza", PlazaPiece::new);
    public static final StructurePieceType CAMP = piece("town_camp", CampPiece::new);

    public static final ResourceKey<Structure> TOWN = ResourceKey.create(Registries.STRUCTURE, TimeMod.id("town"));
    public static final StructureType<TownStructure> TOWN_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, TimeMod.id("town"),
        () -> TownStructure.CODEC);

    private TownPieces() {
    }

    public static void init() {
        Settlements.init();
    }

    private static StructurePieceType piece(String name, Function<CompoundTag, StructurePiece> factory) {
        return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, TimeMod.id(name), (StructurePieceType.ContextlessType) factory::apply);
    }

    /** Entsteht nie von selbst; ihre Starts setzt das Dorf-Wachstum ein. */
    public static class TownStructure extends Structure {
        public static final Codec<TownStructure> CODEC = simpleCodec(TownStructure::new);

        public TownStructure(StructureSettings settings) {
            super(settings);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
            return Optional.empty();
        }

        @Override
        public StructureType<?> type() {
            return TOWN_TYPE;
        }
    }
}

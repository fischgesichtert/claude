package com.fischgesichtert.timemod.worldgen;

import com.fischgesichtert.timemod.TimeMod;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/** Struktur- und Teiltypen; die Platzierung selbst steht in data/timemod/worldgen. */
public final class ModStructures {
    public static final StructurePieceType CLOCK_TOWER_PIECE = piece("clock_tower", ClockTowerPiece::new);
    public static final StructurePieceType OBSERVATORY_PIECE = piece("observatory", ObservatoryPiece::new);
    public static final StructurePieceType CHRONO_RUINS_PIECE = piece("chrono_ruins", ChronoRuinsPiece::new);
    public static final StructurePieceType TIMELESS_WORKSHOP_PIECE = piece("timeless_workshop", TimelessWorkshopPiece::new);

    public static final StructureType<TimeStructure> CLOCK_TOWER = type("clock_tower", TimeStructure.Kind.CLOCK_TOWER);
    public static final StructureType<TimeStructure> OBSERVATORY = type("observatory", TimeStructure.Kind.OBSERVATORY);
    public static final StructureType<TimeStructure> CHRONO_RUINS = type("chrono_ruins", TimeStructure.Kind.CHRONO_RUINS);
    public static final StructureType<TimeStructure> TIMELESS_WORKSHOP = type("timeless_workshop", TimeStructure.Kind.TIMELESS_WORKSHOP);

    private ModStructures() {
    }

    public static void init() {
    }

    static StructureType<?> type(TimeStructure.Kind kind) {
        return switch (kind) {
            case CLOCK_TOWER -> CLOCK_TOWER;
            case OBSERVATORY -> OBSERVATORY;
            case CHRONO_RUINS -> CHRONO_RUINS;
            case TIMELESS_WORKSHOP -> TIMELESS_WORKSHOP;
        };
    }

    private static StructurePieceType piece(String name, Function<CompoundTag, StructurePiece> factory) {
        return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, TimeMod.id(name), (StructurePieceType.ContextlessType) factory::apply);
    }

    private static StructureType<TimeStructure> type(String name, TimeStructure.Kind kind) {
        var codec = TimeStructure.codec(kind);
        return Registry.register(BuiltInRegistries.STRUCTURE_TYPE, TimeMod.id(name), () -> codec);
    }
}

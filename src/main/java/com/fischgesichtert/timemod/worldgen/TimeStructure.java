package com.fischgesichtert.timemod.worldgen;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Die Strukturen von TIME. Wo genau sie entstehen, entscheidet die Art. */
public class TimeStructure extends Structure {
    public enum Kind {
        CLOCK_TOWER, OBSERVATORY, CHRONO_RUINS, TIMELESS_WORKSHOP, TIMELESS_CITADEL
    }

    @FunctionalInterface
    private interface PieceFactory {
        StructurePiece create(BlockPos origin, Direction orientation, long seed);
    }

    private final Kind kind;
    /** Nur zur Fehlersuche (timemod.debug): warum Kandidaten abgelehnt wurden. */
    public static final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> REJECTED = new java.util.concurrent.ConcurrentHashMap<>();

    private static Optional<GenerationStub> reject(String reason) {
        if (com.fischgesichtert.timemod.TimeMod.DEBUG) {
            REJECTED.computeIfAbsent(reason, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
        }
        return Optional.empty();
    }

    public TimeStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    public static Codec<TimeStructure> codec(Kind kind) {
        return simpleCodec(settings -> new TimeStructure(settings, kind));
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return switch (kind) {
            case CLOCK_TOWER -> onSurface(context, ClockTowerPiece.SIZE, 14, false, ClockTowerPiece::new);
            case OBSERVATORY -> onSurface(context, ObservatoryPiece.SIZE, 28, true, ObservatoryPiece::new);
            case CHRONO_RUINS -> underground(context, ChronoRuinsPiece.SIZE, -50, -38, ChronoRuinsPiece::new);
            case TIMELESS_WORKSHOP -> underground(context, TimelessWorkshopPiece.SIZE, -28, 8, TimelessWorkshopPiece::new);
            case TIMELESS_CITADEL -> floating(context, TimelessCitadelPiece.SIZE, 70, 86, TimelessCitadelPiece::new);
        };
    }

    /**
     * Auf dem tiefsten Eckpunkt der Grundfläche, nie im Wasser. Erst die billigen Prüfungen (eine
     * Höhe, Biom), dann die teureren – die meisten Kandidaten scheitern schon am Biom.
     */
    private static Optional<GenerationStub> onSurface(GenerationContext context, int size, int maxSlope, boolean onPeak, PieceFactory factory) {
        ChunkPos chunk = context.chunkPos();
        int x0 = chunk.getMinBlockX() + Math.max(0, (16 - size) / 2);
        int z0 = chunk.getMinBlockZ() + Math.max(0, (16 - size) / 2);
        int cx = x0 + size / 2;
        int cz = z0 + size / 2;
        var generator = context.chunkGenerator();
        int top = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (top < generator.getSeaLevel()) {
            return reject("unter Meereshöhe " + top);
        }
        var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(top), QuartPos.fromBlock(cz),
            context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return reject("Biom " + biome.unwrapKey().map(k -> k.location().getPath()).orElse("?"));
        }
        // Wasser an der Oberfläche: Meeresboden liegt tiefer als die Oberfläche
        int floor = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        if (floor < top) {
            return reject("Wasser");
        }
        int lowest = top;
        for (int[] c : new int[][] {{0, 0}, {size - 1, 0}, {0, size - 1}, {size - 1, size - 1}}) {
            lowest = Math.min(lowest, generator.getFirstOccupiedHeight(x0 + c[0], z0 + c[1], Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()));
        }
        // Ecken dürfen etwas tiefer liegen (Ufer, Mulden) – Fundament und Gelände-Anpassung füllen auf
        if (lowest < generator.getSeaLevel() - 4 || top - lowest > maxSlope) {
            return reject("Hang " + (top - lowest));
        }
        // Auf Gipfeln oben aufsetzen (die Gelände-Anpassung trägt den Gipfel ab), sonst am tiefsten Punkt
        int y = onPeak ? top - 2 : Math.max(lowest, generator.getSeaLevel());
        Direction orientation = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        long seed = context.random().nextLong();
        BlockPos origin = new BlockPos(x0, y, z0);
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(factory.create(origin, orientation, seed))));
    }

    /** In fester Tiefe; der Biom-Check passiert genau dort (z. B. Deep Dark). */
    private static Optional<GenerationStub> underground(GenerationContext context, int size, int minY, int maxY, PieceFactory factory) {
        ChunkPos chunk = context.chunkPos();
        int y = minY + context.random().nextInt(maxY - minY + 1);
        Direction orientation = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        long seed = context.random().nextLong();
        BlockPos origin = new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ());
        BlockPos center = origin.offset(size / 2, 2, size / 2);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(factory.create(origin, orientation, seed))));
    }

    /** Frei schwebend in fester Höhe (äußeres End); das Biom entscheidet, ob hier eine entsteht. */
    private static Optional<GenerationStub> floating(GenerationContext context, int size, int minY, int maxY, PieceFactory factory) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMinBlockX() + size / 2;
        int cz = chunk.getMinBlockZ() + size / 2;
        int y = minY + context.random().nextInt(maxY - minY + 1);
        var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(64), QuartPos.fromBlock(cz),
            context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return reject("Biom " + biome.unwrapKey().map(k -> k.location().getPath()).orElse("?"));
        }
        Direction orientation = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        long seed = context.random().nextLong();
        BlockPos origin = new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ());
        return Optional.of(new GenerationStub(new BlockPos(cx, y + 10, cz), builder -> builder.addPiece(factory.create(origin, orientation, seed))));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.type(kind);
    }
}

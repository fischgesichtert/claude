package com.fischgesichtert.timemod.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Gemeinsame Basis der TIME-Strukturen. Alle Zufallsentscheidungen hängen nur vom Strukturseed und
 * der Position ab – so passt der Verfall über Chunkgrenzen hinweg zusammen, egal in welcher
 * Reihenfolge die Chunks erzeugt werden.
 */
public abstract class TimePiece extends StructurePiece {
    protected static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected final long seed;

    protected TimePiece(StructurePieceType type, BoundingBox box, Direction orientation, long seed) {
        super(type, 0, box);
        this.seed = seed;
        setOrientation(orientation);
    }

    protected TimePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    /** Fester Pseudo-Zufall in [0, 1) für eine Position (und einen Zweck). */
    protected float chance(int x, int y, int z, int salt) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h ^= y * 0xC2B2AE3D27D4EB4FL;
        h ^= z * 0x165667B19E3779F9L;
        h ^= salt * 0x27D4EB2F165667C5L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1L << 24);
    }

    protected void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        placeBlock(level, state, x, y, z, box);
    }

    protected void fill(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    placeBlock(level, state, x, y, z, box);
                }
            }
        }
    }

    protected boolean isAir(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        return getBlock(level, x, y, z, box).isAir();
    }

    /** Wählt je nach Zufall einen der Zustände (gleich gewichtet nach den angegebenen Anteilen). */
    protected BlockState pick(int x, int y, int z, int salt, BlockState base, float p1, BlockState v1, float p2, BlockState v2) {
        float c = chance(x, y, z, salt);
        if (c < p1) {
            return v1;
        }
        if (c < p1 + p2) {
            return v2;
        }
        return base;
    }
}

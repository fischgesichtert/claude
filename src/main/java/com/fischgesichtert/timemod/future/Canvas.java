package com.fischgesichtert.timemod.future;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Worauf die Verwitterung arbeitet: genau ein Chunk, in den geschrieben wird, und seine Nachbarschaft,
 * die nur gelesen wird (soweit bekannt).
 */
public interface Canvas {
    ChunkPos chunk();

    int minY();

    /** Erste Höhe oberhalb der Welt. */
    int maxY();

    long seed();

    /** Der Zustand an einer Stelle; außerhalb des Chunks {@code null}, wenn dort nichts bekannt ist. */
    @Nullable
    BlockState get(int x, int y, int z);

    /** Schreibt – nur innerhalb des Chunks. */
    void set(int x, int y, int z, BlockState state);

    Holder<Biome> biome(int x, int y, int z);

    default boolean inside(int x, int z) {
        return (x >> 4) == chunk().x && (z >> 4) == chunk().z;
    }

    /** Innerhalb des Chunks nie {@code null}. */
    default BlockState at(int x, int y, int z) {
        BlockState state = get(x, y, z);
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }
}

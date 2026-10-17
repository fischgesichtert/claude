package com.fischgesichtert.timemod.future;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Verwitterung während der Welterzeugung: schreibt in den Chunk, der gerade dekoriert wird, liest seine Nachbarn.
 * Was in {@code kept} liegt (etwa ein Lager, in dem heute noch jemand wohnt), bleibt unberührt.
 */
final class RegionCanvas implements Canvas {
    private final WorldGenLevel level;
    private final ChunkPos chunk;
    private final List<BoundingBox> kept;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    RegionCanvas(WorldGenLevel level, ChunkPos chunk) {
        this(level, chunk, List.of());
    }

    RegionCanvas(WorldGenLevel level, ChunkPos chunk, List<BoundingBox> kept) {
        this.level = level;
        this.chunk = chunk;
        this.kept = kept;
    }

    @Override
    public ChunkPos chunk() {
        return chunk;
    }

    @Override
    public int minY() {
        return level.getMinBuildHeight();
    }

    @Override
    public int maxY() {
        return level.getMaxBuildHeight();
    }

    @Override
    public long seed() {
        return level.getSeed();
    }

    @Nullable
    @Override
    public BlockState get(int x, int y, int z) {
        // Die Region reicht einen Chunk weit um den dekorierten herum
        if (Math.abs((x >> 4) - chunk.x) > 1 || Math.abs((z >> 4) - chunk.z) > 1) {
            return null;
        }
        return level.getBlockState(cursor.set(x, y, z));
    }

    @Override
    public void set(int x, int y, int z, BlockState state) {
        if (inside(x, z) && y >= minY() && y < maxY()) {
            cursor.set(x, y, z);
            for (BoundingBox box : kept) {
                if (box.isInside(cursor)) {
                    return;
                }
            }
            level.setBlock(cursor, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Holder<Biome> biome(int x, int y, int z) {
        return level.getBiome(cursor.set(x, y, z));
    }
}

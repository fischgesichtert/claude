package com.fischgesichtert.timemod.future;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Arbeitsabbild eines Chunks der Gegenwart: Die Verwitterung rechnet darauf, erst danach wird das
 * Ergebnis in die Zukunft geschrieben. Die Nachbarschaft liest es aus der Zukunft (nur geladene Chunks).
 */
final class WorkCanvas implements Canvas {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ServerLevel future;
    private final ChunkPos chunk;
    private final int minY;
    private final int maxY;
    private final BlockState[] states;

    WorkCanvas(ServerLevel future, LevelChunk present) {
        this.future = future;
        this.chunk = present.getPos();
        this.minY = future.getMinBuildHeight();
        this.maxY = future.getMaxBuildHeight();
        this.states = new BlockState[16 * 16 * (maxY - minY)];
        java.util.Arrays.fill(states, AIR);
        LevelChunkSection[] sections = present.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) {
                continue;
            }
            int baseY = present.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                int worldY = baseY + y;
                if (worldY < minY || worldY >= maxY) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        states[index(x, worldY, z)] = section.getBlockState(x, y, z);
                    }
                }
            }
        }
    }

    private int index(int localX, int y, int localZ) {
        return ((y - minY) * 16 + localZ) * 16 + localX;
    }

    /** Der berechnete Zustand an einer Stelle innerhalb des Chunks. */
    BlockState local(int localX, int y, int localZ) {
        return states[index(localX, y, localZ)];
    }

    @Override
    public ChunkPos chunk() {
        return chunk;
    }

    @Override
    public int minY() {
        return minY;
    }

    @Override
    public int maxY() {
        return maxY;
    }

    @Override
    public long seed() {
        return future.getSeed();
    }

    @Nullable
    @Override
    public BlockState get(int x, int y, int z) {
        if (y < minY || y >= maxY) {
            return AIR;
        }
        if (inside(x, z)) {
            return states[index(x & 15, y, z & 15)];
        }
        LevelChunk neighbour = future.getChunkSource().getChunkNow(x >> 4, z >> 4);
        return neighbour == null ? null : neighbour.getBlockState(new BlockPos(x, y, z));
    }

    @Override
    public void set(int x, int y, int z, BlockState state) {
        if (y >= minY && y < maxY && inside(x, z)) {
            states[index(x & 15, y, z & 15)] = state;
        }
    }

    @Override
    public Holder<Biome> biome(int x, int y, int z) {
        return future.getBiome(new BlockPos(x, y, z));
    }
}

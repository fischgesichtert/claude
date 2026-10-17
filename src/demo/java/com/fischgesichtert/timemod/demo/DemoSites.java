package com.fischgesichtert.timemod.demo;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** Bühnen für Vorführungen: eine weite, flache Ebene in der Gegenwart. */
final class DemoSites {
    private static final ResourceKey<Biome> PLAINS = ResourceKey.create(Registries.BIOME, new ResourceLocation("plains"));
    private static final ResourceKey<Biome> SUNFLOWER_PLAINS = ResourceKey.create(Registries.BIOME, new ResourceLocation("sunflower_plains"));

    private DemoSites() {
    }

    /**
     * Grob nach einer weiten Ebene suchen (das Biom ist billig), erst dort die Höhen an den Versätzen
     * {@code around} prüfen. Fast ebene Stellen (höchstens 3 Blöcke Unterschied) müssen zusätzlich
     * {@code accept} bestehen; gibt es keine, gewinnt die ebenste.
     */
    static Site flatPlains(ServerLevel present, int[][] around, Predicate<BlockPos> accept) {
        ChunkGenerator generator = present.getChunkSource().getGenerator();
        RandomState random = present.getChunkSource().randomState();
        int sea = generator.getSeaLevel();
        BlockPos best = null;
        int bestSpread = Integer.MAX_VALUE;
        int tried = 0;
        for (BlockPos.MutableBlockPos step : BlockPos.spiralAround(BlockPos.ZERO, 40, Direction.EAST, Direction.SOUTH)) {
            int x = step.getX() * 64;
            int z = step.getZ() * 64;
            if (!isPlains(generator, random, x, sea + 8, z) || !isPlains(generator, random, x + 40, sea + 8, z)
                || !isPlains(generator, random, x - 40, sea + 8, z) || !isPlains(generator, random, x, sea + 8, z + 40)
                || !isPlains(generator, random, x, sea + 8, z - 40)) {
                continue;
            }
            int centre = generator.getBaseHeight(x + 4, z, Heightmap.Types.WORLD_SURFACE_WG, present, random);
            if (centre <= sea + 1 || !isPlains(generator, random, x + 4, centre, z)) {
                continue;
            }
            int spread = 0;
            for (int[] offset : around) {
                int height = generator.getBaseHeight(x + offset[0], z + offset[1], Heightmap.Types.WORLD_SURFACE_WG, present, random);
                spread = Math.max(spread, height <= sea + 1 ? 99 : Math.abs(height - centre));
            }
            BlockPos site = new BlockPos(x, centre, z);
            if (spread < bestSpread && (spread > 3 || accept.test(site))) {
                best = site;
                bestSpread = spread;
            }
            if (bestSpread <= 2 || ++tried >= 80) {
                break;
            }
        }
        if (best == null) {
            best = new BlockPos(0, generator.getBaseHeight(0, 0, Heightmap.Types.WORLD_SURFACE_WG, present, random), 0);
        }
        return new Site(best, bestSpread);
    }

    record Site(BlockPos pos, int spread) {
    }

    private static boolean isPlains(ChunkGenerator generator, RandomState random, int x, int y, int z) {
        var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler());
        return biome.is(PLAINS) || biome.is(SUNFLOWER_PLAINS);
    }
}

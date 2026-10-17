package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wald-Sukzession zwischen den Trümmern: Wo die Zukunft Wald ist, wächst er auch auf dem freien Boden
 * eines abgeleiteten Chunks nach – Holz und Wuchs nach dem Biom, umso dichter, je mehr Jahre vergangen
 * sind. Gebautes bleibt frei (dort wachsen nur die Ruinenbäume der Rückeroberung).
 */
final class Succession {
    private static final long SALT = 0x5ACC_E551L;
    /** So weit (waagrecht) hält ein neuer Baum Abstand zu allem Gebauten. */
    private static final int CLEARANCE = 2;

    private Succession() {
    }

    static int apply(Canvas canvas, long years, WeatheringRules.Resolved rules) {
        double chance = rules.reclaim().forestTrees().at(years);
        if (chance <= 0) {
            return 0;
        }
        double size = Mth.clamp(Math.log10(Math.max(1, years)) / 3.0, 0.0, 1.0);
        ChunkPos chunk = canvas.chunk();
        int grown = 0;
        for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                int y = ground(canvas, x, z);
                if (y <= canvas.minY()) {
                    continue;
                }
                BlockState soil = canvas.at(x, y, z);
                if (!soil.is(BlockTags.DIRT) || rules.isMade(soil) || !canvas.at(x, y + 1, z).getFluidState().isEmpty()) {
                    continue;
                }
                RandomSource random = Weathering.random(canvas.seed(), SALT, x, y, z);
                if (random.nextFloat() >= chance) {
                    continue;
                }
                Holder<Biome> biome = canvas.biome(x, y + 1, z);
                if (!biome.is(FutureBiomeSource.WOODED) || nearMade(canvas, rules, x, y, z)) {
                    continue;
                }
                if (Trees.grow(canvas, x, y + 1, z, Trees.of(biome), random, size)) {
                    grown++;
                }
            }
        }
        return grown;
    }

    /** Der oberste feste Block einer Spalte (Gras, Blumen und Schnee darüber zählen nicht). */
    private static int ground(Canvas canvas, int x, int z) {
        for (int y = canvas.maxY() - 1; y > canvas.minY(); y--) {
            BlockState state = canvas.at(x, y, z);
            if (!state.isAir() && !state.canBeReplaced()) {
                return y;
            }
        }
        return canvas.minY();
    }

    private static boolean nearMade(Canvas canvas, WeatheringRules.Resolved rules, int x, int y, int z) {
        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -CLEARANCE; dx <= CLEARANCE; dx++) {
                for (int dz = -CLEARANCE; dz <= CLEARANCE; dz++) {
                    BlockState state = canvas.get(x + dx, y + dy, z + dz);
                    if (state != null && !state.isAir() && rules.isMade(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

package com.fischgesichtert.timemod.future;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Statik: Jeder gebaute Block hat einen Abstand zur nächsten Stütze. Was auf etwas ruht, erbt dessen
 * Abstand; seitlich oder hängend kommt eins hinzu. Trägt das Material nach so vielen Jahren nicht mehr so
 * weit ({@code span} der Regel), stürzt der Block ein – Dächer brechen ein, Brücken fallen.
 *
 * <p>Natürlicher Boden trägt immer. Am Rand des Chunks gilt ein fester Nachbar als Stütze, damit Bauten
 * über Chunkgrenzen nicht willkürlich zerschnitten werden.
 */
final class Statics {
    private static final long SALT = 0x51A7_1C5EL;

    private Statics() {
    }

    /** Nur im Höhenband {@code low..high} – darüber und darunter steht nichts Gebautes. */
    static void collapse(Canvas canvas, long years, WeatheringRules.Resolved rules, int low, int high) {
        ChunkPos chunk = canvas.chunk();
        int minY = low;
        int height = high - low + 1;
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        int[] distance = new int[16 * 16 * height];
        Arrays.fill(distance, Integer.MAX_VALUE);
        boolean[] made = new boolean[distance.length];
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        boolean any = false;
        for (int i = 0; i < distance.length; i++) {
            int x = x0 + (i & 15);
            int z = z0 + ((i >> 4) & 15);
            int y = minY + (i >> 8);
            BlockState state = canvas.at(x, y, z);
            if (state.isAir() || !rules.isMade(state)) {
                continue;
            }
            made[i] = true;
            any = true;
            if (grounded(canvas, rules, x, y, z)) {
                distance[i] = 0;
                queue.enqueue(i);
            }
        }
        if (!any) {
            return;
        }
        // 0-1-Breitensuche: nach oben kostet nichts (aufliegen), seitlich und nach unten (hängen) eins
        while (!queue.isEmpty()) {
            int i = queue.dequeueInt();
            int d = distance[i];
            int lx = i & 15;
            int lz = (i >> 4) & 15;
            int ly = i >> 8;
            for (Direction direction : Direction.values()) {
                int nx = lx + direction.getStepX();
                int ny = ly + direction.getStepY();
                int nz = lz + direction.getStepZ();
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < 0 || ny >= height) {
                    continue;
                }
                int j = (ny << 8) | (nz << 4) | nx;
                if (!made[j]) {
                    continue;
                }
                int weight = direction == Direction.UP ? 0 : 1;
                if (d + weight < distance[j]) {
                    distance[j] = d + weight;
                    if (weight == 0) {
                        queue.enqueueFirst(j);
                    } else {
                        queue.enqueue(j);
                    }
                }
            }
        }
        for (int i = 0; i < distance.length; i++) {
            if (!made[i]) {
                continue;
            }
            int x = x0 + (i & 15);
            int z = z0 + ((i >> 4) & 15);
            int y = minY + (i >> 8);
            BlockState state = canvas.at(x, y, z);
            WeatheringRule rule = rules.of(state);
            if (rule == null || distance[i] <= rule.span().at(years)) {
                continue;
            }
            BlockState rest = Weathering.removed(state);
            RandomSource random = Weathering.random(canvas.seed(), SALT, x, y, z);
            if (rule.rubble() != null && random.nextFloat() < rule.rubble().chance()) {
                rest = Weathering.dropRubble(canvas, x, y, z, rule.rubble().block(), rest);
            }
            canvas.set(x, y, z, rest);
        }
    }

    /** Ruht auf gewachsenem Boden – oder hängt am Rand an einem festen (oder unbekannten) Nachbarn. */
    private static boolean grounded(Canvas canvas, WeatheringRules.Resolved rules, int x, int y, int z) {
        if (y <= canvas.minY()) {
            return true;
        }
        BlockState below = canvas.at(x, y - 1, z);
        if (!below.isAir() && !below.canBeReplaced() && !rules.isMade(below)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int nx = x + direction.getStepX();
            int nz = z + direction.getStepZ();
            if (canvas.inside(nx, nz)) {
                continue;
            }
            BlockState neighbour = canvas.get(nx, y, nz);
            if (neighbour == null || (!neighbour.isAir() && !neighbour.canBeReplaced())) {
                return true;
            }
        }
        return false;
    }
}

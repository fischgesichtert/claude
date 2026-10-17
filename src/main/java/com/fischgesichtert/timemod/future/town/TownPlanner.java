package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.future.town.BuildingPiece.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Plant, wie ein Dorf gewachsen ist: Ringstraßen um den alten Kern, Hauptstraßen hinaus zu den Toren,
 * Grundstücke an den Straßen (Häuser zur Straße hin), Marktplatz, Kirche und – je nach Stufe – Schenke,
 * Bibliothek, Rathaus, Uhrturm, Mauer mit Türmen, Felder und Mühlen davor.
 *
 * <p>Alles hängt nur am Seed des Dorfes: Wer zweimal hinreist, findet dieselbe Stadt. Die Grundstücke werden
 * in fester Reihenfolge vergeben, so steht das Haus von +100 meist auch bei +1000 noch an seinem Platz.
 */
public final class TownPlanner {
    /** Weiter vom Start-Chunk entfernt erreichen Bauteile ihre Chunks nicht mehr (8 Chunks Reichweite). */
    private static final int REACH = 112;
    private static final int GRID = 256;
    private static final int RING_GAP = 28;
    /** Von der äußeren Ringstraße bis zur Mauer: Platz für eine Reihe Häuser samt Vorgarten. */
    private static final int WALL_GAP = 28;

    /** Neue Häuser (gehören zum Dorf) und Straßen, Mauer, Felder, Markt oder Lager (eigene Struktur). */
    public record Plan(List<StructurePiece> buildings, List<StructurePiece> town, Map<String, Integer> rejected) {
    }

    private final ChunkGenerator generator;
    private final RandomState randomState;
    private final LevelHeightAccessor heights;
    private final TownStyle style;
    private final int stage;
    private final RandomSource random;
    private final int cx;
    private final int cz;
    private final int groundY;
    private final double sx;
    private final double sz;
    /** Nichts mehr frei: Häuser, Felder, Straßen samt Rand. */
    private final boolean[] occupied = new boolean[GRID * GRID];
    /** Gepflastert (dort enden Hauswege). */
    private final boolean[] paved = new boolean[GRID * GRID];
    /** Bebaut: altes Dorf, Grundstücke, Felder, Platz – über Straßen darf ein Platz gelegt werden, hierüber nicht. */
    private final boolean[] built = new boolean[GRID * GRID];
    private final List<BoundingBox> streets = new ArrayList<>();
    private final Map<Long, int[]> terrain = new HashMap<>();
    private final List<StructurePiece> buildings = new ArrayList<>();
    private final List<StructurePiece> roads = new ArrayList<>();
    private final List<StructurePiece> town = new ArrayList<>();
    private final long seed;
    private final Map<String, Integer> rejected = new java.util.TreeMap<>();
    private int r0;

    private TownPlanner(List<StructurePiece> vanilla, ChunkPos start, TownStyle style, ChunkGenerator generator, RandomState randomState,
                        LevelHeightAccessor heights, long seed) {
        this.generator = generator;
        this.randomState = randomState;
        this.heights = heights;
        this.style = style;
        this.stage = style.stage();
        this.seed = seed;
        this.random = RandomSource.create(seed);
        BoundingBox core = vanilla.get(0).getBoundingBox();
        this.cx = core.getCenter().getX();
        this.cz = core.getCenter().getZ();
        this.groundY = core.minY();
        this.sx = start.getMiddleBlockX();
        this.sz = start.getMiddleBlockZ();
        List<Double> spread = new ArrayList<>();
        for (StructurePiece piece : vanilla) {
            BoundingBox box = piece.getBoundingBox();
            boolean street = piece instanceof PoolElementStructurePiece pool && pool.getElement().toString().contains("streets");
            if (street) {
                // Alte Dorfwege: Hier enden neue Hauswege, Häuser dürfen daran stehen
                streets.add(box);
                mark(occupied, box.minX(), box.minZ(), box.maxX(), box.maxZ());
                mark(paved, box.minX(), box.minZ(), box.maxX(), box.maxZ());
            } else {
                mark(occupied, box.minX() - 1, box.minZ() - 1, box.maxX() + 1, box.maxZ() + 1);
                mark(built, box.minX() - 1, box.minZ() - 1, box.maxX() + 1, box.maxZ() + 1);
                spread.add(distance(box.getCenter().getX(), box.getCenter().getZ(), cx, cz));
            }
        }
        // Der Rand des alten Dorfes: Hier läuft der erste Ring (um alte Häuser herum), weiter draußen die übrigen
        spread.sort(Double::compare);
        int typical = spread.isEmpty() ? 20 : (int) Math.round(spread.get((int) (spread.size() * 0.6)));
        this.r0 = Mth.clamp(typical, 16, 60);
    }

    public static Plan plan(List<StructurePiece> vanilla, ChunkPos start, Settlements.Outlook outlook, TownStyle style, ChunkGenerator generator,
                     RandomState randomState, LevelHeightAccessor heights, long seed) {
        TownPlanner planner = new TownPlanner(vanilla, start, style, generator, randomState, heights, seed);
        // Untergegangene Dörfer hinterlassen die Siedlung, die sie bis dahin geworden waren – als Ruine
        if (outlook.stage() > 0) {
            planner.grow();
        }
        if (outlook.fate() == Settlements.Fate.RAIDED) {
            planner.camp();
        }
        List<StructurePiece> town = new ArrayList<>(planner.roads);
        town.addAll(planner.town);
        if (!outlook.alive()) {
            for (StructurePiece piece : town) {
                if (piece instanceof TownPiece townPiece && !(piece instanceof CampPiece)) {
                    townPiece.abandon();
                }
            }
            planner.buildings.forEach(piece -> ((TownPiece) piece).abandon());
        }
        return new Plan(planner.buildings, town, planner.rejected);
    }

    // ------------------------------------------------------------------ Plünderer

    private void camp() {
        int x0 = cx - CampPiece.SIZE / 2;
        int z0 = cz - CampPiece.SIZE / 2;
        int y = median(x0, z0, x0 + CampPiece.SIZE - 1, z0 + CampPiece.SIZE - 1) - 1;
        town.add(new CampPiece(x0, y, z0, seed ^ 0xCA4FL, style));
    }

    // ------------------------------------------------------------------ Wachstum

    private void grow() {
        double limit = REACH - distance(cx, cz, sx, sz);
        int roadWidth = style.roadWidth();
        List<Integer> rings = new ArrayList<>();
        int ringCount = switch (stage) {
            case 1, 2 -> 1;
            case 3 -> 2;
            default -> 3;
        };
        for (int k = 0; k < ringCount; k++) {
            int radius = r0 + 10 + roadWidth / 2 + k * RING_GAP;
            int wall = radius + WALL_GAP;
            if ((stage >= 2 ? wall + 4 : radius + 14) > limit && !rings.isEmpty()) {
                break;
            }
            rings.add(radius);
        }
        int outerRing = rings.get(rings.size() - 1);
        int wallRadius = stage >= 2 ? (int) Math.min(outerRing + WALL_GAP, limit - 4) : -1;
        int avenues = switch (stage) {
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 6;
            default -> 8;
        };
        double theta = random.nextDouble() * Mth.TWO_PI;
        float[] angles = new float[avenues];
        for (int i = 0; i < avenues; i++) {
            angles[i] = (float) Mth.wrapDegrees(Math.toDegrees(theta + i * Mth.TWO_PI / avenues)) * Mth.DEG_TO_RAD;
        }
        // 1. Straßen: Ringe, Hauptstraßen hinaus (bis vor das Tor), Anschlüsse an die alten Dorfwege
        for (int radius : rings) {
            ring(radius, roadWidth);
        }
        int outer = stage >= 2 ? (int) Math.min(wallRadius + 8, limit - 2) : outerRing;
        for (float angle : angles) {
            // Von innen nach außen bis zum ersten freien Stück, dann hinaus bis vor das Tor
            int from = r0 / 2;
            while (from < rings.get(0) && !free(built, point(angle, from)[0] - 2, point(angle, from)[1] - 2, point(angle, from)[0] + 2,
                point(angle, from)[1] + 2, false)) {
                from++;
            }
            int[] inner = point(angle, from);
            int[] end = point(angle, outer);
            if (from < outer - 4 && clear(inner, end, roadWidth)) {
                road(new int[] {inner[0], end[0]}, new int[] {inner[1], end[1]}, false, roadWidth);
                connect(inner, roadWidth);
            }
        }
        // 2. Marktplatz an der ersten Kreuzung, die eben genug ist
        boolean church = false;
        if (stage >= 2) {
            for (float angle : angles) {
                if (plaza(angle, rings.get(0))) {
                    // Die Kirche steht am Markt, die Tür zum Platz
                    int[] behind = point(angle, rings.get(0) + (stage >= 4 ? 17 : 13) / 2 + 13);
                    church = lotFacing(Kind.CHURCH, behind[0], behind[1], facingToward(behind, point(angle, rings.get(0))), wallRadius, 0);
                    break;
                }
            }
        }
        // 3. Grundstücke: erst die besonderen Häuser, dann Wohnhäuser und Werkstätten
        List<Kind> specials = new ArrayList<>();
        if (stage >= 2 && !church) {
            specials.add(Kind.CHURCH);
        }
        if (stage >= 3) {
            specials.add(Kind.TAVERN);
        }
        if (stage >= 4) {
            specials.addAll(Arrays.asList(Kind.CLOCK_TOWER, Kind.LIBRARY, Kind.TOWN_HALL));
        }
        int homes = switch (stage) {
            case 1 -> 5;
            case 2 -> 12;
            case 3 -> 20;
            default -> 30;
        };
        lots(rings, angles, roadWidth, wallRadius, limit, specials, homes);
        // 4. Mauer mit Toren an den Hauptstraßen, ab Stufe 3 mit Türmen dazwischen
        if (stage >= 2) {
            float[] towers = new float[stage >= 3 ? avenues : 0];
            for (int i = 0; i < towers.length; i++) {
                towers[i] = (float) (angles[i] + Math.PI / avenues);
            }
            town.add(new WallPiece(cx, cz, wallRadius, angles, towers, groundY - 40, groundY + 50, seed ^ 0x3A11L, style));
        }
        // 5. Felder vor der Mauer (oder zwischen den Ringen, wenn draußen kein Platz ist), ab Stufe 3 mit Mühlen
        fields(angles, stage >= 2 ? wallRadius + 10 : outerRing + 16, limit, rings, wallRadius);
    }

    /** Eine Ringstraße; wo alte Häuser im Weg stehen, setzt sie aus (dort führen die alten Dorfwege weiter). */
    private void ring(int radius, int width) {
        int count = Math.max(24, (int) (Mth.TWO_PI * radius / 3));
        int[] xs = new int[count];
        int[] zs = new int[count];
        boolean[] blocked = new boolean[count];
        for (int i = 0; i < count; i++) {
            double angle = i * Mth.TWO_PI / count;
            xs[i] = (int) Math.round(cx + Math.cos(angle) * radius);
            zs[i] = (int) Math.round(cz + Math.sin(angle) * radius);
            blocked[i] = !free(built, xs[i] - width / 2 - 1, zs[i] - width / 2 - 1, xs[i] + width / 2 + 1, zs[i] + width / 2 + 1, false);
        }
        int first = -1;
        for (int i = 0; i < count; i++) {
            if (blocked[i]) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            road(xs, zs, true, width);
            return;
        }
        // Freie Bögen zwischen den Hindernissen, jeder ein eigenes Straßenstück
        List<int[]> run = new ArrayList<>();
        for (int n = 1; n <= count; n++) {
            int i = (first + n) % count;
            if (!blocked[i]) {
                run.add(new int[] {xs[i], zs[i]});
            }
            if ((blocked[i] || n == count) && run.size() >= 2) {
                road(run.stream().mapToInt(p -> p[0]).toArray(), run.stream().mapToInt(p -> p[1]).toArray(), false, width);
            }
            if (blocked[i]) {
                run.clear();
            }
        }
    }

    private void road(int[] xs, int[] zs, boolean closed, int width) {
        roads.add(new RoadPiece(xs, zs, closed, width, groundY - 40, groundY + 50, seed ^ (xs[0] * 31L + zs[0]), style));
        int segments = closed ? xs.length : xs.length - 1;
        for (int i = 0; i < segments; i++) {
            int j = (i + 1) % xs.length;
            double length = distance(xs[i], zs[i], xs[j], zs[j]);
            for (double t = 0; t <= length; t += 0.5) {
                double x = xs[i] + (xs[j] - xs[i]) * t / Math.max(length, 1e-6);
                double z = zs[i] + (zs[j] - zs[i]) * t / Math.max(length, 1e-6);
                int r = width / 2 + 1;
                mark(occupied, (int) Math.floor(x) - r, (int) Math.floor(z) - r, (int) Math.floor(x) + r, (int) Math.floor(z) + r);
                mark(paved, (int) Math.floor(x) - width / 2, (int) Math.floor(z) - width / 2, (int) Math.floor(x) + width / 2,
                    (int) Math.floor(z) + width / 2);
            }
        }
    }

    /** Ob eine gerade Straße an keinem Haus vorbei muss. */
    private boolean clear(int[] from, int[] to, int width) {
        double length = distance(from[0], from[1], to[0], to[1]);
        for (double t = 0; t <= length; t += 1.0) {
            int x = (int) Math.round(from[0] + (to[0] - from[0]) * t / Math.max(length, 1e-6));
            int z = (int) Math.round(from[1] + (to[1] - from[1]) * t / Math.max(length, 1e-6));
            if (!free(built, x - width / 2, z - width / 2, x + width / 2, z + width / 2, false)) {
                return false;
            }
        }
        return true;
    }

    /** Vom inneren Ende einer Hauptstraße zum nächsten alten Dorfweg – wenn kein Haus im Weg steht. */
    private void connect(int[] from, int width) {
        BoundingBox best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BoundingBox street : streets) {
            int x = Mth.clamp(from[0], street.minX(), street.maxX());
            int z = Mth.clamp(from[1], street.minZ(), street.maxZ());
            double d = distance(from[0], from[1], x, z);
            if (d < bestDistance) {
                bestDistance = d;
                best = street;
            }
        }
        if (best == null || bestDistance > 40) {
            return;
        }
        int tx = Mth.clamp(from[0], best.minX(), best.maxX());
        int tz = Mth.clamp(from[1], best.minZ(), best.maxZ());
        double length = distance(from[0], from[1], tx, tz);
        for (double t = 0; t <= length; t += 1.0) {
            int x = (int) Math.round(from[0] + (tx - from[0]) * t / Math.max(length, 1e-6));
            int z = (int) Math.round(from[1] + (tz - from[1]) * t / Math.max(length, 1e-6));
            if (taken(built, x, z)) {
                return;
            }
        }
        road(new int[] {from[0], tx}, new int[] {from[1], tz}, false, Math.max(1, width - 2));
    }

    private boolean plaza(float angle, int radius) {
        int size = stage >= 4 ? 17 : 13;
        int[] center = point(angle, radius);
        int x0 = center[0] - size / 2;
        int z0 = center[1] - size / 2;
        int x1 = x0 + size - 1;
        int z1 = z0 + size - 1;
        int[] spread = spread(x0, z0, x1, z1);
        if (spread == null || spread[1] - spread[0] > 4 || !free(built, x0, z0, x1, z1, true)) {
            return false;
        }
        mark(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        mark(built, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        mark(paved, x0, z0, x1, z1);
        town.add(new PlazaPiece(x0, median(x0, z0, x1, z1) - 1, z0, size, seed ^ 0x9A2AL, style));
        return true;
    }

    /**
     * Grundstücke entlang der Ringe, abwechselnd außen und innen, mit der Tür zur Straße. Besondere Häuser
     * bekommen die ersten Plätze, die groß genug sind.
     */
    private void lots(List<Integer> rings, float[] avenues, int roadWidth, int wallRadius, double limit, List<Kind> specials, int homes) {
        int[] counter = {0};
        // Erst die besonderen Häuser – sie brauchen große Grundstücke –, dann Wohnhäuser und Werkstätten
        for (Kind special : specials) {
            place(rings, avenues, roadWidth, wallRadius, limit, () -> special, 1, counter);
        }
        int[] made = {0};
        place(rings, avenues, roadWidth, wallRadius, limit, () -> made[0] % 3 == 2 ? Kind.WORKSHOP : Kind.HOUSE, homes, counter, made);
        infill(rings, wallRadius, limit, homes - made[0], counter, made);
    }

    /**
     * Nachverdichtung: Was zwischen Ringen und Hauptstraßen noch frei ist, wird bebaut – jedes Haus mit der Tür
     * zur nächsten Straße.
     */
    private void infill(List<Integer> rings, int wallRadius, double limit, int wanted, int[] counter, int[] made) {
        int outer = wallRadius > 0 ? wallRadius - 6 : rings.get(rings.size() - 1) + 16;
        int placed = 0;
        for (int radius = r0 + 6; radius <= outer && placed < wanted; radius += 4) {
            double step = 6.0 / radius;
            for (double angle = 0; angle < Mth.TWO_PI && placed < wanted; angle += step) {
                int[] center = point(angle, radius);
                Direction facing = towardRoad(center[0], center[1]);
                if (facing == null) {
                    continue;
                }
                Kind kind = made[0] % 3 == 2 ? Kind.WORKSHOP : Kind.HOUSE;
                if (lotFacing(kind, center[0], center[1], facing, wallRadius, counter[0])) {
                    placed++;
                    made[0]++;
                    counter[0]++;
                }
            }
        }
    }

    /** Die Himmelsrichtung von einem Punkt zu einem anderen (auf die nähere Achse gerundet). */
    private static Direction facingToward(int[] from, int[] to) {
        int dx = to[0] - from[0];
        int dz = to[1] - from[1];
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** In welcher Himmelsrichtung die nächste Straße liegt (höchstens 16 Blöcke weit). */
    private Direction towardRoad(int x, int z) {
        Direction best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int step = 6; step <= 16 && step < bestDistance; step++) {
                if (taken(paved, x + direction.getStepX() * step, z + direction.getStepZ() * step)) {
                    best = direction;
                    bestDistance = step;
                    break;
                }
            }
        }
        return best;
    }

    /** Ein Grundstück mit fester Mitte und Blickrichtung; {@code false}, wenn es hier nicht passt. */
    private boolean lotFacing(Kind kind, int centerX, int centerZ, Direction facing, int wallRadius, int counter) {
        int[] size = size(kind, counter);
        int lotWidth = BuildingPiece.lotWidth(size[0]);
        int lotDepth = BuildingPiece.lotDepth(size[1]);
        int sizeX = facing.getAxis() == Direction.Axis.Z ? lotWidth : lotDepth;
        int sizeZ = facing.getAxis() == Direction.Axis.Z ? lotDepth : lotWidth;
        int x0 = centerX - sizeX / 2;
        int z0 = centerZ - sizeZ / 2;
        int x1 = x0 + sizeX - 1;
        int z1 = z0 + sizeZ - 1;
        if (!reachable(x0, z0, x1, z1) || !free(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1, true)) {
            return reject("verdichten: belegt");
        }
        for (int[] corner : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            if (wallRadius > 0 && distance(corner[0], corner[1], cx, cz) > wallRadius - 3) {
                return reject("verdichten: Mauer");
            }
        }
        int[] spread = spread(x0, z0, x1, z1);
        if (spread == null) {
            return reject("verdichten: Wasser");
        }
        if (spread[1] - spread[0] > (stage >= 2 ? 8 : 6)) {
            return reject("verdichten: Hang");
        }
        int floorY = median(x0, z0, x1, z1) - 1;
        mark(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        mark(built, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        long pieceSeed = seed ^ (x0 * 0x9E3779B97F4A7C15L) ^ (z0 * 0xC2B2AE3D27D4EB4FL);
        buildings.add(new BuildingPiece(kind, new BlockPos(x0, floorY, z0), floorY, facing, size[0], size[1], size[2], pieceSeed, style));
        doorPath(x0, z0, x1, z1, facing);
        return true;
    }

    private void place(List<Integer> rings, float[] avenues, int roadWidth, int wallRadius, double limit, java.util.function.Supplier<Kind> kind,
                       int wanted, int[] counter) {
        place(rings, avenues, roadWidth, wallRadius, limit, kind, wanted, counter, new int[] {0});
    }

    /** Geht die Plätze entlang der Ringe ab (abwechselnd außen und innen) und bebaut, bis {@code wanted} stehen. */
    private void place(List<Integer> rings, float[] avenues, int roadWidth, int wallRadius, double limit, java.util.function.Supplier<Kind> kind,
                       int wanted, int[] counter, int[] made) {
        int placed = 0;
        for (int k = 0; k < rings.size(); k++) {
            int radius = rings.get(k);
            double step = 12.0 / radius;
            double start = avenues[0] + step / 2;
            for (double angle = start; angle < start + Mth.TWO_PI - step / 2; angle += step) {
                if (nearAvenue(angle, avenues, radius, roadWidth)) {
                    continue;
                }
                for (int side : k % 2 == 0 ? new int[] {1, -1} : new int[] {-1, 1}) {
                    if (placed >= wanted) {
                        return;
                    }
                    if (lot(kind.get(), radius, side, angle, roadWidth, wallRadius, limit, counter[0])) {
                        placed++;
                        made[0]++;
                        counter[0]++;
                    }
                }
            }
        }
    }

    private boolean nearAvenue(double angle, float[] avenues, int radius, int roadWidth) {
        for (float avenue : avenues) {
            double gap = Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - avenue))) * Mth.DEG_TO_RAD * radius;
            if (gap < roadWidth / 2.0 + 2) {
                return true;
            }
        }
        return false;
    }

    /** Ein Grundstück prüfen und bebauen; {@code false}, wenn es hier nicht passt. */
    private boolean lot(Kind kind, int radius, int side, double angle, int roadWidth, int wallRadius, double limit, int counter) {
        int[] size = size(kind, counter);
        int width = size[0];
        int depth = size[1];
        int lotWidth = BuildingPiece.lotWidth(width);
        int lotDepth = BuildingPiece.lotDepth(depth);
        // Die Tür schaut zur Straße: außen nach innen, innen nach außen – auf die nächste Himmelsrichtung gerundet
        double vx = -side * Math.cos(angle);
        double vz = -side * Math.sin(angle);
        Direction facing = Math.abs(vx) > Math.abs(vz) ? (vx > 0 ? Direction.EAST : Direction.WEST) : (vz > 0 ? Direction.SOUTH : Direction.NORTH);
        int sizeX = facing.getAxis() == Direction.Axis.Z ? lotWidth : lotDepth;
        int sizeZ = facing.getAxis() == Direction.Axis.Z ? lotDepth : lotWidth;
        // So nah an die Straße wie möglich: schräg zur Straße stehende Grundstücke rücken etwas ab
        int x0 = 0;
        int z0 = 0;
        boolean fits = false;
        for (int extra = 0; extra <= 5 && !fits; extra++) {
            double along = radius + side * (roadWidth / 2.0 + 1.5 + lotDepth / 2.0 + extra);
            x0 = (int) Math.round(cx + Math.cos(angle) * along - sizeX / 2.0);
            z0 = (int) Math.round(cz + Math.sin(angle) * along - sizeZ / 2.0);
            fits = free(occupied, x0 - 1, z0 - 1, x0 + sizeX, z0 + sizeZ, true);
        }
        int x1 = x0 + sizeX - 1;
        int z1 = z0 + sizeZ - 1;
        if (!fits) {
            return reject("belegt");
        }
        for (int[] corner : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            if (distance(corner[0], corner[1], sx, sz) > REACH || (wallRadius > 0 && distance(corner[0], corner[1], cx, cz) > wallRadius - 3)) {
                return reject("Mauer/Reichweite");
            }
        }
        // Hänge sind erlaubt: Fundament und Geländeanpassung (wie bei den Dorfhäusern) gleichen bis zu acht Blöcke aus
        int[] spread = spread(x0, z0, x1, z1);
        if (spread == null) {
            return reject("Wasser");
        }
        if (spread[1] - spread[0] > (stage >= 2 ? 8 : 6)) {
            return reject("Hang");
        }
        int floorY = median(x0, z0, x1, z1) - 1;
        mark(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        mark(built, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        long pieceSeed = seed ^ (x0 * 0x9E3779B97F4A7C15L) ^ (z0 * 0xC2B2AE3D27D4EB4FL);
        buildings.add(new BuildingPiece(kind, new BlockPos(x0, floorY, z0), floorY, facing, width, depth, size[2], pieceSeed, style));
        doorPath(x0, z0, x1, z1, facing);
        return true;
    }

    /** Vom Vorgarten geradeaus bis zur nächsten Straße. */
    private void doorPath(int x0, int z0, int x1, int z1, Direction facing) {
        int x = switch (facing) {
            case EAST -> x1 + 1;
            case WEST -> x0 - 1;
            default -> (x0 + x1) / 2;
        };
        int z = switch (facing) {
            case SOUTH -> z1 + 1;
            case NORTH -> z0 - 1;
            default -> (z0 + z1) / 2;
        };
        for (int step = 0; step <= 16; step++) {
            int px = x + facing.getStepX() * step;
            int pz = z + facing.getStepZ() * step;
            if (taken(paved, px, pz)) {
                if (step > 0) {
                    road(new int[] {x, px}, new int[] {z, pz}, false, 1);
                }
                return;
            }
        }
    }

    /** Breite, Tiefe, Stockwerke. */
    private int[] size(Kind kind, int counter) {
        boolean alt = ((seed >>> (counter % 48)) & 1) == 1;
        return switch (kind) {
            case CHURCH -> stage >= 4 ? new int[] {11, 17, 2} : new int[] {9, 15, 2};
            case TAVERN -> new int[] {11, 11, 2};
            case LIBRARY, TOWN_HALL -> new int[] {13, 13, 2};
            case CLOCK_TOWER -> new int[] {7, 7, 1};
            case MILL -> new int[] {7, 7, 2};
            default -> switch (stage) {
                case 1 -> new int[] {7, alt ? 9 : 7, 1};
                case 2 -> new int[] {7, 9, alt ? 2 : 1};
                case 3 -> new int[] {7, 9, 2};
                // Stadthäuser: schmal, dafür drei Stockwerke
                default -> alt ? new int[] {7, 9, 3} : new int[] {9, 9, 2};
            };
        };
    }

    private void fields(float[] avenues, int preferred, double limit, List<Integer> rings, int wallRadius) {
        int size = stage >= 3 ? 11 : 9;
        int wanted = 2 + stage;
        int mills = stage >= 3 ? 2 : 0;
        int[] radii = {preferred, preferred + 14, rings.get(0) + 13, wallRadius > 0 ? wallRadius - 9 : preferred - 8};
        for (int radius : radii) {
            for (int i = 0; i < avenues.length * 3 && wanted > 0; i++) {
                double angle = avenues[i % avenues.length] + Math.PI / avenues.length + (i / avenues.length - 1) * 0.22;
                int[] center = point(angle, radius);
                int x0 = center[0] - size / 2;
                int z0 = center[1] - size / 2;
                int x1 = x0 + size - 1;
                int z1 = z0 + size - 1;
                if (!reachable(x0, z0, x1, z1) || !free(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1, true)) {
                    continue;
                }
                int[] spread = spread(x0, z0, x1, z1);
                if (spread == null || spread[1] - spread[0] > 2) {
                    continue;
                }
                mark(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
                mark(built, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
                town.add(new FieldPiece(x0, median(x0, z0, x1, z1) - 1, z0, size, size, seed ^ (x0 * 31L + z0), style));
                wanted--;
                if (mills > 0) {
                    // Eine Windmühle gleich neben dem Feld, die Tür zur Stadt
                    if (lotAt(Kind.MILL, center[0] + (int) Math.signum(cx - center[0]) * (size / 2 + 6), center[1], limit)) {
                        mills--;
                    }
                }
            }
        }
    }

    private boolean lotAt(Kind kind, int centerX, int centerZ, double limit) {
        int[] size = size(kind, 0);
        int lotWidth = BuildingPiece.lotWidth(size[0]);
        int lotDepth = BuildingPiece.lotDepth(size[1]);
        double vx = cx - centerX;
        double vz = cz - centerZ;
        Direction facing = Math.abs(vx) > Math.abs(vz) ? (vx > 0 ? Direction.EAST : Direction.WEST) : (vz > 0 ? Direction.SOUTH : Direction.NORTH);
        int sizeX = facing.getAxis() == Direction.Axis.Z ? lotWidth : lotDepth;
        int sizeZ = facing.getAxis() == Direction.Axis.Z ? lotDepth : lotWidth;
        int x0 = centerX - sizeX / 2;
        int z0 = centerZ - sizeZ / 2;
        int x1 = x0 + sizeX - 1;
        int z1 = z0 + sizeZ - 1;
        if (!reachable(x0, z0, x1, z1) || !free(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1, true)) {
            return false;
        }
        int[] spread = spread(x0, z0, x1, z1);
        if (spread == null || spread[1] - spread[0] > 6) {
            return false;
        }
        int floorY = median(x0, z0, x1, z1) - 1;
        mark(occupied, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        mark(built, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
        buildings.add(new BuildingPiece(kind, new BlockPos(x0, floorY, z0), floorY, facing, size[0], size[1], size[2], seed ^ (x0 * 17L + z0),
            style));
        return true;
    }

    private boolean reject(String reason) {
        rejected.merge(reason, 1, Integer::sum);
        return false;
    }

    private boolean reachable(int x0, int z0, int x1, int z1) {
        for (int[] corner : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            if (distance(corner[0], corner[1], sx, sz) > REACH) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ Gelände

    /** Höhe der ersten Luft über dem Gelände und ob dort Wasser steht (gemerkt je Stelle). */
    private int[] ground(int x, int z) {
        long key = BlockPos.asLong(x, 0, z);
        int[] known = terrain.get(key);
        if (known == null) {
            int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
            known = new int[] {surface, floor < surface ? 1 : 0};
            terrain.put(key, known);
        }
        return known;
    }

    /** Niedrigste und höchste Stelle an Ecken und Mitte; {@code null} bei Wasser. */
    private int[] spread(int x0, int z0, int x1, int z1) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int[] p : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {(x0 + x1) / 2, (z0 + z1) / 2}}) {
            int[] ground = ground(p[0], p[1]);
            if (ground[1] == 1) {
                return null;
            }
            low = Math.min(low, ground[0]);
            high = Math.max(high, ground[0]);
        }
        return new int[] {low, high};
    }

    private int median(int x0, int z0, int x1, int z1) {
        int[] values = new int[5];
        int i = 0;
        for (int[] p : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {(x0 + x1) / 2, (z0 + z1) / 2}}) {
            values[i++] = ground(p[0], p[1])[0];
        }
        Arrays.sort(values);
        return values[2];
    }

    // ------------------------------------------------------------------ Belegung

    private int[] point(double angle, double radius) {
        return new int[] {(int) Math.round(cx + Math.cos(angle) * radius), (int) Math.round(cz + Math.sin(angle) * radius)};
    }

    private int index(int x, int z) {
        int gx = x - cx + GRID / 2;
        int gz = z - cz + GRID / 2;
        return gx < 0 || gz < 0 || gx >= GRID || gz >= GRID ? -1 : gz * GRID + gx;
    }

    private void mark(boolean[] grid, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int i = index(x, z);
                if (i >= 0) {
                    grid[i] = true;
                }
            }
        }
    }

    private boolean taken(boolean[] grid, int x, int z) {
        int i = index(x, z);
        return i >= 0 && grid[i];
    }

    /** Frei (und, falls verlangt, ganz im Planungsgebiet). */
    private boolean free(boolean[] grid, int x0, int z0, int x1, int z1, boolean inside) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int i = index(x, z);
                if (i < 0 ? inside : grid[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double distance(double x0, double z0, double x1, double z1) {
        return Math.sqrt((x0 - x1) * (x0 - x1) + (z0 - z1) * (z0 - z1));
    }
}

package com.fischgesichtert.timemod.future;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Die Natur holt sich alles zurück: Ranken wachsen an Mauern hinab, Moos legt sich auf Böden, in Ecken
 * hängen Spinnweben, in Wüsten weht Sand herein, auf offene Böden fällt Schnee, und durch eingestürzte
 * Häuser wachsen Bäume. Auf alten Feldern wird erst Wiese, dann Wald.
 */
public final class Reclaim {
    private static final long SALT = 0x2EC1_A1A1L;
    private static final int MAX_TREES = 3;

    /** Wie stark die Natur nach wie vielen Jahren zugreift (aus {@code data/timemod/weathering/reclaim.json}). */
    public record Settings(Curve vines, Curve moss, Curve cobwebs, Curve sand, Curve snow, Curve ruinTrees, Curve fieldTrees,
                           Curve signFade, Curve bones, Curve descendants, Curve forestTrees, Curve giantTrees, Curve sandbanks) {
        public static final Settings DEFAULT = new Settings(
            curve("10", 0.01, "50", 0.05, "250", 0.15, "1000", 0.3),
            curve("25", 0.02, "100", 0.08, "500", 0.2, "1000", 0.3),
            curve("2", 0.01, "25", 0.06, "250", 0.08, "1000", 0.05),
            curve("25", 0.02, "250", 0.15, "1000", 0.35),
            curve("1", 0.15, "100", 0.35, "1000", 0.6),
            curve("100", 0.0, "250", 0.004, "1000", 0.012),
            curve("250", 0.0, "500", 0.01, "1000", 0.03),
            curve("1", 0.02, "5", 0.08, "50", 0.45, "100", 0.75, "250", 1.0),
            curve("10", 0.0, "25", 0.8, "1000", 0.6),
            curve("10", 0.0, "25", 0.6, "1000", 0.8),
            curve("10", 0.0, "25", 0.006, "100", 0.02, "500", 0.035, "1000", 0.045),
            curve("50", 0.0, "100", 0.03, "250", 0.06, "500", 0.09, "1000", 0.12),
            curve("25", 0.0, "100", 0.25, "500", 0.5, "1000", 0.6));

        public static Settings parse(JsonObject json) {
            return new Settings(get(json, "vines", DEFAULT.vines), get(json, "moss", DEFAULT.moss), get(json, "cobwebs", DEFAULT.cobwebs),
                get(json, "sand", DEFAULT.sand), get(json, "snow", DEFAULT.snow), get(json, "ruin_trees", DEFAULT.ruinTrees),
                get(json, "field_trees", DEFAULT.fieldTrees), get(json, "sign_fade", DEFAULT.signFade), get(json, "bones", DEFAULT.bones),
                get(json, "descendants", DEFAULT.descendants), get(json, "forest_trees", DEFAULT.forestTrees),
                get(json, "giant_trees", DEFAULT.giantTrees), get(json, "sandbanks", DEFAULT.sandbanks));
        }

        private static Curve get(JsonObject json, String key, Curve fallback) {
            return json.has(key) ? Curve.parse(json.get(key)) : fallback;
        }

        private static Curve curve(Object... points) {
            JsonObject json = new JsonObject();
            for (int i = 0; i < points.length; i += 2) {
                json.addProperty((String) points[i], (Double) points[i + 1]);
            }
            return Curve.parse(json);
        }
    }

    private Reclaim() {
    }

    /** Nur im Höhenband {@code low..high}: Natur wächst dort, wo gebaut wurde. */
    static void apply(Canvas canvas, long years, WeatheringRules.Resolved rules, LongList fields, int low, int high) {
        Settings settings = rules.reclaim();
        ChunkPos chunk = canvas.chunk();
        int[] top = surface(canvas);
        double vines = settings.vines().at(years);
        double moss = settings.moss().at(years);
        double cobwebs = settings.cobwebs().at(years);
        double sand = settings.sand().at(years);
        double snow = settings.snow().at(years);
        double ruinTrees = settings.ruinTrees().at(years);
        int trees = 0;
        for (int y = Math.max(canvas.minY() + 1, low); y <= Math.min(canvas.maxY() - 2, high); y++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                    if (!canvas.at(x, y, z).isAir()) {
                        continue;
                    }
                    BlockState below = canvas.at(x, y - 1, z);
                    BlockState above = canvas.at(x, y + 1, z);
                    boolean madeBelow = rules.isMade(below) && full(below);
                    boolean madeAbove = rules.isMade(above) && full(above);
                    Direction wall = null;
                    int walls = 0;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockState side = canvas.get(x + direction.getStepX(), y, z + direction.getStepZ());
                        if (side != null && rules.isMade(side) && full(side)) {
                            walls++;
                            wall = wall == null ? direction : wall;
                        }
                    }
                    if (!madeBelow && !madeAbove && walls == 0) {
                        continue;
                    }
                    RandomSource random = Weathering.random(canvas.seed(), SALT, x, y, z);
                    float treeRoll = random.nextFloat();
                    float mossRoll = random.nextFloat();
                    float coverRoll = random.nextFloat();
                    float webRoll = random.nextFloat();
                    float vineRoll = random.nextFloat();
                    boolean open = y > top[(z & 15) * 16 + (x & 15)];
                    Holder<Biome> biome = canvas.biome(x, y, z);
                    if (madeBelow && open && trees < MAX_TREES && treeRoll < ruinTrees && interior(canvas, x, z) && ruin(canvas, rules, x, y, z)) {
                        if (tree(canvas, x, y, z, random, biome, true)) {
                            trees++;
                            continue;
                        }
                    }
                    if (madeBelow && open && dusty(biome) && coverRoll < sand) {
                        canvas.set(x, y, z, biome.is(BiomeTags.IS_BADLANDS) ? Blocks.RED_SAND.defaultBlockState() : Blocks.SAND.defaultBlockState());
                        continue;
                    }
                    if (madeBelow && open && biome.value().coldEnoughToSnow(new BlockPos(x, y, z)) && coverRoll < snow) {
                        int layers = 1 + random.nextInt(years >= 250 ? 4 : 2);
                        canvas.set(x, y, z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers));
                        continue;
                    }
                    if (madeBelow && mossRoll < moss) {
                        canvas.set(x, y, z, Blocks.MOSS_CARPET.defaultBlockState());
                        continue;
                    }
                    if (madeAbove && !open && walls >= 2 && webRoll < cobwebs) {
                        canvas.set(x, y, z, Blocks.COBWEB.defaultBlockState());
                        continue;
                    }
                    if (wall != null && vineRoll < vines) {
                        hangVine(canvas, x, y, z, wall, 1 + random.nextInt(years >= 250 ? 6 : 3));
                    }
                }
            }
        }
        coverFields(canvas, years, rules, fields, settings, trees);
    }

    /** Was einst Feld war: Gras, Blumen, später junge Bäume. */
    private static void coverFields(Canvas canvas, long years, WeatheringRules.Resolved rules, LongList fields, Settings settings, int trees) {
        double fieldTrees = settings.fieldTrees().at(years);
        for (int i = 0; i < fields.size(); i++) {
            BlockPos pos = BlockPos.of(fields.getLong(i));
            BlockState ground = canvas.at(pos.getX(), pos.getY(), pos.getZ());
            WeatheringRule rule = rules.of(ground);
            if (!canvas.at(pos.getX(), pos.getY() + 1, pos.getZ()).isAir()) {
                continue;
            }
            RandomSource random = Weathering.random(canvas.seed(), SALT ^ 0xF1E1DL, pos.getX(), pos.getY(), pos.getZ());
            if (random.nextFloat() < fieldTrees && trees < MAX_TREES * 2 && interior(canvas, pos.getX(), pos.getZ())
                && tree(canvas, pos.getX(), pos.getY() + 1, pos.getZ(), random, canvas.biome(pos.getX(), pos.getY(), pos.getZ()), false)) {
                trees++;
                continue;
            }
            if (rule == null) {
                rule = rules.of(Blocks.FARMLAND.defaultBlockState());
            }
            if (rule == null) {
                continue;
            }
            for (WeatheringRule.Cover cover : rule.cover()) {
                if (random.nextFloat() < cover.chance().at(years)) {
                    place(canvas, pos.getX(), pos.getY() + 1, pos.getZ(), cover.block());
                    break;
                }
            }
        }
    }

    private static void place(Canvas canvas, int x, int y, int z, BlockState state) {
        if (state.getBlock() instanceof DoublePlantBlock) {
            if (canvas.at(x, y + 1, z).isAir()) {
                canvas.set(x, y, z, state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
                canvas.set(x, y + 1, z, state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
            }
            return;
        }
        canvas.set(x, y, z, state);
    }

    private static void hangVine(Canvas canvas, int x, int y, int z, Direction wall, int length) {
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(wall), true);
        for (int i = 0; i < length; i++) {
            int vy = y - i;
            if (!canvas.at(x, vy, z).isAir()) {
                return;
            }
            BlockState behind = canvas.at(x + wall.getStepX(), vy, z + wall.getStepZ());
            if (!full(behind)) {
                return;
            }
            canvas.set(x, vy, z, vine);
        }
    }

    /** Ein Baum, der durch eine Ruine oder auf einem alten Feld wächst; die Wurzeln brechen durch den Boden. */
    private static boolean tree(Canvas canvas, int x, int y, int z, RandomSource random, Holder<Biome> biome, boolean breakFloor) {
        int height = 4 + random.nextInt(3);
        for (int i = 0; i < height + 1; i++) {
            BlockState state = canvas.at(x, y + i, z);
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        Block[] wood = wood(biome);
        if (breakFloor) {
            canvas.set(x, y - 1, z, Blocks.ROOTED_DIRT.defaultBlockState());
        }
        BlockState log = wood[0].defaultBlockState();
        if (log.hasProperty(RotatedPillarBlock.AXIS)) {
            log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        for (int i = 0; i < height; i++) {
            canvas.set(x, y + i, z, log);
        }
        BlockState leaves = wood[1].defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int crown = y + height - 1;
        for (int dy = -2; dy <= 1; dy++) {
            int radius = dy >= 0 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (corner && (dy == 1 || random.nextBoolean())) {
                        continue;
                    }
                    int lx = x + dx;
                    int lz = z + dz;
                    if (!canvas.inside(lx, lz)) {
                        continue;
                    }
                    BlockState current = canvas.at(lx, crown + dy, lz);
                    if (current.isAir() || current.canBeReplaced()) {
                        canvas.set(lx, crown + dy, lz, leaves);
                    }
                }
            }
        }
        return true;
    }

    private static Block[] wood(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_TAIGA) || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.GROVE)) {
            return new Block[] {Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES};
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return new Block[] {Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES};
        }
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return new Block[] {Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES};
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return new Block[] {Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES};
        }
        return new Block[] {Blocks.OAK_LOG, Blocks.OAK_LEAVES};
    }

    private static boolean dusty(Holder<Biome> biome) {
        return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_BEACH);
    }

    /** Drinnen in einer Ruine: in mindestens drei Richtungen steht in der Nähe noch Mauerwerk. */
    private static boolean ruin(Canvas canvas, WeatheringRules.Resolved rules, int x, int y, int z) {
        int sides = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int step = 1; step <= 6; step++) {
                BlockState state = canvas.get(x + direction.getStepX() * step, y, z + direction.getStepZ() * step);
                if (state != null && rules.isMade(state)) {
                    sides++;
                    break;
                }
            }
        }
        return sides >= 3;
    }

    /** Weit genug vom Chunkrand, dass die Krone im Chunk bleibt. */
    private static boolean interior(Canvas canvas, int x, int z) {
        int lx = x & 15;
        int lz = z & 15;
        return lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13;
    }

    private static boolean full(BlockState state) {
        return !state.isAir() && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** Höchster fester Block je Spalte: Wer darüber steht, sieht den Himmel. */
    private static int[] surface(Canvas canvas) {
        ChunkPos chunk = canvas.chunk();
        int[] top = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int y = canvas.maxY() - 1;
                while (y > canvas.minY()) {
                    BlockState state = canvas.at(chunk.getMinBlockX() + x, y, chunk.getMinBlockZ() + z);
                    if (!state.isAir() && !state.canBeReplaced()) {
                        break;
                    }
                    y--;
                }
                top[z * 16 + x] = y;
            }
        }
        return top;
    }
}

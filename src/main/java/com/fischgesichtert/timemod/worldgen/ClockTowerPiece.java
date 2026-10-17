package com.fischgesichtert.timemod.worldgen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Verfallener Uhrturm: Steinziegelturm mit vier Zifferblättern, stehengebliebenem Uhrwerk und
 * Glocke unter dem Dach. Ranken, Moos, Risse, eingebrochene Böden und Spinnweben – hier hat seit
 * Jahrhunderten niemand mehr die Uhr aufgezogen.
 */
public class ClockTowerPiece extends TimePiece {
    public static final ResourceLocation LOOT = TimeMod.id("chests/clock_tower");
    public static final ResourceLocation LOOT_TOP = TimeMod.id("chests/clock_tower_top");
    static final int SIZE = 11;
    static final int HEIGHT = 40;
    private static final int TOP = 31;
    private static final int[] FLOORS = {7, 14, 21};

    public ClockTowerPiece(BlockPos origin, Direction orientation, long seed) {
        super(ModStructures.CLOCK_TOWER_PIECE, makeBoundingBox(origin.getX(), origin.getY(), origin.getZ(), orientation, SIZE, HEIGHT, SIZE), orientation, seed);
    }

    public ClockTowerPiece(CompoundTag tag) {
        super(ModStructures.CLOCK_TOWER_PIECE, tag);
    }

    private BlockState wall(int x, int y, int z) {
        BlockState state = pick(x, y, z, 1, Blocks.STONE_BRICKS.defaultBlockState(),
            0.2F, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 0.13F, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        // Nach oben hin weniger Moos, mehr Risse
        if (y > 18 && state.is(Blocks.MOSSY_STONE_BRICKS)) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        return state;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // Sockel mit Fundament bis zum Boden
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                put(level, box, x, 0, z, pick(x, 0, z, 2, Blocks.STONE_BRICKS.defaultBlockState(),
                    0.3F, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 0.1F, Blocks.COBBLESTONE.defaultBlockState()));
                fillColumnDown(level, Blocks.COBBLESTONE.defaultBlockState(), x, -1, z, box);
            }
        }
        // Innenraum leeren, Mauern hochziehen
        fill(level, box, 2, 1, 2, 8, TOP, 8, AIR);
        for (int y = 1; y <= TOP; y++) {
            for (int i = 1; i <= 9; i++) {
                wallBlock(level, box, i, y, 1);
                wallBlock(level, box, i, y, 9);
                wallBlock(level, box, 1, y, i);
                wallBlock(level, box, 9, y, i);
            }
        }
        // Holzbalken-Ringe und Böden (mit Löchern)
        for (int floor : FLOORS) {
            for (int i = 1; i <= 9; i++) {
                BlockState beamX = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                BlockState beamZ = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                if (i != 1 && i != 9) {
                    put(level, box, i, floor, 1, beamX);
                    put(level, box, i, floor, 9, beamX);
                    put(level, box, 1, floor, i, beamZ);
                    put(level, box, 9, floor, i, beamZ);
                }
            }
            for (int x = 2; x <= 8; x++) {
                for (int z = 2; z <= 8; z++) {
                    boolean hole = floor != 21 && chance(x, floor, z, 3) < 0.16F;
                    boolean ladderShaft = x == 5 && z == 8;
                    put(level, box, x, floor, z, hole || ladderShaft ? AIR : Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
            }
        }
        // Leiter an der Rückwand bis in den Uhrenraum
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = 1; y <= 21; y++) {
            if (chance(5, y, 8, 4) > 0.08F || y < 3) {
                put(level, box, 5, y, 8, ladder);
            }
        }
        // Eingang (Tür verrottet)
        fill(level, box, 5, 1, 1, 5, 2, 1, AIR);
        // Fenster mit alten Gitterstäben
        for (int y : new int[] {3, 4, 10, 11, 17, 18}) {
            window(level, box, 5, y, 1, true);
            window(level, box, 5, y, 9, true);
            window(level, box, 1, y, 5, false);
            window(level, box, 9, y, 5, false);
        }
        clockFaces(level, box);
        clockwork(level, box);
        roof(level, box);
        decay(level, box);
        createChest(level, box, random, 2, 1, 2, LOOT);
        createChest(level, box, random, 7, 22, 3, LOOT_TOP);
    }

    private void wallBlock(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        boolean corner = (x == 1 || x == 9) && (z == 1 || z == 9);
        if (corner) {
            put(level, box, x, y, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            return;
        }
        // Löcher im Mauerwerk (nicht unten, nicht im Zifferblatt-Bereich)
        if (y > 3 && y < 22 && chance(x, y, z, 5) < 0.035F) {
            put(level, box, x, y, z, AIR);
            return;
        }
        put(level, box, x, y, z, wall(x, y, z));
    }

    private void window(WorldGenLevel level, BoundingBox box, int x, int y, int z, boolean alongX) {
        if (chance(x, y, z, 6) < 0.35F) {
            put(level, box, x, y, z, AIR);
            return;
        }
        BlockState bars = Blocks.IRON_BARS.defaultBlockState()
            .setValue(alongX ? IronBarsBlock.EAST : IronBarsBlock.NORTH, true)
            .setValue(alongX ? IronBarsBlock.WEST : IronBarsBlock.SOUTH, true);
        put(level, box, x, y, z, bars);
    }

    /** Vier Zifferblätter (Quarz, Messingrand, Zeiger aus Schwarzstein) – alle zeigen 4:50. */
    private void clockFaces(WorldGenLevel level, BoundingBox box) {
        int cy = 26;
        for (int dy = -3; dy <= 3; dy++) {
            for (int d = -3; d <= 3; d++) {
                double r = Math.sqrt(d * d + dy * dy);
                BlockState state;
                if (r <= 2.6) {
                    state = chance(d, cy + dy, 0, 7) < 0.08F ? Blocks.CALCITE.defaultBlockState() : Blocks.QUARTZ_BLOCK.defaultBlockState();
                } else if (r <= 3.5) {
                    state = ModBlocks.BRASS_BLOCK.defaultBlockState();
                } else {
                    continue;
                }
                put(level, box, 5 + d, cy + dy, 1, state);
                put(level, box, 5 - d, cy + dy, 9, state);
                put(level, box, 1, cy + dy, 5 - d, state);
                put(level, box, 9, cy + dy, 5 + d, state);
            }
        }
        BlockState hand = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        int[][] hands = {{0, 1}, {0, 2}, {-1, -1}, {-2, -1}};
        for (int[] h : hands) {
            put(level, box, 5 + h[0], cy + h[1], 1, hand);
            put(level, box, 5 - h[0], cy + h[1], 9, hand);
            put(level, box, 1, cy + h[1], 5 - h[0], hand);
            put(level, box, 9, cy + h[1], 5 + h[0], hand);
        }
        put(level, box, 5, cy, 1, ModBlocks.BRASS_BLOCK.defaultBlockState());
        put(level, box, 5, cy, 9, ModBlocks.BRASS_BLOCK.defaultBlockState());
        put(level, box, 1, cy, 5, ModBlocks.BRASS_BLOCK.defaultBlockState());
        put(level, box, 9, cy, 5, ModBlocks.BRASS_BLOCK.defaultBlockState());
    }

    /** Stehengebliebenes Uhrwerk: ein großes Messingrad mit Achse, Ketten und die Glocke. */
    private void clockwork(WorldGenLevel level, BoundingBox box) {
        BlockState brass = ModBlocks.BRASS_BLOCK.defaultBlockState();
        int cx = 5;
        int cy = 26;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                double r = Math.sqrt(dx * dx + dy * dy);
                double a = Math.atan2(dy, dx);
                boolean tooth = Math.cos(a * 8) > 0.3;
                if ((r <= 2.2 && r >= 1.2) || (r > 2.2 && r <= 3.2 && tooth)) {
                    put(level, box, cx + dx, cy + dy, 5, brass);
                }
            }
        }
        BlockState axle = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        for (int z = 2; z <= 8; z++) {
            put(level, box, cx, cy, z, z == 5 ? brass : axle);
        }
        for (int y = 23; y <= 29; y++) {
            put(level, box, 3, y, 7, Blocks.CHAIN.defaultBlockState());
            if (y >= 25) {
                put(level, box, 7, y, 3, Blocks.CHAIN.defaultBlockState());
            }
        }
        put(level, box, 5, TOP - 1, 3, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        // Decke des Uhrenraums
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                put(level, box, x, TOP, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
        }
    }

    /** Pyramidendach aus Tiefenschiefer-Fliesen mit Messingspitze; ein paar Ziegel fehlen. */
    private void roof(WorldGenLevel level, BoundingBox box) {
        for (int k = 0; k <= 5; k++) {
            int y = TOP + 1 + k;
            int lo = k;
            int hi = SIZE - 1 - k;
            if (lo > hi) {
                break;
            }
            for (int x = lo; x <= hi; x++) {
                for (int z = lo; z <= hi; z++) {
                    boolean edge = x == lo || x == hi || z == lo || z == hi;
                    if (!edge) {
                        continue;
                    }
                    if (k > 0 && chance(x, y, z, 8) < 0.06F) {
                        continue;
                    }
                    Direction facing;
                    if (z == lo) {
                        facing = Direction.SOUTH;
                    } else if (z == hi) {
                        facing = Direction.NORTH;
                    } else if (x == lo) {
                        facing = Direction.EAST;
                    } else {
                        facing = Direction.WEST;
                    }
                    boolean corner = (x == lo || x == hi) && (z == lo || z == hi);
                    put(level, box, x, y, z, corner ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                        : Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
                }
            }
        }
        put(level, box, 5, TOP + 6, 5, ModBlocks.BRASS_BLOCK.defaultBlockState());
        put(level, box, 5, TOP + 7, 5, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    /** Ranken außen, Spinnweben und Schutt innen. */
    private void decay(WorldGenLevel level, BoundingBox box) {
        for (int i = 1; i <= 9; i++) {
            vineColumn(level, box, i, 0, VineBlock.SOUTH);
            vineColumn(level, box, i, 10, VineBlock.NORTH);
            vineColumn(level, box, 0, i, VineBlock.EAST);
            vineColumn(level, box, 10, i, VineBlock.WEST);
        }
        for (int y = 2; y <= TOP - 1; y++) {
            for (int[] c : new int[][] {{2, 2}, {8, 2}, {2, 8}, {8, 8}, {2, 5}, {8, 5}}) {
                if (chance(c[0], y, c[1], 9) < 0.12F && isAir(level, box, c[0], y, c[1])) {
                    put(level, box, c[0], y, c[1], Blocks.COBWEB.defaultBlockState());
                }
            }
        }
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                if (chance(x, 1, z, 10) < 0.1F && !(x == 5 && z == 8)) {
                    put(level, box, x, 1, z, Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
                }
            }
        }
    }

    private void vineColumn(WorldGenLevel level, BoundingBox box, int x, int z, net.minecraft.world.level.block.state.properties.BooleanProperty side) {
        if (chance(x, 0, z, 11) > 0.45F) {
            return;
        }
        int top = 6 + (int) (chance(x, 1, z, 12) * 16);
        int length = 3 + (int) (chance(x, 2, z, 13) * 10);
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(side, true);
        for (int y = top; y > top - length && y >= 1; y--) {
            if (isAir(level, box, x, y, z)) {
                put(level, box, x, y, z, vine);
            }
        }
    }
}

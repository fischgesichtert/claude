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
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Die Zeitlose Zitadelle: eine schwebende Insel im äußeren End mit einem Platz wie ein Zifferblatt,
 * einem Turm aus Purpur und Zeitglas (innen eine Wendeltreppe), vier Kristalltürmchen, einem
 * erloschenen Tor aus Messing – und oben die Arena mit dem Spiegel-Altar, an dem Paradox erwacht.
 */
public class TimelessCitadelPiece extends TimePiece {
    public static final ResourceLocation LOOT = TimeMod.id("chests/timeless_citadel");
    static final int SIZE = 41;
    static final int HEIGHT = 52;
    private static final int C = 20;
    private static final int PLAZA = 10;
    private static final int ARENA = 45;
    private static final double TOWER_R = 5.5;
    /** Die letzten beiden Stufen liegen schon in Höhe des Arena-Bodens. */
    private static final int STAIR_STEPS = 70;
    /** Über diesen Stufen ist der Arena-Boden offen (Kopffreiheit). */
    private static final int OPENING_FROM = 62;
    private static final double STAIR_STEP_ANGLE = Math.PI * 2 / 24;

    public TimelessCitadelPiece(BlockPos origin, Direction orientation, long seed) {
        super(ModStructures.TIMELESS_CITADEL_PIECE, makeBoundingBox(origin.getX(), origin.getY(), origin.getZ(), orientation, SIZE, HEIGHT, SIZE),
            orientation, seed);
    }

    public TimelessCitadelPiece(CompoundTag tag) {
        super(ModStructures.TIMELESS_CITADEL_PIECE, tag);
    }

    private static double dist(int x, int z) {
        return Math.hypot(x - C, z - C);
    }

    /** Winkel um die Mitte in [0, 2π). */
    private static double angle(int x, int z) {
        double a = Math.atan2(z - C, x - C);
        return a < 0 ? a + Math.PI * 2 : a;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        island(level, box);
        plaza(level, box);
        tower(level, box);
        spires(level, box);
        gate(level, box);
        arena(level, box);
        stairs(level, box);
        createChest(level, box, random, C - 3, PLAZA + 1, C + 1, LOOT);
        createChest(level, box, random, C + 3, PLAZA + 1, C - 1, LOOT);
    }

    /** Schwebende Insel: ein umgedrehter Kegel aus Endstein mit Amethyst- und Obsidianadern. */
    private void island(WorldGenLevel level, BoundingBox box) {
        for (int y = 0; y < PLAZA; y++) {
            double radius = 3.0 + 16.5 * Math.pow(y / (double) PLAZA, 0.6);
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    double d = dist(x, z) + (chance(x, 0, z, 1) - 0.5) * 3.0;
                    if (d > radius) {
                        continue;
                    }
                    BlockState stone = pick(x, y, z, 2, Blocks.END_STONE.defaultBlockState(), 0.06F, Blocks.AMETHYST_BLOCK.defaultBlockState(),
                        0.03F, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    put(level, box, x, y, z, stone);
                }
            }
        }
    }

    /** Der Platz: ein Zifferblatt aus Endsteinziegeln, Purpur-Ringe, zwölf Quarz-Striche und ein Geländer. */
    private void plaza(WorldGenLevel level, BoundingBox box) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = dist(x, z);
                if (d > 19.5) {
                    continue;
                }
                BlockState floor = Blocks.END_STONE_BRICKS.defaultBlockState();
                if ((d >= 7.5 && d < 8.5) || (d >= 15.5 && d < 16.5)) {
                    floor = Blocks.PURPUR_BLOCK.defaultBlockState();
                }
                double hour = angle(x, z) / (Math.PI / 6);
                if (d >= 17 && d < 19 && Math.abs(hour - Math.round(hour)) < 0.12) {
                    floor = Blocks.QUARTZ_BLOCK.defaultBlockState();
                }
                put(level, box, x, PLAZA, z, floor);
                for (int y = PLAZA + 1; y < ARENA; y++) {
                    put(level, box, x, y, z, AIR);
                }
                if (d >= 18.8) {
                    boolean post = Math.abs(hour - Math.round(hour)) < 0.1;
                    put(level, box, x, PLAZA + 1, z, post ? Blocks.PURPUR_PILLAR.defaultBlockState() : Blocks.END_STONE_BRICK_WALL.defaultBlockState());
                    if (post) {
                        put(level, box, x, PLAZA + 2, z, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
                    }
                }
            }
        }
    }

    /** Der Turm: Purpur mit Säulenringen, Fenster aus Zeitglas, Tor nach Süden. */
    private void tower(WorldGenLevel level, BoundingBox box) {
        for (int x = C - 6; x <= C + 6; x++) {
            for (int z = C - 6; z <= C + 6; z++) {
                double d = dist(x, z);
                if (d > TOWER_R) {
                    continue;
                }
                for (int y = PLAZA + 1; y < ARENA; y++) {
                    BlockState state;
                    if (d < 1.3) {
                        state = Blocks.PURPUR_PILLAR.defaultBlockState();
                    } else if (d < TOWER_R - 1) {
                        state = AIR;
                    } else if ((y - PLAZA) % 8 == 0) {
                        state = Blocks.PURPUR_PILLAR.defaultBlockState();
                    } else if ((y - PLAZA) % 8 >= 3 && (y - PLAZA) % 8 <= 5 && Math.abs(Math.sin(angle(x, z) * 2)) > 0.7) {
                        state = ModBlocks.TIME_GLASS.defaultBlockState();
                    } else {
                        state = pick(x, y, z, 3, Blocks.PURPUR_BLOCK.defaultBlockState(), 0.04F, ModBlocks.TIME_GLASS.defaultBlockState(), 0, AIR);
                    }
                    put(level, box, x, y, z, state);
                }
            }
        }
        // Tor nach Süden
        fill(level, box, C - 1, PLAZA + 1, C + 4, C + 1, PLAZA + 3, C + 6, AIR);
        put(level, box, C, PLAZA + 4, C + 5, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
    }

    /** Wendeltreppe aus Purpur-Stufen: jede Stufe eine halbe Höhe, einmal herum sind es zwölf Blöcke. */
    private void stairs(WorldGenLevel level, BoundingBox box) {
        for (int step = 0; step < STAIR_STEPS; step++) {
            double a0 = (step * STAIR_STEP_ANGLE) % (Math.PI * 2);
            double a1 = a0 + STAIR_STEP_ANGLE;
            int y = PLAZA + 1 + step / 2;
            BlockState slab = Blocks.PURPUR_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, step % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP);
            for (int x = C - 5; x <= C + 5; x++) {
                for (int z = C - 5; z <= C + 5; z++) {
                    double d = dist(x, z);
                    double a = angle(x, z);
                    if (d >= 1.3 && d < TOWER_R - 1 && inArc(a, a0, a1)) {
                        put(level, box, x, y, z, slab);
                    }
                }
            }
        }
    }

    /** Liegt der Winkel {@code a} im Bogen von {@code a0} bis {@code a1} (darf über 2π hinausreichen)? */
    private static boolean inArc(double a, double a0, double a1) {
        return a1 <= Math.PI * 2 ? a >= a0 && a < a1 : a >= a0 || a < a1 - Math.PI * 2;
    }

    /** Vier Kristalltürmchen auf den Diagonalen. */
    private void spires(WorldGenLevel level, BoundingBox box) {
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int sx = C + (int) Math.round(Math.cos(a) * 13.5);
            int sz = C + (int) Math.round(Math.sin(a) * 13.5);
            for (int x = sx - 2; x <= sx + 2; x++) {
                for (int z = sz - 2; z <= sz + 2; z++) {
                    double d = Math.hypot(x - sx, z - sz);
                    if (d > 1.6) {
                        continue;
                    }
                    for (int y = PLAZA + 1; y <= PLAZA + 12; y++) {
                        put(level, box, x, y, z, d < 0.5 ? Blocks.PURPUR_PILLAR.defaultBlockState() : Blocks.PURPUR_BLOCK.defaultBlockState());
                    }
                    put(level, box, x, PLAZA + 13, z, Blocks.AMETHYST_BLOCK.defaultBlockState());
                }
            }
            put(level, box, sx, PLAZA + 14, sz, Blocks.AMETHYST_BLOCK.defaultBlockState());
            put(level, box, sx, PLAZA + 15, sz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            put(level, box, sx, PLAZA + 16, sz, AIR);
        }
    }

    /** Das erloschene Tor zum Zeitstrom: ein Ring aus Messing im Norden des Platzes. */
    private void gate(WorldGenLevel level, BoundingBox box) {
        int gz = C - 13;
        int gy = PLAZA + 5;
        for (int x = C - 5; x <= C + 5; x++) {
            for (int y = PLAZA + 1; y <= PLAZA + 10; y++) {
                double d = Math.hypot(x - C, y - gy);
                if (d >= 3.3 && d < 4.5) {
                    BlockState ring = (x + y) % 3 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : ModBlocks.BRASS_BLOCK.defaultBlockState();
                    put(level, box, x, y, gz, ring);
                }
            }
        }
        fill(level, box, C - 2, PLAZA + 1, gz - 1, C + 2, PLAZA + 1, gz + 1, ModBlocks.BRASS_BLOCK.defaultBlockState());
    }

    /** Oben die Arena: Zifferblatt-Boden, Spiegel-Altar in der Mitte, Deckungssäulen und Geländer. */
    private void arena(WorldGenLevel level, BoundingBox box) {
        // Stützen vom Platz zur Arena
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2;
            int px = C + (int) Math.round(Math.cos(a) * 11);
            int pz = C + (int) Math.round(Math.sin(a) * 11);
            fill(level, box, px, PLAZA + 1, pz, px, ARENA - 1, pz, Blocks.PURPUR_PILLAR.defaultBlockState());
        }
        double openFrom = (OPENING_FROM * STAIR_STEP_ANGLE) % (Math.PI * 2);
        double openTo = openFrom + (STAIR_STEPS - OPENING_FROM) * STAIR_STEP_ANGLE;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = dist(x, z);
                if (d > 14.5) {
                    continue;
                }
                double a = angle(x, z);
                boolean opening = d >= 1.3 && d < TOWER_R - 1 && inArc(a, openFrom, openTo);
                BlockState floor = Blocks.END_STONE_BRICKS.defaultBlockState();
                if (d < 2.5) {
                    floor = Blocks.QUARTZ_BRICKS.defaultBlockState();
                } else if (d >= 6 && d < 7) {
                    floor = Blocks.PURPUR_BLOCK.defaultBlockState();
                }
                double hour = a / (Math.PI / 6);
                if (d >= 11.5 && d < 13 && Math.abs(hour - Math.round(hour)) < 0.12) {
                    floor = Blocks.AMETHYST_BLOCK.defaultBlockState();
                }
                put(level, box, x, ARENA, z, opening ? AIR : floor);
                for (int y = ARENA + 1; y < HEIGHT; y++) {
                    put(level, box, x, y, z, AIR);
                }
                if (d >= 13.5) {
                    boolean post = Math.abs(a / (Math.PI / 4) - Math.round(a / (Math.PI / 4))) < 0.08;
                    if (post) {
                        fill(level, box, x, ARENA + 1, z, x, ARENA + 3, z, Blocks.PURPUR_PILLAR.defaultBlockState());
                        put(level, box, x, ARENA + 4, z, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
                    } else {
                        put(level, box, x, ARENA + 1, z, Blocks.END_STONE_BRICK_WALL.defaultBlockState());
                    }
                }
            }
        }
        // Deckungssäulen
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int px = C + (int) Math.round(Math.cos(a) * 8.5);
            int pz = C + (int) Math.round(Math.sin(a) * 8.5);
            fill(level, box, px, ARENA + 1, pz, px, ARENA + 3, pz, Blocks.PURPUR_PILLAR.defaultBlockState());
            put(level, box, px, ARENA + 4, pz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
        }
        put(level, box, C, ARENA + 1, C, ModBlocks.PARADOX_ALTAR.defaultBlockState());
    }
}

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
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Zeitlose Werkstatt: ein versiegelter Saal tief unter der Erde. Riesige Messingräder an den
 * Wänden, ein Zifferblatt-Mosaik im Boden, Werkbänke und Ambosse – und in der Mitte der
 * Uhrwerk-Altar, an dem der Uhrmacher erwacht.
 */
public class TimelessWorkshopPiece extends TimePiece {
    public static final ResourceLocation LOOT = TimeMod.id("chests/timeless_workshop");
    static final int SIZE = 21;
    static final int HEIGHT = 11;
    private static final int C = 10;

    public TimelessWorkshopPiece(BlockPos origin, Direction orientation, long seed) {
        super(ModStructures.TIMELESS_WORKSHOP_PIECE, makeBoundingBox(origin.getX(), origin.getY(), origin.getZ(), orientation, SIZE, HEIGHT, SIZE), orientation, seed);
    }

    public TimelessWorkshopPiece(CompoundTag tag) {
        super(ModStructures.TIMELESS_WORKSHOP_PIECE, tag);
    }

    private BlockState shell(int x, int y, int z) {
        return pick(x, y, z, 1, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
            0.12F, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 0.06F, Blocks.GILDED_BLACKSTONE.defaultBlockState());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // Hülle (zwei Blöcke dick, ohne Zugang) und Innenraum
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean inside = x >= 2 && x <= SIZE - 3 && z >= 2 && z <= SIZE - 3 && y >= 2 && y <= HEIGHT - 3;
                    put(level, box, x, y, z, inside ? AIR : shell(x, y, z));
                }
            }
        }
        BlockState brass = ModBlocks.BRASS_BLOCK.defaultBlockState();
        // Boden mit Zifferblatt-Mosaik
        for (int x = 2; x <= SIZE - 3; x++) {
            for (int z = 2; z <= SIZE - 3; z++) {
                double d = Math.hypot(x - C, z - C);
                double a = Math.toDegrees(Math.atan2(z - C, x - C));
                BlockState floor;
                if (d <= 6.4) {
                    boolean marker = d > 4.6 && Math.abs(((a + 360 + 15) % 30) - 15) < 4;
                    floor = marker ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
                } else if (d <= 7.5) {
                    floor = brass;
                } else {
                    floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                }
                put(level, box, x, 1, z, floor);
            }
        }
        // Zeiger: 11:55 – fast Mitternacht
        for (int i = 1; i <= 5; i++) {
            put(level, box, C, 1, C - i, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        }
        for (int i = 1; i <= 3; i++) {
            put(level, box, C - (i + 1) / 2, 1, C - i, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        }
        put(level, box, C, 1, C, brass);
        put(level, box, C, 2, C, ModBlocks.CLOCKMAKER_ALTAR.defaultBlockState());
        // Messingräder an allen vier Wänden
        wallGear(level, box, Direction.NORTH);
        wallGear(level, box, Direction.SOUTH);
        wallGear(level, box, Direction.WEST);
        wallGear(level, box, Direction.EAST);
        // Messing-Pfeiler in den Ecken, Lampen an Ketten
        for (int[] c : new int[][] {{2, 2}, {SIZE - 3, 2}, {2, SIZE - 3}, {SIZE - 3, SIZE - 3}}) {
            for (int y = 2; y <= HEIGHT - 3; y++) {
                put(level, box, c[0], y, c[1], brass);
            }
        }
        for (int[] l : new int[][] {{6, 6}, {14, 6}, {6, 14}, {14, 14}}) {
            put(level, box, l[0], HEIGHT - 3, l[1], Blocks.CHAIN.defaultBlockState());
            put(level, box, l[0], HEIGHT - 4, l[1], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // Werkbänke, Ambosse, Truhen
        put(level, box, 4, 2, 3, ModBlocks.CLOCKMAKER_BENCH.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, 16, 2, 3, ModBlocks.CLOCKMAKER_BENCH.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        put(level, box, 3, 2, 16, Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST));
        put(level, box, 17, 2, 16, Blocks.DAMAGED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.WEST));
        put(level, box, 3, 2, 10, Blocks.SMITHING_TABLE.defaultBlockState());
        createChest(level, box, random, 5, 2, 17, LOOT);
        createChest(level, box, random, 15, 2, 17, LOOT);
        createChest(level, box, random, 17, 2, 4, LOOT);
    }

    /** Ein riesiges Zahnrad (Radius 5) in der Wand, zur Hälfte im Boden versunken. */
    private void wallGear(WorldGenLevel level, BoundingBox box, Direction wall) {
        BlockState brass = ModBlocks.BRASS_BLOCK.defaultBlockState();
        BlockState dark = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        int cy = 3;
        for (int u = -6; u <= 6; u++) {
            for (int v = -6; v <= 6; v++) {
                int y = cy + v;
                if (y < 2 || y > HEIGHT - 3) {
                    continue;
                }
                double r = Math.sqrt(u * u + v * v);
                double a = Math.atan2(v, u);
                boolean rim = r >= 3.4 && r <= 4.5;
                boolean tooth = r > 4.5 && r <= 5.6 && Math.cos(a * 10) > 0.25;
                boolean spoke = r < 3.4 && (u == 0 || v == 0);
                boolean hub = r < 1.3;
                if (!(rim || tooth || spoke || hub)) {
                    continue;
                }
                BlockState state = hub ? dark : brass;
                switch (wall) {
                    case NORTH -> put(level, box, C + u, y, 2, state);
                    case SOUTH -> put(level, box, C + u, y, SIZE - 3, state);
                    case WEST -> put(level, box, 2, y, C + u, state);
                    default -> put(level, box, SIZE - 3, y, C + u, state);
                }
            }
        }
    }
}

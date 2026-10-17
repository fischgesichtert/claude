package com.fischgesichtert.timemod.future.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;

/**
 * Der Marktplatz: gepflastert, in der Mitte ein Brunnen (ab Stufe 3 ein Springbrunnen), an den Ecken
 * Marktstände mit bunten Dächern und Händlern, Laternen, und die Eisengolems der Siedlung.
 */
public class PlazaPiece extends TownPiece {
    private static final Block[] AWNINGS = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.ORANGE_WOOL,
        Blocks.MAGENTA_WOOL};
    private static final Block[] GOODS = {Blocks.BARREL, Blocks.COMPOSTER, Blocks.SMOKER, Blocks.FLETCHING_TABLE};

    public PlazaPiece(int x0, int y, int z0, int size, long seed, TownStyle style) {
        super(TownPieces.PLAZA, new BoundingBox(x0, y - 1, z0, x0 + size - 1, y + 8, z0 + size - 1), Direction.NORTH, seed, style);
    }

    public PlazaPiece(CompoundTag tag) {
        super(TownPieces.PLAZA, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        int size = boundingBox.getXSpan();
        int c = size / 2;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                for (int y = 2; y <= 8; y++) {
                    put(level, box, x, y, z, OPEN);
                }
                boolean ring = Math.max(Math.abs(x - c), Math.abs(z - c)) % 3 == 0;
                put(level, box, x, 1, z, state(ring ? style.wallStone() : style.road()));
                fillColumnDown(level, state(style.foundation()), x, 0, z, box);
            }
        }
        if (style.stage() >= 3) {
            fountain(level, box, c);
        } else {
            well(level, box, c);
        }
        int[][] corners = {{2, 2}, {size - 5, 2}, {2, size - 5}, {size - 5, size - 5}};
        for (int i = 0; i < corners.length; i++) {
            stall(level, box, corners[i][0], corners[i][1], i);
        }
        for (int[] lamp : new int[][] {{0, 0}, {size - 1, 0}, {0, size - 1}, {size - 1, size - 1}}) {
            put(level, box, lamp[0], 2, lamp[1], state(style.wallCap()));
            put(level, box, lamp[0], 3, lamp[1], state(style.fence()));
            put(level, box, lamp[0], 4, lamp[1], Blocks.LANTERN.defaultBlockState());
        }
        int golems = style.city() ? 3 : style.stage() >= 3 ? 2 : 1;
        for (int g = 0; g < golems; g++) {
            golem(level, box, c - 3 + g * 3, 2, c + 4);
        }
        villager(level, box, c + 3, 2, c - 3, false);
        villager(level, box, c - 3, 2, c - 3, chance(c, 2, c, 9) < 0.5F);
    }

    /** Ein Brunnen wie im Dorf: Wasser im Steinkranz, Pfosten und ein kleines Dach. */
    private void well(WorldGenLevel level, BoundingBox box, int c) {
        for (int x = c - 1; x <= c + 1; x++) {
            for (int z = c - 1; z <= c + 1; z++) {
                boolean middle = x == c && z == c;
                put(level, box, x, 1, z, middle ? Blocks.WATER.defaultBlockState() : state(style.wallStone()));
                put(level, box, x, 0, z, middle ? Blocks.WATER.defaultBlockState() : state(style.wallStone()));
                if (!middle) {
                    put(level, box, x, 2, z, state(style.wallCap()));
                }
            }
        }
        for (int[] post : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            put(level, box, c + post[0], 3, c + post[1], state(style.fence()));
            put(level, box, c + post[0], 4, c + post[1], state(style.fence()));
        }
        for (int x = c - 1; x <= c + 1; x++) {
            for (int z = c - 1; z <= c + 1; z++) {
                put(level, box, x, 5, z, slab(style.roofBlock()));
            }
        }
    }

    /** Springbrunnen: Becken mit Rand, in der Mitte eine Säule, aus der Wasser quillt. */
    private void fountain(WorldGenLevel level, BoundingBox box, int c) {
        for (int x = c - 2; x <= c + 2; x++) {
            for (int z = c - 2; z <= c + 2; z++) {
                boolean rim = Math.abs(x - c) == 2 || Math.abs(z - c) == 2;
                put(level, box, x, 1, z, rim ? state(style.wallStone()) : Blocks.WATER.defaultBlockState());
                put(level, box, x, 0, z, state(style.wallStone()));
                if (rim) {
                    put(level, box, x, 2, z, slab(style.wallStone() == Blocks.STONE_BRICKS ? Blocks.STONE_BRICK_SLAB : Blocks.SMOOTH_STONE_SLAB));
                }
            }
        }
        for (int y = 1; y <= 3; y++) {
            put(level, box, c, y, c, state(style.city() ? Blocks.QUARTZ_PILLAR : style.wallStone()));
        }
        BlockPos spout = getWorldPos(c, 4, c);
        if (box.isInside(spout)) {
            level.setBlock(spout, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.scheduleTick(spout, Fluids.WATER, 0);
        }
    }

    /** Marktstand: zwei Pfosten, Theke mit Ware, Stoffdach in einer Farbe. */
    private void stall(WorldGenLevel level, BoundingBox box, int x0, int z0, int index) {
        Block awning = AWNINGS[Math.floorMod((int) (seed >>> 4) + index, AWNINGS.length)];
        for (int x = x0; x < x0 + 3; x++) {
            put(level, box, x, 2, z0 + 2, x == x0 + 1 ? GOODS[Math.floorMod(index + (int) (seed >>> 9), GOODS.length)].defaultBlockState()
                : Blocks.BARREL.defaultBlockState());
            for (int z = z0; z < z0 + 3; z++) {
                put(level, box, x, 5, z, awning.defaultBlockState());
            }
        }
        for (int[] post : new int[][] {{x0, z0}, {x0 + 2, z0}}) {
            for (int y = 2; y <= 4; y++) {
                put(level, box, post[0], y, post[1], state(style.fence()));
            }
        }
        put(level, box, x0 + 1, 2, z0 + 1, OPEN);
        villager(level, box, x0 + 1, 2, z0 + 1, false);
    }

    private static BlockState slab(Block block) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(SlabBlock.TYPE) ? state.setValue(SlabBlock.TYPE, SlabType.BOTTOM) : state;
    }
}

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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Ein Feld vor der Siedlung: Rahmen aus Stämmen, Wassergraben in der Mitte, reifende Feldfrüchte. */
public class FieldPiece extends TownPiece {
    public FieldPiece(int x0, int y, int z0, int width, int depth, long seed, TownStyle style) {
        super(TownPieces.FIELD, new BoundingBox(x0, y - 1, z0, x0 + width - 1, y + 3, z0 + depth - 1), Direction.NORTH, seed, style);
    }

    public FieldPiece(CompoundTag tag) {
        super(TownPieces.FIELD, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        int width = boundingBox.getXSpan();
        int depth = boundingBox.getZSpan();
        Block crop = crop();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean border = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                boolean channel = !border && x == width / 2;
                for (int y = 2; y <= 4; y++) {
                    put(level, box, x, y, z, OPEN);
                }
                fillColumnDown(level, state(Blocks.DIRT), x, 0, z, box);
                if (border) {
                    boolean alongX = z == 0 || z == depth - 1;
                    put(level, box, x, 1, z, style.pillar().defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)
                        ? style.pillar().defaultBlockState().setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z)
                        : style.pillar().defaultBlockState());
                } else if (channel) {
                    put(level, box, x, 1, z, Blocks.WATER.defaultBlockState());
                    put(level, box, x, 0, z, state(Blocks.DIRT));
                } else {
                    put(level, box, x, 1, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                    put(level, box, x, 2, z, ripe(crop, x, z));
                }
            }
        }
        // Eine Vogelscheuche aus Zaun und Kürbis an einer Ecke
        put(level, box, 1, 2, 1, state(style.fence()));
        put(level, box, 1, 3, 1, Blocks.CARVED_PUMPKIN.defaultBlockState());
    }

    private Block crop() {
        return switch (Math.floorMod((int) (seed >>> 5), 4)) {
            case 1 -> Blocks.CARROTS;
            case 2 -> Blocks.POTATOES;
            case 3 -> style.type().equals("snowy") || style.type().equals("taiga") ? Blocks.SWEET_BERRY_BUSH : Blocks.BEETROOTS;
            default -> Blocks.WHEAT;
        };
    }

    private BlockState ripe(Block crop, int x, int z) {
        if (crop instanceof CropBlock cropBlock) {
            int age = chance(x, 0, z, 41) < 0.5F ? cropBlock.getMaxAge() : (int) (chance(x, 1, z, 42) * cropBlock.getMaxAge());
            return cropBlock.getStateForAge(age);
        }
        if (crop == Blocks.SWEET_BERRY_BUSH) {
            return Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 2);
        }
        return crop.defaultBlockState();
    }
}

package com.fischgesichtert.timemod.worldgen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.ChroniteOreBlock;
import com.fischgesichtert.timemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Chrono-Ruinen tief im Deep Dark: eine eingestürzte Halle aus Tiefenschiefer, zerbrochene Säulen
 * voller Chronit-Adern, Sculk überall – und in der Mitte der Phagen-Altar, umringt von vier Sockeln.
 */
public class ChronoRuinsPiece extends TimePiece {
    public static final ResourceLocation LOOT = TimeMod.id("chests/chrono_ruins");
    static final int SIZE = 29;
    static final int HEIGHT = 13;
    private static final int C = 14;
    private static final double R = 13.5;

    public ChronoRuinsPiece(BlockPos origin, Direction orientation, long seed) {
        super(ModStructures.CHRONO_RUINS_PIECE, makeBoundingBox(origin.getX(), origin.getY(), origin.getZ(), orientation, SIZE, HEIGHT, SIZE), orientation, seed);
    }

    public ChronoRuinsPiece(CompoundTag tag) {
        super(ModStructures.CHRONO_RUINS_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d > R) {
                    continue;
                }
                // Gewölbe: in der Mitte hoch, zum Rand flacher
                int roof = (int) Math.round(10 * Math.sqrt(Math.max(0, 1 - (d / R) * (d / R)))) + 1;
                for (int y = 1; y <= Math.min(HEIGHT - 2, roof); y++) {
                    put(level, box, x, y, z, AIR);
                }
                BlockState floor = pick(x, 0, z, 1, Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    0.28F, Blocks.SCULK.defaultBlockState(), 0.14F, Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState());
                put(level, box, x, 0, z, floor);
                if (floor.is(Blocks.SCULK) && chance(x, 1, z, 2) < 0.12F) {
                    put(level, box, x, 1, z, Blocks.SCULK_VEIN.defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.DOWN), true));
                }
            }
        }
        // Säulenraster mit zufälligen Bruchhöhen und Chronit-Adern
        for (int px = 4; px <= 24; px += 5) {
            for (int pz = 4; pz <= 24; pz += 5) {
                double d = Math.hypot(px - C, pz - C);
                if (d < 6 || d > R - 1.5) {
                    continue;
                }
                int height = 2 + (int) (chance(px, 0, pz, 3) * 8);
                for (int y = 1; y <= height; y++) {
                    BlockState block;
                    if (chance(px, y, pz, 4) < 0.18F) {
                        block = ModBlocks.DEEPSLATE_CHRONITE_ORE.defaultBlockState().setValue(ChroniteOreBlock.PHASED, false);
                    } else if (y == height) {
                        block = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                    } else {
                        block = pick(px, y, pz, 5, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 0.25F, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
                            0.1F, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    }
                    put(level, box, px, y, pz, block);
                }
                // Trümmer daneben
                if (height < 6) {
                    int dx = chance(px, 1, pz, 6) < 0.5F ? 1 : -1;
                    put(level, box, px + dx, 1, pz, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                    put(level, box, px + dx, 2, pz + dx, Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
                }
            }
        }
        // Zentrale Plattform mit dem Phagen-Altar und vier Sockeln
        for (int x = C - 4; x <= C + 4; x++) {
            for (int z = C - 4; z <= C + 4; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d <= 4.3) {
                    put(level, box, x, 1, z, d > 3.4 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
            }
        }
        put(level, box, C, 2, C, ModBlocks.PHAGE_ALTAR.defaultBlockState());
        for (int[] o : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            put(level, box, C + o[0], 2, C + o[1], Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            put(level, box, C + o[0], 3, C + o[1], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        }
        for (int[] o : new int[][] {{0, -4}, {0, 4}, {-4, 0}, {4, 0}}) {
            put(level, box, C + o[0], 2, C + o[1], Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        createChest(level, box, random, 7, 1, 21, LOOT);
        createChest(level, box, random, 22, 1, 8, LOOT);
    }
}

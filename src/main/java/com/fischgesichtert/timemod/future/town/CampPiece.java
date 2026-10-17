package com.fischgesichtert.timemod.future.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Ein Plünderer-Lager in den Ruinen eines untergegangenen Dorfes: Zelte aus grauer Wolle, ein Lagerfeuer,
 * ein Pfahl mit dem Banner der Illager, ein Käfig – und die Plünderer samt Hauptmann.
 */
public class CampPiece extends TownPiece {
    static final int SIZE = 17;

    public CampPiece(int x0, int y, int z0, long seed, TownStyle style) {
        super(TownPieces.CAMP, new BoundingBox(x0, y - 1, z0, x0 + SIZE - 1, y + 10, z0 + SIZE - 1), Direction.NORTH, seed, style);
    }

    public CampPiece(CompoundTag tag) {
        super(TownPieces.CAMP, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        int c = SIZE / 2;
        // Lagerplatz: festgetretener Boden
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double r = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
                if (r > c + 0.5) {
                    continue;
                }
                for (int y = 2; y <= 6; y++) {
                    put(level, box, x, y, z, OPEN);
                }
                put(level, box, x, 1, z, chance(x, 1, z, 3) < 0.6F ? state(Blocks.COARSE_DIRT) : state(Blocks.PODZOL));
                fillColumnDown(level, state(Blocks.DIRT), x, 0, z, box);
            }
        }
        put(level, box, c, 2, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            put(level, box, c + side.getStepX() * 2, 2, c + side.getStepZ() * 2, Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, side));
        }
        tent(level, box, 1, 2, Blocks.GRAY_WOOL);
        tent(level, box, SIZE - 6, 2, Blocks.LIGHT_GRAY_WOOL);
        tent(level, box, 1, SIZE - 7, Blocks.GRAY_WOOL);
        cage(level, box, SIZE - 6, SIZE - 6);
        // Bannerpfahl
        for (int y = 2; y <= 6; y++) {
            put(level, box, c, y, 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
        }
        BlockPos banner = getWorldPos(c, 7, 2);
        if (box.isInside(banner)) {
            level.setBlock(banner, Blocks.WHITE_BANNER.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(banner) instanceof BannerBlockEntity entity) {
                entity.fromItem(Raid.getLeaderBannerInstance(), net.minecraft.world.item.DyeColor.WHITE);
            }
        }
        put(level, box, c + 3, 2, c - 2, Blocks.BARREL.defaultBlockState());
        put(level, box, c + 4, 2, c - 2, Blocks.FLETCHING_TABLE.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            pillager(level, box, c - 3 + i * 2, 2, c + 3, i == 0);
        }
    }

    /** Ein Spitzzelt: zwei schräge Wollreihen über einem Firstbalken. */
    private void tent(WorldGenLevel level, BoundingBox box, int x0, int z0, Block wool) {
        for (int z = z0; z < z0 + 5; z++) {
            for (int y = 0; y < 3; y++) {
                put(level, box, x0 + y, 2 + y, z, wool.defaultBlockState());
                put(level, box, x0 + 4 - y, 2 + y, z, wool.defaultBlockState());
            }
            put(level, box, x0 + 2, 5, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
        }
        put(level, box, x0 + 2, 2, z0 + 2, Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
            .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        put(level, box, x0 + 2, 2, z0 + 1, Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
            .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
    }

    /** Ein Käfig aus Zaun und Gitter – leer, die Gefangenen sind längst fort. */
    private void cage(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        for (int x = x0; x < x0 + 4; x++) {
            for (int z = z0; z < z0 + 4; z++) {
                boolean edge = x == x0 || z == z0 || x == x0 + 3 || z == z0 + 3;
                if (!edge) {
                    continue;
                }
                for (int y = 2; y <= 4; y++) {
                    put(level, box, x, y, z, Blocks.IRON_BARS.defaultBlockState());
                }
                put(level, box, x, 5, z, Blocks.DARK_OAK_SLAB.defaultBlockState());
            }
        }
    }

    private void pillager(WorldGenLevel level, BoundingBox box, int x, int y, int z, boolean captain) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Pillager pillager = EntityType.PILLAGER.create(level.getLevel());
        if (pillager == null) {
            return;
        }
        pillager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, chance(x, y, z, 8) * 360.0F, 0.0F);
        pillager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        if (captain) {
            pillager.setPatrolLeader(true);
            pillager.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
            pillager.setDropChance(EquipmentSlot.HEAD, 2.0F);
        }
        pillager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(pillager);
    }
}

package com.fischgesichtert.timemod.era;

import com.fischgesichtert.timemod.block.TimeCapsuleBlock;
import com.fischgesichtert.timemod.block.entity.TimeCapsuleBlockEntity;
import com.fischgesichtert.timemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Eine versiegelte Kapsel taucht in der Gegenwart auf: an derselben Stelle, notfalls im Erdreich vergraben. */
public final class TimeCapsules {
    private static final int[] HEIGHTS = {0, 1, -1, 2, -2, 3, -3};

    private TimeCapsules() {
    }

    static void deliver(ServerLevel present, Timeline.Entry entry) {
        NonNullList<ItemStack> sealed = NonNullList.withSize(TimeCapsuleBlockEntity.SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(entry.data(), sealed);
        NonNullList<ItemStack> aged = NonNullList.withSize(TimeCapsuleBlockEntity.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < sealed.size(); i++) {
            aged.set(i, Aging.age(sealed.get(i), entry.years()));
        }
        BlockPos spot = spot(present, entry.pos());
        present.setBlockAndUpdate(spot, ModBlocks.TIME_CAPSULE.defaultBlockState().setValue(TimeCapsuleBlock.STAGE, TimeCapsuleBlock.Stage.ANCIENT));
        if (present.getBlockEntity(spot) instanceof TimeCapsuleBlockEntity capsule) {
            capsule.fill(aged, entry.years(), entry.data().getString("Sender"));
        }
    }

    /** Dieselbe Stelle, wenn dort nur Natur ist; sonst daneben – nie in jemandes Bauwerk. */
    static BlockPos spot(ServerLevel level, BlockPos wanted) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int radius = 0; radius <= 4; radius++) {
            for (int dy : HEIGHTS) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        pos.set(wanted.getX() + dx, wanted.getY() + dy, wanted.getZ() + dz);
                        if (isNatural(level.getBlockState(pos)) && level.getBlockEntity(pos) == null) {
                            return settle(level, pos.immutable());
                        }
                    }
                }
            }
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wanted.getX(), wanted.getZ());
        return new BlockPos(wanted.getX(), top, wanted.getZ());
    }

    /** In der Luft oder im Wasser sinkt die Kapsel bis auf den Grund. */
    private static BlockPos settle(ServerLevel level, BlockPos pos) {
        BlockPos at = pos;
        for (int i = 0; i < 64 && at.getY() > level.getMinBuildHeight() + 1; i++) {
            BlockState below = level.getBlockState(at.below());
            if (!(below.isAir() || below.canBeReplaced())) {
                break;
            }
            at = at.below();
        }
        return at;
    }

    /** Gewachsene Natur: Luft, Wasser, Pflanzen, Erde, Sand, Kies, Stein. */
    static boolean isNatural(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
            || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.SNOW)
            || state.is(BlockTags.ICE) || state.is(Blocks.CLAY) || state.is(Blocks.MUD);
    }
}

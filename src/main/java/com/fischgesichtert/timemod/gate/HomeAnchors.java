package com.fischgesichtert.timemod.gate;

import com.fischgesichtert.timemod.block.entity.HomeAnchorBlockEntity;
import com.fischgesichtert.timemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Setzt am Ziel einer Reise den Heimkehr-Anker – oder benutzt einen, der von früher schon dasteht. */
public final class HomeAnchors {
    private static final int REUSE_RADIUS = 12;

    private HomeAnchors() {
    }

    public static void placeNear(ServerLevel level, BlockPos landing, ResourceKey<Level> returnLevel, BlockPos returnPos) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -REUSE_RADIUS; dx <= REUSE_RADIUS; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -REUSE_RADIUS; dz <= REUSE_RADIUS; dz++) {
                    pos.set(landing.getX() + dx, landing.getY() + dy, landing.getZ() + dz);
                    if (level.getBlockEntity(pos) instanceof HomeAnchorBlockEntity anchor && anchor.leadsTo(returnLevel, returnPos)) {
                        return;
                    }
                }
            }
        }
        BlockPos spot = freeSpot(level, landing);
        level.setBlockAndUpdate(spot, ModBlocks.HOME_ANCHOR.defaultBlockState());
        if (level.getBlockEntity(spot) instanceof HomeAnchorBlockEntity anchor) {
            anchor.setReturn(returnLevel, returnPos);
        }
    }

    /** Zwei Schritte neben der Landestelle auf festem Boden; zur Not direkt daneben. */
    private static BlockPos freeSpot(ServerLevel level, BlockPos landing) {
        for (int distance = 2; distance <= 3; distance++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos candidate = landing.relative(direction, distance).above(dy);
                    BlockState below = level.getBlockState(candidate.below());
                    if (level.getBlockState(candidate).canBeReplaced() && level.getBlockState(candidate).getFluidState().isEmpty()
                        && below.isFaceSturdy(level, candidate.below(), Direction.UP) && level.getBlockState(candidate.above()).isAir()) {
                        return candidate;
                    }
                }
            }
        }
        return landing.east();
    }
}

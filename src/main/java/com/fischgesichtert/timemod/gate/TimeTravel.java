package com.fischgesichtert.timemod.gate;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Die Reise selbst: Wer durch das Tor geht, steht im Zieljahr an derselben Stelle der Welt – auf
 * festem Boden, nicht im Meer und nicht im Fels.
 */
public final class TimeTravel {
    /** So weit sucht die Landung nach trockenem Boden, bevor sie ein Floß baut. */
    private static final int SEARCH_RADIUS = 48;

    private TimeTravel() {
    }

    public static boolean travel(ServerPlayer player, Era target) {
        ServerLevel destination = player.server.getLevel(target.level());
        if (destination == null || player.isOnPortalCooldown() || player.isPassenger() || player.isVehicle()) {
            return false;
        }
        player.setPortalCooldown();
        Vec3 landing = landing(destination, player.blockPosition());
        player.teleportTo(destination, landing.x, landing.y, landing.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        destination.playSound(null, landing.x, landing.y, landing.z, ModSounds.GATE_ARRIVE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.connection.send(new ClientboundSetTitleTextPacket(target.yearText().copy().withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(target.title().copy().withStyle(ChatFormatting.GRAY)));
        return true;
    }

    /** Ein sicherer Landeplatz nahe der gleichen Stelle; über offenem Wasser ein kleines Floß. */
    public static Vec3 landing(ServerLevel level, BlockPos near) {
        for (int radius = 0; radius <= SEARCH_RADIUS; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    BlockPos ground = surface(level, near.getX() + dx, near.getZ() + dz);
                    if (isSafeGround(level, ground)) {
                        return Vec3.atBottomCenterOf(ground.above());
                    }
                }
            }
        }
        BlockPos water = surface(level, near.getX(), near.getZ());
        BlockState raft = Blocks.OAK_PLANKS.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(water.offset(dx, 0, dz), raft);
            }
        }
        return Vec3.atBottomCenterOf(water.above());
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos ground) {
        BlockState state = level.getBlockState(ground);
        return ground.getY() > level.getMinBuildHeight() && state.getFluidState().isEmpty() && state.isFaceSturdy(level, ground, Direction.UP)
            && !state.is(Blocks.MAGMA_BLOCK) && isFree(level, ground.above()) && isFree(level, ground.above(2));
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }
}

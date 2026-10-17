package com.fischgesichtert.timemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Erwecken der Bosse an ihren Altären (die Bosse selbst folgen mit den Boss-Wesen). */
public final class BossAltars {
    private BossAltars() {
    }

    static InteractionResult tryAwaken(BossAltarBlock altar, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}

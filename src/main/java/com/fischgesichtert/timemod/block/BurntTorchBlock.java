package com.fischgesichtert.timemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Was nach Jahren von einer Fackel bleibt: kein Licht, keine Flamme, kein Rauch. */
public class BurntTorchBlock extends TorchBlock {
    public BurntTorchBlock(Properties properties) {
        super(properties, ParticleTypes.SMOKE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    /** Dieselbe Fackel an der Wand. */
    public static class Wall extends WallTorchBlock {
        public Wall(Properties properties) {
            super(properties, ParticleTypes.SMOKE);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        }
    }
}

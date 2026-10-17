package com.fischgesichtert.timemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Die Glocke der letzten Stadt: eine grün angelaufene Glocke in einem morschen Joch auf einem bemoosten
 * Sockel, mitten in den Ruinen einer Stadt von +1000. Drei Wächterkerne bringen sie zum Klingen – und
 * wecken, was unter dem alten Marktplatz schläft.
 */
public class CityBellBlock extends BossAltarBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(4, 8, 4, 12, 15, 12), Block.box(2, 5, 7, 14, 16, 9));

    public CityBellBlock(BlockBehaviour.Properties properties) {
        super("message.timemod.altar.city_bell", properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.6,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, -0.01, 0);
        }
    }
}

package com.fischgesichtert.timemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chronit-Erz flackert durch die Zeit: Es ist nur zeitweise „da“ und nur dann abbaubar. Der Wechsel
 * läuft über geplante Ticks – in einer Stopp-Zone (Moment-Linse!) bleibt es also, wie es gerade ist.
 */
public class ChroniteOreBlock extends DropExperienceBlock {
    public static final BooleanProperty PHASED = BooleanProperty.create("phased");

    public ChroniteOreBlock(BlockBehaviour.Properties properties) {
        super(properties, UniformInt.of(4, 9));
        registerDefaultState(stateDefinition.any().setValue(PHASED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASED);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !old.is(this)) {
            level.scheduleTick(pos, this, 20 + level.random.nextInt(60));
        }
    }

    /** Erz aus der Weltgenerierung startet sein Flackern beim ersten Zufalls-Tick. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 10 + random.nextInt(40));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean phased = !state.getValue(PHASED);
        level.setBlock(pos, state.setValue(PHASED, phased), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, phased ? 30 + random.nextInt(50) : 60 + random.nextInt(80));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PHASED) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PHASED) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(PHASED) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(PHASED) ? 2 : 6) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}

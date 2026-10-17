package com.fischgesichtert.timemod.block;

import com.fischgesichtert.timemod.block.entity.TimeCapsuleBlockEntity;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.era.Timeline;
import com.fischgesichtert.timemod.registry.ModAdvancements;
import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Die Zeitkapsel: In der Vergangenheit befüllt und (Schleichen + Benutzen) versiegelt, liegt sie in der
 * Gegenwart an derselben Stelle – mit allem, was die Jahre davon übrig gelassen haben.
 */
public class TimeCapsuleBlock extends BaseEntityBlock {
    public enum Stage implements StringRepresentable {
        OPEN, SEALED, ANCIENT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public TimeCapsuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.OPEN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeCapsuleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TimeCapsuleBlockEntity capsule)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(STAGE) == Stage.SEALED) {
            player.displayClientMessage(Component.translatable("message.timemod.capsule.sealed").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && state.getValue(STAGE) == Stage.OPEN) {
            seal((ServerLevel) level, pos, state, capsule, player);
            return InteractionResult.CONSUME;
        }
        if (state.getValue(STAGE) == Stage.ANCIENT && player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "capsule");
        }
        player.openMenu(capsule);
        return InteractionResult.CONSUME;
    }

    /** Versiegeln: Der Inhalt verlässt diese Zeit und wartet in der Gegenwart. */
    private static void seal(ServerLevel level, BlockPos pos, BlockState state, TimeCapsuleBlockEntity capsule, Player player) {
        Era era = Eras.byLevel(level.dimension());
        if (era == null || !era.isPast()) {
            player.displayClientMessage(Component.translatable("message.timemod.capsule.not_past").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        if (capsule.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.timemod.capsule.empty").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        CompoundTag data = new CompoundTag();
        NonNullList<ItemStack> contents = NonNullList.withSize(TimeCapsuleBlockEntity.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < TimeCapsuleBlockEntity.SIZE; i++) {
            contents.set(i, capsule.getItem(i).copy());
        }
        ContainerHelper.saveAllItems(data, contents, true);
        data.putString("Sender", player.getGameProfile().getName());
        capsule.clearContent();
        capsule.setChanged();
        level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.SEALED));
        Timeline.get(level.getServer()).add(level.getServer(), new Timeline.Entry(Timeline.Kind.CAPSULE, pos, -(long) era.years(), data));
        level.playSound(null, pos, ModSounds.GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 40, 0.3, 0.4, 0.3, 0.05);
        player.displayClientMessage(Component.translatable("message.timemod.capsule.sent").withStyle(ChatFormatting.AQUA), true);
    }

    /** Eine Kapsel, die die Jahre überdauert hat, schimmert leise – so entdeckt man sie auch im hohen Gras. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) != Stage.ANCIENT) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.9,
                pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TimeCapsuleBlockEntity capsule) {
            Containers.dropContents(level, pos, capsule);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}

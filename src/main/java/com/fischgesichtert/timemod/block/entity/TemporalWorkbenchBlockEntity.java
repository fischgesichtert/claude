package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.block.TemporalWorkbenchBlock;
import com.fischgesichtert.timemod.menu.TemporalWorkbenchMenu;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModSounds;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Hält das Werkzeug, an dem gerade gearbeitet wird (es schwebt über der Werkbank). */
public class TemporalWorkbenchBlockEntity extends BlockEntity {
    private static final Component TITLE = Component.translatable("container.timemod.temporal_workbench");
    private static final DustParticleOptions CHRONO_DUST = new DustParticleOptions(new Vector3f(0.45F, 0.9F, 1.0F), 1.0F);

    private final SimpleContainer tool = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            TemporalWorkbenchBlockEntity.this.setChanged();
            sync();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    @Nullable
    private UUID user;

    public TemporalWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPORAL_WORKBENCH, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TemporalWorkbenchBlockEntity self) {
        if (level.getGameTime() % 10 == 0) {
            boolean formed = TemporalWorkbenchBlock.isFormed(level, pos);
            if (formed != state.getValue(TemporalWorkbenchBlock.FORMED)) {
                level.setBlock(pos, state.setValue(TemporalWorkbenchBlock.FORMED, formed), Block.UPDATE_ALL);
                level.playSound(null, pos, formed ? ModSounds.TIME_RESUME : ModSounds.CLOCK_TICK, SoundSource.BLOCKS, 0.8F, formed ? 1.4F : 0.6F);
            }
        }
        if (state.getValue(TemporalWorkbenchBlock.FORMED) && level.getGameTime() % 4 == 0 && level instanceof ServerLevel serverLevel) {
            // Zeitfäden von einer Säulenspitze zur Werkbank
            BlockPos pillar = pos.offset(TemporalWorkbenchBlock.PILLARS[1 + 2 * level.random.nextInt(4)]);
            Vec3 from = Vec3.atCenterOf(pillar).add(0, 0.5, 0);
            Vec3 to = Vec3.atCenterOf(pos).add(0, 0.9, 0);
            double t = level.random.nextDouble();
            Vec3 at = from.lerp(to, t);
            serverLevel.sendParticles(CHRONO_DUST, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public ItemStack tool() {
        return tool.getItem(0);
    }

    public SimpleContainer toolContainer() {
        return tool;
    }

    /** Öffnet die Oberfläche – nur wenn der Multiblock steht und gerade niemand sonst daran arbeitet. */
    public void open(Player player) {
        if (level == null || !getBlockState().getValue(TemporalWorkbenchBlock.FORMED)) {
            player.displayClientMessage(Component.translatable("message.timemod.workbench.incomplete").withStyle(ChatFormatting.GOLD), true);
            showMissingPillars();
            return;
        }
        if (user != null && !user.equals(player.getUUID()) && level.getPlayerByUUID(user) instanceof ServerPlayer other
            && other.containerMenu instanceof TemporalWorkbenchMenu menu && menu.isFor(this)) {
            player.displayClientMessage(Component.translatable("message.timemod.workbench.busy").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        user = player.getUUID();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
            new TemporalWorkbenchMenu(id, inventory, tool, ContainerLevelAccess.create(level, worldPosition), this), TITLE));
    }

    public void release(Player player) {
        if (player.getUUID().equals(user)) {
            user = null;
        }
    }

    private void showMissingPillars() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (BlockPos offset : TemporalWorkbenchBlock.PILLARS) {
            BlockPos target = worldPosition.offset(offset);
            if (!TemporalWorkbenchBlock.isFormed(level, worldPosition) && !level.getBlockState(target).is(com.fischgesichtert.timemod.registry.ModBlocks.CHRONO_PILLAR)) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.0);
            }
        }
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, tool);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("Tool", tool.getItem(0).save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        tool.setItem(0, ItemStack.of(tag.getCompound("Tool")));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

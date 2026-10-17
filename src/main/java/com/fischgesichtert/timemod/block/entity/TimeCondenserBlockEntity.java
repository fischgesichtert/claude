package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Zeitkondensator: Kristalle → Chronon-Puffer → eingelegtes Werkzeug bzw. Zeitzelle. */
public class TimeCondenserBlockEntity extends BlockEntity {
    public static final int BUFFER_CAPACITY = 20_000;
    public static final int CHRONON_PER_CRYSTAL = 2_000;
    public static final int CHRONON_PER_LEVEL = 100;
    private static final int PRODUCE_PER_TICK = 50;
    private static final int CHARGE_PER_TICK = 100;

    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack charging = ItemStack.EMPTY;
    private int buffer;
    private int burn;

    public TimeCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_CONDENSER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TimeCondenserBlockEntity self) {
        boolean changed = false;
        if (self.burn <= 0 && !self.fuel.isEmpty() && self.buffer < BUFFER_CAPACITY) {
            self.fuel.shrink(1);
            self.burn = CHRONON_PER_CRYSTAL;
            changed = true;
        }
        if (self.burn > 0 && self.buffer < BUFFER_CAPACITY) {
            int produced = Math.min(PRODUCE_PER_TICK, Math.min(self.burn, BUFFER_CAPACITY - self.buffer));
            self.burn -= produced;
            self.buffer += produced;
            changed = true;
            if (level.getGameTime() % 10 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 2, 0.15, 0.05, 0.15, 0.01);
            }
        }
        if (!self.charging.isEmpty() && self.buffer > 0) {
            int moved = Chronon.insert(self.charging, Math.min(CHARGE_PER_TICK, self.buffer));
            if (moved > 0) {
                self.buffer -= moved;
                changed = true;
                if (level.getGameTime() % 20 == 0) {
                    level.playSound(null, pos, ModSounds.CLOCK_TICK, SoundSource.BLOCKS, 0.3F, 1.6F);
                }
            }
        }
        if (changed) {
            self.markChangedAndSync();
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.CHRONITE_CRYSTAL)) {
            if (fuel.isEmpty()) {
                fuel = held.split(held.getCount());
            } else if (fuel.getCount() < fuel.getMaxStackSize()) {
                int moved = Math.min(held.getCount(), fuel.getMaxStackSize() - fuel.getCount());
                fuel.grow(moved);
                held.shrink(moved);
            }
            markChangedAndSync();
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof ChrononHolder && charging.isEmpty()) {
            charging = held.split(1);
            markChangedAndSync();
            level.playSound(null, worldPosition, ModSounds.CLOCK_WIND, SoundSource.BLOCKS, 0.6F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown() && player.experienceLevel > 0 && buffer + CHRONON_PER_LEVEL <= BUFFER_CAPACITY) {
                player.giveExperienceLevels(-1);
                buffer += CHRONON_PER_LEVEL;
                markChangedAndSync();
                level.playSound(null, worldPosition, ModSounds.HEARTBEAT, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.CONSUME;
            }
            if (!charging.isEmpty()) {
                player.setItemInHand(hand, charging);
                charging = ItemStack.EMPTY;
            } else if (!fuel.isEmpty()) {
                player.setItemInHand(hand, fuel);
                fuel = ItemStack.EMPTY;
            } else {
                player.displayClientMessage(Component.translatable("message.timemod.condenser.status", Chronon.format(buffer), Chronon.format(BUFFER_CAPACITY))
                    .withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.CONSUME;
            }
            markChangedAndSync();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public ItemStack charging() {
        return charging;
    }

    public ItemStack fuel() {
        return fuel;
    }

    public int buffer() {
        return buffer;
    }

    public int comparatorSignal() {
        return Math.round(15.0F * buffer / BUFFER_CAPACITY);
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), charging);
            fuel = ItemStack.EMPTY;
            charging = ItemStack.EMPTY;
        }
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("Fuel", fuel.save(new CompoundTag()));
        tag.put("Charging", charging.save(new CompoundTag()));
        tag.putInt("Buffer", buffer);
        tag.putInt("Burn", burn);
    }

    @Override
    public void load(CompoundTag tag) {
        fuel = ItemStack.of(tag.getCompound("Fuel"));
        charging = ItemStack.of(tag.getCompound("Charging"));
        buffer = tag.getInt("Buffer");
        burn = tag.getInt("Burn");
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

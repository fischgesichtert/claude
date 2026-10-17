package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.block.TimeFieldProjectorBlock;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeAnchored;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Zeitfeld-Projektor: eine dauerhafte, würfelförmige Zeitzone um den Block, bezahlt mit Chronon. */
public class TimeFieldProjectorBlockEntity extends BlockEntity implements TimeAnchored {
    public static final double[] FACTORS = {2.0, 3.0, 4.0, 0.5, 0.25};
    public static final int[] RADII = {2, 3, 4, 6, 8};
    public static final int CAPACITY = 50_000;

    private int factorIndex;
    private int radiusIndex = 2;
    private int buffer;
    private double debt;
    private int zoneId = -1;

    public TimeFieldProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_FIELD_PROJECTOR, pos, state);
    }

    public double factor() {
        return FACTORS[Math.floorMod(factorIndex, FACTORS.length)];
    }

    public int radius() {
        return RADII[Math.floorMod(radiusIndex, RADII.length)];
    }

    /** Chronon pro Tick: wächst mit der Fläche und der Stärke der Zeitverschiebung. */
    public double costPerTick() {
        double r = radius();
        double strength = factor() >= 1 ? factor() - 1 : 1.0 / factor() - 1;
        return r * r * (0.5 + strength) * 0.04;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TimeFieldProjectorBlockEntity self) {
        boolean shouldRun = !level.hasNeighborSignal(pos) && self.buffer > 0;
        if (shouldRun) {
            self.debt += self.costPerTick();
            int pay = (int) self.debt;
            if (pay > 0) {
                self.debt -= pay;
                self.buffer = Math.max(0, self.buffer - pay);
                self.setChanged();
            }
            self.ensureZone((ServerLevel) level);
        } else {
            self.removeZone((ServerLevel) level);
        }
        if (state.getValue(TimeFieldProjectorBlock.ACTIVE) != shouldRun) {
            level.setBlock(pos, state.setValue(TimeFieldProjectorBlock.ACTIVE, shouldRun), Block.UPDATE_ALL);
        }
    }

    private void ensureZone(ServerLevel level) {
        ServerTimeEngine engine = ServerTimeEngine.of(level);
        if (zoneId >= 0 && engine.zone(zoneId) != null) {
            return;
        }
        int r = radius();
        AABB box = new AABB(worldPosition).inflate(r);
        TimeZone zone = engine.addZone(new ZoneShape.Box(box), factor(), TimeFilter.DEFAULT, null, -1);
        zoneId = zone.id();
    }

    private void removeZone(ServerLevel level) {
        if (zoneId >= 0) {
            ServerTimeEngine.of(level).removeZone(zoneId);
            zoneId = -1;
        }
    }

    public void shutdown() {
        if (level instanceof ServerLevel serverLevel) {
            removeZone(serverLevel);
        }
    }

    @Override
    public void setRemoved() {
        shutdown();
        super.setRemoved();
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ChrononHolder) {
            int moved = Chronon.extract(held, CAPACITY - buffer);
            buffer += moved;
            setChanged();
            level.playSound(null, worldPosition, ModSounds.CLOCK_WIND, SoundSource.BLOCKS, 0.6F, 1.0F);
            status(player);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            radiusIndex = Math.floorMod(radiusIndex + 1, RADII.length);
        } else {
            factorIndex = Math.floorMod(factorIndex + 1, FACTORS.length);
        }
        removeZone(serverLevel);
        setChanged();
        level.playSound(null, worldPosition, ModSounds.CLOCK_TICK, SoundSource.BLOCKS, 0.8F, 1.2F);
        status(player);
        return InteractionResult.CONSUME;
    }

    private void status(Player player) {
        player.displayClientMessage(Component.translatable("message.timemod.projector.status",
            String.format(Locale.ROOT, "%.2f", factor()), radius(), Chronon.format(buffer), String.format(Locale.ROOT, "%.1f", costPerTick() * 20))
            .withStyle(ChatFormatting.AQUA), true);
    }

    public int buffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = Math.max(0, Math.min(CAPACITY, buffer));
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("Factor", factorIndex);
        tag.putInt("Radius", radiusIndex);
        tag.putInt("Buffer", buffer);
    }

    @Override
    public void load(CompoundTag tag) {
        factorIndex = tag.getInt("Factor");
        radiusIndex = tag.contains("Radius") ? tag.getInt("Radius") : 2;
        buffer = tag.getInt("Buffer");
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

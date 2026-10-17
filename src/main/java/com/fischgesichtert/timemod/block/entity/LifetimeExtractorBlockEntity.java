package com.fischgesichtert.timemod.block.entity;

import com.fischgesichtert.timemod.block.LifetimeExtractorBlock;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.network.Aging;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModDamageTypes;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.time.TimeEngine;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Puffer und Arbeitslogik des Lebenszeit-Extraktors. */
public class LifetimeExtractorBlockEntity extends BlockEntity {
    public static final int BUFFER_CAPACITY = 50_000;
    /** Chronon je entzogenem Lebenspunkt: eine Kuh (10 Leben) bringt 250. */
    public static final int CHRONON_PER_HEALTH = 25;
    /** Alle wie viele Ticks ein Lebenspunkt entzogen wird. */
    public static final int INTERVAL = 20;
    private static final int CHARGE_PER_TICK = 100;

    private int buffer;
    private ItemStack charging = ItemStack.EMPTY;
    private int clock;

    public LifetimeExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIFETIME_EXTRACTOR, pos, state);
    }

    /** Wirkbereich: zwei Blöcke um den Extraktor, bis drei Blöcke hoch. */
    public AABB area() {
        return new AABB(worldPosition).inflate(2.0, 0.0, 2.0).expandTowards(0, 3.0, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LifetimeExtractorBlockEntity self) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean worked = false;
        if (!powered && ++self.clock >= INTERVAL) {
            self.clock = 0;
            worked = self.drain((ServerLevel) level);
        }
        if (!self.charging.isEmpty() && self.buffer > 0) {
            int moved = Chronon.insert(self.charging, Math.min(CHARGE_PER_TICK, self.buffer));
            if (moved > 0) {
                self.buffer -= moved;
                self.setChanged();
            }
        }
        if (self.clock == 0 && state.getValue(LifetimeExtractorBlock.ACTIVE) != worked) {
            level.setBlock(pos, state.setValue(LifetimeExtractorBlock.ACTIVE, worked), Block.UPDATE_ALL);
        }
    }

    private boolean drain(ServerLevel level) {
        if (buffer >= BUFFER_CAPACITY) {
            return false;
        }
        boolean any = false;
        DamageSource oldAge = ModDamageTypes.oldAge(level);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area(), m -> m.isAlive() && !TimeEngine.isOutsideTime(m))) {
            if (mob.isInvulnerableTo(oldAge)) {
                continue;
            }
            float before = mob.getHealth();
            if (before > 1.0F) {
                // Altern tut nicht weh: kein Treffer-Aufblitzen, nur weniger Lebenszeit
                mob.setHealth(before - 1.0F);
            } else if (!mob.hurt(oldAge, before + 100.0F)) {
                continue;
            }
            float drained = Math.max(0.0F, before - Math.max(0.0F, mob.getHealth()));
            buffer = Math.min(BUFFER_CAPACITY, buffer + Math.round(drained * CHRONON_PER_HEALTH));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
            float age = 1.0F - Math.max(0.0F, mob.getHealth()) / mob.getMaxHealth();
            Aging.send(mob, age);
            level.sendParticles(ParticleTypes.WHITE_ASH, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 12,
                mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.3, mob.getBbWidth() * 0.4, 0.01);
            any = true;
        }
        if (any) {
            setChanged();
            level.playSound(null, worldPosition, ModSounds.CLOCK_TICK, SoundSource.BLOCKS, 0.4F, 0.5F);
        }
        return any;
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ChrononHolder && charging.isEmpty()) {
            charging = held.split(1);
            setChanged();
            level.playSound(null, worldPosition, ModSounds.CLOCK_WIND, SoundSource.BLOCKS, 0.6F, 0.8F);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            if (!charging.isEmpty()) {
                player.setItemInHand(hand, charging);
                charging = ItemStack.EMPTY;
                setChanged();
            } else {
                player.displayClientMessage(Component.translatable("message.timemod.extractor.status", Chronon.format(buffer),
                    Chronon.format(BUFFER_CAPACITY)).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public int buffer() {
        return buffer;
    }

    public ItemStack charging() {
        return charging;
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), charging);
            charging = ItemStack.EMPTY;
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("Buffer", buffer);
        tag.put("Charging", charging.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        buffer = tag.getInt("Buffer");
        charging = ItemStack.of(tag.getCompound("Charging"));
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

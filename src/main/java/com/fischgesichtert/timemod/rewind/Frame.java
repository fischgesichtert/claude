package com.fischgesichtert.timemod.rewind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Alles, was in einem echten Tick passiert ist: Ereignisse in Reihenfolge (zum Rückgängigmachen)
 * und der Zustand am Ende des Ticks (zum Wiederherstellen).
 */
final class Frame {
    final long index;
    long dayTime;

    /** Blockänderungen in der Reihenfolge, in der sie passiert sind (alter Zustand). */
    final List<BlockChange> blockChanges = new ArrayList<>();
    /** Entstandene und entfernte Entities in Reihenfolge. */
    final List<EntityEvent> entityEvents = new ArrayList<>();
    /** Block-Entities, deren Daten sich geändert haben: Stand am Ende des Ticks. */
    final Map<BlockPos, CompoundTag> blockEntityAfter = new HashMap<>();
    /** Position und Lebenswerte aller aufgezeichneten Entities am Ende des Ticks. */
    final Map<UUID, Pose> poses = new HashMap<>();
    /** Vollständige Entity-Daten (alle 20 Ticks), für alles, was keine Pose ist. */
    final Map<UUID, CompoundTag> keyframes = new HashMap<>();
    /** Spieler: Inventar, Endertruhe und Effekte – nur wenn sie sich geändert haben. */
    final Map<UUID, PlayerItems> playerItems = new HashMap<>();
    /** Spieler: Hunger, Erfahrung, gewählter Platz – jeden Tick. */
    final Map<UUID, PlayerVitals> playerVitals = new HashMap<>();

    Frame(long index) {
        this.index = index;
    }

    record BlockChange(BlockPos pos, BlockState oldState, @Nullable CompoundTag oldBlockEntity) {
    }

    /** {@code removedState == null}: Entity ist entstanden; sonst wurde sie mit diesem Zustand entfernt. */
    record EntityEvent(UUID uuid, @Nullable CompoundTag removedState, Vec3 pos) {
        boolean spawned() {
            return removedState == null;
        }
    }

    record Pose(double x, double y, double z, float yRot, float xRot, float headRot, float bodyRot,
                double vx, double vy, double vz, boolean onGround, float fallDistance,
                float health, int fire, int air) {
    }

    record PlayerItems(ListTag inventory, ListTag enderChest, ListTag effects) {
    }

    record PlayerVitals(int food, float saturation, int xpLevel, float xpProgress, int totalXp, int selected) {
    }
}

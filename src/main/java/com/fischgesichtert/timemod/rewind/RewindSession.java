package com.fischgesichtert.timemod.rewind;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Spielt das Zeitjournal rückwärts ab. Jeder Schritt macht einen Tick rückgängig: Blockänderungen
 * in umgekehrter Reihenfolge, Entstandenes verschwindet, Verschwundenes kehrt zurück (mit derselben
 * UUID), und alle Entities und Spieler nehmen ihre Position vom Tick davor ein – so laufen sie
 * ihre Wege sichtbar rückwärts.
 */
public final class RewindSession {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final ChronoJournal journal;
    private final ServerLevel level;
    private final int totalSteps;
    private final int speed;
    @Nullable
    private final Region region;
    private int done;
    private boolean finished;
    @Nullable
    private TimeZone zone;
    private final Set<BlockPos> touched = new HashSet<>();
    private final List<Runnable> onFinish = new java.util.ArrayList<>();

    /** Bereich, auf den das Zurückspulen wirkt. */
    public record Region(Vec3 center, double radius) {
        boolean contains(double x, double y, double z) {
            return center.distanceToSqr(x, y, z) <= radius * radius;
        }

        boolean contains(BlockPos pos) {
            return contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }

    RewindSession(ChronoJournal journal, ServerLevel level, int steps, int speed, @Nullable Region region, @Nullable UUID owner) {
        this.journal = journal;
        this.level = level;
        this.totalSteps = steps;
        this.speed = speed;
        this.region = region;
        if (speed > 0) {
            ZoneShape shape = region != null ? new ZoneShape.Sphere(region.center(), region.radius()) : ZoneShape.Global.INSTANCE;
            int mask = TimeFilter.DEFAULT | TimeFilter.PLAYERS;
            zone = ServerTimeEngine.of(level).addRewindZone(shape, mask, owner);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    /** Wird direkt nach dem Abschluss ausgeführt (bevor die Welt weitertickt). */
    public void onFinish(Runnable action) {
        if (finished) {
            action.run();
        } else {
            onFinish.add(action);
        }
    }

    /** Fortschritt 0..1 (für Anzeigen). */
    public float progress() {
        return totalSteps == 0 ? 1.0F : (float) done / totalSteps;
    }

    void tick() {
        for (int i = 0; i < speed && !finished; i++) {
            step();
        }
    }

    void runToEnd() {
        while (!finished) {
            step();
        }
    }

    private boolean inRegion(double x, double y, double z) {
        return region == null || region.contains(x, y, z);
    }

    private void step() {
        Frame frame = journal.frames().pollLast();
        if (frame == null || done >= totalSteps) {
            finish();
            return;
        }
        journal.beginReplay();
        try {
            undo(frame);
            Frame previous = journal.frames().peekLast();
            if (previous != null) {
                applyPoses(previous, false);
            }
        } finally {
            journal.endReplay();
        }
        done++;
        if (done >= totalSteps || journal.frames().isEmpty()) {
            finish();
        }
    }

    private void undo(Frame frame) {
        // 1) Blockänderungen in umgekehrter Reihenfolge
        List<Frame.BlockChange> changes = frame.blockChanges;
        for (int i = changes.size() - 1; i >= 0; i--) {
            Frame.BlockChange change = changes.get(i);
            if (region != null && !region.contains(change.pos())) {
                continue;
            }
            level.setBlock(change.pos(), change.oldState(), SET_FLAGS);
            if (change.oldBlockEntity() != null) {
                loadBlockEntity(change.pos(), change.oldBlockEntity());
            }
            touched.add(change.pos());
        }
        // 2) Block-Entities, deren Daten sich in diesem Tick geändert haben
        for (BlockPos pos : frame.blockEntityAfter.keySet()) {
            if (region != null && !region.contains(pos)) {
                continue;
            }
            CompoundTag before = journal.blockEntityState(pos);
            if (before != null) {
                loadBlockEntity(pos, before);
            }
        }
        // 3) Entities: Entstandenes verschwindet, Entferntes kehrt zurück
        List<Frame.EntityEvent> events = frame.entityEvents;
        for (int i = events.size() - 1; i >= 0; i--) {
            Frame.EntityEvent event = events.get(i);
            if (!inRegion(event.pos().x, event.pos().y, event.pos().z)) {
                continue;
            }
            if (event.spawned()) {
                Entity entity = level.getEntity(event.uuid());
                if (entity != null && !(entity instanceof ServerPlayer)) {
                    entity.discard();
                }
            } else if (level.getEntity(event.uuid()) == null) {
                revive(event);
            }
        }
        // 4) Spieler-Inventare
        for (Map.Entry<UUID, Frame.PlayerItems> entry : frame.playerItems.entrySet()) {
            if (level.getEntity(entry.getKey()) instanceof ServerPlayer player && inRegion(player.getX(), player.getY(), player.getZ())) {
                Frame.PlayerItems before = journal.playerItems(entry.getKey());
                if (before != null) {
                    applyItems(player, before);
                }
            }
        }
    }

    private void loadBlockEntity(BlockPos pos, CompoundTag tag) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.load(tag);
            blockEntity.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void revive(Frame.EntityEvent event) {
        // Wurde die Entity in eine andere Dimension gebracht, dort entfernen (sonst gäbe es sie doppelt)
        for (ServerLevel other : level.getServer().getAllLevels()) {
            if (other != level) {
                Entity copy = other.getEntity(event.uuid());
                if (copy != null) {
                    copy.discard();
                }
            }
        }
        Entity entity = EntityType.loadEntityRecursive(event.removedState().copy(), level, e -> e);
        if (entity == null) {
            TimeMod.LOGGER.warn("Zeitjournal: Entity {} konnte nicht wiederhergestellt werden", event.uuid());
            return;
        }
        if (entity instanceof LivingEntity living) {
            living.deathTime = 0;
            living.setHealth(Math.max(living.getHealth(), 0.5F));
        }
        level.addFreshEntity(entity);
    }

    /** Alle aufgezeichneten Entities (und Spieler) an ihre Position aus {@code frame} setzen. */
    private void applyPoses(Frame frame, boolean exact) {
        for (Map.Entry<UUID, Frame.Pose> entry : frame.poses.entrySet()) {
            Frame.Pose pose = entry.getValue();
            if (!inRegion(pose.x(), pose.y(), pose.z())) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            if (entity instanceof ServerPlayer player) {
                player.connection.teleport(pose.x(), pose.y(), pose.z(), pose.yRot(), pose.xRot());
                player.setHealth(pose.health());
                Frame.PlayerVitals vitals = frame.playerVitals.get(entry.getKey());
                if (vitals != null) {
                    applyVitals(player, vitals);
                }
                continue;
            }
            entity.setPos(pose.x(), pose.y(), pose.z());
            entity.setYRot(pose.yRot());
            entity.setXRot(pose.xRot());
            entity.setYHeadRot(pose.headRot());
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = pose.bodyRot();
                if (living.isAlive() || pose.health() > 0) {
                    living.setHealth(pose.health());
                }
            }
            entity.setOnGround(pose.onGround());
            entity.fallDistance = pose.fallDistance();
            entity.setRemainingFireTicks(pose.fire());
            entity.setAirSupply(pose.air());
            entity.setDeltaMovement(exact ? new Vec3(pose.vx(), pose.vy(), pose.vz()) : Vec3.ZERO);
            entity.hasImpulse = true;
        }
        if (region == null) {
            level.setDayTime(frame.dayTime);
        }
    }

    private static void applyVitals(ServerPlayer player, Frame.PlayerVitals vitals) {
        player.getFoodData().setFoodLevel(vitals.food());
        player.getFoodData().setSaturation(vitals.saturation());
        player.experienceLevel = vitals.xpLevel();
        player.experienceProgress = vitals.xpProgress();
        player.totalExperience = vitals.totalXp();
        player.getInventory().selected = vitals.selected();
        player.resetSentInfo();
    }

    private static void applyItems(ServerPlayer player, Frame.PlayerItems items) {
        player.getInventory().load(items.inventory());
        player.getEnderChestInventory().fromTag(items.enderChest());
        player.removeAllEffects();
        for (Tag tag : items.effects()) {
            MobEffectInstance effect = MobEffectInstance.load((CompoundTag) tag);
            if (effect != null) {
                player.addEffect(effect);
            }
        }
        player.inventoryMenu.broadcastChanges();
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        journal.beginReplay();
        try {
            Frame target = journal.frames().peekLast();
            if (target != null) {
                // Innere Zustände (KI, Inventare von Mobs …) aus dem letzten Schlüsselbild ≤ Ziel
                for (UUID uuid : target.poses.keySet()) {
                    Entity entity = level.getEntity(uuid);
                    if (entity == null || entity instanceof ServerPlayer) {
                        continue;
                    }
                    CompoundTag state = journal.entityState(uuid);
                    if (state != null && inRegion(entity.getX(), entity.getY(), entity.getZ())) {
                        CompoundTag copy = state.copy();
                        copy.remove("id");
                        entity.load(copy);
                    }
                }
                applyPoses(target, true);
            }
            // Geplante Ticks neu anstoßen: Wasser fließt weiter, Sand fällt, Redstone prüft sich
            for (BlockPos pos : touched) {
                BlockState state = level.getBlockState(pos);
                FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty()) {
                    level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
                }
                if (state.getBlock() instanceof FallingBlock) {
                    level.scheduleTick(pos, state.getBlock(), 2);
                } else if (!state.isAir()) {
                    level.neighborChanged(pos, state.getBlock(), pos);
                }
            }
        } finally {
            journal.endReplay();
        }
        if (zone != null) {
            ServerTimeEngine.of(level).removeZone(zone.id());
        }
        journal.restartAfterRewind();
        TimeMod.LOGGER.info("Zurückgespult: {} Ticks", done);
        onFinish.forEach(Runnable::run);
        onFinish.clear();
    }
}

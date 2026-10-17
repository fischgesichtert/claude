package com.fischgesichtert.timemod.rewind;

import com.fischgesichtert.timemod.TimeMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Das Zeitjournal einer Welt: ein Ringpuffer der letzten Sekunden (Standard 60 s).
 * <p>
 * Pro echtem Tick wird ein {@link Frame} geschrieben: Blockänderungen und Entstehen/Verschwinden
 * von Entities als Ereignisse in Reihenfolge, Positionen, Lebenswerte und Inventare als Zustand am
 * Tick-Ende. Blockänderungen werden direkt in {@code LevelChunk#setBlockState} abgegriffen und
 * erfassen damit alles – Abbauen, Bauen, Wachstum, Wasser, Feuer, Explosionen, andere Mods.
 */
public final class ChronoJournal {
    public static final int DEFAULT_SECONDS = 60;
    private static final int KEYFRAME_INTERVAL = 20;

    private final ServerLevel level;
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private int capacity = DEFAULT_SECONDS * 20;
    private boolean recording;
    private int replaying;
    private long nextIndex;
    private Frame open;

    /** Aufgezeichneter Bereich: um Spieler herum und um feste Anker (z. B. Maschinen, Tests). */
    private double playerRadius = 96;
    private final List<Anchor> anchors = new ArrayList<>();

    /** Letzter bekannter Stand vor dem ältesten Frame. */
    private final Map<BlockPos, CompoundTag> blockEntityBaseline = new HashMap<>();
    private final Map<UUID, CompoundTag> entityBaseline = new HashMap<>();
    private final Map<UUID, Frame.PlayerItems> playerBaseline = new HashMap<>();
    private final LongSet baselinedChunks = new LongOpenHashSet();

    private final Set<BlockPos> dirtyBlockEntities = new HashSet<>();
    private final Map<BlockPos, Integer> blockEntityHashes = new HashMap<>();
    private final Map<UUID, Integer> playerItemHashes = new HashMap<>();

    @Nullable
    private RewindSession session;

    private record Anchor(Vec3 center, double radius) {
    }

    public ChronoJournal(ServerLevel level) {
        this.level = level;
    }

    // ------------------------------------------------------------------
    // Steuerung
    // ------------------------------------------------------------------

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean on) {
        if (on == recording) {
            return;
        }
        recording = on;
        clear();
        if (on) {
            open = new Frame(nextIndex++);
            baselineChunks();
            TimeMod.LOGGER.info("Zeitjournal in {}: Aufzeichnung läuft ({} s)", level.dimension().location(), capacity / 20);
        }
    }

    public void setSeconds(int seconds) {
        capacity = Math.max(20, seconds * 20);
        while (frames.size() > capacity) {
            evict();
        }
    }

    public void addAnchor(Vec3 center, double radius) {
        anchors.add(new Anchor(center, radius));
        if (recording) {
            baselineChunks();
        }
    }

    public void clearAnchors() {
        anchors.clear();
    }

    public void setPlayerRadius(double radius) {
        playerRadius = radius;
    }

    /** Wie viele vollständige Ticks gerade zurückgespult werden können. */
    public int availableTicks() {
        return frames.size();
    }

    public boolean isReplaying() {
        return replaying > 0;
    }

    @Nullable
    public RewindSession session() {
        return session;
    }

    private void clear() {
        frames.clear();
        open = null;
        blockEntityBaseline.clear();
        entityBaseline.clear();
        playerBaseline.clear();
        baselinedChunks.clear();
        dirtyBlockEntities.clear();
        blockEntityHashes.clear();
        playerItemHashes.clear();
    }

    /** Nach einem Zurückspulen beginnt die Geschichte an diesem Punkt neu. */
    void restartAfterRewind() {
        boolean wasRecording = recording;
        recording = false;
        clear();
        if (wasRecording) {
            setRecording(true);
        }
    }

    // ------------------------------------------------------------------
    // Bereich
    // ------------------------------------------------------------------

    public boolean isRecorded(double x, double y, double z) {
        for (Anchor anchor : anchors) {
            if (anchor.center.distanceToSqr(x, y, z) <= anchor.radius * anchor.radius) {
                return true;
            }
        }
        double r2 = playerRadius * playerRadius;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecorded(BlockPos pos) {
        return isRecorded(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private boolean active() {
        return recording && replaying == 0 && open != null;
    }

    // ------------------------------------------------------------------
    // Ereignisse (aus Mixins)
    // ------------------------------------------------------------------

    /** Vor jeder Blockänderung: alter Zustand samt Block-Entity-Daten. */
    public void onBlockChange(LevelChunk chunk, BlockPos pos, BlockState newState) {
        if (!active()) {
            return;
        }
        BlockState old = chunk.getBlockState(pos);
        if (old == newState || !isRecorded(pos)) {
            return;
        }
        CompoundTag oldBlockEntity = null;
        if (old.hasBlockEntity()) {
            BlockEntity blockEntity = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
            if (blockEntity != null) {
                oldBlockEntity = blockEntity.saveWithFullMetadata();
            }
        }
        open.blockChanges.add(new Frame.BlockChange(pos.immutable(), old, oldBlockEntity));
    }

    public void onBlockEntityChanged(BlockPos pos) {
        if (active() && isRecorded(pos)) {
            dirtyBlockEntities.add(pos.immutable());
        }
    }

    public void onEntitySpawned(Entity entity) {
        if (!active() || entity instanceof Player || !isRecorded(entity.getX(), entity.getY(), entity.getZ())) {
            return;
        }
        open.entityEvents.add(new Frame.EntityEvent(entity.getUUID(), null, entity.position()));
    }

    public void onEntityRemoved(Entity entity, Entity.RemovalReason reason) {
        if (!active() || entity instanceof Player || !isRecorded(entity.getX(), entity.getY(), entity.getZ())) {
            return;
        }
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED && reason != Entity.RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        CompoundTag state = fullState(entity);
        if (state != null) {
            open.entityEvents.add(new Frame.EntityEvent(entity.getUUID(), state, entity.position()));
        }
    }

    @Nullable
    static CompoundTag fullState(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!entity.getType().canSerialize()) {
            return null;
        }
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        tag.putString("id", key.toString());
        return tag;
    }

    // ------------------------------------------------------------------
    // Tick-Grenzen
    // ------------------------------------------------------------------

    /** Am Anfang eines echten Ticks: laufende Rückspul-Wiedergabe fortsetzen. */
    public void beginRealTick() {
        if (session != null) {
            session.tick();
            if (session.isFinished()) {
                session = null;
            }
        }
    }

    /** Am Ende eines echten Ticks: Zustand festhalten und den Frame abschließen. */
    public void endRealTick() {
        if (!recording || session != null || open == null) {
            return;
        }
        Frame frame = open;
        captureState(frame);
        frames.addLast(frame);
        while (frames.size() > capacity) {
            evict();
        }
        open = new Frame(nextIndex++);
        baselineChunks();
    }

    private void captureState(Frame frame) {
        frame.dayTime = level.getDayTime();
        for (BlockPos pos : dirtyBlockEntities) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                CompoundTag tag = blockEntity.saveWithFullMetadata();
                Integer last = blockEntityHashes.put(pos, tag.hashCode());
                if (last == null || last != tag.hashCode()) {
                    frame.blockEntityAfter.put(pos, tag);
                }
            }
        }
        dirtyBlockEntities.clear();
        boolean keyframe = frame.index % KEYFRAME_INTERVAL == 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.isRemoved() || !isRecorded(entity.getX(), entity.getY(), entity.getZ())) {
                continue;
            }
            frame.poses.put(entity.getUUID(), pose(entity));
            if (entity instanceof ServerPlayer player) {
                capturePlayer(frame, player);
            } else if (keyframe) {
                CompoundTag state = fullState(entity);
                if (state != null) {
                    frame.keyframes.put(entity.getUUID(), state);
                }
            }
        }
    }

    static Frame.Pose pose(Entity entity) {
        float headRot = entity.getYHeadRot();
        float bodyRot = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
        float health = entity instanceof LivingEntity living ? living.getHealth() : 0.0F;
        Vec3 motion = entity.getDeltaMovement();
        return new Frame.Pose(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(), headRot, bodyRot,
            motion.x, motion.y, motion.z, entity.onGround(), entity.fallDistance, health, entity.getRemainingFireTicks(), entity.getAirSupply());
    }

    private void capturePlayer(Frame frame, ServerPlayer player) {
        frame.playerVitals.put(player.getUUID(), new Frame.PlayerVitals(player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(),
            player.experienceLevel, player.experienceProgress, player.totalExperience, player.getInventory().selected));
        ListTag inventory = player.getInventory().save(new ListTag());
        ListTag ender = player.getEnderChestInventory().createTag();
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(effect.save(new CompoundTag()));
        }
        int hash = inventory.hashCode() * 31 * 31 + ender.hashCode() * 31 + effects.hashCode();
        Integer last = playerItemHashes.put(player.getUUID(), hash);
        if (last == null || last != hash) {
            frame.playerItems.put(player.getUUID(), new Frame.PlayerItems(inventory, ender, effects));
        }
    }

    /** Ältesten Frame verwerfen; sein Stand wird zur neuen Grundlinie. */
    private void evict() {
        Frame oldest = frames.pollFirst();
        if (oldest == null) {
            return;
        }
        blockEntityBaseline.putAll(oldest.blockEntityAfter);
        entityBaseline.putAll(oldest.keyframes);
        playerBaseline.putAll(oldest.playerItems);
        for (Frame.EntityEvent event : oldest.entityEvents) {
            if (event.spawned()) {
                continue;
            }
            entityBaseline.remove(event.uuid());
        }
    }

    /** Block-Entities in neu erfassten Chunks einmal sichern, damit ihr Ausgangszustand bekannt ist. */
    private void baselineChunks() {
        List<Vec3> centers = new ArrayList<>();
        List<Double> radii = new ArrayList<>();
        for (Anchor anchor : anchors) {
            centers.add(anchor.center);
            radii.add(anchor.radius);
        }
        for (ServerPlayer player : level.players()) {
            centers.add(player.position());
            radii.add(playerRadius);
        }
        for (int i = 0; i < centers.size(); i++) {
            Vec3 c = centers.get(i);
            int r = (int) Math.ceil(radii.get(i) / 16.0);
            int cx = ((int) Math.floor(c.x)) >> 4;
            int cz = ((int) Math.floor(c.z)) >> 4;
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    long key = ChunkPos.asLong(x, z);
                    if (baselinedChunks.contains(key)) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null) {
                        continue;
                    }
                    baselinedChunks.add(key);
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        blockEntityBaseline.putIfAbsent(blockEntity.getBlockPos().immutable(), blockEntity.saveWithFullMetadata());
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Zurückspulen
    // ------------------------------------------------------------------

    /**
     * Startet das Zurückspulen um {@code ticks} echte Ticks. {@code speed} = Ticks pro Tick
     * (0 = sofort). {@code region} begrenzt die Wirkung (null = ganzer aufgezeichneter Bereich).
     */
    @Nullable
    public RewindSession rewind(int ticks, int speed, @Nullable RewindSession.Region region, @Nullable UUID owner) {
        if (!recording || session != null || open == null) {
            return null;
        }
        // Der laufende (unvollständige) Tick wird immer zuerst rückgängig gemacht
        captureState(open);
        frames.addLast(open);
        open = null;
        int count = Math.min(ticks, frames.size() - 1);
        session = new RewindSession(this, level, count + 1, speed, region, owner);
        if (speed <= 0) {
            session.runToEnd();
            session = null;
        }
        return session;
    }

    ArrayDeque<Frame> frames() {
        return frames;
    }

    void beginReplay() {
        replaying++;
    }

    void endReplay() {
        replaying--;
    }

    /** Neuester bekannter Stand einer Block-Entity in den verbleibenden Frames (oder Grundlinie). */
    @Nullable
    CompoundTag blockEntityState(BlockPos pos) {
        Iterator<Frame> it = frames.descendingIterator();
        while (it.hasNext()) {
            CompoundTag tag = it.next().blockEntityAfter.get(pos);
            if (tag != null) {
                return tag;
            }
        }
        return blockEntityBaseline.get(pos);
    }

    @Nullable
    CompoundTag entityState(UUID uuid) {
        Iterator<Frame> it = frames.descendingIterator();
        while (it.hasNext()) {
            CompoundTag tag = it.next().keyframes.get(uuid);
            if (tag != null) {
                return tag;
            }
        }
        return entityBaseline.get(uuid);
    }

    @Nullable
    Frame.PlayerItems playerItems(UUID uuid) {
        Iterator<Frame> it = frames.descendingIterator();
        while (it.hasNext()) {
            Frame.PlayerItems items = it.next().playerItems.get(uuid);
            if (items != null) {
                return items;
            }
        }
        return playerBaseline.get(uuid);
    }
}

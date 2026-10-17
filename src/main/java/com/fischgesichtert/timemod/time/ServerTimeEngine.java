package com.fischgesichtert.timemod.time;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.network.TimeNetworking;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.Nullable;

/**
 * Die Zeit-Engine einer Server-Welt.
 *
 * <h2>Ablauf pro echtem Tick</h2>
 * <ol>
 *   <li>Eine volle globale Zone legt fest, wie viele Welt-Schritte es gibt (0 = eingefrorener
 *       Durchlauf, in dem nur Spieler und Netzwerk weiterlaufen; 2+ = zusätzliche komplette
 *       Welt-Ticks, in denen Spieler ausgelassen werden).</li>
 *   <li>In jedem Welt-Schritt bekommt jede lokale Zone ihre Anzahl Spiel-Ticks. Im normalen
 *       Vanilla-Durchlauf tickt ihr Inhalt einmal (oder gar nicht bei 0), alle weiteren Ticks
 *       laufen am Ende als Teilschritte in Vanilla-Reihenfolge: geplante Block-Ticks →
 *       Flüssigkeiten → Zufalls-Ticks → Block-Events → Entities → Block-Entities.</li>
 * </ol>
 * Dadurch ist 2× exakt dasselbe wie zwei normale Ticks – ein schneller abgespieltes Video.
 */
public final class ServerTimeEngine extends TimeEngine {
    private final ServerLevel level;
    private int nextId = 1;

    private boolean ticking;
    private boolean inExtraPass;
    private boolean frozenPass;
    private int globalSteps = 1;
    private int substepDepth;
    private boolean entitiesTicked;
    private boolean routingDisabled;
    private boolean dirty;
    private BooleanSupplier hasTime = () -> true;

    private final List<Runnable> pending = new ArrayList<>();
    private final List<TimeZone> active = new ArrayList<>();
    private final ChronoJournal journal;

    public ServerTimeEngine(ServerLevel level) {
        super(level);
        this.level = level;
        this.journal = new ChronoJournal(level);
    }

    /** Das Zeitjournal dieser Welt (Aufzeichnung fürs Zurückspulen). */
    public ChronoJournal journal() {
        return journal;
    }

    public static ServerTimeEngine of(ServerLevel level) {
        return (ServerTimeEngine) ((TimeLevel) level).timemod$getEngine();
    }

    // ------------------------------------------------------------------
    // Zonen verwalten
    // ------------------------------------------------------------------

    /** Legt eine Zone an. Während die Welt tickt, wird sie zum nächsten Tick wirksam. */
    public TimeZone addZone(ZoneShape shape, double factor, int mask, @Nullable UUID owner, int durationTicks) {
        TimeZone zone = new TimeZone(nextId++, shape, factor, mask, owner, durationTicks, true);
        runOrDefer(() -> {
            zone.setClock(level.getLevelData().getGameTime());
            List<TimeZone> replaced = new ArrayList<>();
            if (zone.isFullGlobal()) {
                for (TimeZone existing : zones) {
                    if (existing.isFullGlobal()) {
                        replaced.add(existing);
                    }
                }
                zones.removeAll(replaced);
            }
            zones.add(zone);
            onZonesChanged(replaced);
            playZoneSound(zone, true);
        });
        return zone;
    }

    /** Zone, in der gerade zurückgespult wird: alles steht, die Wiedergabe bewegt es. */
    public TimeZone addRewindZone(ZoneShape shape, int mask, @Nullable UUID owner) {
        return addRewindZone(shape, mask, owner, false);
    }

    public TimeZone addRewindZone(ZoneShape shape, int mask, @Nullable UUID owner, boolean ownerFree) {
        TimeZone zone = new TimeZone(nextId++, shape, 0.0, mask, owner, -1, true, true);
        if (ownerFree) {
            zone.freeOwner();
        }
        runOrDefer(() -> {
            zone.setClock(level.getLevelData().getGameTime());
            zones.add(zone);
            onZonesChanged(List.of());
            level.playSound(null, zone.shape().bounds() == null ? level.getSharedSpawnPos() : BlockPos.containing(zone.shape().bounds().getCenter()),
                ModSounds.REWIND, SoundSource.PLAYERS, 3.0F, 1.0F);
        });
        return zone;
    }

    public void removeZone(int id) {
        runOrDefer(() -> {
            TimeZone zone = zone(id);
            if (zone != null) {
                zones.remove(zone);
                onZonesChanged(List.of(zone));
            }
        });
    }

    public void removeZonesOwnedBy(UUID owner) {
        runOrDefer(() -> {
            List<TimeZone> removed = new ArrayList<>();
            for (TimeZone zone : zones) {
                if (owner.equals(zone.owner())) {
                    removed.add(zone);
                }
            }
            if (!removed.isEmpty()) {
                zones.removeAll(removed);
                onZonesChanged(removed);
            }
        });
    }

    public void clear() {
        runOrDefer(() -> {
            if (!zones.isEmpty()) {
                List<TimeZone> removed = new ArrayList<>(zones);
                zones.clear();
                onZonesChanged(removed);
            }
        });
    }

    private void runOrDefer(Runnable action) {
        if (ticking) {
            pending.add(action);
        } else {
            action.run();
        }
    }

    private void applyPending() {
        if (!pending.isEmpty()) {
            List<Runnable> actions = new ArrayList<>(pending);
            pending.clear();
            actions.forEach(Runnable::run);
        }
    }

    private void onZonesChanged(Collection<TimeZone> removed) {
        recomputeFullGlobal();
        rebalanceTicks(removed);
        dirty = true;
        for (TimeZone zone : removed) {
            playZoneSound(zone, false);
        }
    }

    /** Klang beim Erzeugen bzw. Auflösen einer Zone: Zeitstopp, Weiterlaufen, Vorspulen, Zeitlupe. */
    private void playZoneSound(TimeZone zone, boolean created) {
        SoundEvent sound;
        if (zone.isRewind()) {
            return;
        }
        if (zone.isStop()) {
            sound = created ? ModSounds.TIME_STOP : ModSounds.TIME_RESUME;
        } else if (!created) {
            return;
        } else {
            sound = zone.factor() > 1.0 ? ModSounds.FAST_FORWARD : ModSounds.SLOW_MOTION;
        }
        if (zone.shape().isGlobal()) {
            for (ServerPlayer player : level.players()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return;
        }
        AABB bounds = zone.shape().bounds();
        double x = (bounds.minX + bounds.maxX) / 2;
        double y = (bounds.minY + bounds.maxY) / 2;
        double z = (bounds.minZ + bounds.maxZ) / 2;
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, 3.0F, 1.0F);
    }

    /** Mitwandernde Zonen, deren Entity verschwunden ist, lösen sich auf. */
    private void dropOrphanedFollowers() {
        List<TimeZone> orphaned = null;
        for (TimeZone zone : zones) {
            if (zone.isTargeted() && (zone.targetUuid() == null || level.getEntity(zone.targetUuid()) == null)) {
                if (orphaned == null) {
                    orphaned = new ArrayList<>();
                }
                orphaned.add(zone);
            }
        }
        if (orphaned != null) {
            zones.removeAll(orphaned);
            onZonesChanged(orphaned);
        }
    }

    /** Zone, die nur auf eine Entity wirkt und ihr folgt (Bremsfeder, Moment-Linse). */
    public TimeZone addEntityZone(Entity target, double factor, int mask, @Nullable UUID owner, int durationTicks) {
        double radius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.75 + 0.25;
        TimeZone zone = new TimeZone(nextId++, new ZoneShape.Sphere(target.getBoundingBox().getCenter(), radius), factor, mask, owner, durationTicks, true).target(target);
        runOrDefer(() -> {
            zone.setClock(level.getLevelData().getGameTime());
            zones.add(zone);
            onZonesChanged(List.of());
            playZoneSound(zone, true);
        });
        return zone;
    }

    private void expireZones() {
        List<TimeZone> expired = null;
        for (TimeZone zone : zones) {
            if (zone.countDown()) {
                if (expired == null) {
                    expired = new ArrayList<>();
                }
                expired.add(zone);
            }
        }
        if (expired != null) {
            zones.removeAll(expired);
            onZonesChanged(expired);
        }
    }

    public void markDirty() {
        dirty = true;
    }

    // ------------------------------------------------------------------
    // Tick-Ablauf (aus ServerLevel#tick)
    // ------------------------------------------------------------------

    public void onLevelTickStart(BooleanSupplier hasTime) {
        if (inExtraPass) {
            beginPass(false);
            return;
        }
        journal.beginRealTick();
        if (zones.isEmpty() && pending.isEmpty()) {
            return;
        }
        dropOrphanedFollowers();
        updateFollowingZones();
        this.hasTime = hasTime;
        applyPending();
        expireZones();
        ticking = true;
        globalSteps = fullGlobal != null ? fullGlobal.advance() : 1;
        beginPass(globalSteps == 0);
    }

    private void beginPass(boolean frozen) {
        frozenPass = frozen;
        entitiesTicked = false;
        for (TimeZone zone : zones) {
            if (zone == fullGlobal) {
                zone.setSteps(frozen ? 0 : 1);
            } else if (frozen) {
                zone.setSteps(0);
            } else if (zone.advance() > 0) {
                // Die Zonen-Uhr springt wie die Weltzeit am Anfang des Ticks weiter.
                zone.tickClock();
            }
        }
    }

    public void onLevelTickEnd() {
        if (!ticking) {
            if (!inExtraPass) {
                journal.endRealTick();
            }
            if (dirty) {
                sync();
            }
            return;
        }
        runSubsteps();
        if (inExtraPass) {
            return;
        }
        if (TimeMod.DEBUG && globalSteps != 1) {
            TimeMod.LOGGER.info("Globale Zeit: {} Welt-Schritte in diesem Tick (Weltzeit {})", globalSteps, level.getGameTime());
        }
        for (int i = 1; i < globalSteps; i++) {
            inExtraPass = true;
            try {
                level.tick(hasTime);
            } finally {
                inExtraPass = false;
            }
        }
        ticking = false;
        frozenPass = false;
        applyPending();
        journal.endRealTick();
        if (dirty) {
            sync();
        }
    }

    /** {@code false}, wenn die Welt in diesem Durchlauf eingefroren ist (globaler Stopp/Zeitlupe). */
    public boolean worldAdvances() {
        return !frozenPass;
    }

    public boolean isExtraPass() {
        return inExtraPass;
    }

    /** Wie oft ein globales Welt-System (Tag/Nacht, Wetter, Ereignisse) in diesem Durchlauf läuft. */
    public int worldSystemSteps(int category) {
        if (frozenPass) {
            return 0;
        }
        TimeZone best = null;
        for (TimeZone zone : zones) {
            if (zone != fullGlobal && zone.shape().isGlobal() && zone.accepts(category) && (best == null || zone.outranks(best))) {
                best = zone;
            }
        }
        return best == null ? 1 : best.steps();
    }

    // ------------------------------------------------------------------
    // Hooks im normalen Vanilla-Durchlauf
    // ------------------------------------------------------------------

    /** Vor {@code ServerLevel#tickNonPassenger}. {@code false} = diesen Tick auslassen. */
    public boolean beforeEntityTick(Entity entity) {
        if (entity instanceof TimeHitQueue queue && queue.timemod$hasPendingHits() && !isFrozen(entity)) {
            queue.timemod$releaseHits();
        }
        if (substepDepth > 0 || zones.isEmpty()) {
            return true;
        }
        if (inExtraPass && isOutsideTime(entity)) {
            return false;
        }
        TimeZone zone = resolveEntity(entity);
        if (zone == null) {
            return true;
        }
        if (zone.steps() == 0) {
            if (zone != fullGlobal) {
                ((TimeAware) entity).timemod$addEigenzeit(-1);
            }
            return false;
        }
        TimeContext.enter(level, ((TimeAware) entity).timemod$getEigenzeit());
        return true;
    }

    /** Vanilla tickt in diesem Durchlauf Entities und Block-Entities (sonst ruhen sie, z. B. leere Welt). */
    public void onEntityPhase() {
        entitiesTicked = true;
    }

    public void afterEntityTick() {
        if (substepDepth == 0) {
            TimeContext.exit();
        }
    }

    /** Spieler als Passagiere ticken in zusätzlichen Welt-Durchläufen nicht mit. */
    public boolean allowPassengerTick(Entity passenger) {
        return !(inExtraPass && isOutsideTime(passenger));
    }

    @Override
    public void tickBlockEntity(TickingBlockEntity ticker, Runnable original) {
        if (zones.isEmpty() || substepDepth > 0) {
            original.run();
            return;
        }
        BlockPos pos = ticker.getPos();
        TimeZone zone = resolveBlock(pos, TimeFilter.MACHINES);
        if (zone == null) {
            original.run();
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TimeAnchored) {
            original.run();
            return;
        }
        TimeAware aware = blockEntity instanceof TimeAware a ? a : null;
        if (zone.steps() == 0) {
            if (zone != fullGlobal && aware != null) {
                aware.timemod$addEigenzeit(-1);
            }
            return;
        }
        TimeContext.enter(level, aware != null ? aware.timemod$getEigenzeit() : 0);
        try {
            original.run();
        } finally {
            TimeContext.exit();
        }
    }

    @Nullable
    private TimeAware timeAware(BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof TimeAware aware ? aware : null;
    }

    public boolean allowRandomTick(BlockPos pos, Block block) {
        if (zones.isEmpty()) {
            return true;
        }
        TimeZone zone = resolveBlock(pos, TimeFilter.randomTickCategory(block));
        return zone == null || zone.steps() > 0;
    }

    public boolean allowEnvironment(BlockPos pos) {
        if (zones.isEmpty()) {
            return true;
        }
        TimeZone zone = resolveBlock(pos, TimeFilter.ENVIRONMENT);
        return zone == null || zone.steps() > 0;
    }

    public boolean allowBlockEvent(BlockPos pos) {
        if (zones.isEmpty()) {
            return true;
        }
        TimeZone zone = resolveBlock(pos, TimeFilter.BLOCK_TICKS);
        return zone == null || zone.steps() > 0;
    }

    /** Nach den geplanten Block-Ticks der Welt: Zonen mit mindestens einem Schritt ziehen nach. */
    public void afterWorldBlockTicks() {
        for (TimeZone zone : zones) {
            if (zone != fullGlobal && zone.steps() > 0 && zone.blockTicks != null && !zone.blockTicks.isEmpty()) {
                zone.blockTicks.run(zone.clock(), level::shouldTickBlocksAt, level::tickBlock);
            }
        }
    }

    public void afterWorldFluidTicks() {
        for (TimeZone zone : zones) {
            if (zone != fullGlobal && zone.steps() > 0 && zone.fluidTicks != null && !zone.fluidTicks.isEmpty()) {
                zone.fluidTicks.run(zone.clock(), level::shouldTickBlocksAt, level::tickFluid);
            }
        }
    }

    /** Schnelle Entities werden jeden Tick an Clients geschickt, damit sie flüssig aussehen. */
    public boolean wantsFastSync(Entity entity) {
        if (zones.isEmpty()) {
            return false;
        }
        TimeZone zone = resolveEntity(entity);
        return zone != null && (zone.factor() > 1.0 || zone.isRewind());
    }

    // ------------------------------------------------------------------
    // Teilschritte
    // ------------------------------------------------------------------

    private void runSubsteps() {
        int max = 0;
        for (TimeZone zone : zones) {
            if (zone != fullGlobal) {
                max = Math.max(max, zone.steps());
            }
        }
        if (max <= 1) {
            return;
        }
        substepDepth++;
        try {
            for (int sub = 1; sub < max; sub++) {
                runSubstep(sub);
            }
        } finally {
            substepDepth--;
            TimeContext.exit();
        }
    }

    private void runSubstep(int sub) {
        active.clear();
        for (TimeZone zone : zones) {
            if (zone != fullGlobal && zone.steps() > sub) {
                zone.tickClock();
                active.add(zone);
            }
        }
        if (active.isEmpty()) {
            return;
        }
        for (TimeZone zone : active) {
            if (zone.blockTicks != null && !zone.blockTicks.isEmpty()) {
                zone.blockTicks.run(zone.clock(), level::shouldTickBlocksAt, level::tickBlock);
            }
        }
        for (TimeZone zone : active) {
            if (zone.fluidTicks != null && !zone.fluidTicks.isEmpty()) {
                zone.fluidTicks.run(zone.clock(), level::shouldTickBlocksAt, level::tickFluid);
            }
        }
        for (TimeZone zone : active) {
            if (zone.accepts(TimeFilter.GROWTH | TimeFilter.ENVIRONMENT)) {
                randomTicks(zone);
            }
        }
        runBlockEvents(sub);
        if (entitiesTicked) {
            level.entityTickList.forEach(entity -> substepEntity(entity, sub));
            substepBlockEntities(sub);
        }
    }

    private boolean isActive(@Nullable TimeZone zone, int sub) {
        return zone != null && zone != fullGlobal && zone.steps() > sub;
    }

    private void substepEntity(Entity entity, int sub) {
        if (entity.isRemoved() || entity.isPassenger()) {
            return;
        }
        TimeZone zone = resolveEntity(entity);
        if (!isActive(zone, sub)) {
            return;
        }
        if (!level.getChunkSource().chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().toLong())) {
            return;
        }
        entity.checkDespawn();
        if (entity.isRemoved()) {
            return;
        }
        TimeAware aware = (TimeAware) entity;
        aware.timemod$addEigenzeit(1);
        TimeContext.enter(level, aware.timemod$getEigenzeit());
        try {
            level.guardEntityTick(level::tickNonPassenger, entity);
        } finally {
            TimeContext.exit();
        }
    }

    private void substepBlockEntities(int sub) {
        List<TickingBlockEntity> tickers = level.blockEntityTickers;
        for (int i = 0, n = tickers.size(); i < n && i < tickers.size(); i++) {
            TickingBlockEntity ticker = tickers.get(i);
            if (ticker.isRemoved()) {
                continue;
            }
            BlockPos pos = ticker.getPos();
            TimeZone zone = resolveBlock(pos, TimeFilter.MACHINES);
            if (!isActive(zone, sub) || !level.shouldTickBlocksAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TimeAnchored) {
                continue;
            }
            TimeAware aware = blockEntity instanceof TimeAware a ? a : null;
            if (aware != null) {
                aware.timemod$addEigenzeit(1);
            }
            TimeContext.enter(level, aware != null ? aware.timemod$getEigenzeit() : 0);
            try {
                ticker.tick();
            } finally {
                TimeContext.exit();
            }
        }
    }

    private void runBlockEvents(int sub) {
        ObjectLinkedOpenHashSet<BlockEventData> events = level.blockEvents;
        for (int round = 0; round < 64 && !events.isEmpty(); round++) {
            List<BlockEventData> batch = null;
            Iterator<BlockEventData> it = events.iterator();
            while (it.hasNext()) {
                BlockEventData event = it.next();
                if (isActive(resolveBlock(event.pos(), TimeFilter.BLOCK_TICKS), sub) && level.shouldTickBlocksAt(event.pos())) {
                    if (batch == null) {
                        batch = new ArrayList<>();
                    }
                    batch.add(event);
                    it.remove();
                }
            }
            if (batch == null) {
                return;
            }
            for (BlockEventData event : batch) {
                if (level.doBlockEvent(event)) {
                    BlockPos pos = event.pos();
                    level.getServer().getPlayerList().broadcast(null, pos.getX(), pos.getY(), pos.getZ(), 64.0, level.dimension(),
                        new ClientboundBlockEventPacket(pos, event.block(), event.paramA(), event.paramB()));
                }
            }
        }
    }

    /** Zufalls-Ticks und Niederschlag wie in {@code ServerLevel#tickChunk}, beschränkt auf die Zone. */
    private void randomTicks(TimeZone zone) {
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        AABB bounds = zone.shape().bounds();
        if (bounds == null) {
            for (ChunkHolder holder : chunkMap.getChunks()) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null) {
                    randomTicksInChunk(zone, chunk, speed, chunkMap, null);
                }
            }
            return;
        }
        int minCx = SectionPos.blockToSectionCoord(bounds.minX);
        int maxCx = SectionPos.blockToSectionCoord(bounds.maxX);
        int minCz = SectionPos.blockToSectionCoord(bounds.minZ);
        int maxCz = SectionPos.blockToSectionCoord(bounds.maxZ);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null) {
                    randomTicksInChunk(zone, chunk, speed, chunkMap, bounds);
                }
            }
        }
    }

    private void randomTicksInChunk(TimeZone zone, LevelChunk chunk, int speed, ChunkMap chunkMap, @Nullable AABB bounds) {
        ChunkPos chunkPos = chunk.getPos();
        if (!level.isNaturalSpawningAllowed(chunkPos) || !chunkMap.anyPlayerCloseEnoughForSpawning(chunkPos) || !level.shouldTickBlocksAt(chunkPos.toLong())) {
            return;
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        if (zone.accepts(TimeFilter.ENVIRONMENT)) {
            precipitation(zone, minX, minZ);
        }
        if (speed <= 0) {
            return;
        }
        LevelChunkSection[] sections = chunk.getSections();
        for (int n = 0; n < sections.length; n++) {
            LevelChunkSection section = sections[n];
            if (!section.isRandomlyTicking()) {
                continue;
            }
            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(n));
            if (bounds != null && (minY + 16 <= bounds.minY || minY >= bounds.maxY)) {
                continue;
            }
            for (int m = 0; m < speed; m++) {
                BlockPos pos = level.getBlockRandomPos(minX, minY, minZ, 15);
                BlockState state = section.getBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
                if (state.isRandomlyTicking() && resolveBlock(pos, TimeFilter.randomTickCategory(state.getBlock())) == zone) {
                    state.randomTick(level, pos, level.random);
                }
                FluidState fluid = state.getFluidState();
                if (fluid.isRandomlyTicking() && resolveBlock(pos, TimeFilter.ENVIRONMENT) == zone) {
                    fluid.randomTick(level, pos, level.random);
                }
            }
        }
    }

    private void precipitation(TimeZone zone, int minX, int minZ) {
        if (level.random.nextInt(16) != 0) {
            return;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(minX, 0, minZ, 15));
        BlockPos below = top.below();
        if (resolveBlock(below, TimeFilter.ENVIRONMENT) != zone) {
            return;
        }
        Biome biome = level.getBiome(top).value();
        if (biome.shouldFreeze(level, below)) {
            level.setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
        }
        if (level.isRaining()) {
            int maxHeight = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
            if (maxHeight > 0 && biome.shouldSnow(level, top)) {
                BlockState state = level.getBlockState(top);
                if (state.is(Blocks.SNOW)) {
                    int layers = state.getValue(SnowLayerBlock.LAYERS);
                    if (layers < Math.min(maxHeight, 8)) {
                        BlockState more = state.setValue(SnowLayerBlock.LAYERS, layers + 1);
                        Block.pushEntitiesUp(state, more, level, top);
                        level.setBlockAndUpdate(top, more);
                    }
                } else {
                    level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                }
            }
            Biome.Precipitation precipitation = biome.getPrecipitationAt(below);
            if (precipitation != Biome.Precipitation.NONE) {
                BlockState state = level.getBlockState(below);
                state.getBlock().handlePrecipitation(state, level, below, precipitation);
            }
        }
    }

    // ------------------------------------------------------------------
    // Geplante Ticks: Weiterleitung in Zonen
    // ------------------------------------------------------------------

    @Nullable
    private TimeZone resolveTickZone(BlockPos pos, boolean fluid) {
        TimeZone best = null;
        int category = fluid ? TimeFilter.FLUIDS : TimeFilter.BLOCK_TICKS;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        for (TimeZone zone : zones) {
            if (zone != fullGlobal && zone.accepts(category) && zone.shape().contains(x, y, z) && (best == null || zone.outranks(best))) {
                best = zone;
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private static <T> ZoneTicks<T> queue(TimeZone zone, boolean fluid) {
        return (ZoneTicks<T>) (fluid ? zone.fluidTicks : zone.blockTicks);
    }

    /** Aus {@code LevelTicks#schedule}: liegt die Position in einer Zone, plant die Zone den Tick. */
    public <T> boolean routeSchedule(ScheduledTick<T> tick, boolean fluid) {
        if (routingDisabled || zones.isEmpty()) {
            return false;
        }
        TimeZone zone = resolveTickZone(tick.pos(), fluid);
        if (zone == null) {
            return false;
        }
        long delay = tick.triggerTick() - level.getLevelData().getGameTime();
        ZoneTicks<T> queue = queue(zone, fluid);
        queue.schedule(new ScheduledTick<>(tick.type(), tick.pos(), zone.clock() + delay, tick.priority(), tick.subTickOrder()));
        return true;
    }

    public <T> boolean hasZoneTick(BlockPos pos, T type, boolean fluid) {
        if (zones.isEmpty()) {
            return false;
        }
        TimeZone zone = resolveTickZone(pos, fluid);
        return zone != null && queue(zone, fluid).hasScheduledTick(pos, type);
    }

    public <T> boolean willZoneTickThisStep(BlockPos pos, T type, boolean fluid) {
        if (zones.isEmpty()) {
            return false;
        }
        TimeZone zone = resolveTickZone(pos, fluid);
        return zone != null && queue(zone, fluid).willTickThisStep(pos, type);
    }

    /** Neu geladener Chunk: seine geplanten Ticks wandern in die zuständigen Zonen. */
    public <T> void onContainerAdded(ChunkPos chunkPos, LevelChunkTicks<T> container, boolean fluid) {
        if (zones.isEmpty() || !anyTickZoneTouches(chunkPos)) {
            return;
        }
        long gameTime = level.getLevelData().getGameTime();
        List<ScheduledTick<T>> moved = new ArrayList<>();
        container.removeIf(tick -> {
            if (resolveTickZone(tick.pos(), fluid) != null) {
                moved.add(tick);
                return true;
            }
            return false;
        });
        for (ScheduledTick<T> tick : moved) {
            TimeZone zone = resolveTickZone(tick.pos(), fluid);
            queue(zone, fluid).schedule(new ScheduledTick<>(tick.type(), tick.pos(), zone.clock() + tick.triggerTick() - gameTime, tick.priority(), tick.subTickOrder()));
        }
    }

    /** Chunk wird entladen: seine Zonen-Ticks wurden schon mitgespeichert und werden verworfen. */
    public void onContainerRemoved(ChunkPos chunkPos, boolean fluid) {
        for (TimeZone zone : zones) {
            ZoneTicks<?> queue = fluid ? zone.fluidTicks : zone.blockTicks;
            if (queue != null && !queue.isEmpty()) {
                queue.extract(tick -> ChunkPos.asLong(tick.pos()) == chunkPos.toLong());
            }
        }
    }

    /** Beim Speichern eines Chunks: Zonen-Ticks als normale Ticks mit Restverzögerung mitschreiben. */
    public void appendSavedTicks(ChunkPos chunkPos, CompoundTag chunkTag) {
        if (zones.isEmpty()) {
            return;
        }
        long gameTime = level.getLevelData().getGameTime();
        ListTag blocks = chunkTag.getList("block_ticks", Tag.TAG_COMPOUND);
        ListTag fluids = chunkTag.getList("fluid_ticks", Tag.TAG_COMPOUND);
        for (TimeZone zone : zones) {
            if (zone.blockTicks != null) {
                for (ScheduledTick<Block> tick : zone.blockTicks.snapshot(t -> ChunkPos.asLong(t.pos()) == chunkPos.toLong())) {
                    blocks.add(SavedTick.saveTick(toWorldTick(tick, zone, gameTime), b -> BuiltInRegistries.BLOCK.getKey(b).toString(), gameTime));
                }
            }
            if (zone.fluidTicks != null) {
                for (ScheduledTick<Fluid> tick : zone.fluidTicks.snapshot(t -> ChunkPos.asLong(t.pos()) == chunkPos.toLong())) {
                    fluids.add(SavedTick.saveTick(toWorldTick(tick, zone, gameTime), f -> BuiltInRegistries.FLUID.getKey(f).toString(), gameTime));
                }
            }
        }
        chunkTag.put("block_ticks", blocks);
        chunkTag.put("fluid_ticks", fluids);
    }

    private static <T> ScheduledTick<T> toWorldTick(ScheduledTick<T> tick, TimeZone zone, long gameTime) {
        return new ScheduledTick<>(tick.type(), tick.pos(), gameTime + (tick.triggerTick() - zone.clock()), tick.priority(), tick.subTickOrder());
    }

    private boolean anyTickZoneTouches(ChunkPos chunkPos) {
        for (TimeZone zone : zones) {
            if (zone != fullGlobal && zone.accepts(TimeFilter.BLOCK_TICKS | TimeFilter.FLUIDS) && zone.shape().intersectsChunk(chunkPos.x, chunkPos.z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Nach jeder Änderung der Zonen: Alle betroffenen geplanten Ticks werden mit ihrer
     * Restverzögerung neu einsortiert – in die jetzt zuständige Zone oder zurück in die Welt.
     */
    private void rebalanceTicks(Collection<TimeZone> removed) {
        long gameTime = level.getLevelData().getGameTime();
        List<Moved<Block>> blocks = new ArrayList<>();
        List<Moved<Fluid>> fluids = new ArrayList<>();
        List<TimeZone> holders = new ArrayList<>(removed);
        holders.addAll(zones);
        for (TimeZone zone : holders) {
            if (zone.blockTicks != null) {
                for (ScheduledTick<Block> tick : zone.blockTicks.extract(t -> true)) {
                    blocks.add(new Moved<>(tick, tick.triggerTick() - zone.clock()));
                }
            }
            if (zone.fluidTicks != null) {
                for (ScheduledTick<Fluid> tick : zone.fluidTicks.extract(t -> true)) {
                    fluids.add(new Moved<>(tick, tick.triggerTick() - zone.clock()));
                }
            }
        }
        LongSet visited = new LongOpenHashSet();
        for (TimeZone zone : zones) {
            if (zone == fullGlobal || !zone.accepts(TimeFilter.BLOCK_TICKS | TimeFilter.FLUIDS)) {
                continue;
            }
            AABB bounds = zone.shape().bounds();
            if (bounds == null) {
                collectFromWorld(level.getBlockTicks(), false, null, visited, blocks, gameTime);
                collectFromWorld(level.getFluidTicks(), true, null, visited, fluids, gameTime);
                break;
            }
            collectFromWorld(level.getBlockTicks(), false, bounds, visited, blocks, gameTime);
            collectFromWorld(level.getFluidTicks(), true, bounds, visited, fluids, gameTime);
        }
        routingDisabled = true;
        try {
            for (Moved<Block> moved : blocks) {
                reschedule(level.getBlockTicks(), moved, false, gameTime);
            }
            for (Moved<Fluid> moved : fluids) {
                reschedule(level.getFluidTicks(), moved, true, gameTime);
            }
        } finally {
            routingDisabled = false;
        }
    }

    private <T> void collectFromWorld(LevelTicks<T> ticks, boolean fluid, @Nullable AABB bounds, LongSet visited, List<Moved<T>> out, long gameTime) {
        for (var entry : ticks.allContainers.long2ObjectEntrySet()) {
            long chunk = entry.getLongKey();
            int cx = ChunkPos.getX(chunk);
            int cz = ChunkPos.getZ(chunk);
            if (bounds != null && (bounds.maxX <= (cx << 4) || bounds.minX >= (cx << 4) + 16 || bounds.maxZ <= (cz << 4) || bounds.minZ >= (cz << 4) + 16)) {
                continue;
            }
            if (!visited.add(chunk * 2 + (fluid ? 1 : 0))) {
                continue;
            }
            entry.getValue().removeIf(tick -> {
                if (resolveTickZone(tick.pos(), fluid) != null) {
                    out.add(new Moved<>(tick, tick.triggerTick() - gameTime));
                    return true;
                }
                return false;
            });
        }
    }

    private <T> void reschedule(LevelTicks<T> ticks, Moved<T> moved, boolean fluid, long gameTime) {
        ScheduledTick<T> tick = moved.tick();
        TimeZone zone = resolveTickZone(tick.pos(), fluid);
        if (zone != null) {
            ZoneTicks<T> queue = queue(zone, fluid);
            queue.schedule(new ScheduledTick<>(tick.type(), tick.pos(), zone.clock() + moved.remaining(), tick.priority(), tick.subTickOrder()));
            return;
        }
        LevelChunkTicks<T> container = ticks.allContainers.get(ChunkPos.asLong(tick.pos()));
        if (container != null) {
            container.schedule(new ScheduledTick<>(tick.type(), tick.pos(), gameTime + moved.remaining(), tick.priority(), tick.subTickOrder()));
        }
    }

    private record Moved<T>(ScheduledTick<T> tick, long remaining) {
    }

    // ------------------------------------------------------------------
    // Synchronisation
    // ------------------------------------------------------------------

    private void sync() {
        dirty = false;
        TimeNetworking.sendZones(level, zones);
        if (TimeMod.DEBUG) {
            TimeMod.LOGGER.info("Zeitzonen in {}: {}", level.dimension().location(), zones);
        }
    }
}

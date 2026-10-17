package com.fischgesichtert.timemod.rewind;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.item.RewindPersistent;
import com.fischgesichtert.timemod.safety.Safety;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
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
 * <p>
 * Damit sich durch Zurückspulen nichts verdoppeln lässt, gelten zwei Regeln:
 * <ul>
 * <li>Blöcke, Maschinen und Gegenstände laufen nur gemeinsam zurück, und mit ihnen die Inventare
 * aller Spieler im Bereich. Das gilt auch für den Auslöser, wenn er selbst an Ort und Stelle bleibt.
 * Wer eines davon wählt, spult alles zurück.</li>
 * <li>Chronon ist geronnene Zeit: Ausgegebenes kommt durch Zurückspulen nicht wieder
 * ({@link #settleChronon()}).</li>
 * </ul>
 * Teilweises Zurückspulen (nur Wesen, Natur oder Tageszeit) bewegt und heilt. Es weckt keine Toten
 * auf und stellt nur Nachwachsendes wieder her.
 */
public final class RewindSession {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** Alles zurückspulen (Standard ohne Filtermodule). */
    public static final int EVERYTHING = TimeFilter.ALL_ENTITIES | TimeFilter.ALL_BLOCKS | TimeFilter.ALL_WORLD;
    /** Was nur gemeinsam mit allem anderen zurückläuft. */
    public static final int ITEM_BOUND = TimeFilter.BLOCK_TICKS | TimeFilter.MACHINES | TimeFilter.ITEMS;
    /** Blockänderungen, die auch einzeln zurückgespult werden dürfen: alles Nachwachsende. */
    private static final int RENEWABLE = TimeFilter.GROWTH | TimeFilter.ENVIRONMENT | TimeFilter.FLUIDS;

    private final ChronoJournal journal;
    private final ServerLevel level;
    private final int requestedTicks;
    private int totalSteps;
    private boolean started;
    private final int speed;
    @Nullable
    private final Region region;
    private final int mask;
    /** Alles Gegenständliche läuft mit zurück (Blöcke, Maschinen, Gegenstände, Inventare). */
    private final boolean full;
    @Nullable
    private final UUID owner;
    private final boolean ownerFree;
    private int done;
    private boolean finished;
    @Nullable
    private TimeZone zone;
    @Nullable
    private Predicate<ServerPlayer> whileHeld;
    private final Set<BlockPos> touched = new HashSet<>();
    private final List<Runnable> onFinish = new ArrayList<>();

    /** Chronon, das beim Zurückspulen aus Dingen verschwunden ist und anderswo wieder auftauchen darf. */
    private long chrononCredit;
    /** Chronon, das im laufenden Schritt hinzugekommen ist … */
    private long stepGain;
    /** … und woraus es sich wieder entnehmen lässt (mit dem eigenen Zugewinn jedes Behälters). */
    private final List<StepGain> stepDrains = new ArrayList<>();

    private record StepGain(IntUnaryOperator drain, long gained) {
    }

    /** Bereich, auf den das Zurückspulen wirkt. */
    public record Region(Vec3 center, double radius) {
        boolean contains(double x, double y, double z) {
            return center.distanceToSqr(x, y, z) <= radius * radius;
        }

        boolean contains(BlockPos pos) {
            return contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }

    RewindSession(ChronoJournal journal, ServerLevel level, int requestedTicks, int speed, @Nullable Region region, @Nullable UUID owner, int mask,
                  boolean ownerFree) {
        this.journal = journal;
        this.level = level;
        this.requestedTicks = requestedTicks;
        this.speed = speed;
        this.region = region;
        this.mask = effectiveMask(mask);
        this.full = isFull(mask);
        this.owner = owner;
        this.ownerFree = ownerFree && owner != null;
        if (speed > 0) {
            ZoneShape shape = region != null ? new ZoneShape.Sphere(region.center(), region.radius()) : ZoneShape.Global.INSTANCE;
            zone = ServerTimeEngine.of(level).addRewindZone(shape, this.mask, owner, this.ownerFree);
        }
    }

    /** Wer Blöcke, Maschinen oder Gegenstände wählt, spult alles Gegenständliche mit zurück. */
    public static int effectiveMask(int mask) {
        return isFull(mask) ? mask | TimeFilter.ALL_ENTITIES | TimeFilter.ALL_BLOCKS : mask;
    }

    /** Läuft bei dieser Auswahl alles Gegenständliche zurück? */
    public static boolean isFull(int mask) {
        return (mask & ITEM_BOUND) != 0;
    }

    /**
     * Darf dieser Spieler gerade nichts fallen lassen? Das gilt für den Auslöser, der beim vollen
     * Zurückspulen frei bleibt. Sein Inventar folgt der Welt, und Fallengelassenes käme doppelt zurück.
     */
    public static boolean holdsInventory(ServerPlayer player) {
        RewindSession session = ServerTimeEngine.of(player.serverLevel()).journal().session();
        return session != null && !session.finished && session.full && session.isFreeOwner(player.getUUID());
    }

    /** Bleibt der Auslöser stehen, während um ihn herum zurückgespult wird? */
    private boolean isFreeOwner(UUID uuid) {
        return ownerFree && uuid.equals(owner);
    }

    /** Nur so lange zurückspulen, wie der Auslöser es will, etwa solange er die Taste hält. */
    public void whileHeld(Predicate<ServerPlayer> condition) {
        whileHeld = condition;
    }

    /** Schon jetzt anhalten (Loslassen beim Scrubbing): die Welt bleibt bei diesem Stand. */
    public void stop() {
        finish();
    }

    int requestedTicks() {
        return requestedTicks;
    }

    /** Vom Journal: Jetzt geht es los, {@code steps} Schritte stehen zur Verfügung. */
    void start(int steps) {
        totalSteps = steps;
        started = true;
    }

    /** Wie viele Ticks schon zurückgespult sind. */
    public int done() {
        return done;
    }

    public int totalSteps() {
        return totalSteps;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    private boolean wants(int category) {
        return (mask & category) != 0;
    }

    /**
     * Zu welcher Filter-Kategorie gehört eine Blockänderung? Pflanzen und Bäume zählen zum Wachstum,
     * Wasser und Lava zu den Flüssigkeiten, Schnee, Eis und Feuer zur Umwelt – alles andere
     * (gebaute Blöcke, Redstone, Explosionen) zu den Blöcken/Redstone.
     */
    static int blockCategory(BlockState before, BlockState after) {
        if (!before.getFluidState().isEmpty() || !after.getFluidState().isEmpty()) {
            return TimeFilter.FLUIDS;
        }
        if (isGrowth(before) || isGrowth(after)) {
            return TimeFilter.GROWTH;
        }
        if (isEnvironment(before) || isEnvironment(after)) {
            return TimeFilter.ENVIRONMENT;
        }
        return TimeFilter.BLOCK_TICKS;
    }

    private static boolean isGrowth(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)
            || state.is(BlockTags.FLOWERS) || state.is(BlockTags.REPLACEABLE_BY_TREES)
            || (state.isRandomlyTicking() && TimeFilter.randomTickCategory(state.getBlock()) == TimeFilter.GROWTH);
    }

    private static boolean isEnvironment(BlockState state) {
        return state.is(BlockTags.SNOW) || state.is(BlockTags.ICE) || state.is(BlockTags.FIRE);
    }

    /** Teil-Zurückspulen: nur Nachwachsendes, und nie etwas mit Block-Entity (Truhen, Maschinen …). */
    private boolean partialAllows(Frame.BlockChange change) {
        BlockState current = level.getBlockState(change.pos());
        int category = blockCategory(change.oldState(), current);
        return (category & RENEWABLE) != 0 && wants(category) && change.oldBlockEntity() == null
            && !change.oldState().hasBlockEntity() && !current.hasBlockEntity();
    }

    /** Hält etwas Gegenstände fest, die beim Entfernen verloren gingen? (Teil-Zurückspulen lässt es stehen.) */
    private static boolean holdsItems(Entity entity) {
        return entity instanceof ContainerEntity || entity instanceof ItemFrame || entity instanceof ArmorStand;
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
        if (!started) {
            return;
        }
        if (whileHeld != null && owner != null
            && !(level.getPlayerByUUID(owner) instanceof ServerPlayer player && whileHeld.test(player))) {
            finish();
            return;
        }
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
        settleChronon();
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
            if (!full && !partialAllows(change)) {
                continue;
            }
            long before = Chronon.total(level.getBlockEntity(change.pos()));
            level.setBlock(change.pos(), change.oldState(), SET_FLAGS);
            if (change.oldBlockEntity() != null) {
                loadBlockEntity(change.pos(), change.oldBlockEntity());
            }
            BlockEntity now = level.getBlockEntity(change.pos());
            noteChronon(before, Chronon.total(now), amount -> Chronon.drain(now, amount));
            touched.add(change.pos());
        }
        // 2) Block-Entities, deren Daten sich in diesem Tick geändert haben (nur beim vollen Zurückspulen)
        if (full) {
            for (BlockPos pos : frame.blockEntityAfter.keySet()) {
                if (region != null && !region.contains(pos)) {
                    continue;
                }
                CompoundTag state = journal.blockEntityState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (state != null && blockEntity != null) {
                    long before = Chronon.total(blockEntity);
                    loadBlockEntity(pos, state);
                    noteChronon(before, Chronon.total(blockEntity), amount -> Chronon.drain(blockEntity, amount));
                }
            }
        }
        // 3) Entities: Entstandenes verschwindet, Entferntes kehrt zurück (nur beim vollen Zurückspulen)
        List<Frame.EntityEvent> events = frame.entityEvents;
        for (int i = events.size() - 1; i >= 0; i--) {
            Frame.EntityEvent event = events.get(i);
            if (!inRegion(event.pos().x, event.pos().y, event.pos().z) || !wants(event.category())) {
                continue;
            }
            if (event.spawned()) {
                Entity entity = level.getEntity(event.uuid());
                if (entity != null && !(entity instanceof ServerPlayer) && (full || !holdsItems(entity))) {
                    noteChronon(Chronon.total(entity), 0, amount -> 0);
                    entity.discard();
                }
            } else if (full && level.getEntity(event.uuid()) == null) {
                Entity revived = revive(event);
                if (revived != null) {
                    noteChronon(0, Chronon.total(revived), amount -> Chronon.drain(revived, amount));
                }
            }
        }
        // 4) Spieler-Inventare: beim vollen Zurückspulen für alle im Bereich, auch den freien Auslöser
        if (full) {
            for (Map.Entry<UUID, Frame.PlayerItems> entry : frame.playerItems.entrySet()) {
                if (level.getEntity(entry.getKey()) instanceof ServerPlayer player && inRegion(player.getX(), player.getY(), player.getZ())) {
                    Frame.PlayerItems before = journal.playerItems(entry.getKey());
                    if (before != null) {
                        restoreItems(player, before);
                    }
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

    @Nullable
    private Entity revive(Frame.EntityEvent event) {
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
            return null;
        }
        if (entity instanceof LivingEntity living) {
            living.deathTime = 0;
            living.dead = false;
            living.setHealth(Math.max(living.getHealth(), 0.5F));
        }
        level.addFreshEntity(entity);
        return entity;
    }

    /** Alle aufgezeichneten Entities (und Spieler) an ihre Position aus {@code frame} setzen. */
    private void applyPoses(Frame frame, boolean exact) {
        for (Map.Entry<UUID, Frame.Pose> entry : frame.poses.entrySet()) {
            Frame.Pose pose = entry.getValue();
            if (!inRegion(pose.x(), pose.y(), pose.z())) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof ServerPlayer player) {
                applyPlayer(player, pose, frame.playerVitals.get(entry.getKey()));
                continue;
            }
            if (entity == null || !wants(TimeFilter.categoryOf(entity))) {
                continue;
            }
            entity.setPos(pose.x(), pose.y(), pose.z());
            entity.setYRot(pose.yRot());
            entity.setXRot(pose.xRot());
            entity.setYHeadRot(pose.headRot());
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = pose.bodyRot();
                boolean dying = living.deathTime > 0 || living.isDeadOrDying();
                // Teil-Zurückspulen weckt keine Sterbenden auf: ihre Beute ist schon gefallen
                if (full || !dying) {
                    if (living.isAlive() || pose.health() > 0) {
                        living.setHealth(pose.health());
                    }
                    if (pose.health() > 0 && living.deathTime > 0) {
                        // Mitten im Sterben zurückgespult: wieder ganz lebendig (sonst stirbt es nie wieder)
                        living.deathTime = 0;
                        living.dead = false;
                    }
                }
            }
            entity.setOnGround(pose.onGround());
            entity.fallDistance = pose.fallDistance();
            entity.setRemainingFireTicks(pose.fire());
            entity.setAirSupply(pose.air());
            entity.setDeltaMovement(exact ? new Vec3(pose.vx(), pose.vy(), pose.vz()) : Vec3.ZERO);
            entity.hasImpulse = true;
        }
        if (region == null && wants(TimeFilter.DAYTIME)) {
            level.setDayTime(frame.dayTime);
        }
    }

    /**
     * Spieler: Körper (Ort, Leben, gewählter Platz) nur mit Spieler-Filter und nicht beim freien
     * Auslöser. Hunger und Erfahrung gehören zum Inventar und folgen beim vollen Zurückspulen der Welt.
     */
    private void applyPlayer(ServerPlayer player, Frame.Pose pose, @Nullable Frame.PlayerVitals vitals) {
        boolean body = wants(TimeFilter.PLAYERS) && !isFreeOwner(player.getUUID());
        if (body) {
            player.connection.teleport(pose.x(), pose.y(), pose.z(), pose.yRot(), pose.xRot());
            player.setHealth(pose.health());
        }
        if (vitals == null || !(body || full)) {
            return;
        }
        if (full) {
            player.getFoodData().setFoodLevel(vitals.food());
            player.getFoodData().setSaturation(vitals.saturation());
            player.experienceLevel = vitals.xpLevel();
            player.experienceProgress = vitals.xpProgress();
            player.totalExperience = vitals.totalXp();
        }
        if (body) {
            player.getInventory().selected = vitals.selected();
        }
        player.resetSentInfo();
    }

    private void restoreItems(ServerPlayer player, Frame.PlayerItems items) {
        long before = Chronon.total(player);
        List<ItemStack> persistent = persistentItems(player);
        player.getInventory().load(items.inventory());
        player.getEnderChestInventory().fromTag(items.enderChest());
        player.removeAllEffects();
        for (Tag tag : items.effects()) {
            MobEffectInstance effect = MobEffectInstance.load((CompoundTag) tag);
            if (effect != null) {
                player.addEffect(effect);
            }
        }
        carryPersistent(player, persistent);
        player.inventoryMenu.broadcastChanges();
        noteChronon(before, Chronon.total(player), amount -> Chronon.drain(player, amount));
    }

    // ------------------------------------------------------------------ Was ein Zurückspulen übersteht

    private static List<ItemStack> persistentItems(ServerPlayer player) {
        List<ItemStack> out = new ArrayList<>();
        for (Container container : List.<Container>of(player.getInventory(), player.getEnderChestInventory())) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.getItem() instanceof RewindPersistent) {
                    out.add(stack.copy());
                }
            }
        }
        return out;
    }

    private static void carryPersistent(ServerPlayer player, List<ItemStack> before) {
        if (before.isEmpty()) {
            return;
        }
        List<ItemStack> unused = new ArrayList<>(before);
        for (Container container : List.<Container>of(player.getInventory(), player.getEnderChestInventory())) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack restored = container.getItem(i);
                if (!(restored.getItem() instanceof RewindPersistent persistent)) {
                    continue;
                }
                for (Iterator<ItemStack> it = unused.iterator(); it.hasNext(); ) {
                    ItemStack current = it.next();
                    if (current.is(restored.getItem())) {
                        persistent.carryThroughRewind(current, restored);
                        it.remove();
                        break;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Chronon-Bilanz

    private void noteChronon(long before, long after, IntUnaryOperator drain) {
        stepGain += after - before;
        if (after > before) {
            stepDrains.add(new StepGain(drain, after - before));
        }
    }

    /**
     * Bilanz eines Schritts: Chronon, das wieder auftaucht, ohne anderswo zu verschwinden (etwa der
     * bezahlte Preis in der Uhr), wird gleich wieder entnommen. Was nur den Ort wechselt (eine Zelle
     * zurück in die Truhe), bleibt.
     */
    private void settleChronon() {
        if (stepGain < 0) {
            chrononCredit -= stepGain;
        } else if (stepGain > 0) {
            long covered = Math.min(chrononCredit, stepGain);
            chrononCredit -= covered;
            long excess = stepGain - covered;
            // Jeder Behälter gibt zuerst zurück, was er selbst dazugewonnen hat …
            for (StepGain gain : stepDrains) {
                if (excess <= 0) {
                    break;
                }
                excess -= gain.drain().applyAsInt((int) Math.min(Math.min(excess, gain.gained()), Integer.MAX_VALUE));
            }
            // … und was dann noch fehlt (ein Behälter war schon leerer), kommt aus den übrigen
            for (StepGain gain : stepDrains) {
                if (excess <= 0) {
                    break;
                }
                excess -= gain.drain().applyAsInt((int) Math.min(excess, Integer.MAX_VALUE));
            }
        }
        stepGain = 0;
        stepDrains.clear();
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (!started) {
            // Abgebrochen, bevor es losging: nichts zurückgespult
            journal.cancelPending(this);
            if (zone != null) {
                ServerTimeEngine.of(level).removeZone(zone.id());
            }
            runFinishActions();
            return;
        }
        journal.beginReplay();
        try {
            Frame target = journal.frames().peekLast();
            if (target != null) {
                // Innere Zustände (KI, Inventare von Mobs …) aus dem letzten Schlüsselbild ≤ Ziel
                if (full) {
                    for (UUID uuid : target.poses.keySet()) {
                        Entity entity = level.getEntity(uuid);
                        if (entity == null || entity instanceof ServerPlayer || !wants(TimeFilter.categoryOf(entity))) {
                            continue;
                        }
                        CompoundTag state = journal.entityState(uuid);
                        if (state != null && inRegion(entity.getX(), entity.getY(), entity.getZ())) {
                            long before = Chronon.total(entity);
                            CompoundTag copy = state.copy();
                            copy.remove("id");
                            entity.load(copy);
                            noteChronon(before, Chronon.total(entity), amount -> Chronon.drain(entity, amount));
                        }
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
        } catch (Throwable error) {
            // Absturzschutz: Was sich nicht wiederherstellen ließ, bleibt wie es ist – das Ende kommt trotzdem
            Safety.report("Zurückspulen: Abschluss", error);
        } finally {
            journal.endReplay();
        }
        Safety.run("Zurückspulen: Chronon", this::settleChronon);
        if (zone != null) {
            ServerTimeEngine.of(level).removeZone(zone.id());
        }
        Safety.run("Zurückspulen: Neubeginn der Aufzeichnung", journal::restartAfterRewind);
        TimeMod.LOGGER.info("Zurückgespult: {} Ticks", done);
        runFinishActions();
    }

    private void runFinishActions() {
        List<Runnable> actions = new ArrayList<>(onFinish);
        onFinish.clear();
        for (Runnable action : actions) {
            Safety.run("Zurückspulen: Abschluss-Aktion", action);
        }
    }
}

package com.fischgesichtert.timemod.time;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.ScheduledTick;

/**
 * Geplante Ticks einer Zeitzone, gemessen in der Eigenzeit der Zone.
 * <p>
 * Funktioniert wie {@code LevelTicks}: Ticks werden pro Schritt erst gesammelt und dann in
 * Vanilla-Reihenfolge ausgeführt, damit ein Block sich während seines Ticks neu einplanen kann.
 */
public final class ZoneTicks<T> {
    private final PriorityQueue<ScheduledTick<T>> queue = new PriorityQueue<>(ScheduledTick.DRAIN_ORDER);
    private final Set<ScheduledTick<?>> scheduled = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
    private final ArrayDeque<ScheduledTick<T>> running = new ArrayDeque<>();
    private final Set<ScheduledTick<?>> runningSet = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);

    public void schedule(ScheduledTick<T> tick) {
        if (scheduled.add(tick)) {
            queue.add(tick);
        }
    }

    public boolean hasScheduledTick(BlockPos pos, T type) {
        return scheduled.contains(ScheduledTick.probe(type, pos));
    }

    public boolean willTickThisStep(BlockPos pos, T type) {
        if (runningSet.isEmpty() && !running.isEmpty()) {
            runningSet.addAll(running);
        }
        return runningSet.contains(ScheduledTick.probe(type, pos));
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    /** Führt alle Ticks aus, die bis {@code clock} fällig sind und in tickenden Chunks liegen. */
    public void run(long clock, LongPredicate chunkTicking, BiConsumer<BlockPos, T> ticker) {
        List<ScheduledTick<T>> deferred = null;
        while (!queue.isEmpty() && queue.peek().triggerTick() <= clock) {
            ScheduledTick<T> tick = queue.poll();
            if (!chunkTicking.test(ChunkPos.asLong(tick.pos()))) {
                if (deferred == null) {
                    deferred = new ArrayList<>();
                }
                deferred.add(tick);
                continue;
            }
            scheduled.remove(tick);
            running.add(tick);
        }
        if (deferred != null) {
            queue.addAll(deferred);
        }
        while (!running.isEmpty()) {
            ScheduledTick<T> tick = running.poll();
            if (!runningSet.isEmpty()) {
                runningSet.remove(tick);
            }
            ticker.accept(tick.pos(), tick.type());
        }
        runningSet.clear();
    }

    /** Entfernt alle Ticks, auf die {@code filter} zutrifft, und gibt sie zurück. */
    public List<ScheduledTick<T>> extract(Predicate<ScheduledTick<T>> filter) {
        List<ScheduledTick<T>> out = new ArrayList<>();
        queue.removeIf(tick -> {
            if (filter.test(tick)) {
                out.add(tick);
                scheduled.remove(tick);
                return true;
            }
            return false;
        });
        return out;
    }

    public List<ScheduledTick<T>> snapshot(Predicate<ScheduledTick<T>> filter) {
        List<ScheduledTick<T>> out = new ArrayList<>();
        for (ScheduledTick<T> tick : queue) {
            if (filter.test(tick)) {
                out.add(tick);
            }
        }
        return out;
    }
}

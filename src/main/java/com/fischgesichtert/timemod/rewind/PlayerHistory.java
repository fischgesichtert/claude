package com.fischgesichtert.timemod.rewind;

import com.fischgesichtert.timemod.safety.Safety;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kurzes persönliches Gedächtnis jedes Spielers (die letzten 5 Sekunden): Grundlage der
 * Rückblick-Uhr. Unabhängig vom großen Zeitjournal und sehr günstig.
 */
public final class PlayerHistory {
    private static final int CAPACITY = 5 * 20 + 1;
    private static final int RECALL_TICKS = 10;

    private record Entry(ServerLevel level, double x, double y, double z, float yRot, float xRot, float health) {
    }

    private static final Map<UUID, ArrayDeque<Entry>> HISTORY = new HashMap<>();
    private static final Map<UUID, Recall> ACTIVE = new HashMap<>();

    private static final class Recall {
        final List<Entry> path;
        int step;

        Recall(List<Entry> path) {
            this.path = path;
        }
    }

    private PlayerHistory() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> Safety.run("Spieler-Verlauf", () -> tick(server)));
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Recall recall = ACTIVE.get(id);
            if (recall != null) {
                play(player, recall);
                continue;
            }
            ArrayDeque<Entry> history = HISTORY.computeIfAbsent(id, k -> new ArrayDeque<>());
            history.addLast(new Entry(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.getHealth()));
            while (history.size() > CAPACITY) {
                history.pollFirst();
            }
        }
        HISTORY.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    public static boolean canRecall(ServerPlayer player) {
        ArrayDeque<Entry> history = HISTORY.get(player.getUUID());
        return history != null && history.size() > 5 && !ACTIVE.containsKey(player.getUUID());
    }

    /** Startet den Rücksprung: der Weg wird in einer halben Sekunde rückwärts abgefahren. */
    public static void startRecall(ServerPlayer player, int ticks) {
        ArrayDeque<Entry> history = HISTORY.get(player.getUUID());
        if (history == null) {
            return;
        }
        List<Entry> path = new ArrayList<>();
        Iterator<Entry> it = history.descendingIterator();
        while (it.hasNext() && path.size() <= ticks) {
            Entry entry = it.next();
            if (entry.level() != player.serverLevel()) {
                break;
            }
            path.add(entry);
        }
        if (path.size() > 1) {
            ACTIVE.put(player.getUUID(), new Recall(path));
        }
    }

    private static void play(ServerPlayer player, Recall recall) {
        int perTick = Math.max(1, (int) Math.ceil(recall.path.size() / (double) RECALL_TICKS));
        recall.step = Math.min(recall.path.size() - 1, recall.step + perTick);
        Entry entry = recall.path.get(recall.step);
        player.connection.teleport(entry.x(), entry.y(), entry.z(), entry.yRot(), entry.xRot());
        player.fallDistance = 0;
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, entry.x(), entry.y() + 1, entry.z(), 6, 0.2, 0.4, 0.2, 0.01);
        if (recall.step >= recall.path.size() - 1) {
            player.setHealth(entry.health());
            ACTIVE.remove(player.getUUID());
            ArrayDeque<Entry> history = HISTORY.get(player.getUUID());
            if (history != null) {
                history.clear();
            }
        }
    }
}

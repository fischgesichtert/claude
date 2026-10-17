package com.fischgesichtert.timemod.gate;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.FutureDerivation;
import com.fischgesichtert.timemod.paradox.ParadoxPressure;
import com.fischgesichtert.timemod.registry.ModAdvancements;
import com.fischgesichtert.timemod.registry.ModSounds;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Der Zeitstrom: Wer durch das Tor geht, fliegt fünf bis acht Sekunden durch einen Tunnel aus Jahren.
 * Die Welt wechselt gleich zu Beginn – der Reisende schwebt unsichtbar hoch über dem Ziel, während die
 * Landschaft darunter entsteht. Gelandet wird erst, wenn der Boden bereit ist.
 */
public final class TimeStream {
    public static final ResourceLocation START = TimeMod.id("stream_start");
    public static final ResourceLocation ARRIVE = TimeMod.id("stream_arrive");
    /** Markiert Reisende auch über einen Absturz hinweg, damit niemand schwebend hängen bleibt. */
    public static final String TAG = "timemod.in_stream";
    private static final String TAG_HIDDEN = "timemod.stream_hidden";
    private static final int BASE_TICKS = 100;
    private static final int EXTRA_TICKS = 60;
    /** So lange wird höchstens auf das Ziel gewartet, danach wird erzwungen gelandet. */
    private static final int MAX_WAIT = 300;
    private static final int READY_RADIUS = 2;

    private static final Map<UUID, Journey> JOURNEYS = new HashMap<>();

    private TimeStream() {
    }

    /**
     * Eine laufende Reise.
     *
     * @param target      Zieljahr
     * @param near        wo gelandet wird (gleiche Stelle der Welt)
     * @param returnLevel wohin der Heimkehr-Anker zurückführt
     * @param returnPos   wo dort gelandet wird
     * @param anchor      ob am Ziel ein Heimkehr-Anker entstehen soll
     */
    private record Journey(Era target, BlockPos near, ResourceKey<Level> returnLevel, BlockPos returnPos, boolean anchor, int duration,
                           Vec3 hold, int[] ticks) {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TimeStream::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !JOURNEYS.containsKey(entity.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Journey journey = JOURNEYS.remove(handler.player.getUUID());
            if (journey != null) {
                land(handler.player, journey, false);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> recover(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID uuid : new ArrayList<>(JOURNEYS.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                Journey journey = JOURNEYS.remove(uuid);
                if (player != null && journey != null) {
                    land(player, journey, false);
                }
            }
            JOURNEYS.clear();
        });
    }

    public static boolean inStream(UUID player) {
        return JOURNEYS.containsKey(player);
    }

    /** Flugdauer: fünf Sekunden für ein Jahr, acht für 245 Millionen. */
    public static int duration(Era from, Era to) {
        double distance = Math.abs((double) to.years() - from.years());
        return BASE_TICKS + (int) Math.round(EXTRA_TICKS * Math.min(1.0, Math.log10(1 + distance) / 8.4));
    }

    /**
     * Beginnt eine Reise.
     *
     * @param landNear  wo im Ziel gelandet wird; null = an derselben Stelle der Welt
     * @param returnPos wohin der Heimkehr-Anker zurückführt (in der Welt, in der die Reise beginnt)
     * @param anchor    ob am Ziel ein Heimkehr-Anker entstehen soll
     */
    public static boolean depart(ServerPlayer player, Era target, @Nullable BlockPos landNear, BlockPos returnPos, boolean anchor) {
        MinecraftServer server = player.getServer();
        ServerLevel destination = server == null ? null : server.getLevel(target.level());
        if (destination == null || JOURNEYS.containsKey(player.getUUID()) || player.isOnPortalCooldown() || player.isPassenger()
            || player.isVehicle() || player.isSleeping() || !player.isAlive()) {
            return false;
        }
        Era from = Eras.byLevel(player.level().dimension());
        if (from == null) {
            from = Eras.PRESENT;
        }
        BlockPos near = landNear != null ? landNear : player.blockPosition();
        int duration = duration(from, target);
        Vec3 hold = new Vec3(near.getX() + 0.5, destination.getMaxBuildHeight() - 6, near.getZ() + 0.5);
        Journey journey = new Journey(target, near, player.level().dimension(), returnPos.immutable(), anchor && !target.isPresent(), duration, hold,
            new int[1]);
        JOURNEYS.put(player.getUUID(), journey);
        ParadoxPressure.onTravel(player, from, target);
        player.setPortalCooldown();
        player.addTag(TAG);
        // Erst ankündigen, dann die Welt wechseln: So zeigt der Client den Zeitstrom statt „Landschaft wird geladen“
        if (ServerPlayNetworking.canSend(player, START)) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(from.years());
            buf.writeInt(target.years());
            buf.writeVarInt(duration);
            ServerPlayNetworking.send(player, START, buf);
        }
        player.stopRiding();
        player.stopFallFlying();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(destination, hold.x, hold.y, hold.z, player.getYRot(), player.getXRot());
        player.setNoGravity(true);
        // Unsichtbar, solange man über dem Ziel schwebt – ein eigener Trank bleibt unangetastet
        if (!player.hasEffect(MobEffects.INVISIBILITY)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration + MAX_WAIT + 20, 0, false, false, false));
            player.addTag(TAG_HIDDEN);
        }
        player.resetFallDistance();
        destination.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(near), 3, near);
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (JOURNEYS.isEmpty()) {
            return;
        }
        List<UUID> landed = new ArrayList<>();
        for (Map.Entry<UUID, Journey> entry : JOURNEYS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Journey journey = entry.getValue();
            if (player == null) {
                landed.add(entry.getKey());
                continue;
            }
            int ticks = ++journey.ticks()[0];
            // In der Schwebe halten, bis der Boden bereit ist
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
            if (player.position().distanceToSqr(journey.hold()) > 0.25) {
                player.teleportTo(journey.hold().x, journey.hold().y, journey.hold().z);
            }
            if ((ticks >= journey.duration() && isReady(player.serverLevel(), journey.near())) || ticks >= journey.duration() + MAX_WAIT) {
                landed.add(entry.getKey());
            }
        }
        for (UUID uuid : landed) {
            Journey journey = JOURNEYS.remove(uuid);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (journey != null && player != null) {
                land(player, journey, true);
            }
        }
    }

    private static boolean isReady(ServerLevel level, BlockPos near) {
        ChunkPos center = new ChunkPos(near);
        for (int dx = -READY_RADIUS; dx <= READY_RADIUS; dx++) {
            for (int dz = -READY_RADIUS; dz <= READY_RADIUS; dz++) {
                if (level.getChunkSource().getChunkNow(center.x + dx, center.z + dz) == null
                    || !FutureDerivation.isSettled(level, new ChunkPos(center.x + dx, center.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Landen: sicherer Boden, Anker daneben, Titel und Ankunftsklang. */
    private static void land(ServerPlayer player, Journey journey, boolean announce) {
        ServerLevel level = player.server.getLevel(journey.target().level());
        if (level == null) {
            level = player.serverLevel();
        }
        Vec3 landing = TimeTravel.landing(level, journey.near());
        player.setNoGravity(false);
        player.removeTag(TAG);
        reveal(player);
        player.teleportTo(level, landing.x, landing.y, landing.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.setPortalCooldown();
        if (journey.anchor()) {
            HomeAnchors.placeNear(level, BlockPos.containing(landing), journey.returnLevel(), journey.returnPos());
        }
        if (!announce) {
            return;
        }
        level.playSound(null, landing.x, landing.y, landing.z, ModSounds.GATE_ARRIVE, SoundSource.PLAYERS, 1.2F, 1.0F);
        if (ServerPlayNetworking.canSend(player, ARRIVE)) {
            ServerPlayNetworking.send(player, ARRIVE, new FriendlyByteBuf(Unpooled.buffer()));
        }
        player.connection.send(new ClientboundSetTitleTextPacket(journey.target().yearText().copy().withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(journey.target().title().copy().withStyle(ChatFormatting.GRAY)));
        ModAdvancements.award(player, "first_journey");
        if (journey.target().age() == Era.Age.MESOZOIC) {
            ModAdvancements.award(player, "mesozoic");
        }
        if (journey.target().years() >= 1000) {
            ModAdvancements.award(player, "far_future");
        }
    }

    private static void reveal(ServerPlayer player) {
        if (player.getTags().contains(TAG_HIDDEN)) {
            player.removeTag(TAG_HIDDEN);
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    /** Wer mitten im Flug das Spiel verlassen hat (oder der Server abgestürzt ist), landet beim nächsten Betreten. */
    private static void recover(ServerPlayer player) {
        if (player.getTags().contains(TAG) && !JOURNEYS.containsKey(player.getUUID())) {
            player.setNoGravity(false);
            player.removeTag(TAG);
            reveal(player);
            Vec3 landing = TimeTravel.landing(player.serverLevel(), player.blockPosition());
            player.teleportTo(landing.x, landing.y, landing.z);
            player.resetFallDistance();
        }
    }
}

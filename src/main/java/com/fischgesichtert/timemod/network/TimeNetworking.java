package com.fischgesichtert.timemod.network;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Netzwerk: Der Server schickt jedem Spieler die Zeitzonen seiner aktuellen Welt. */
public final class TimeNetworking {
    public static final ResourceLocation ZONES = TimeMod.id("zones");

    private TimeNetworking() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendZonesTo(handler.getPlayer()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sendZonesTo(player));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer player) {
                sendZonesTo(player);
            }
        });
    }

    public static FriendlyByteBuf encode(List<TimeZone> zones) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(zones.size());
        for (TimeZone zone : zones) {
            zone.write(buf);
        }
        return buf;
    }

    public static void sendZones(ServerLevel level, List<TimeZone> zones) {
        for (ServerPlayer player : level.players()) {
            if (ServerPlayNetworking.canSend(player, ZONES)) {
                ServerPlayNetworking.send(player, ZONES, encode(zones));
            }
        }
    }

    public static void sendZonesTo(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ZONES)) {
            return;
        }
        var engine = ((TimeLevel) player.serverLevel()).timemod$getEngine();
        List<TimeZone> zones = engine == null ? List.of() : engine.zones();
        ServerPlayNetworking.send(player, ZONES, encode(zones));
    }
}

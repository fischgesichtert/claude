package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.network.Aging;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Wie alt wirkt ein Wesen? (vom Lebenszeit-Extraktor; 0 = jung, 1 = am Ende) */
public final class ClientAging {
    private static final Int2FloatMap AGE = new Int2FloatOpenHashMap();

    private ClientAging() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Aging.PACKET, (client, handler, buf, sender) -> {
            int id = buf.readVarInt();
            float age = buf.readFloat();
            client.execute(() -> AGE.put(id, Math.max(AGE.getOrDefault(id, 0.0F), age)));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AGE.clear());
    }

    public static float age(int entityId) {
        return AGE.getOrDefault(entityId, 0.0F);
    }

    public static void forget(int entityId) {
        AGE.remove(entityId);
    }
}

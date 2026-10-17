package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.network.TimeNetworking;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TimeModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TimeNetworking.ZONES, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<TimeZone> zones = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                zones.add(TimeZone.read(buf));
            }
            client.execute(() -> {
                if (client.level != null && ((TimeLevel) client.level).timemod$getEngine() instanceof ClientTimeEngine engine) {
                    engine.setZones(zones);
                }
            });
        });
    }
}

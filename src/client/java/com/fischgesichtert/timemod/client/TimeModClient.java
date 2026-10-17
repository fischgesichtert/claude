package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.effect.TimeStopEffect;
import com.fischgesichtert.timemod.network.TimeNetworking;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class TimeModClient implements ClientModInitializer {
    private StopAmbienceSound ambience;

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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TimeStopEffect.clear());
        WorldRenderEvents.END.register(TimeStopEffect::render);
        ClientTickEvents.END_CLIENT_TICK.register(this::tickAmbience);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return TimeMod.id("time_stop_shader");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                TimeStopEffect.reset();
            }
        });
    }

    private void tickAmbience(Minecraft mc) {
        if (mc.level == null) {
            ambience = null;
            return;
        }
        if (TimeStopEffect.cameraStillness() > 0.0F && (ambience == null || ambience.isStopped())) {
            ambience = new StopAmbienceSound();
            mc.getSoundManager().play(ambience);
        }
    }
}

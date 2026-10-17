package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.chronos.ChronosClient;
import com.fischgesichtert.timemod.client.effect.RewindEffect;
import com.fischgesichtert.timemod.client.effect.TimeStopEffect;
import com.fischgesichtert.timemod.client.effect.ZoneSight;
import com.fischgesichtert.timemod.client.model.ChronophageModel;
import com.fischgesichtert.timemod.client.model.ClockmakerModel;
import com.fischgesichtert.timemod.client.render.ChronoKnifeRenderer;
import com.fischgesichtert.timemod.client.render.ChronophageRenderer;
import com.fischgesichtert.timemod.client.render.ClockmakerRenderer;
import com.fischgesichtert.timemod.client.render.ParadoxRenderer;
import com.fischgesichtert.timemod.client.render.SpinningItemRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import com.fischgesichtert.timemod.client.render.TemporalWorkbenchRenderer;
import com.fischgesichtert.timemod.client.render.TimeCondenserRenderer;
import com.fischgesichtert.timemod.client.screen.ChronikScreen;
import com.fischgesichtert.timemod.client.screen.ClockmakerScreen;
import com.fischgesichtert.timemod.item.ChronikItem;
import com.fischgesichtert.timemod.client.screen.TemporalWorkbenchScreen;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModMenus;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
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
    private ConditionalLoopSound stopAmbience;
    private ConditionalLoopSound rewindLoop;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.CLOCKMAKER, ClockmakerScreen::new);
        ChronikItem.opener = () -> Minecraft.getInstance().setScreen(new ChronikScreen());
        MenuScreens.register(ModMenus.TEMPORAL_WORKBENCH, TemporalWorkbenchScreen::new);
        BlockEntityRenderers.register(ModBlockEntities.TEMPORAL_WORKBENCH, TemporalWorkbenchRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TEMPORAL_WORKBENCH, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CHRONO_PILLAR, RenderType.cutout());
        EntityRendererRegistry.register(ModEntities.CHRONO_KNIFE, ChronoKnifeRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ClockmakerModel.LAYER, ClockmakerModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(ChronophageModel.LAYER, ChronophageModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.CLOCKMAKER, ClockmakerRenderer::new);
        EntityRendererRegistry.register(ModEntities.CHRONOPHAGE, ChronophageRenderer::new);
        EntityRendererRegistry.register(ModEntities.PARADOX, context -> new ParadoxRenderer<>(context, false));
        EntityRendererRegistry.register(ModEntities.PARADOX_ECHO, context -> new ParadoxRenderer<>(context, true));
        EntityRendererRegistry.register(ModEntities.GEAR_SAW, ctx -> new SpinningItemRenderer<>(ctx, 1.6F, 45.0F, false));
        EntityRendererRegistry.register(ModEntities.TIME_LEECH, ctx -> new SpinningItemRenderer<>(ctx, 0.9F, -12.0F, true));
        BlockEntityRenderers.register(ModBlockEntities.TIME_CONDENSER, TimeCondenserRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TIME_GLASS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DEEPSLATE_CHRONITE_ORE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CHRONITE_SPROUT, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TIME_CONDENSER, RenderType.translucent());

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
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ZoneSight::render);
        ClientAging.init();
        ChronosClient.init();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LIFETIME_EXTRACTOR, RenderType.cutout());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientClock.tick());
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
            stopAmbience = null;
            rewindLoop = null;
            return;
        }
        if (TimeStopEffect.cameraStillness() > 0.0F && (stopAmbience == null || stopAmbience.isStopped())) {
            stopAmbience = new ConditionalLoopSound(ModSounds.STOP_AMBIENCE, 0.9F, () -> TimeStopEffect.cameraStillness() > 0.0F);
            mc.getSoundManager().play(stopAmbience);
        }
        if (RewindEffect.cameraInRewind() && (rewindLoop == null || rewindLoop.isStopped())) {
            rewindLoop = new ConditionalLoopSound(ModSounds.REWIND, 0.7F, RewindEffect::cameraInRewind);
            mc.getSoundManager().play(rewindLoop);
        }
    }
}

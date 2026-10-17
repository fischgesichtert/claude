package com.fischgesichtert.timemod;

import com.fischgesichtert.timemod.command.ChronoCommand;
import com.fischgesichtert.timemod.network.TimeNetworking;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeMod implements ModInitializer {
    public static final String MOD_ID = "timemod";
    public static final Logger LOGGER = LoggerFactory.getLogger("TIME");
    public static final boolean DEBUG = Boolean.getBoolean("timemod.debug");

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModItems.init();
        TimeNetworking.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ChronoCommand.register(dispatcher));
        LOGGER.info("TIME geladen – die Zeit gehört dir.");
    }
}

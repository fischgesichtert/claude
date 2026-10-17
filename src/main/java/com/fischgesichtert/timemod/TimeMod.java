package com.fischgesichtert.timemod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeMod implements ModInitializer {
    public static final String MOD_ID = "timemod";
    public static final Logger LOGGER = LoggerFactory.getLogger("TIME");

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("TIME geladen – die Zeit gehört dir.");
    }
}

package com.fischgesichtert.timemod;

import com.fischgesichtert.timemod.armor.ChronoArmorItem;
import com.fischgesichtert.timemod.command.ChronoCommand;
import com.fischgesichtert.timemod.item.ChronikGift;
import com.fischgesichtert.timemod.era.Timeline;
import com.fischgesichtert.timemod.future.ChangeTracking;
import com.fischgesichtert.timemod.future.FutureDerivation;
import com.fischgesichtert.timemod.future.WeatheringRules;
import com.fischgesichtert.timemod.gate.TimeStream;
import com.fischgesichtert.timemod.item.TimeLoopItem;
import com.fischgesichtert.timemod.network.ChronosNetworking;
import com.fischgesichtert.timemod.network.TimeNetworking;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModLoot;
import com.fischgesichtert.timemod.registry.ModMenus;
import com.fischgesichtert.timemod.registry.ModRecipes;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.registry.ModWorldgen;
import com.fischgesichtert.timemod.rewind.PlayerHistory;
import com.fischgesichtert.timemod.rewind.RecordingDemand;
import com.fischgesichtert.timemod.time.TimeImmunity;
import com.fischgesichtert.timemod.worldgen.ModStructures;
import com.fischgesichtert.timemod.future.town.TownPieces;
import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import com.fischgesichtert.timemod.worldgen.RemappedBiomeSource;
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
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModEntities.init();
        ModMenus.init();
        ModRecipes.init();
        ModLoot.init();
        ModWorldgen.init();
        ModStructures.init();
        RemappedBiomeSource.init();
        FutureBiomeSource.init();
        TownPieces.init();
        PlayerHistory.init();
        RecordingDemand.init();
        TimeNetworking.init();
        ChronosNetworking.init();
        TimeImmunity.register(ChronoArmorItem::hasFullSet);
        ChronikGift.init();
        TimeLoopItem.init();
        TimeStream.init();
        Timeline.init();
        WeatheringRules.init();
        ChangeTracking.init();
        FutureDerivation.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ChronoCommand.register(dispatcher));
        LOGGER.info("TIME geladen – die Zeit gehört dir.");
    }
}

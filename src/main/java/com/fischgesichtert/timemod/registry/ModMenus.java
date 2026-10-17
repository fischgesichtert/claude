package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.menu.ChronosConsoleMenu;
import com.fischgesichtert.timemod.menu.ClockmakerMenu;
import com.fischgesichtert.timemod.menu.TemporalWorkbenchMenu;
import com.fischgesichtert.timemod.menu.TimeCapsuleMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<ClockmakerMenu> CLOCKMAKER = Registry.register(BuiltInRegistries.MENU, TimeMod.id("clockmaker_bench"),
        new MenuType<>(ClockmakerMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<TemporalWorkbenchMenu> TEMPORAL_WORKBENCH = Registry.register(BuiltInRegistries.MENU, TimeMod.id("temporal_workbench"),
        new MenuType<>(TemporalWorkbenchMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<ChronosConsoleMenu> CHRONOS_CONSOLE = Registry.register(BuiltInRegistries.MENU, TimeMod.id("chronos_console"),
        new MenuType<>(ChronosConsoleMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<TimeCapsuleMenu> TIME_CAPSULE = Registry.register(BuiltInRegistries.MENU, TimeMod.id("time_capsule"),
        new ExtendedScreenHandlerType<>(TimeCapsuleMenu::new));

    private ModMenus() {
    }

    public static void init() {
    }
}

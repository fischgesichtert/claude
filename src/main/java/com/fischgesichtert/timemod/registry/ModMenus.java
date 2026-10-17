package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.menu.ClockmakerMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<ClockmakerMenu> CLOCKMAKER = Registry.register(BuiltInRegistries.MENU, TimeMod.id("clockmaker_bench"),
        new MenuType<>(ClockmakerMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }

    public static void init() {
    }
}

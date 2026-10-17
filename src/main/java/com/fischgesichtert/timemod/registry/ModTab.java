package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Eigener Kreativ-Reiter „TIME“. */
public final class ModTab {
    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TimeMod.id("time"),
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.timemod.time"))
            .icon(() -> new ItemStack(ModItems.POCKET_WATCH))
            .displayItems((params, output) -> {
                for (Item item : ModItems.TAB_ORDER) {
                    output.accept(item);
                }
            })
            .build());

    private ModTab() {
    }

    static void init() {
    }
}

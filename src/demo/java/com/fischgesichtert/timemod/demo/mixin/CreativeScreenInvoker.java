package com.fischgesichtert.timemod.demo.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Damit die Vorführung den TIME-Reiter direkt aufschlagen kann. */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeScreenInvoker {
    @Invoker("selectTab")
    void timemod$selectTab(CreativeModeTab tab);

    @Accessor("selectedTab")
    static CreativeModeTab timemod$selectedTab() {
        throw new AssertionError();
    }
}

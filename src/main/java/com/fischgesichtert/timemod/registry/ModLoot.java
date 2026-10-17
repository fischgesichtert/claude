package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.safety.Safety;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootItem;

/**
 * Fossilien liegen unter dem Pinsel: in verdächtigem Sand und Kies der Wüstentempel, Brunnen,
 * Ozeanruinen und Pfadruinen. Bernstein brennt im Ofen – nicht umsonst heißt er „Brennstein“.
 */
public final class ModLoot {
    /** Archäologie-Tabellen und das Gewicht des Fossils darin (die Pfadruinen haben viele Einträge). */
    private static final Map<ResourceLocation, Integer> ARCHAEOLOGY = Map.of(
        new ResourceLocation("archaeology/desert_pyramid"), 2,
        new ResourceLocation("archaeology/desert_well"), 2,
        new ResourceLocation("archaeology/ocean_ruin_cold"), 2,
        new ResourceLocation("archaeology/ocean_ruin_warm"), 2,
        new ResourceLocation("archaeology/trail_ruins_common"), 6,
        new ResourceLocation("archaeology/trail_ruins_rare"), 2);

    private ModLoot() {
    }

    public static void init() {
        LootTableEvents.MODIFY.register((resources, loot, id, table, source) -> Safety.run("Beute: Fossilien", () -> {
            Integer weight = ARCHAEOLOGY.get(id);
            if (weight != null && source.isBuiltin()) {
                // In denselben Pool: Ein Pinselstrich bringt genau einen Fund hervor
                table.modifyPools(pool -> pool.add(LootItem.lootTableItem(ModItems.FOSSIL_FRAGMENT).setWeight(weight)));
            }
        }));
        FuelRegistry.INSTANCE.add(ModItems.AMBER, 1200);
        FuelRegistry.INSTANCE.add(ModItems.AMBER_BLOCK, 12_000);
    }
}

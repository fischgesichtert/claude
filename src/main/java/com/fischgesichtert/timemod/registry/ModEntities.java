package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.ThrownChronoKnife;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<ThrownChronoKnife> CHRONO_KNIFE = Registry.register(BuiltInRegistries.ENTITY_TYPE, TimeMod.id("chrono_knife"),
        FabricEntityTypeBuilder.<ThrownChronoKnife>create(MobCategory.MISC, ThrownChronoKnife::new)
            .dimensions(EntityDimensions.fixed(0.5F, 0.5F)).trackRangeChunks(4).trackedUpdateRate(20).build());

    private ModEntities() {
    }

    public static void init() {
    }
}

package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.GearSaw;
import com.fischgesichtert.timemod.entity.StasisBolt;
import com.fischgesichtert.timemod.entity.ThrownChronoKnife;
import com.fischgesichtert.timemod.entity.TimeLeech;
import com.fischgesichtert.timemod.entity.TimeWarden;
import com.fischgesichtert.timemod.entity.boss.ChronophageBoss;
import com.fischgesichtert.timemod.entity.boss.ClockmakerBoss;
import com.fischgesichtert.timemod.entity.boss.ParadoxBoss;
import com.fischgesichtert.timemod.entity.boss.ParadoxEcho;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<ThrownChronoKnife> CHRONO_KNIFE = register("chrono_knife",
        FabricEntityTypeBuilder.<ThrownChronoKnife>create(MobCategory.MISC, ThrownChronoKnife::new)
            .dimensions(EntityDimensions.fixed(0.5F, 0.5F)).trackRangeChunks(4).trackedUpdateRate(20).build());
    public static final EntityType<GearSaw> GEAR_SAW = register("gear_saw",
        FabricEntityTypeBuilder.<GearSaw>create(MobCategory.MISC, GearSaw::new)
            .dimensions(EntityDimensions.fixed(0.6F, 0.6F)).trackRangeChunks(6).trackedUpdateRate(2).build());
    public static final EntityType<TimeLeech> TIME_LEECH = register("time_leech",
        FabricEntityTypeBuilder.<TimeLeech>create(MobCategory.MISC, TimeLeech::new)
            .dimensions(EntityDimensions.fixed(0.4F, 0.4F)).trackRangeChunks(6).trackedUpdateRate(2).build());
    public static final EntityType<ClockmakerBoss> CLOCKMAKER = register("clockmaker",
        FabricEntityTypeBuilder.<ClockmakerBoss>create(MobCategory.MONSTER, ClockmakerBoss::new)
            .dimensions(EntityDimensions.fixed(1.5F, 3.3F)).fireImmune().trackRangeChunks(10).build());
    public static final EntityType<ChronophageBoss> CHRONOPHAGE = register("chronophage",
        FabricEntityTypeBuilder.<ChronophageBoss>create(MobCategory.MONSTER, ChronophageBoss::new)
            .dimensions(EntityDimensions.fixed(2.4F, 2.2F)).fireImmune().trackRangeChunks(10).build());
    public static final EntityType<ParadoxBoss> PARADOX = register("paradox",
        FabricEntityTypeBuilder.<ParadoxBoss>create(MobCategory.MONSTER, ParadoxBoss::new)
            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)).fireImmune().trackRangeChunks(10).build());
    public static final EntityType<ParadoxEcho> PARADOX_ECHO = register("paradox_echo",
        FabricEntityTypeBuilder.<ParadoxEcho>create(MobCategory.MONSTER, ParadoxEcho::new)
            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)).fireImmune().trackRangeChunks(8).build());
    public static final EntityType<TimeWarden> TIME_WARDEN = register("time_warden",
        FabricEntityTypeBuilder.<TimeWarden>create(MobCategory.MONSTER, TimeWarden::new)
            .dimensions(EntityDimensions.fixed(0.7F, 2.3F)).fireImmune().trackRangeChunks(10).build());
    public static final EntityType<StasisBolt> STASIS_BOLT = register("stasis_bolt",
        FabricEntityTypeBuilder.<StasisBolt>create(MobCategory.MISC, StasisBolt::new)
            .dimensions(EntityDimensions.fixed(0.5F, 0.5F)).trackRangeChunks(6).trackedUpdateRate(2).build());

    private ModEntities() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, TimeMod.id(name), type);
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(CLOCKMAKER, ClockmakerBoss.createAttributes());
        FabricDefaultAttributeRegistry.register(CHRONOPHAGE, ChronophageBoss.createAttributes());
        FabricDefaultAttributeRegistry.register(PARADOX, ParadoxBoss.createAttributes());
        FabricDefaultAttributeRegistry.register(PARADOX_ECHO, ParadoxEcho.createAttributes());
        FabricDefaultAttributeRegistry.register(TIME_WARDEN, TimeWarden.createAttributes());
    }
}

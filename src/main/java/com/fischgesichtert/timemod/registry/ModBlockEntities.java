package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.entity.TimeCondenserBlockEntity;
import com.fischgesichtert.timemod.block.entity.TimeFieldProjectorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<TimeCondenserBlockEntity> TIME_CONDENSER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
        TimeMod.id("time_condenser"), FabricBlockEntityTypeBuilder.create(TimeCondenserBlockEntity::new, ModBlocks.TIME_CONDENSER).build());
    public static final BlockEntityType<TimeFieldProjectorBlockEntity> TIME_FIELD_PROJECTOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
        TimeMod.id("time_field_projector"), FabricBlockEntityTypeBuilder.create(TimeFieldProjectorBlockEntity::new, ModBlocks.TIME_FIELD_PROJECTOR).build());

    private ModBlockEntities() {
    }

    public static void init() {
    }
}

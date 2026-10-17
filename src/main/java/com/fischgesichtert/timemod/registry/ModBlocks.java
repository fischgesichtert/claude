package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.ChronoPillarBlock;
import com.fischgesichtert.timemod.block.ChroniteOreBlock;
import com.fischgesichtert.timemod.block.ChroniteSproutBlock;
import com.fischgesichtert.timemod.block.ClockmakerBenchBlock;
import com.fischgesichtert.timemod.block.TemporalWorkbenchBlock;
import com.fischgesichtert.timemod.block.TimeCondenserBlock;
import com.fischgesichtert.timemod.block.TimeFieldProjectorBlock;
import com.fischgesichtert.timemod.block.TimeSandBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Block BRASS_BLOCK = register("brass_block", new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(4.0F, 6.0F).sound(SoundType.METAL)));
    public static final Block CLOCKMAKER_BENCH = register("clockmaker_bench", new ClockmakerBenchBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD)));
    public static final Block TIME_SAND = register("time_sand", new TimeSandBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.6F).sound(SoundType.SAND).lightLevel(s -> 3)));
    public static final Block TIME_GLASS = register("time_glass", new GlassBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_CYAN).strength(0.4F).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 5)
        .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final Block DEEPSLATE_CHRONITE_ORE = register("deepslate_chronite_ore", new ChroniteOreBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.5F, 3.0F)
        .sound(SoundType.DEEPSLATE).lightLevel(s -> s.getValue(ChroniteOreBlock.PHASED) ? 9 : 4).noOcclusion()));
    public static final Block CHRONITE_SPROUT = register("chronite_sprout", new ChroniteSproutBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_PURPLE).strength(1.5F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().randomTicks()
        .lightLevel(s -> 3 + 3 * s.getValue(ChroniteSproutBlock.AGE))));
    public static final Block TIME_CONDENSER = register("time_condenser", new TimeCondenserBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(4.0F, 8.0F).sound(SoundType.METAL).lightLevel(s -> 6).noOcclusion()));
    public static final Block TIME_FIELD_PROJECTOR = register("time_field_projector", new TimeFieldProjectorBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5.0F, 9.0F).sound(SoundType.METAL)
        .lightLevel(s -> s.getValue(TimeFieldProjectorBlock.ACTIVE) ? 12 : 3)));
    public static final Block TEMPORAL_WORKBENCH = register("temporal_workbench", new TemporalWorkbenchBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(6.0F, 12.0F).sound(SoundType.NETHERITE_BLOCK)
        .lightLevel(s -> s.getValue(TemporalWorkbenchBlock.FORMED) ? 13 : 4).noOcclusion()));
    public static final Block CHRONO_PILLAR = register("chrono_pillar", new ChronoPillarBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(5.0F, 9.0F).sound(SoundType.METAL).lightLevel(s -> 7).noOcclusion()));

    private ModBlocks() {
    }

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, TimeMod.id(name), block);
    }

    public static void init() {
        // Laden der Klasse registriert die Blöcke.
    }
}

package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.BossAltarBlock;
import com.fischgesichtert.timemod.block.BurntTorchBlock;
import com.fischgesichtert.timemod.block.ChronoPillarBlock;
import com.fischgesichtert.timemod.block.ChronosConsoleBlock;
import com.fischgesichtert.timemod.block.ChronosPortalBlock;
import com.fischgesichtert.timemod.block.HomeAnchorBlock;
import com.fischgesichtert.timemod.block.TimeCapsuleBlock;
import com.fischgesichtert.timemod.block.ChroniteOreBlock;
import com.fischgesichtert.timemod.block.ChroniteSproutBlock;
import com.fischgesichtert.timemod.block.ClockmakerBenchBlock;
import com.fischgesichtert.timemod.block.LifetimeExtractorBlock;
import com.fischgesichtert.timemod.block.TemporalWorkbenchBlock;
import com.fischgesichtert.timemod.block.TimeCondenserBlock;
import com.fischgesichtert.timemod.block.TimeFieldProjectorBlock;
import com.fischgesichtert.timemod.block.TimeSandBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

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
    public static final Block LIFETIME_EXTRACTOR = register("lifetime_extractor", new LifetimeExtractorBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(6.0F, 12.0F).sound(SoundType.NETHERITE_BLOCK)
        .lightLevel(s -> s.getValue(LifetimeExtractorBlock.ACTIVE) ? 9 : 2).noOcclusion()));
    public static final Block CLOCKMAKER_ALTAR = register("clockmaker_altar", new BossAltarBlock("message.timemod.altar.clockmaker",
        BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)
            .lightLevel(s -> 10).noOcclusion()));
    public static final Block PHAGE_ALTAR = register("phage_altar", new BossAltarBlock("message.timemod.altar.phage",
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.SCULK_CATALYST)
            .lightLevel(s -> 8).noOcclusion()));
    public static final Block PARADOX_ALTAR = register("paradox_altar", new BossAltarBlock("message.timemod.altar.paradox",
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.AMETHYST)
            .lightLevel(s -> 12).noOcclusion()));


    // Stufe V: die Zeitmaschine
    public static final Block CHRONOS_GATE_FRAME = register("chronos_gate_frame", new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(8.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 4)));
    public static final Block CHRONOS_CONSOLE = register("chronos_console", new ChronosConsoleBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(6.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 9).noOcclusion()));
    public static final Block CHRONOS_PORTAL = register("chronos_portal", new ChronosPortalBlock(BlockBehaviour.Properties.of()
        .noCollission().strength(-1.0F, 3600000.0F).sound(SoundType.GLASS).lightLevel(s -> 12).noLootTable().pushReaction(PushReaction.BLOCK)
        .noOcclusion()));
    public static final Block HOME_ANCHOR = register("home_anchor", new HomeAnchorBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F, 1200.0F).sound(SoundType.AMETHYST).lightLevel(s -> 10).noOcclusion().noLootTable()));
    public static final Block TIME_CAPSULE = register("time_capsule", new TimeCapsuleBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(3.0F, 1200.0F).sound(SoundType.METAL).lightLevel(s -> 3).noOcclusion()));
    // Bernstein aus dem Pleistozän (Brennstein: er brennt wirklich)
    public static final Block AMBER_ORE = register("amber_ore", new DropExperienceBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(1, 4)));
    public static final Block AMBER_BLOCK = register("amber_block", new GlassBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_ORANGE).strength(1.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 6)
        .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    // Zukunft: was die Jahre aus Licht, Eisen und Tieren machen
    public static final Block BURNT_TORCH = register("burnt_torch", new BurntTorchBlock(BlockBehaviour.Properties.of()
        .noCollission().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)));
    public static final Block BURNT_WALL_TORCH = register("burnt_wall_torch", new BurntTorchBlock.Wall(BlockBehaviour.Properties.of()
        .noCollission().instabreak().sound(SoundType.WOOD).dropsLike(BURNT_TORCH).pushReaction(PushReaction.DESTROY)));
    public static final Block DULL_LANTERN = register("dull_lantern", new LanternBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL).forceSolidOn().requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.LANTERN).noOcclusion()
        .pushReaction(PushReaction.DESTROY)));
    public static final Block RUSTY_IRON_BLOCK = register("rusty_iron_block", new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(4.0F, 5.0F)
        .sound(SoundType.METAL)));
    public static final Block RUSTY_IRON_BARS = register("rusty_iron_bars", new IronBarsBlock(BlockBehaviour.Properties.of()
        .requiresCorrectToolForDrops().strength(4.0F, 5.0F).sound(SoundType.METAL).noOcclusion()));
    public static final Block BONE_PILE = register("bone_pile", new CarpetBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.SAND).strength(0.3F).sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {
    }

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, TimeMod.id(name), block);
    }

    public static void init() {
        // Laden der Klasse registriert die Blöcke.
    }
}

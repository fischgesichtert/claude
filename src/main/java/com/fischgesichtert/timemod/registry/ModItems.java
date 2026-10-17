package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.armor.ChronoArmorItem;
import com.fischgesichtert.timemod.armor.ChronoUpgradeTemplate;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.energy.ChrononCellItem;
import com.fischgesichtert.timemod.item.BrakeSpringItem;
import com.fischgesichtert.timemod.item.ChronikItem;
import com.fischgesichtert.timemod.item.ChronoDebugWandItem;
import com.fischgesichtert.timemod.item.ChronoKnifeItem;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.item.EraFocusItem;
import com.fischgesichtert.timemod.item.GrowthHourglassItem;
import com.fischgesichtert.timemod.item.MomentLensItem;
import com.fischgesichtert.timemod.item.PocketWatchItem;
import com.fischgesichtert.timemod.item.RecallWatchItem;
import com.fischgesichtert.timemod.item.RewindSpoolItem;
import com.fischgesichtert.timemod.item.SlowPendulumItem;
import com.fischgesichtert.timemod.item.StasisWatchItem;
import com.fischgesichtert.timemod.item.TimeAcceleratorItem;
import com.fischgesichtert.timemod.item.TimeLoopItem;
import com.fischgesichtert.timemod.module.ModuleItem;
import com.fischgesichtert.timemod.time.TimeFilter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class ModItems {
    /** Reihenfolge im Kreativ-Reiter. */
    static final List<Item> TAB_ORDER = new ArrayList<>();

    // Handbuch
    public static final Item CHRONIK = register("chronik", new ChronikItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Werkzeuge
    public static final Item POCKET_WATCH = register("pocket_watch", new PocketWatchItem(new Item.Properties()));
    public static final Item GROWTH_HOURGLASS = register("growth_hourglass", new GrowthHourglassItem(new Item.Properties()));
    public static final Item BRAKE_SPRING = register("brake_spring", new BrakeSpringItem(new Item.Properties()));
    public static final Item MOMENT_LENS = register("moment_lens", new MomentLensItem(new Item.Properties()));
    public static final Item RECALL_WATCH = register("recall_watch", new RecallWatchItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Item STASIS_WATCH = register("stasis_watch", new StasisWatchItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Item TIME_ACCELERATOR = register("time_accelerator", new TimeAcceleratorItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Item SLOW_PENDULUM = register("slow_pendulum", new SlowPendulumItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Item REWIND_SPOOL = register("rewind_spool", new RewindSpoolItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Item CHRONO_KNIFE = register("chrono_knife", new ChronoKnifeItem(new Item.Properties().stacksTo(16)));

    // Energie
    public static final Item POCKET_TIME_CELL = register("pocket_time_cell", new ChrononCellItem(5_000, new Item.Properties().stacksTo(1)));
    public static final Item CHRONITE_TIME_CELL = register("chronite_time_cell", new ChrononCellItem(25_000, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Materialien
    public static final Item BRASS_INGOT = register("brass_ingot", new Item(new Item.Properties()));
    public static final Item BRASS_NUGGET = register("brass_nugget", new Item(new Item.Properties()));
    public static final Item BRASS_GEAR = register("brass_gear", new Item(new Item.Properties()));
    public static final Item MAINSPRING = register("mainspring", new Item(new Item.Properties()));
    public static final Item BALANCE_WHEEL = register("balance_wheel", new Item(new Item.Properties()));
    public static final Item CLOCKWORK = register("clockwork", new Item(new Item.Properties()));
    public static final Item RAW_CHRONITE = register("raw_chronite", new Item(new Item.Properties()));
    public static final Item CHRONITE_SEED = register("chronite_seed", new BlockItem(ModBlocks.CHRONITE_SPROUT, new Item.Properties()));
    public static final Item CHRONITE_CRYSTAL = register("chronite_crystal", new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Item TIME_DUST = register("time_dust", new Item(new Item.Properties()));

    // Blöcke
    public static final Item BRASS_BLOCK = block(ModBlocks.BRASS_BLOCK);
    public static final Item CLOCKMAKER_BENCH = block(ModBlocks.CLOCKMAKER_BENCH);
    public static final Item TIME_SAND = block(ModBlocks.TIME_SAND);
    public static final Item TIME_GLASS = block(ModBlocks.TIME_GLASS);
    public static final Item DEEPSLATE_CHRONITE_ORE = block(ModBlocks.DEEPSLATE_CHRONITE_ORE);
    public static final Item TIME_CONDENSER = block(ModBlocks.TIME_CONDENSER);
    public static final Item TIME_FIELD_PROJECTOR = block(ModBlocks.TIME_FIELD_PROJECTOR);

    // Stufe III: Temporaltechnik
    public static final Item TEMPORAL_WORKBENCH = block(ModBlocks.TEMPORAL_WORKBENCH);
    public static final Item CHRONO_PILLAR = block(ModBlocks.CHRONO_PILLAR);
    public static final Item CLOCKWORK_CORE = register("clockwork_core", new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
    public static final Item PHAGE_HEART = register("phage_heart", new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));
    public static final Item TIME_CORE_FRAGMENT = register("time_core_fragment", new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Item CLOCKMAKER_ALTAR = block(ModBlocks.CLOCKMAKER_ALTAR);
    public static final Item CLOCKMAKER_SPAWN_EGG = register("clockmaker_spawn_egg", new SpawnEggItem(ModEntities.CLOCKMAKER, 0xB0782A, 0x6FE3FF, new Item.Properties()));
    public static final Item CHRONOPHAGE_SPAWN_EGG = register("chronophage_spawn_egg", new SpawnEggItem(ModEntities.CHRONOPHAGE, 0x24142E, 0xD050E0, new Item.Properties()));
    public static final Item PHAGE_ALTAR = block(ModBlocks.PHAGE_ALTAR);
    public static final Item CHRONITE_PLATE = register("chronite_plate", new Item(new Item.Properties().fireResistant()));
    public static final Item CHRONO_UPGRADE_TEMPLATE = register("chrono_upgrade_template", ChronoUpgradeTemplate.create());
    public static final Item CHRONO_HELMET = register("chrono_helmet", new ChronoArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item CHRONO_CHESTPLATE = register("chrono_chestplate", new ChronoArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item CHRONO_LEGGINGS = register("chrono_leggings", new ChronoArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item CHRONO_BOOTS = register("chrono_boots", new ChronoArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item PARADOX_TIME_CELL = register("paradox_time_cell", new ChrononCellItem(100_000, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final Item LIFETIME_EXTRACTOR = block(ModBlocks.LIFETIME_EXTRACTOR);
    public static final Item BLANK_MODULE = module("blank_module", ModuleItem.Kind.BLANK, 0);
    public static final Item FILTER_ANIMALS = module("filter_animals", ModuleItem.Kind.FILTER, TimeFilter.ANIMALS);
    public static final Item FILTER_MONSTERS = module("filter_monsters", ModuleItem.Kind.FILTER, TimeFilter.MONSTERS);
    public static final Item FILTER_VILLAGERS = module("filter_villagers", ModuleItem.Kind.FILTER, TimeFilter.VILLAGERS);
    public static final Item FILTER_PLAYERS = module("filter_players", ModuleItem.Kind.FILTER, TimeFilter.PLAYERS);
    public static final Item FILTER_PROJECTILES = module("filter_projectiles", ModuleItem.Kind.FILTER, TimeFilter.PROJECTILES | TimeFilter.OTHER_ENTITIES);
    public static final Item FILTER_ITEMS = module("filter_items", ModuleItem.Kind.FILTER, TimeFilter.ITEMS);
    public static final Item FILTER_VEHICLES = module("filter_vehicles", ModuleItem.Kind.FILTER, TimeFilter.VEHICLES);
    public static final Item FILTER_GROWTH = module("filter_growth", ModuleItem.Kind.FILTER, TimeFilter.GROWTH);
    public static final Item FILTER_ENVIRONMENT = module("filter_environment", ModuleItem.Kind.FILTER, TimeFilter.ENVIRONMENT);
    public static final Item FILTER_FLUIDS = module("filter_fluids", ModuleItem.Kind.FILTER, TimeFilter.FLUIDS);
    public static final Item FILTER_REDSTONE = module("filter_redstone", ModuleItem.Kind.FILTER, TimeFilter.BLOCK_TICKS);
    public static final Item FILTER_MACHINES = module("filter_machines", ModuleItem.Kind.FILTER, TimeFilter.MACHINES);
    public static final Item RADIUS_MODULE = module("radius_module", ModuleItem.Kind.RADIUS, 0);
    public static final Item DURATION_MODULE = module("duration_module", ModuleItem.Kind.DURATION, 0);
    public static final Item STRENGTH_MODULE = module("strength_module", ModuleItem.Kind.STRENGTH, 0);
    public static final Item EFFICIENCY_MODULE = module("efficiency_module", ModuleItem.Kind.EFFICIENCY, 0);

    // Stufe IV: Herr der Zeit
    public static final Item PARADOX_ALTAR = block(ModBlocks.PARADOX_ALTAR);
    public static final Item PARADOX_SPAWN_EGG = register("paradox_spawn_egg", new SpawnEggItem(ModEntities.PARADOX, 0x2A1446, 0x6FE3FF, new Item.Properties()));
    public static final Item HEART_OF_TIME = register("heart_of_time", new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));
    public static final Item CHRONOS_WATCH = register("chronos_watch", new ChronosWatchItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item TIME_LOOP = register("time_loop", new TimeLoopItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item INFINITY_TIME_CELL = register("infinity_time_cell", new ChrononCellItem(1_000_000,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // Stufe V: die Zeitmaschine
    public static final Item CHRONOS_GATE_FRAME = block(ModBlocks.CHRONOS_GATE_FRAME);
    public static final Item CHRONOS_CONSOLE = register("chronos_console", new BlockItem(ModBlocks.CHRONOS_CONSOLE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final Item FOSSIL_FRAGMENT = register("fossil_fragment", new Item(new Item.Properties()));
    public static final Item ERA_FOCUS = register("era_focus", new EraFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
        EnumSet.of(Era.Age.STONE_AGE, Era.Age.ICE_AGE, Era.Age.CENOZOIC)));
    public static final Item AMBER_ORE = block(ModBlocks.AMBER_ORE);
    public static final Item AMBER = register("amber", new Item(new Item.Properties()));
    public static final Item AMBER_BLOCK = block(ModBlocks.AMBER_BLOCK);
    public static final Item INSECT_AMBER = register("insect_amber", new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Item DNA_SAMPLE = register("dna_sample", new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final Item MESOZOIC_FOCUS = register("mesozoic_focus", new EraFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC),
        EnumSet.of(Era.Age.STONE_AGE, Era.Age.ICE_AGE, Era.Age.CENOZOIC, Era.Age.MESOZOIC)));

    // Entwicklung
    public static final Item CHRONO_DEBUG_WAND = register("chrono_debug_wand", new ChronoDebugWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {
    }

    private static Item register(String name, Item item) {
        Item registered = Registry.register(BuiltInRegistries.ITEM, TimeMod.id(name), item);
        TAB_ORDER.add(registered);
        return registered;
    }

    private static Item module(String name, ModuleItem.Kind kind, int mask) {
        return register(name, new ModuleItem(kind, mask, new Item.Properties().rarity(kind == ModuleItem.Kind.BLANK ? Rarity.COMMON : Rarity.UNCOMMON)));
    }

    private static Item block(Block block) {
        return register(BuiltInRegistries.BLOCK.getKey(block).getPath(), new BlockItem(block, new Item.Properties()));
    }

    public static void init() {
        ModTab.init();
    }
}

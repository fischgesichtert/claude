package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.energy.ChrononCellItem;
import com.fischgesichtert.timemod.item.BrakeSpringItem;
import com.fischgesichtert.timemod.item.ChronoDebugWandItem;
import com.fischgesichtert.timemod.item.ChronoKnifeItem;
import com.fischgesichtert.timemod.item.GrowthHourglassItem;
import com.fischgesichtert.timemod.item.MomentLensItem;
import com.fischgesichtert.timemod.item.PocketWatchItem;
import com.fischgesichtert.timemod.item.RecallWatchItem;
import com.fischgesichtert.timemod.item.RewindSpoolItem;
import com.fischgesichtert.timemod.item.SlowPendulumItem;
import com.fischgesichtert.timemod.item.StasisWatchItem;
import com.fischgesichtert.timemod.item.TimeAcceleratorItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public final class ModItems {
    /** Reihenfolge im Kreativ-Reiter. */
    static final List<Item> TAB_ORDER = new ArrayList<>();

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

    // Entwicklung
    public static final Item CHRONO_DEBUG_WAND = register("chrono_debug_wand", new ChronoDebugWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {
    }

    private static Item register(String name, Item item) {
        Item registered = Registry.register(BuiltInRegistries.ITEM, TimeMod.id(name), item);
        TAB_ORDER.add(registered);
        return registered;
    }

    private static Item block(Block block) {
        return register(BuiltInRegistries.BLOCK.getKey(block).getPath(), new BlockItem(block, new Item.Properties()));
    }

    public static void init() {
        ModTab.init();
    }
}

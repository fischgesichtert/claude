package com.fischgesichtert.timemod.time;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.FrogspawnBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.VineBlock;

/**
 * Kategorien, die eine Zeitzone beeinflussen kann (Bitmaske).
 * <p>
 * Die Einteilung ist bewusst grob gehalten, damit sie auch Inhalte anderer Mods erfasst:
 * Mod-Kreaturen landen über ihre Klasse bzw. {@link MobCategory} in der passenden Gruppe,
 * Mod-Maschinen sind Block-Entities.
 */
public final class TimeFilter {
    public static final int ANIMALS = 1;
    public static final int MONSTERS = 1 << 1;
    public static final int VILLAGERS = 1 << 2;
    public static final int PLAYERS = 1 << 3;
    public static final int PROJECTILES = 1 << 4;
    public static final int ITEMS = 1 << 5;
    public static final int VEHICLES = 1 << 6;
    public static final int OTHER_ENTITIES = 1 << 7;
    /** Zufalls-Ticks wachsender Dinge: Pflanzen, Bäume, Gras, Amethyst, Eier. */
    public static final int GROWTH = 1 << 8;
    /** Übrige Zufalls-Ticks und Niederschlag: Eis, Schnee, Laub, Kupfer, Feuer durch Lava. */
    public static final int ENVIRONMENT = 1 << 9;
    /** Geplante Flüssigkeits-Ticks (Wasser- und Lavafluss). */
    public static final int FLUIDS = 1 << 10;
    /** Geplante Block-Ticks und Block-Events (Redstone, Kolben, fallende Blöcke …). */
    public static final int BLOCK_TICKS = 1 << 11;
    /** Block-Entities: Öfen, Trichter, Spawner, Maschinen anderer Mods. */
    public static final int MACHINES = 1 << 12;
    /** Nur global: Tag/Nacht. */
    public static final int DAYTIME = 1 << 13;
    /** Nur global: Wetter. */
    public static final int WEATHER = 1 << 14;
    /** Nur global: Überfälle, Händler, natürliches Spawnen. */
    public static final int WORLD_EVENTS = 1 << 15;

    public static final int ALL_ENTITIES = ANIMALS | MONSTERS | VILLAGERS | PLAYERS | PROJECTILES | ITEMS | VEHICLES | OTHER_ENTITIES;
    public static final int ALL_BLOCKS = GROWTH | ENVIRONMENT | FLUIDS | BLOCK_TICKS | MACHINES;
    public static final int ALL_WORLD = DAYTIME | WEATHER | WORLD_EVENTS;
    /** Alles außer Spielern – Standard für Werkzeuge. */
    public static final int DEFAULT = (ALL_ENTITIES & ~PLAYERS) | ALL_BLOCKS | ALL_WORLD;

    private static final Map<Block, Integer> RANDOM_TICK_CATEGORY = new ConcurrentHashMap<>();

    private TimeFilter() {
    }

    public static int categoryOf(Entity entity) {
        if (entity instanceof Player) {
            return PLAYERS;
        }
        if (entity instanceof Projectile) {
            return PROJECTILES;
        }
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            return ITEMS;
        }
        if (entity instanceof AbstractMinecart || entity instanceof Boat) {
            return VEHICLES;
        }
        if (entity instanceof AbstractVillager) {
            return VILLAGERS;
        }
        if (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER) {
            return MONSTERS;
        }
        if (entity instanceof Mob) {
            return ANIMALS;
        }
        return OTHER_ENTITIES;
    }

    /** Kategorie eines Zufalls-Ticks für diesen Block ({@link #GROWTH} oder {@link #ENVIRONMENT}). */
    public static int randomTickCategory(Block block) {
        return RANDOM_TICK_CATEGORY.computeIfAbsent(block, TimeFilter::classifyRandomTick);
    }

    private static int classifyRandomTick(Block block) {
        boolean growing = block instanceof BushBlock
            || block instanceof BonemealableBlock
            || block instanceof CactusBlock
            || block instanceof SugarCaneBlock
            || block instanceof VineBlock
            || block instanceof GrowingPlantBlock
            || block instanceof ChorusFlowerBlock
            || block instanceof BambooStalkBlock
            || block instanceof CocoaBlock
            || block instanceof SpreadingSnowyDirtBlock
            || block instanceof BuddingAmethystBlock
            || block instanceof TurtleEggBlock
            || block instanceof SnifferEggBlock
            || block instanceof FrogspawnBlock;
        return growing ? GROWTH : ENVIRONMENT;
    }

    public static boolean accepts(int mask, int category) {
        return (mask & category) != 0;
    }
}

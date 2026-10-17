package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.energy.Chronon;
import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Der Chrono-Händler: ein Beruf der Zukunft. Sein Arbeitsplatz ist das Chronometer, das in den Städten
 * der Zukunft steht (und das man mit in die Gegenwart nehmen kann). Er kauft Rost und Messing und
 * verkauft, was man sonst mühsam suchen muss – bis hin zu Zeitsiegeln, Zeitkapseln und Zeitsaat.
 */
public final class ModVillagers {
    public static final ResourceKey<PoiType> CHRONOMETER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, TimeMod.id("chronometer"));
    public static final VillagerProfession CHRONO_TRADER = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, TimeMod.id("chrono_trader"),
        new VillagerProfession("chrono_trader", holder -> holder.is(CHRONOMETER_POI), holder -> holder.is(CHRONOMETER_POI), ImmutableSet.of(),
            ImmutableSet.of(), ModSounds.CLOCK_TICK));

    private ModVillagers() {
    }

    public static void init() {
        PointOfInterestHelper.register(CHRONOMETER_POI.location(), 1, 1, ModBlocks.CHRONOMETER);
        TradeOfferHelper.registerVillagerOffers(CHRONO_TRADER, 1, offers -> {
            offers.add(buy(ModItems.RUSTY_IRON, 4, 1, 16, 2));
            offers.add(buy(ModItems.BRASS_INGOT, 6, 1, 16, 2));
            offers.add(sell(ModItems.TIME_DUST, 3, 2, 12, 1));
        });
        TradeOfferHelper.registerVillagerOffers(CHRONO_TRADER, 2, offers -> {
            offers.add(sell(ModItems.POCKET_WATCH, 1, 6, 8, 5));
            offers.add(sell(ModItems.CHRONITE_SEED, 1, 8, 8, 5));
            offers.add(buy(ModItems.AMBER, 4, 1, 12, 10));
        });
        TradeOfferHelper.registerVillagerOffers(CHRONO_TRADER, 3, offers -> {
            offers.add(sell(ModItems.TIME_SEAL, 1, 14, 4, 10));
            offers.add(sell(ModItems.TIME_GLASS, 4, 3, 12, 10));
            offers.add(sell(ModItems.BRASS_GEAR, 4, 2, 12, 10));
        });
        TradeOfferHelper.registerVillagerOffers(CHRONO_TRADER, 4, offers -> {
            offers.add((trader, random) -> {
                ItemStack cell = new ItemStack(ModItems.POCKET_TIME_CELL);
                Chronon.set(cell, 5_000);
                return new MerchantOffer(new ItemStack(Items.EMERALD, 18), cell, 3, 15, 0.05F);
            });
            offers.add(sell(ModItems.FOSSIL_FRAGMENT, 1, 10, 4, 15));
        });
        TradeOfferHelper.registerVillagerOffers(CHRONO_TRADER, 5, offers -> {
            offers.add(sell(ModItems.TIME_CAPSULE, 1, 20, 2, 30));
            offers.add(sell(ModItems.TIME_SEED, 1, 24, 2, 30));
        });
    }

    private static VillagerTrades.ItemListing sell(Item item, int count, int emeralds, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item, count), uses, xp, 0.05F);
    }

    private static VillagerTrades.ItemListing buy(Item item, int count, int emeralds, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD, emeralds), uses, xp, 0.05F);
    }
}

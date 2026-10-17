package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.block.entity.LifetimeExtractorBlockEntity;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.item.StasisWatchItem;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Chrono-Rüstung (JoJo-Regel, Ersparnis, Schmiedetisch) und Lebenszeit-Extraktor. */
public class ArmorAndExtractorTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static void dress(ServerPlayer player, int pieces) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        ItemStack[] armor = {new ItemStack(ModItems.CHRONO_HELMET), new ItemStack(ModItems.CHRONO_CHESTPLATE),
            new ItemStack(ModItems.CHRONO_LEGGINGS), new ItemStack(ModItems.CHRONO_BOOTS)};
        for (int i = 0; i < pieces; i++) {
            player.setItemSlot(slots[i], armor[i]);
        }
    }

    /** Nach JoJo-Regel: Mit vollem Set bewegt man sich in der angehaltenen Zeit anderer, mit drei Teilen nicht. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void fullSetMovesInForeignStoppedTime(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer jotaro = helper.makeMockServerPlayerInLevel();
        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        for (ServerPlayer p : new ServerPlayer[] {owner, jotaro, bystander}) {
            p.teleportTo(at.x, at.y, at.z);
        }
        dress(jotaro, 4);
        dress(bystander, 3);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        TimeZone zone = engine.addZone(new ZoneShape.Sphere(at, 8), 0.0, TimeFilter.DEFAULT | TimeFilter.PLAYERS, owner.getUUID(), 40);
        if (engine.isFrozenPlayer(jotaro)) {
            helper.fail("Wer die volle Chrono-Rüstung trägt, darf im fremden Zeitstopp nicht einfrieren");
        }
        if (!engine.isFrozenPlayer(bystander)) {
            helper.fail("Mit drei Teilen friert man ein");
        }
        engine.removeZone(zone.id());
        owner.discard();
        jotaro.discard();
        bystander.discard();
        helper.succeed();
    }

    /** Vier Teile sparen 20 % Chronon. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void armorSavesChronon(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        dress(player, 4);
        ItemStack watch = new ItemStack(ModItems.STASIS_WATCH);
        Chronon.set(watch, 4000);
        player.setItemInHand(InteractionHand.MAIN_HAND, watch);
        watch.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        int paid = 4000 - Chronon.stored(watch);
        int expected = (int) Math.ceil(StasisWatchItem.COST * 0.8);
        if (paid != expected) {
            helper.fail("Mit 4 Teilen kostet die Stillstand-Uhr " + expected + " statt " + StasisWatchItem.COST + ", bezahlt: " + paid);
        }
        ServerTimeEngine.of(helper.getLevel()).removeZonesOwnedBy(player.getUUID());
        player.discard();
        helper.succeed();
    }

    /** Netherit-Helm + Vorlage + Platte am Schmiedetisch ergibt den Chrono-Helm – Verzauberungen bleiben. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void smithingUpgradesNetherite(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        SimpleContainer table = new SimpleContainer(new ItemStack(ModItems.CHRONO_UPGRADE_TEMPLATE), helmet, new ItemStack(ModItems.CHRONITE_PLATE));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, table, helper.getLevel());
        if (recipe.isEmpty()) {
            helper.fail("Kein Schmiederezept für den Chrono-Helm gefunden");
        }
        ItemStack result = recipe.get().assemble(table, helper.getLevel().registryAccess());
        if (!result.is(ModItems.CHRONO_HELMET)) {
            helper.fail("Ergebnis muss der Chrono-Helm sein, ist " + result);
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, result) != 4) {
            helper.fail("Die Schutz-Verzauberung muss beim Aufwerten erhalten bleiben");
        }
        helper.succeed();
    }

    /** Der Extraktor lässt Hühner an Altersschwäche sterben und sammelt ihre Lebenszeit; Redstone hält ihn an. */
    @GameTest(template = ARENA, timeoutTicks = 160, setupTicks = 20)
    public void extractorDrainsLifetime(GameTestHelper helper) {
        BlockPos working = new BlockPos(8, 2, 8);
        BlockPos stopped = new BlockPos(24, 2, 8);
        helper.setBlock(working, ModBlocks.LIFETIME_EXTRACTOR);
        helper.setBlock(stopped, ModBlocks.LIFETIME_EXTRACTOR);
        helper.setBlock(stopped.east(), Blocks.REDSTONE_BLOCK);
        Chicken victim = helper.spawn(EntityType.CHICKEN, new Vec3(9.5, 2, 8.5));
        Chicken spared = helper.spawn(EntityType.CHICKEN, new Vec3(23.5, 2, 8.5));
        victim.setNoAi(true);
        spared.setNoAi(true);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 near = Vec3.atBottomCenterOf(helper.absolutePos(working.west()));
        player.teleportTo(near.x, near.y, near.z);
        float playerHealth = player.getHealth();

        helper.runAtTickTime(45, () -> {
            if (victim.getHealth() >= victim.getMaxHealth()) {
                helper.fail("Das Huhn am Extraktor muss altern (Leben " + victim.getHealth() + ")");
            }
        });
        helper.runAtTickTime(130, () -> {
            LifetimeExtractorBlockEntity extractor = (LifetimeExtractorBlockEntity) helper.getBlockEntity(working);
            if (victim.isAlive()) {
                helper.fail("Nach über 4 Sekunden muss das Huhn (4 Leben) an Altersschwäche gestorben sein");
            }
            int expected = Math.round(4 * LifetimeExtractorBlockEntity.CHRONON_PER_HEALTH);
            if (extractor.buffer() != expected) {
                helper.fail("4 Lebenspunkte müssen " + expected + " Chronon ergeben, im Puffer: " + extractor.buffer());
            }
            if (spared.getHealth() < spared.getMaxHealth()) {
                helper.fail("Ein Extraktor mit Redstone-Signal darf nichts entziehen");
            }
            if (player.getHealth() != playerHealth) {
                helper.fail("Spieler dürfen nicht altern");
            }
            spared.discard();
            player.discard();
            helper.succeed();
        });
    }
}

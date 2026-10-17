package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.entity.MossBear;
import com.fischgesichtert.timemod.entity.RuinGuardian;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.town.RuinWatch;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModVillagers;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Wesen der Zukunft: Ruinenwächter (Schlaf, Alarm, Zerfall), Moosbär (Schur, Futter, Spawnregel), Chrono-Händler (Angebote). */
public class CreatureTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static ServerPlayer survivor(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    /** Ein Wächter ruht, bis jemand die Ruine antastet – dann erwacht er und nimmt den Eindringling ins Visier. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5, batch = "timemod_creatures")
    public void guardianSleepsUntilTheRuinIsDisturbed(GameTestHelper helper) {
        RuinGuardian guardian = helper.spawn(ModEntities.RUIN_GUARDIAN, new Vec3(8.5, 2, 8.5));
        ServerPlayer thief = survivor(helper, new BlockPos(24, 2, 8));
        helper.runAfterDelay(30, () -> {
            if (guardian.isAwake()) {
                helper.fail("Ohne Besucher ruht der Wächter");
            }
            if (guardian.post() == null) {
                helper.fail("Der Wächter merkt sich seinen Posten");
            }
            int alarmed = RuinWatch.alarm(helper.getLevel(), thief, helper.absolutePos(new BlockPos(20, 2, 8)));
            if (alarmed != 1 || !guardian.isAwake() || guardian.getTarget() != thief || !guardian.isAngryAt(thief)) {
                helper.fail("Wer die Ruine antastet, weckt den Wächter und wird sein Ziel (alarmiert: " + alarmed + ")");
            }
            guardian.discard();
            thief.discard();
            helper.succeed();
        });
    }

    /** In beschleunigter Zeit holen die Jahrhunderte den Wächter ein; in normaler Zeit bleibt er heil. */
    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 5, batch = "timemod_creatures")
    public void guardiansCrumbleInFastTime(GameTestHelper helper) {
        RuinGuardian fast = helper.spawn(ModEntities.RUIN_GUARDIAN, new Vec3(6.5, 2, 8.5));
        RuinGuardian calm = helper.spawn(ModEntities.RUIN_GUARDIAN, new Vec3(26.5, 2, 8.5));
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        TimeZone zone = engine.addZone(new ZoneShape.Sphere(fast.position(), 4), 4.0, TimeFilter.DEFAULT, null, 200);
        helper.runAfterDelay(60, () -> {
            if (fast.getHealth() >= fast.getMaxHealth()) {
                helper.fail("Im 4×-Raffer muss der Wächter zerfallen, Leben: " + fast.getHealth());
            }
            if (calm.getHealth() < calm.getMaxHealth()) {
                helper.fail("In normaler Zeit bleibt der Wächter heil, Leben: " + calm.getHealth());
            }
            engine.removeZone(zone.id());
            fast.discard();
            calm.discard();
            helper.succeed();
        });
    }

    /** Die Schere erntet Moos (es wächst nach), Beeren sind Futter, Junge bleiben ungeschoren. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_creatures")
    public void mossBearCanBeShearedAndFed(GameTestHelper helper) {
        MossBear bear = helper.spawn(ModEntities.MOSS_BEAR, new Vec3(12.5, 2, 8.5));
        bear.setNoAi(true);
        bear.setAge(0);
        MossBear cub = helper.spawn(ModEntities.MOSS_BEAR, new Vec3(18.5, 2, 8.5));
        cub.setNoAi(true);
        cub.setAge(-24000);
        ServerPlayer player = survivor(helper, new BlockPos(10, 2, 8));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        bear.mobInteract(player, InteractionHand.MAIN_HAND);
        if (!bear.isSheared() || bear.readyForShearing()) {
            helper.fail("Nach der Schur ist der Bär geschoren");
        }
        if (cub.readyForShearing()) {
            helper.fail("Junge Moosbären haben noch nichts zum Scheren");
        }
        if (!bear.isFood(new ItemStack(Items.SWEET_BERRIES)) || !bear.isFood(new ItemStack(Items.GLOW_BERRIES)) || bear.isFood(new ItemStack(Items.SALMON))) {
            helper.fail("Moosbären fressen Beeren");
        }
        helper.runAfterDelay(5, () -> {
            long moss = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(bear.blockPosition()).inflate(4),
                item -> item.getItem().is(Items.MOSS_BLOCK)).stream().mapToInt(item -> item.getItem().getCount()).sum();
            if (moss < 1) {
                helper.fail("Die Schur bringt Moos");
            }
            bear.setSheared(false);
            if (!bear.readyForShearing()) {
                helper.fail("Nachgewachsen lässt er sich wieder scheren");
            }
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(bear.blockPosition()).inflate(6)).forEach(ItemEntity::discard);
            bear.discard();
            cub.discard();
            player.discard();
            helper.succeed();
        });
    }

    /** Moosbären leben nur in der Zukunft (ab +100 Jahren), nicht in der Gegenwart. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void mossBearsBelongToTheFuture(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        BlockPos here = helper.absolutePos(new BlockPos(1, 2, 1));
        present.setBlockAndUpdate(here.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        RandomSource random = RandomSource.create(1);
        if (MossBear.checkMossBearSpawnRules(ModEntities.MOSS_BEAR, present, MobSpawnType.NATURAL, here, random)) {
            helper.fail("In der Gegenwart spawnen keine Moosbären");
        }
        ServerLevel future = present.getServer().getLevel(Eras.byYears(250).level());
        ServerLevel near = present.getServer().getLevel(Eras.byYears(50).level());
        if (future == null || near == null) {
            helper.fail("Die Zukunfts-Epochen fehlen");
            return;
        }
        // Hoch über dem Gelände, damit sicher Himmelslicht hinfällt
        BlockPos spot = new BlockPos(1000, 300, 1000);
        for (ServerLevel level : new ServerLevel[] {future, near}) {
            level.getChunk(spot);
            level.setBlockAndUpdate(spot.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        }
        helper.runAfterDelay(20, () -> {
            if (!MossBear.checkMossBearSpawnRules(ModEntities.MOSS_BEAR, future, MobSpawnType.NATURAL, spot, random)) {
                helper.fail("Bei +250 Jahren spawnen Moosbären auf Gras (Licht " + future.getRawBrightness(spot, 0) + ")");
            }
            if (MossBear.checkMossBearSpawnRules(ModEntities.MOSS_BEAR, near, MobSpawnType.NATURAL, spot, random)) {
                helper.fail("Bei +50 Jahren gibt es noch keine Moosbären");
            }
            helper.succeed();
        });
    }

    /** Der Chrono-Händler handelt mit der Zeit: als Meister mit Zeitkapseln und Zeitsaat, als Geselle mit Zeitsiegeln. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void chronoTraderSellsTime(GameTestHelper helper) {
        Set<Item> journeyman = Set.of(ModItems.TIME_SEAL, ModItems.TIME_GLASS, ModItems.BRASS_GEAR);
        Set<Item> master = Set.of(ModItems.TIME_CAPSULE, ModItems.TIME_SEED);
        for (int level : new int[] {3, 5}) {
            Villager villager = EntityType.VILLAGER.create(helper.getLevel());
            villager.setVillagerData(villager.getVillagerData().setType(VillagerType.PLAINS).setProfession(ModVillagers.CHRONO_TRADER).setLevel(level));
            var offers = villager.getOffers();
            if (offers.isEmpty()) {
                helper.fail("Ein Chrono-Händler der Stufe " + level + " hat Angebote");
            }
            for (MerchantOffer offer : offers) {
                Item item = offer.getResult().getItem();
                if (!(level == 3 ? journeyman : master).contains(item) || !offer.getBaseCostA().is(Items.EMERALD)) {
                    helper.fail("Unerwartetes Angebot auf Stufe " + level + ": " + offer.getResult());
                }
            }
            villager.discard();
        }
        helper.succeed();
    }
}

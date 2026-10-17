package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.block.BossAltars;
import com.fischgesichtert.timemod.block.entity.TimeFieldProjectorBlockEntity;
import com.fischgesichtert.timemod.entity.RuinGuardian;
import com.fischgesichtert.timemod.entity.boss.CityColossus;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Der Wächter der letzten Stadt: Erwecken an der Glocke, Beständigkeit und Zerfall, Glockenschlag, Phasen, sein Herz. */
public class LastCityTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static ServerPlayer survivor(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static void use(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void discardAll(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64);
        helper.getLevel().getEntitiesOfClass(CityColossus.class, area).forEach(CityColossus::discard);
        helper.getLevel().getEntitiesOfClass(RuinGuardian.class, area).forEach(RuinGuardian::discard);
    }

    /** Zwei Wächterkerne reichen nicht; drei lassen die Glocke klingen, und der Koloss steigt aus dem Boden. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_last_city")
    public void bellAwakensTheColossus(GameTestHelper helper) {
        BlockPos bell = new BlockPos(16, 2, 8);
        helper.setBlock(bell, ModBlocks.CITY_BELL);
        ServerPlayer player = survivor(helper, new BlockPos(16, 2, 4));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUARDIAN_CORE, BossAltars.GUARDIAN_CORES - 1));
        use(helper, player, bell);
        AABB area = new AABB(helper.absolutePos(bell)).inflate(20);
        if (!helper.getLevel().getEntitiesOfClass(CityColossus.class, area).isEmpty()) {
            helper.fail("Mit zu wenigen Wächterkernen bleibt die Glocke stumm");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUARDIAN_CORE, BossAltars.GUARDIAN_CORES + 1));
        use(helper, player, bell);
        var colossi = helper.getLevel().getEntitiesOfClass(CityColossus.class, area);
        if (colossi.size() != 1 || player.getMainHandItem().getCount() != 1) {
            helper.fail("Drei Wächterkerne wecken genau einen Koloss (und werden verbraucht): " + colossi.size());
            return;
        }
        CityColossus colossus = colossi.get(0);
        if (colossus.emergeTicks() <= 0 || colossus.distanceTo(player) < 6.0) {
            helper.fail("Der Koloss steigt jenseits der Glocke aus dem Boden");
        }
        float before = colossus.getHealth();
        colossus.hurt(helper.getLevel().damageSources().playerAttack(player), 50.0F);
        if (colossus.getHealth() != before) {
            helper.fail("Während er aufsteigt, ist er unverwundbar");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUARDIAN_CORE, BossAltars.GUARDIAN_CORES));
        use(helper, player, bell);
        if (helper.getLevel().getEntitiesOfClass(CityColossus.class, area).size() != 1) {
            helper.fail("Solange er lebt, ruht die Glocke");
        }
        discardAll(helper);
        player.discard();
        helper.succeed();
    }

    /** Er hält stand (60 % Schaden) und heilt sich – in beschleunigter Zeit nimmt er 150 % und zerfällt. */
    @GameTest(template = ARENA, timeoutTicks = 140, setupTicks = 5, batch = "timemod_last_city")
    public void onlyAcceleratedTimeWearsHimDown(GameTestHelper helper) {
        CityColossus calm = helper.spawn(ModEntities.CITY_COLOSSUS, new Vec3(6.5, 2, 8.5));
        CityColossus aged = helper.spawn(ModEntities.CITY_COLOSSUS, new Vec3(26.5, 2, 8.5));
        calm.setNoAi(true);
        aged.setNoAi(true);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        TimeZone zone = engine.addZone(new ZoneShape.Sphere(aged.position().add(0, 2, 0), 5), 4.0, TimeFilter.DEFAULT, null, 400);
        helper.runAfterDelay(3, () -> {
            float max = calm.getMaxHealth();
            calm.hurt(helper.getLevel().damageSources().generic(), 100.0F);
            aged.hurt(helper.getLevel().damageSources().generic(), 100.0F);
            if (Math.abs(calm.getHealth() - (max - 100.0F * CityColossus.ENDURING_FACTOR)) > 0.01F) {
                helper.fail("In normaler Zeit kommen nur 60 % an: " + calm.getHealth());
            }
            if (Math.abs(aged.getHealth() - (max - 100.0F * CityColossus.FAST_FACTOR)) > 2.5F) {
                helper.fail("In beschleunigter Zeit kommen 150 % an: " + aged.getHealth());
            }
        });
        helper.runAfterDelay(83, () -> {
            float max = calm.getMaxHealth();
            if (calm.getHealth() <= max - 100.0F * CityColossus.ENDURING_FACTOR) {
                helper.fail("Außerhalb beschleunigter Zeit heilt er sich: " + calm.getHealth());
            }
            if (aged.getHealth() >= max - 100.0F * CityColossus.FAST_FACTOR) {
                helper.fail("In beschleunigter Zeit zerfällt er weiter: " + aged.getHealth());
            }
            engine.removeZone(zone.id());
            discardAll(helper);
            helper.succeed();
        });
    }

    /** Der Glockenschlag löst Zeitzonen in der Nähe auf (ferne bleiben) und lässt Getroffene taumeln. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_last_city")
    public void bellSetsTimeRight(GameTestHelper helper) {
        CityColossus colossus = helper.spawn(ModEntities.CITY_COLOSSUS, new Vec3(10.5, 2, 8.5));
        colossus.setNoAi(true);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(14.5, 2, 8.5));
        zombie.setNoAi(true);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        TimeZone near = engine.addZone(new ZoneShape.Sphere(colossus.position(), 4), 3.0, TimeFilter.DEFAULT, null, 400);
        TimeZone far = engine.addZone(new ZoneShape.Sphere(colossus.position().add(40, 0, 0), 3), 3.0, TimeFilter.DEFAULT, null, 400);
        helper.runAfterDelay(2, () -> {
            colossus.ringNow();
            helper.runAfterDelay(2, () -> {
                if (engine.zone(near.id()) != null) {
                    helper.fail("Die Glocke stellt die Zeit in der Nähe richtig");
                }
                if (engine.zone(far.id()) == null) {
                    helper.fail("Ferne Zeitzonen bleiben");
                }
                if (!zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || zombie.getHealth() >= zombie.getMaxHealth()) {
                    helper.fail("Wer in der Nähe steht, wird getroffen und taumelt");
                }
                engine.removeZone(far.id());
                zombie.discard();
                discardAll(helper);
                helper.succeed();
            });
        });
    }

    /** Bei zwei Dritteln und einem Drittel ruft er je zwei Ruinenwächter; unter der Hälfte fallen seine Platten. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_last_city")
    public void phasesCallGuardiansAndShedPlates(GameTestHelper helper) {
        CityColossus colossus = helper.spawn(ModEntities.CITY_COLOSSUS, new Vec3(16.5, 2, 8.5));
        colossus.setNoAi(true);
        AABB area = new AABB(colossus.blockPosition()).inflate(16);
        colossus.setHealth(colossus.getMaxHealth() * 0.6F);
        helper.runAfterDelay(2, () -> {
            int guardians = helper.getLevel().getEntitiesOfClass(RuinGuardian.class, area).size();
            if (guardians != 2 || colossus.isBare()) {
                helper.fail("Bei 60 % Leben: zwei Wächter, noch mit Platten – " + guardians + " Wächter, ohne Platten " + colossus.isBare()
                    + " (entfernt " + colossus.isRemoved() + ", Ticks " + colossus.tickCount + ", Leben " + colossus.getHealth() + ")");
            }
            colossus.setHealth(colossus.getMaxHealth() * 0.3F);
        });
        helper.runAfterDelay(5, () -> {
            int guardians = helper.getLevel().getEntitiesOfClass(RuinGuardian.class, area).size();
            if (guardians != 4 || !colossus.isBare()) {
                helper.fail("Bei 30 % Leben: vier Wächter, ohne Platten – " + guardians + ", ohne Platten " + colossus.isBare());
            }
            discardAll(helper);
            helper.succeed();
        });
    }

    /** Das Herz der letzten Stadt macht den Projektor zur Stadtuhr: größere Reichweite, ein Viertel des Verbrauchs. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void heartTurnsProjectorIntoCityClock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.TIME_FIELD_PROJECTOR);
        TimeFieldProjectorBlockEntity projector = (TimeFieldProjectorBlockEntity) helper.getBlockEntity(pos);
        double before = projector.costPerTick();
        ServerPlayer player = survivor(helper, new BlockPos(1, 2, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HEART_OF_THE_LAST_CITY));
        projector.interact(player, InteractionHand.MAIN_HAND);
        if (!projector.hasCityHeart() || !player.getMainHandItem().isEmpty()) {
            helper.fail("Das Herz wird eingesetzt (und verbraucht)");
        }
        if (Math.abs(projector.costPerTick() - before * TimeFieldProjectorBlockEntity.CITY_COST) > 1e-9) {
            helper.fail("Mit dem Herzen kostet der Projektor ein Viertel: " + projector.costPerTick() + " statt " + before);
        }
        int max = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        for (int i = 0; i < TimeFieldProjectorBlockEntity.CITY_RADII.length; i++) {
            projector.interact(player, InteractionHand.MAIN_HAND);
            max = Math.max(max, projector.radius());
        }
        if (max != 24) {
            helper.fail("Als Stadtuhr reicht der Projektor bis 24 Blöcke, höchstens: " + max);
        }
        player.discard();
        helper.succeed();
    }
}

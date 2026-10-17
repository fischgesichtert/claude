package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;

/** Zeitstopp: Schaden wird gesammelt und auf einmal ausgeteilt; fremde Spieler sind eingefroren. */
public class TimeStopTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 20)
    public void damageArrivesAllAtOnceAfterStop(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(10.5, 2, 8.5));
        pig.setNoAi(true);
        float startHealth = pig.getHealth();
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(10, 2, 8)));
        ServerTimeEngine.of(helper.getLevel()).addZone(new ZoneShape.Sphere(center, 4), 0.0, TimeFilter.DEFAULT, null, 20);
        helper.runAtTickTime(2, () -> pig.hurt(helper.getLevel().damageSources().generic(), 3.0F));
        helper.runAtTickTime(3, () -> pig.hurt(helper.getLevel().damageSources().generic(), 3.0F));
        helper.runAtTickTime(4, () -> pig.hurt(helper.getLevel().damageSources().generic(), 2.0F));
        helper.runAtTickTime(15, () -> {
            if (pig.getHealth() != startHealth) {
                helper.fail("Im Zeitstopp darf noch kein Schaden ankommen (Leben " + pig.getHealth() + " statt " + startHealth + ")");
            }
        });
        helper.runAtTickTime(25, () -> {
            float expected = startHealth - 8.0F;
            if (Math.abs(pig.getHealth() - expected) > 0.001F) {
                helper.fail("Nach dem Stopp sollten alle drei Treffer (8 Schaden) gleichzeitig wirken: Leben " + pig.getHealth() + ", erwartet " + expected);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40, setupTicks = 5)
    public void otherPlayersFreezeButNotTheOwner(GameTestHelper helper) {
        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 2, 8)));
        other.teleportTo(pos.x, pos.y, pos.z);
        owner.teleportTo(pos.x + 1, pos.y, pos.z);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        engine.addZone(new ZoneShape.Sphere(pos, 6), 0.0, TimeFilter.DEFAULT | TimeFilter.PLAYERS, owner.getUUID(), 10);
        if (!engine.isFrozenPlayer(other)) {
            helper.fail("Ein fremder Spieler in der Stopp-Zone muss eingefroren sein");
        }
        if (engine.isFrozenPlayer(owner)) {
            helper.fail("Wer die Zeit anhält, darf sich weiter bewegen");
        }
        helper.runAtTickTime(15, () -> {
            if (engine.isFrozenPlayer(other)) {
                helper.fail("Nach Ablauf der Zone darf niemand mehr eingefroren sein");
            }
            other.discard();
            owner.discard();
            helper.succeed();
        });
    }
}

package com.fischgesichtert.timemod.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class SmokeTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void modLoads(GameTestHelper helper) {
        helper.succeed();
    }
}

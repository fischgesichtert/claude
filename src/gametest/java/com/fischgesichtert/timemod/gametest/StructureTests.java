package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Alle vier Strukturen lassen sich erzeugen und enthalten ihr Herzstück und Beute-Truhen. */
public class StructureTests implements FabricGameTest {
    private record Expectation(String name, Block marker, String loot, int maxY) {
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void structuresGenerate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        Expectation[] expected = {
            new Expectation("clock_tower", Blocks.BELL, "timemod:chests/clock_tower_top", 40),
            new Expectation("observatory", Blocks.LECTERN, "timemod:chests/observatory", 40),
            new Expectation("chrono_ruins", ModBlocks.PHAGE_ALTAR, "timemod:chests/chrono_ruins", 40),
            new Expectation("timeless_workshop", ModBlocks.CLOCKMAKER_ALTAR, "timemod:chests/timeless_workshop", 40),
            new Expectation("timeless_citadel", ModBlocks.PARADOX_ALTAR, "timemod:chests/timeless_citadel", 160),
        };
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(400, 0, 400);
        for (int i = 0; i < expected.length; i++) {
            Expectation e = expected[i];
            BlockPos at = base.offset(i * 96, 0, 0);
            // /place verlangt geladene Chunks rund um die Struktur
            for (int cx = (at.getX() >> 4) - 3; cx <= (at.getX() >> 4) + 3; cx++) {
                for (int cz = (at.getZ() >> 4) - 3; cz <= (at.getZ() >> 4) + 3; cz++) {
                    level.getChunk(cx, cz);
                }
            }
            int result = server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                "place structure timemod:" + e.name() + " " + at.getX() + " " + at.getY() + " " + at.getZ());
            if (result <= 0) {
                helper.fail("Struktur " + e.name() + " ließ sich nicht platzieren");
            }
            boolean marker = false;
            boolean chest = false;
            int minX = (at.getX() >> 4 << 4) - 8;
            int minZ = (at.getZ() >> 4 << 4) - 8;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = minX; x < minX + 48; x++) {
                for (int z = minZ; z < minZ + 48; z++) {
                    for (int y = level.getMinBuildHeight(); y < e.maxY(); y++) {
                        pos.set(x, y, z);
                        if (level.getBlockState(pos).is(e.marker())) {
                            marker = true;
                        }
                        if (level.getBlockEntity(pos) instanceof ChestBlockEntity box
                            && e.loot().equals(box.saveWithoutMetadata().getString("LootTable"))) {
                            chest = true;
                        }
                    }
                }
            }
            if (!marker) {
                helper.fail(e.name() + ": Herzstück " + e.marker() + " fehlt");
            }
            if (!chest) {
                helper.fail(e.name() + ": keine Truhe mit " + e.loot());
            }
        }
        helper.succeed();
    }
}

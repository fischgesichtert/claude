package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.block.ChronosPortalBlock;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.gate.ChronosGate;
import com.fischgesichtert.timemod.gate.TimeTravel;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Das Chronos-Tor: Ring erkennen, aufladen, öffnen, zusammenbrechen, Kosten, Fokus und die Reise selbst. */
public class GateTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";
    /** Linke untere Ecke des Rings im Arena-Feld (Achse X, steht bei z = 8). */
    private static final BlockPos RING = new BlockPos(10, 1, 8);
    private static final BlockPos CONSOLE = new BlockPos(13, 1, 4);

    /** Baut den 7×7-Ring aus Tor-Rahmen (relativ zur Test-Struktur). */
    private static ChronosGate.Shape buildRing(GameTestHelper helper, BlockPos corner, Direction.Axis axis) {
        ChronosGate.Shape shape = new ChronosGate.Shape(helper.absolutePos(corner), axis);
        for (BlockPos pos : shape.frame()) {
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CHRONOS_GATE_FRAME.defaultBlockState());
        }
        return shape;
    }

    private static ChronosConsoleBlockEntity placeConsole(GameTestHelper helper) {
        helper.setBlock(CONSOLE, ModBlocks.CHRONOS_CONSOLE);
        return (ChronosConsoleBlockEntity) helper.getBlockEntity(CONSOLE);
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5, batch = "timemod_gate_find")
    public void ringIsFoundAndLost(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChronosGate.Shape ring = buildRing(helper, RING, Direction.Axis.X);
        ChronosGate.Shape found = ChronosGate.find(level, helper.absolutePos(CONSOLE), ChronosConsoleBlockEntity.SEARCH_RADIUS);
        if (found == null || !found.equals(ring)) {
            helper.fail("Ring entlang X nicht erkannt: " + found);
        }
        if (ring.frame().size() != 24 || ring.opening().size() != 21) {
            helper.fail("Ring muss 24 Rahmen und 21 Öffnungsfelder haben");
        }
        // Derselbe Ring entlang Z, daneben aufgebaut
        ChronosGate.Shape crosswise = buildRing(helper, new BlockPos(28, 1, 2), Direction.Axis.Z);
        ChronosGate.Shape foundZ = ChronosGate.find(level, helper.absolutePos(new BlockPos(25, 1, 5)), 4);
        if (foundZ == null || foundZ.axis() != Direction.Axis.Z || !foundZ.equals(crosswise)) {
            helper.fail("Ring entlang Z nicht erkannt: " + foundZ);
        }
        ChronosConsoleBlockEntity console = placeConsole(helper);
        helper.runAtTickTime(30, () -> {
            if (console.gateState() != ChronosConsoleBlockEntity.GateState.READY || !ring.equals(console.gate())) {
                helper.fail("Konsole findet den Ring nicht: " + console.gateState());
            }
            // Ein Rahmenblock fehlt: Das Tor ist keines mehr
            level.setBlockAndUpdate(ring.at(6, 3), Blocks.AIR.defaultBlockState());
        });
        helper.runAtTickTime(60, () -> {
            if (console.gateState() != ChronosConsoleBlockEntity.GateState.MISSING) {
                helper.fail("Ohne vollständigen Ring darf das Tor nicht bereit sein: " + console.gateState());
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 320, setupTicks = 5, batch = "timemod_gate_charge")
    public void chargingOpensThePortal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChronosGate.Shape ring = buildRing(helper, RING, Direction.Axis.X);
        ChronosConsoleBlockEntity console = placeConsole(helper);
        ServerPlayer player = player(helper, new BlockPos(13, 1, 2));
        console.addChronon(200_000);
        helper.runAtTickTime(10, () -> {
            Era target = console.target();
            if (target.years() != -1) {
                helper.fail("Aus der Gegenwart zeigt die Konsole zuerst auf −1 Jahr, nicht " + target.years());
            }
            if (console.charge(player) != null || console.phase() != ChronosConsoleBlockEntity.Phase.CHARGING) {
                helper.fail("Das Tor lädt nicht: " + console.charge(player));
            }
            if (console.buffer() != 200_000 - 14_500) {
                helper.fail("Eine Reise um ein Jahr kostet 14.500 Chronon, abgezogen wurden " + (200_000 - console.buffer()));
            }
        });
        helper.runAtTickTime(10 + ChronosConsoleBlockEntity.CHARGE_TICKS + 5, () -> {
            if (console.phase() != ChronosConsoleBlockEntity.Phase.OPEN) {
                helper.fail("Nach zehn Sekunden muss das Tor offen sein: " + console.phase());
            }
            for (BlockPos pos : ring.opening()) {
                BlockState state = level.getBlockState(pos);
                if (!state.is(ModBlocks.CHRONOS_PORTAL) || ChronosPortalBlock.era(state).years() != -1
                    || state.getValue(ChronosPortalBlock.AXIS) != Direction.Axis.X) {
                    helper.fail("Öffnungsfeld " + pos + " ist " + state);
                }
            }
            // Ein Rahmen bricht: Die ganze Öffnung fällt in sich zusammen
            level.destroyBlock(ring.at(0, 3), false);
        });
        helper.runAtTickTime(10 + ChronosConsoleBlockEntity.CHARGE_TICKS + 30, () -> {
            for (BlockPos pos : ring.opening()) {
                if (level.getBlockState(pos).is(ModBlocks.CHRONOS_PORTAL)) {
                    helper.fail("Portal bleibt ohne Rahmen bestehen bei " + pos);
                }
            }
            if (console.phase() != ChronosConsoleBlockEntity.Phase.IDLE) {
                helper.fail("Konsole muss nach dem Zusammenbruch wieder bereit sein: " + console.phase());
            }
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_gate_focus")
    public void focusAndChrononGuardTheGate(GameTestHelper helper) {
        buildRing(helper, RING, Direction.Axis.X);
        ChronosConsoleBlockEntity console = placeConsole(helper);
        ServerPlayer player = player(helper, new BlockPos(13, 1, 2));
        if (ChronosConsoleBlockEntity.cost(Eras.PRESENT, Eras.byYears(1)) != 14_500
            || ChronosConsoleBlockEntity.cost(Eras.PRESENT, Eras.byYears(-245_000_000)) != 136_000
            || ChronosConsoleBlockEntity.cost(Eras.byYears(-70_000_000), Eras.byYears(-66_000_000)) >= ChronosConsoleBlockEntity.cost(Eras.PRESENT,
            Eras.byYears(-66_000_000))) {
            helper.fail("Kosten folgen nicht dem Abstand der Jahre");
        }
        helper.runAtTickTime(25, () -> {
            // ◀◀ springt aus der jüngsten Vergangenheit in die Jungsteinzeit
            console.select(0);
            if (console.target().years() != -5_000) {
                helper.fail("Schnellsprung landet bei " + console.target().years());
            }
            if (console.charge(player) == null) {
                helper.fail("Ohne Epochen-Fokus darf die Steinzeit nicht erreichbar sein");
            }
            ItemStack focus = new ItemStack(ModItems.ERA_FOCUS);
            console.items().setItem(ChronosConsoleBlockEntity.SLOT_FOCUS, focus);
            if (console.charge(player) == null || console.phase() != ChronosConsoleBlockEntity.Phase.IDLE) {
                helper.fail("Ohne Chronon darf das Tor nicht laden");
            }
            // Bis ins Mesozoikum reicht der Epochen-Fokus nicht
            console.select(0);
            console.select(0);
            console.select(0);
            console.addChronon(400_000);
            if (console.target().age() != Era.Age.MESOZOIC || console.charge(player) == null) {
                helper.fail("Das Mesozoikum braucht den Fokus: Mesozoikum (Ziel " + console.target().years() + ")");
            }
            console.items().setItem(ChronosConsoleBlockEntity.SLOT_FOCUS, new ItemStack(ModItems.MESOZOIC_FOCUS));
            if (console.charge(player) != null) {
                helper.fail("Mit Fokus und Chronon muss das Tor laden");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Zurückspulen erstattet keine Reise: Das ausgegebene Chronon der Konsole bleibt ausgegeben. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5, batch = "timemod_gate_rewind")
    public void rewindDoesNotRefundTheTrip(GameTestHelper helper) {
        buildRing(helper, RING, Direction.Axis.X);
        ChronosConsoleBlockEntity console = placeConsole(helper);
        ServerPlayer player = player(helper, new BlockPos(13, 1, 2));
        console.addChronon(200_000);
        ChronoJournal journal = ServerTimeEngine.of(helper.getLevel()).journal();
        helper.runAtTickTime(25, () -> {
            journal.clearAnchors();
            journal.addAnchor(Vec3.atCenterOf(helper.absolutePos(new BlockPos(13, 3, 8))), 40);
            journal.setRecording(true);
        });
        helper.runAtTickTime(32, () -> {
            if (console.charge(player) != null) {
                helper.fail("Das Tor muss laden");
            }
        });
        helper.runAtTickTime(45, () -> {
            // Nur die eigene Arena: Reste früherer Tests in der Nähe sollen nicht mitspielen
            journal.rewind(20, 0, new RewindSession.Region(Vec3.atCenterOf(helper.absolutePos(new BlockPos(13, 3, 6))), 12), null);
            journal.setRecording(false);
            journal.clearAnchors();
            ChronosConsoleBlockEntity restored = (ChronosConsoleBlockEntity) helper.getBlockEntity(CONSOLE);
            if (restored.storedChronon() != 200_000 - 14_500) {
                helper.fail("Dupe! Nach dem Zurückspulen hat die Konsole " + restored.storedChronon() + " statt 185.500 Chronon");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Durch das Portal: im Zieljahr an derselben Stelle, auf festem Boden. */
    @GameTest(template = ARENA, timeoutTicks = 200, setupTicks = 5, batch = "timemod_gate_travel")
    public void portalSendsPlayersThroughTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChronosGate.Shape ring = buildRing(helper, RING, Direction.Axis.X);
        BlockPos cell = ring.at(3, 1);
        BlockState portal = ModBlocks.CHRONOS_PORTAL.defaultBlockState().setValue(ChronosPortalBlock.AXIS, Direction.Axis.X)
            .setValue(ChronosPortalBlock.ERA, Eras.indexOf(Eras.byYears(-1)));
        for (BlockPos pos : ring.opening()) {
            level.setBlock(pos, portal, Block.UPDATE_CLIENTS);
        }
        ServerPlayer player = player(helper, new BlockPos(13, 2, 8));
        BlockPos before = player.blockPosition();
        level.getBlockState(cell).entityInside(level, cell, player);
        ServerLevel arrived = player.serverLevel();
        if (arrived.dimension() != Eras.byYears(-1).level()) {
            helper.fail("Spieler ist nicht im Jahr −1 angekommen, sondern in " + arrived.dimension().location());
        }
        BlockPos feet = player.blockPosition();
        if (Math.abs(feet.getX() - before.getX()) > 48 || Math.abs(feet.getZ() - before.getZ()) > 48) {
            helper.fail("Landung zu weit vom Ausgangsort: " + before + " → " + feet);
        }
        BlockState ground = arrived.getBlockState(feet.below());
        if (!ground.isFaceSturdy(arrived, feet.below(), Direction.UP) || !ground.getFluidState().isEmpty()
            || !arrived.getBlockState(feet).getCollisionShape(arrived, feet).isEmpty()) {
            helper.fail("Unsichere Landung auf " + ground + " bei " + feet);
        }
        // Gleich wieder hinein: Die Abklingzeit verhindert ein Hin- und Herspringen
        if (TimeTravel.travel(player, Eras.PRESENT)) {
            helper.fail("Direkt nach der Ankunft darf keine zweite Reise starten");
        }
        player.discard();
        helper.succeed();
    }

    /** Fossilien unter dem Pinsel, Bernstein mit Insekten im Gestein, und die Konsole behält ihr Chronon. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void fossilsAmberAndConsoleLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable pyramid = level.getServer().getLootData().getLootTable(new ResourceLocation("archaeology/desert_pyramid"));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.ARCHAEOLOGY);
        int fossils = 0;
        for (int i = 0; i < 400; i++) {
            List<ItemStack> found = pyramid.getRandomItems(params, 20261017L + i);
            if (found.size() != 1) {
                helper.fail("Ein Pinselstrich muss genau einen Fund liefern, nicht " + found.size());
            }
            if (found.get(0).is(ModItems.FOSSIL_FRAGMENT)) {
                fossils++;
            }
        }
        if (fossils < 40 || fossils > 140) {
            helper.fail("Fossilien in Wüstentempeln: " + fossils + " von 400");
        }
        BlockPos orePos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(orePos, ModBlocks.AMBER_ORE.defaultBlockState());
        int amber = 0;
        int insects = 0;
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        for (int i = 0; i < 300; i++) {
            for (ItemStack drop : Block.getDrops(ModBlocks.AMBER_ORE.defaultBlockState(), level, orePos, null, null, pickaxe)) {
                if (drop.is(ModItems.AMBER)) {
                    amber += drop.getCount();
                } else if (drop.is(ModItems.INSECT_AMBER)) {
                    insects++;
                }
            }
        }
        if (amber < 300 || insects < 10 || insects > 60) {
            helper.fail("Bernsteinerz: " + amber + " Bernstein, " + insects + " mit Insekt aus 300 Blöcken");
        }
        BlockPos consolePos = helper.absolutePos(new BlockPos(2, 1, 1));
        level.setBlockAndUpdate(consolePos, ModBlocks.CHRONOS_CONSOLE.defaultBlockState());
        ChronosConsoleBlockEntity console = (ChronosConsoleBlockEntity) level.getBlockEntity(consolePos);
        console.addChronon(123_456);
        List<ItemStack> drops = Block.getDrops(level.getBlockState(consolePos), level, consolePos, console);
        boolean kept = drops.stream().anyMatch(stack -> stack.is(ModItems.CHRONOS_CONSOLE) && stack.getTag() != null
            && stack.getTag().getCompound("BlockEntityTag").getInt("Chronon") == 123_456);
        if (!kept) {
            helper.fail("Die abgebaute Konsole muss ihr Chronon behalten: " + drops);
        }
        helper.succeed();
    }
}

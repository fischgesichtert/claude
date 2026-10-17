package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * „Video-Äquivalenz“: Zwei identische Aufbauten laufen nebeneinander, einer davon in einer
 * Zeitzone. Bei Faktor f muss Aufbau B nach t Ticks exakt so aussehen wie Aufbau A nach f·t Ticks –
 * fallender Sand, Wasserfluss, Redstone, Ofen, Lore und Pfeil, alles Block für Block.
 */
public class TimeEquivalenceTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";
    private static final int A = 0;
    private static final int B = 20;
    private static final int WIDTH = 20;

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20)
    public void doubleSpeedEqualsTwoTicks(GameTestHelper helper) {
        build(helper, A);
        build(helper, B);
        zone(helper, B, 2.0, -1);
        String[] b = new String[1];
        helper.runAtTickTime(30, () -> b[0] = snapshot(helper, B));
        helper.runAtTickTime(60, () -> {
            assertSame(helper, "2× nach 30 Ticks", snapshot(helper, A), b[0]);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20)
    public void halfSpeedEqualsHalfTicks(GameTestHelper helper) {
        build(helper, A);
        build(helper, B);
        zone(helper, B, 0.5, -1);
        String[] a = new String[1];
        helper.runAtTickTime(30, () -> a[0] = snapshot(helper, A));
        helper.runAtTickTime(60, () -> {
            assertSame(helper, "0,5× nach 60 Ticks", a[0], snapshot(helper, B));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20)
    public void fractionalSpeed(GameTestHelper helper) {
        build(helper, A);
        build(helper, B);
        zone(helper, B, 1.5, -1);
        String[] b = new String[1];
        helper.runAtTickTime(40, () -> b[0] = snapshot(helper, B));
        helper.runAtTickTime(60, () -> {
            assertSame(helper, "1,5× nach 40 Ticks", snapshot(helper, A), b[0]);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20)
    public void tenTimesSpeed(GameTestHelper helper) {
        build(helper, A);
        build(helper, B);
        zone(helper, B, 10.0, -1);
        String[] b = new String[1];
        helper.runAtTickTime(6, () -> b[0] = snapshot(helper, B));
        helper.runAtTickTime(60, () -> {
            assertSame(helper, "10× nach 6 Ticks", snapshot(helper, A), b[0]);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20)
    public void stopFreezesAndResumesExactly(GameTestHelper helper) {
        build(helper, A);
        build(helper, B);
        zone(helper, B, 0.0, 20);
        // Frisch gespawnte Entities sind erst nach dem nächsten Welt-Tick abfragbar; B steht ab Tick 1 still.
        String[] start = new String[1];
        String[] a = new String[1];
        helper.runAtTickTime(2, () -> start[0] = snapshot(helper, B));
        helper.runAtTickTime(20, () -> assertSame(helper, "Stopp: nichts darf sich bewegen", start[0], snapshot(helper, B)));
        helper.runAtTickTime(30, () -> a[0] = snapshot(helper, A));
        helper.runAtTickTime(50, () -> {
            assertSame(helper, "nach dem Stopp genau 20 Ticks hinterher", a[0], snapshot(helper, B));
            helper.succeed();
        });
    }

    /**
     * Volle globale Zone (Welt-Uhr) mit einer lokalen 0,5×-Zone darin: Faktoren multiplizieren sich.
     * Achtung: Gametests zählen Ticks in Spielzeit – bei globalem 2× vergehen pro echtem Tick zwei.
     * {@code setupTicks} gibt den frisch erzwungen geladenen Chunks Zeit, voll zu ticken.
     */
    @GameTest(template = ARENA, timeoutTicks = 200, setupTicks = 20, batch = "timemod_global")
    public void globalSpeedAndNesting(GameTestHelper helper) {
        build(helper, A);
        build(helper, B);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        // 30 echte Ticks lang: Welt 2×, A darin 0,5× (also effektiv 1×), B 2×
        engine.addZone(ZoneShape.Global.INSTANCE, 2.0, TimeFilter.DEFAULT, null, 30);
        zone(helper, A, 0.5, 30);
        int startServerTick = helper.getLevel().getServer().getTickCount();
        String[] b = new String[1];
        helper.runAtTickTime(60, () -> {
            int realTicks = helper.getLevel().getServer().getTickCount() - startServerTick;
            if (realTicks != 30) {
                helper.fail("60 Ticks Spielzeit sollten bei globalem 2× in 30 echten Ticks vergehen, waren aber " + realTicks);
            }
            b[0] = snapshot(helper, B);
        });
        // danach läuft alles normal: A hat nach 30 Schritten noch 30 Schritte bis zum Stand von B
        helper.runAtTickTime(90, () -> {
            assertSame(helper, "global 2× + lokal 0,5×", snapshot(helper, A), b[0]);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------

    private static void zone(GameTestHelper helper, int ox, double factor, int ticks) {
        BlockPos min = helper.absolutePos(new BlockPos(ox, 1, 0));
        BlockPos max = helper.absolutePos(new BlockPos(ox + WIDTH - 1, 14, 15));
        AABB box = new AABB(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()),
            Math.max(min.getX(), max.getX()) + 1, Math.max(min.getY(), max.getY()) + 1, Math.max(min.getZ(), max.getZ()) + 1);
        ServerTimeEngine.of(helper.getLevel()).addZone(new ZoneShape.Box(box), factor, TimeFilter.DEFAULT, null, ticks);
    }

    /** Baut einen Testaufbau mit deterministischen Systemen ab Spalte {@code ox}. */
    private static void build(GameTestHelper helper, int ox) {
        // Fallender Sand (geplanter Tick + Entity-Physik)
        helper.setBlock(new BlockPos(ox + 2, 12, 2), Blocks.SAND);
        helper.setBlock(new BlockPos(ox + 3, 8, 2), Blocks.GRAVEL);

        // Wasserbecken (geplante Flüssigkeits-Ticks)
        for (int x = ox + 4; x <= ox + 12; x++) {
            for (int z = 1; z <= 7; z++) {
                if (x == ox + 4 || x == ox + 12 || z == 1 || z == 7) {
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.GLASS);
                }
            }
        }
        helper.setBlock(new BlockPos(ox + 6, 2, 3), Blocks.WATER);

        // Redstone-Kette (Repeater mit 4 Ticks Verzögerung)
        for (int x = ox + 3; x <= ox + 12; x++) {
            helper.setBlock(new BlockPos(x, 2, 10), Blocks.REPEATER.defaultBlockState()
                .setValue(RepeaterBlock.FACING, Direction.WEST)
                .setValue(RepeaterBlock.DELAY, 4));
        }
        helper.setBlock(new BlockPos(ox + 13, 2, 10), Blocks.REDSTONE_LAMP);
        helper.setBlock(new BlockPos(ox + 2, 2, 10), Blocks.REDSTONE_BLOCK);

        // Ofen (Block-Entity)
        helper.setBlock(new BlockPos(ox + 15, 2, 2), Blocks.FURNACE);
        if (helper.getBlockEntity(new BlockPos(ox + 15, 2, 2)) instanceof FurnaceBlockEntity furnace) {
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
            furnace.setItem(1, new ItemStack(Items.COAL, 2));
        }

        // Lore auf Schienen (Entity-Physik)
        for (int x = ox + 1; x <= ox + 18; x++) {
            helper.setBlock(new BlockPos(x, 2, 14), Blocks.RAIL);
        }
        Minecart cart = helper.spawn(EntityType.MINECART, new Vec3(ox + 2.5, 2.0625, 14.5));
        cart.setDeltaMovement(0.2, 0, 0);

        // Pfeil im Flug
        Arrow arrow = helper.spawn(EntityType.ARROW, new Vec3(ox + 15.5, 7.0, 8.5));
        arrow.setDeltaMovement(0.12, 0.25, 0.05);
    }

    /** Vollständiger Zustand eines Aufbaus, relativ zu seinem Ursprung. */
    private static String snapshot(GameTestHelper helper, int ox) {
        StringBuilder out = new StringBuilder();
        for (int x = ox; x < ox + WIDTH; x++) {
            for (int y = 1; y <= 14; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = helper.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    out.append(x - ox).append(',').append(y).append(',').append(z).append(' ').append(state);
                    if (!state.getFluidState().isEmpty() && state.getFluidState().getType() != Fluids.EMPTY) {
                        out.append(" fluid=").append(state.getFluidState().getAmount());
                    }
                    BlockEntity blockEntity = helper.getBlockEntity(pos);
                    if (blockEntity != null) {
                        CompoundTag tag = blockEntity.saveWithoutMetadata();
                        tag.remove("timemod:Eigenzeit");
                        out.append(' ').append(tag);
                    }
                    out.append('\n');
                }
            }
        }
        BlockPos min = helper.absolutePos(new BlockPos(ox, 1, 0));
        BlockPos max = helper.absolutePos(new BlockPos(ox + WIDTH - 1, 14, 15));
        AABB box = new AABB(min, max).inflate(0.5).expandTowards(1, 1, 1);
        BlockPos origin = helper.absolutePos(new BlockPos(ox, 0, 0));
        List<String> entities = new ArrayList<>();
        for (Entity entity : helper.getLevel().getEntities((Entity) null, box, e -> !(e instanceof Player))) {
            Vec3 p = entity.position().subtract(origin.getX(), origin.getY(), origin.getZ());
            Vec3 v = entity.getDeltaMovement();
            entities.add(String.format(Locale.ROOT, "%s @ %.6f %.6f %.6f v %.6f %.6f %.6f age=%d",
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), p.x, p.y, p.z, v.x, v.y, v.z, entity.tickCount));
        }
        entities.sort(Comparator.naturalOrder());
        entities.forEach(e -> out.append(e).append('\n'));
        return out.toString();
    }

    private static void assertSame(GameTestHelper helper, String what, String expected, String actual) {
        if (expected.equals(actual)) {
            return;
        }
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("snapshots");
            java.nio.file.Files.createDirectories(dir);
            String file = what.replaceAll("[^A-Za-z0-9]+", "_");
            java.nio.file.Files.writeString(dir.resolve(file + "_erwartet.txt"), expected);
            java.nio.file.Files.writeString(dir.resolve(file + "_tatsaechlich.txt"), actual);
        } catch (java.io.IOException ignored) {
        }
        String[] e = expected.split("\n");
        String[] a = actual.split("\n");
        for (int i = 0; i < Math.max(e.length, a.length); i++) {
            String el = i < e.length ? e[i] : "<fehlt>";
            String al = i < a.length ? a[i] : "<fehlt>";
            if (!el.equals(al)) {
                StringBuilder entities = new StringBuilder();
                for (String line : e) {
                    if (line.startsWith("minecraft:")) {
                        entities.append("\n  [erwartet] ").append(line);
                    }
                }
                for (String line : a) {
                    if (line.startsWith("minecraft:")) {
                        entities.append("\n  [tatsächlich] ").append(line);
                    }
                }
                helper.fail(what + ": Unterschied in Zeile " + i + "\n  erwartet: " + el + "\n  tatsächlich: " + al + entities);
                return;
            }
        }
    }
}

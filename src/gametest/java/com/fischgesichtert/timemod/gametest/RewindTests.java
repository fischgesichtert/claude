package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Zurückspulen: Nach 40 Ticks voller Ereignisse (Explosion, Baum wächst, Wasser fließt, Sand fällt,
 * Tier stirbt, neues Tier, Item fällt, Ofen und Truhe ändern sich) muss die Welt nach dem
 * Zurückspulen exakt so aussehen wie vorher – Block für Block, Entity für Entity.
 */
public class RewindTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20, batch = "timemod_rewind")
    public void rewindRestoresEverything(GameTestHelper helper) {
        // Ausgangslage
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.FURNACE);
        if (helper.getBlockEntity(new BlockPos(5, 2, 5)) instanceof FurnaceBlockEntity furnace) {
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 16));
            furnace.setItem(1, new ItemStack(Items.COAL, 4));
        }
        helper.setBlock(new BlockPos(7, 2, 5), Blocks.CHEST);
        if (helper.getBlockEntity(new BlockPos(7, 2, 5)) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        }
        for (int x = 20; x <= 26; x++) {
            for (int z = 4; z <= 10; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.OAK_PLANKS);
            }
        }
        helper.setBlock(new BlockPos(12, 2, 12), Blocks.OAK_SAPLING);
        Pig doomed = helper.spawn(EntityType.PIG, new Vec3(30.5, 2, 12.5));
        Pig survivor = helper.spawn(EntityType.PIG, new Vec3(34.5, 2, 4.5));

        ChronoJournal journal = ServerTimeEngine.of(helper.getLevel()).journal();
        journal.clearAnchors();
        journal.addAnchor(Vec3.atCenterOf(helper.absolutePos(new BlockPos(20, 6, 8))), 40);
        journal.setRecording(true);

        String[] before = new String[1];
        helper.runAtTickTime(5, () -> before[0] = snapshot(helper));
        // Ereignisse
        helper.runAtTickTime(6, () -> {
            helper.setBlock(new BlockPos(3, 9, 12), Blocks.SAND);
            helper.setBlock(new BlockPos(16, 2, 3), Blocks.WATER);
            helper.destroyBlock(new BlockPos(20, 3, 4));
        });
        helper.runAtTickTime(10, doomed::kill);
        helper.runAtTickTime(12, () -> helper.spawn(EntityType.PIG, new Vec3(14.5, 2, 6.5)));
        helper.runAtTickTime(16, () -> {
            if (helper.getBlockEntity(new BlockPos(7, 2, 5)) instanceof ChestBlockEntity chest) {
                chest.setItem(0, ItemStack.EMPTY);
                chest.setItem(5, new ItemStack(Items.EMERALD, 3));
            }
        });
        helper.runAtTickTime(18, () -> helper.spawnItem(Items.GOLD_INGOT, 9.5F, 3F, 9.5F));
        helper.runAtTickTime(20, () -> {
            BlockPos center = helper.absolutePos(new BlockPos(23, 3, 7));
            helper.getLevel().explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 2.5F, Level.ExplosionInteraction.TNT);
        });
        helper.runAtTickTime(25, () -> {
            BlockPos sapling = helper.absolutePos(new BlockPos(12, 2, 12));
            for (int i = 0; i < 2; i++) {
                BlockState state = helper.getLevel().getBlockState(sapling);
                if (state.getBlock() instanceof SaplingBlock block) {
                    block.advanceTree(helper.getLevel(), sapling, state, helper.getLevel().random);
                }
            }
        });
        helper.runAtTickTime(45, () -> {
            String after45 = snapshot(helper);
            if (after45.equals(before[0])) {
                helper.fail("Der Test ist wertlos: In 40 Ticks hat sich nichts verändert");
            }
            journal.rewind(40, 0, null, null);
            String rewound = snapshot(helper);
            journal.setRecording(false);
            journal.clearAnchors();
            if (!survivor.isAlive()) {
                helper.fail("Das zweite Schwein sollte leben");
            }
            TimeEquivalenceTests.assertSameSnapshot(helper, "Zurückspulen um 40 Ticks", before[0], rewound);
            helper.succeed();
        });
    }

    /** Blöcke, Block-Entity-Daten und Entities (mit UUID, Position, Leben) des Testbereichs. */
    static String snapshot(GameTestHelper helper) {
        StringBuilder out = new StringBuilder();
        for (int x = 0; x < 40; x++) {
            for (int y = 1; y <= 14; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = helper.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    out.append(x).append(',').append(y).append(',').append(z).append(' ').append(state);
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
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        AABB box = new AABB(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(39, 14, 15))).inflate(1);
        List<String> entities = new ArrayList<>();
        for (Entity entity : helper.getLevel().getEntities((Entity) null, box, e -> !(e instanceof Player))) {
            String extra = entity instanceof LivingEntity living ? String.format(Locale.ROOT, " hp=%.2f", living.getHealth())
                : entity instanceof ItemEntity item ? " " + item.getItem() : "";
            entities.add(String.format(Locale.ROOT, "%s %s @ %.4f %.4f %.4f%s", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), entity.getUUID(),
                entity.getX() - origin.getX(), entity.getY() - origin.getY(), entity.getZ() - origin.getZ(), extra));
        }
        entities.sort(String::compareTo);
        entities.forEach(e -> out.append(e).append('\n'));
        return out.toString();
    }
}

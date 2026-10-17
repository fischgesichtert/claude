package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.AncientTrees;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.ChangeTracking;
import com.fischgesichtert.timemod.future.FutureChunks;
import com.fischgesichtert.timemod.future.FutureDerivation;
import com.fischgesichtert.timemod.future.RiverBanksFeature;
import com.fischgesichtert.timemod.registry.ModWorldgen;
import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Die Welt der Zukunft: Wälder wachsen in die Ebenen, der Schnee zieht sich zurück, alte Bäume werden riesig. */
public class ClimateTests implements FabricGameTest {
    private static final int[] FUTURE = {10, 100, 500, 1000};

    private static ServerLevel era(GameTestHelper helper, int years) {
        return helper.getLevel().getServer().getLevel(Eras.byYears(years).level());
    }

    /** Stichproben auf einem weiten Gitter: heute und in vier Zukünften an derselben Stelle. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void forestsSpreadIntoThePlains(GameTestHelper helper) {
        ServerLevel[] levels = new ServerLevel[FUTURE.length];
        for (int i = 0; i < FUTURE.length; i++) {
            levels[i] = era(helper, FUTURE[i]);
        }
        FutureBiomeSource far = (FutureBiomeSource) levels[3].getChunkSource().getGenerator().getBiomeSource();
        BiomeSource today = far.base();
        Climate.Sampler sampler = levels[3].getChunkSource().randomState().sampler();
        int open = 0;
        int grown = 0;
        int samples = 0;
        int nearChanges = 0;
        int thawed = 0;
        int spread = 0;
        for (int gx = -40; gx < 40; gx++) {
            for (int gz = -40; gz < 40; gz++) {
                int x = gx * 113 + 17;
                int z = gz * 109 - 31;
                int qx = QuartPos.fromBlock(x);
                int qz = QuartPos.fromBlock(z);
                // An der Oberfläche: Darunter liegen Höhlen-Biome (die feuchtere Zukunft hat dort mehr üppige Höhlen)
                int qy = QuartPos.fromBlock(levels[3].getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    levels[3], levels[3].getChunkSource().randomState()));
                samples++;
                Holder<Biome> now = today.getNoiseBiome(qx, qy, qz, sampler);
                boolean wooded = false;
                for (int i = 0; i < FUTURE.length; i++) {
                    BiomeSource source = levels[i].getChunkSource().getGenerator().getBiomeSource();
                    Holder<Biome> then = source.getNoiseBiome(qx, qy, qz, levels[i].getChunkSource().randomState().sampler());
                    String where = " bei " + QuartPos.toBlock(qx) + "/" + QuartPos.toBlock(qz) + " im Jahr +" + FUTURE[i] + ": " + name(now)
                        + " → " + name(then);
                    if (now.is(FutureBiomeSource.STEADFAST) && then != now) {
                        helper.fail("Gewachsene Wälder bleiben" + where);
                    }
                    if (wooded && !then.is(FutureBiomeSource.WOODED)) {
                        helper.fail("Was einmal Wald ist, bleibt Wald" + where);
                    }
                    if (now.is(FutureBiomeSource.WOODED) && !then.is(FutureBiomeSource.WOODED)) {
                        helper.fail("Der Wald zieht sich nie zurück" + where);
                    }
                    if (cold(then) && !cold(now)) {
                        helper.fail("Es wird nirgends kälter" + where);
                    }
                    if (now.is(Biomes.DESERT) && !then.is(Biomes.DESERT)) {
                        helper.fail("Wüsten schrumpfen nicht" + where);
                    }
                    wooded |= then.is(FutureBiomeSource.WOODED);
                    if (FUTURE[i] == 10 && then != now) {
                        nearChanges++;
                    }
                    if (FUTURE[i] == 1000) {
                        if (isPlains(now)) {
                            open++;
                            grown += then.is(FutureBiomeSource.WOODED) ? 1 : 0;
                        }
                        thawed += cold(now) && !cold(then) ? 1 : 0;
                        spread += then.is(FutureBiomeSource.SPREADING) && !now.is(FutureBiomeSource.SPREADING) ? 1 : 0;
                    }
                }
            }
        }
        double share = open == 0 ? 0 : grown / (double) open;
        TimeMod.LOGGER.info(String.format(Locale.ROOT, "Zukunft +1000: %d von %d Ebenen bewaldet (%.0f %%), %d aufgetaut, %d Wüste/Mangrove dazu, "
            + "+10: %d von %d Stellen anders", grown, open, share * 100, thawed, spread, nearChanges, samples));
        if (open < 50) {
            helper.fail("Zu wenige Ebenen in der Stichprobe: " + open);
        }
        if (share < 0.15 || share > 0.7) {
            helper.fail(String.format(Locale.ROOT, "Nach tausend Jahren soll ein guter Teil der Ebenen Wald sein, nicht alles: %.0f %%", share * 100));
        }
        if (nearChanges > samples / 50) {
            helper.fail("Nach zehn Jahren sieht die Welt fast aus wie heute, anders sind aber " + nearChanges + " von " + samples);
        }
        helper.succeed();
    }

    /** Ein Uralter, wie ihn die Wälder der Zukunft tragen: hoch, breit, und nichts außerhalb seines Bereichs. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void ancientGiantsStandInFutureForests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<PlacedFeature> grove = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(ModWorldgen.ANCIENT_GROVE);
        if (!biomes.getHolderOrThrow(Biomes.FOREST).value().getGenerationSettings().hasFeature(grove.value())
            || biomes.getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(grove.value())) {
            helper.fail("Die Riesen gehören in die Wälder, nicht in die Ebenen");
        }
        int[] offsets = {0, 48};
        String[] kinds = {"Eiche", "Fichte"};
        Holder<Biome>[] woods = new Holder[] {biomes.getHolderOrThrow(Biomes.FOREST), biomes.getHolderOrThrow(Biomes.TAIGA)};
        for (int i = 0; i < 2; i++) {
            BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(4000 + offsets[i], 0, 4000);
            level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
            BlockPos ground = new BlockPos(origin.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX(), origin.getZ()), origin.getZ());
            BoundingBox area = new BoundingBox(ground.getX() - 18, ground.getY() - 6, ground.getZ() - 18, ground.getX() + 18, ground.getY() + 60,
                ground.getZ() + 18);
            AncientTrees.ancient(level, ground, woods[i], 1000, RandomSource.create(7 + i), area);
            int logs = 0;
            int leaves = 0;
            int top = 0;
            int outside = 0;
            for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-24, -6, -24), ground.offset(24, 64, 24))) {
                var state = level.getBlockState(pos);
                boolean tree = state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
                if (tree && !area.isInside(pos)) {
                    outside++;
                }
                if (state.is(BlockTags.LOGS)) {
                    logs++;
                    top = Math.max(top, pos.getY() - ground.getY());
                } else if (state.is(BlockTags.LEAVES)) {
                    leaves++;
                }
            }
            if (logs < 80 || leaves < 300 || top < 22 || outside > 0) {
                helper.fail(kinds[i] + "-Riese zu klein oder außerhalb: " + logs + " Stämme, " + leaves + " Laub, " + top + " hoch, " + outside
                    + " außerhalb");
            }
        }
        helper.succeed();
    }

    /** Ein seichter Fluss bekommt eine Sandbank, sein niedriges Ufer einen Sandstrand. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void riversGrowSandbanks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        int surface = o.getY() + 2;
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(o.offset(x, 0, 0), Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(o.offset(x, 1, 0), Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(o.offset(x, 2, 0), Blocks.WATER.defaultBlockState());
            // Das Ufer daneben liegt eine Stufe über dem Wasser
            level.setBlockAndUpdate(o.offset(x, 1, 1), Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(o.offset(x, 2, 1), Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(o.offset(x, 3, 1), Blocks.GRASS_BLOCK.defaultBlockState());
        }
        level.setBlockAndUpdate(o.offset(1, 4, 1), Blocks.POPPY.defaultBlockState());
        // Erst das Ufer (es braucht das Wasser nebenan), dann die Bank im Fluss
        RiverBanksFeature.shape(level, o.getX() + 1, o.getZ() + 1, surface, 1.0, 0.6);
        RiverBanksFeature.shape(level, o.getX() + 1, o.getZ(), surface, 1.0, 0.6);
        // Weit genug abgelagert ragt die Bank aus dem Wasser
        helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 2, 1));
        helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 3, 1));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(1, 3, 1));
        // Das Ufer: Die Kante ist abgebrochen, die Blume weggespült, darunter Sand
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 4, 2));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 5, 2));
        helper.assertBlockPresent(Blocks.SAND, new BlockPos(2, 3, 2));
        // Wo sich nichts ablagert, bleibt der Fluss, wie er ist
        RiverBanksFeature.shape(level, o.getX(), o.getZ(), surface, 0.1, 0.6);
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(1, 2, 1));
        helper.succeed();
    }

    /** Ein Bauplatz, wo die Zukunft Wald ist: Zwischen den Mauern wachsen Bäume, aber nicht in sie hinein. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void forestReclaimsDerivedChunks(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        ServerLevel far = era(helper, 1000);
        BlockPos start = helper.absolutePos(BlockPos.ZERO).offset(5000, 0, 5000);
        ChunkPos chunk = null;
        int ground = 0;
        for (int i = 0; i < 400 && chunk == null; i++) {
            ChunkPos candidate = new ChunkPos((start.getX() >> 4) + (i % 20) * 3, (start.getZ() >> 4) + (i / 20) * 3);
            present.getChunk(candidate.x, candidate.z);
            int y = present.getHeight(Heightmap.Types.WORLD_SURFACE, candidate.getMiddleBlockX(), candidate.getMiddleBlockZ());
            boolean wooded = true;
            for (int dx = 2; dx <= 13 && wooded; dx += 11) {
                for (int dz = 2; dz <= 13 && wooded; dz += 11) {
                    wooded = far.getBiome(new BlockPos(candidate.getMinBlockX() + dx, y, candidate.getMinBlockZ() + dz)).is(Biomes.FOREST);
                }
            }
            if (wooded) {
                chunk = candidate;
                ground = y;
            }
        }
        if (chunk == null) {
            helper.fail("Kein künftiger Eichenwald über der flachen Testwelt gefunden");
            return;
        }
        BlockPos wall = new BlockPos(chunk.getMinBlockX() + 7, ground, chunk.getMinBlockZ() + 7);
        for (int y = 0; y < 3; y++) {
            present.setBlockAndUpdate(wall.above(y), Blocks.STONE_BRICKS.defaultBlockState());
        }
        ChangeTracking.changed(present, wall, true);
        ChunkPos target = chunk;
        int floor = ground;
        far.setChunkForced(target.x, target.z, true);
        helper.startSequence().thenWaitUntil(() -> {
            if (!FutureDerivation.isSettled(far, target) || !FutureChunks.get(far).isDerived(target.toLong())) {
                helper.fail("Die Zukunft wird noch abgeleitet");
            }
        }).thenExecute(() -> {
            int trunks = 0;
            for (BlockPos pos : BlockPos.betweenClosed(target.getMinBlockX(), floor, target.getMinBlockZ(), target.getMaxBlockX(), floor,
                target.getMaxBlockZ())) {
                if (far.getBlockState(pos).is(BlockTags.LOGS)) {
                    trunks++;
                    if (Math.abs(pos.getX() - wall.getX()) <= 2 && Math.abs(pos.getZ() - wall.getZ()) <= 2) {
                        helper.fail("Kein Baum wächst direkt an der Mauer: " + pos);
                    }
                }
            }
            far.setChunkForced(target.x, target.z, false);
            if (trunks < 2) {
                helper.fail("Nach tausend Jahren wächst im künftigen Wald auch zwischen den Trümmern Wald, gefunden: " + trunks + " Stämme");
            }
        }).thenSucceed();
    }

    private static boolean isPlains(Holder<Biome> biome) {
        return biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS);
    }

    private static boolean cold(Holder<Biome> biome) {
        return biome.value().getBaseTemperature() < 0.15F;
    }

    private static String name(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().getPath()).orElse("?");
    }
}

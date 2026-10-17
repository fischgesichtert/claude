package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.EraSpawns;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.worldgen.RemappedBiomeSource;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.random.WeightedRandomList;

/** Die Epochen der Zeitmaschine: Dimensionen, Meeresspiegel, Landschaft, Lebewelt und Böden. */
public class EraTests implements FabricGameTest {
    private static final ResourceKey<Biome> PANGAEA_DESERT = ResourceKey.create(Registries.BIOME, TimeMod.id("pangaea_desert"));
    private static final ResourceKey<Biome> VOLCANIC_WASTES = ResourceKey.create(Registries.BIOME, TimeMod.id("volcanic_wastes"));

    private static ServerLevel era(GameTestHelper helper, int years) {
        Era era = Eras.byYears(years);
        ServerLevel level = era == null ? null : helper.getLevel().getServer().getLevel(era.level());
        if (level == null) {
            helper.fail("Keine Welt für das Jahr " + years);
        }
        return level;
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void everyEraHasItsWorld(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (Era era : Eras.all()) {
            if (server.getLevel(era.level()) == null) {
                helper.fail("Dimension fehlt: " + era.level().location());
            }
        }
        for (ResourceKey<Level> key : server.levelKeys()) {
            if (Eras.isEraDimension(key.location()) && Eras.byLevel(key) == null) {
                helper.fail("Dimension ohne Epoche: " + key.location());
            }
        }
        if (Eras.all().size() != 34 || !Eras.all().get(0).equals(Eras.byYears(-245_000_000)) || Eras.PRESENT.years() != 0) {
            helper.fail("Stufenliste unvollständig: " + Eras.all().size());
        }
        // Schnellsprung: von der jüngsten Vergangenheit direkt in die Jungsteinzeit, dann in die Eiszeit
        Era jumped = Eras.jump(Eras.byYears(-5), -1);
        if (jumped.years() != -5_000 || Eras.jump(jumped, -1).years() != -20_000 || Eras.jump(Eras.byYears(-66_000_000), -1).years() != -245_000_000
            || Eras.jump(Eras.PRESENT, 1).years() != 1 || Eras.jump(Eras.byYears(1), 1).years() != 1000) {
            helper.fail("Schnellsprung landet falsch: " + jumped.years());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void seaLevelsFollowEarthHistory(GameTestHelper helper) {
        int[][] expected = {{1000, 63}, {-1, 63}, {-20_000, 50}, {-100_000, 56}, {-1_000_000, 56}, {-10_000_000, 64}, {-70_000_000, 65},
            {-150_000_000, 64}, {-245_000_000, 63}};
        for (int[] pair : expected) {
            int sea = era(helper, pair[0]).getSeaLevel();
            if (sea != pair[1]) {
                helper.fail("Meeresspiegel im Jahr " + pair[0] + ": " + sea + " statt " + pair[1]);
            }
        }
        helper.succeed();
    }

    /** Nahe Epochen sind deine Welt ohne deine Bauten; die Eiszeit hat dieselben Berge, aber weniger Meer. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nearErasKeepTheLandscape(GameTestHelper helper) {
        ServerLevel past = era(helper, -1);
        ServerLevel future = era(helper, 1000);
        ServerLevel ice = era(helper, -20_000);
        ChunkGenerator pastGenerator = past.getChunkSource().getGenerator();
        int drained = 0;
        for (int i = 0; i < 64; i++) {
            int x = (i % 8) * 613 - 2400;
            int z = (i / 8) * 587 - 2300;
            int floor = pastGenerator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, past, past.getChunkSource().randomState());
            int futureFloor = future.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, future,
                future.getChunkSource().randomState());
            int iceFloor = ice.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ice, ice.getChunkSource().randomState());
            if (floor != futureFloor || floor != iceFloor) {
                helper.fail("Gelände weicht ab bei " + x + "/" + z + ": " + floor + ", " + futureFloor + ", " + iceFloor);
            }
            if (floor < 63 && floor >= ice.getSeaLevel()) {
                drained++;
            }
        }
        if (drained == 0) {
            helper.fail("Keine Stichprobe liegt im flachen Meer – die Eiszeit legt nichts trocken");
        }
        // Die Eiszeit deutet die Biome der heutigen Welt um, Stelle für Stelle
        RemappedBiomeSource remapped = (RemappedBiomeSource) ice.getChunkSource().getGenerator().getBiomeSource();
        BiomeSource today = pastGenerator.getBiomeSource();
        Climate.Sampler todaySampler = past.getChunkSource().randomState().sampler();
        Climate.Sampler iceSampler = ice.getChunkSource().randomState().sampler();
        boolean changed = false;
        for (int i = 0; i < 256; i++) {
            int qx = QuartPos.fromBlock((i % 16) * 397 - 3000);
            int qz = QuartPos.fromBlock((i / 16) * 419 - 3100);
            int qy = QuartPos.fromBlock(72);
            Holder<Biome> original = today.getNoiseBiome(qx, qy, qz, todaySampler);
            Holder<Biome> glacial = remapped.getNoiseBiome(qx, qy, qz, iceSampler);
            if (glacial != remapped.map(original)) {
                helper.fail("Eiszeit-Biom passt nicht zur heutigen Welt: " + original.unwrapKey() + " → " + glacial.unwrapKey());
            }
            changed |= glacial != original;
        }
        if (!changed) {
            helper.fail("Die Eiszeit sieht genauso aus wie heute");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deepPastHasNoVanillaLife(GameTestHelper helper) {
        for (Era era : Eras.all()) {
            if (era.vanillaLife() || era.years() == -1_000_000) {
                continue;
            }
            ServerLevel level = era(helper, era.years());
            for (Holder<Biome> biome : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
                String id = biome.unwrapKey().map(key -> key.location().toString()).orElse("?");
                if (!id.startsWith(TimeMod.MOD_ID + ":")) {
                    helper.fail("Vanilla-Biom in der Urzeit (" + era.years() + "): " + id);
                }
                for (MobCategory category : MobCategory.values()) {
                    if (!biome.value().getMobSettings().getMobs(category).isEmpty()) {
                        helper.fail("Urzeit-Biom " + id + " spawnt " + category);
                    }
                }
            }
        }
        // Im Pleistozän stehen noch Vanilla-Taigas: Deren Wesen fallen weg, fremde bleiben
        ServerLevel steppe = era(helper, -1_000_000);
        WeightedRandomList<MobSpawnSettings.SpawnerData> taiga = WeightedRandomList.create(
            new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 100, 1, 4), new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4));
        if (!EraSpawns.filter(steppe.dimension(), taiga).isEmpty()) {
            helper.fail("Im Pleistozän spawnen noch Zombies und Wölfe");
        }
        if (EraSpawns.filter(era(helper, -20_000).dimension(), taiga) != taiga || EraSpawns.filter(Level.OVERWORLD, taiga) != taiga) {
            helper.fail("Eiszeit und Gegenwart dürfen ihre Wesen behalten");
        }
        helper.succeed();
    }

    /** Jede Epoche braucht Land zum Erkunden und Meer für Binnenmeere – keine Wasserwüste. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void everyEraHasLandAndSea(GameTestHelper helper) {
        StringBuilder report = new StringBuilder();
        boolean failed = false;
        for (Era era : Eras.all()) {
            if (era.isPresent() || (era.years() > -20_000 && era.years() != -1)) {
                continue;
            }
            ServerLevel level = era(helper, era.years());
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            int sea = generator.getSeaLevel();
            int land = 0;
            int samples = 0;
            for (int gx = -16; gx < 16; gx++) {
                for (int gz = -16; gz < 16; gz++) {
                    int floor = generator.getBaseHeight(gx * 160 + 37, gz * 160 + 53, Heightmap.Types.OCEAN_FLOOR_WG, level,
                        level.getChunkSource().randomState());
                    samples++;
                    if (floor >= sea) {
                        land++;
                    }
                }
            }
            double share = land / (double) samples;
            report.append(String.format(Locale.ROOT, " %d:%.2f", era.years(), share));
            failed |= share < 0.25 || share > 0.85;
        }
        TimeMod.LOGGER.info("Landanteile:{}", report);
        if (failed) {
            helper.fail("Landanteil außerhalb von 25–85 %:" + report);
        }
        helper.succeed();
    }

    /** Minecraft bricht ab, wenn Biome einer Welt ihre Features widersprüchlich ordnen. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void eraFeaturesHaveAConsistentOrder(GameTestHelper helper) {
        for (Era era : Eras.all()) {
            ServerLevel level = helper.getLevel().getServer().getLevel(era.level());
            List<Holder<Biome>> biomes = List.copyOf(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
            try {
                FeatureSorter.buildFeaturesPerStep(biomes, biome -> biome.value().getGenerationSettings().features(), true);
            } catch (IllegalStateException e) {
                helper.fail("Feature-Reihenfolge im Jahr " + era.years() + ": " + e.getMessage());
            }
        }
        helper.succeed();
    }

    /** Eigene Mod-Daten gelten nicht als „experimentell“ (sonst warnt Minecraft bei jeder Welt). */
    @GameTest(template = EMPTY_STRUCTURE)
    public void modWorldgenIsStable(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Biome desert = biomes.getOrThrow(PANGAEA_DESERT);
        if (biomes.lifecycle(desert) != Lifecycle.stable()) {
            helper.fail("Urzeit-Biom gilt als experimentell: " + biomes.lifecycle(desert));
        }
        // Mit allen 33 Epochen-Dimensionen bleibt die Welt „stabil“ (keine Backup-Warnung beim Laden)
        Lifecycle world = helper.getLevel().getServer().getWorldData().worldGenSettingsLifecycle();
        if (world != Lifecycle.stable()) {
            helper.fail("Welt mit Epochen gilt als experimentell: " + world);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void primevalGroundsGenerate(GameTestHelper helper) {
        checkGround(helper, era(helper, -245_000_000), PANGAEA_DESERT, Set.of(Blocks.RED_SAND, Blocks.SAND, Blocks.RED_SANDSTONE));
        checkGround(helper, era(helper, -200_000_000), VOLCANIC_WASTES, Set.of(Blocks.MAGMA_BLOCK, Blocks.BASALT, Blocks.BLACKSTONE, Blocks.TUFF));
        helper.succeed();
    }

    /** Sucht das Biom, erzeugt den Chunk und prüft, dass der Boden aus den Urzeit-Regeln stammt. */
    private static void checkGround(GameTestHelper helper, ServerLevel level, ResourceKey<Biome> biome, Set<Block> ground) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(holder -> holder.is(biome), new BlockPos(0, 80, 0), 6400, 32, 64);
        if (found == null) {
            helper.fail(biome.location() + " nicht gefunden");
        }
        BlockPos at = found.getFirst();
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        int columns = 0;
        int matches = 0;
        int minX = at.getX() >> 4 << 4;
        int minZ = at.getZ() >> 4 << 4;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1, z);
                if (!level.getBiome(top).is(biome) || !level.getFluidState(top.above()).isEmpty()) {
                    continue;
                }
                columns++;
                if (ground.contains(level.getBlockState(top).getBlock())) {
                    matches++;
                }
            }
        }
        if (columns == 0 || matches * 2 < columns) {
            helper.fail(biome.location() + ": nur " + matches + " von " + columns + " Säulen haben den richtigen Boden bei " + at);
        }
    }
}

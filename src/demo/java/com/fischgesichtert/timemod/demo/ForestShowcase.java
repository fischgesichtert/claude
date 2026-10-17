package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import com.mojang.datafixers.util.Pair;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vorführung „Die Welt der Zukunft“: Biomkarten von heute bis +1000, dieselbe Luftaufnahme über einer
 * Ebene am Waldrand in vier Zeiten, ein Uralter aus der Nähe und eine Flussbiegung mit Sandbänken.
 */
final class ForestShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("TIME-Demo");
    private static final int[] ERAS = {100, 500, 1000};
    private static final int MAP_BLOCKS = 3072;
    private static final int MAP_STEP = 12;

    private static volatile BlockPos site = BlockPos.ZERO;
    private static volatile BlockPos river = BlockPos.ZERO;
    private static volatile Vec3 giantEye;
    private static volatile Vec3 giantTarget;

    private ForestShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("gamerule sendCommandFeedback false")
            .command("gamerule announceAdvancements false")
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 5000")
            .client(mc -> mc.options.renderDistance().set(10))
            .wait(20)
            .server(player -> {
                site = findSite(player);
                LOGGER.info("Bühne am Waldrand: {}", site);
                maps(player);
                aerial(player, player.server.overworld());
            })
            .wait(480).screenshot("w0_gegenwart");
        for (int i = 0; i < ERAS.length; i++) {
            int years = ERAS[i];
            d.server(player -> aerial(player, level(player, years)))
                .wait(i == 0 ? 620 : 420).screenshot(String.format("w%d_plus%d", i + 1, years));
        }
        d.server(ForestShowcase::findGiant)
            .wait(5)
            .server(player -> {
                if (giantEye != null) {
                    look(player, level(player, 1000), giantEye, giantTarget);
                }
            })
            .wait(160).screenshot("w4_riese_plus1000")
            .server(player -> {
                river = findRiver(player);
                LOGGER.info("Fluss: {}", river);
                riverView(player, player.server.overworld());
            })
            .wait(360).screenshot("w5_fluss_gegenwart")
            .server(player -> riverView(player, level(player, 1000)))
            .wait(360).screenshot("w6_fluss_plus1000")
            .wait(5).quit();
    }

    private static ServerLevel level(ServerPlayer player, int years) {
        return player.server.getLevel(Eras.byYears(years).level());
    }

    /**
     * Eine weite Ebene, an die heute nur wenig Wald grenzt, in tausend Jahren aber viel: Gesucht wird nur in den
     * Biomquellen (nichts wird erzeugt).
     */
    private static BlockPos findSite(ServerPlayer player) {
        ServerLevel far = level(player, 1000);
        FutureBiomeSource future = (FutureBiomeSource) far.getChunkSource().getGenerator().getBiomeSource();
        BiomeSource today = future.base();
        Climate.Sampler sampler = far.getChunkSource().randomState().sampler();
        // Hoch über allem Gelände: dort gilt das Biom der Oberfläche (darunter lägen Höhlen-Biome)
        int qy = QuartPos.fromBlock(200);
        ServerLevel present = player.server.overworld();
        var generator = present.getChunkSource().getGenerator();
        BlockPos best = BlockPos.ZERO;
        double bestScore = -9;
        for (BlockPos.MutableBlockPos step : BlockPos.spiralAround(BlockPos.ZERO, 24, Direction.EAST, Direction.SOUTH)) {
            int cx = step.getX() * 128;
            int cz = step.getZ() * 128;
            int plains = 0;
            int woodedToday = 0;
            int woodedLater = 0;
            int tropical = 0;
            int water = 0;
            for (int gx = -4; gx <= 4; gx++) {
                for (int gz = -4; gz <= 4; gz++) {
                    int qx = QuartPos.fromBlock(cx + gx * 22);
                    int qz = QuartPos.fromBlock(cz + gz * 22 - 40);
                    Holder<Biome> now = today.getNoiseBiome(qx, qy, qz, sampler);
                    Holder<Biome> later = future.getNoiseBiome(qx, qy, qz, sampler);
                    plains += now.is(Biomes.PLAINS) || now.is(Biomes.SUNFLOWER_PLAINS) ? 1 : 0;
                    woodedToday += now.is(FutureBiomeSource.WOODED) ? 1 : 0;
                    woodedLater += later.is(FutureBiomeSource.WOODED) ? 1 : 0;
                    tropical += later.is(BiomeTags.IS_JUNGLE) || later.is(BiomeTags.IS_SAVANNA) ? 1 : 0;
                    water += now.is(BiomeTags.IS_OCEAN) || now.is(BiomeTags.IS_RIVER) ? 1 : 0;
                }
            }
            // Viel Ebene heute, etwas Waldrand, in tausend Jahren etwa die Hälfte Laubwald (kein Dschungel)
            double score = plains / 81.0 - Math.abs(woodedLater / 81.0 - 0.55) - water / 40.0 - Math.max(0, woodedToday / 81.0 - 0.25)
                - tropical / 30.0;
            if (woodedToday <= 2 || score <= bestScore) {
                continue;
            }
            // Erst für gute Kandidaten das Gelände: flach soll es sein, sonst verdeckt ein Hügel die Sicht
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            for (int gx = -2; gx <= 2; gx++) {
                for (int gz = -2; gz <= 2; gz++) {
                    int h = generator.getBaseHeight(cx + gx * 40, cz + gz * 40 - 40, Heightmap.Types.WORLD_SURFACE_WG, present,
                        present.getChunkSource().randomState());
                    low = Math.min(low, h);
                    high = Math.max(high, h);
                }
            }
            score -= Math.max(0, high - low - 8) / 20.0;
            if (score > bestScore) {
                bestScore = score;
                best = new BlockPos(cx, 0, cz);
            }
        }
        int y = generator.getBaseHeight(best.getX(), best.getZ(), Heightmap.Types.WORLD_SURFACE_WG, present, present.getChunkSource().randomState());
        LOGGER.info("Waldrand-Wertung {}", bestScore);
        return new BlockPos(best.getX(), y, best.getZ());
    }

    /** Hoch über dem Waldrand, Blick nach Norden über die Ebene. */
    private static void aerial(ServerPlayer player, ServerLevel level) {
        player.teleportTo(level, site.getX() + 0.5, site.getY() + 58, site.getZ() + 84.5, 180.0F, 34.0F);
    }

    /** Ein Riese der Zukunft: Seine Stämme stehen immer nahe der Mitte eines Chunks. */
    private static void findGiant(ServerPlayer player) {
        ServerLevel far = level(player, 1000);
        BlockPos best = null;
        int bestHeight = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int cx = (site.getX() >> 4) - 8; cx <= (site.getX() >> 4) + 8; cx++) {
            for (int cz = (site.getZ() >> 4) - 6; cz <= (site.getZ() >> 4) + 8; cz++) {
                LevelChunk chunk = far.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (int lx = 6; lx <= 10; lx++) {
                    for (int lz = 6; lz <= 10; lz++) {
                        int x = chunk.getPos().getMinBlockX() + lx;
                        int z = chunk.getPos().getMinBlockZ() + lz;
                        int height = trunk(far, x, z);
                        double distance = new Vec3(x, 0, z).distanceTo(new Vec3(site.getX(), 0, site.getZ() + 40));
                        if (height >= 18 && (height > bestHeight + 4 || (height >= bestHeight - 4 && distance < bestDistance))) {
                            best = new BlockPos(x, far.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                            bestHeight = height;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        if (best == null) {
            LOGGER.warn("Kein Riese in der Nähe gefunden");
            giantEye = new Vec3(site.getX() + 0.5, site.getY() + 30, site.getZ() + 40);
            giantTarget = new Vec3(site.getX(), site.getY() + 10, site.getZ());
            return;
        }
        int ground = best.getY() - bestHeight;
        LOGGER.info("Riese bei {} ({} Blöcke Stamm)", best, bestHeight);
        giantTarget = new Vec3(best.getX() + 0.5, ground + bestHeight * 0.45, best.getZ() + 0.5);
        // Die Kamera schräg davor, sicher über Gelände und Laub an ihrem Standort
        int ex = best.getX() + 24;
        int ez = best.getZ() + 24;
        double eyeY = Math.max(ground + bestHeight * 0.4, far.getHeight(Heightmap.Types.MOTION_BLOCKING, ex, ez) + 3);
        giantEye = new Vec3(ex + 0.5, eyeY, ez + 0.5);
    }

    /** Wie hoch der Stamm an dieser Stelle über den Boden ragt (0: kein Stamm). */
    private static int trunk(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int y = top;
        while (y > level.getMinBuildHeight() && level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.LOGS)) {
            y--;
        }
        return top - y;
    }

    private static BlockPos findRiver(ServerPlayer player) {
        ServerLevel present = player.server.overworld();
        BlockPos from = site.offset(0, 0, 40);
        Pair<BlockPos, Holder<Biome>> found = present.findClosestBiome3d(biome -> biome.is(Biomes.RIVER), new BlockPos(from.getX(),
            present.getSeaLevel(), from.getZ()), 2400, 16, 64);
        return found == null ? site : found.getFirst();
    }

    private static void riverView(ServerPlayer player, ServerLevel level) {
        int sea = player.server.overworld().getSeaLevel();
        look(player, level, new Vec3(river.getX() + 0.5, sea + 22, river.getZ() + 30.5), new Vec3(river.getX() + 0.5, sea, river.getZ()));
    }

    private static void look(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye.y + player.getEyeHeight() - target.y, Math.sqrt(dx * dx + dz * dz)));
        player.teleportTo(level, eye.x, eye.y, eye.z, yaw, pitch);
    }

    /** Biomkarten rund um die Bühne: heute, +100, +500, +1000 (ein Bildpunkt = zwölf Blöcke). */
    private static void maps(ServerPlayer player) {
        ServerLevel far = level(player, 1000);
        Climate.Sampler sampler = far.getChunkSource().randomState().sampler();
        int qy = QuartPos.fromBlock(200);
        int size = MAP_BLOCKS / MAP_STEP;
        Map<Holder<Biome>, Integer> colors = new HashMap<>();
        int[] years = {0, 100, 500, 1000};
        for (int era : years) {
            BiomeSource source = era == 0 ? ((FutureBiomeSource) far.getChunkSource().getGenerator().getBiomeSource()).base()
                : level(player, era).getChunkSource().getGenerator().getBiomeSource();
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int px = 0; px < size; px++) {
                for (int pz = 0; pz < size; pz++) {
                    int x = site.getX() - MAP_BLOCKS / 2 + px * MAP_STEP;
                    int z = site.getZ() - MAP_BLOCKS / 2 + pz * MAP_STEP;
                    Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z), sampler);
                    image.setRGB(px, pz, colors.computeIfAbsent(biome, ForestShowcase::color));
                }
            }
            try {
                Path file = DemoDirector.dir().toPath().resolve("demo-frames/maps/biome_" + era + ".png");
                Files.createDirectories(file.getParent());
                ImageIO.write(image, "png", file.toFile());
            } catch (IOException e) {
                LOGGER.error("Karte {} nicht gespeichert", era, e);
            }
        }
    }

    /** Kartenfarben wie in gängigen Biomkarten. */
    private static int color(Holder<Biome> biome) {
        String name = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
        return switch (name) {
            case "plains" -> 0x8DB360;
            case "sunflower_plains" -> 0xB5DB88;
            case "meadow" -> 0x83BB6D;
            case "forest" -> 0x056621;
            case "flower_forest" -> 0x2D8E49;
            case "birch_forest" -> 0x307444;
            case "old_growth_birch_forest" -> 0x589C6C;
            case "dark_forest" -> 0x40511A;
            case "taiga" -> 0x0B6659;
            case "old_growth_pine_taiga" -> 0x596651;
            case "old_growth_spruce_taiga" -> 0x818E79;
            case "snowy_taiga" -> 0x31554A;
            case "snowy_plains" -> 0xFFFFFF;
            case "ice_spikes" -> 0xB4DCDC;
            case "snowy_slopes" -> 0xC4C4C4;
            case "grove" -> 0x47726C;
            case "frozen_peaks" -> 0xA0A0FF;
            case "jagged_peaks" -> 0xDCDCDC;
            case "stony_peaks" -> 0x888888;
            case "jungle" -> 0x537B09;
            case "sparse_jungle" -> 0x628B17;
            case "bamboo_jungle" -> 0x768E14;
            case "savanna" -> 0xBDB25F;
            case "savanna_plateau" -> 0xA79D64;
            case "windswept_savanna" -> 0xE5DA87;
            case "desert" -> 0xFA9418;
            case "badlands" -> 0xD94515;
            case "eroded_badlands" -> 0xFF6D3D;
            case "wooded_badlands" -> 0xB09765;
            case "swamp" -> 0x07F9B2;
            case "mangrove_swamp" -> 0x2CCC8E;
            case "windswept_hills" -> 0x606060;
            case "windswept_gravelly_hills" -> 0x789878;
            case "windswept_forest" -> 0x507050;
            case "cherry_grove" -> 0xFFB7D5;
            case "mushroom_fields" -> 0xFF00FF;
            case "beach" -> 0xFADE55;
            case "snowy_beach" -> 0xFAF0C0;
            case "stony_shore" -> 0xA2A284;
            case "river" -> 0x0000FF;
            case "frozen_river" -> 0xA0A0FF;
            case "frozen_ocean", "deep_frozen_ocean" -> 0x7070D6;
            case "cold_ocean", "deep_cold_ocean" -> 0x202070;
            case "lukewarm_ocean", "deep_lukewarm_ocean" -> 0x000090;
            case "warm_ocean" -> 0x0000AC;
            case "deep_ocean" -> 0x000030;
            case "ocean" -> 0x000070;
            default -> 0x101010 | (name.hashCode() & 0xFFFFFF);
        };
    }
}

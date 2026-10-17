package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.town.Settlements;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vorführung „Dörfer wachsen“: dasselbe Dorf heute, +10, +100, +500 und +1000 Jahre – aus derselben Luftperspektive,
 * dazu Nahaufnahmen der Stadt (Tor, Markt, Uhrturm) und ein Dorf, in dessen Ruinen Plünderer lagern.
 */
final class TownShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("TIME-Demo");
    private static final int[] ERAS = {10, 100, 500, 1000};

    private static volatile ChunkPos town = new ChunkPos(0, 0);
    private static volatile ChunkPos raided;
    private static volatile int ground = 70;
    private static volatile BlockPos center = BlockPos.ZERO;

    private TownShowcase() {
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
            .client(mc -> mc.options.renderDistance().set(12))
            .wait(20)
            .server(TownShowcase::findVillages)
            .server(player -> aerial(player, player.server.overworld()))
            .wait(520).screenshot("t0_gegenwart");
        for (int i = 0; i < ERAS.length; i++) {
            int years = ERAS[i];
            d.server(player -> aerial(player, level(player, years)))
                .wait(i == 0 ? 620 : 520).screenshot(String.format("t%d_plus%d", i + 1, years));
        }
        // Nahaufnahmen: Kleinstadt von +100 und +500, dann die Stadt von +1000
        d.server(player -> street(player, level(player, 100), Math.PI * 0.25))
            .wait(360).screenshot("t5a_markt_plus100")
            .server(player -> street(player, level(player, 500), Math.PI * 0.25))
            .wait(360).screenshot("t5b_kleinstadt_plus500")
            .server(player -> street(player, level(player, 1000), 0.0))
            .wait(200).screenshot("t5_strasse_plus1000")
            .server(player -> street(player, level(player, 1000), Math.PI * 0.75))
            .wait(200).screenshot("t6_viertel_plus1000")
            .server(player -> {
                if (raided != null) {
                    ServerLevel level = level(player, 1000);
                    int y = level.getChunkSource().getGenerator().getBaseHeight(raided.getMiddleBlockX(), raided.getMiddleBlockZ(),
                        Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                    look(player, level, new Vec3(raided.getMiddleBlockX() + 22, y + 16, raided.getMiddleBlockZ() + 22),
                        new Vec3(raided.getMiddleBlockX(), y + 2, raided.getMiddleBlockZ()));
                }
            })
            .wait(520).screenshot("t7_pluenderer_plus1000")
            .wait(5).quit();
    }

    private static ServerLevel level(ServerPlayer player, int years) {
        return player.server.getLevel(Eras.byYears(years).level());
    }

    /** Das nächste Dorf, das bis +1000 zur Stadt wächst (und eines, in dem Plünderer lagern). */
    private static void findVillages(ServerPlayer player) {
        ServerLevel present = player.server.overworld();
        var structures = present.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> villages = structures.getTag(StructureTags.VILLAGE).orElseThrow();
        long seed = present.getSeed();
        ChunkPos best = null;
        for (int i = 0; i < 24 && (best == null || raided == null); i++) {
            double angle = i * 2.399963;
            BlockPos from = new BlockPos((int) (Math.cos(angle) * (300 + i * 260)), 64, (int) (Math.sin(angle) * (300 + i * 260)));
            Pair<BlockPos, Holder<Structure>> found = present.getChunkSource().getGenerator().findNearestMapStructure(present, villages, from, 8, false);
            if (found == null) {
                continue;
            }
            ChunkPos chunk = new ChunkPos(found.getFirst());
            Settlements.Outlook outlook = Settlements.outlook(seed, chunk, 1000);
            LOGGER.info("Dorf {} ({}): {}", chunk, found.getSecond().unwrapKey().map(k -> k.location().getPath()).orElse("?"), outlook);
            boolean plains = found.getSecond().is(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
                new net.minecraft.resources.ResourceLocation("village_plains")));
            if (best == null && outlook.fate() == Settlements.Fate.THRIVING && outlook.stage() == 4 && plains) {
                best = chunk;
            }
            if (raided == null && outlook.fate() == Settlements.Fate.RAIDED) {
                raided = chunk;
            }
        }
        town = best == null ? new ChunkPos(0, 0) : best;
        // Die Mitte des Dorfes: der Dorfplatz, mit dem Minecraft das Dorf angelegt hat
        var chunk = present.getChunk(town.x, town.z, net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS);
        center = new BlockPos(town.getMiddleBlockX(), 0, town.getMiddleBlockZ());
        for (var start : chunk.getAllStarts().values()) {
            if (start.isValid() && !start.getPieces().isEmpty()) {
                center = start.getPieces().get(0).getBoundingBox().getCenter();
            }
        }
        ground = present.getChunkSource().getGenerator().getBaseHeight(center.getX(), center.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
            present, present.getChunkSource().randomState());
        LOGGER.info("Stadt der Zukunft: {} Mitte {} (Boden {}), Plünderer: {}", town, center, ground, raided);
    }

    /** Hoch über dem Dorf, schräg nach unten auf die Mitte. */
    private static void aerial(ServerPlayer player, ServerLevel level) {
        Vec3 target = new Vec3(center.getX(), ground, center.getZ());
        look(player, level, target.add(0, 96, 132), target);
    }

    /** Auf Augenhöhe in einer Straße der Stadt, Blick zur Mitte. */
    private static void street(ServerPlayer player, ServerLevel level, double angle) {
        double radius = 64;
        int x = (int) (center.getX() + Math.cos(angle) * radius);
        int z = (int) (center.getZ() + Math.sin(angle) * radius);
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        look(player, level, new Vec3(x + 0.5, y + 9, z + 0.5), new Vec3(center.getX(), y + 2, center.getZ()));
    }

    private static void look(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye.y + player.getEyeHeight() - target.y, Math.sqrt(dx * dx + dz * dz)));
        player.teleportTo(level, eye.x, eye.y, eye.z, yaw, pitch);
    }
}

package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.TimeMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

/** Vorführung der Strukturen: Uhrturm, Observatorium, Chrono-Ruinen und Zeitlose Werkstatt. */
final class StructureShowcase {
    private static final Map<String, BoundingBox> PLACED = new HashMap<>();
    private static final Map<String, BlockPos> FOUND = new HashMap<>();

    private StructureShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("gamerule sendCommandFeedback false")
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 5000")
            .command("effect give @a night_vision infinite 0 true");
        surface(d, "clock_tower", 0, 0, 30.0, 14.0, "s1_uhrturm");
        surface(d, "observatory", 3000, 3000, 26.0, 12.0, "s2_observatorium");
        inside(d, "chrono_ruins", 1200, 0, new Vec3(0.5, 5.5, -9.5), 18.0F, "s3_chrono_ruinen");
        inside(d, "timeless_workshop", 1800, 0, new Vec3(0.5, 6.2, -6.5), 30.0F, "s4_werkstatt");
        d.wait(5).quit();
    }

    /**
     * Oberflächenstruktur: dort suchen, wo die Welt sie von selbst erzeugt (/locate), hinfliegen,
     * Chunks laden lassen und dann schräg von vorn-oben ansehen.
     */
    private static void surface(DemoDirector d, String name, int x, int z, double distance, double height, String shot) {
        d.server(player -> {
            ServerLevel level = player.serverLevel();
            Holder<Structure> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, TimeMod.id(name)));
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), new BlockPos(x, 64, z), 12, false);
            if (found != null) {
                BlockPos pos = found.getFirst();
                FOUND.put(name, pos);
                TimeMod.LOGGER.info("Demo: {} natürlich gefunden bei {}", name, pos);
                player.teleportTo(level, pos.getX() + 8, 260, pos.getZ() + 8, 0, 90);
            } else {
                TimeMod.LOGGER.warn("Demo: {} nicht gefunden – wird erzwungen", name);
            }
            TimeMod.LOGGER.info("Demo: Ablehnungen {}", com.fischgesichtert.timemod.worldgen.TimeStructure.REJECTED);
            com.fischgesichtert.timemod.worldgen.TimeStructure.REJECTED.clear();
        }).wait(160).server(player -> {
            ServerLevel level = player.serverLevel();
            BoundingBox box = null;
            BlockPos pos = FOUND.get(name);
            if (pos != null) {
                Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TimeMod.id(name));
                StructureStart start = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getStartForStructure(structure);
                if (start != null && start.isValid()) {
                    box = start.getBoundingBox();
                }
            }
            if (box == null) {
                box = placeNear(level, name, new BlockPos(x, 0, z), 200);
            }
            if (box != null) {
                PLACED.put(name, box);
                clearVegetation(level, box.inflatedBy(18), box);
                Vec3 c = box.getCenter().getCenter();
                lookAt(player, c.add(distance * 0.7, height, -distance * 0.7), c.add(0, -2, 0));
            }
        }).wait(100).screenshot(shot).server(player -> {
            BoundingBox box = PLACED.get(name);
            if (box != null) {
                Vec3 c = box.getCenter().getCenter();
                lookAt(player, c.add(-distance * 0.9, height * 0.4, distance * 0.3), c.add(0, 1, 0));
            }
        }).wait(60).screenshot(shot + "_b");
    }

    /** Unterirdische Struktur: Kamera in den Innenraum. */
    private static void inside(DemoDirector d, String name, int x, int z, Vec3 eye, float pitch, String shot) {
        d.command("tp @a " + x + " 0 " + z).wait(40).server(player -> {
            BoundingBox box = placeNear(player.serverLevel(), name, new BlockPos(x, 0, z), 1);
            if (box != null) {
                PLACED.put(name, box);
                Vec3 c = new Vec3(box.getCenter().getX(), box.minY(), box.getCenter().getZ());
                lookAt(player, c.add(eye), c.add(0, 1.5, 0));
            }
        }).wait(80).screenshot(shot);
    }

    /** Für freie Sicht: Bäume, Laub und Ranken rund um die Struktur entfernen (nur in der Vorführung). */
    private static void clearVegetation(ServerLevel level, BoundingBox area, BoundingBox keep) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                for (int y = area.minY(); y <= area.maxY() + 20; y++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.VINE) || state.is(Blocks.BAMBOO)
                        || state.is(Blocks.COCOA) || state.is(Blocks.MELON)) {
                        // Die Ranken des Turms selbst bleiben
                        if (!(state.is(Blocks.VINE) && keep.isInside(pos))) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                        }
                    }
                }
            }
        }
    }

    private static void lookAt(ServerPlayer player, Vec3 from, Vec3 target) {
        Vec3 dir = target.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        player.teleportTo(player.serverLevel(), from.x, from.y - player.getEyeHeight(), from.z, yaw, pitch);
    }

    /** Wie /place structure, aber mit Suche nach einem gültigen Chunk und mit Rückgabe der Bounding Box. */
    static BoundingBox placeNear(ServerLevel level, String name, BlockPos start, int attempts) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TimeMod.id(name));
        if (structure == null) {
            return null;
        }
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        for (int i = 0; i < attempts; i++) {
            ChunkPos chunk = new ChunkPos(new BlockPos(start.getX() + (i % 15) * 96, 0, start.getZ() + (i / 15) * 96));
            StructureStart s = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureManager(), level.getSeed(), chunk, 0, level, h -> true);
            if (!s.isValid()) {
                continue;
            }
            BoundingBox box = s.getBoundingBox();
            ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
            ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
            ChunkPos.rangeClosed(min, max).forEach(cp -> {
                level.getChunk(cp.x, cp.z);
                s.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                    new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(), cp.getMaxBlockX(), level.getMaxBuildHeight(), cp.getMaxBlockZ()), cp);
            });
            return box;
        }
        return null;
    }
}

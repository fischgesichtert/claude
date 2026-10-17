package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.era.Eras;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** Rundflug durch die Epochen: dieselbe Welt in der Gegenwart, in der Eiszeit und in der Urzeit. */
final class EraShowcase {
    private static final float YAW = 135.0F;
    private static final float PITCH = 18.0F;

    private EraShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 5000")
            .client(mc -> mc.options.renderDistance().set(10));
        view(d, 0, "minecraft:forest", "e00_gegenwart");
        view(d, -1000, "minecraft:dark_forest", "e01_fruehmittelalter");
        view(d, -20_000, "minecraft:ice_spikes", "e02_eiszeit_eisspitzen");
        view(d, -20_000, "minecraft:snowy_taiga", "e03_eiszeit_taiga");
        view(d, -1_000_000, "timemod:mammoth_steppe", "e04_pleistozaen_steppe");
        view(d, -10_000_000, "timemod:miocene_savanna", "e05_miozaen_savanne");
        view(d, -70_000_000, "timemod:fern_jungle", "e06_kreide_farnwald");
        view(d, -100_000_000, "timemod:primeval_swamp", "e07_kreide_sumpf");
        view(d, -150_000_000, "timemod:conifer_forest", "e08_jura_nadelwald");
        view(d, -200_000_000, "timemod:volcanic_wastes", "e09_trias_vulkanfeld");
        view(d, -245_000_000, "timemod:pangaea_desert", "e10_pangaea_wueste");
        d.wait(5).quit();
    }

    /** Sucht an Land ein großes Stück des Bioms und schaut aus einiger Entfernung schräg darauf. */
    private static void view(DemoDirector d, int years, String biome, String shot) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, new ResourceLocation(biome));
        d.server(player -> {
            ServerLevel level = player.server.getLevel(Eras.byYears(years).level());
            BlockPos target = findLand(level, key);
            double dx = Mth.sin(YAW * Mth.DEG_TO_RAD) * 56;
            double dz = -Mth.cos(YAW * Mth.DEG_TO_RAD) * 56;
            int camX = Mth.floor(target.getX() + dx);
            int camZ = Mth.floor(target.getZ() + dz);
            int top = level.getSeaLevel();
            for (int i = 0; i <= 4; i++) {
                int x = Mth.floor(target.getX() + dx * i / 4);
                int z = Mth.floor(target.getZ() + dz * i / 4);
                level.getChunk(x >> 4, z >> 4);
                top = Math.max(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
            }
            player.teleportTo(level, camX + 0.5, top + 22, camZ + 0.5, YAW, PITCH);
        }).wait(340).screenshot(shot);
    }

    /** Spiralförmig nach außen: Landstelle, an der das Biom auch ringsum liegt. */
    private static BlockPos findLand(ServerLevel level, ResourceKey<Biome> biome) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int sea = generator.getSeaLevel();
        for (BlockPos.MutableBlockPos step : BlockPos.spiralAround(BlockPos.ZERO, 60, Direction.EAST, Direction.SOUTH)) {
            int x = step.getX() * 48;
            int z = step.getZ() * 48;
            if (isLandOf(level, generator, random, sea, biome, x, z) && isLandOf(level, generator, random, sea, biome, x + 40, z)
                && isLandOf(level, generator, random, sea, biome, x - 40, z) && isLandOf(level, generator, random, sea, biome, x, z + 40)
                && isLandOf(level, generator, random, sea, biome, x, z - 40)) {
                return new BlockPos(x, sea, z);
            }
        }
        return BlockPos.ZERO;
    }

    private static boolean isLandOf(ServerLevel level, ChunkGenerator generator, RandomState random, int sea, ResourceKey<Biome> biome, int x, int z) {
        int y = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
        if (y <= sea) {
            return false;
        }
        return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler()).is(biome);
    }
}

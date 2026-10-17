package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.future.Curve;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

/**
 * Was aus einem Dorf der Gegenwart in der Zukunft wird – nur aus Seed, Lage und Jahr: Es wächst (bis zur
 * Stadt), bleibt klein, verfällt zur Ruine oder wird von Plünderern besetzt. Wer einmal verfallen ist,
 * bleibt es; wer wächst, wächst Stufe für Stufe weiter.
 */
public final class Settlements {
    /** Anteil der Dörfer, die bis zu einem Jahr untergegangen sind. */
    private static final Curve RUINED = curve("10", 0.03, "50", 0.08, "100", 0.12, "250", 0.17, "500", 0.22, "1000", 0.26);
    /** Von den untergegangenen: wie viele Plünderer besetzt halten (erst nach einer Weile). */
    private static final double RAIDED = 0.45;
    private static final int RAIDED_FROM = 25;
    /** So viele Dörfer bleiben klein, egal wie viel Zeit vergeht. */
    private static final double STAGNANT = 0.15;
    /** Rund um eine lebende Siedlung bleibt das Land offen: Felder, Weiden, Gärten (Radius, dann ausklingend). */
    public static final int CLEARING = 104;
    private static final int CLEARING_FADE = 24;

    private static volatile long worldSeed;
    @Nullable
    private static volatile RandomSpreadStructurePlacement villages;
    private static final Map<Long, Optional<ChunkPos>> SITES = new ConcurrentHashMap<>();

    public enum Fate {
        /** Gewachsen – je nach Jahr bis zur Stadt. */
        THRIVING,
        /** Geblieben, wie es war (höchstens ein paar neue Häuser). */
        STAGNANT,
        /** Verlassen und verfallen. */
        RUINED,
        /** Verfallen, und in den Ruinen lagern Plünderer. */
        RAIDED;

        public boolean alive() {
            return this == THRIVING || this == STAGNANT;
        }
    }

    /**
     * Das Schicksal eines Dorfes in einer Zukunft.
     *
     * @param stage 0 wie heute, 1 ein paar neue Häuser und Felder, 2 Steinstraßen, Mauer, Markt und Kirche,
     *              3 Kleinstadt mit Türmen und Mühlen, 4 Stadt mit Bibliothek, Uhrturm und großer Mauer – bei
     *              untergegangenen Dörfern die Stufe, die sie vor dem Untergang erreicht hatten
     * @param since bei untergegangenen Dörfern das letzte Jahr, in dem sie noch lebten (0: schon im ersten
     *              Jahrzehnt verlassen); sonst das Jahr selbst
     */
    public record Outlook(Fate fate, int stage, int since) {
        public Outlook(Fate fate, int stage) {
            this(fate, stage, 0);
        }

        public boolean alive() {
            return fate.alive();
        }

        /** Wie lange die Siedlung schon verlassen ist (für die Verwitterung ihrer Ruinen). */
        public long abandonedFor(long years) {
            return alive() ? 0 : Math.max(10, years - since);
        }
    }

    /** Die Jahre der Zukunfts-Epochen, aufsteigend (wann ein Dorf untergegangen ist, zählt nur in diesen Schritten). */
    private static final int[] ERAS = com.fischgesichtert.timemod.era.Eras.all().stream()
        .filter(com.fischgesichtert.timemod.era.Era::isFuture).mapToInt(com.fischgesichtert.timemod.era.Era::years).sorted().toArray();

    private Settlements() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            worldSeed = server.getWorldData().worldGenOptions().seed();
            villages = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getOptional(BuiltinStructureSets.VILLAGES)
                .map(set -> set.placement() instanceof RandomSpreadStructurePlacement spread ? spread : null).orElse(null);
            SITES.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            villages = null;
            SITES.clear();
        });
    }

    public static Outlook outlook(long seed, ChunkPos start, long years) {
        if (years <= 0) {
            return new Outlook(Fate.THRIVING, 0, 0);
        }
        double fall = roll(seed, start, 1);
        boolean stagnant = roll(seed, start, 3) < STAGNANT;
        if (fall < RUINED.at(years)) {
            // Bis zur letzten Epoche vor dem Untergang ist das Dorf gewachsen – seine Ruinen haben diese Größe
            int since = 0;
            for (int era : ERAS) {
                if (era > years || fall < RUINED.at(era)) {
                    break;
                }
                since = era;
            }
            int stage = stagnant ? Math.min(stage(since), 1) : stage(since);
            boolean raided = years >= RAIDED_FROM && roll(seed, start, 2) < RAIDED;
            return new Outlook(raided ? Fate.RAIDED : Fate.RUINED, stage, since);
        }
        int stage = stage(years);
        if (stagnant) {
            return new Outlook(Fate.STAGNANT, Math.min(stage, 1), (int) years);
        }
        return new Outlook(Fate.THRIVING, stage, (int) years);
    }

    /** Die Wachstumsstufe eines blühenden Dorfes nach so vielen Jahren. */
    public static int stage(long years) {
        if (years < 10) {
            return 0;
        }
        if (years < 100) {
            return 1;
        }
        if (years < 500) {
            return 2;
        }
        return years < 1000 ? 3 : 4;
    }

    /**
     * Wie sehr eine Stelle zum offenen Land einer lebenden Siedlung gehört: 1 mitten darin, 0 weit weg. Die
     * Zukunfts-Biome lassen dort keinen Wald wachsen. Wo Dörfer stehen, ergibt sich aus der Platzierung der
     * Dörfer und dem Biom von heute (wie bei Minecraft selbst).
     */
    public static double clearing(int blockX, int blockZ, long years, MultiNoiseBiomeSource today, Climate.Sampler sampler) {
        RandomSpreadStructurePlacement placement = villages;
        if (placement == null || years < 10) {
            return 0.0;
        }
        int spacing = placement.spacing();
        int cellX = Math.floorDiv(blockX >> 4, spacing);
        int cellZ = Math.floorDiv(blockZ >> 4, spacing);
        double best = 0.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos site = site(placement, cellX + dx, cellZ + dz, today, sampler);
                if (site == null || !outlook(worldSeed, site, years).alive()) {
                    continue;
                }
                double distance = Math.sqrt(Mth.square((double) blockX - site.getMiddleBlockX()) + Mth.square((double) blockZ - site.getMiddleBlockZ()));
                best = Math.max(best, Mth.clamp((CLEARING + CLEARING_FADE - distance) / CLEARING_FADE, 0.0, 1.0));
            }
        }
        return best;
    }

    /** Der Dorf-Chunk einer Platzierungs-Zelle – sofern das Biom von heute dort ein Dorf erlaubt. */
    @Nullable
    private static ChunkPos site(RandomSpreadStructurePlacement placement, int cellX, int cellZ, MultiNoiseBiomeSource today,
                                 Climate.Sampler sampler) {
        long key = ChunkPos.asLong(cellX, cellZ);
        Optional<ChunkPos> known = SITES.get(key);
        if (known == null) {
            ChunkPos chunk = placement.getPotentialStructureChunk(worldSeed, cellX * placement.spacing(), cellZ * placement.spacing());
            // Hoch über dem Gelände gilt das Biom der Oberfläche
            Holder<Biome> biome = today.getNoiseBiome(sampler.sample(QuartPos.fromBlock(chunk.getMiddleBlockX()), QuartPos.fromBlock(200),
                QuartPos.fromBlock(chunk.getMiddleBlockZ())));
            boolean village = biome.is(BiomeTags.HAS_VILLAGE_PLAINS) || biome.is(BiomeTags.HAS_VILLAGE_DESERT) || biome.is(BiomeTags.HAS_VILLAGE_SAVANNA)
                || biome.is(BiomeTags.HAS_VILLAGE_SNOWY) || biome.is(BiomeTags.HAS_VILLAGE_TAIGA);
            known = village ? Optional.of(chunk) : Optional.empty();
            SITES.put(key, known);
        }
        return known.orElse(null);
    }

    /**
     * Wie Siedlungen einen Chunk der Zukunft betreffen: Gehört er zu einem lebenden Dorf (dann wird hier gepflegt,
     * nicht verfallen) – und welche Plünderer-Lager stehen hier (die bleiben, wie sie sind).
     */
    public record Presence(boolean living, @Nullable TownStyle style, List<BoundingBox> camps, boolean abandoned, long abandonedFor) {
        /** Wie viele Jahre hier verwittern: bei Ruinen einer Siedlung nur die Zeit seit ihrem Untergang. */
        public long decay(long years) {
            return abandoned ? Math.min(years, abandonedFor) : years;
        }
    }

    public static Presence presence(WorldGenLevel level, ChunkAccess chunk, long years) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        boolean living = false;
        boolean abandoned = false;
        long abandonedFor = Long.MAX_VALUE;
        TownStyle style = null;
        List<BoundingBox> camps = new ArrayList<>();
        for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
            Holder<Structure> holder = registry.wrapAsHolder(entry.getKey());
            boolean village = holder.is(StructureTags.VILLAGE);
            boolean town = holder.is(TownPieces.TOWN);
            if (!village && !town) {
                continue;
            }
            for (long start : entry.getValue()) {
                ChunkPos pos = new ChunkPos(start);
                if (village) {
                    Outlook outlook = outlook(level.getSeed(), pos, years);
                    if (!outlook.alive()) {
                        abandoned = true;
                        abandonedFor = Math.min(abandonedFor, outlook.abandonedFor(years));
                    }
                    if (outlook.alive()) {
                        living = true;
                        ResourceLocation id = registry.getKey(entry.getKey());
                        TownStyle candidate = TownStyle.of(TownStyle.typeOf(id == null ? "" : id.toString()), outlook.stage());
                        if (style == null || candidate.stage() > style.stage()) {
                            style = candidate;
                        }
                    }
                    continue;
                }
                ChunkAccess origin = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, false);
                StructureStart found = origin == null ? null : origin.getStartForStructure(entry.getKey());
                if (found != null) {
                    for (StructurePiece piece : found.getPieces()) {
                        if (piece instanceof CampPiece) {
                            camps.add(piece.getBoundingBox());
                        }
                    }
                }
            }
        }
        return new Presence(living, style, camps, abandoned && !living, abandonedFor);
    }

    /** In einer lebenden Siedlung: ab Stufe 2 sind die alten Trampelpfade gepflastert, in der Stadt brennen Laternen. */
    public static void renovate(WorldGenLevel level, ChunkPos chunk, Presence presence) {
        TownStyle style = presence.style();
        if (style == null || style.stage() < 2) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                for (int y = top; y >= top - 2; y--) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.is(Blocks.DIRT_PATH)) {
                        boolean accent = roll(level.getSeed(), new ChunkPos(x, z), y) < 0.25;
                        level.setBlock(pos, (accent ? style.roadAccent() : style.road()).defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (style.city() && state.is(Blocks.TORCH)) {
                        level.setBlock(pos, Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Ob die Stelle in einer Stadt mit Mauer liegt (gewachsene Siedlung ab Stufe 2). */
    public static boolean inCity(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        Structure town = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TownPieces.TOWN);
        if (town == null) {
            return false;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, town);
        return start.isValid() && start.getPieces().stream().anyMatch(WallPiece.class::isInstance);
    }

    /** Ein fester Zufall je Dorf und Zweck in [0, 1). */
    static double roll(long seed, ChunkPos start, int salt) {
        long h = seed ^ 0x5E77_1E5EL;
        h ^= start.x * 0x9E3779B97F4A7C15L;
        h ^= start.z * 0xC2B2AE3D27D4EB4FL;
        h ^= salt * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    private static Curve curve(Object... points) {
        JsonObject json = new JsonObject();
        for (int i = 0; i < points.length; i += 2) {
            json.addProperty((String) points[i], (Double) points[i + 1]);
        }
        return Curve.parse(json);
    }
}

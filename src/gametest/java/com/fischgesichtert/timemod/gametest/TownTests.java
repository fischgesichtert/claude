package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.town.BuildingPiece;
import com.fischgesichtert.timemod.future.town.CampPiece;
import com.fischgesichtert.timemod.future.town.FieldPiece;
import com.fischgesichtert.timemod.future.town.PlazaPiece;
import com.fischgesichtert.timemod.future.town.RoadPiece;
import com.fischgesichtert.timemod.future.town.Settlements;
import com.fischgesichtert.timemod.future.town.TownGrowth;
import com.fischgesichtert.timemod.future.town.TownPieces;
import com.fischgesichtert.timemod.future.town.TownPlanner;
import com.fischgesichtert.timemod.future.town.TownStyle;
import com.fischgesichtert.timemod.future.town.WallPiece;
import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/** Dörfer der Zukunft: Schicksal, Lage, Wachstum bis zur Stadt. */
public class TownTests implements FabricGameTest {
    private static final int[] FUTURE = {1, 10, 25, 50, 100, 250, 500, 1000};

    private static ServerLevel era(GameTestHelper helper, int years) {
        return helper.getLevel().getServer().getLevel(Eras.byYears(years).level());
    }

    /** Wer untergeht, bleibt untergegangen; wer lebt, wächst nur weiter; die Anteile sind plausibel. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void villageFatesAreStable(GameTestHelper helper) {
        long seed = helper.getLevel().getSeed();
        int ruined = 0;
        int raided = 0;
        int stagnant = 0;
        int samples = 3000;
        for (int i = 0; i < samples; i++) {
            ChunkPos start = new ChunkPos(i * 37 - 50_000, i * 53 % 7919 - 4000);
            boolean fallen = false;
            int stage = 0;
            for (int years : FUTURE) {
                Settlements.Outlook outlook = Settlements.outlook(seed, start, years);
                if (fallen && outlook.alive()) {
                    helper.fail("Ein verfallenes Dorf lebt im Jahr +" + years + " wieder: " + start);
                }
                if (outlook.alive() && outlook.stage() < stage) {
                    helper.fail("Ein Dorf schrumpft im Jahr +" + years + ": " + start);
                }
                fallen |= !outlook.alive();
                stage = outlook.alive() ? outlook.stage() : stage;
            }
            Settlements.Outlook far = Settlements.outlook(seed, start, 1000);
            ruined += far.alive() ? 0 : 1;
            raided += far.fate() == Settlements.Fate.RAIDED ? 1 : 0;
            stagnant += far.fate() == Settlements.Fate.STAGNANT ? 1 : 0;
        }
        TimeMod.LOGGER.info(String.format(Locale.ROOT, "Dörfer nach 1000 Jahren: %d von %d untergegangen (%d von Plünderern besetzt), %d klein geblieben",
            ruined, samples, raided, stagnant));
        if (ruined < samples * 0.2 || ruined > samples * 0.32 || raided < ruined * 0.35 || raided > ruined * 0.55 || stagnant < samples * 0.08) {
            helper.fail("Unplausible Anteile: " + ruined + " untergegangen, " + raided + " Plünderer, " + stagnant + " klein");
        }
        if (Settlements.stage(5) != 0 || Settlements.stage(10) != 1 || Settlements.stage(100) != 2 || Settlements.stage(500) != 3
            || Settlements.stage(1000) != 4) {
            helper.fail("Wachstumsstufen passen nicht zu den Epochen");
        }
        helper.succeed();
    }

    /**
     * Ein Dorf steht bei +1000 genau dort und genau so wie heute – trotz wärmerem, waldreicherem Klima – und wächst
     * dann je nach Schicksal. (Die Testwelt erzeugt selbst keine Bauwerke, deshalb wird das Dorf hier direkt angelegt.)
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void futureVillagesStandWhereTheyStandToday(GameTestHelper helper) {
        ServerLevel soon = era(helper, 1);
        ServerLevel far = era(helper, 1000);
        var access = far.registryAccess();
        var structures = access.registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> villages = structures.getTag(StructureTags.VILLAGE).orElseThrow();
        var state = far.getChunkSource().getGeneratorState();
        var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) state
            .getPlacementsForStructure(villages.get(0)).get(0);
        var templates = far.getServer().getStructureManager();
        long seed = far.getSeed();
        int checked = 0;
        int climateDiffers = 0;
        for (int i = 0; i < 80 && checked < 6; i++) {
            ChunkPos chunk = placement.getPotentialStructureChunk(seed, (i % 10) * 34 - 170, (i / 10) * 34 - 136);
            for (Holder<Structure> village : villages) {
                Structure structure = village.value();
                StructureStart today = structure.generate(access, soon.getChunkSource().getGenerator(), soon.getChunkSource().getGenerator()
                    .getBiomeSource(), soon.getChunkSource().randomState(), templates, seed, chunk, 0, soon, structure.biomes()::contains);
                if (!today.isValid()) {
                    continue;
                }
                StructureStart then = structure.generate(access, far.getChunkSource().getGenerator(), far.getChunkSource().getGenerator()
                    .getBiomeSource(), far.getChunkSource().randomState(), templates, seed, chunk, 0, far, structure.biomes()::contains);
                if (!then.isValid() || then.getPieces().size() != today.getPieces().size()) {
                    helper.fail("Das Dorf bei " + chunk + " steht in der Zukunft nicht (oder anders): " + then.getPieces().size() + " statt "
                        + today.getPieces().size() + " Teile");
                    return;
                }
                for (int p = 0; p < today.getPieces().size(); p++) {
                    if (!today.getPieces().get(p).getBoundingBox().equals(then.getPieces().get(p).getBoundingBox())) {
                        helper.fail("Der alte Dorfkern bei " + chunk + " unterscheidet sich: Teil " + p);
                    }
                }
                BlockPos core = today.getPieces().get(0).getBoundingBox().getCenter();
                var source = (FutureBiomeSource) far.getChunkSource().getGenerator().getBiomeSource();
                var sampler = far.getChunkSource().randomState().sampler();
                if (source.getNoiseBiome(core.getX() >> 2, core.getY() >> 2, core.getZ() >> 2, sampler)
                    != source.base().getNoiseBiome(core.getX() >> 2, core.getY() >> 2, core.getZ() >> 2, sampler)) {
                    climateDiffers++;
                }
                checkGrowth(helper, far, structure, then, chunk, seed);
                checked++;
                break;
            }
        }
        TimeMod.LOGGER.info("{} Dörfer geprüft, an {} davon ist das Klima der Zukunft ein anderes", checked, climateDiffers);
        if (checked < 3) {
            helper.fail("Zu wenige Dörfer gefunden: " + checked);
        }
        helper.succeed();
    }

    private static void checkGrowth(GameTestHelper helper, ServerLevel far, Structure structure, StructureStart start, ChunkPos chunk, long seed) {
        TownGrowth.Growth growth = TownGrowth.plan(start, structure, 1000, far.registryAccess(), far.getChunkSource().getGenerator(),
            far.getChunkSource().randomState(), far, seed);
        Settlements.Outlook outlook = Settlements.outlook(seed, chunk, 1000);
        int houses = growth.village().getPieces().size() - start.getPieces().size();
        List<StructurePiece> town = growth.town() == null ? List.of() : growth.town().getPieces();
        boolean camp = town.stream().anyMatch(CampPiece.class::isInstance);
        boolean abandoned = growth.village().getPieces().stream().skip(start.getPieces().size())
            .allMatch(piece -> ((BuildingPiece) piece).isAbandoned());
        TimeMod.LOGGER.info("Dorf {} bei +1000: {} – {} neue Häuser, {} Stadtteile, Lager {}, Kern {} Teile", chunk, outlook, houses, town.size(), camp,
            start.getPieces().size());
        TownPlanner.Plan plan = TownPlanner.plan(start.getPieces(), chunk, outlook, TownStyle.of("plains", Math.max(1, outlook.stage())),
            far.getChunkSource().getGenerator(), far.getChunkSource().randomState(), far, seed);
        TimeMod.LOGGER.info("  abgelehnt: {}", plan.rejected());
        switch (outlook.fate()) {
            case RUINED, RAIDED -> {
                if (outlook.stage() > 0 && (houses == 0 || !abandoned)) {
                    helper.fail("Ein untergegangenes Dorf hinterlässt die Ruinen dessen, was es geworden war: " + outlook + ", " + houses);
                }
                if (camp != (outlook.fate() == Settlements.Fate.RAIDED)) {
                    helper.fail("Plünderer lagern genau in besetzten Dörfern: " + outlook);
                }
            }
            default -> {
                if (houses <= 0 || town.isEmpty() || camp || (houses > 0 && abandoned)) {
                    helper.fail("Ein lebendes Dorf wächst und ist bewohnt: " + outlook + ", " + houses + " neue Häuser");
                }
            }
        }
        // Rund um ein lebendes Dorf bleibt das Land offen
        FutureBiomeSource source = (FutureBiomeSource) far.getChunkSource().getGenerator().getBiomeSource();
        double clearing = Settlements.clearing(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 1000, (MultiNoiseBiomeSource) source.base(),
            far.getChunkSource().randomState().sampler());
        if (outlook.alive() && clearing < 0.99) {
            TimeMod.LOGGER.warn("Siedlungsland fehlt bei {}: {}", chunk, clearing);
        }
        if (!outlook.alive() && clearing > 0.0) {
            helper.fail("Um eine Ruine wächst der Wald: " + outlook + ", offen " + clearing);
        }
    }

    /** Auf flachem Land wächst ein Dorf in tausend Jahren zur Stadt – mit allem, was dazugehört. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void villagesGrowIntoCities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(9000, 0, 9000);
        level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX(), origin.getZ());
        ChunkPos start = new ChunkPos(origin);
        // Ein kleiner Dorfkern als Ausgangspunkt (wie der Brunnenplatz eines Dorfes)
        List<StructurePiece> core = List.of(new FieldPiece(origin.getX() - 10, ground, origin.getZ() - 10, 21, 21, 1L, TownStyle.of("plains", 1)));
        RandomSource random = RandomSource.create(5);
        for (int stage : new int[] {1, 2, 4}) {
            TownPlanner.Plan plan = TownPlanner.plan(core, start, new Settlements.Outlook(Settlements.Fate.THRIVING, stage), TownStyle.of("plains", stage),
                level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, 1234L + stage);
            EnumSet<BuildingPiece.Kind> kinds = EnumSet.noneOf(BuildingPiece.Kind.class);
            plan.buildings().forEach(piece -> kinds.add(((BuildingPiece) piece).kind()));
            long walls = plan.town().stream().filter(WallPiece.class::isInstance).count();
            long roads = plan.town().stream().filter(RoadPiece.class::isInstance).count();
            long fields = plan.town().stream().filter(FieldPiece.class::isInstance).count();
            long plazas = plan.town().stream().filter(PlazaPiece.class::isInstance).count();
            TimeMod.LOGGER.info("Stufe {}: {} Gebäude {}, {} Straßen, {} Mauern, {} Felder, {} Plätze", stage, plan.buildings().size(), kinds, roads,
                walls, fields, plazas);
            switch (stage) {
                case 1 -> {
                    if (plan.buildings().size() != 5 || walls != 0 || fields < 2 || plazas != 0) {
                        helper.fail("Stufe 1: ein paar neue Häuser und Felder, keine Mauer – " + plan.buildings().size() + " Häuser");
                    }
                }
                case 2 -> {
                    if (plan.buildings().size() < 10 || walls != 1 || plazas != 1 || !kinds.contains(BuildingPiece.Kind.CHURCH)) {
                        helper.fail("Stufe 2: Mauer, Marktplatz, Kirche – " + kinds + ", " + walls + " Mauern, " + plazas + " Plätze");
                    }
                }
                default -> {
                    if (plan.buildings().size() < 16 || walls != 1 || !kinds.containsAll(Set.of(BuildingPiece.Kind.CHURCH, BuildingPiece.Kind.LIBRARY,
                        BuildingPiece.Kind.TOWN_HALL, BuildingPiece.Kind.CLOCK_TOWER, BuildingPiece.Kind.TAVERN))) {
                        helper.fail("Stufe 4: Stadt mit Bibliothek, Rathaus, Uhrturm, Schenke – " + plan.buildings().size() + " Gebäude " + kinds);
                    }
                    build(level, plan);
                    checkCity(helper, level, origin, plan);
                }
            }
            // Kein Gebäude überlappt ein anderes oder eine Straße
            List<BoundingBox> lots = new ArrayList<>();
            for (StructurePiece piece : plan.buildings()) {
                BoundingBox box = piece.getBoundingBox();
                for (BoundingBox other : lots) {
                    if (box.intersects(other.minX(), other.minZ(), other.maxX(), other.maxZ())) {
                        helper.fail("Grundstücke überlappen: " + box + " / " + other);
                    }
                }
                lots.add(box);
            }
        }
        helper.succeed();
    }

    /** Baut alle Teile Chunk für Chunk, wie die Welterzeugung es täte. */
    private static void build(ServerLevel level, TownPlanner.Plan plan) {
        List<StructurePiece> all = new ArrayList<>(plan.town());
        all.addAll(plan.buildings());
        BoundingBox total = null;
        for (StructurePiece piece : all) {
            total = total == null ? piece.getBoundingBox() : BoundingBox.encapsulatingBoxes(List.of(total, piece.getBoundingBox())).orElse(total);
        }
        RandomSource random = RandomSource.create(9);
        for (int cx = total.minX() >> 4; cx <= total.maxX() >> 4; cx++) {
            for (int cz = total.minZ() >> 4; cz <= total.maxZ() >> 4; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                level.getChunk(cx, cz);
                BoundingBox box = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight() + 1, chunk.getMinBlockZ(), chunk.getMaxBlockX(),
                    level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                for (StructurePiece piece : all) {
                    if (piece.getBoundingBox().intersects(box)) {
                        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), random, box, chunk,
                            chunk.getWorldPosition());
                    }
                }
            }
        }
    }

    private static void checkCity(GameTestHelper helper, ServerLevel level, BlockPos origin, TownPlanner.Plan plan) {
        int doors = 0;
        int beds = 0;
        int lanterns = 0;
        int copper = 0;
        for (StructurePiece piece : plan.buildings()) {
            for (BlockPos pos : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
                piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
                var state = level.getBlockState(pos);
                doors += state.is(BlockTags.DOORS) ? 1 : 0;
                beds += state.is(BlockTags.BEDS) ? 1 : 0;
                lanterns += state.is(Blocks.LANTERN) ? 1 : 0;
                copper += state.is(Blocks.WAXED_CUT_COPPER_STAIRS) || state.is(Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS)
                    || state.is(Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS) || state.is(Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS) ? 1 : 0;
            }
        }
        AABB area = new AABB(origin).inflate(130, 40, 130);
        int villagers = level.getEntitiesOfClass(Villager.class, area).size();
        int golems = level.getEntitiesOfClass(IronGolem.class, area).size();
        TimeMod.LOGGER.info("Stadt: {} Türhälften, {} Betthälften, {} Laternen, {} Kupferdachstufen, {} Bewohner, {} Golems", doors, beds, lanterns,
            copper, villagers, golems);
        if (doors < plan.buildings().size() || beds < 20 || lanterns < 20 || copper < 100 || villagers < 20 || golems < 3) {
            helper.fail("Die Stadt ist unvollständig: " + doors + " Türen, " + beds + " Betten, " + lanterns + " Laternen, " + copper + " Kupfer, "
                + villagers + " Bewohner, " + golems + " Golems");
        }
        level.getEntitiesOfClass(Villager.class, area).forEach(entity -> entity.discard());
        level.getEntitiesOfClass(IronGolem.class, area).forEach(entity -> entity.discard());
    }
}

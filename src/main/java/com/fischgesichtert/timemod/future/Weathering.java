package com.fischgesichtert.timemod.future;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Die Verwitterungs-Pipeline für einen Chunk: erst jeder Block nach seiner Regel (zerfallen, bemoosen,
 * rosten, anlaufen, erlöschen), dann die Statik (was keinen Halt mehr hat, stürzt ein), zuletzt holt sich
 * die Natur alles zurück (Ranken, Moos, Spinnweben, Sand, Schnee, Bäume).
 *
 * <p>Jeder Block hat seinen festen Zufall aus Weltseed und Position – unabhängig vom Jahr. Was bei +50
 * zerfallen ist, fehlt deshalb auch bei +500, und wer zweimal ins selbe Jahr reist, sieht dieselbe Ruine.
 */
public final class Weathering {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Map<Block, Optional<Block>> MOSSY = new ConcurrentHashMap<>();
    private static final Map<Block, Optional<Block>> CRACKED = new ConcurrentHashMap<>();

    private Weathering() {
    }

    /** Altert den Chunk der Leinwand um {@code years} Jahre. */
    public static void apply(Canvas canvas, long years, WeatheringRules.Resolved rules) {
        if (years <= 0) {
            return;
        }
        LongList fields = new LongArrayList();
        int[] band = blocks(canvas, years, rules, fields);
        if (band == null) {
            return;
        }
        // Statik und Natur arbeiten nur dort, wo überhaupt gebaut wurde (ein Block Rand für Böden und Decken)
        int low = Math.max(canvas.minY(), band[0] - 1);
        int high = Math.min(canvas.maxY() - 1, band[1] + 1);
        Statics.collapse(canvas, years, rules, low, high);
        Reclaim.apply(canvas, years, rules, fields, low, high);
    }

    /** Der Blockdurchgang; gibt das Höhenband der gebauten Blöcke zurück ({@code null}: hier wurde nichts gebaut). */
    private static int[] blocks(Canvas canvas, long years, WeatheringRules.Resolved rules, LongList fields) {
        ChunkPos chunk = canvas.chunk();
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int y = canvas.minY(); y < canvas.maxY(); y++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                    BlockState state = canvas.at(x, y, z);
                    if (state.isAir()) {
                        continue;
                    }
                    WeatheringRule rule = rules.of(state);
                    if (rule == null) {
                        continue;
                    }
                    low = Math.min(low, y);
                    high = Math.max(high, y);
                    BlockState aged = age(canvas, state, rule, x, y, z, years);
                    if (aged != state) {
                        canvas.set(x, y, z, aged);
                    }
                    if (!rule.cover().isEmpty() && !aged.isAir()) {
                        fields.add(BlockPos.asLong(x, y, z));
                    }
                }
            }
        }
        return low > high ? null : new int[] {low, high};
    }

    /** Ein Block nach seiner Regel. Die Würfe geschehen immer in derselben Reihenfolge – das hält das Ergebnis stabil. */
    static BlockState age(Canvas canvas, BlockState state, WeatheringRule rule, int x, int y, int z, long years) {
        RandomSource random = forGroup(canvas.seed(), x, y, z, state);
        float removeRoll = random.nextFloat();
        float rubbleRoll = random.nextFloat();
        float[] rolls = new float[rule.convert().size()];
        for (int i = 0; i < rolls.length; i++) {
            rolls[i] = random.nextFloat();
        }
        if (removeRoll < rule.remove().at(years)) {
            BlockState rest = removed(state);
            if (rule.rubble() != null && rubbleRoll < rule.rubble().chance()) {
                rest = dropRubble(canvas, x, y, z, rule.rubble().block(), rest);
            }
            return rest;
        }
        for (int i = 0; i < rolls.length; i++) {
            WeatheringRule.Conversion conversion = rule.convert().get(i);
            if (rolls[i] < conversion.chance().at(years)) {
                BlockState converted = transform(state, conversion, years);
                if (converted != state) {
                    return converted;
                }
            }
        }
        return state;
    }

    /** Was von einem verschwundenen Block bleibt: Luft – oder das Wasser, in dem er stand. */
    static BlockState removed(BlockState state) {
        return state.getFluidState().isSource() ? state.getFluidState().createLegacyBlock() : AIR;
    }

    /**
     * Schutt fällt bis auf den Boden. Ruht der zerfallene Block selbst auf dem Boden, bleibt der Schutt an
     * seiner Stelle – das gibt die Antwort zurück: was nun an der Stelle des Blocks liegt.
     */
    static BlockState dropRubble(Canvas canvas, int x, int y, int z, BlockState rubble, BlockState rest) {
        for (int ty = y - 1; ty > canvas.minY() && ty > y - 32; ty--) {
            BlockState below = canvas.at(x, ty, z);
            if (!below.isAir() && !below.canBeReplaced()) {
                if (ty + 1 == y) {
                    return rubble;
                }
                BlockState landing = canvas.at(x, ty + 1, z);
                if (landing.isAir() || landing.canBeReplaced()) {
                    canvas.set(x, ty + 1, z, rubble);
                }
                return rest;
            }
        }
        return rest;
    }

    static BlockState transform(BlockState state, WeatheringRule.Conversion conversion, long years) {
        return switch (conversion.to()) {
            case "@mossy" -> variant(state, MOSSY, "mossy_");
            case "@cracked" -> variant(state, CRACKED, "cracked_");
            case "@oxidize" -> oxidize(state, (int) Math.floor(conversion.steps().at(years)));
            case "@unlit" -> state.hasProperty(BlockStateProperties.LIT) ? state.setValue(BlockStateProperties.LIT, false) : state;
            default -> BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(conversion.to()))
                .map(block -> copyProperties(state, block.defaultBlockState())).orElse(state);
        };
    }

    private static BlockState variant(BlockState state, Map<Block, Optional<Block>> cache, String prefix) {
        Optional<Block> variant = cache.computeIfAbsent(state.getBlock(), block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            return BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(id.getNamespace(), prefix + id.getPath()));
        });
        return variant.map(block -> copyProperties(state, block.defaultBlockState())).orElse(state);
    }

    private static BlockState oxidize(BlockState state, int steps) {
        Block block = state.getBlock();
        for (int i = 0; i < steps; i++) {
            Optional<Block> next = WeatheringCopper.getNext(block);
            if (next.isEmpty()) {
                break;
            }
            block = next.get();
        }
        return block == state.getBlock() ? state : copyProperties(state, block.defaultBlockState());
    }

    /** Überträgt alle Eigenschaften, die beide Zustände kennen (Richtung, Hälfte, Wasser …). */
    public static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copy(from, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    /** Fester Zufall einer Stelle; {@code salt} trennt die Würfe der einzelnen Durchgänge. */
    static RandomSource random(long seed, long salt, int x, int y, int z) {
        return RandomSource.create(seed ^ salt ^ Mth.getSeed(x, y, z));
    }

    /** Türen und Betten zerfallen als Ganzes: beide Hälften würfeln mit dem Zufall derselben Stelle. */
    private static RandomSource forGroup(long seed, int x, int y, int z, BlockState state) {
        int gx = x;
        int gy = y;
        int gz = z;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            gy--;
        } else if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            Direction toFoot = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
            gx += toFoot.getStepX();
            gz += toFoot.getStepZ();
        }
        return random(seed, 0x7E3A_11C5_9B2DL, gx, gy, gz);
    }
}

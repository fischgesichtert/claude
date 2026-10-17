package com.fischgesichtert.timemod.worldgen;

import com.fischgesichtert.timemod.TimeMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Befragt eine andere Biomquelle und deutet deren Biome um: dieselbe Landschaft wie heute, aber aus
 * Ebenen werden Urwälder, aus Wüsten Gletscher, aus Dschungeln Farnwälder. Die Epochen-Dimensionen
 * nutzen das unter {@code "type": "timemod:remapped"}.
 */
public class RemappedBiomeSource extends BiomeSource {
    public static final Codec<RemappedBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("base").forGetter(source -> source.base),
            Codec.unboundedMap(ResourceKey.codec(Registries.BIOME), Biome.CODEC).fieldOf("remap").forGetter(source -> source.remap))
        .apply(instance, RemappedBiomeSource::new));

    private final BiomeSource base;
    private final Map<ResourceKey<Biome>, Holder<Biome>> remap;

    public RemappedBiomeSource(BiomeSource base, Map<ResourceKey<Biome>, Holder<Biome>> remap) {
        this.base = base;
        this.remap = Map.copyOf(remap);
    }

    public static void init() {
        Registry.register(BuiltInRegistries.BIOME_SOURCE, TimeMod.id("remapped"), CODEC);
    }

    public BiomeSource base() {
        return base;
    }

    /** Das Biom, das in dieser Epoche an die Stelle des ursprünglichen tritt. */
    public Holder<Biome> map(Holder<Biome> biome) {
        if (biome instanceof Holder.Reference<Biome> reference) {
            Holder<Biome> replacement = remap.get(reference.key());
            if (replacement != null) {
                return replacement;
            }
        }
        return biome;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return base.possibleBiomes().stream().map(this::map).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return map(base.getNoiseBiome(x, y, z, sampler));
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, Climate.Sampler sampler) {
        base.addDebugInfo(info, pos, sampler);
    }
}

package com.fischgesichtert.timemod.worldgen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.future.town.Settlements;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Die Biome der Zukunft: dieselbe Landschaft wie heute, aber das Klima ist ein wenig feuchter und wärmer.
 * Wälder wachsen von ihren Rändern in die Ebenen, mitten im offenen Land entstehen neue Haine, der Schnee
 * zieht sich zurück, Wüsten und Mangroven wachsen an ihren Rändern. Gewachsene Wälder bleiben, wie sie sind,
 * und rund um Dörfer, die noch leben, halten die Bewohner das Land offen.
 *
 * <p>Gerechnet wird mit Minecrafts eigener Klimatabelle: An jeder Stelle werden Feuchte und Temperatur
 * etwas angehoben und das Biom neu nachgeschlagen. Wie stark, hängt von den Jahren ab und schwankt von
 * Gegend zu Gegend – angehoben wird aber nie weniger als gar nicht. Deshalb gilt: Was bei +100 Wald ist,
 * ist auch bei +1000 noch Wald. Gelände und Seed bleiben unberührt; alles ist deterministisch.
 *
 * <p>Welche Biome wie reagieren, steht in Biom-Tags unter {@code timemod:future/}, damit Datenpakete und
 * Biom-Mods mitspielen können.
 */
public class FutureBiomeSource extends BiomeSource {
    public static final Codec<FutureBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("base").forGetter(FutureBiomeSource::base),
            Codec.intRange(1, 1_000_000).fieldOf("years").forGetter(FutureBiomeSource::years),
            Codec.floatRange(0.0F, 0.25F).optionalFieldOf("humidity", 0.0F).forGetter(source -> source.humidity),
            Codec.floatRange(0.0F, 0.15F).optionalFieldOf("temperature", 0.0F).forGetter(source -> source.temperature),
            Codec.floatRange(0.0F, 0.5F).optionalFieldOf("groves", 0.0F).forGetter(source -> source.groves),
            Codec.floatRange(0.0F, 0.9F).optionalFieldOf("variation", 0.0F).forGetter(source -> source.variation))
        .apply(instance, FutureBiomeSource::new));

    /** Gewachsene Wälder (und was das Klima nicht anrührt): bleiben, wie sie heute sind. */
    public static final TagKey<Biome> STEADFAST = tag("steadfast");
    /** Bewaldet: Hierhin wächst der Wald, und von hier zieht er sich nie zurück. */
    public static final TagKey<Biome> WOODED = tag("wooded");
    /** Schnee und Eis (und Sümpfe): folgen der Erwärmung in ihre wärmere Form. */
    public static final TagKey<Biome> FOLLOWS_WARMING = tag("follows_warming");
    /** Was sich bei Wärme ausbreitet: Wüsten, Tafelberge, Mangroven. */
    public static final TagKey<Biome> SPREADING = tag("spreading");

    /** Großräumig: welche Gegend besonders feucht oder warm wird (Blöcke je Rausch-Einheit). */
    private static final double REGION_SCALE = 2400.0;
    private static final double REGION_DETAIL = 900.0;
    /** Haine: einzelne neue Waldstücke mitten im offenen Land. */
    private static final double GROVE_SCALE = 260.0;
    private static final double GROVE_DETAIL = 96.0;
    private static final double GROVE_THRESHOLD = 0.3;

    private final BiomeSource base;
    private final int years;
    private final float humidity;
    private final float temperature;
    private final float groves;
    private final float variation;
    private final boolean unchanged;
    private volatile Noises noises;

    public FutureBiomeSource(BiomeSource base, int years, float humidity, float temperature, float groves, float variation) {
        this.base = base;
        this.years = years;
        this.unchanged = humidity <= 0.0F && temperature <= 0.0F && groves <= 0.0F;
        this.humidity = humidity;
        this.temperature = temperature;
        this.groves = groves;
        this.variation = variation;
    }

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, TimeMod.id("future/" + name));
    }

    public static void init() {
        Registry.register(BuiltInRegistries.BIOME_SOURCE, TimeMod.id("future"), CODEC);
    }

    /** Die Biome von heute (Bauwerke stehen in der Zukunft dort, wo sie heute stehen würden). */
    public BiomeSource base() {
        return base;
    }

    /** Wie viele Jahre diese Zukunft von heute entfernt ist. */
    public int years() {
        return years;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Nachgeschlagen wird immer in derselben Tabelle: Es kommen nur Biome vor, die es heute auch gibt
        return base.possibleBiomes().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        if (unchanged || !(base instanceof MultiNoiseBiomeSource table)) {
            return base.getNoiseBiome(x, y, z, sampler);
        }
        Climate.TargetPoint point = sampler.sample(x, y, z);
        Holder<Biome> today = table.getNoiseBiome(point);
        if (today.is(STEADFAST)) {
            return today;
        }
        int blockX = QuartPos.toBlock(x);
        int blockZ = QuartPos.toBlock(z);
        // Rund um lebende Siedlungen bleibt das Land offen: Felder, Weiden, Gärten statt Wald
        double cleared = Settlements.clearing(blockX, blockZ, years, table, sampler);
        Shift shift = shift(sampler, blockX, blockZ, 1.0 - cleared);
        if (shift.isNone()) {
            return today;
        }
        long wetter = Climate.quantizeCoord(shift.humidity());
        long warmer = Climate.quantizeCoord(shift.temperature());
        Holder<Biome> grown = wetter == 0 ? today : table.getNoiseBiome(moved(point, 0, wetter));
        Holder<Biome> warm = warmer == 0 ? grown : table.getNoiseBiome(moved(point, warmer, wetter));
        if (grown.is(WOODED)) {
            // Der Wald gewinnt gegen die Dürre – nur Schnee und Eis tauen trotzdem
            return today.is(FOLLOWS_WARMING) && warm.is(WOODED) ? warm : grown;
        }
        return today.is(FOLLOWS_WARMING) || warm.is(SPREADING) ? warm : grown;
    }

    private static Climate.TargetPoint moved(Climate.TargetPoint point, long warmer, long wetter) {
        return new Climate.TargetPoint(point.temperature() + warmer, point.humidity() + wetter, point.continentalness(), point.erosion(),
            point.depth(), point.weirdness());
    }

    /** Wie viel feuchter und wärmer es an einer Stelle ist; beides nie negativ. {@code wild}: 1 in freier Natur, 0 im Siedlungsland. */
    public Shift shift(Climate.Sampler sampler, int blockX, int blockZ, double wild) {
        Noises noise = noises(sampler);
        double wet = 1.0 + variation * noise.region(noise.wet, blockX, blockZ);
        double warm = 1.0 + variation * noise.region(noise.warm, blockX, blockZ);
        double grove = groves * noise.grove(blockX, blockZ);
        return new Shift((float) ((humidity * wet + grove) * wild), (float) (temperature * warm));
    }

    /**
     * Die Rauschfelder hängen am Seed: Er steckt im Klima-Sampler der Welt (alle Epochen mit denselben
     * Einstellungen haben dasselbe Klima, also auch dieselben Haine).
     */
    private Noises noises(Climate.Sampler sampler) {
        Noises current = noises;
        if (current == null || current.sampler != sampler) {
            current = new Noises(sampler);
            noises = current;
        }
        return current;
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, Climate.Sampler sampler) {
        base.addDebugInfo(info, pos, sampler);
        Shift shift = shift(sampler, pos.getX(), pos.getZ(), 1.0);
        info.add(String.format(Locale.ROOT, "Future climate: humidity +%.3f temperature +%.3f", shift.humidity(), shift.temperature()));
    }

    /** Die Verschiebung des Klimas an einer Stelle. */
    public record Shift(float humidity, float temperature) {
        public boolean isNone() {
            return humidity <= 0.0F && temperature <= 0.0F;
        }
    }

    private static final class Noises {
        final Climate.Sampler sampler;
        final SimplexNoise wet;
        final SimplexNoise warm;
        final SimplexNoise grove;
        final SimplexNoise groveDetail;

        Noises(Climate.Sampler sampler) {
            this.sampler = sampler;
            RandomSource random = RandomSource.create(salt(sampler));
            this.wet = new SimplexNoise(random);
            this.warm = new SimplexNoise(random);
            this.grove = new SimplexNoise(random);
            this.groveDetail = new SimplexNoise(random);
        }

        /** Ein Fingerabdruck des Seeds: das Klima an zwei festen, weit entfernten Stellen. */
        private static long salt(Climate.Sampler sampler) {
            DensityFunction.SinglePointContext far = new DensityFunction.SinglePointContext(1_234_567, 0, -7_654_321);
            DensityFunction.SinglePointContext other = new DensityFunction.SinglePointContext(-3_141_592, 0, 2_718_281);
            long salt = Double.doubleToLongBits(sampler.temperature().compute(far));
            salt = salt * 31 + Double.doubleToLongBits(sampler.humidity().compute(other));
            salt = salt * 31 + Double.doubleToLongBits(sampler.continentalness().compute(far));
            return salt ^ 0x5EED_F07E_57L;
        }

        /** Großräumig zwischen −1 und 1. */
        double region(SimplexNoise noise, int x, int z) {
            double value = 0.75 * noise.getValue(x / REGION_SCALE, z / REGION_SCALE) + 0.25 * noise.getValue(x / REGION_DETAIL + 37.1,
                z / REGION_DETAIL - 11.3);
            return Mth.clamp(value, -1.0, 1.0);
        }

        /** 0 im offenen Land, bis 1 in der Mitte eines neuen Hains. */
        double grove(int x, int z) {
            double value = 0.75 * grove.getValue(x / GROVE_SCALE, z / GROVE_SCALE) + 0.25 * groveDetail.getValue(x / GROVE_DETAIL,
                z / GROVE_DETAIL);
            double t = Mth.clamp((value - GROVE_THRESHOLD) / (1.0 - GROVE_THRESHOLD) * 1.6, 0.0, 1.0);
            return t * t * (3.0 - 2.0 * t);
        }
    }
}

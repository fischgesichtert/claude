package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.TimeMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Lädt die Verwitterungsregeln aus den Datenpaketen ({@code data/<ns>/weathering/}) und ordnet jedem Block
 * seine Regel zu: zuerst ausdrückliche Listen nach Priorität, dann – nur für Blöcke anderer Mods – der
 * Klang des Blocks (Holz, Glas, Metall, Wolle …). Was zur Natur gehört ({@code #timemod:weathering/natural}),
 * bekommt nie eine Rückfall-Regel.
 */
public final class WeatheringRules extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    public static final TagKey<Block> NATURAL = TagKey.create(Registries.BLOCK, TimeMod.id("weathering/natural"));
    private static final Gson GSON = new GsonBuilder().create();
    /** Diese Datei regelt die Rückeroberung durch die Natur statt eines Blocks. */
    private static final ResourceLocation RECLAIM = TimeMod.id("reclaim");
    private static final Map<String, SoundType> SOUNDS = Map.ofEntries(
        Map.entry("wood", SoundType.WOOD), Map.entry("bamboo_wood", SoundType.BAMBOO_WOOD), Map.entry("cherry_wood", SoundType.CHERRY_WOOD),
        Map.entry("nether_wood", SoundType.NETHER_WOOD), Map.entry("bamboo", SoundType.BAMBOO), Map.entry("glass", SoundType.GLASS),
        Map.entry("wool", SoundType.WOOL), Map.entry("metal", SoundType.METAL), Map.entry("copper", SoundType.COPPER),
        Map.entry("chain", SoundType.CHAIN), Map.entry("lantern", SoundType.LANTERN), Map.entry("anvil", SoundType.ANVIL),
        Map.entry("netherite_block", SoundType.NETHERITE_BLOCK), Map.entry("ladder", SoundType.LADDER), Map.entry("scaffolding", SoundType.SCAFFOLDING),
        Map.entry("stone", SoundType.STONE), Map.entry("deepslate_bricks", SoundType.DEEPSLATE_BRICKS), Map.entry("nether_bricks", SoundType.NETHER_BRICKS),
        Map.entry("mud_bricks", SoundType.MUD_BRICKS), Map.entry("polished_deepslate", SoundType.POLISHED_DEEPSLATE),
        Map.entry("deepslate_tiles", SoundType.DEEPSLATE_TILES));

    private static volatile List<WeatheringRule> rules = List.of();
    private static volatile Reclaim.Settings reclaim = Reclaim.Settings.DEFAULT;
    private static volatile Resolved resolved;

    /** Die gültige Zuordnung und die Einstellungen der Rückeroberung. */
    public record Resolved(Map<Block, WeatheringRule> byBlock, Reclaim.Settings reclaim) {
        @Nullable
        public WeatheringRule of(BlockState state) {
            return byBlock.get(state.getBlock());
        }

        /** Von Menschen gemacht: alles, wofür eine Regel gilt. */
        public boolean isMade(BlockState state) {
            return byBlock.containsKey(state.getBlock());
        }
    }

    private WeatheringRules() {
        super(GSON, "weathering");
    }

    public static void init() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new WeatheringRules());
    }

    @Override
    public ResourceLocation getFabricId() {
        return TimeMod.id("weathering");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<WeatheringRule> parsed = new ArrayList<>();
        Reclaim.Settings settings = Reclaim.Settings.DEFAULT;
        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            try {
                if (file.getKey().equals(RECLAIM)) {
                    settings = Reclaim.Settings.parse(file.getValue().getAsJsonObject());
                } else {
                    parsed.add(WeatheringRule.parse(file.getKey(), file.getValue().getAsJsonObject()));
                }
            } catch (RuntimeException e) {
                TimeMod.LOGGER.error("Verwitterungsregel {} ist fehlerhaft: {}", file.getKey(), e.getMessage());
            }
        }
        parsed.sort(Comparator.comparingInt(WeatheringRule::priority).reversed().thenComparing(rule -> rule.id().toString()));
        rules = List.copyOf(parsed);
        reclaim = settings;
        resolved = null;
    }

    /** Beim ersten Gebrauch nach dem Laden berechnet (dann sind auch die Tags gebunden), danach unveränderlich. */
    public static Resolved get() {
        Resolved current = resolved;
        if (current == null) {
            synchronized (WeatheringRules.class) {
                current = resolved;
                if (current == null) {
                    current = resolve(rules, reclaim);
                    resolved = current;
                }
            }
        }
        return current;
    }

    private static Resolved resolve(List<WeatheringRule> rules, Reclaim.Settings reclaim) {
        Map<Block, WeatheringRule> map = new IdentityHashMap<>();
        for (WeatheringRule rule : rules) {
            for (String selector : rule.blocks()) {
                if (selector.startsWith("#")) {
                    TagKey<Block> tag = TagKey.create(Registries.BLOCK, new ResourceLocation(selector.substring(1)));
                    for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                        map.putIfAbsent(holder.value(), rule);
                    }
                } else {
                    BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(selector)).ifPresent(block -> map.putIfAbsent(block, rule));
                }
            }
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (map.containsKey(block) || state.isAir() || state.is(NATURAL) || BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft")) {
                continue;
            }
            SoundType sound = state.getSoundType();
            for (WeatheringRule rule : rules) {
                if (rule.sounds().stream().anyMatch(name -> SOUNDS.get(name) == sound)) {
                    map.put(block, rule);
                    break;
                }
            }
        }
        return new Resolved(map, reclaim);
    }
}

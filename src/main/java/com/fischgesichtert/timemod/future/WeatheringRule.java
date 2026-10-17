package com.fischgesichtert.timemod.future;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Was die Jahre mit einer Art Block machen. Aus {@code data/<ns>/weathering/*.json}:
 *
 * <pre>
 * {
 *   "priority": 10,
 *   "blocks": ["#minecraft:planks", "minecraft:crafting_table"],   // ausdrücklich
 *   "sounds": ["wood"],                                            // Rückfall für Blöcke anderer Mods
 *   "remove": {"50": 0.15, "1000": 1.0},                           // Chance, ganz zu verschwinden
 *   "convert": [{"to": "@mossy", "chance": {"50": 0.3}}],          // Block-ID oder @mossy, @cracked, @oxidize, @unlit
 *   "rubble": {"block": "minecraft:cobblestone", "chance": 0.3},   // was beim Zerfall herunterfällt
 *   "span": {"1": 12, "1000": 4},                                  // Statik: wie weit es ohne Stütze trägt
 *   "cover": [{"block": "minecraft:grass", "chance": {"5": 0.4}}]  // was obendrauf wächst
 * }
 * </pre>
 */
public record WeatheringRule(ResourceLocation id, int priority, List<String> blocks, List<String> sounds, Curve remove,
                             List<Conversion> convert, @Nullable Rubble rubble, Curve span, List<Cover> cover) {

    /** Umwandlung: {@code to} ist eine Block-ID oder eine Verwandlung ({@code @mossy}, {@code @cracked}, {@code @oxidize}, {@code @unlit}). */
    public record Conversion(String to, Curve chance, Curve steps) {
    }

    public record Rubble(BlockState block, float chance) {
    }

    public record Cover(BlockState block, Curve chance) {
    }

    /** Wie weit ein Block ohne Stütze trägt, wenn die Regel nichts sagt. */
    public static final double DEFAULT_SPAN = 12;

    public static WeatheringRule parse(ResourceLocation id, JsonObject json) {
        List<String> blocks = strings(json, "blocks");
        List<String> sounds = strings(json, "sounds");
        List<Conversion> convert = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "convert", new JsonArray())) {
            JsonObject object = element.getAsJsonObject();
            convert.add(new Conversion(GsonHelper.getAsString(object, "to"), Curve.parse(object.get("chance")),
                Curve.parse(object.get("steps"), 1.0)));
        }
        Rubble rubble = null;
        if (json.has("rubble")) {
            JsonObject object = GsonHelper.getAsJsonObject(json, "rubble");
            rubble = new Rubble(block(GsonHelper.getAsString(object, "block")), GsonHelper.getAsFloat(object, "chance", 0.3F));
        }
        List<Cover> cover = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "cover", new JsonArray())) {
            JsonObject object = element.getAsJsonObject();
            cover.add(new Cover(block(GsonHelper.getAsString(object, "block")), Curve.parse(object.get("chance"))));
        }
        Curve span = Curve.parse(json.has("span") ? json.get("span") : new JsonPrimitive(DEFAULT_SPAN), DEFAULT_SPAN);
        return new WeatheringRule(id, GsonHelper.getAsInt(json, "priority", 0), blocks, sounds, Curve.parse(json.get("remove")), convert,
            rubble, span, cover);
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> list = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(id)).defaultBlockState();
    }
}

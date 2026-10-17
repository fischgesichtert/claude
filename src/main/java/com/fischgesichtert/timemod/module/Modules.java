package com.fischgesichtert.timemod.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.fischgesichtert.timemod.time.TimeFilter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Lesen und Schreiben der eingebauten Module eines Werkzeugs (NBT-Liste von Item-IDs). */
public final class Modules {
    public static final String KEY = "Modules";
    /** Radius und Dauer wachsen je Verstärker um 50 %. */
    public static final double STEP = 1.5;
    /** Jedes Effizienzmodul spart ein Viertel. */
    public static final double EFFICIENCY = 0.75;

    private static final int[] FILTER_BITS = {TimeFilter.ANIMALS, TimeFilter.MONSTERS, TimeFilter.VILLAGERS, TimeFilter.PLAYERS,
        TimeFilter.PROJECTILES, TimeFilter.ITEMS, TimeFilter.VEHICLES, TimeFilter.GROWTH, TimeFilter.ENVIRONMENT, TimeFilter.FLUIDS,
        TimeFilter.BLOCK_TICKS, TimeFilter.MACHINES, TimeFilter.DAYTIME, TimeFilter.WEATHER};
    private static final String[] FILTER_NAMES = {"animals", "monsters", "villagers", "players", "projectiles", "items", "vehicles",
        "growth", "environment", "fluids", "redstone", "machines", "daytime", "weather"};

    private Modules() {
    }

    /** „Tiere, Monster, Wachstum“ – lesbare Liste der Kategorien einer Filtermaske. */
    public static Component describe(int mask) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (int i = 0; i < FILTER_BITS.length; i++) {
            if ((mask & FILTER_BITS[i]) != 0) {
                if (!first) {
                    out.append(", ");
                }
                out.append(Component.translatable("filter.timemod." + FILTER_NAMES[i]));
                first = false;
            }
        }
        return out;
    }

    /** Werte-Zeilen für Tooltip und Werkbank: Reichweite/Dauer/Kosten und ggf. die Filter. */
    public static List<Component> stats(Effects effects, Component values) {
        List<Component> out = new ArrayList<>();
        out.add(values);
        if (effects.filter() != 0) {
            out.add(Component.translatable("tooltip.timemod.affects", describe(effects.filter())));
        }
        return out;
    }

    public static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    /** Eingebaute Module in Platz-Reihenfolge (leere Plätze ausgelassen). */
    public static List<ModuleItem> installed(ItemStack tool) {
        List<ModuleItem> out = new ArrayList<>();
        CompoundTag tag = tool.getTag();
        if (tag == null || !tag.contains(KEY, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = tag.getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null && BuiltInRegistries.ITEM.get(id) instanceof ModuleItem module) {
                out.add(module);
            }
        }
        return out;
    }

    /** Ein Modul je Platz; leere Plätze werden als "" gespeichert, damit die Plätze stabil bleiben. */
    public static List<ItemStack> slots(ItemStack tool, int count) {
        List<ItemStack> out = new ArrayList<>();
        CompoundTag tag = tool.getTag();
        ListTag list = tag != null ? tag.getList(KEY, Tag.TAG_STRING) : new ListTag();
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (i < list.size()) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null && BuiltInRegistries.ITEM.get(id) instanceof ModuleItem module) {
                    stack = new ItemStack(module);
                }
            }
            out.add(stack);
        }
        return out;
    }

    public static void write(ItemStack tool, List<ItemStack> slots) {
        ListTag list = new ListTag();
        boolean any = false;
        for (ItemStack stack : slots) {
            if (stack.getItem() instanceof ModuleItem) {
                list.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
                any = true;
            } else {
                list.add(StringTag.valueOf(""));
            }
        }
        if (any) {
            tool.getOrCreateTag().put(KEY, list);
        } else if (tool.getTag() != null) {
            tool.getTag().remove(KEY);
            if (tool.getTag().isEmpty()) {
                tool.setTag(null);
            }
        }
    }

    public static Effects effects(ItemStack tool) {
        int filter = 0;
        int radius = 0;
        int duration = 0;
        int strength = 0;
        int efficiency = 0;
        int damping = 0;
        for (ModuleItem module : installed(tool)) {
            switch (module.kind()) {
                case FILTER -> filter |= module.mask();
                case RADIUS -> radius++;
                case DURATION -> duration++;
                case STRENGTH -> strength++;
                case EFFICIENCY -> efficiency++;
                case DAMPING -> damping++;
                default -> {
                }
            }
        }
        return new Effects(filter, radius, duration, strength, efficiency, damping);
    }

    /** Zusammengefasste Wirkung aller eingebauten Module. */
    public record Effects(int filter, int radius, int duration, int strength, int efficiency, int damping) {
        public static final Effects NONE = new Effects(0, 0, 0, 0, 0, 0);

        /** Ohne Filtermodul wirkt das Werkzeug auf seine Standard-Kategorien. */
        public int mask(int fallback) {
            return filter != 0 ? filter : fallback;
        }

        public double radius(double base) {
            return base * Math.pow(STEP, radius);
        }

        public int duration(int base) {
            return (int) Math.round(base * Math.pow(STEP, duration));
        }

        /** Größerer Radius kostet nach Fläche, längere Dauer linear; Effizienz spart. */
        public int cost(double base) {
            return (int) Math.ceil(base * Math.pow(STEP, 2 * radius) * Math.pow(STEP, duration) * Math.pow(EFFICIENCY, efficiency));
        }
    }
}

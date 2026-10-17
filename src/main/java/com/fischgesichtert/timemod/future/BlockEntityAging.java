package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.Aging;
import com.fischgesichtert.timemod.registry.ModBlockEntities;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Was in Block-Entities die Jahre überdauert. Lager (Truhen, Fässer, Pulte …): der Inhalt altert wie in
 * der Zeitkapsel und wird zum {@link Echo} – sichtbar, aber nicht greifbar. Deko: Schilder verblassen bis
 * zur Unlesbarkeit. Maschinen (Öfen, Trichter, Spender …) stehen leer. Alles andere – TIME-Maschinen,
 * Befehlsblöcke, Block-Entities anderer Mods – beginnt frisch, ohne Daten aus der Gegenwart.
 */
final class BlockEntityAging {
    enum Kind {
        STORAGE, DECOR, MACHINE, FRESH
    }

    private static final Set<BlockEntityType<?>> STORAGE = Set.of(BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.BARREL,
        BlockEntityType.SHULKER_BOX, BlockEntityType.CHISELED_BOOKSHELF, BlockEntityType.LECTERN, BlockEntityType.JUKEBOX,
        BlockEntityType.DECORATED_POT, ModBlockEntities.TIME_CAPSULE);
    private static final Set<BlockEntityType<?>> MACHINES = Set.of(BlockEntityType.FURNACE, BlockEntityType.BLAST_FURNACE, BlockEntityType.SMOKER,
        BlockEntityType.HOPPER, BlockEntityType.DROPPER, BlockEntityType.DISPENSER, BlockEntityType.BREWING_STAND, BlockEntityType.CAMPFIRE);
    private static final String[] MACHINE_STATE = {"Items", "BurnTime", "CookTime", "CookTimeTotal", "CookingTimes", "CookingTotalTimes",
        "BrewTime", "Fuel", "TransferCooldown"};

    private BlockEntityAging() {
    }

    static Kind kind(BlockEntityType<?> type) {
        if (STORAGE.contains(type)) {
            return Kind.STORAGE;
        }
        if (MACHINES.contains(type)) {
            return Kind.MACHINE;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        if (id == null || !id.getNamespace().equals("minecraft") || type == BlockEntityType.COMMAND_BLOCK
            || type == BlockEntityType.STRUCTURE_BLOCK || type == BlockEntityType.JIGSAW) {
            return Kind.FRESH;
        }
        return Kind.DECOR;
    }

    /**
     * Altert die Daten an Ort und Stelle und gibt den passenden Blockzustand zurück – oder {@code null},
     * wenn das Block-Entity frisch beginnen soll.
     */
    @Nullable
    static BlockState age(CompoundTag tag, BlockState state, BlockEntityType<?> type, Level future, long years, RandomSource random,
                          Reclaim.Settings settings) {
        Kind kind = kind(type);
        if (kind == Kind.FRESH) {
            return null;
        }
        if (kind == Kind.MACHINE) {
            for (String key : MACHINE_STATE) {
                tag.remove(key);
            }
            return state;
        }
        if (kind == Kind.STORAGE && tag.contains("Items", Tag.TAG_LIST)) {
            ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
            ListTag aged = new ListTag();
            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i);
                ItemStack stack = Aging.age(ItemStack.of(entry), years);
                if (!stack.isEmpty()) {
                    Echo.mark(stack, future);
                    CompoundTag saved = stack.save(new CompoundTag());
                    if (entry.contains("Slot")) {
                        saved.putByte("Slot", entry.getByte("Slot"));
                    }
                    aged.add(saved);
                }
            }
            tag.put("Items", aged);
            if (state.getBlock() instanceof ChiseledBookShelfBlock) {
                state = bookSlots(state, aged);
            }
        }
        if (tag.contains("Book", Tag.TAG_COMPOUND)) {
            ItemStack book = Aging.age(ItemStack.of(tag.getCompound("Book")), years);
            if (book.isEmpty()) {
                tag.remove("Book");
                tag.remove("Page");
                if (state.hasProperty(LecternBlock.HAS_BOOK)) {
                    state = state.setValue(LecternBlock.HAS_BOOK, false);
                }
            } else {
                Echo.mark(book, future);
                tag.put("Book", book.save(new CompoundTag()));
            }
        }
        for (String key : new String[] {"RecordItem", "item"}) {
            if (tag.contains(key, Tag.TAG_COMPOUND)) {
                ItemStack stored = Aging.age(ItemStack.of(tag.getCompound(key)), years);
                if (stored.isEmpty()) {
                    tag.remove(key);
                } else {
                    Echo.mark(stored, future);
                    tag.put(key, stored.save(new CompoundTag()));
                }
            }
        }
        double fade = settings.signFade().at(years) * (tag.getBoolean("is_waxed") ? 0.5 : 1.0);
        for (String side : new String[] {"front_text", "back_text"}) {
            if (tag.contains(side, Tag.TAG_COMPOUND)) {
                fadeText(tag.getCompound(side), fade, random);
            }
        }
        return state;
    }

    /** Buchstaben verblassen einzeln; ist fast alles weg, bleibt nichts Lesbares. */
    private static void fadeText(CompoundTag text, double fade, RandomSource random) {
        ListTag messages = text.getList("messages", Tag.TAG_STRING);
        ListTag faded = new ListTag();
        for (int i = 0; i < messages.size(); i++) {
            String plain;
            try {
                Component component = Component.Serializer.fromJson(messages.getString(i));
                plain = component == null ? "" : component.getString();
            } catch (RuntimeException e) {
                plain = "";
            }
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < plain.length(); c++) {
                line.append(random.nextFloat() < fade ? ' ' : plain.charAt(c));
            }
            String result = fade >= 0.999 ? "" : line.toString().stripTrailing();
            faded.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(result))));
        }
        text.put("messages", faded);
        text.putBoolean("has_glowing_text", false);
    }

    private static BlockState bookSlots(BlockState state, ListTag items) {
        boolean[] occupied = new boolean[ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size()];
        for (int i = 0; i < items.size(); i++) {
            int slot = items.getCompound(i).getByte("Slot") & 255;
            if (slot < occupied.length) {
                occupied[slot] = true;
            }
        }
        for (int i = 0; i < occupied.length; i++) {
            BooleanProperty property = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            state = state.setValue(property, occupied[i]);
        }
        return state;
    }
}

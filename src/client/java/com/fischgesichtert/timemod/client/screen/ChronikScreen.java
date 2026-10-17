package com.fischgesichtert.timemod.client.screen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * „Die Chronik“ – das Handbuch als aufgeschlagenes Buch mit Doppelseiten: vorn ein Inhaltsverzeichnis
 * (anklickbar), dann je Kapitel eine Reihe Item-Bilder und der Text, automatisch umbrochen.
 */
public class ChronikScreen extends Screen {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/gui/chronik.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 180;
    private static final int PAGE_W = 112;
    private static final int LINE_H = 9;
    private static final int LINES = 15;
    private static final int TEXT_COLOR = 0x3B2A1A;
    private static final int TITLE_COLOR = 0x4A2468;

    private record Chapter(String key, List<Item> icons) {
    }

    private static final List<Chapter> CHAPTERS = List.of(
        new Chapter("intro", List.of(ModItems.POCKET_WATCH, ModItems.CHRONITE_CRYSTAL, ModItems.STASIS_WATCH, ModItems.REWIND_SPOOL)),
        new Chapter("tier1", List.of(ModItems.CLOCKMAKER_BENCH, ModItems.CLOCKWORK, ModItems.GROWTH_HOURGLASS, ModItems.BRAKE_SPRING,
            ModItems.MOMENT_LENS, ModItems.RECALL_WATCH)),
        new Chapter("tier2", List.of(ModItems.DEEPSLATE_CHRONITE_ORE, ModItems.CHRONITE_SEED, ModItems.TIME_CONDENSER, ModItems.POCKET_TIME_CELL,
            ModItems.TIME_ACCELERATOR, ModItems.TIME_FIELD_PROJECTOR)),
        new Chapter("tier2_tools", List.of(ModItems.STASIS_WATCH, ModItems.SLOW_PENDULUM, ModItems.REWIND_SPOOL, ModItems.CHRONO_KNIFE)),
        new Chapter("tier3", List.of(ModItems.TEMPORAL_WORKBENCH, ModItems.CHRONO_PILLAR, ModItems.BLANK_MODULE, ModItems.FILTER_GROWTH,
            ModItems.RADIUS_MODULE, ModItems.STRENGTH_MODULE)),
        new Chapter("tier3_gear", List.of(ModItems.CHRONO_UPGRADE_TEMPLATE, ModItems.CHRONITE_PLATE, ModItems.CHRONO_HELMET,
            ModItems.LIFETIME_EXTRACTOR, ModItems.PARADOX_TIME_CELL)),
        new Chapter("places", List.of(Items.FILLED_MAP, Items.SPYGLASS, ModItems.TIME_CORE_FRAGMENT, ModItems.CLOCKMAKER_ALTAR)),
        new Chapter("clockmaker", List.of(ModItems.CLOCKMAKER_ALTAR, ModItems.CLOCKWORK, ModItems.SLOW_PENDULUM, ModItems.CLOCKWORK_CORE)),
        new Chapter("chronophage", List.of(ModItems.PHAGE_ALTAR, ModItems.TIME_CORE_FRAGMENT, ModItems.STASIS_WATCH, ModItems.PHAGE_HEART)),
        new Chapter("paradox", List.of(ModItems.PARADOX_ALTAR, ModItems.PARADOX_TIME_CELL, ModItems.CHRONO_CHESTPLATE, ModItems.HEART_OF_TIME)),
        new Chapter("tier4", List.of(ModItems.HEART_OF_TIME, Items.NETHER_STAR, ModItems.STRENGTH_MODULE, ModItems.CHRONOS_WATCH)),
        new Chapter("future", List.of(Items.CLOCK, ModItems.CHRONITE_CRYSTAL, Items.ENDER_EYE)));

    /** Eine Buchseite: Zeilen, ggf. Kapitelkopf mit Bildern. */
    private record Page(@Nullable Chapter chapter, boolean head, List<FormattedCharSequence> lines) {
    }

    private final List<Page> pages = new ArrayList<>();
    private final int[] chapterPage = new int[CHAPTERS.size()];
    private int spread;
    private int left;
    private int top;
    private PageButton next;
    private PageButton previous;

    public ChronikScreen() {
        super(Component.translatable("chronik.timemod.title"));
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
        layout();
        next = addRenderableWidget(new PageButton(left + BOOK_W - 44, top + BOOK_H - 22, true, b -> turn(1), true));
        previous = addRenderableWidget(new PageButton(left + 20, top + BOOK_H - 22, false, b -> turn(-1), true));
        updateButtons();
    }

    /** Alle Kapitel in Seiten aufteilen (Seite 0 und 1 sind das Inhaltsverzeichnis). */
    private void layout() {
        pages.clear();
        pages.add(new Page(null, false, List.of()));
        pages.add(new Page(null, false, List.of()));
        for (int c = 0; c < CHAPTERS.size(); c++) {
            Chapter chapter = CHAPTERS.get(c);
            chapterPage[c] = pages.size();
            List<FormattedCharSequence> lines = font.split(Component.translatable("chronik.timemod." + chapter.key() + ".text"), PAGE_W);
            int firstCapacity = LINES - 4;
            int index = 0;
            List<FormattedCharSequence> first = new ArrayList<>(lines.subList(0, Math.min(lines.size(), firstCapacity)));
            pages.add(new Page(chapter, true, first));
            index = first.size();
            while (index < lines.size()) {
                int end = Math.min(lines.size(), index + LINES);
                pages.add(new Page(chapter, false, new ArrayList<>(lines.subList(index, end))));
                index = end;
            }
            // Kapitel beginnen immer links
            if (pages.size() % 2 == 1) {
                pages.add(new Page(chapter, false, List.of()));
            }
        }
    }

    private int spreads() {
        return (pages.size() + 1) / 2;
    }

    private void turn(int delta) {
        spread = Math.max(0, Math.min(spreads() - 1, spread + delta));
        updateButtons();
    }

    private void updateButtons() {
        if (next != null) {
            next.visible = spread < spreads() - 1;
            previous.visible = spread > 0;
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        turn(amount < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (spread == 0) {
            int hit = indexEntryAt(mouseX, mouseY);
            if (hit >= 0) {
                spread = chapterPage[hit] / 2;
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int indexEntryAt(double mouseX, double mouseY) {
        for (int c = 0; c < CHAPTERS.size(); c++) {
            int[] at = indexPosition(c);
            if (mouseX >= at[0] && mouseX < at[0] + PAGE_W && mouseY >= at[1] - 2 && mouseY < at[1] + 18) {
                return c;
            }
        }
        return -1;
    }

    /** Wo steht der Eintrag {@code c} im Inhaltsverzeichnis? (erste Hälfte links, der Rest rechts) */
    private int[] indexPosition(int c) {
        int perColumn = (CHAPTERS.size() + 1) / 2;
        int column = c < perColumn ? 0 : 1;
        int row = column == 0 ? c : c - perColumn;
        int spacing = Math.min(24, (BOOK_H - 64) / perColumn);
        return new int[] {pageX(column), top + 36 + row * spacing};
    }

    private int pageX(int side) {
        return left + (side == 0 ? 24 : 158);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.blit(TEXTURE, left, top, 0, 0, BOOK_W, BOOK_H, 512, 256);
        if (spread == 0) {
            renderIndex(graphics, mouseX, mouseY);
        } else {
            renderPage(graphics, spread * 2, 0);
            renderPage(graphics, spread * 2 + 1, 1);
        }
        for (int side = 0; side < 2; side++) {
            int number = spread * 2 + side + 1;
            String label = String.valueOf(number);
            graphics.drawString(font, label, pageX(side) + PAGE_W / 2 - font.width(label) / 2, top + BOOK_H - 18, 0x8A7552, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderIndex(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("chronik.timemod.title").withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, title, pageX(0) + PAGE_W / 2 - font.width(title) / 2, top + 14, TITLE_COLOR, false);
        Component subtitle = Component.translatable("chronik.timemod.subtitle");
        graphics.drawString(font, subtitle, pageX(1) + PAGE_W / 2 - font.width(subtitle) / 2, top + 14, 0x8A7552, false);
        int hovered = indexEntryAt(mouseX, mouseY);
        for (int c = 0; c < CHAPTERS.size(); c++) {
            int[] at = indexPosition(c);
            Chapter chapter = CHAPTERS.get(c);
            if (c == hovered) {
                graphics.fill(at[0] - 2, at[1] - 2, at[0] + PAGE_W, at[1] + 18, 0x30A07840);
            }
            graphics.renderItem(new ItemStack(chapter.icons().get(chapter.icons().size() - 1)), at[0], at[1]);
            Component name = Component.translatable("chronik.timemod." + chapter.key() + ".title");
            List<FormattedCharSequence> lines = font.split(name, PAGE_W - 20);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), at[0] + 20, at[1] + (lines.size() == 1 ? 4 : i * 9), c == hovered ? TITLE_COLOR : TEXT_COLOR, false);
            }
        }
    }

    private void renderPage(GuiGraphics graphics, int index, int side) {
        if (index >= pages.size()) {
            return;
        }
        Page page = pages.get(index);
        int x = pageX(side);
        int y = top + 14;
        if (page.head() && page.chapter() != null) {
            Component title = Component.translatable("chronik.timemod." + page.chapter().key() + ".title").withStyle(ChatFormatting.BOLD);
            for (FormattedCharSequence line : font.split(title, PAGE_W)) {
                graphics.drawString(font, line, x, y, TITLE_COLOR, false);
                y += LINE_H;
            }
            y += 2;
            List<Item> icons = page.chapter().icons();
            int iconX = x + (PAGE_W - icons.size() * 18) / 2;
            for (int i = 0; i < icons.size(); i++) {
                graphics.renderItem(new ItemStack(icons.get(i)), iconX + i * 18, y);
            }
            y += 20;
        }
        for (FormattedCharSequence line : page.lines()) {
            graphics.drawString(font, line, x, y, TEXT_COLOR, false);
            y += LINE_H;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

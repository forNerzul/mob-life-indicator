package com.fornerzul.moblifeindicator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartBarTest {

    private final HeartBar heartBar = new HeartBar();

    @Test
    void fullHealthShowsAllFullHearts() {
        List<TextColor> hearts = colorsOf(heartBar.render(20, 20));

        assertEquals(10, hearts.size());
        assertEquals(Collections.nCopies(10, NamedTextColor.RED), hearts);
    }

    @Test
    void partialHealthMixesFullHalfAndEmpty() {
        List<TextColor> hearts = colorsOf(heartBar.render(13, 20));

        assertEquals(10, hearts.size());
        assertEquals(6, count(hearts, NamedTextColor.RED));
        assertEquals(1, count(hearts, NamedTextColor.GOLD));
        assertEquals(3, count(hearts, NamedTextColor.DARK_GRAY));
    }

    @Test
    void veryLowHealthShowsOneHalfHeart() {
        List<TextColor> hearts = colorsOf(heartBar.render(1, 20));

        assertEquals(10, hearts.size());
        assertEquals(1, count(hearts, NamedTextColor.GOLD));
        assertEquals(0, count(hearts, NamedTextColor.RED));
        assertEquals(9, count(hearts, NamedTextColor.DARK_GRAY));
    }

    @Test
    void fractionalHealthBelowHalfThresholdStillShowsAHalfHeart() {
        List<TextColor> hearts = colorsOf(heartBar.render(0.4, 20));

        assertEquals(10, hearts.size());
        assertTrue(count(hearts, NamedTextColor.GOLD) >= 1, "expected at least one half heart for health > 0");
        assertEquals(0, count(hearts, NamedTextColor.RED));
    }

    @Test
    void zeroHealthShowsAllEmptyHearts() {
        List<TextColor> hearts = colorsOf(heartBar.render(0, 20));

        assertEquals(10, hearts.size());
        assertEquals(10, count(hearts, NamedTextColor.DARK_GRAY));
    }

    @Test
    void healthAboveMaxIsClampedToFull() {
        List<TextColor> hearts = colorsOf(heartBar.render(999, 20));

        assertEquals(10, hearts.size());
        assertEquals(10, count(hearts, NamedTextColor.RED));
    }

    @Test
    void lowMaxHealthUsesFewerSlots() {
        List<TextColor> hearts = colorsOf(heartBar.render(8, 8));

        assertEquals(4, hearts.size());
        assertEquals(4, count(hearts, NamedTextColor.RED));
    }

    @Test
    void highMaxHealthCapsAtTenSlots() {
        List<TextColor> hearts = colorsOf(heartBar.render(300, 300));

        assertEquals(10, hearts.size());
        assertEquals(10, count(hearts, NamedTextColor.RED));
    }

    @Test
    void scaledHealthSplitsFullAndEmptyEvenly() {
        List<TextColor> hearts = colorsOf(heartBar.render(150, 300));

        assertEquals(10, hearts.size());
        assertEquals(5, count(hearts, NamedTextColor.RED));
        assertEquals(5, count(hearts, NamedTextColor.DARK_GRAY));
    }

    @Test
    void zeroMaxHealthDoesNotCrash() {
        Component component = heartBar.render(0, 0);

        assertEquals("", PlainTextComponentSerializer.plainText().serialize(component));
    }

    @Test
    void negativeMaxHealthDoesNotCrash() {
        Component component = heartBar.render(5, -1);

        assertEquals("", PlainTextComponentSerializer.plainText().serialize(component));
    }

    private static long count(List<TextColor> colors, TextColor color) {
        return colors.stream().filter(color::equals).count();
    }

    private static List<TextColor> colorsOf(Component component) {
        List<TextColor> colors = new ArrayList<>();
        collect(component, colors);
        return colors;
    }

    private static void collect(Component component, List<TextColor> out) {
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            TextColor color = text.color();
            int codePoints = text.content().codePointCount(0, text.content().length());
            for (int i = 0; i < codePoints; i++) {
                out.add(color);
            }
        }
        for (Component child : component.children()) {
            collect(child, out);
        }
    }
}

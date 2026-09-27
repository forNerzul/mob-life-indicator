package com.fornerzul.moblifeindicator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Pure renderer that turns a health/maxHealth pair into a heart bar
 * {@link Component}. Has no dependency on Bukkit so it can be unit tested
 * without a server.
 *
 * <p>Up to {@value #MAX_SLOTS} heart slots are used. Mobs with more than
 * {@code MAX_SLOTS * HP_PER_SLOT} max health have their health scaled so the
 * bar still fits in {@value #MAX_SLOTS} hearts (e.g. a 300 HP boss uses 30 HP
 * per heart instead of the vanilla 2).
 */
public final class HeartBar {

    private static final int MAX_SLOTS = 10;
    private static final double VANILLA_HP_PER_HEART = 2.0;

    private enum SlotState {
        FULL(NamedTextColor.RED),
        HALF(NamedTextColor.GOLD),
        EMPTY(NamedTextColor.DARK_GRAY);

        private final TextColor color;

        SlotState(TextColor color) {
            this.color = color;
        }
    }

    /**
     * Renders the heart bar for the given health values.
     *
     * @param health    current health, clamped to {@code [0, maxHealth]}
     * @param maxHealth max health; a value {@code <= 0} renders an empty
     *                  component instead of crashing
     */
    public Component render(double health, double maxHealth) {
        int slots = slotCount(maxHealth);
        if (slots <= 0) {
            return Component.empty();
        }

        double clampedHealth = Math.max(0, Math.min(health, maxHealth));
        double perSlot = maxHealth / slots;
        double halfThreshold = perSlot / 2.0;

        SlotState[] states = new SlotState[slots];
        boolean anyVisible = false;
        for (int i = 0; i < slots; i++) {
            double remaining = clampedHealth - (i * perSlot);
            SlotState state;
            if (remaining >= perSlot) {
                state = SlotState.FULL;
            } else if (remaining >= halfThreshold) {
                state = SlotState.HALF;
            } else {
                state = SlotState.EMPTY;
            }
            states[i] = state;
            if (state != SlotState.EMPTY) {
                anyVisible = true;
            }
        }

        // A mob that is still alive must show at least a sliver of health,
        // even if its fraction of a single slot rounds down to empty.
        if (clampedHealth > 0 && !anyVisible) {
            states[0] = SlotState.HALF;
        }

        return buildComponent(states);
    }

    private static int slotCount(double maxHealth) {
        if (maxHealth <= 0) {
            return 0;
        }
        return (int) Math.min(Math.ceil(maxHealth / VANILLA_HP_PER_HEART), MAX_SLOTS);
    }

    private static Component buildComponent(SlotState[] states) {
        Component result = Component.empty();
        int i = 0;
        while (i < states.length) {
            SlotState current = states[i];
            int runLength = 1;
            while (i + runLength < states.length && states[i + runLength] == current) {
                runLength++;
            }
            result = result.append(Component.text("❤".repeat(runLength), current.color));
            i += runLength;
        }
        return result;
    }
}

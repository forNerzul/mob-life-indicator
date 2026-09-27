package com.fornerzul.moblifeindicator;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the MobLifeIndicator plugin.
 *
 * <p>Shows a floating heart bar (as the entity's custom name) above any
 * damaged {@link org.bukkit.entity.LivingEntity} for a short window after the
 * last hit, then restores the entity's original name and visibility.
 */
public final class MobLifeIndicatorPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("MobLifeIndicator enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MobLifeIndicator disabled.");
    }
}

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

    private HealthDisplayTracker tracker;

    @Override
    public void onEnable() {
        tracker = new HealthDisplayTracker(this);
        HeartBar heartBar = new HeartBar();
        getServer().getPluginManager().registerEvents(new DamageListener(this, tracker, heartBar), this);
        getLogger().info("MobLifeIndicator enabled.");
    }

    @Override
    public void onDisable() {
        if (tracker != null) {
            tracker.restoreAllTracked();
        }
        getLogger().info("MobLifeIndicator disabled.");
    }
}

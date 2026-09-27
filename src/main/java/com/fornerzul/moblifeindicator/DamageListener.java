package com.fornerzul.moblifeindicator;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Wires Bukkit events to {@link HealthDisplayTracker}: show the heart bar on
 * damage, refresh it on heal, and restore the mob's real name on death, on
 * a name-tag rename, and when a stale bar is found on entity load.
 */
public final class DamageListener implements Listener {

    private final Plugin plugin;
    private final HealthDisplayTracker tracker;
    private final HeartBar heartBar;

    public DamageListener(Plugin plugin, HealthDisplayTracker tracker, HeartBar heartBar) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.heartBar = heartBar;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !isEligible(living)) {
            return;
        }

        // EntityDamageEvent fires BEFORE the damage is subtracted from
        // getHealth(), so getHealth() still reports the pre-hit value here.
        // We compute the post-hit health analytically (getHealth() -
        // getFinalDamage()) instead of scheduling a next-tick read: it keeps
        // the bar update on the same tick as the hit (no one-tick flash of
        // stale health) and avoids scheduling an extra per-hit task.
        double healthAfterDamage = living.getHealth() - event.getFinalDamage();
        if (healthAfterDamage <= 0) {
            // Dying/dead: EntityDeathEvent will restore the original name.
            return;
        }

        tracker.show(living, heartBar.render(healthAfterDamage, maxHealth(living)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !tracker.isTracked(living)) {
            return;
        }
        double max = maxHealth(living);
        double newHealth = Math.min(living.getHealth() + event.getAmount(), max);
        tracker.show(living, heartBar.render(newHealth, max));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // Default priority so this runs before any MONITOR-priority listener
        // (e.g. a death-log plugin) reads the entity's display name for a
        // death message; the mob's real name is restored first.
        tracker.restoreImmediately(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onNameTag(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity living) || !isEligible(living)) {
            return;
        }
        if (!tracker.isTracked(living)) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (item == null || item.getType() != Material.NAME_TAG) {
            return;
        }
        // Vanilla applies the rename right after this event resolves
        // (assuming it isn't cancelled), so read the new name back next
        // tick instead of racing it here.
        living.getScheduler().run(plugin, task -> tracker.captureRenamedOriginal(living), null);
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living) {
            tracker.restoreIfStale(living);
        }
    }

    static boolean isEligible(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof ArmorStand);
    }

    private static double maxHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : entity.getHealth();
    }
}

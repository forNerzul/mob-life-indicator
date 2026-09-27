package com.fornerzul.moblifeindicator;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows and hides the heart-bar custom name on a mob, preserving whatever
 * name/visibility it had before the bar was shown and restoring it once the
 * bar hides.
 *
 * <p>Original state is written to the entity's {@link PersistentDataContainer}
 * as soon as a mob starts being tracked, and removed once it is restored.
 * That PDC marker is also the source of truth for crash recovery: if the
 * server stops while a bar is showing, {@link #restoreIfStale(LivingEntity)}
 * (wired to {@code EntityAddToWorldEvent}) restores it the next time the
 * entity is loaded, even though the in-memory state below was lost.
 */
public final class HealthDisplayTracker {

    /** 5 seconds, matching the acceptance criteria. */
    static final long HIDE_DELAY_TICKS = 100L;

    private final Plugin plugin;
    private final NamespacedKey hasNameKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey nameVisibleKey;

    /** Mobs currently showing a bar in this run, keyed by entity UUID. */
    private final Map<UUID, TrackedMob> tracked = new ConcurrentHashMap<>();

    public HealthDisplayTracker(Plugin plugin) {
        this.plugin = plugin;
        this.hasNameKey = new NamespacedKey(plugin, "original_has_name");
        this.nameKey = new NamespacedKey(plugin, "original_name");
        this.nameVisibleKey = new NamespacedKey(plugin, "original_name_visible");
    }

    /**
     * True if the entity's PDC still carries the "original state" marker,
     * meaning a bar is (or was, before a crash) showing on it.
     */
    public boolean isTracked(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(nameVisibleKey, PersistentDataType.BOOLEAN);
    }

    /**
     * Shows (or refreshes) the heart bar on the entity and (re)schedules the
     * hide timer, cancelling any previous one.
     */
    public void show(LivingEntity entity, Component hearts) {
        if (!isTracked(entity)) {
            captureOriginal(entity);
        }
        entity.customName(hearts);
        entity.setCustomNameVisible(true);
        scheduleHide(entity);
    }

    private void captureOriginal(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Component original = entity.customName();
        pdc.set(hasNameKey, PersistentDataType.BOOLEAN, original != null);
        if (original != null) {
            pdc.set(nameKey, PersistentDataType.STRING, GsonComponentSerializer.gson().serialize(original));
        } else {
            pdc.remove(nameKey);
        }
        pdc.set(nameVisibleKey, PersistentDataType.BOOLEAN, entity.isCustomNameVisible());
    }

    private void scheduleHide(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        // Entity schedulers (rather than the global/Bukkit scheduler) keep
        // this Folia-safe: the delayed task always runs on the thread that
        // owns the entity's region.
        ScheduledTask task = entity.getScheduler().runDelayed(
                plugin,
                scheduledTask -> hide(entity),
                null,
                HIDE_DELAY_TICKS);
        TrackedMob previous = (task != null)
                ? tracked.put(id, new TrackedMob(entity, task))
                : tracked.remove(id);
        if (previous != null) {
            previous.hideTask().cancel();
        }
    }

    /** Hides the bar (timer elapsed) and restores the original name. */
    public void hide(LivingEntity entity) {
        tracked.remove(entity.getUniqueId());
        restore(entity);
    }

    /**
     * Restores immediately, bypassing the timer. Used for death and plugin
     * disable, where waiting 5 more seconds would leave a stale bar.
     */
    public void restoreImmediately(LivingEntity entity) {
        TrackedMob previous = tracked.remove(entity.getUniqueId());
        if (previous != null) {
            previous.hideTask().cancel();
        }
        restore(entity);
    }

    private void restore(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(nameVisibleKey, PersistentDataType.BOOLEAN)) {
            return;
        }
        boolean hadName = pdc.getOrDefault(hasNameKey, PersistentDataType.BOOLEAN, false);
        if (hadName) {
            String json = pdc.get(nameKey, PersistentDataType.STRING);
            entity.customName(json != null ? GsonComponentSerializer.gson().deserialize(json) : null);
        } else {
            entity.customName(null);
        }
        entity.setCustomNameVisible(pdc.getOrDefault(nameVisibleKey, PersistentDataType.BOOLEAN, false));
        pdc.remove(hasNameKey);
        pdc.remove(nameKey);
        pdc.remove(nameVisibleKey);
    }

    /**
     * Crash recovery hook: call for every entity as it is added to a world
     * (e.g. on chunk load). If it still carries the original-state marker
     * from before a server crash/kill, restore it right away.
     */
    public void restoreIfStale(LivingEntity entity) {
        if (isTracked(entity)) {
            restoreImmediately(entity);
        }
    }

    /** Restores every mob still tracked in this run. Call on plugin disable. */
    public void restoreAllTracked() {
        List<TrackedMob> snapshot = new ArrayList<>(tracked.values());
        for (TrackedMob mob : snapshot) {
            restoreImmediately(mob.entity());
        }
    }

    /**
     * After a player renames a tracked (bar-visible) mob with a name tag,
     * vanilla overwrites the entity's actual custom name with the tag text.
     * Called one tick later (once that rename has applied) so the stored
     * "original name" becomes the new tag name instead of the pre-rename
     * one. See README for the known display limitation this leaves.
     */
    public void captureRenamedOriginal(LivingEntity entity) {
        if (!isTracked(entity)) {
            return;
        }
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Component currentName = entity.customName();
        pdc.set(hasNameKey, PersistentDataType.BOOLEAN, currentName != null);
        if (currentName != null) {
            pdc.set(nameKey, PersistentDataType.STRING, GsonComponentSerializer.gson().serialize(currentName));
        } else {
            pdc.remove(nameKey);
        }
    }

    private record TrackedMob(LivingEntity entity, ScheduledTask hideTask) {
    }
}

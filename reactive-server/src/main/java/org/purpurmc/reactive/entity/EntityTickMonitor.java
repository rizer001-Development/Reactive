package org.purpurmc.reactive.entity;

import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * EntityTickMonitor — monitors per-entity-type tick time consumption and manages
 * paused entity types that should skip their {@code tick()} during server overload.
 * <p>
 * Called from:
 * <ul>
 *   <li>{@code ServerLevel.tickNonPassenger()} — per-entity timing instrumentation</li>
 *   <li>{@code EntityTickLimiterTask} — periodic analysis and pause decisions</li>
 * </ul>
 * <p>
 * Thread safety: all public methods are safe to call from the server thread only
 * (entity ticking runs on the main server thread).
 */
public final class EntityTickMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger("EntityTickMonitor");

    private EntityTickMonitor() {}

    // ──────────────────────────────────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────────────────────────────────

    /** Whether monitoring and pausing is globally enabled. */
    public static volatile boolean enabled = true;

    /**
     * Entity type names (via {@code entity.getClass().getSimpleName()}) that
     * should skip their {@code tick()} call. Managed by {@code EntityTickLimiterTask}.
     */
    private static final Set<String> pausedEntityTypes = ConcurrentHashMap.newKeySet();

    /**
     * Nanosecond accumulator per entity type for the current tick window.
     * Reset periodically by {@code EntityTickLimiterTask}.
     */
    private static final ConcurrentHashMap<String, LongAdder> typeTickNanos = new ConcurrentHashMap<>();

    /** Per-entity start timestamp (set in tickNonPassenger before entity.tick()). */
    private static final ConcurrentHashMap<Integer, Long> entityStartNanos = new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────────────────────────────────
    // Called from ServerLevel.tickNonPassenger() at the start
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records the start of an entity's tick. Called at the beginning of
     * {@code ServerLevel.tickNonPassenger()} before entity processing.
     */
    public static void startEntityTick(final Entity entity) {
        if (!enabled) return;
        entityStartNanos.put(entity.getId(), System.nanoTime());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Called from ServerLevel.tickNonPassenger() to decide whether to skip
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given entity's type is currently paused
     * (its tick should be skipped to reduce server load).
     */
    public static boolean shouldSkipTick(final Entity entity) {
        if (!enabled) return false;
        return pausedEntityTypes.contains(entity.getClass().getSimpleName());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Called from ServerLevel.tickNonPassenger() after entity.tick()
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records the elapsed tick time for the given entity. Computes the duration
     * from the start timestamp stored by {@link #startEntityTick(Entity)}.
     */
    public static void endEntityTick(final Entity entity) {
        if (!enabled) return;

        final Integer id = entity.getId();
        final Long startNanos = entityStartNanos.remove(id);
        if (startNanos == null) return;

        final long elapsed = System.nanoTime() - startNanos;
        final String typeName = entity.getClass().getSimpleName();

        typeTickNanos
            .computeIfAbsent(typeName, k -> new LongAdder())
            .add(elapsed);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Called from EntityTickLimiterTask
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of accumulated tick times per entity type and resets
     * the counters for the next window.
     */
    public static Map<String, Long> getSnapshotAndReset() {
        final Map<String, Long> snapshot = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : typeTickNanos.entrySet()) {
            final long value = entry.getValue().sumThenReset();
            if (value > 0) {
                snapshot.put(entry.getKey(), value);
            }
        }
        return snapshot;
    }

    /**
     * Pauses (adds to skip set) the given entity types by class simple name.
     */
    public static void pauseEntityTypes(final Set<String> typeNames) {
        pausedEntityTypes.addAll(typeNames);
    }

    /**
     * Unpauses (removes from skip set) the given entity types.
     */
    public static void unpauseEntityTypes(final Set<String> typeNames) {
        pausedEntityTypes.removeAll(typeNames);
    }

    /**
     * Unpauses ALL currently paused entity types.
     */
    public static void unpauseAll() {
        pausedEntityTypes.clear();
    }

    /**
     * Returns a copy of the set of currently paused entity type names.
     */
    public static Set<String> getPausedEntityTypes() {
        return Set.copyOf(pausedEntityTypes);
    }

    /**
     * Returns {@code true} if any entity types are currently paused.
     */
    public static boolean hasPausedEntityTypes() {
        return !pausedEntityTypes.isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    /** Resets all state (counters, paused types, start timestamps). */
    public static void resetAll() {
        enabled = true;
        pausedEntityTypes.clear();
        typeTickNanos.clear();
        entityStartNanos.clear();
    }
}

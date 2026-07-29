package org.purpurmc.reactive.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.purpurmc.purpur.util.MinecraftInternalPlugin;
import org.purpurmc.reactive.config.ReactiveConfig;
import org.purpurmc.reactive.entity.EntityTickMonitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * EntityTickLimiterTask — monitors server MSPT and per-entity-type tick time
 * consumption. When MSPT exceeds the configured threshold and a specific entity
 * type contributes a significant portion of tick time, that entity type is
 * "paused" (its {@code tick()} call is skipped) until the server recovers.
 * <p>
 * All thresholds, cooldown, and permission are configurable via
 * {@code config/reactive-config.yml} → {@code reactive.entity-tick-limiter}.
 */
public class EntityTickLimiterTask extends BukkitRunnable {

    private static EntityTickLimiterTask instance;
    private long lastAnnouncementTime = 0;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private EntityTickLimiterTask() {}

    public static EntityTickLimiterTask instance() {
        if (instance == null) {
            instance = new EntityTickLimiterTask();
        }
        return instance;
    }

    @Override
    public void run() {
        if (!ReactiveConfig.entityTickLimiterEnabled) return;

        final double mspt = Bukkit.getAverageTickTime();
        final long now = System.currentTimeMillis();
        final long cooldownMs = Math.max(1000L, ReactiveConfig.entityTickLimiterCooldownSeconds * 1000L);

        // ── Check if server is overloaded ──
        if (mspt > ReactiveConfig.entityTickLimiterThreshold) {
            // Read accumulated tick time data
            final Map<String, Long> snapshot = EntityTickMonitor.getSnapshotAndReset();

            if (snapshot.isEmpty()) {
                // No data yet — try again next cycle
                return;
            }

            // Calculate total tick time across all entity types
            long totalNanos = 0;
            for (long nanos : snapshot.values()) {
                totalNanos += nanos;
            }

            if (totalNanos <= 0) return;

            // Find entity types that consumed more than minContributionPct of total tick time
            final double thresholdPct = ReactiveConfig.entityTickLimiterMinContributionPercent / 100.0D;
            final Set<String> typesToPause = new HashSet<>();

            for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
                final double contributionPct = (double) entry.getValue() / (double) totalNanos;
                if (contributionPct >= thresholdPct) {
                    typesToPause.add(entry.getKey());
                }
            }

            if (!typesToPause.isEmpty()) {
                // Pause these entity types
                EntityTickMonitor.pauseEntityTypes(typesToPause);

                // Announce — respect cooldown
                if (now - lastAnnouncementTime >= cooldownMs) {
                    lastAnnouncementTime = now;
                    announcePaused(typesToPause, mspt);
                }
            }
        } else {
            // Server is running fine — unpause all
            if (EntityTickMonitor.hasPausedEntityTypes()) {
                EntityTickMonitor.unpauseAll();

                // Announce recovery with cooldown
                if (now - lastAnnouncementTime >= cooldownMs) {
                    lastAnnouncementTime = now;
                    announceRecovered();
                }
            }
        }
    }

    private void announcePaused(final Set<String> types, final double mspt) {
        final String typeList = String.join("<gray>, </gray><white>", types);
        final String message = "<red>⚠ Performance Alert!</red> <gray>MSPT: </gray><yellow>"
            + String.format(Locale.US, "%.1f", mspt)
            + "</yellow><gray> ms — Pausing tick for: </gray><white>"
            + typeList
            + "</white>";

        final Component msg = MM.deserialize(message);
        final String permission = ReactiveConfig.entityTickLimiterPermission;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(msg);
            }
        }
    }

    private void announceRecovered() {
        final String message = "<green>✔ Server recovered!</green> <gray>All entity types resumed normal ticking.</gray>";

        final Component msg = MM.deserialize(message);
        final String permission = ReactiveConfig.entityTickLimiterPermission;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(msg);
            }
        }
    }

    public void start() {
        if (getTaskId() != -1) return;
        long interval = Math.max(1, ReactiveConfig.entityTickLimiterCheckIntervalTicks);
        this.runTaskTimer(new MinecraftInternalPlugin(), interval, interval);
    }

    public static void startTask() {
        instance().start();
    }

    public static void stopTask() {
        if (instance != null) {
            instance.cancel();
            instance = null;
        }
        EntityTickMonitor.resetAll();
    }
}

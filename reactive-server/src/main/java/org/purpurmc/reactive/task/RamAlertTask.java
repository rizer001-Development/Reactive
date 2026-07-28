package org.purpurmc.reactive.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.purpurmc.purpur.util.MinecraftInternalPlugin;
import org.purpurmc.reactive.config.ReactiveConfig;

import java.util.Locale;

/**
 * RamAlertTask — monitors server JVM memory usage and sends
 * warnings to players with the configured permission.
 * <p>
 * <ul>
 *   <li><b>RAM usage &gt; 90%</b> — critical alert (red)</li>
 *   <li><b>RAM usage &gt; 80%</b> — high usage warning (gold)</li>
 *   <li><b>Cooldown:</b> 10 seconds between warnings to prevent spam</li>
 * </ul>
 * <p>
 * All thresholds, cooldown, and permission are configurable via
 * {@code config/reactive-config.yml} → {@code reactive.ram-alert}.
 */
public class RamAlertTask extends BukkitRunnable {

    private static RamAlertTask instance;
    private long lastWarningTime = 0;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private RamAlertTask() {}

    /**
     * Returns the singleton instance, creating it if necessary.
     */
    public static RamAlertTask instance() {
        if (instance == null) {
            instance = new RamAlertTask();
        }
        return instance;
    }

    @Override
    public void run() {
        // Quick check — disabled?
        if (!ReactiveConfig.ramAlertEnabled) return;

        final Runtime runtime = Runtime.getRuntime();
        final long maxMemory = runtime.maxMemory();
        if (maxMemory <= 0) return;

        final long totalMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final long usedMemory = totalMemory - freeMemory;
        final double usagePct = (double) usedMemory / (double) maxMemory * 100.0D;

        final long now = System.currentTimeMillis();
        // Enforce minimum 1-second cooldown
        final long cooldownMs = Math.max(1000L, ReactiveConfig.ramAlertCooldownSeconds * 1000L);

        // Respect cooldown
        if (now - lastWarningTime < cooldownMs) return;

        // Format GB values once (avoids duplication in both branches)
        final String usedGb = String.format(Locale.US, "%.1f", usedMemory / (1024.0D * 1024.0D * 1024.0D));
        final String maxGb = String.format(Locale.US, "%.1f", maxMemory / (1024.0D * 1024.0D * 1024.0D));
        final String pct = String.format(Locale.US, "%.0f", usagePct);

        final String message;
        if (usagePct > ReactiveConfig.ramAlertCriticalThreshold) {
            message = "<red>⚠ Critical RAM usage!</red> <gray>"
                + usedGb + "GB / " + maxGb + "GB (</gray><yellow>" + pct + "%</yellow><gray>)</gray>";
        } else if (usagePct > ReactiveConfig.ramAlertWarningThreshold) {
            message = "<gold>⚡ High RAM usage!</gold> <gray>"
                + usedGb + "GB / " + maxGb + "GB (</gray><yellow>" + pct + "%</yellow><gray>)</gray>";
        } else {
            return;
        }

        lastWarningTime = now;
        final Component msg = MM.deserialize(message);
        final String permission = ReactiveConfig.ramAlertPermission;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(msg);
            }
        }
    }

    /**
     * Starts the task, running at the configured interval.
     */
    public void start() {
        // Guard: prevent IllegalStateException if already scheduled
        if (getTaskId() != -1) return;
        long interval = Math.max(1, ReactiveConfig.ramAlertIntervalTicks);
        this.runTaskTimer(new MinecraftInternalPlugin(), interval, interval);
    }

    /**
     * Starts the RAM alert task (convenience for server boot).
     */
    public static void startTask() {
        instance().start();
    }

    /**
     * Stops the RAM alert task and clears the singleton.
     */
    public static void stopTask() {
        if (instance != null) {
            instance.cancel();
            instance = null;
        }
    }
}

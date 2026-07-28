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
 * MSPTAlertTask — monitors server MSPT (milliseconds per tick) and sends
 * warnings to players with the configured permission (default: {@code reactive.alerts}).
 * <p>
 * All thresholds, cooldown, and permission are configurable via
 * {@code config/reactive-config.yml} → {@code reactive.mspt-alert}.
 */
public class MSPTAlertTask extends BukkitRunnable {

    private static MSPTAlertTask instance;
    private long lastWarningTime = 0;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MSPTAlertTask() {}

    /**
     * Returns the singleton instance, creating it if necessary.
     */
    public static MSPTAlertTask instance() {
        if (instance == null) {
            instance = new MSPTAlertTask();
        }
        return instance;
    }

    @Override
    public void run() {
        // Quick check — disabled?
        if (!ReactiveConfig.msptAlertEnabled) return;

        final double mspt = Bukkit.getAverageTickTime();
        final long now = System.currentTimeMillis();
        // Enforce minimum 1-second cooldown
        final long cooldownMs = Math.max(1000L, ReactiveConfig.msptAlertCooldownSeconds * 1000L);

        // Respect cooldown
        if (now - lastWarningTime < cooldownMs) return;

        final String message;
        if (mspt > ReactiveConfig.msptAlertCriticalThreshold) {
            message = "<red>⚠ Server Overloaded!</red> <gray>MSPT: </gray><yellow>"
                + String.format(Locale.US, "%.1f", mspt) + "</yellow><gray> ms</gray>";
        } else if (mspt > ReactiveConfig.msptAlertWarningThreshold) {
            message = "<gold>⚡ High server load!</gold> <gray>MSPT: </gray><yellow>"
                + String.format(Locale.US, "%.1f", mspt) + "</yellow><gray> ms</gray>";
        } else {
            return;
        }

        lastWarningTime = now;
        final Component msg = MM.deserialize(message);
        final String permission = ReactiveConfig.msptAlertPermission;

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
        long interval = Math.max(1, ReactiveConfig.msptAlertIntervalTicks);
        this.runTaskTimer(new MinecraftInternalPlugin(), interval, interval);
    }

    /**
     * Starts the MSPT alert task (convenience for server boot).
     */
    public static void startTask() {
        instance().start();
    }

    /**
     * Stops the MSPT alert task and clears the singleton.
     */
    public static void stopTask() {
        if (instance != null) {
            instance.cancel();
            instance = null;
        }
    }
}

package org.purpurmc.reactive.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.purpurmc.purpur.util.MinecraftInternalPlugin;

/**
 * MSPTAlertTask — monitors server MSPT (milliseconds per tick) and sends
 * warnings to players with the {@code reactive.alerts} permission.
 * <p>
 * <ul>
 *   <li><b>MSPT &gt; 50</b> — critical overload warning (red)</li>
 *   <li><b>MSPT &gt; 40</b> — high load warning (gold)</li>
 *   <li><b>Cooldown:</b> 10 seconds between warnings to prevent spam</li>
 * </ul>
 */
public class MSPTAlertTask extends BukkitRunnable {

    private static MSPTAlertTask instance;
    private long lastWarningTime = 0;
    private static final long COOLDOWN_MS = 10_000L;
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
        final double mspt = Bukkit.getAverageTickTime();
        final long now = System.currentTimeMillis();

        // Respect cooldown
        if (now - lastWarningTime < COOLDOWN_MS) return;

        final String message;
        if (mspt > 50.0D) {
            message = "<red>⚠ Server Overloaded!</red> <gray>MSPT: </gray><yellow>"
                + String.format("%.1f", mspt) + "</yellow><gray> ms</gray>";
        } else if (mspt > 40.0D) {
            message = "<gold>⚡ High server load!</gold> <gray>MSPT: </gray><yellow>"
                + String.format("%.1f", mspt) + "</yellow><gray> ms</gray>";
        } else {
            return;
        }

        lastWarningTime = now;
        final Component msg = MM.deserialize(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("reactive.alerts")) {
                player.sendMessage(msg);
            }
        }
    }

    /**
     * Starts the task, running every 20 ticks (1 second).
     */
    public void start() {
        this.runTaskTimer(new MinecraftInternalPlugin(), 20L, 20L);
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

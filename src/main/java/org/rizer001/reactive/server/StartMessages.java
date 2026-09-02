package org.rizer001.reactive.server;

import org.rizer001.reactive.config.ReactiveConfig;
import org.rizer001.reactive.command.CommandRegistry;
import org.rizer001.reactive.command.ReactiveCommands;
import org.rizer001.reactive.gamerules.ReactiveGameRuleManager;

/**
 * Reactive Server 26.2 — Entry point
 */
public class StartMessages {
    public static void main(String[] args) {
        System.out.println("[Reactive] By using this server core, you automatically agree to the license (AGPLv3).");
        System.out.println("[Reactive] Starting the Reactive server...");
        
        // Load Reactive config (creates reactive.toml from default on first run)
        ReactiveConfig.load();
        
        // Initialize game rules manager (creates gamerules.toml from default on first run)
        ReactiveGameRuleManager.init();
        
        // Start auto-save with the configured interval
        int autosaveInterval = ReactiveConfig.getInstance().getGamerulesAutosaveIntervalSeconds();
        ReactiveGameRuleManager.startAutoSave(autosaveInterval);
        
        // Register /reactive command hook (stored in CommandRegistry, 
        // fired when vanilla Commands constructor runs — no vanilla class loading here)
        CommandRegistry.registerHook(ReactiveCommands::register);
        
        // Add shutdown hook to save game rules before JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ReactiveGameRuleManager.saveAll();
                ReactiveGameRuleManager.stopAutoSave();
            } catch (Exception e) {
                System.out.println("[Reactive] Shutdown hook error: " + e.getMessage());
            }
        }, "Reactive-Shutdown"));
        
        net.minecraft.server.Main.main(args);
    }
}

package org.rizer001.reactive.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Central hook registry for Reactive commands.
 * Called from the vanilla Commands constructor (2-line vanilla change).
 * 
 * Hook registration is done BEFORE Main.main() via StartMessages.
 * The hooks are stored here (no vanilla class loading needed).
 */
public class CommandRegistry {
    
    private static final List<Consumer<CommandDispatcher<CommandSourceStack>>> hooks = new ArrayList<>();
    
    /**
     * Register a command hook. Called from StartMessages before server starts.
     */
    public static void registerHook(Consumer<CommandDispatcher<CommandSourceStack>> hook) {
        hooks.add(hook);
    }
    
    /**
     * Called from Commands constructor (vanilla code, 2 lines).
     * Registers all hooks on the dispatcher.
     */
    public static void fireHooks(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Consumer<CommandDispatcher<CommandSourceStack>> hook : hooks) {
            hook.accept(dispatcher);
        }
    }
}

package org.bukkit.command;

/**
 * Minimal compile-time stub of org.bukkit.command.CommandMap (Paper API)。
 */
public interface CommandMap {

    boolean register(String fallbackPrefix, Command command);

    boolean register(String fallbackPrefix, String label, Command command);

    boolean dispatch(CommandSender sender, String commandLine);

    void clearCommands();
}

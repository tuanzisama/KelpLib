package com.velocitypowered.api.command;

/**
 * Minimal compile-time stub of com.velocitypowered.api.command.CommandManager.
 */
public interface CommandManager {

    void register(CommandMeta meta, Command... commands);

    void unregister(String alias);

    boolean hasCommand(String alias);
}

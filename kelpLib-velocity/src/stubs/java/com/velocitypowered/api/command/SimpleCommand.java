package com.velocitypowered.api.command;

import java.util.List;

/**
 * Minimal compile-time stub of com.velocitypowered.api.command.SimpleCommand.
 */
public interface SimpleCommand extends Command {

    @Override
    void execute(Invocation invocation);

    default boolean hasPermission(Invocation invocation) {
        return true;
    }

    default List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

package com.velocitypowered.api.command;

/**
 * Minimal compile-time stub of com.velocitypowered.api.command.Command.
 */
public interface Command {

    void execute(Invocation invocation);

    interface Invocation {

        CommandSource source();

        String alias();

        String[] arguments();
    }
}

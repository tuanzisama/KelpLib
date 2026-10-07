package com.velocitypowered.api.command;

/**
 * Minimal compile-time stub of com.velocitypowered.api.command.CommandMeta.
 */
public interface CommandMeta {

    static Builder builder(String alias) {
        throw new UnsupportedOperationException("stub");
    }

    interface Builder {

        Builder aliases(String... aliases);

        Builder plugin(Object plugin);

        CommandMeta build();
    }
}

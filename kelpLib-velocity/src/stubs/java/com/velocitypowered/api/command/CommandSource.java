package com.velocitypowered.api.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.pointer.Pointers;

/**
 * Minimal compile-time stub of com.velocitypowered.api.command.CommandSource.
 */
public interface CommandSource extends Audience {

    @Override
    default Pointers pointers() {
        return Pointers.empty();
    }

    boolean hasPermission(String permission);
}

package org.bukkit.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.pointer.Pointers;

/**
 * Minimal compile-time stub of org.bukkit.command.CommandSender (Paper: Audience)。
 */
public interface CommandSender extends Audience {

    @Override
    default Pointers pointers() {
        return Pointers.empty();
    }

    void sendMessage(String message);

    boolean hasPermission(String name);

    String getName();
}

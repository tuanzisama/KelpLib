package org.bukkit.scoreboard;

import net.kyori.adventure.text.Component;

/**
 * Minimal compile-time stub of org.bukkit.scoreboard.Team.
 */
public interface Team {

    String getName();

    void addEntry(String entry) throws IllegalStateException;

    void prefix(Component prefix) throws IllegalStateException;

    void suffix(Component suffix) throws IllegalStateException;

    void unregister() throws IllegalStateException;
}

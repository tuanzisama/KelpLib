package org.bukkit.scoreboard;

import net.kyori.adventure.text.Component;

/**
 * Minimal compile-time stub of org.bukkit.scoreboard.Objective.
 */
public interface Objective {

    String getName();

    Component displayName();

    void displayName(Component displayName);

    void setDisplaySlot(DisplaySlot slot);

    Score getScore(String entry) throws IllegalArgumentException;

    void unregister() throws IllegalStateException;
}

package org.bukkit.scoreboard;

import net.kyori.adventure.text.Component;

/**
 * Minimal compile-time stub of org.bukkit.scoreboard.Scoreboard.
 */
public interface Scoreboard {

    Objective registerNewObjective(String name, String criteria, Component displayName);

    Team registerNewTeam(String name);

    void clearSlot(org.bukkit.scoreboard.DisplaySlot slot);

    void resetScores(String entry) throws IllegalArgumentException;
}

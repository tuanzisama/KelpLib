package org.bukkit.scoreboard;

/**
 * Minimal compile-time stub of org.bukkit.scoreboard.Score.
 */
public interface Score {

    void setScore(int score) throws IllegalStateException;

    int getScore() throws IllegalStateException;

    boolean isScoreSet() throws IllegalStateException;

    String getEntry();
}

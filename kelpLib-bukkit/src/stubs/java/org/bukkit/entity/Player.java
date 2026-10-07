package org.bukkit.entity;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.pointer.Pointers;

import org.bukkit.Location;

import java.util.Locale;

/**
 * Minimal compile-time stub of org.bukkit.entity.Player (Paper 侧 Player 同时是 Audience)。
 */
public interface Player extends HumanEntity, Audience {

    @Override
    default net.kyori.adventure.pointer.Pointers pointers() {
        return Pointers.empty();
    }

    Locale locale();

    boolean teleport(Location location);

    boolean hasPermission(String name);

    boolean sendPluginMessage(org.bukkit.plugin.Plugin source, String channel, byte[] message);

    boolean isOnline();

    void setScoreboard(org.bukkit.scoreboard.Scoreboard scoreboard);

    org.bukkit.scoreboard.Scoreboard getScoreboard();
}

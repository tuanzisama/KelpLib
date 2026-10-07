package org.bukkit;

import org.bukkit.command.CommandMap;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Collection;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;

/**
 * Minimal compile-time stub of org.bukkit.Server.
 */
public interface Server {

    BukkitScheduler getScheduler();

    GlobalRegionScheduler getGlobalRegionScheduler();

    RegionScheduler getRegionScheduler();

    AsyncScheduler getAsyncScheduler();

    PluginManager getPluginManager();

    ScoreboardManager getScoreboardManager();

    ConsoleCommandSender getConsoleSender();

    CommandMap getCommandMap();

    Messenger getMessenger();

    Collection<? extends Player> getOnlinePlayers();

    World getWorld(String name);
}

package org.bukkit;

// 编译期桩（compile-only，不打包进产物 jar）：镜像本环境不可达的 paper-api 子集。
// 仅声明 KelpLib 用到的成员；运行时使用服务器提供的真实实现。

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
 * Minimal compile-time stub of org.bukkit.Bukkit.
 */
public final class Bukkit {

    private Bukkit() {
    }

    public static Server getServer() {
        throw new UnsupportedOperationException("stub");
    }

    public static BukkitScheduler getScheduler() {
        return getServer().getScheduler();
    }

    public static GlobalRegionScheduler getGlobalRegionScheduler() {
        return getServer().getGlobalRegionScheduler();
    }

    public static RegionScheduler getRegionScheduler() {
        return getServer().getRegionScheduler();
    }

    public static AsyncScheduler getAsyncScheduler() {
        return getServer().getAsyncScheduler();
    }

    public static PluginManager getPluginManager() {
        return getServer().getPluginManager();
    }

    public static ScoreboardManager getScoreboardManager() {
        return getServer().getScoreboardManager();
    }

    public static ConsoleCommandSender getConsoleSender() {
        return getServer().getConsoleSender();
    }

    public static CommandMap getCommandMap() {
        return getServer().getCommandMap();
    }

    public static Messenger getMessenger() {
        return getServer().getMessenger();
    }

    public static Collection<? extends Player> getOnlinePlayers() {
        return getServer().getOnlinePlayers();
    }

    public static World getWorld(String name) {
        return getServer().getWorld(name);
    }
}

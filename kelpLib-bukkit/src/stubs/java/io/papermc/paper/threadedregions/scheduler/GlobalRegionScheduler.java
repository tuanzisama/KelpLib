package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Minimal compile-time stub of Folia's GlobalRegionScheduler.
 */
public interface GlobalRegionScheduler {

    void run(Plugin plugin, Consumer<ScheduledTask> task);

    void runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks);

    void runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    void cancelTasks(Plugin plugin);
}

package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Minimal compile-time stub of Folia's RegionScheduler.
 */
public interface RegionScheduler {

    void run(Plugin plugin, Location location, Consumer<ScheduledTask> task);

    void runDelayed(Plugin plugin, Location location, Consumer<ScheduledTask> task, long delayTicks);

    void runAtFixedRate(Plugin plugin, Location location, Consumer<ScheduledTask> task,
                        long initialDelayTicks, long periodTicks);
}

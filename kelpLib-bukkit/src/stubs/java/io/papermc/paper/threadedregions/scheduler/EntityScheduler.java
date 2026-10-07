package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Minimal compile-time stub of Folia's EntityScheduler.
 */
public interface EntityScheduler {

    ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired);

    ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long delayTicks);

    ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired,
                                 long initialDelayTicks, long periodTicks);
}

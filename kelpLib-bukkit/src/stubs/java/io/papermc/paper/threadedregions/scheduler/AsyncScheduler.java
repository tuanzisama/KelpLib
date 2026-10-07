package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Minimal compile-time stub of Folia's AsyncScheduler.
 */
public interface AsyncScheduler {

    void runNow(Plugin plugin, Consumer<ScheduledTask> task);

    void runDelayed(Plugin plugin, Consumer<ScheduledTask> task, Duration delay);

    void runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, Duration initialDelay, Duration period);

    void cancelTasks(Plugin plugin);
}

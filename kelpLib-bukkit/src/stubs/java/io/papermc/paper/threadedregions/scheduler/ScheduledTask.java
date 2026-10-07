package io.papermc.paper.threadedregions.scheduler;

/**
 * Minimal compile-time stub of Folia's ScheduledTask.
 */
public interface ScheduledTask {

    boolean cancel();

    boolean isCancelled();
}

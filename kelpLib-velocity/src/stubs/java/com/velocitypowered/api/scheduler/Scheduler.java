package com.velocitypowered.api.scheduler;

/**
 * Minimal compile-time stub of com.velocitypowered.api.scheduler.Scheduler.
 */
public interface Scheduler {

    TaskBuilder buildTask(Object plugin, Runnable runnable);

    <T> TaskBuilder buildTask(Object plugin, java.util.function.Supplier<T> supplier);

    void cancelTasks(Object plugin);
}

package com.velocitypowered.api.scheduler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Minimal compile-time stub of com.velocitypowered.api.scheduler.TaskBuilder.
 */
public interface TaskBuilder {

    TaskBuilder delay(Duration duration);

    TaskBuilder delay(long time, TimeUnit unit);

    TaskBuilder repeat(Duration duration);

    TaskBuilder repeat(long time, TimeUnit unit);

    TaskBuilder clearDelay();

    TaskBuilder clearRepeat();

    ScheduledTask schedule();
}

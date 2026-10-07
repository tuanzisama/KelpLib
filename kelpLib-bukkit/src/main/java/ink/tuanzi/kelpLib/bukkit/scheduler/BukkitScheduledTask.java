package ink.tuanzi.kelpLib.bukkit.scheduler;

import ink.tuanzi.kelpLib.api.scheduler.ScheduledTask;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ScheduledTask} 统一包装：Paper 走 BukkitTask，Folia 走 region ScheduledTask。
 * 取消状态自持（底层任务的取消为幂等转发）。
 */
final class BukkitScheduledTask implements ScheduledTask {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile Object handle; // org.bukkit.scheduler.BukkitTask 或 Folia ScheduledTask

    void attach(Object task) {
        this.handle = task;
        if (task == null) {
            cancelled.set(true);
            return;
        }
        if (cancelled.get()) {
            cancelHandle(task);
        }
    }

    void markCancelled() {
        cancelled.set(true);
    }

    @Override
    public boolean cancel() {
        if (cancelled.compareAndSet(false, true)) {
            Object task = handle;
            if (task != null) {
                cancelHandle(task);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    private static void cancelHandle(Object task) {
        if (task instanceof org.bukkit.scheduler.BukkitTask bukkitTask) {
            bukkitTask.cancel();
        } else if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask) {
            foliaTask.cancel();
        }
    }
}

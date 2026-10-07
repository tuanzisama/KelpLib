package ink.tuanzi.kelpLib.bukkit.scheduler;

import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Paper 调度实现（§5.3）：GLOBAL 走 BukkitScheduler 同步任务（主线程），ASYNC 走异步任务。
 */
public final class PaperScheduler implements KelpScheduler {

    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    static long toTicks(Duration duration) {
        return Math.max(1L, Math.round(duration.toMillis() / 50.0));
    }

    @Override
    public ScheduledTask run(Consumer<ScheduledTask> task) {
        return runLater(Duration.ZERO, task);
    }

    @Override
    public ScheduledTask runLater(Duration delay, Consumer<ScheduledTask> task) {
        BukkitScheduledTask handle = new BukkitScheduledTask();
        long ticks = toTicks(delay);
        Runnable body = () -> {
            if (!handle.isCancelled()) {
                task.accept(handle);
            }
        };
        if (ticks <= 0) {
            handle.attach(Bukkit.getScheduler().runTask(plugin, body));
        } else {
            handle.attach(Bukkit.getScheduler().runTaskLater(plugin, body, ticks));
        }
        return handle;
    }

    @Override
    public ScheduledTask runTimer(Duration initialDelay, Duration period, Consumer<ScheduledTask> task) {
        BukkitScheduledTask handle = new BukkitScheduledTask();
        handle.attach(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> {
                    if (!handle.isCancelled()) {
                        task.accept(handle);
                    }
                },
                toTicks(initialDelay), toTicks(period)));
        return handle;
    }

    @Override
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor());
    }

    @Override
    public Executor syncExecutor() {
        return runnable -> runLater(Duration.ZERO, task -> runnable.run());
    }

    @Override
    public Executor asyncExecutor() {
        return runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}

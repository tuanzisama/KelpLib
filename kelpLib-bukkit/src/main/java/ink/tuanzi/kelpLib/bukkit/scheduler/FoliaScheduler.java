package ink.tuanzi.kelpLib.bukkit.scheduler;

import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.scheduler.ScheduledTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Folia 调度实现（§5.3）：GLOBAL→GlobalRegionScheduler、ASYNC→AsyncScheduler（Duration 语义）。
 */
public final class FoliaScheduler implements KelpScheduler {

    private final Plugin plugin;
    private final GlobalRegionScheduler global;
    private final AsyncScheduler async;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.global = Bukkit.getGlobalRegionScheduler();
        this.async = Bukkit.getAsyncScheduler();
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
        Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask> adapted = foliaTask -> {
            if (!handle.isCancelled()) {
                task.accept(handle);
            }
        };
        if (ticks <= 0) {
            global.run(plugin, adapted);
        } else {
            global.runDelayed(plugin, adapted, ticks);
        }
        return handle;
    }

    @Override
    public ScheduledTask runTimer(Duration initialDelay, Duration period, Consumer<ScheduledTask> task) {
        BukkitScheduledTask handle = new BukkitScheduledTask();
        global.runAtFixedRate(plugin, foliaTask -> {
            if (!handle.isCancelled()) {
                task.accept(handle);
            }
        }, toTicks(initialDelay), toTicks(period));
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
        return runnable -> async.runNow(plugin, task -> runnable.run());
    }

    @Override
    public void cancelTasks() {
        global.cancelTasks(plugin);
        async.cancelTasks(plugin);
    }
}

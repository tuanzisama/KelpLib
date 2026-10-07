package ink.tuanzi.kelpLib.velocity.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.TaskBuilder;

import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.scheduler.ScheduledTask;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Velocity 调度实现（§5.4）：代理端无主线程/区域概念——GLOBAL 与 ASYNC 均映射到
 * VelocityScheduler 执行池（delay/repeat 用 Duration 表达）。
 */
public final class VelocityPlatformScheduler implements KelpScheduler {

    private final ProxyServer proxy;
    private final Object pluginInstance;

    public VelocityPlatformScheduler(ProxyServer proxy, Object pluginInstance) {
        this.proxy = proxy;
        this.pluginInstance = pluginInstance;
    }

    @Override
    public ScheduledTask run(Consumer<ScheduledTask> task) {
        return schedule(builder -> builder, task);
    }

    @Override
    public ScheduledTask runLater(Duration delay, Consumer<ScheduledTask> task) {
        return schedule(builder -> builder.delay(delay), task);
    }

    @Override
    public ScheduledTask runTimer(Duration initialDelay, Duration period, Consumer<ScheduledTask> task) {
        return schedule(builder -> builder.delay(initialDelay).repeat(period), task);
    }

    private ScheduledTask schedule(java.util.function.UnaryOperator<TaskBuilder> config,
                                   Consumer<ScheduledTask> task) {
        VelocityTaskHandle handle = new VelocityTaskHandle();
        TaskBuilder builder = config.apply(proxy.getScheduler().buildTask(pluginInstance, () -> {
            if (!handle.isCancelled()) {
                task.accept(handle);
            }
        }));
        handle.attach(builder.schedule());
        return handle;
    }

    @Override
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor());
    }

    @Override
    public Executor syncExecutor() {
        // Velocity 无主线程：sync 语义 = 调度器执行池（§5.4）
        return this::executeOnPool;
    }

    @Override
    public Executor asyncExecutor() {
        return this::executeOnPool;
    }

    private void executeOnPool(Runnable runnable) {
        proxy.getScheduler().buildTask(pluginInstance, runnable).schedule();
    }

    @Override
    public void cancelTasks() {
        proxy.getScheduler().cancelTasks(pluginInstance);
    }

    /** Velocity ScheduledTask 包装（幂等取消）。 */
    private static final class VelocityTaskHandle implements ScheduledTask {

        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile com.velocitypowered.api.scheduler.ScheduledTask handle;

        void attach(com.velocitypowered.api.scheduler.ScheduledTask task) {
            this.handle = task;
            if (task == null) {
                cancelled.set(true);
                return;
            }
            if (cancelled.get()) {
                task.cancel();
            }
        }

        @Override
        public boolean cancel() {
            if (cancelled.compareAndSet(false, true)) {
                com.velocitypowered.api.scheduler.ScheduledTask task = handle;
                if (task != null) {
                    task.cancel();
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
    }
}

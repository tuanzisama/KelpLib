package ink.tuanzi.kelpLib.core.promise;

import ink.tuanzi.kelpLib.api.promise.Promise;
import ink.tuanzi.kelpLib.api.promise.PromiseFactory;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Promise 引擎（平台无关）：注入平台 "同步/异步" 执行器与共享定时器。
 * 自身是 Terminable——close 时取消全部未完成的 Promise（插件 disable 语义，§6.2）。
 */
public final class CorePromiseFactory implements PromiseFactory, Terminable {

    private final Executor sync;
    private final Executor async;
    private final ScheduledExecutorService timer;
    private final Set<CorePromise<?>> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    /** @param sync  GLOBAL 作用域执行器 @param async ASYNC 作用域执行器 */
    public CorePromiseFactory(Executor sync, Executor async) {
        this.sync = sync;
        this.async = async;
        this.timer = Executors.newScheduledThreadPool(2, new NamedDaemonFactory("KelpLib-PromiseTimer"));
    }

    private <T> Promise<T> wrap(CompletableFuture<T> delegate) {
        CorePromise<T> promise = new CorePromise<>(delegate, sync, async, timer);
        if (!closed) {
            pending.add(promise);
            // 完成后自动移出 pending，避免集合无限增长
            promise.future().whenComplete((v, t) -> pending.remove(promise));
        } else {
            promise.close();
        }
        return promise;
    }

    @Override
    public <T> Promise<T> completed(T value) {
        return wrap(CompletableFuture.completedFuture(value));
    }

    @Override
    public <T> Promise<T> failed(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return wrap(future);
    }

    @Override
    public Promise<Void> start() {
        return completed(null);
    }

    @Override
    public <T> Promise<T> of(CompletableFuture<T> future) {
        return wrap(future);
    }

    @Override
    public <T> Promise<T> supply(Supplier<T> supplier) {
        return wrap(CompletableFuture.supplyAsync(supplier, async));
    }

    /** 关闭并取消全部未完成的 Promise（未完成的以 {@link java.util.concurrent.CancellationException} 异常完成）。 */
    @Override
    public void close() {
        closed = true;
        for (CorePromise<?> promise : pending) {
            promise.close();
        }
        pending.clear();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private static final class NamedDaemonFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger();

        private NamedDaemonFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}

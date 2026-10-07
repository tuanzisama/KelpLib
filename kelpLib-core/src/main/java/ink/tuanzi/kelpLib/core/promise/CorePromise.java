package ink.tuanzi.kelpLib.core.promise;

import ink.tuanzi.kelpLib.api.promise.Promise;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Promise 默认实现：包装 {@link CompletableFuture}，sync/async 语义经注入执行器映射，
 * 延迟与超时经共享定时器实现。close 幂等（取消底层 future）。
 */
final class CorePromise<T> implements Promise<T> {

    private final CompletableFuture<T> delegate;
    private final Executor sync;
    private final Executor async;
    private final ScheduledExecutorService timer;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    CorePromise(CompletableFuture<T> delegate, Executor sync, Executor async, ScheduledExecutorService timer) {
        this.delegate = delegate;
        this.sync = sync;
        this.async = async;
        this.timer = timer;
    }

    private <U> Promise<U> wrap(CompletableFuture<U> next) {
        return new CorePromise<>(next, sync, async, timer);
    }

    @Override
    public CompletableFuture<T> future() {
        return delegate;
    }

    @Override
    public <U> Promise<U> thenApplySync(Function<? super T, ? extends U> fn) {
        return wrap(delegate.thenApplyAsync(fn, sync));
    }

    @Override
    public <U> Promise<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return wrap(delegate.thenApplyAsync(fn, async));
    }

    @Override
    public Promise<T> thenRunSync(Runnable action) {
        return wrap(delegate.thenApplyAsync(value -> {
            action.run();
            return value;
        }, sync));
    }

    @Override
    public Promise<T> thenRunAsync(Runnable action) {
        return wrap(delegate.thenApplyAsync(value -> {
            action.run();
            return value;
        }, async));
    }

    @Override
    public Promise<Void> thenAcceptSync(Consumer<? super T> action) {
        return wrap(delegate.thenApplyAsync(value -> {
            action.accept(value);
            return null;
        }, sync));
    }

    @Override
    public Promise<Void> thenAcceptAsync(Consumer<? super T> action) {
        return wrap(delegate.thenApplyAsync(value -> {
            action.accept(value);
            return null;
        }, async));
    }

    @Override
    public <U> Promise<U> thenComposeSync(Function<? super T, Promise<U>> fn) {
        return wrap(delegate.thenComposeAsync(value -> fn.apply(value).future(), sync));
    }

    @Override
    public <U> Promise<U> thenComposeAsync(Function<? super T, Promise<U>> fn) {
        return wrap(delegate.thenComposeAsync(value -> fn.apply(value).future(), async));
    }

    @Override
    public Promise<T> thenDelay(Duration delay) {
        CompletableFuture<T> out = new CompletableFuture<>();
        delegate.whenComplete((value, throwable) -> {
            if (out.isDone()) {
                return;
            }
            if (throwable != null) {
                out.completeExceptionally(throwable);
                return;
            }
            timer.schedule(() -> out.complete(value), Math.max(0, delay.toMillis()), TimeUnit.MILLISECONDS);
        });
        return wrap(out);
    }

    @Override
    public Promise<T> timeout(Duration timeout) {
        CompletableFuture<T> out = new CompletableFuture<>();
        ScheduledFuture<?> watchdog = timer.schedule(
                () -> out.completeExceptionally(new TimeoutException("Promise timed out after " + timeout)),
                Math.max(0, timeout.toMillis()), TimeUnit.MILLISECONDS);
        delegate.whenComplete((value, throwable) -> {
            watchdog.cancel(false);
            if (throwable != null) {
                out.completeExceptionally(throwable);
            } else {
                out.complete(value);
            }
        });
        return wrap(out);
    }

    @Override
    public <U, V> Promise<V> combine(Promise<U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(delegate.thenCombineAsync(other.future(), fn, async));
    }

    @Override
    public Promise<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return wrap(delegate.exceptionallyAsync(fn, sync));
    }

    @Override
    public T join() {
        return delegate.join();
    }

    @Override
    public T join(Duration timeout) {
        try {
            return delegate.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new java.util.concurrent.CompletionException("Promise timed out after " + timeout, e);
        } catch (Exception e) {
            throw new IllegalStateException("Promise failed", e);
        }
    }

    @Override
    public void close() {
        if (cancelled.compareAndSet(false, true)) {
            delegate.cancel(true);
        }
    }

    @Override
    public boolean isClosed() {
        return cancelled.get();
    }
}

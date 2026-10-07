package ink.tuanzi.kelpLib.api.promise;

import ink.tuanzi.kelpLib.api.Kelp;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 跨线程链式 Promise（§6.2）：负责"一个值在哪个线程、何时产生"。
 *
 * <p>{@code sync} 语义三平台统一映射（Bukkit/Folia 为 GLOBAL 作用域执行器，Velocity 为调度器执行池），
 * {@code async} 语义为各平台异步池；延迟与超时一律用 {@link Duration} 表达。</p>
 *
 * <p><b>生命周期</b>：所有 Promise 默认纳入 Terminable 体系——插件 disable 时未完成的 Promise
 * 被取消并异常完成；也可通过 {@link #bindWith(ink.tuanzi.kelpLib.api.terminable.TerminableConsumer)} 绑定到更细的分组。</p>
 *
 * <pre>{@code
 * Promise<String> name = Promise.start()
 *     .thenApplySync(() -> resolveInput())
 *     .thenApplyAsync(storage::loadName)
 *     .thenDelay(Duration.ofSeconds(2))
 *     .timeout(Duration.ofSeconds(5));
 *
 * name.thenAccept(n -> messages.send(player, "welcome", ...));
 * }</pre>
 *
 * @param <T> 结果类型
 */
public interface Promise<T> extends Terminable {

    /**
     * 以一个已完成值开始链式调用。
     *
     * @param value 初始值
     * @param <T>   值类型
     * @return 已完成的 Promise
     */
    static <T> Promise<T> completed(T value) {
        return Kelp.platform().promiseFactory().completed(value);
    }

    /** 以异常完成态开始（供测试与错误传播）。 */
    static <T> Promise<T> failed(Throwable throwable) {
        return Kelp.platform().promiseFactory().failed(throwable);
    }

    /** 空起点（值为 {@code null} 的已完成 Promise），便于书写链式调用。 */
    static Promise<Void> start() {
        return Kelp.platform().promiseFactory().start();
    }

    /** 包装既有 future（取消传播：关闭 Promise 会取消底层 future）。 */
    static <T> Promise<T> of(CompletableFuture<T> future) {
        return Kelp.platform().promiseFactory().of(future);
    }

    /** 在 ASYNC 作用域执行 supplier 并开启链式调用。 */
    static <T> Promise<T> supply(Supplier<T> supplier) {
        return Kelp.platform().promiseFactory().supply(supplier);
    }

    /** 底层 {@link CompletableFuture}（与平台/标准库互操作）。 */
    CompletableFuture<T> future();

    /**
     * 平台 "同步" 语义映射函数（GLOBAL 作用域执行）。
     *
     * @param fn 转换函数
     * @param <U> 目标类型
     * @return 新 Promise
     */
    <U> Promise<U> thenApplySync(Function<? super T, ? extends U> fn);

    /** 异步池执行的转换函数。 */
    <U> Promise<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    /** 同步语义执行无返回值动作，链式值原样传递。 */
    Promise<T> thenRunSync(Runnable action);

    /** 异步池执行无返回值动作，链式值原样传递。 */
    Promise<T> thenRunAsync(Runnable action);

    /** 同步语义消费链式值（如回到主线程更新玩家展示）。 */
    Promise<Void> thenAcceptSync(Consumer<? super T> action);

    /** 异步池消费链式值。 */
    Promise<Void> thenAcceptAsync(Consumer<? super T> action);

    /** 同步语义衔接下一级 Promise（扁平化组合）。 */
    <U> Promise<U> thenComposeSync(Function<? super T, Promise<U>> fn);

    /** 异步池衔接下一级 Promise（扁平化组合）。 */
    <U> Promise<U> thenComposeAsync(Function<? super T, Promise<U>> fn);

    /**
     * 延迟继续：链式值在指定延迟后向后续阶段传递（Duration 表达，非 tick）。
     *
     * @param delay 延迟时长
     * @return 新 Promise
     */
    Promise<T> thenDelay(Duration delay);

    /**
     * 超时约束：未在时限内完成则异常完成（{@link java.util.concurrent.TimeoutException}）。
     *
     * @param timeout 超时时长
     * @return 新 Promise
     */
    Promise<T> timeout(Duration timeout);

    /**
     * 与另一 Promise 汇合（两者都完成后以 BiFunction 合并，异步执行）。
     *
     * @param other 另一 Promise
     * @param fn    合并函数
     * @param <U>   另一 Promise 的值类型
     * @param <V>   合并结果类型
     * @return 新 Promise
     */
    <U, V> Promise<V> combine(Promise<U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    /** 异常恢复：链路抛错时以返回值恢复（返回新 Promise，不影响原 Promise）。 */
    Promise<T> exceptionally(Function<Throwable, ? extends T> fn);

    /** 阻塞等待结果（不推荐在主线程使用）。 */
    T join();

    /** 阻塞等待结果，超时抛 {@link java.util.concurrent.TimeoutException}。 */
    T join(Duration timeout);
}

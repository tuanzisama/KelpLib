package ink.tuanzi.kelpLib.api.promise;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Promise 工厂 SPI（内部）：由核心实现经平台注册，注入各平台 "同步/异步" 执行器语义。
 * 业务插件应使用 {@link Promise} 的静态工厂。
 */
@ApiStatus.Internal
public interface PromiseFactory {

    /** 见 {@link Promise#completed(Object)}。 */
    <T> Promise<T> completed(T value);

    /** 见 {@link Promise#failed(Throwable)}。 */
    <T> Promise<T> failed(Throwable throwable);

    /** 见 {@link Promise#start()}。 */
    Promise<Void> start();

    /** 见 {@link Promise#of(CompletableFuture)}。 */
    <T> Promise<T> of(CompletableFuture<T> future);

    /** 见 {@link Promise#supply(Supplier)}。 */
    <T> Promise<T> supply(Supplier<T> supplier);
}

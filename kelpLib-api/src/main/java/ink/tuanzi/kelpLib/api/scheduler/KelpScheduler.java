package ink.tuanzi.kelpLib.api.scheduler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 统一调度抽象（P2：一切线程操作经此收口，库内禁止直接调用平台调度器）。
 *
 * <p>共享接口仅含 <b>GLOBAL</b> 与 <b>ASYNC</b> 两种作用域——三平台（Paper/Folia/Velocity）的最大公约数：</p>
 * <ul>
 *   <li>GLOBAL：Paper/Folia 为主线程或全局区域线程（Global Region），Velocity 为调度器执行池；</li>
 *   <li>ASYNC：各平台异步池。</li>
 * </ul>
 *
 * <p>位置/实体作用域依赖 Bukkit 类型，由 kelpLib-bukkit 导出 {@code RegionSchedulerView}
 * （Paper 上退化为普通同步任务）。周期与延迟一律用 {@link Duration} 表达（内部换算 tick），
 * 避免平台 tick 语义泄漏到 API。</p>
 */
public interface KelpScheduler {

    /**
     * 在 GLOBAL 作用域立即执行一次任务。
     *
     * @param task 任务体，入参为任务句柄（可用于自取消）
     * @return 任务句柄（实现 {@link ink.tuanzi.kelpLib.api.terminable.Terminable} 语义的 ScheduledTask）
     */
    ScheduledTask run(Consumer<ScheduledTask> task);

    /**
     * 在 GLOBAL 作用域延迟执行一次任务。
     *
     * @param delay 延迟时长（Duration 表达，内部换算 tick）
     * @param task  任务体
     * @return 任务句柄
     */
    ScheduledTask runLater(Duration delay, Consumer<ScheduledTask> task);

    /**
     * 在 GLOBAL 作用域按固定周期执行任务。
     *
     * @param initialDelay 首次执行前的延迟
     * @param period       执行周期
     * @param task         任务体
     * @return 任务句柄（取消即停止后续执行）
     */
    ScheduledTask runTimer(Duration initialDelay, Duration period, Consumer<ScheduledTask> task);

    /**
     * 在 ASYNC 作用域执行有返回值的任务，结果经 {@link CompletableFuture} 返回。
     *
     * @param supplier 产出逻辑
     * @param <T>     结果类型
     * @return 异步结果的 future
     */
    <T> CompletableFuture<T> supply(Supplier<T> supplier);

    /**
     * GLOBAL 作用域执行器：供 {@code Promise} 等共享层实现 "同步语义"（thenApplySync 等）。
     *
     * @return 全局同步执行器
     */
    Executor syncExecutor();

    /**
     * ASYNC 作用域执行器：供共享层异步链路使用。
     *
     * @return 异步执行器
     */
    Executor asyncExecutor();

    /**
     * 取消本插件发起的全部调度任务（插件级统一取消）。
     */
    void cancelTasks();
}

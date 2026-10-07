package ink.tuanzi.kelpLib.api.scheduler;

import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;

/**
 * 调度任务句柄。实现 {@link Terminable}（§5.2）——任务可经 {@link #bindWith(TerminableConsumer)}
 * 纳入插件生命周期（插件 disable 自动取消），也可在任意作用域下手动 {@link #cancel()}。
 */
public interface ScheduledTask extends Terminable {

    /**
     * 取消任务；重复调用幂等。
     *
     * @return 本次调用是否真正取消了任务
     */
    boolean cancel();

    /** 任务是否已被取消。 */
    boolean isCancelled();

    /** 等价于 {@link #cancel()}（Terminable 语义：随生命周期关闭）。 */
    @Override
    default void close() {
        cancel();
    }

    /** {@inheritDoc} */
    @Override
    default boolean isClosed() {
        return isCancelled();
    }
}

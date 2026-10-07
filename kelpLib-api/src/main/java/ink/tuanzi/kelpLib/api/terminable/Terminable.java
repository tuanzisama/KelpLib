package ink.tuanzi.kelpLib.api.terminable;

/**
 * 统一生命周期接口（P9 的底座）：库内一切可注销物——调度任务、监听器绑定、文件监听、
 * Redis 订阅、GUI 会话、计分板会话、Messenger agent、命令注册——都实现本接口。
 *
 * <p>{@link #close()} 必须<b>幂等</b>（可重复调用，重复调用为无操作）；绑定到
 * {@link TerminableConsumer}（插件实例即 consumer）后随插件 disable 自动关闭。</p>
 */
public interface Terminable extends AutoCloseable {

    /**
     * 关闭并注销资源；幂等，可重复调用。
     */
    @Override
    void close();

    /** 资源是否已关闭。 */
    boolean isClosed();

    /**
     * 将本对象绑定到指定 consumer，随其生命周期统一关闭。
     *
     * @param consumer 生命周期 consumer（如 {@code KelpBukkit.lifecycle(plugin)}）
     * @param <T>     本对象类型（便于链式使用）
     * @return 本对象
     */
    @SuppressWarnings("unchecked")
    default <T extends Terminable> T bindWith(TerminableConsumer consumer) {
        return (T) consumer.bind(this);
    }

    /** 空实现（close 为无操作）。 */
    static Terminable empty() {
        return new Terminable() {
            private volatile boolean closed;

            @Override
            public void close() {
                closed = true;
            }

            @Override
            public boolean isClosed() {
                return closed;
            }
        };
    }
}

package ink.tuanzi.kelpLib.api.messenger;

import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.function.Consumer;

/**
 * 传输实现 SPI：Messenger 之上的通道层与底层传输解耦。
 * 内置 {@code redis}（Lettuce）与 {@code plugin-messaging}（Bukkit↔Velocity 直连）。
 *
 * <p>传输层只负责"频道名 → 字节载荷"的广播与订阅；编解码、agent 生命周期由通道层负责。</p>
 */
public interface Transport {

    /** 传输 id（{@code "redis"} / {@code "plugin-messaging"}）。 */
    String id();

    /**
     * 向指定频道广播载荷。
     *
     * @param channel 频道名（含前缀）
     * @param payload 载荷字节
     */
    void publish(String channel, byte[] payload);

    /**
     * 订阅频道；close 即退订。同一频道可多订阅者（并发安全）。
     *
     * @param channel  频道名
     * @param listener 载荷回调（可能来自传输线程，监听器内不要做阻塞操作）
     * @return 订阅句柄
     */
    Terminable subscribe(String channel, Consumer<byte[]> listener);

    /** 传输启动钩子（注册时调用，可做连接建立等）。 */
    default void start() {
    }

    /** 传输关闭钩子（KelpLib disable 时调用）。 */
    default void stop() {
    }
}

package ink.tuanzi.kelpLib.api.messenger;

/**
 * fire-and-forget 广播频道：所有订阅端（后端服与代理端）都会收到消息，无回执。
 *
 * @param <M> 消息类型
 */
public interface Channel<M> {

    /** 频道名（含 kelplib: 前缀）。 */
    String name();

    /** 消息类型。 */
    Class<M> type();

    /**
     * 广播消息到全部订阅端。
     *
     * @param message 消息（record/POJO，需可被配置序列化器编码）
     */
    void sendMessage(M message);

    /** 创建订阅 agent（Terminable，可绑定插件生命周期自动退订）。 */
    MessageAgent<M> newAgent();
}

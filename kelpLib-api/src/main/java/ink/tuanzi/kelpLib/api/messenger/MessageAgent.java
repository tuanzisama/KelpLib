package ink.tuanzi.kelpLib.api.messenger;

import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.function.BiConsumer;

/**
 * 频道订阅 agent（Terminable）：close 即退订；可经 {@code bindWith(consumer)} 绑定插件生命周期。
 *
 * @param <M> 消息类型
 */
public interface MessageAgent<M> extends Terminable {

    /**
     * 注册消息监听（可注册多个；close 后自动失效）。
     *
     * @param listener 监听器（入参为 agent 与消息）
     */
    void addListener(BiConsumer<MessageAgent<M>, M> listener);
}

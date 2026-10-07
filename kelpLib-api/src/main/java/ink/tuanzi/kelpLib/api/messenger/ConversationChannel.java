package ink.tuanzi.kelpLib.api.messenger;

import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.function.BiConsumer;

/**
 * 带会话 ID 的请求/回复频道（§6.9 第二层）：消息携带会话标识，回复可路由回发起方。
 *
 * @param <M> 请求消息类型
 * @param <R> 回复消息类型
 * @param <C> 会话 ID 类型
 */
public interface ConversationChannel<M, R, C> {

    /** 频道名（含 kelplib: 前缀）。 */
    String name();

    /**
     * 发送携带会话 ID 的消息。
     *
     * @param message       消息
     * @param conversationId 会话 ID（由发起方生成，回复按其路由）
     */
    void sendMessage(M message, C conversationId);

    /** 创建订阅 agent（同时可收消息与回复）。 */
    ConversationAgent<M, R, C> newAgent();

    /**
     * 会话 agent：监听消息/回复并应答（Terminable，close 幂等即退订）。
     *
     * @param <M> 请求消息类型
     * @param <R> 回复消息类型
     * @param <C> 会话 ID 类型
     */
    interface ConversationAgent<M, R, C> extends Terminable {

        /**
         * 监听进入的消息（非回复）。
         *
         * @param listener 回调
         */
        void addListener(BiConsumer<ConversationAgent<M, R, C>, ConversationMessage<M, R, C>> listener);

        /**
         * 监听进入的回复。
         *
         * @param listener 回调
         */
        void addReplyListener(BiConsumer<ConversationAgent<M, R, C>, ConversationMessage<M, R, C>> listener);

        /**
         * 应答一条消息。
         *
         * @param original 原始消息上下文
         * @param reply    回复内容
         */
        void reply(ConversationMessage<M, R, C> original, R reply);
    }
}

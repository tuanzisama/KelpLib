package ink.tuanzi.kelpLib.api.messenger;

/**
 * 会话频道中的一条消息上下文。
 *
 * @param message       消息正文（请求或回复）
 * @param reply         回复正文（若为回复帧；否则 null）
 * @param conversationId 会话 ID
 * @param isReply       是否为回复帧
 * @param <M>           请求消息类型
 * @param <R>           回复消息类型
 * @param <C>           会话 ID 类型
 */
public record ConversationMessage<M, R, C>(M message, R reply, C conversationId, boolean isReply) {
}

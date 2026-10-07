package ink.tuanzi.kelpLib.api.messenger;

import java.util.Collection;

/**
 * Messenger 消息通道门面（§6.9）：三通道分层、传输可插拔。
 *
 * <p>信任边界：Messenger 假定运行于可信内网（服务器家族自控网络），不提供端到端加密与鉴权；
 * 频道名统一加 {@code kelplib:} 前缀避免与第三方插件冲突。</p>
 *
 * <p>传输实现：{@code redis}（Lettuce，双端可用——后端↔代理端共享数据的主载体）与
 * {@code plugin-messaging}（Bukkit↔Velocity 直连通道）。消息编解码复用配置序列化器。</p>
 */
public interface Messenger {

    /**
     * 取（或创建）fire-and-forget 广播频道。
     *
     * @param name 频道名（自动加 kelplib: 前缀）
     * @param type 消息类型（record/POJO）
     * @param <M>  消息类型
     * @return 频道
     */
    <M> Channel<M> getChannel(String name, Class<M> type);

    /**
     * 取（或创建）带会话 ID 与超时的请求/回复频道。
     *
     * @param name     频道名
     * @param type     请求消息类型
     * @param replyType 回复消息类型
     * @param <M>      请求消息类型
     * @param <R>      回复消息类型
     * @return 会话频道
     */
    <M, R> ConversationChannel<M, R, String> getConversationChannel(String name, Class<M> type, Class<R> replyType);

    /**
     * 取（或创建）Promise 化 RPC 频道。
     *
     * @param name     频道名
     * @param reqType  请求类型
     * @param replyType 回复类型
     * @param <RQ>     请求类型
     * @param <RS>     回复类型
     * @return RPC 频道
     */
    <RQ, RS> ReqRespChannel<RQ, RS> getReqRespChannel(String name, Class<RQ> reqType, Class<RS> replyType);

    /**
     * 注册传输实现（Redis / 插件消息等），同 id 重复注册覆盖。
     *
     * @param transport 传输实现
     */
    void registerTransport(Transport transport);

    /** 已注册的全部传输。 */
    Collection<Transport> transports();

    /** 按 id 查询传输；不存在返回 {@code null}。 */
    Transport transport(String id);
}

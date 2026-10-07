package ink.tuanzi.kelpLib.api.messenger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Promise 化 RPC 频道（§6.9 第三层）：请求方得到 {@link CompletableFuture}，超时自动异常完成。
 *
 * <pre>{@code
 * ReqRespChannel<TeleportReq, Boolean> rpc =
 *     ms.getReqRespChannel("tp-req", TeleportReq.class, Boolean.class);
 *
 * rpc.responseHandler(req -> doTeleport(req));                              // 服务端
 * CompletableFuture<Boolean> ok = rpc.request(req, Duration.ofSeconds(5));  // 客户端
 * }</pre>
 *
 * @param <RQ> 请求类型
 * @param <RS> 回复类型
 */
public interface ReqRespChannel<RQ, RS> {

    /** 频道名（含 kelplib: 前缀）。 */
    String name();

    /**
     * 注册响应处理器（服务端角色；重复注册覆盖）。
     *
     * @param handler 请求 → 回复（同步快速逻辑；耗时逻辑请内部转异步）
     */
    void responseHandler(Function<RQ, RS> handler);

    /**
     * 发起 RPC 请求。
     *
     * @param request 请求
     * @param timeout 超时（超时后 future 以 {@link java.util.concurrent.TimeoutException} 异常完成）
     * @return 回复 future
     */
    CompletableFuture<RS> request(RQ request, Duration timeout);
}

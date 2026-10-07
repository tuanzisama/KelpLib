package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.api.messenger.ConversationChannel;
import ink.tuanzi.kelpLib.api.messenger.ReqRespChannel;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Promise 化 RPC 频道默认实现（§6.9 第三层）：基于会话频道 + UUID 会话 ID + 超时看门狗。
 */
final class ReqRespChannelImpl<RQ, RS> implements ReqRespChannel<RQ, RS> {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");

    private final String name;
    private final ConversationChannelImpl<RQ, RS> conversation;
    private final Class<RQ> reqType;
    private final Class<RS> replyType;
    private final MessengerContext context;
    private final Map<String, CompletableFuture<RS>> pending = new ConcurrentHashMap<>();
    private volatile Function<RQ, RS> responseHandler;
    private volatile boolean responderRegistered;
    private volatile boolean replyAgentRegistered;

    ReqRespChannelImpl(String name, ConversationChannelImpl<RQ, RS> conversation,
                       Class<RQ> reqType, Class<RS> replyType, MessengerContext context) {
        this.name = name;
        this.conversation = conversation;
        this.reqType = reqType;
        this.replyType = replyType;
        this.context = context;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void responseHandler(Function<RQ, RS> handler) {
        this.responseHandler = handler;
        if (responderRegistered) {
            return;
        }
        synchronized (this) {
            if (responderRegistered) {
                return;
            }
            ConversationChannel.ConversationAgent<RQ, RS, String> agent = conversation.newAgent();
            agent.addListener((a, message) -> {
                if (message.isReply()) {
                    return;
                }
                Function<RQ, RS> fn = responseHandler;
                if (fn == null) {
                    return;
                }
                try {
                    a.reply(message, fn.apply(message.message()));
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "ReqResp response handler failed on " + name, t);
                    a.reply(message, null);
                }
            });
            context.bind(terminableOf(agent));
            responderRegistered = true;
        }
    }

    @Override
    public CompletableFuture<RS> request(RQ request, Duration timeout) {
        CompletableFuture<RS> future = new CompletableFuture<>();
        String conversationId = UUID.randomUUID().toString();
        pending.put(conversationId, future);
        ensureReplyAgent();
        conversation.sendMessage(request, conversationId);
        ScheduledFuture<?> watchdog = context.timer().schedule(() -> {
            CompletableFuture<RS> removed = pending.remove(conversationId);
            if (removed != null) {
                removed.completeExceptionally(new TimeoutException("ReqResp request timed out after " + timeout));
            }
        }, Math.max(0, timeout.toMillis()), TimeUnit.MILLISECONDS);
        future.whenComplete((rs, throwable) -> watchdog.cancel(false));
        return future;
    }

    private void ensureReplyAgent() {
        if (replyAgentRegistered) {
            return;
        }
        synchronized (this) {
            if (replyAgentRegistered) {
                return;
            }
            ConversationChannel.ConversationAgent<RQ, RS, String> agent = conversation.newAgent();
            agent.addReplyListener((a, message) -> {
                if (!message.isReply()) {
                    return;
                }
                CompletableFuture<RS> future = pending.remove(message.conversationId());
                if (future != null) {
                    future.complete(message.reply());
                }
            });
            context.bind(terminableOf(agent));
            replyAgentRegistered = true;
        }
    }

    private static Terminable terminableOf(AutoCloseable closeable) {
        return new Terminable() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void close() {
                if (closed.compareAndSet(false, true)) {
                    try {
                        closeable.close();
                    } catch (Exception ignored) {
                        // 关闭异常不外抛
                    }
                }
            }

            @Override
            public boolean isClosed() {
                return closed.get();
            }
        };
    }

    @SuppressWarnings("unused")
    private Class<RQ> reqType() {
        return reqType;
    }
}

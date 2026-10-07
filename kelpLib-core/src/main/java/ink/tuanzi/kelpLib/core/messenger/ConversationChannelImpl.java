package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.api.messenger.ConversationChannel;
import ink.tuanzi.kelpLib.api.messenger.ConversationMessage;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * 带会话 ID 的请求/回复频道默认实现（§6.9 第二层）。
 */
final class ConversationChannelImpl<M, R> implements ConversationChannel<M, R, String> {

    private final String name;
    private final Class<M> type;
    private final Class<R> replyType;
    private final MessengerContext context;

    ConversationChannelImpl(String name, Class<M> type, Class<R> replyType, MessengerContext context) {
        this.name = name;
        this.type = type;
        this.replyType = replyType;
        this.context = context;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void sendMessage(M message, String conversationId) {
        context.publish(name, MessengerContext.encodeFrame(conversationId, false, context.encodePayload(message)));
    }

    @Override
    public ConversationAgent<M, R, String> newAgent() {
        return new Agent();
    }

    private final class Agent implements ConversationAgent<M, R, String> {

        private final List<BiConsumer<ConversationAgent<M, R, String>, ConversationMessage<M, R, String>>> messageListeners =
                new CopyOnWriteArrayList<>();
        private final List<BiConsumer<ConversationAgent<M, R, String>, ConversationMessage<M, R, String>>> replyListeners =
                new CopyOnWriteArrayList<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Terminable handle;

        Agent() {
            this.handle = context.subscribeRaw(name, bytes -> context.dispatch().execute(() -> {
                MessengerContext.Frame frame = MessengerContext.decodeFrame(bytes);
                if (frame == null) {
                    return;
                }
                if (frame.reply()) {
                    R reply = context.decodePayload(frame.data(), replyType);
                    ConversationMessage<M, R, String> message = new ConversationMessage<>(null, reply, frame.conversationId(), true);
                    for (var listener : replyListeners) {
                        safeAccept(listener, message);
                    }
                } else {
                    M message = context.decodePayload(frame.data(), type);
                    ConversationMessage<M, R, String> envelope = new ConversationMessage<>(message, null, frame.conversationId(), false);
                    for (var listener : messageListeners) {
                        safeAccept(listener, envelope);
                    }
                }
            }));
        }

        private void safeAccept(BiConsumer<ConversationAgent<M, R, String>, ConversationMessage<M, R, String>> listener,
                                ConversationMessage<M, R, String> message) {
            try {
                listener.accept(this, message);
            } catch (Throwable ignored) {
                // 单个监听器异常不影响其余监听器
            }
        }

        @Override
        public void addListener(BiConsumer<ConversationAgent<M, R, String>, ConversationMessage<M, R, String>> listener) {
            messageListeners.add(listener);
        }

        @Override
        public void addReplyListener(BiConsumer<ConversationAgent<M, R, String>, ConversationMessage<M, R, String>> listener) {
            replyListeners.add(listener);
        }

        @Override
        public void reply(ConversationMessage<M, R, String> original, R reply) {
            context.publish(name, MessengerContext.encodeFrame(original.conversationId(), true, context.encodePayload(reply)));
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                handle.close();
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }
}

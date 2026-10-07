package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.api.messenger.Channel;
import ink.tuanzi.kelpLib.api.messenger.MessageAgent;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * fire-and-forget 广播频道默认实现。
 */
final class SimpleChannel<M> implements Channel<M> {

    private final String name;
    private final Class<M> type;
    private final MessengerContext context;

    SimpleChannel(String name, Class<M> type, MessengerContext context) {
        this.name = name;
        this.type = type;
        this.context = context;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<M> type() {
        return type;
    }

    @Override
    public void sendMessage(M message) {
        context.publish(name, context.codec(type).encode(message));
    }

    @Override
    public MessageAgent<M> newAgent() {
        return new Agent();
    }

    private final class Agent implements MessageAgent<M> {

        private final List<BiConsumer<MessageAgent<M>, M>> listeners = new CopyOnWriteArrayList<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Terminable handle;

        Agent() {
            this.handle = context.subscribeRaw(name, bytes -> context.dispatch().execute(() -> {
                M message = context.codec(type).decode(bytes, type);
                if (message == null) {
                    return;
                }
                for (BiConsumer<MessageAgent<M>, M> listener : listeners) {
                    try {
                        listener.accept(this, message);
                    } catch (Throwable ignored) {
                        // 单个监听器异常不影响其余监听器
                    }
                }
            }));
        }

        @Override
        public void addListener(BiConsumer<MessageAgent<M>, M> listener) {
            listeners.add(listener);
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

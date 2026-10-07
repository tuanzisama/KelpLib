package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.api.config.SerializerRegistry;
import ink.tuanzi.kelpLib.api.messenger.Channel;
import ink.tuanzi.kelpLib.api.messenger.ConversationChannel;
import ink.tuanzi.kelpLib.api.messenger.Messenger;
import ink.tuanzi.kelpLib.api.messenger.ReqRespChannel;
import ink.tuanzi.kelpLib.api.messenger.Transport;
import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.core.config.SerializerRegistryImpl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Messenger 默认实现：三通道分层 + 传输可插拔（§6.9）。
 * 频道自动加 {@code kelplib:} 前缀；投递回调统一经注入执行器（异步池）执行；agent 均为 Terminable。
 * 注意：同时注册多个传输会导致重复投递，生产环境请只注册一种。
 */
public final class MessengerImpl implements Messenger, MessengerContext, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");

    private final Map<String, Transport> transports = new ConcurrentHashMap<>();
    private final Map<String, Object> channels = new ConcurrentHashMap<>();
    private final Map<Class<?>, MessageCodec> codecs = new ConcurrentHashMap<>();
    private final ConfigAccess configAccess;
    private final Executor dispatch;
    private final ScheduledExecutorService timer;
    private final CompositeTerminable owned = CompositeTerminable.create();

    /** 序列化器访问（解码/编码字符串）。 */
    interface ConfigAccess extends Function<Object, String> {
        Object decode(Class<?> type, String raw);
    }

    public MessengerImpl(SerializerRegistry registry, Executor dispatch) {
        SerializerRegistryImpl impl = (SerializerRegistryImpl) registry;
        this.configAccess = new ConfigAccess() {
            @Override
            public Object decode(Class<?> type, String raw) {
                return impl.deserialize(type, raw);
            }

            @Override
            public String apply(Object value) {
                return impl.serialize(value);
            }
        };
        this.dispatch = dispatch;
        this.timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "KelpLib-MessengerTimer");
            thread.setDaemon(true);
            return thread;
        });
    }

    // ---- Messenger 门面 ----

    @Override
    @SuppressWarnings("unchecked")
    public <M> Channel<M> getChannel(String name, Class<M> type) {
        return (Channel<M>) channels.computeIfAbsent(MessengerContext.prefixed(name),
                key -> new SimpleChannel<M>(key, type, this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <M, R> ConversationChannel<M, R, String> getConversationChannel(String name, Class<M> type, Class<R> replyType) {
        return (ConversationChannel<M, R, String>) channels.computeIfAbsent("conv:" + MessengerContext.prefixed(name),
                key -> new ConversationChannelImpl<M, R>(key, type, replyType, this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <RQ, RS> ReqRespChannel<RQ, RS> getReqRespChannel(String name, Class<RQ> reqType, Class<RS> replyType) {
        return (ReqRespChannel<RQ, RS>) channels.computeIfAbsent("rpc:" + MessengerContext.prefixed(name), key -> {
            ConversationChannelImpl<RQ, RS> conversation =
                    (ConversationChannelImpl<RQ, RS>) getConversationChannel(name, reqType, replyType);
            return new ReqRespChannelImpl<>(key, conversation, reqType, replyType, this);
        });
    }

    @Override
    public void registerTransport(Transport transport) {
        Transport existing = transports.put(transport.id(), transport);
        if (existing != null) {
            try {
                existing.stop();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Messenger old transport stop failed: " + existing.id(), t);
            }
        }
        transport.start();
    }

    @Override
    public Collection<Transport> transports() {
        return List.copyOf(transports.values());
    }

    @Override
    public Transport transport(String id) {
        return transports.get(id);
    }

    // ---- MessengerContext ----

    @Override
    public void publish(String channel, byte[] payload) {
        for (Transport transport : transports.values()) {
            try {
                transport.publish(channel, payload);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Messenger publish failed on transport " + transport.id(), t);
            }
        }
    }

    @Override
    public Terminable subscribeRaw(String channel, Consumer<byte[]> listener) {
        List<Terminable> subscriptions = new ArrayList<>();
        for (Transport transport : transports.values()) {
            try {
                subscriptions.add(transport.subscribe(channel, listener));
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Messenger subscribe failed on transport " + transport.id(), t);
            }
        }
        return new Terminable() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void close() {
                if (closed.compareAndSet(false, true)) {
                    for (Terminable subscription : subscriptions) {
                        subscription.close();
                    }
                }
            }

            @Override
            public boolean isClosed() {
                return closed.get();
            }
        };
    }

    @Override
    public Executor dispatch() {
        return dispatch;
    }

    @Override
    public ScheduledExecutorService timer() {
        return timer;
    }

    @Override
    public MessageCodec codec(Class<?> messageType) {
        return codecs.computeIfAbsent(messageType, type -> new MessageCodec(type, configAccess::decode, configAccess));
    }

    @Override
    public void bind(Terminable terminable) {
        owned.bind(terminable);
    }

    /** 关闭：退订全部 agent、停止全部传输。 */
    @Override
    public void close() {
        owned.close();
        for (Transport transport : transports.values()) {
            try {
                transport.stop();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Messenger transport stop failed: " + transport.id(), t);
            }
        }
        timer.shutdownNow();
    }
}

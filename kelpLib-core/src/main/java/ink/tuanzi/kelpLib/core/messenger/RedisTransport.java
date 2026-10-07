package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.api.messenger.Transport;
import ink.tuanzi.kelpLib.api.storage.RedisSpec;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Redis 传输实现（Lettuce Pub/Sub，§6.9）：双端可用——后端↔代理端共享数据的主载体。
 * 回调发生在 Lettuce 线程上，通道层负责切回调度器执行池。
 */
public final class RedisTransport implements Transport {

    private final RedisSpec spec;
    private final RedisClient client;
    private final StatefulRedisConnection<byte[], byte[]> publisher;
    private final StatefulRedisPubSubConnection<byte[], byte[]> subscriber;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<byte[]>>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /** 建立连接（连接失败抛出，由调用方决定降级行为）。 */
    public RedisTransport(RedisSpec spec) {
        this.spec = spec;
        this.client = RedisClient.create(spec.uri());
        this.publisher = client.connect(new ByteArrayCodec());
        this.subscriber = client.connectPubSub(new ByteArrayCodec());
        this.subscriber.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(byte[] channel, byte[] message) {
                deliver(new String(channel, StandardCharsets.UTF_8), message);
            }
        });
    }

    private void deliver(String channel, byte[] payload) {
        CopyOnWriteArrayList<Consumer<byte[]>> list = listeners.get(channel);
        if (list == null) {
            return;
        }
        for (Consumer<byte[]> listener : list) {
            try {
                listener.accept(payload);
            } catch (Throwable t) {
                // 单个订阅者异常不影响其他订阅者
            }
        }
    }

    @Override
    public String id() {
        return "redis";
    }

    @Override
    public void publish(String channel, byte[] payload) {
        publisher.async().publish(channel.getBytes(StandardCharsets.UTF_8), payload);
    }

    @Override
    public Terminable subscribe(String channel, Consumer<byte[]> listener) {
        byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
        CopyOnWriteArrayList<Consumer<byte[]>> list =
                listeners.computeIfAbsent(channel, key -> new CopyOnWriteArrayList<>());
        boolean first = list.isEmpty();
        list.add(listener);
        if (first) {
            subscriber.async().subscribe(channelBytes);
        }
        return new Terminable() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void close() {
                if (closed.compareAndSet(false, true)) {
                    list.remove(listener);
                    if (list.isEmpty()) {
                        subscriber.async().unsubscribe(channelBytes);
                        listeners.remove(channel, list);
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
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        for (String channel : List.copyOf(listeners.keySet())) {
            subscriber.async().unsubscribe(channel.getBytes(StandardCharsets.UTF_8));
        }
        listeners.clear();
        publisher.close();
        subscriber.close();
        client.shutdown();
    }

    @SuppressWarnings("unused")
    private RedisSpec spec() {
        return spec;
    }
}

package ink.tuanzi.kelpLib.core.storage;

import ink.tuanzi.kelpLib.api.storage.Identifiable;
import ink.tuanzi.kelpLib.api.storage.RedisSpec;
import ink.tuanzi.kelpLib.api.storage.Repository;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 仓库（Lettuce）：hash 结构 {@code {prefix}repo:{类型}:{id}}，值为 YAML 序列化实体。全 API 异步。
 */
public final class RedisRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID>, Terminable {

    private final RedisSpec spec;
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final String hashKey;
    private final Class<T> type;
    private final Class<ID> idType;
    private final EntityCodec codec;
    private final ExecutorService ioPool;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    RedisRepository(RedisSpec spec, Class<T> type, Class<ID> idType, EntityCodec codec, ExecutorService ioPool) {
        this.spec = spec;
        this.type = type;
        this.idType = idType;
        this.codec = codec;
        this.ioPool = ioPool;
        this.client = RedisClient.create(spec.uri());
        this.connection = client.connect();
        this.hashKey = spec.keyPrefix() + "repo:" + type.getSimpleName().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public CompletableFuture<Optional<T>> load(ID id) {
        return supply(() -> {
            String value = connection.sync().hget(hashKey, String.valueOf(id));
            return Optional.ofNullable(codec.decode(value, type));
        });
    }

    @Override
    public CompletableFuture<Void> save(T entity) {
        return run(() -> connection.sync().hset(hashKey, String.valueOf(entity.id()), codec.encode(entity)));
    }

    @Override
    public CompletableFuture<Void> delete(ID id) {
        return run(() -> connection.sync().hdel(hashKey, String.valueOf(id)));
    }

    @Override
    public CompletableFuture<List<T>> loadAll() {
        return supply(() -> {
            List<T> all = new ArrayList<>();
            Map<String, String> entries = connection.sync().hgetall(hashKey);
            for (String value : entries.values()) {
                T entity = codec.decode(value, type);
                if (entity != null) {
                    all.add(entity);
                }
            }
            return all;
        });
    }

    private CompletableFuture<Void> run(RunnableWithException operation) {
        return CompletableFuture.runAsync(() -> {
            try {
                operation.run();
            } catch (Exception e) {
                throw new IllegalStateException("Redis operation failed on " + hashKey, e);
            }
        }, ioPool);
    }

    private <R> CompletableFuture<R> supply(SupplierWithException<R> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.get();
            } catch (Exception e) {
                throw new IllegalStateException("Redis operation failed on " + hashKey, e);
            }
        }, ioPool);
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface SupplierWithException<R> {
        R get() throws Exception;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        connection.close();
        client.shutdown();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @SuppressWarnings("unused")
    private RedisSpec spec() {
        return spec;
    }

    @SuppressWarnings("unused")
    private Class<ID> idType() {
        return idType;
    }
}

package ink.tuanzi.kelpLib.core.storage;

import ink.tuanzi.kelpLib.api.config.SerializerRegistry;
import ink.tuanzi.kelpLib.api.storage.Identifiable;
import ink.tuanzi.kelpLib.api.storage.RedisSpec;
import ink.tuanzi.kelpLib.api.storage.Repository;
import ink.tuanzi.kelpLib.api.storage.SqlSpec;
import ink.tuanzi.kelpLib.api.storage.StorageEngine;
import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import ink.tuanzi.kelpLib.core.config.ConfigScanner;
import ink.tuanzi.kelpLib.core.config.SerializerRegistryImpl;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 存储引擎默认实现：文件 / SQL / Redis 仓库工厂；共享 IO 线程池与序列化器注册表。
 * 同参数重复创建返回同一仓库实例；引擎关闭（KelpLib disable）时统一 flush 并释放连接。
 */
public final class StorageEngineImpl implements StorageEngine, AutoCloseable {

    private final Function<Object, Path> dataDirectories;
    private final SerializerRegistryImpl registry;
    private final ExecutorService ioPool;
    private final CompositeTerminable owned = CompositeTerminable.create();
    private final java.util.concurrent.ConcurrentHashMap<String, Repository<?, ?>> repositories =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicInteger threadCounter = new AtomicInteger();

    public StorageEngineImpl(Function<Object, Path> dataDirectories, SerializerRegistryImpl registry) {
        this.dataDirectories = dataDirectories;
        this.registry = registry;
        this.ioPool = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "KelpLib-Storage-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    private <T extends Identifiable<ID>, ID> EntityCodec codecFor(Class<T> type) {
        return new EntityCodec(ConfigScanner.of(type), registry::deserialize, registry::serialize);
    }

    private <T extends Identifiable<ID>, ID> String repoKey(Object plugin, String kind, String detail, Class<T> type) {
        return kind + "|" + System.identityHashCode(plugin) + "|" + detail + "|" + type.getName();
    }

    @Override
    public <T extends Identifiable<ID>, ID> Repository<T, ID> file(
            Object plugin, String dir, Class<T> type, Class<ID> idType) {
        return (Repository<T, ID>) repositories.computeIfAbsent(
                repoKey(plugin, "file", dir, type),
                key -> {
                    FileRepository<T, ID> repository = new FileRepository<>(
                            type, idType,
                            dataDirectories.apply(plugin).resolve(dir).resolve(type.getSimpleName()),
                            codecFor(type), ioPool);
                    owned.bind(repository);
                    return repository;
                });
    }

    @Override
    public <T extends Identifiable<ID>, ID> Repository<T, ID> sql(
            Object plugin, SqlSpec spec, Class<T> type, Class<ID> idType) {
        return (Repository<T, ID>) repositories.computeIfAbsent(
                repoKey(plugin, "sql", Objects.toString(strategyKey(spec)), type),
                key -> {
                    SqlRepository<T, ID> repository = new SqlRepository<>(
                            spec, pluginNamespace(plugin), type, idType, codecFor(type), ioPool);
                    owned.bind(repository);
                    return repository;
                });
    }

    private String strategyKey(SqlSpec spec) {
        return spec.dialect() + (spec.isEmbedded()
                ? ":" + spec.file().toAbsolutePath().normalize()
                : ":" + spec.host() + ":" + spec.port() + "/" + spec.database());
    }

    @Override
    public <T extends Identifiable<ID>, ID> Repository<T, ID> redis(
            Object plugin, RedisSpec spec, Class<T> type, Class<ID> idType) {
        return (Repository<T, ID>) repositories.computeIfAbsent(
                repoKey(plugin, "redis", spec.uri() + "@" + spec.keyPrefix(), type),
                key -> {
                    RedisRepository<T, ID> repository =
                            new RedisRepository<>(spec, type, idType, codecFor(type), ioPool);
                    owned.bind(repository);
                    return repository;
                });
    }

    private String pluginNamespace(Object plugin) {
        String className = plugin == null ? "unknown" : plugin.getClass().getName();
        return className.length() > 60 ? className.substring(className.length() - 60) : className;
    }

    /** 关闭全部仓库（flush 文件缓存、释放 SQL/Redis 连接）。 */
    @Override
    public void close() {
        owned.close();
        ioPool.shutdown();
    }
}

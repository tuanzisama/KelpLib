package ink.tuanzi.kelpLib.core.storage;

import ink.tuanzi.kelpLib.api.storage.Identifiable;
import ink.tuanzi.kelpLib.api.storage.Repository;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * YAML 文件仓库：目录按实体类型组织、内存缓存 + 脏标记、滚动备份（保留最近 N 份）、
 * 关闭时 flush（吸收 helper FileStorageHandler 思路，§6.9）。全 API 异步。
 */
public final class FileRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID>, Terminable {

    private static final int MAX_BACKUPS = 10;

    private final Class<T> type;
    private final Class<ID> idType;
    private final Path directory;
    private final Path backups;
    private final EntityCodec codec;
    private final ExecutorService ioPool;
    private final Map<ID, T> cache = new ConcurrentHashMap<>();
    private final Set<ID> dirty = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    FileRepository(Class<T> type, Class<ID> idType, Path directory, EntityCodec codec, ExecutorService ioPool) {
        this.type = type;
        this.idType = idType;
        this.directory = directory;
        this.backups = directory.resolve("backups");
        this.codec = codec;
        this.ioPool = ioPool;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create repository directory " + directory, e);
        }
    }

    @Override
    public CompletableFuture<Optional<T>> load(ID id) {
        return CompletableFuture.supplyAsync(() -> {
            T cached = cache.get(id);
            if (cached != null) {
                return Optional.of(cached);
            }
            Path file = fileFor(id);
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            try {
                T entity = codec.decode(Files.readString(file, StandardCharsets.UTF_8), type);
                if (entity != null) {
                    cache.put(id, entity);
                }
                return Optional.ofNullable(entity);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load " + file, e);
            }
        }, ioPool);
    }

    @Override
    public CompletableFuture<Void> save(T entity) {
        ID id = entity.id();
        cache.put(id, entity);
        dirty.add(id);
        return CompletableFuture.runAsync(() -> writeNow(id, entity), ioPool);
    }

    @Override
    public CompletableFuture<Void> delete(ID id) {
        cache.remove(id);
        dirty.remove(id);
        return CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(fileFor(id));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete " + id, e);
            }
        }, ioPool);
    }

    @Override
    public CompletableFuture<List<T>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<T> all = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.yml")) {
                for (Path file : stream) {
                    T entity = codec.decode(Files.readString(file, StandardCharsets.UTF_8), type);
                    if (entity != null) {
                        all.add(entity);
                        cache.put(entity.id(), entity);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list " + directory, e);
            }
            return all;
        }, ioPool);
    }

    private void writeNow(ID id, T entity) {
        Path file = fileFor(id);
        try {
            Files.createDirectories(backups);
            if (Files.isRegularFile(file)) {
                String stamp = Instant.now().toString().replace(':', '-');
                Files.move(file, backups.resolve(fileName(id, stamp)), StandardCopyOption.REPLACE_EXISTING);
                pruneBackups();
            }
            Files.writeString(file, codec.encode(entity), StandardCharsets.UTF_8);
            dirty.remove(id);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save " + file, e);
        }
    }

    private void pruneBackups() {
        try (Stream<Path> list = Files.list(backups)) {
            List<Path> files = list.sorted().toList();
            int over = files.size() - MAX_BACKUPS;
            for (int i = 0; i < over; i++) {
                Files.deleteIfExists(files.get(i));
            }
        } catch (IOException ignored) {
            // 备份清理失败不影响主流程
        }
    }

    private Path fileFor(ID id) {
        return directory.resolve(fileName(id, null));
    }

    private String fileName(ID id, String suffix) {
        String raw = String.valueOf(id);
        String safe = raw.matches("[A-Za-z0-9._\\-]+")
                ? raw
                : URLEncoder.encode(raw, StandardCharsets.UTF_8);
        return suffix == null ? safe + ".yml" : safe + "-" + suffix + ".yml";
    }

    /** 关闭：flush 全部脏实体（吸收 FileStorageHandler 的关服保存语义）。 */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        List<CompletableFuture<Void>> flushes = new ArrayList<>();
        for (Map.Entry<ID, T> entry : cache.entrySet()) {
            if (dirty.contains(entry.getKey())) {
                flushes.add(CompletableFuture.runAsync(() -> writeNow(entry.getKey(), entry.getValue()), ioPool));
            }
        }
        CompletableFuture.allOf(flushes.toArray(CompletableFuture[]::new)).join();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @SuppressWarnings("unused")
    private Class<ID> idType() {
        return idType;
    }
}

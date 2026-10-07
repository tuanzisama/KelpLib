package ink.tuanzi.kelpLib.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ink.tuanzi.kelpLib.api.storage.Identifiable;
import ink.tuanzi.kelpLib.api.storage.Repository;
import ink.tuanzi.kelpLib.api.storage.SqlSpec;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQL 仓库：HikariCP 连接池 + 方言层文档表（id 主键 + data YAML 文本）+ 迁移增量执行。
 * 连接操作在独立线程池执行；驱动缺失等初始化失败时所有操作以带说明的异常完成（优雅降级）。
 */
public final class SqlRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID>, Terminable {

    private final SqlSpec spec;
    private final DialectStrategy strategy;
    private final String table;
    private final String namespace;
    private final Class<T> type;
    private final Class<ID> idType;
    private final EntityCodec codec;
    private final ExecutorService ioPool;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object initLock = new Object();
    private volatile HikariDataSource dataSource;
    private volatile Throwable initError;

    SqlRepository(SqlSpec spec, String namespace, Class<T> type, Class<ID> idType,
                  EntityCodec codec, ExecutorService ioPool) {
        this.spec = spec;
        this.strategy = DialectStrategy.of(spec.dialect());
        this.namespace = namespace;
        this.table = "kelplib_doc_" + type.getSimpleName().toLowerCase(java.util.Locale.ROOT);
        this.type = type;
        this.idType = idType;
        this.codec = codec;
        this.ioPool = ioPool;
        ensureEmbeddedDirectory();
    }

    private void ensureEmbeddedDirectory() {
        if (spec.isEmbedded()) {
            try {
                Path parent = spec.file().toAbsolutePath().normalize().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create embedded database directory", e);
            }
        }
    }

    private HikariDataSource dataSource() throws SQLException {
        HikariDataSource source = dataSource;
        if (source != null) {
            return source;
        }
        Throwable error = initError;
        if (error != null) {
            throw new SQLException("SQL repository unavailable (init failed): " + error.getMessage(), error);
        }
        synchronized (initLock) {
            if (dataSource != null) {
                return dataSource;
            }
            if (initError != null) {
                throw new SQLException("SQL repository unavailable (init failed): " + initError.getMessage(), initError);
            }
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(strategy.jdbcUrl(spec));
                config.setMaximumPoolSize(spec.poolSize());
                config.setPoolName("KelpLib-SQL-" + table);
                config.setInitializationFailTimeout(-1);
                HikariDataSource created = new HikariDataSource(config);
                try (Connection connection = created.getConnection()) {
                    try (PreparedStatement ps = connection.prepareStatement(strategy.createTable(table))) {
                        ps.execute();
                    }
                    Migrations.apply(connection, type.getClassLoader(), spec.dialect(), namespace);
                }
                dataSource = created;
                return created;
            } catch (Throwable t) {
                initError = t;
                throw new SQLException("SQL repository init failed for " + spec.dialect(), t);
            }
        }
    }

    @Override
    public CompletableFuture<Optional<T>> load(ID id) {
        return run(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT data FROM " + table + " WHERE id = ?")) {
                ps.setString(1, String.valueOf(id));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.<T>empty();
                    }
                    return Optional.ofNullable(codec.decode(rs.getString(1), type));
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> save(T entity) {
        return runVoid(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(strategy.upsert(table))) {
                ps.setString(1, String.valueOf(entity.id()));
                ps.setString(2, codec.encode(entity));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(ID id) {
        return runVoid(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM " + table + " WHERE id = ?")) {
                ps.setString(1, String.valueOf(id));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<List<T>> loadAll() {
        return run(connection -> {
            List<T> all = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT data FROM " + table);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    T entity = codec.decode(rs.getString(1), type);
                    if (entity != null) {
                        all.add(entity);
                    }
                }
            }
            return all;
        });
    }

    private <R> CompletableFuture<R> run(SqlFunction<Connection, R> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource().getConnection()) {
                return operation.apply(connection);
            } catch (Exception e) {
                throw new IllegalStateException("SQL operation failed on " + table, e);
            }
        }, ioPool);
    }

    private CompletableFuture<Void> runVoid(SqlConsumer<Connection> operation) {
        return run(connection -> {
            operation.accept(connection);
            return null;
        });
    }

    @FunctionalInterface
    private interface SqlFunction<A, R> {
        R apply(A input) throws Exception;
    }

    @FunctionalInterface
    private interface SqlConsumer<A> {
        void accept(A input) throws Exception;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        HikariDataSource source = dataSource;
        if (source != null) {
            source.close();
        }
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

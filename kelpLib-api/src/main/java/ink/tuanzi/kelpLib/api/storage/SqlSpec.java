package ink.tuanzi.kelpLib.api.storage;

import java.nio.file.Path;

/**
 * SQL 连接规格：嵌入式方言用 {@link #of(SqlDialect, Path)}，网络数据库用
 * {@link #of(SqlDialect, String, int, String, String, String)}。
 *
 * @param dialect  SQL 方言
 * @param file     嵌入式数据库文件（H2/SQLITE 使用；网络方言为 null）
 * @param host     主机（网络方言使用）
 * @param port     端口（网络方言使用）
 * @param database 数据库名（网络方言使用）
 * @param user     用户名（网络方言使用）
 * @param password 密码（网络方言使用）
 * @param poolSize 连接池大小
 */
public record SqlSpec(
        SqlDialect dialect,
        Path file,
        String host,
        int port,
        String database,
        String user,
        String password,
        int poolSize
) {

    /** 默认连接池大小。 */
    public static final int DEFAULT_POOL_SIZE = 8;

    /** 嵌入式（H2/SQLITE）规格。 */
    public static SqlSpec of(SqlDialect dialect, Path file) {
        if (dialect != SqlDialect.H2 && dialect != SqlDialect.SQLITE) {
            throw new IllegalArgumentException("Embedded SqlSpec requires H2 or SQLITE dialect, got " + dialect);
        }
        return new SqlSpec(dialect, file, null, 0, null, null, null, DEFAULT_POOL_SIZE);
    }

    /** 网络数据库（MYSQL/MARIADB）规格。 */
    public static SqlSpec of(SqlDialect dialect, String host, int port, String database, String user, String password) {
        if (dialect != SqlDialect.MYSQL && dialect != SqlDialect.MARIADB) {
            throw new IllegalArgumentException("Network SqlSpec requires MYSQL or MARIADB dialect, got " + dialect);
        }
        return new SqlSpec(dialect, null, host, port, database, user, password, DEFAULT_POOL_SIZE);
    }

    /** 自定义连接池大小。 */
    public SqlSpec poolSize(int poolSize) {
        return new SqlSpec(dialect, file, host, port, database, user, password, poolSize);
    }

    /** 是否为嵌入式方言。 */
    public boolean isEmbedded() {
        return file != null;
    }
}

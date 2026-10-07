package ink.tuanzi.kelpLib.core.storage;

import ink.tuanzi.kelpLib.api.storage.SqlDialect;
import ink.tuanzi.kelpLib.api.storage.SqlSpec;

/**
 * 方言策略（§6.9）：收口 JDBC URL、建表语句与 UPSERT 差异；调用方不写方言 SQL。
 * 文档表统一结构：{@code id 主键 + data 文本（YAML 序列化实体）}。
 */
enum DialectStrategy {

    H2 {
        @Override
        String jdbcUrl(SqlSpec spec) {
            return "jdbc:h2:file:" + spec.file().toAbsolutePath().normalize().toString().replace('\\', '/');
        }

        @Override
        String createTable(String table) {
            return "CREATE TABLE IF NOT EXISTS " + table + " (id VARCHAR(255) PRIMARY KEY, data CLOB)";
        }

        @Override
        String upsert(String table) {
            return "MERGE INTO " + table + " (id, data) KEY (id) VALUES (?, ?)";
        }
    },

    SQLITE {
        @Override
        String jdbcUrl(SqlSpec spec) {
            return "jdbc:sqlite:" + spec.file().toAbsolutePath().normalize();
        }

        @Override
        String createTable(String table) {
            return "CREATE TABLE IF NOT EXISTS " + table + " (id TEXT PRIMARY KEY, data TEXT)";
        }

        @Override
        String upsert(String table) {
            return "INSERT INTO " + table + " (id, data) VALUES (?, ?) "
                    + "ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        }
    },

    MYSQL {
        @Override
        String jdbcUrl(SqlSpec spec) {
            return "jdbc:mysql://" + spec.host() + ":" + spec.port() + "/" + spec.database()
                    + "?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true";
        }

        @Override
        String createTable(String table) {
            return "CREATE TABLE IF NOT EXISTS " + table
                    + " (id VARCHAR(255) PRIMARY KEY, data MEDIUMTEXT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }

        @Override
        String upsert(String table) {
            return "INSERT INTO " + table + " (id, data) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE data = VALUES(data)";
        }
    },

    MARIADB {
        @Override
        String jdbcUrl(SqlSpec spec) {
            return "jdbc:mariadb://" + spec.host() + ":" + spec.port() + "/" + spec.database();
        }

        @Override
        String createTable(String table) {
            return "CREATE TABLE IF NOT EXISTS " + table
                    + " (id VARCHAR(255) PRIMARY KEY, data MEDIUMTEXT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }

        @Override
        String upsert(String table) {
            return "INSERT INTO " + table + " (id, data) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE data = VALUES(data)";
        }
    };

    abstract String jdbcUrl(SqlSpec spec);

    abstract String createTable(String table);

    abstract String upsert(String table);

    /** 迁移脚本目录名（migrations/{dialect}/）。 */
    String migrationDir() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    static DialectStrategy of(SqlDialect dialect) {
        return DialectStrategy.valueOf(dialect.name());
    }
}

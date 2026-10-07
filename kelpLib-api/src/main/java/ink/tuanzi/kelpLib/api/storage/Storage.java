package ink.tuanzi.kelpLib.api.storage;

import ink.tuanzi.kelpLib.api.Kelp;

/**
 * 存储静态门面（§6.9）。文件 / SQL / Redis 全套仓储的统一工厂入口。
 *
 * <pre>{@code
 * Storage.file(plugin, "data", PlayerData.class)                         // YAML 文件仓库
 * Storage.sql(plugin, SqlSpec.of(SqlDialect.H2, path))                   // 嵌入式零配置起步
 * Storage.sql(plugin, SqlSpec.of(SqlDialect.MYSQL, host, db, user, pw))  // 多服共享
 * Storage.redis(plugin, RedisSpec.of("redis://localhost:6379/0"))        // 跨服热数据
 * }</pre>
 */
public final class Storage {

    private Storage() {
    }

    /** 见 {@link StorageEngine#file(Object, String, Class, Class)}。 */
    public static <T extends Identifiable<ID>, ID> Repository<T, ID> file(
            Object plugin, String dir, Class<T> type, Class<ID> idType) {
        return Kelp.platform().storageEngine().file(plugin, dir, type, idType);
    }

    /** 见 {@link StorageEngine#sql(Object, SqlSpec, Class, Class)}。 */
    public static <T extends Identifiable<ID>, ID> Repository<T, ID> sql(
            Object plugin, SqlSpec spec, Class<T> type, Class<ID> idType) {
        return Kelp.platform().storageEngine().sql(plugin, spec, type, idType);
    }

    /** 见 {@link StorageEngine#redis(Object, RedisSpec, Class, Class)}。 */
    public static <T extends Identifiable<ID>, ID> Repository<T, ID> redis(
            Object plugin, RedisSpec spec, Class<T> type, Class<ID> idType) {
        return Kelp.platform().storageEngine().redis(plugin, spec, type, idType);
    }
}

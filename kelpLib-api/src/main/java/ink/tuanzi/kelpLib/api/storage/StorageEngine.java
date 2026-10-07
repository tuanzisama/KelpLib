package ink.tuanzi.kelpLib.api.storage;

import org.jetbrains.annotations.ApiStatus;

/**
 * 存储引擎 SPI（内部）：经平台注册，由 {@link Storage} 静态门面分发。
 */
@ApiStatus.Internal
public interface StorageEngine {

    /**
     * 创建 YAML 文件仓库（目录按实体类型组织，含滚动备份与关服 flush）。
     *
     * @param plugin 业务插件实例
     * @param dir    数据子目录名（相对插件数据目录）
     * @param type   实体类型
     * @param idType 主键类型
     * @param <T>    实体类型
     * @param <ID>   主键类型
     * @return 文件仓库
     */
    <T extends Identifiable<ID>, ID> Repository<T, ID> file(Object plugin, String dir, Class<T> type, Class<ID> idType);

    /**
     * 创建 SQL 仓库（HikariCP 连接池 + 方言层收口 + 迁移脚本增量执行）。
     *
     * @param plugin 业务插件实例（迁移脚本从其 jar 资源 {@code migrations/{dialect}/} 或 {@code migrations/common/} 读取）
     * @param spec   连接规格
     * @param type   实体类型
     * @param idType 主键类型
     * @param <T>    实体类型
     * @param <ID>   主键类型
     * @return SQL 仓库
     */
    <T extends Identifiable<ID>, ID> Repository<T, ID> sql(Object plugin, SqlSpec spec, Class<T> type, Class<ID> idType);

    /**
     * 创建 Redis 仓库（hash 结构，键前缀见 {@link RedisSpec}）。
     *
     * @param plugin 业务插件实例
     * @param spec   Redis 连接规格
     * @param type   实体类型
     * @param idType 主键类型
     * @param <T>    实体类型
     * @param <ID>   主键类型
     * @return Redis 仓库
     */
    <T extends Identifiable<ID>, ID> Repository<T, ID> redis(Object plugin, RedisSpec spec, Class<T> type, Class<ID> idType);
}

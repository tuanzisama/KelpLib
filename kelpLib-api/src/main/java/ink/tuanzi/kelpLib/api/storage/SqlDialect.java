package ink.tuanzi.kelpLib.api.storage;

/**
 * SQL 方言（§6.9）：方言层收口建表语句、自增主键、UPSERT、类型映射差异，调用方不写方言 SQL。
 *
 * <ul>
 *   <li>{@link #H2} / {@link #SQLITE}：单服或开发环境（零外部依赖，嵌入式）；</li>
 *   <li>{@link #MYSQL} / {@link #MARIADB}：多服共享数据库。</li>
 * </ul>
 */
public enum SqlDialect {

    /** H2 2.x（嵌入式文件库）。 */
    H2,

    /** SQLite（sqlite-jdbc）。 */
    SQLITE,

    /** MySQL（mysql-connector-j）。 */
    MYSQL,

    /** MariaDB（官方 mariadb 驱动，非 mysql 兼容模式）。 */
    MARIADB
}

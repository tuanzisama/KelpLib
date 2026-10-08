# Storage

文件 / SQL / Redis 三种仓储共享同一 `Repository` API，全异步。静态门面 `ink.tuanzi.kelpLib.api.storage.Storage`：

```java
Storage.file(plugin, "data", PlayerData.class, UUID.class);                        // YAML 文件仓库
Storage.sql(plugin, SqlSpec.of(SqlDialect.H2, path), PlayerData.class, UUID.class); // 嵌入式零配置起步
Storage.sql(plugin, SqlSpec.of(SqlDialect.MYSQL, host, 3306, db, user, pw),
        PlayerData.class, UUID.class);                                             // 多服共享
Storage.redis(plugin, RedisSpec.of("redis://localhost:6379/0"),
        SessionData.class, String.class);                                          // 跨服热数据
```

同参数（插件实例 + 规格 + 实体类型）重复创建返回同一仓库实例。

## 实体约定

实体为 record / POJO，实现 `Identifiable<ID>`：

```java
public record PlayerData(UUID id, String name, int homes) implements Identifiable<UUID> {}
```

字段编解码复用配置序列化器（见 [Config](config.md)），自定义字段类型先注册序列化器。

## Repository

```java
public interface Repository<T, ID> {
    CompletableFuture<Optional<T>> load(ID id);      // 不存在返回空 Optional
    CompletableFuture<Void> save(T entity);          // upsert 语义
    CompletableFuture<Void> delete(ID id);
    CompletableFuture<List<T>> loadAll();            // 数据量大时慎用
}
```

连接操作在独立 IO 线程池（守护线程 `KelpLib-Storage-*`）执行；需要主线程语义时用 [Promise](promise.md) 的 sync 系列衔接。

```java
repository.save(entity)
        .thenCompose(unused -> repository.load(entity.id()))
        .thenAccept(loaded -> loaded.ifPresent(v -> use(v)));
```

## 文件仓库

目录按 `<数据目录>/<dir>/<实体类名>/` 组织，每实体一个 YAML 文件。内存缓存 + 脏标记写入，滚动备份（保留最近 10 份），关服 flush。

## SQL 仓库

HikariCP 连接池 + 方言层收口建表 / UPSERT / 类型差异，调用方不写方言 SQL。文档表模型：`kelplib_doc_<实体名小写>`（id 主键 + data YAML 文本列）。

`SqlSpec`：

```java
SqlSpec.of(SqlDialect.H2, pluginDir.resolve("data/db"))          // 嵌入式：H2 / SQLITE
SqlSpec.of(SqlDialect.MYSQL, host, port, database, user, pw)     // 网络：MYSQL / MARIADB
spec.poolSize(16)                                                // 默认 8
```

驱动缺失等初始化失败时所有操作以带说明的异常完成（优雅降级）。运行时驱动：Bukkit 端经 `plugin.yml` `libraries:` 提供，Velocity 端 relocate 进 KelpLib jar。

### 迁移

迁移脚本从业务插件 jar 资源读取，增量执行，记录于 `kelplib_migrations` 版本表（按插件命名空间隔离）：

- `migrations/{dialect}/V<n>__<名称>.sql`（方言目录优先）
- `migrations/common/V<n>__<名称>.sql`

```sql
-- migrations/common/V1__init.sql
CREATE TABLE IF NOT EXISTS demo_homes (...);
```

首次连接（load/save）时执行，幂等。

## Redis 仓库

hash 结构存储，键 `前缀<实体名小写>:<id>`，键前缀默认 `kelplib:`（`RedisSpec.keyPrefix` 自定义）。

```java
RedisSpec.of("redis://localhost:6379/0")            // URI 形式（支持 redis://:pass@host:port）
RedisSpec.of("localhost", 6379, password)           // 主机 / 端口 / 密码
```

RedisSpec 同时用于 Messenger 的 Redis 传输（见 [Messenger](messenger.md)）。

## 关闭

仓库由存储引擎统一持有，KelpLib disable 时 flush 并释放连接，业务插件无需手动关闭。

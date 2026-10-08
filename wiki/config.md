# Config

注解驱动的 record / POJO ↔ YAML 双向映射，入口 `Kelp.configs()`（`ink.tuanzi.kelpLib.api.config.ConfigEngine`）。基于 SnakeYAML 节点树操作，写回时保留原文件注释与键顺序；新建文件时按注解顺序与默认值生成。

Velocity 侧根目录为 dataDirectory，Bukkit 侧为插件数据目录，由平台适配。

## 声明

```java
@KelpConfig(path = "config.yml")
public record PluginConfig(
        @Key("prefix") @Default("<green>[Kelp]</green>") Component prefix,
        @Key("max-homes") @Default("3") int maxHomes
) {}
```

| 注解 | 说明 |
|------|------|
| `@KelpConfig(path)` | 标记 record / POJO 为配置类；`path` 相对插件数据目录，默认 `config.yml` |
| `@Key(value)` | 字段到 YAML 键的显式映射，支持点分嵌套路径（如 `database.host`）；缺省用组件名的 kebab-case 形式 |
| `@Default(value)` | 默认值字符串，经序列化器解析为目标类型；文件缺失键或新建文件时使用 |

POJO 与 record 均可；复杂结构（`List<T>` / `Map<String, T>` / 嵌套配置类）由引擎按元素类型递归处理。

## 读写

```java
PluginConfig cfg = Kelp.configs().load(plugin, PluginConfig.class);   // 加载（或首次生成）
Kelp.configs().save(plugin, cfg);                                     // 写回（保留注释）
```

## 热重载

`watch` 监听配置文件变更（watch service），变更时异步重读、经平台 GLOBAL 执行器回调：

```java
Terminable handle = Kelp.configs().watch(plugin, PluginConfig.class, fresh -> {
    this.cfg = fresh;
});
```

返回监听句柄（Terminable），随插件 disable 自动关闭，也可手动 close 停止监听。

## 序列化器

内置序列化器覆盖平台无关类型：

| 类型 | YAML 表达 |
|------|-----------|
| `String` / 基本类型及包装 | 字面量 |
| `Component` | MiniMessage 字符串 |
| `Duration` | 紧凑时长（`5m30s`，见 [TimeUtil](utilities.md)）|
| 枚举 | 名称 |
| `UUID` / `Locale` / `InetSocketAddress` | 标准字符串形式 |
| `List<T>` / `Map<String, T>` | 按元素类型递归 |

Bukkit 模块追加 `org.bukkit.Location`（`world;x;y;z;yaw;pitch`）。

自定义类型经 `Kelp.configs().serializers()` 注册：

```java
Kelp.configs().serializers().register(Home.class, new ConfigSerializer<Home>() {
    @Override
    public Home deserialize(String raw) { return Home.parse(raw); }

    @Override
    public String serialize(Home value) { return value.toString(); }
});
```

`SerializerRegistry` 同时为配置引擎、存储实体（[Storage](storage.md)）与消息编解码（[Messenger](messenger.md)）共用，注册一次全场景生效。

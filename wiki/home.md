# KelpLib Wiki

KelpLib 是 Paper / Folia 与 Velocity 共用的 Minecraft 插件开发工具库，为 utoverse 服务器家族的业务插件提供共享基础设施：调度、异步编排、生命周期、事件、命令、配置、消息、展示、存储、跨服通信。

一份 API 双端通用。Folia 适配与线程细节由库内部处理，插件卸载零残留。

- 运行环境：Paper / Folia 1.21+、Velocity 3.x、Java 21+
- 坐标：`ink.tuanzi:kelp-lib-api`（GitHub Packages）
- JavaDoc：<https://tuanzisama.github.io/KelpLib>
- 协议：Apache-2.0

## 模块索引

| 页面 | 内容 | 平台 |
|------|------|------|
| [Installing](installing.md) | 安装与依赖声明 | — |
| [Scheduler](scheduler.md) | 统一调度、区域 / 实体作用域 | 双端 |
| [Promise](promise.md) | 跨线程链式异步 | 双端 |
| [Terminables](terminables.md) | 生命周期与自动清理 | 双端 |
| [Events](events.md) | 函数式事件订阅 | Bukkit |
| [Commands](commands.md) | 注解式命令 | 双端 |
| [Config](config.md) | 注解驱动 YAML 配置 | 双端 |
| [Messages](messages.md) | 多语言消息、MiniMessage | 双端 |
| [Display](display.md) | ActionBar / Title / BossBar / 计分板 | 双端（计分板 Bukkit）|
| [Items](items.md) | ItemBuilder、NBT | Bukkit |
| [Storage](storage.md) | 文件 / SQL / Redis 仓储 | 双端 |
| [Messenger](messenger.md) | 跨服消息通道 | 双端 |
| [Utilities](utilities.md) | Cooldown / Bucket / WeightedRandom / TimeUtil | 双端 |
| [GUI](gui.md) | GUI 门控与 Schemes | Bukkit |
| [Diagnostics](diagnostics.md) | `/kelp` 命令 | 双端 |

## API 入口

平台无关门面为 `ink.tuanzi.kelpLib.api.Kelp`：

| 方法 | 模块 |
|------|------|
| `Kelp.scheduler()` | 调度 |
| `Kelp.commands()` | 命令注册 |
| `Kelp.messenger()` | 跨服消息通道 |
| `Kelp.display()` | 展示 |
| `Kelp.configs()` | 配置引擎 |
| `Kelp.isAvailable()` | KelpLib 是否已完成加载 |

存储经 `ink.tuanzi.kelpLib.api.storage.Storage` 静态门面，见 [Storage](storage.md)。

Bukkit 侧 `KelpBukkit` 额外导出 `regions()`（区域 / 实体调度视图）、`listeners()`（传统监听器注册）、`lifecycle(plugin)`（插件生命周期 consumer）。Velocity 侧直接使用 `Kelp`。

## 示例

```java
// 异步编排
Promise.start()
        .thenApplyAsync(uuid -> loadPlayer(uuid))
        .timeout(Duration.ofSeconds(5))
        .thenAcceptSync(data -> showWelcome(player, data)); // 回 GLOBAL 作用域

// 注解命令
@Command(name = "home")
public final class HomeCommand {
    @Execute(name = "set")
    public void set(@Sender Player player, @Arg(name = "名称") String name) { /* ... */ }
}
Kelp.commands().register(plugin, HomeCommand.class);

// 配置 + 热重载
@KelpConfig(path = "config.yml")
public record PluginConfig(@Key("max-homes") @Default("3") int maxHomes) {}

PluginConfig cfg = Kelp.configs().load(plugin, PluginConfig.class);
Kelp.configs().watch(plugin, PluginConfig.class, fresh -> this.cfg = fresh);
```

安装后可用 `/kelp demo <module>` 查看各模块运行效果，见 [Diagnostics](diagnostics.md)。

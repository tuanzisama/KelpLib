<div align="center">

# KelpLib

**Paper / Folia 与 Velocity 共用的 Minecraft 插件开发工具库**

[![Release](https://img.shields.io/github/v/release/tuanzisama/KelpLib)](https://github.com/tuanzisama/KelpLib/releases)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-online-4c1.svg)](https://tuanzisama.github.io/KelpLib)
[![Build](https://github.com/tuanzisama/KelpLib/actions/workflows/release.yml/badge.svg)](https://github.com/tuanzisama/KelpLib/actions/workflows/release.yml)

</div>

一份 API 双端通用：调度、命令、配置、消息、存储、跨服通信。Folia 适配与线程细节由库内部处理，插件卸载零残留。

环境要求：Paper / Folia 1.21+、Velocity 3.x、Java 21+。

## 功能

- **调度** `KelpScheduler` —— Paper / Folia 自动适配；区域 / 实体作用域仅 Bukkit（`KelpBukkit.regions()`）
- **Promise / Terminable** —— 异步编排、超时；任务与监听随插件卸载自动关闭
- **命令** —— `@Command` `@Execute` `@Arg` `@Sender` `@Flag` 注解声明，自带权限与 Tab 补全
- **事件** —— 函数式订阅，过滤、自动过期（Bukkit）
- **配置** —— record / POJO 注解映射 YAML，保注释保序，`watch` 热重载
- **消息 / 展示** —— MiniMessage + per-player i18n；ActionBar / Title / BossBar 双端，计分板 Bukkit
- **存储** —— 文件 / SQL（H2、SQLite、MySQL、MariaDB + 迁移）/ Redis，统一 `Repository`
- **跨服通信** —— Channel / Conversation / ReqResp 三层通道，Redis 与插件消息传输

## 使用

插件 jar 放入 `plugins/`（见 [Releases](https://github.com/tuanzisama/KelpLib/releases)），业务插件声明依赖：

```yaml
depend: [KelpLib]
```

```java
@Plugin(id = "my-plugin", dependencies = {@Dependency(id = "kelplib")})
```

编译依赖（发布于 GitHub Packages，读取需 GitHub Token，~~`也许 JitPack 也可以`~~）：

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/tuanzisama/KelpLib")
        credentials {
            username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("ink.tuanzi:kelp-lib-api:1.0.0") // 坐标为 kelp-lib-*（全小写），非模块名
}
```

快速上手：

```java
// 异步编排
Promise.start()
        .thenApplyAsync(uuid -> loadPlayer(uuid))
        .timeout(Duration.ofSeconds(5))
        .thenAcceptSync(data -> showWelcome(player, data)); // 回主线程

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

其余模块用法见 [JavaDoc](https://tuanzisama.github.io/KelpLib)；安装后 `/kelp demo <module>` 可看运行效果。

## 构建

```bash
./gradlew build
```

欢迎 Issue / PR。

## License

Apache-2.0

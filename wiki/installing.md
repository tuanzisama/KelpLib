# 安装与依赖

## 运行环境

| 项 | 要求 |
|------|------|
| 服务端 | Paper / Folia 1.21+（`api-version: '1.21'`，`folia-supported: true`）或 Velocity 3.x |
| Java | 21+ |

## 服务器侧安装

将 KelpLib jar 放入插件目录：

- Bukkit 系：`plugins/`
- Velocity：`plugins/`

运行时依赖（HikariCP、Lettuce、JDBC 驱动）在 Bukkit 端经 KelpLib 的 `plugin.yml` `libraries:` 由服务端自动拉取，不打包进业务可见的类路径；Velocity 端打包进 KelpLib jar 并 relocate 到 `ink.tuanzi.kelpLib.libs.*`。

## 业务插件声明依赖

Bukkit（`plugin.yml`）：

```yaml
depend: [KelpLib]
```

Velocity：

```java
@Plugin(id = "my-plugin", dependencies = {@Dependency(id = "kelplib")})
```

Velocity 插件 id 为 `kelplib`（`velocity-plugin.json`）。

## 加载时机

KelpLib 以 `load: STARTUP` 加载，`onLoad()` 内完成 `Kelp` 门面初始化。声明依赖的插件在自己的 `onLoad()` 中即可使用门面。不确定时用 `Kelp.isAvailable()` 判断。

## 编译依赖

构件发布于 GitHub Packages（读取需 GitHub Token）：

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
    compileOnly("ink.tuanzi:kelp-lib-api:1.0.0") // 坐标 kelp-lib-*（全小写），非模块名
}
```

需要 `RedisTransport` 等核心实现类时改依赖 `ink.tuanzi:kelp-lib-core`，见 [Messenger](messenger.md)。

版本随 [Releases](https://github.com/tuanzisama/KelpLib/releases) 调整。项目内版本唯一来源是 `gradle.properties`。

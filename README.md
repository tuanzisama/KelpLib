# KelpLib

面向 **Paper / Folia（后端）与 Velocity（代理端）** 的实用工具与扩展 API 集合，服务 utoverse 服务器家族。
以平台插件形式安装，业务插件通过依赖接入；平台无关能力共享同一套 API（`kelpLib-api`）。

> 设计与里程碑见 [PLAN.md](PLAN.md)（v0.7 定稿）。当前版本 `1.0.0`（gradle.properties）。

## 构建与产物

```bash
gradlew build          # 产出全部 jar（Windows: gradlew.bat build）
gradlew :kelpLib-api:publishToMavenLocal   # 发布 api 构件到 mavenLocal（0.x 阶段分发方式，§8）
gradlew :kelpLib-bukkit:runServer          # 本地 Paper 26.3 冒烟（run-paper）
```

## CI：GitHub Packages 发布 + JavaDoc Pages

工作流 [`.github/workflows/release.yml`](.github/workflows/release.yml)：推送 `v*` tag（或手动触发）时自动

1. 构建 → `gradlew publish` 把 **api / core / bukkit / velocity** 四个构件发布到 GitHub Packages Maven 仓库（`https://maven.pkg.github.com/<owner>/<repo>`；bukkit/velocity 附带 `all` 分类器的可安装插件 jar；tag 版本号自动映射 Maven 版本，如 `v1.2.3` → `1.2.3`）。Maven 坐标为 `ink.tuanzi:kelp-lib-{api,core,bukkit,velocity}`——**GitHub Packages 要求 artifactId 全小写**，故发布坐标与模块目录名（`kelpLib-*`）不同；
2. 生成 **kelpLib-api JavaDoc** 并部署到 GitHub Pages（`https://<owner>.github.io/<repo>/`）。

**前置设置（一次性）**：仓库 Settings → Pages → Source 选 "GitHub Actions"。

> **注意**：GitHub Packages 的包版本**不可覆盖**——同一版本重复发布返回 422。工作流发布前会逐模块探测、已存在的版本自动跳过（重跑幂等）；若需强制重发某版本，先在 Packages → 对应包 → Versions 删除该版本，或换新版本号 tag。

业务插件从 Packages 消费（`settings.gradle.kts`）：

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/<owner>/<repo>")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
        mavenCentral()
    }
}
// dependencies { compileOnly("ink.tuanzi:kelp-lib-api:1.0.0") }
```


| 产物 | 说明 |
|------|------|
| `kelpLib-bukkit-1.0.0-all.jar` | Bukkit/Folia 插件 jar（含 api+core+捆绑实现） |
| `kelpLib-velocity-1.0.0-all.jar` | Velocity 代理插件 jar（内部型依赖 relocate 捆绑：HikariCP/Lettuce/四 JDBC 驱动/SnakeYAML → `ink.tuanzi.kelpLib.libs.*`） |
| `kelpLib-api-1.0.0.jar` | 平台无关稳定面（业务插件编译依赖） |
| `kelpLib-core-1.0.0.jar` | 平台无关通用实现（随平台插件捆绑，不单独分发） |

字节码目标 **21**（Java 25 toolchain 编译）；`plugin.yml` 为 `api-version: '1.21'` + `folia-supported: true` + `load: STARTUP`。

## 双端接入

### Bukkit / Folia 侧

1. 安装 `kelpLib-bukkit-*-all.jar` 到 `plugins/`（JDBC 驱动等经 plugin.yml `libraries:` 首次启动联网下载，可预置 `libraries/` 目录离线使用）。
2. 业务插件 `plugin.yml` 声明 `depend: [KelpLib]`；编译依赖 `ink.tuanzi:kelpLib-api:1.0.0`（mavenLocal）。
3. 代码入口：

```java
// onLoad 之后任意时机（KelpLib 为 STARTUP 加载，onLoad 即已初始化门面）
KelpScheduler scheduler = Kelp.scheduler();
TerminableConsumer lifecycle = KelpBukkit.lifecycle(plugin);   // 平台专属视图
Kelp.commands().register(plugin, MyCommand.class);
```

### Velocity 侧

1. 安装 `kelpLib-velocity-*-all.jar` 到 `plugins/`。
2. 业务插件 `@Plugin(dependencies = @Dependency(id = "kelplib"))`；注入 `KelpLibVelocity` 实例或经 `KelpVelocity` 门面使用。
3. 平台无关 API 与 Bukkit 侧完全一致（`Kelp.scheduler()` / `Kelp.messenger()` / …）。

## 模块 × 平台矩阵（§6.0）

| 模块 | Paper/Folia | Velocity | 说明 |
|------|:-----------:|:--------:|------|
| 调度抽象 | ✅ | ✅ | GLOBAL/ASYNC 共享接口；区域/实体作用域仅 Bukkit（`KelpBukkit.regions()`） |
| Promise / Terminables | ✅ | ✅ | 平台无关；sync 语义经各平台调度器映射 |
| 事件订阅（函数式） | ✅ | — | `Events.subscribe/merge` + `EventFilters` + `expireAfter`；仅 Bukkit 侧 |
| ItemBuilder / NBT | ✅ | — | NBT 为可选集成（见下方环境受限说明），缺省引导降级 PDC |
| GUI（InvUI 集成） | ◐ 门控 | — | 本构建未捆绑 InvUI（见下方说明）；`KelpGui.available()` 门控 |
| 命令框架 | ✅ | ✅ | 注解式单入口（见下方说明）；`Kelp.commands().register(...)` |
| 配置与序列化 | ✅ | ✅ | `@KelpConfig` record/POJO ↔ YAML（保注释保序）+ watch 热重载 |
| 消息 / i18n | ✅ | ✅ | YAML 键值 + MiniMessage + per-player locale + 三级回退 |
| 展示 API | ✅ 全量 | ◐ 子集 | ActionBar/Title/BossBar 双端；计分板仅 Bukkit（`ScoreboardDisplay`） |
| 存储 | ✅ | ✅ | 文件（含滚动备份）/ SQL 四方言（H2/SQLite/MySQL/MariaDB + 迁移）/ Redis |
| Messenger | ✅ | ✅ | Channel / ConversationChannel / ReqRespChannel；Redis + 插件消息传输 |
| 基础工具 | ✅ | ✅ | Cooldown / TimeUtil / WeightedRandom / Bucket |

## 模块速览

```java
// 调度（一切线程操作收口，P2）
Kelp.scheduler().runLater(Duration.ofSeconds(1), task -> ...);
Kelp.scheduler().supply(() -> queryDb()).thenAccept(...);
KelpBukkit.regions().forEntity(player, task -> ...);   // Folia 实体线程

// Promise（§6.2）
Promise.start()
    .thenApplyAsync(storage::loadName)
    .thenDelay(Duration.ofSeconds(2))
    .timeout(Duration.ofSeconds(5))
    .thenAcceptSync(name -> messages.send(player, "welcome"));

// Terminables（P9：一切可注销物随插件 disable 自动关闭）
Kelp.scheduler().runTimer(d1, d2, t -> ...).bindWith(lifecycle);

// 函数式事件订阅（仅 Bukkit，§6.2）
Events.subscribe(PlayerJoinEvent.class)
    .filter(EventFilters.ignoreCancelled())
    .expireAfter(1)
    .handler(e -> e.getPlayer().sendMessage(...))
    .bindWith(KelpBukkit.lifecycle(plugin));

// 配置（§6.6）
@KelpConfig(path = "config.yml")
public record PluginConfig(@Key("prefix") @Default("<green>[Kelp]</green>") String prefix,
                           @Key("max-homes") @Default("3") int maxHomes) {}
PluginConfig cfg = Kelp.configs().load(plugin, PluginConfig.class);
Kelp.configs().watch(plugin, PluginConfig.class, fresh -> this.cfg = fresh);

// 消息 / 展示（§6.7）
Messages messages = Messages.create(plugin).defaultLocale("zh_cn").build();
messages.send(player, "welcome", Placeholder.unparsed("player", player.getName()));
Kelp.display().actionBar(player, "<gold>+10 金币</gold>");

// 存储（§6.9）
Repository<PlayerData, UUID> repo = Storage.file(plugin, "data", PlayerData.class, UUID.class);
Repository<PlayerData, UUID> sql = Storage.sql(plugin, SqlSpec.of(SqlDialect.H2, dbPath));

// Messenger（§6.9）
Channel<Ping> ping = Kelp.messenger().getChannel("ping", Ping.class);
ReqRespChannel<TeleportReq, Boolean> rpc = Kelp.messenger().getReqRespChannel("tp", TeleportReq.class, Boolean.class);
```

内置 `/kelp demo <module>`（Bukkit：scheduler/promise/event/listener/item/nbt/command/config/message/scoreboard/gui/storage/messenger；Velocity：scheduler/promise/config/storage/messenger）为各里程碑手动验收工具。

## ⚠️ 环境受限偏差记录（构建时网络不可达所致）

本工程构建期间，国际 Maven 仓库（repo1.maven.org / repo.papermc.io / repo.xenondevs.xyz / repo.panda-lang.org / plugins.gradle.org 等）在本机持续不可达（国内镜像仅覆盖 Maven Central 主体）。按 PLAN §11.4 既定回退路径做了如下处理，**恢复网络后可按注释一键切回原选型**：

1. **paper-api / velocity-api → 编译桩**：`kelpLib-{bukkit,velocity}/src/stubs/java` 镜像了用到的 API 子集，仅参与编译、**不打包进 jar**（已验证无泄漏）。桩签名按 1.21+ 稳定 API 编写，运行时使用服务器真实实现；若与 Paper 26.3 实际签名有出入，运行期才会暴露——首次冒烟时留意。切回真实依赖：启用 build.gradle.kts 中被注释的 `paper-api`/`velocity-api` 依赖并删除 `sourceSets.create("stubs")` 两行。
2. **LiteCommands / Cloud 不可达 → 自研命令层**（§11.4 记载的最后回退）：注解模型 `@Command/@Execute/@Arg/@Sender/@Flag` 语义与 LiteCommands 对齐，底层注册到平台命令表（Paper `CommandMap` / Velocity `CommandManager`），含节点权限（补全侧隐藏）与静态/自定义参数补全；Brigadier 原生类型化补全 UI 未达成（LiteCommands 恢复后切回）。
3. **InvUI 不可达 → GUI 能力门控脚手架**：`KelpGui.available()==false`（§6.8 的 Capabilities 门控语义），掩码 sugar（`Schemes`）与生命周期/线程路由约定就位，调用入口给出明确引导。
4. **ItemNBTAPI 不可达 → 反射式可选集成**：`Nbt` 经运行时探测（relocate 捆绑副本或服务器独立安装），缺失时 `available()==false` 并引导降级 PDC——与 §6.4 "可选依赖 + 自动检测 + 优雅降级" 的设计一致。
5. `plugin.yml` 的 `libraries:`（HikariCP/Lettuce/H2/SQLite/MySQL/MariaDB）保持 §8 设计：由服务器运行时从 Maven Central 下载；Velocity 侧 relocate 捆绑已完成。

其余全部按 PLAN.md v0.7 落地：多模块架构与依赖规则（§4）、统一调度抽象 + Folia/Paper 双实现（§5）、Promise/Terminables/函数式事件（§6.2）、util 系列（§6.3）、配置引擎（SnakeYAML 节点树保注释保序 + watch 热重载，§6.6）、Messages/i18n/Display（§6.7）、存储三层 + 方言层 + 迁移（§6.9）、Messenger 三通道 + Redis/插件消息传输（§6.9）、`/kelp demo`（§6.10）、版本兼容基线（§7：字节码 21、api-version 1.21、folia-supported、maven-publish）。

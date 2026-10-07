# KelpLib 开发计划书

> 版本：v0.7 · 2026-10-06 · **定稿**——需求确认与选型全部完成，可直接开工 M0
> 负责人：evenwan（utoverse 服务器家族）
>
> 阅读指引：§2 为全部关键决策的速查表；§3 为贯穿全文的设计原则（P1–P10）；§9 为里程碑与 M0 执行清单；§11 为选型对比依据与"不采用"记录。v0.7 仅为结构重排与去冗，技术内容与 v0.6 定稿一致。

---

## 1. 项目概述

**KelpLib** 是一套面向 **Paper / Folia（后端服务端）与 Velocity（代理端）** 的实用工具与扩展 API 集合，以平台插件形式安装，供业务插件通过依赖接入。目标是把插件开发中重复、易错的部分——**调度线程、任务链与生命周期、GUI 菜单、命令注册、配置管理、物品构建与 NBT、消息多语言、跨服消息、数据持久化**——沉淀为一套默认线程安全、风格统一的共享库，让业务插件"快速、简便"地开发。

- 命名空间：`ink.tuanzi.kelpLib`（Gradle group：`ink.tuanzi`）
- 技术基线：Java 25 toolchain（发布字节码目标 21，见 §7）· Paper API 26.3 · Velocity API 3.x · Gradle 9.4
- 目标平台：Paper（主）、Folia（区域化线程模型下可用）、Velocity（代理端，平台无关模块全集 + 命令/文本子集）

### 1.1 背景

utoverse 服务器家族的业务插件反复面对同类底层问题。KelpLib 将其收敛为统一抽象，一次实现、全家族复用，后端与代理端共享同一套平台无关 API（各痛点对应一个模块域）：

- Folia 下 `Bukkit.getScheduler()` 不可用，线程模型易写错，Paper/Folia 代码难复用 → 调度抽象（§5）；
- 后端与代理端各写一套命令、配置、存储、消息逻辑，跨服协同（Redis Pub/Sub、消息总线）重复造轮子 → 共享层 + Messenger（§6.9）；
- 同步/异步任务链、监听器与资源的生命周期散乱，插件 disable 后残留任务/监听是常见 bug 源 → Promise / Terminables（§6.2）；
- GUI 菜单、命令注册、物品构建、配置加载各插件重复实现、质量参差 → §6.3–§6.8；
- 文本渲染（MiniMessage 组件）与玩家展示（ActionBar / Title / BossBar / 计分板）散落各处、写法不一 → 消息与展示 API（§6.7）；
- 持久化选型（文件 / SQL / Redis）与连接管理重复造轮子，且少有考虑异步与跨服场景 → 存储抽象（§6.9）。

### 1.2 目标

| # | 目标 | 衡量方式 |
|---|------|----------|
| G1 | 业务插件接入成本 ≤ 10 分钟 | 依赖 API 构件 + 平台声明即可使用基础能力 |
| G2 | 同一份业务代码在 Paper 与 Folia 上行为一致；平台无关模块在 Velocity 上使用同一套 API | 统一调度抽象收口线程操作（§5）；模块×平台矩阵公开（§6.0） |
| G3 | 核心 API 全部异步优先 | 阻塞式 IO API 不进入 `api` 模块 |
| G4 | API 稳定可依赖 | 语义化版本 + 稳定性注解（§3 P6） |
| G5 | 文档完整 | `api` 模块 100% JavaDoc，README 覆盖双端接入与各模块速览 |
| G6 | 可验证 | 内置 `/kelp demo` 系列命令，作为各里程碑的手动验收工具 |

### 1.3 非目标与暂缓项

**v1.0 明确排除**：

- 不做 NMS / 每版本适配层，不依赖 Mojang 映射内部 API；
- 不内置经济、权限的具体实现；
- 函数式事件订阅层仅覆盖 Bukkit 侧事件（Velocity 侧使用原生监听 + Terminable 绑定，见 §6.2）；
- 不做插件脚手架 / 代码生成器；
- 不做 Velocity 上的"物品栏类"能力模拟（代理端无物品栏/世界模型，相关模块明确不适用，见 §6.0 矩阵）；
- 不做 Velocity 计分板（需后端转发，Display 保持 ActionBar/Title/BossBar 子集）；
- 不做 Kotlin DSL（Java 为主）；
- v1.0 不投入单元测试与 CI（质量基线为"轻量"，见 §8；列入 1.0 后增强路线）；
- 不采纳 helper 的 Maven Annotations（运行时下载 Maven 依赖：供应链风险、需联网、与 relocate 方案冲突）及 helper-js / helper-lilypad / helper-mongo（见 §11.1）。

**暂缓至 1.x（备选池，需求出现时启动）**：

- Velocity 侧函数式事件订阅（§6.2）；
- Folia 跨玩家共享库存——InvUI 零共享约束下的"团队共享箱子"，方案为 per-viewer 副本 + 变更同步的胶水扩展；
- Vault 等经济/权限桥接（业务插件现直接依赖 Vault 本体）；
- bStats / 更新检查（按需再加）；
- helper 候选能力：类型化元数据（MetadataKey + ExpiringValue）、Hologram / Sign Prompt / Profiles / NPC、Plugin Annotations（见 §11.1）；
- api 构件正式发布基建（GitHub Packages / 内部 Nexus，M6 前评估，见 §8）。

---

## 2. 决策一览

下表为全部关键决策的速查；设计细节在各 § 展开，选型对比依据见 §11。

| 决策点 | 结论 | 落点 |
|--------|------|------|
| 项目定位 | **内部优先、按公开标准设计** | API/实现分离、语义化版本、完整 JavaDoc，未来可平滑开源 |
| Folia 支持策略 | **统一调度抽象**（`KelpScheduler` 多作用域，Paper 自动降级），业务代码零改动跨平台 | §5 |
| 平台范围 | **Paper + Folia + Velocity**；多模块架构：共享 `api/core` + 各平台实现模块，平台无关模块双端同 API，平台专属模块仅 Bukkit 侧 | §4、§6.0 |
| 接入方式 | **插件依赖**：以插件/代理插件安装；同时发布独立 `kelpLib-api` 构件供编译；可选库按导出型/内部型分发 | §4、§8 |
| 版本范围 | **近期版本（1.21.x ~ 26.x）**：字节码目标 21、运行时能力探测 + 调用点隔离、`api-version: '1.21'` | §7 |
| v1.0 模块全集 | 见模块×平台矩阵 | §6.0 |
| 存储能力 | **文件 + SQL + Redis 全套**（`Repository` 抽象）；SQL 四方言 | §6.9 |
| 消息能力 | MiniMessage 封装、per-player locale、展示 API（Velocity 为子集） | §6.7 |
| NBT 集成 | **封装 ItemNBTAPI（tr7zw）**：可选依赖 + 自动检测，缺失时明确报错并引导降级 PDC | §6.4 |
| GUI | **引入社区包 InvUI**（不自研）；仅支持 26.x；Folia 官方实验性支持 + 两条硬约束 | §6.8 |
| 命令框架 | **引入 LiteCommands**（不自研）：注解式为唯一正式入口、底层落到 Brigadier | §6.5 |
| 事件订阅 | **自研完整函数式**（流式风格，与 Promise/Terminable 统一）；仅 Bukkit 侧 | §6.2 |
| Promise / Terminables / Messenger | **自研**（借鉴 [lucko/helper](https://github.com/lucko/helper/wiki/) 的设计思想而非代码） | §6.2、§6.9、§11.1 |
| JDBC 驱动分发 | Bukkit/Folia 侧经 plugin.yml `libraries:` 动态引入；Velocity 侧捆绑 relocate | §6.9、§8 |
| 质量基线 | **轻量**：JavaDoc + README + 手动冒烟；暂不做 CI 与单测 | §8 |
| 交付节奏 | **分模块迭代**（M0–M6），每阶段可用，优先级随内部插件实际需求调整 | §9 |
| API 构件发布 | **暂缓**：0.x 用 mavenLocal，M6 前再评估 GitHub Packages / 内部 Nexus | §8 |

---

## 3. 设计原则

- **P1 API 与实现分离**：`api` 模块只含接口、模型、注解，且**禁止依赖任何平台类型**（Bukkit/Velocity 均不可出现）；平台专属 API 从对应平台模块导出。
- **P2 默认线程安全**：一切线程操作经 `KelpScheduler` 收口；库内禁止直接调用 `Bukkit.getScheduler()`；服务注册表使用 `ConcurrentHashMap`，无跨线程共享的可变状态。
- **P3 异步优先**：所有 IO 类 API（存储、文件监听、Redis）返回 `CompletableFuture`/`Promise`；同步便捷方法仅用于明确安全的场景。
- **P4 核心零硬依赖 + 社区优先**：`api` 模块仅依赖 Adventure 套件；成熟社区实现优先引入而非重复造轮子（GUI→InvUI、NBT→ItemNBTAPI、命令→LiteCommands），自研仅覆盖社区没有或不适配的空白（调度、Promise、Terminables、Messenger、函数式事件订阅）；可选依赖启动时探测、缺失即优雅降级。
- **P5 现代 Java**：面向接口与 record、sealed 类型、模式匹配；受字节码目标 21 约束，不使用 21 之后的语言特性（§7）。
- **P6 显式稳定性**：0.x 阶段允许破坏性变更；1.0 起遵循语义化版本。不稳定 API 一律标注 `@ApiStatus.Experimental`，内部实现标注 `@ApiStatus.Internal`。
- **P7 近版本兼容靠探测而非分支**：单一构件覆盖 1.21.x ~ 26.x，用能力探测（Capabilities）+ 调用点隔离处理版本差异（§7）。
- **P8 共享优先**：能放进 `api/core` 的逻辑不进平台模块；平台模块只承载平台交互，保证 Velocity 侧能力随共享层自动获得。
- **P9 生命周期自治**：库内一切可注销物（调度任务、监听器、文件监听、Redis 订阅、GUI 会话、计分板会话）实现统一 `Terminable` 接口，绑定到插件即随插件 disable 自动关闭（§6.2）。
- **P10 声明式与流式分层**：静态结构（配置映射、命令树）用注解声明——编译期已知、一次扫描；运行时行为（调度、Promise、Terminable、事件订阅）用流式 API——动态创建、带生命周期。两类风格在各自领域保持统一。

---

## 4. 总体架构（多模块）

```
KelpLib/
├── kelpLib-api/             // ★ 平台无关稳定面（发布为 kelpLib-api 构件，供双端业务插件编译依赖）
│   ├── Kelp.java            //   服务门面（平台无关）：scheduler() / promise() / configs() / commands() / messages() / storage() / messenger() ...；listeners()/items() 等平台视图由平台模块门面导出
│   ├── scheduler/           //   KelpScheduler（GLOBAL/ASYNC 作用域）、ScheduledTask
│   ├── promise/             //   Promise 跨线程链式（§6.2）
│   ├── terminable/          //   Terminable、TerminableConsumer、CompositeTerminable（§6.2）
│   ├── command/             //   命令注册门面（注解模型由 LiteCommands 提供，双端共享胶水）
│   ├── config/              //   @KelpConfig、@Key、ConfigManager、Serializer 注册表
│   ├── storage/             //   Repository、SqlDialect、SqlSpec
│   ├── messenger/           //   Messenger、Channel、ConversationChannel、ReqRespChannel、Transport（§6.9）
│   ├── text/                //   Messages、I18n、Display（Velocity 支持子集，见 §6.0）
│   └── util/                //   Cooldown、TimeUtil、WeightedRandom、Bucket ...
├── kelpLib-core/            // 平台无关通用实现（Promise/Terminable 引擎、i18n、序列化器、文件/SQL/Redis 仓库、注解扫描）
├── kelpLib-bukkit/          // Paper/Folia 平台模块（既是实现，也导出 Bukkit 专属 API）
│   ├── scheduler/           //   Bukkit/Folia 调度实现 + RegionSchedulerView（at/forEntity 作用域）
│   ├── listener/            //   原生 Listener 的 Terminable 绑定（§6.2）
│   ├── item/                //   ItemBuilder、NbtService（仅 Bukkit 侧）
│   ├── gui/                 //   InvUI 集成胶水（仅 Bukkit 侧，§6.8）
│   ├── command/             //   LiteCommands 集成胶水（litecommands-bukkit/folia，落到 Paper Brigadier）
│   ├── text/                //   Display 全量实现（含计分板）
│   └── KelpLib.java         //   JavaPlugin 入口（plugin.yml：depend 接入）
└── kelpLib-velocity/        // Velocity 平台模块（实现 + 导出代理端 API）
    ├── scheduler/           //   VelocityScheduler 实现（§5.4）
    ├── command/             //   LiteCommands 集成胶水（litecommands-velocity）
    ├── text/                //   Display 子集实现（ActionBar/Title/BossBar）
    └── KelpLibVelocity.java //   @Plugin 注解入口（velocity-plugin.json）
```

**依赖规则**（构建期约束）：`kelpLib-api` 与 `kelpLib-core` 的 `build.gradle.kts` **不声明** Bukkit/Velocity 依赖；平台交互只出现在平台模块——从结构上杜绝平台类型泄漏进共享层（P8）。

**业务插件接入方式**（插件依赖模式）：

- **Bukkit 侧**：`plugin.yml` 声明 `depend: [KelpLib]`；编译依赖 `kelpLib-api` + `kelpLib-bukkit`（后者提供 ItemBuilder/GUI 互操作/Display 全量等 Bukkit 专属类型）；运行期 `KelpLib.getInstance().kelp()` 获取门面。KelpLib `load: STARTUP` 且在 **`onLoad()` 完成门面初始化**，同为 STARTUP 的依赖方在其 `onLoad` 中亦可安全获取（呼应 AGENTS.md 的 STARTUP 约束）。
- **Velocity 侧**：`@Plugin` 注解声明 `dependencies = @Dependency(id = "kelplib")`；编译依赖 `kelpLib-api` + `kelpLib-velocity`；运行期经 KelpLib 的插件实例（Velocity 依赖注入传入）获取 `Kelp` 门面。

---

## 5. 线程模型与调度抽象（核心章节）

### 5.1 Paper 与 Folia 线程模型差异

| 场景 | Paper | Folia |
|------|-------|-------|
| 全局操作（世界创建、日程表等） | 主线程 | 全局区域线程（Global Region） |
| 世界/区块操作 | 主线程 | 该区块所属区域线程（并发多线程） |
| 实体操作 | 主线程 | 该实体所属区域线程（实体可跨区迁移） |
| `Bukkit.getScheduler()` | ✅ | ❌（抛异常） |
| 事件回调线程 | 主线程 | 触发事件所在区域线程 |

### 5.2 统一调度抽象（kelpLib-api）

```java
public interface KelpScheduler {
    ScheduledTask run(Consumer<ScheduledTask> task);                 // GLOBAL
    ScheduledTask runLater(Duration delay, Consumer<ScheduledTask> task);
    ScheduledTask runTimer(Duration initialDelay, Duration period,
                           Consumer<ScheduledTask> task);
    <T> CompletableFuture<T> supply(Supplier<T> supplier);           // ASYNC
    void cancelTasks();                                              // 插件级统一取消
}

// 便捷入口（api 门面）：
Kelp.scheduler().runLater(d, task);
Kelp.scheduler().supply(supplier);
```

共享接口仅含 **GLOBAL 与 ASYNC** 两种作用域——这是三平台的最大公约数。位置/实体作用域依赖 Bukkit 类型，放平台模块：

```java
/** 仅 kelpLib-bukkit 导出；Paper 上退化为普通同步任务 */
public interface RegionSchedulerView {
    ScheduledTask at(Location location, Consumer<ScheduledTask> task);
    ScheduledTask forEntity(Entity entity, Consumer<ScheduledTask> task); // Folia 下跟随实体迁移
}
```

`ScheduledTask` 实现 `Terminable`（§6.2）——任务可 `bindWith(consumer)` 纳入生命周期，也可在任意作用域下手动取消。

### 5.3 Bukkit/Folia 实现要点

- **平台检测**：类存在性检测 `io.papermc.paper.threadedregions.RegionizedServer`；
- Folia：GLOBAL→`GlobalRegionScheduler`、REGION→`RegionScheduler`、ENTITY→`EntityScheduler`（含实体 retired 自动取消）；Paper：除 ASYNC 外全部走 `BukkitScheduler` 同步任务；
- 周期参数用 `Duration` 表达（内部换算 tick），避免平台 tick 语义差异泄漏到 API；
- **库内铁律**：所有模块（GUI 刷新、配置热重载、冷却清理等）只能经 `KelpScheduler` 发起任务，违反即视为 bug；
- 服务注册表、菜单会话表、冷却表等一律 `ConcurrentHashMap`；GUI 的点击事件在 Folia 下于区域线程触发，会话状态按 viewer 隔离；事件监听器回调线程不做假设。

### 5.4 Velocity 映射（kelpLib-velocity）

- Velocity 无主线程/区域概念：GLOBAL→`VelocityScheduler` 调度任务（其执行池），ASYNC→同一调度器；
- `RegionSchedulerView` 在 Velocity 不存在（类型上即不可见），共享层代码天然不会触碰；
- Velocity 事件多为异步触发，`text`/`storage` 等共享模块本就线程安全，无需特殊处理。

---

## 6. 功能模块设计

### 6.0 模块 × 平台支持矩阵

| 模块 | Paper/Folia | Velocity | 说明 |
|------|:-----------:|:--------:|------|
| 调度抽象 | ✅ | ✅ | 共享接口（GLOBAL/ASYNC）；区域/实体作用域仅 Bukkit 侧 |
| Promise / Terminables | ✅ | ✅ | 平台无关；sync 语义经各平台调度器映射 |
| 事件订阅（函数式，流式） | ✅ | — | 事件类型属 Bukkit；Velocity 用原生监听 + Terminable（§6.2） |
| 基础工具集 | ✅ | ✅ | 纯 Java |
| ItemBuilder / NBT | ✅ | — | 依赖 Bukkit 物品模型与 Data Component |
| GUI 菜单（InvUI 集成） | ✅ 仅 26.x | — | 代理端不适用；Folia 官方实验性支持（viewer EntityScheduler 路由，§6.8）；1.21.x 门控禁用 |
| 命令框架（LiteCommands） | ✅ | ✅ | 注解式单入口（动态结构走 LiteCommands 原生 API）；Paper 端落到原生 Brigadier；Folia 走官方 folia 模块 |
| 配置与序列化 | ✅ | ✅ | Velocity 侧基于 `dataDirectory` |
| 消息 / i18n | ✅ | ✅ | Adventure 为双平台原生共享 |
| 展示 API | ✅ 全量 | ◐ 子集 | Velocity 支持 ActionBar / Title / BossBar；计分板不支持（见 §1.3） |
| 存储（文件/SQL/Redis） | ✅ | ✅ | 纯 Java，双端同构 |
| Messenger 消息通道 | ✅ | ✅ | 三通道分层；Redis 传输双端可用，插件消息传输 M5 起（§6.9） |

### 6.1 调度抽象（M1）

见 §5。本模块是所有其他模块的地基，最先交付，且**三端实现同期落地**（Velocity 实现很薄，可尽早验证抽象的通用性）。

### 6.2 Promise / Terminables（M1）与函数式事件订阅（M2）

两者互为表里：Promise 负责"一个值在哪个线程、何时产生"，Terminable 负责"产生的资源由谁、何时关闭"。

**Promise**（替代早期计划的 AsyncChain）：

```java
Promise<String> name = Promise.start()
    .thenApplySync(() -> resolveInput())            // 平台"同步"语义（Velocity 上 = 调度器池）
    .thenApplyAsync(storage::loadName)              // 异步池
    .thenDelay(Duration.ofSeconds(2))               // 延迟继续（Duration 表达，非 tick）
    .timeout(Duration.ofSeconds(5));                // 超时异常完成

name.thenAccept(n -> messages.send(player, "welcome", ...));   // 回到声明作用域
```

- `thenApplySync/Async`、`thenRunSync/Async`、`thenAccept*`、`thenDelay`、`timeout`、`combine`、`exceptionally`；
- sync 语义三平台统一映射（§5.4：Velocity 上 = VelocityScheduler 执行池）；
- **所有 Promise 默认纳入 Terminable 体系**：插件 disable 时未完成的 Promise 取消并异常完成。

**Terminables**（P9 的底座，参考 helper 的 Terminable 设计）：

```java
public interface Terminable extends AutoCloseable { void close(); }

// 一切可注销物都返回 Terminable，可绑定到插件/组合注册表：
ScheduledTask task = Kelp.scheduler().runTimer(d1, d2, t).bindWith(plugin);
Terminable watch  = Configs.watch(plugin, Config.class, cb).bindWith(plugin);
composite.bind(subscription).bind(menuSession);

// 插件 disable 时，绑定到其 TerminableConsumer 的对象统一关闭（幂等，可重复 close）
```

- `TerminableConsumer`（插件实例即 consumer）、`CompositeTerminable`（分组注册表）、`TerminableModule`（按功能分组注册）；
- **库内统一**：调度任务、监听器绑定、文件监听、Redis 订阅、GUI 会话、计分板会话、Messenger agent 全部实现 Terminable；
- 平台无关（api/core），双端同一套 API。

**函数式事件订阅（M2，仅 Bukkit 侧；流式风格）**：

```java
// 订阅即流：过滤 → 过期 → 处理 → 绑定生命周期，一行收口
Events.subscribe(PlayerJoinEvent.class)
    .filter(EventFilters.ignoreCancelled())
    .handler(e -> messages.send(e.getPlayer(), "welcome"))
    .bindWith(plugin);                                   // 随插件 disable 自动注销

// 一次性/限时订阅（等一次点击、限时活动窗口）
Events.subscribe(InventoryClickEvent.class)
    .expireAfter(1)                                      // 执行一次后自动注销
    .handler(ctx -> ...);

// 多事件归一（共享父类，如 PlayerEvent）
Events.merge(PlayerEvent.class, PlayerQuitEvent.class, PlayerKickEvent.class)
    .expireAfter(Duration.ofMinutes(30))
    .handler(e -> ...);

// 异类事件绑定公共类型
Events.merge(Player.class)
    .bindEvent(PlayerQuitEvent.class, PlayerQuitEvent::getPlayer)
    .bindEvent(PlayerKickEvent.class, PlayerKickEvent::getPlayer)
    .handler(e -> ...);

// 与原生互操作：传统 @EventHandler 监听器同样纳入 Terminable 生命周期
// Listener 为 Bukkit 类型：绑定入口在 bukkit 模块的门面扩展上（与 RegionSchedulerView 同模式，P1）
KelpBukkit.listeners().register(plugin, new LegacyListener()).bindWith(plugin);
```

- `subscribe` / `merge` / `bindEvent`（多事件归一与公共类型提取）；`EventFilters` 预置过滤器（ignoreCancelled、ignoreSameBlock、playerHasPermission…）；`expireAfter`（按 `Duration` 或执行次数）；
- 风格依据 P10：事件属运行时行为，用流式 API（注解留给静态结构——配置与命令树）；
- Folia 适配：注册动作并发安全；回调线程不做假设（§5.3）；
- Velocity 侧 v1.0 不提供函数式层（事件类型属 Bukkit），使用原生监听 + Terminable 绑定。

### 6.3 基础工具集（M1）

```java
// 物品构建（基于 Data Component API，非 legacy MaterialData）—— 仅 Bukkit 侧
ItemStack blade = ItemBuilder.of(Material.NETHERITE_SWORD)
    .name("<gold>烈焰之刃</gold>")            // MiniMessage 直出
    .lore("<gray>攻击时点燃目标</gray>")
    .enchantment(Enchantment.SHARPNESS, 5)
    .pdc(Key.key("utoverse", "item-id"), PersistentDataType.STRING, "flame_blade")
    .skull(owner)
    .build();

// 冷却（并发安全，按 UUID 键控，双平台可用）
Cooldown cd = Cooldown.of(Duration.ofSeconds(30));
if (cd.tryAcquire(player)) { /* 放行 */ } else { /* 剩余时间提示 */ }

// 其余（双平台）：TimeUtil（相对时间格式化/解析）、WeightedRandom、
//                Bucket（按 tick 分区分散大批量处理，Folia 下分散负载场景实用）；
//                平台专属（LocationUtil、ChunkKey）在 bukkit 模块
```

### 6.4 Item-NBT 封装（M2，仅 Bukkit 侧）

- 封装 [tr7zw/ItemNBTAPI](https://github.com/tr7zw/Item-NBT-API) 为 `NbtService`，作为**可选依赖**随 kelpLib-bukkit jar relocate 打包（业务插件无感知）；
- 启动时自动检测；未加载/不可用时 `Nbt.available() == false`，`Nbt` 门面抛出带说明的 `IllegalStateException`，并引导降级到 PDC；
- 覆盖场景：任意 tag 读写、NBT 复制、与 `PersistentDataContainer` 互转。

```java
ItemStack out = Nbt.edit(item, nbt -> {
    nbt.setString("utoverse:quest", "q_07");
    return nbt.toItem();
});
```

### 6.5 命令框架（M2，引入 LiteCommands，注解式单入口）

**决策**：不自研命令框架，引入 [LiteCommands](https://github.com/Rollczi/LiteCommands)（Apache-2.0，v3.x 活跃维护，官方 `litecommands-bukkit / folia / velocity` 三平台模块）。选型对比与回退路径见 §11.4。

**注册入口策略（注解式为唯一正式入口）**：

- **注解式（唯一门面入口）**：`@Command`、`@Execute`、`@Arg`、`@Flag`、`@Sender`，含子命令树、每节点权限（客户端命令树中直接隐藏无权限分支）、参数校验与默认值、内置参数类型与自定义解析器；业务插件仅经 `Kelp.commands().register(...)` 一条路径注册；
- **动态命令的表达**：区分两类"动态"——**动态参数值**（补全/取值来自配置或运行时数据，如 `/warp <名字>`，占绝大多数）用**自定义参数解析器 + 运行时 suggestion** 在注解模型内表达；**动态结构**（运行时才知道有几个子命令，极罕见）不设门面入口，业务插件直接使用 LiteCommands 原生 builder API（导出型依赖、原生 API 不遮蔽，可直接 import）或重新建模为动态参数。单入口避免双写法的心智负担，也避免在 LiteCommands 低层 builder 之上自研并长期维护一套高层 DSL（P4）；
- **底层落到 Brigadier**：Paper 端经 `io.papermc.paper.command.brigadier` + `LifecycleEvents.COMMANDS` 注册（1.21.x 与 26.x 均可用），获得原生类型化补全 UI；Velocity 端注册到 `CommandManager`（`BrigadierCommand` 包装可用）。不手写 Brigadier 树，样板由框架代劳。

```java
// 静态命令——一个类一棵树
@Command(name = "heal", permission = "utoverse.heal")
public final class HealCommand {
    @Execute
    void self(@Sender Player player) { ... }

    @Execute
    void other(@Sender CommandSource src, @Arg("目标") OnlinePlayer target) { ... } // 平台参数类型
}
Kelp.commands().register(plugin, HealCommand.class);

// 动态参数值——自定义解析器 + 运行时补全（仍在注解模型内）
@Command(name = "warp")
public final class WarpCommand {
    @Execute
    void go(@Sender Player player, @Arg("传送点") Warp warp) { ... }
}
// WarpArg 解析器：候选列表运行时从配置/数据读取
```

**集成方式**：`litecommands-bukkit/folia/velocity` 以 compileOnly 引入并 shadow 打进对应平台 jar，**捆绑但不 relocate**（导出型依赖，库插件模式，见 §8；发布于 Maven Central，无额外仓库需求）；`Kelp.commands()` 门面暴露唯一的 `register(...)` 入口，LiteCommands 原生 API（含低层 builder，即动态结构的逃生舱）不遮蔽，业务插件不应自行再 shade 同名库。

**胶水层**：i18n 注入（命令描述、参数名、错误消息经 §6.7 Messages 消息键渲染，per-player locale）；权限校验统一走 KelpLib 权限约定；命令注册纳入 Terminable 生命周期（插件 disable 自动注销）；耗时补全异步执行。

**M2 冒烟验收（选型风险的关闭条件）**：

1. **Brigadier 保真度**：Paper 26.x 下注解命令注册后，客户端补全为原生 Brigadier 类型化 UI（非旧式 tab-completer 字符串猜测）；
2. **Folia**：`litecommands-folia` 模块下注册、执行、补全、权限分支隐藏均正常；
3. Velocity 侧注册与异步补全——随 M5（litecommands-velocity 接入）验收，M2 冒烟仅覆盖 Paper/Folia。

任一不达标的回退路径：评估 Cloud（Folia 需自行验证/适配）或回到自研双后端方案（要点见 §11.4）。

### 6.6 配置与序列化（M2，双平台）

- 注解驱动的 record/POJO ↔ YAML 双向映射；**保留注释与键顺序**（基于 SnakeYAML 节点树操作，而非原 Configuration API）；
- 内置序列化器：`Component`、`Duration`、枚举、UUID、`InetSocketAddress` 等平台无关类型；`ItemStack`/`Location` 序列化器在 bukkit 模块追加注册；
- 热重载：文件变更监听（watch service，返回 Terminable）+ `KelpScheduler` 异步读、同步安全回调；
- Velocity 侧根目录为 `dataDirectory`，Bukkit 侧为插件数据目录，`Configs.load` 自动适配。

```java
@KelpConfig(path = "config.yml")
public record PluginConfig(
    @Key("prefix") @Default("<green>[Kelp]</green>") Component prefix,
    @Key("max-homes") @Default("3") int maxHomes
) {}

PluginConfig cfg = Configs.load(plugin, PluginConfig.class);
Configs.watch(plugin, PluginConfig.class, fresh -> this.cfg = fresh); // 热重载
```

### 6.7 消息、多语言与展示 API（M3）

```java
Messages messages = Messages.create(plugin)
    .defaultLocale("zh_cn")
    .localeResolver(player -> player.locale())   // per-player locale（Velocity 用 PlayerSettings）
    .build();

messages.send(player, "welcome", Placeholder.unparsed("player", player.getName()));

// 展示 API（全部经 KelpScheduler，线程安全）
Display.actionBar(player, "<gold>+10 金币</gold>");
Display.title(player, "<bold>胜利</bold>", "<gray>本轮结束</gray>");
Display.bossBar(player, name, progress, Color.RED);
Display.scoreboard(player, board -> board.title(...).line(1, ...)); // 仅 Bukkit；见 §6.0
```

- 语言文件沿用 YAML，键即消息 id，值支持 MiniMessage；
- 缺失键回退默认语言，再回退 key 本身；开发模式打印缺失键告警；
- Display 引擎在 kelpLib-core 定义平台无关接口，Bukkit 全量实现，Velocity 实现 ActionBar / Title / BossBar 子集。

### 6.8 GUI 菜单（M3，仅 Bukkit 侧，引入 InvUI）

**决策**：不自研 GUI 框架，引入 [InvUI](https://github.com/NichtStudioCode/InvUI)（MIT，持续维护至 26.x，原生 MiniMessage，Normal/Paged/Tab/Scroll 四类 GUI，支持 Chest/Anvil/Crafter 等十余种容器界面，社区活跃）。选型对比见 §11.2。

- **集成方式**：InvUI 作为 kelpLib-bukkit 的捆绑依赖，**捆绑但不 relocate**（导出型依赖，库插件模式，见 §8）；业务插件可直接使用 InvUI 原生 API（KelpLib 不遮蔽它），但不应自行再 shade 同名库；
- **KelpLib 胶水层**（薄）：
  - `ItemBuilder` ↔ InvUI 物品构建互操作；
  - `Messages`/i18n 注入：标题、物品名、lore 经消息键渲染（per-player locale）；
  - 菜单会话纳入 Terminable 生命周期（关闭/死亡/迁移自动清理）；
  - 周期刷新/动画经 viewer 的 EntityScheduler 驱动（`RegionSchedulerView.forEntity`，见下方 Folia 硬约束）；
  - 可选 sugar：`Schemes.mask("110000011")` 掩码字符串 → InvUI 槽位集合（helper 的 MenuScheme 思路，低成本）；
- **版本线**：GUI 模块**仅支持 26.x**（捆绑 InvUI v2，与 InvUI 版本线对齐）；1.21.x 下经 Capabilities 门控禁用（新 API 调用点隔离，保证类加载安全）并在文档标注；
- **Folia 线程模型（官方实验性支持 + 两条硬约束）**：InvUI 官方声明**实验性**支持 Folia，条件是——① 一切 InvUI 调用（点击回调、刷新、动画、标题更新）必须在 **viewer 实体所属线程**（EntityScheduler）执行，胶水层强制经 `RegionSchedulerView.forEntity()` 收口，禁止直接调用；② **跨玩家零共享**——`Gui`、`Item`、`ItemProvider`、`VirtualInventory` 均不可跨玩家复用，菜单会话与内容严格按 viewer 实例化（跨玩家共享库存类需求见 §1.3 暂缓项）。"实验性"意味着 API 可能随小版本变动，版本锁定（§8）为硬性要求；
- **M3 验收关注点**：Folia 下经 viewer EntityScheduler 路由的分页/点击/关闭清理全流程正常；viewer 隔离验证（双人同时打开同型菜单互不干扰）。

### 6.9 存储抽象 与 Messenger 消息通道（M4，双平台）

**存储**：

```java
public interface Repository<T, ID> {
    CompletableFuture<Optional<T>> load(ID id);
    CompletableFuture<Void> save(T entity);
    CompletableFuture<Void> delete(ID id);
}

// 统一工厂创建：
Storage.file(plugin, "data", PlayerData.class)                        // JSON/YAML 文件仓库（含滚动备份）
Storage.sql(plugin, SqlSpec.of(SqlDialect.H2, path))                  // 嵌入式零配置起步
Storage.sql(plugin, SqlSpec.of(SqlDialect.MYSQL, host, db, user, pw)) // 多服共享
```

- `SqlDialect { H2, SQLITE, MYSQL, MARIADB }`，方言层收口建表语句、自增主键、UPSERT、类型映射差异；调用方不写方言 SQL；
- 迁移：`migrations/{dialect}/V1__init.sql` 按方言目录查找，缺失时回退 `migrations/common/`；版本表记录已执行脚本，启动时增量执行；
- 连接池统一 HikariCP，驱动 **H2 2.x、sqlite-jdbc、mysql-connector-j、mariadb-java-client**（均兼容 Java 21）；分发按 §8：Bukkit/Folia 侧经 plugin.yml `libraries:` 动态引入，Velocity 侧 relocate 捆绑；
- 选型引导：H2 / SQLite 面向单服或开发环境（零外部依赖），MySQL / MariaDB 面向多服共享数据库（MariaDB 使用官方 mariadb 驱动而非 mysql 驱动兼容模式）；
- 文件仓库含滚动备份（保留最近 N 份）与关服 flush（吸收 helper 的 FileStorageHandler 思路）；
- 全 API 异步（连接操作在独立线程池，回调切回调度器）。

**Messenger（三通道分层，传输可插拔）**：

```java
Messenger ms = Kelp.messenger();

// 1) Channel —— fire-and-forget 广播
Channel<NotifyMsg> ch = ms.getChannel("notify", NotifyMsg.class);
ch.sendMessage(new NotifyMsg(...));                        // 所有订阅端收到
ch.newAgent().addListener((agent, msg) -> ...);            // 订阅（agent 为 Terminable）

// 2) ConversationChannel —— 带会话 ID 与超时的请求/回复
// 3) ReqRespChannel —— Promise 化 RPC
ReqRespChannel<TeleportReq, Boolean> rpc =
    ms.getReqRespChannel("tp-req", TeleportReq.class, Boolean.class);

rpc.responseHandler(req -> doTeleport(req));               // 服务端（Bukkit 侧）
CompletableFuture<Boolean> ok = rpc.request(req, Duration.ofSeconds(5));  // 客户端（Velocity 侧），超时自动异常完成
```

- 传输实现：`redis`（Lettuce，M4，双端可用——后端↔代理端共享数据的主载体）+ `plugin messaging`（M5，Bukkit↔Velocity 直连通道）；
- 消息编解码复用 §6.6 序列化器；
- **信任边界**：Messenger 假定运行于可信内网（服务器家族自控网络），不提供端到端加密与鉴权；频道名统一前缀（如 `kelplib:`）避免与第三方插件冲突；
- 原计划的 "RedisBus" 概念并入 Messenger 的 redis 传输实现。

### 6.10 `/kelp demo` 验收命令（贯穿各里程碑）

内置 `/kelp demo scheduler|promise|event|listener|item|nbt|command|config|message|gui|storage|messenger` 子命令，用最小代码演示各模块能力，同时作为里程碑验收的统一手段（替代正式测试基建，符合"轻量"质量决策）。Bukkit 与 Velocity 各自提供平台可用子集。

---

## 7. 版本兼容策略（1.21.x ~ 26.x + Velocity 3.x）

选了"近期版本"兼容，有硬约束，均为 M0 落地项：

**Bukkit/Folia 侧**：

1. **字节码目标 = 21**。1.21.x 服务端最低运行 Java 21，若以 25 编译发布，旧服上会直接 `UnsupportedClassVersionError`。做法：toolchain 保持 25，各模块统一 `options.release.set(21)`；**21 之后的语言特性一律不用**（P5）。
2. **`plugin.yml` 的 `api-version` 降至 `'1.21'`**（当前为 `'26.3'`，会阻止在 1.21.x 加载）；同时添加 `folia-supported: true`（Folia 加载的必要标志）。版本号仍由 `gradle.properties` 模板注入。
3. **能力探测 + 调用点隔离**。编译依赖保持 paper-api 26.3；对只在 26.x 存在的 API：`Capabilities` 记录特性开关（启动时探测）；新 API 的调用**收敛到独立类**（方法签名/字段不得引用缺失类型，否则整个类在旧版加载即失败）；能力缺失时提供降级实现或在文档中标注"需 26.x"。预期差异很小：Brigadier、Adventure、Data Component、PDC 在 1.21.x 均已可用。

**Velocity 侧**：

4. 跟随 Velocity API 最新 3.x（`com.velocitypowered:velocity-api`，`annotationProcessor` 生成 `velocity-plugin.json`）；Velocity 3.3+ 运行于 Java 21+，与字节码目标 21 一致，无需额外降级。

**双平台共享层**：

5. **Adventure 版本对齐**。kelpLib-api/core 运行时使用平台内置的 Adventure（不可 relocate——组件对象必须与平台互通），编译以两平台最低公共版本为准；API 面回避两平台间签名有差异的较新 Adventure API，双端冒烟覆盖文本渲染路径。

依赖的最低版本同样需 Java 21 兼容：HikariCP 6.x、Lettuce 6.x、H2 2.x、mariadb-java-client 3.x（均满足）；InvUI 版本线见 §6.8（GUI 仅 26.x）。

---

## 8. 工程化与质量（轻量基线）

- **构建与产物**：多模块 Gradle；`gradle build` 产出 `kelpLib-bukkit`（Bukkit 插件 jar）与 `kelpLib-velocity`（代理插件 jar）。第三方依赖以 `compileOnly` 引入并 shadow 打进对应平台 jar，按暴露面分三类：
  - **导出型**（LiteCommands、InvUI）：业务插件被允许直接使用其原生 API，**捆绑但不 relocate**，走"库插件"模式（同 Vault/ProtocolLib）；版本由 KelpLib 锁定，业务插件不应自行再 shade 同名库；
  - **内部型**（HikariCP、Lettuce、四个 JDBC 驱动）：仅经 KelpLib 门面触达。Bukkit/Folia 侧经 plugin.yml `libraries:` 动态引入（平台原生机制，运行时自 Maven Central 下载至服务器 libraries 目录；首次启动需联网，离线环境可预置该目录）；Velocity 侧 shadow + relocate（`ink.tuanzi.kelpLib.libs.*`）捆绑（Velocity 无 libraries 机制）；
  - **ItemNBTAPI**：两侧均 relocate 捆绑（§6.4）。
- **构件发布**：0.x 阶段用 `mavenLocal()` 发布 `kelpLib-api` 构件（`maven-publish`）；正式发布基建（GitHub Packages / 内部 Nexus）暂缓，M6 前再评估。
- **依赖仓库**：InvUI 托管于 `repo.xenondevs.xyz`（非 Maven Central），构建脚本中显式声明仓库并锁定版本，若可用性成为风险则评估镜像或 vendoring；LiteCommands 发布于 Maven Central；ItemNBTAPI 坐标 `de.tr7zw:item-nbt-api:2.x`（Maven Central，另有 CodeMC/JitPack 渠道），运行时无额外仓库需求。
- **Gradle wrapper**：M0 执行 `gradle wrapper` 生成 `gradlew`（当前缺失，便于协作与后续 CI）。
- **本地运行**：Bukkit 侧沿用 `gradle runServer`（run-paper，Paper 26.3）；Velocity 侧配置本地运行任务（velocity 官方模板的 run 任务或手动下载 jar），M0 起保证双端一键冒烟。
- **JavaDoc**：`kelpLib-api` 100% 覆盖（G5）；`core`/平台模块不强制。
- **README**：双端接入指引（依赖声明、depend/Dependency 声明、获取服务）、模块×平台矩阵、各模块速览 + 最小示例。
- **冒烟**：每个里程碑在 Paper 26.3 跑通 `/kelp demo` 全套；**Folia 冒烟**：验证 run-paper 3.x 的 Folia 运行支持，若不支持则以手动下载对应 Folia jar 的方式执行同一冒烟清单（含跨区传送、实体迁移场景）；**Velocity 冒烟**：本地代理 + 至少一个后端服，验证命令/配置/存储/Messenger 通路。插件 disable 后零残留纳入每个里程碑冒烟；`/reload`、PlugManX 类热重载场景 0.x 不承诺，1.x 验证。
- **测试与 CI**：v1.0 不做；1.0 后增强路线：core 单测（调度抽象与 Promise 可 mock 验证、SQL 方言层可用内存 H2 真实验证、Terminable 关闭语义可纯 JVM 测试）→ GitHub Actions 构建 + JavaDoc 检查。

---

## 9. 里程碑与路线图

分模块迭代；每里程碑交付即内部可用，后续优先级可根据 utoverse 业务插件的实际使用反馈调整。周期按"每周数个半天"的业余节奏估算。

| 里程碑 | 内容 | 版本 | 验收标准 | 建议周期 |
|--------|------|------|----------|----------|
| **M0 工程骨架（多模块）** | 见 §9.1 执行清单 | 0.1.0 | Paper runServer 与 Velocity 本地运行均空载入通过；门面在双端依赖插件中可获取 | 1 周 |
| **M1 调度 + Promise/Terminables + 基础工具** | `KelpScheduler` 三端实现（Paper/Folia/Velocity）+ `RegionSchedulerView`；**Promise 链式**（sync/async/delay/timeout/combine）；**Terminables 体系**（含监听器绑定、库内统一收口）；ItemBuilder、Cooldown、util 系列；`/kelp demo scheduler/promise/listener/item` | 0.2.0 | 同一 demo 插件零改动在 Paper、Folia 均运行；demo 插件 disable 后任务/监听零残留；Velocity 侧 GLOBAL/ASYNC demo 通过 | 1.5~2 周 |
| **M2 NBT + 配置 + 命令 + 事件层** | 函数式事件订阅（subscribe/merge/filters/expireAfter，§6.2）；NbtService（ItemNBTAPI 封装）；配置映射/热重载/序列化器（**双端可用**，watch 返回 Terminable）；LiteCommands 集成（注解式单入口、自定义参数解析器、i18n/权限胶水）；`/kelp demo event/nbt/config/command` | 0.3.0 | 事件过期/过滤器/绑定注销 demo 通过；命令 Brigadier 保真度冒烟通过（Paper 26.x 原生类型化补全）；Folia 下注册/执行/补全正常；配置修改后热重载生效；Velocity 侧配置 demo 通过 | 2.5~3 周 |
| **M3 消息 + GUI（InvUI 集成）** | Messages/I18n/Display（Bukkit 全量）；InvUI 捆绑 + 胶水层（互操作/i18n 注入/会话 Terminable/掩码 sugar/**viewer EntityScheduler 线程路由 + 跨玩家零共享**）；GUI 按 Capabilities 门控（仅 26.x 生效，§6.8）；`/kelp demo message/gui` | 0.4.0 | Folia 下经 viewer EntityScheduler 路由的分页/点击/关闭清理正常且双人同开互不干扰；1.21.x 下 GUI 禁用且无类加载错误 | 2~2.5 周 |
| **M4 存储抽象 + Messenger（Redis 传输）** | 文件仓库（含备份）→ SQL 四方言（H2/SQLite/MySQL/MariaDB + 迁移）→ Messenger 三通道（Redis 传输）；`/kelp demo storage/messenger` | 0.5.0 | demo 插件在 H2 与 MySQL 完成"存-读-改-删"闭环（SQLite/MariaDB 各跑一次冒烟）；双端 ReqResp RPC 经 Redis 打通且超时生效；SQL/Redis 缺失时优雅降级 | 2.5 周 |
| **M5 Velocity 平台补全** | LiteCommands Velocity 平台接入（litecommands-velocity）；Display 子集（ActionBar/Title/BossBar）；Messages localeResolver 经 PlayerSettings；**Messenger 插件消息传输**（Bukkit↔Velocity 直连）；接入文档 | 0.6.0 | Velocity 全套 demo（命令/配置/消息/存储/Messenger）通过；代理端↔后端 RPC 经插件消息通道验证 | 1.5~2 周 |
| **M6 打磨与 1.0** | JavaDoc 补全；README（双端）；api 构件发布（maven-publish）；API 面评审冻结 | 1.0.0 | 全模块双端 demo 通过；文档可支撑新插件 10 分钟接入（G1） | 1 周 |

**总计约 12~14 周**（M1~M4 内容偏满，可视实际需要拆成更小的 0.x 版本交付）。

### 9.1 M0 执行清单（第一版基线改动）

- `gradle wrapper` 生成 wrapper 脚本；
- `settings.gradle.kts`：`include("kelpLib-api", "kelpLib-core", "kelpLib-bukkit", "kelpLib-velocity")`；
- 根 `build.gradle.kts`：公共配置（`options.release.set(21)`、JavaDoc、publish、shadow 约定）；`api/core` 两模块不声明任何平台依赖（§4 依赖规则）；
- `kelpLib-bukkit`：迁入现有 build 内容（paper-api 26.3、run-paper、plugin.yml 模板展开）；`plugin.yml` 改 `api-version: '1.21'`、加 `folia-supported: true`（`version` 模板与 `load: STARTUP` 不动）；
- `kelpLib-velocity`：velocity-api 3.x + annotation processor，空 `@Plugin(id = "kelplib")` 入口与本地运行任务（id 与依赖方 `@Dependency` 声明一致，§4）；
- `gradle.properties`：`version=0.1.0`；
- 各模块包骨架与 `Kelp` 门面、平台探测；
- 同步更新 `AGENTS.md`：反映多模块结构、api-version/folia-supported 变更与 wrapper 用法（现说明基于单模块）。

---

## 10. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| Folia 线程问题难复现、难测试（无自动化测试基建） | 高 | 调度抽象强制收口（P2）；GUI/会话状态按 viewer 隔离；每里程碑执行 Folia 冒烟清单（含跨区传送/实体迁移）；库内禁用 BukkitScheduler 并以 code review 把关 |
| 旧版本（1.21.x）类加载失败 | 高 | 字节码目标 21；调用点隔离模式（§7）；M1 起每版本在最低支持版本上做一次加载冒烟 |
| InvUI Folia 为官方实验性支持（viewer EntityScheduler 路由 + 跨玩家零共享为硬约束，API 可能随小版本变动）；GUI 仅支持 26.x（1.21.x 禁用） | 中 | 胶水层强制 EntityScheduler 收口与 viewer 隔离（§6.8）；版本锁定为硬性要求；Capabilities 门控保证 1.21.x 无类加载错误并文档标注 |
| InvUI 依赖仓库（repo.xenondevs.xyz）可用性 | 低 | 锁定版本；必要时镜像/vendoring（§8） |
| LiteCommands：Folia 模块为社区贡献、Paper Brigadier 保真度未实测 | 中 | 列为 M2 第一冒烟验收项（§6.5）；不达标回退：Cloud（Folia 自行验证/适配）或自研双后端 |
| Paper 与 Velocity 内置 Adventure 版本不一致 | 中 | api/core 以最低公共版本编译，禁止 relocate Adventure（§7）；双端文本渲染冒烟覆盖 |
| libraries: 动态引入需首次联网下载（离线/内网服务器） | 低 | 可预置服务器 libraries 目录；必要时提供 full 捆绑备选发行版（§8） |
| Bukkit/Velocity 平台类型泄漏进共享模块 | 中（架构性） | 构建期约束：api/core 模块不声明平台依赖（§4）；模块×平台矩阵（§6.0）作为评审清单 |
| 双平台维护面扩大（M5 起） | 中 | P8 共享优先：平台模块只承载平台交互；Velocity 冒烟清单化；命令/配置/存储/Messenger 大头在共享层 |
| 内部型依赖 relocate 后的行为差异 / 导出型依赖版本锁定 | 中 | 各自的 demo 覆盖真实读写路径（M4 用 H2 内存库做真实 SQL 验证）；relocate 包名固定约定，导出型版本由 KelpLib 锁定（§8），文档标注 |
| API 设计返工（0.x 无外部反馈） | 中 | `@ApiStatus.Experimental` 明确标记不稳定面；内部业务插件尽早试用、驱动迭代 |
| 单人维护带宽有限 | 中 | 模块边界清晰、可独立交付；大模块允许里程碑内再拆分；轻量质量基线降低持续成本 |
| run-paper 不支持 Folia 运行 | 低 | 手动下载 Folia jar 冒烟（§8），不阻塞交付 |

---

## 11. 选型与调研依据

### 11.1 lucko/helper 评估

> 借鉴 [lucko/helper](https://github.com/lucko/helper/wiki/) 的 **API 设计思想而非代码**。helper 是 MIT 协议的 Bukkit 工具库，但为 Java 8 时代产物（2017–2020，早于 Folia、现代 Paper API 与 Data Component），作者已停止功能开发，且整体绑定 Bukkit 调度器静态上下文、不兼容 Folia，不宜作为运行时依赖引入。其 Guava `TypeToken` 依赖在本项目中以现代 Java 泛型手段替代。

| helper 能力 | 说明 | 处置 |
|-------------|------|------|
| Promise | 跨同步/异步线程的链式调用（`thenApplySync/Async`、延迟、超时） | ✅ 自研（M1，§6.2） |
| Terminables 生命周期 | `Terminable`、`bindWith(consumer)`、插件 disable 自动关闭、`TerminableModule` | ✅ 自研（M1，§6.2），并作为库内统一生命周期底座（P9） |
| Messenger 三通道 | `Channel` / `ConversationChannel` / `ReqRespChannel`（Promise 化 RPC + 超时），传输可插拔 | ✅ 自研（M4/M5，§6.9；Redis 传输双端，插件消息传输 M5） |
| MenuScheme 掩码布局 | 逐行掩码字符串 + populator 顺序填充 + 配色 scheme | ◐ GUI 整体引入 InvUI；掩码 sugar 作为可选低成本补充（§6.8） |
| GUI / 菜单 | Gui.redraw 模型、分页、ticker 动画 | ✅ 引入社区包 InvUI，不自研（§6.8，对比见 §11.2）；版本线仅 26.x |
| 函数式事件订阅 | `Events.subscribe(...).filter(...).expireAfter(...)`、`EventFilters`、merge/bindEvent | ⚠ 无成熟社区包（§11.3）→ ✅ 自研完整函数式（流式风格，§6.2；原生 Listener 绑定保留为互操作路径） |
| FileStorageHandler | 文件仓库带滚动备份、关服自动保存 | ✅ 吸收进 M4 文件仓库（§6.9） |
| 类型化元数据 | `MetadataKey` + `ExpiringValue` 自动过期 + 玩家退出清理 | ◐ 候选 1.x |
| Bucket | 按 tick 分区分散大批量元素处理 | ✅ 并入 util（M1，§6.3） |
| 异步 Scoreboard（基于 ProtocolLib） | 高频更新防抖 | ◐ 仅借鉴"更新合并去抖"设计，不引入 ProtocolLib |
| Hologram / Sign Prompt / Profiles / NPC | 浮空字、告示牌输入、UUID 查询缓存、玩家 NPC | ◐ 1.x 候选；1.21+ `TextDisplay` 降低 Hologram 成本；NPC 超出"无 NMS/外部协议库"边界 |
| Scheduler / Cooldown / 加权随机 / ItemBuilder / SQL/Redis 插件化 | 与既定模块重叠 | ✅ 已覆盖且更现代（作用域化调度抽象、四方言 SQL） |
| Plugin Annotations（注解生成 plugin.yml） | — | ◐ 1.x 可选 |
| Maven Annotations（运行时下载 Maven 依赖） | — | ✗ 不做（供应链风险、需网络、与 relocate 方案冲突） |
| helper-js（Nashorn 脚本）/ helper-lilypad / helper-mongo | 场景不符 | ✗ 不做 |

### 11.2 GUI 选型对比

| 候选 | 维护状态 | 版本覆盖 | 关键特性 | 结论 |
|------|----------|----------|----------|------|
| **InvUI**（NichtStudioCode/xenondevs） | 活跃（~900 commits，跟随 26.3） | v2 ↔ 26.x；v1.49 ↔ 1.14–1.21.11（双版本线） | Normal/Paged/Tab/Scroll、Chest/Anvil/Crafter 等十余容器、原生 MiniMessage、动画示例 | **选定**：功能最全、维护最紧、MiniMessage 原生契合；官方声明**实验性 Folia 支持**（viewer EntityScheduler 线程规则 + 跨玩家零共享，§6.8） |
| TriumphGUI（TriumphTeam） | 中等（3.1.12，2025-04） | 未明确声明 | builder 风格、分页 | 备选（文档欠奉、版本覆盖不明） |
| InventoryFramework（stefvanschie） | 稳定但传统 | 宽 | XML + Pane 模型 | 备选（风格偏旧，社区讨论中 InvUI 为进阶首选） |
| GUI-API（downfalls） | 小众 | 明确支持 Folia | 组件化 | 观察（"成熟度"不足） |

### 11.3 函数式事件订阅调研

未找到成熟、在维护的社区库——检索仅见小众未维护仓库（如 conclube/EventBuilder）与 Kotlin 生态方案（KSpigot，不适用 Java 项目）；helper 自身的 Events 模块是领域内唯一"成熟"实现，但其整体绑定 Bukkit 调度器静态上下文、不兼容 Folia、依赖停滞，不宜作为运行时依赖引入。结论：自研完整函数式（流式风格，§6.2）。

### 11.4 命令框架选型

| 候选 | Paper/Folia | Velocity | 维护状态 | 许可 | 结论 |
|------|:-----------:|:--------:|----------|------|------|
| **LiteCommands**（Rollczi/LiteDevelopers） | ✅ 官方 bukkit/folia 模块 | ✅ 官方模块 | 活跃（v3.11.0，Renovate 自动化，官方 IntelliJ 插件） | Apache-2.0 | **选定**：唯一官方覆盖三平台者；注解模型（`@Command`/`@Execute`/`@Arg`/`@Flag`/`@Sender`）与本项目设计几乎 1:1 |
| Cloud（Incendo） | ✅ cloud-paper | ✅ cloud-velocity | 成熟稳定（Cloud 2.1.0，生态使用最广，Brigadier 一等公民） | MIT | 备选/回退：无 Folia 官方模块 |
| CommandAPI（JorelAli） | ✅ | ❌ 无 Velocity 模块 | 成熟活跃 | — | 出局：不满足三平台 |
| ACF（aikar） | ✅ | ❌ 仅 Bukkit/Bungee | 停滞（Java 8 时代） | — | 出局 |

**注册入口策略**（选型决策第二部分）：注解式为唯一正式入口——动态参数值经自定义参数解析器 + 运行时补全表达，底层落到 Brigadier（Paper 原生类型化补全 UI / Velocity `BrigadierCommand`），不手写 Brigadier、样板由框架代劳；罕见的动态结构不设门面入口，直接使用 LiteCommands 原生 builder API（导出型依赖、不遮蔽）。详见 §6.5。

**风险与回退**：Folia 模块为社区贡献、Paper 端 Brigadier 整合深度未实测 → 列为 M2 第一冒烟验收项（§6.5）；不达标回退 Cloud（Folia 自行验证/适配）或自研双后端方案（调度走 `KelpScheduler`，Paper 落 Brigadier、Velocity 落原生 CommandManager）。

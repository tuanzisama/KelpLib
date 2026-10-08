# Scheduler

统一调度抽象为 `Kelp.scheduler()`（`ink.tuanzi.kelpLib.api.scheduler.KelpScheduler`），是全部线程操作的收口——业务代码不直接调用 `Bukkit.getScheduler()` 或平台原生调度器。

## 作用域

共享接口含两种作用域，为 Paper / Folia / Velocity 三平台的最大公约数：

| 作用域 | Paper | Folia | Velocity |
|--------|-------|-------|----------|
| GLOBAL（同步语义）| 主线程 | 全局区域线程（Global Region）| 调度器执行池 |
| ASYNC（异步语义）| 异步池 | 异步池 | 调度器执行池 |

位置 / 实体作用域为 Bukkit 专属，见下文 [RegionSchedulerView](#regionschedulerview)。

## 方法

| 方法 | 说明 |
|------|------|
| `run(Consumer<ScheduledTask>)` | GLOBAL 立即执行一次 |
| `runLater(Duration, Consumer<ScheduledTask>)` | GLOBAL 延迟执行一次 |
| `runTimer(Duration, Duration, Consumer<ScheduledTask>)` | GLOBAL 固定周期执行（首次延迟、周期）|
| `supply(Supplier<T>)` | ASYNC 执行，结果经 `CompletableFuture` 返回 |
| `syncExecutor()` | GLOBAL 执行器（`Executor` 视图）|
| `asyncExecutor()` | ASYNC 执行器 |
| `cancelTasks()` | 取消 KelpLib 发起的全部任务 |

周期与延迟一律用 `java.time.Duration` 表达，内部换算 tick（最小 1 tick），平台 tick 语义不泄漏到 API。

任务句柄 `ScheduledTask` 实现 `Terminable`：`cancel()` 幂等取消，`bindWith(consumer)` 纳入插件生命周期（见 [Terminables](terminables.md)）。

## 示例

```java
Kelp.scheduler().runLater(Duration.ofSeconds(1), task ->
        sender.sendMessage("1 秒后执行"));

Kelp.scheduler().runTimer(Duration.ofSeconds(10), Duration.ofMinutes(1), task ->
        saveAll());

Kelp.scheduler().supply(() -> loadRemote())
        .thenAccept(data -> use(data));
```

## RegionSchedulerView

Bukkit 侧 `KelpBukkit.regions()` 返回区域 / 实体作用域视图：

| 方法 | 说明 |
|------|------|
| `at(Location, Consumer<ScheduledTask>)` | 在位置所属区域线程执行 |
| `atLater(Location, Duration, Consumer<ScheduledTask>)` | 延迟在位置所属区域线程执行 |
| `forEntity(Entity, Consumer<ScheduledTask>)` | 在实体所属线程执行；Folia 下跟随实体迁移，实体 retired 时自动取消 |

Folia 下任务运行在目标区块 / 实体所属的区域线程；Paper 上退化为普通主线程任务。

```java
KelpBukkit.regions().forEntity(player, task ->
        player.setHealth(20.0));
```

Folia 检测基于 `io.papermc.paper.threadedregions.RegionizedServer` 类存在性，启动时完成，运行期不切换。

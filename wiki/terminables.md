# Terminables

库内一切可注销资源——调度任务、事件订阅、监听器、文件监听、Redis 订阅、GUI / 计分板会话、Messenger agent、命令注册——统一实现 `Terminable` 接口。插件 disable 时统一关闭，保证零残留。

## 接口

`ink.tuanzi.kelpLib.api.terminable.Terminable`：

```java
public interface Terminable extends AutoCloseable {
    void close();          // 幂等：重复调用为无操作
    boolean isClosed();
    default <T extends Terminable> T bindWith(TerminableConsumer consumer);
}
```

## 绑定生命周期

`TerminableConsumer` 是可绑定 `Terminable` 的注册表，插件实例即 consumer：

- Bukkit：`KelpBukkit.lifecycle(plugin)`——插件 disable（`PluginDisableEvent`）时统一关闭其绑定物
- Velocity：`Kelp.platform().lifecycle(plugin)`（内部接口 `@ApiStatus.Internal`，常规绑定经由命令注册等入口自动完成）——关服 / 卸载时关闭

同一插件重复获取返回同一实例。

```java
TerminableConsumer lifecycle = KelpBukkit.lifecycle(this);

Kelp.scheduler().runTimer(...).bindWith(lifecycle);
Events.subscribe(PlayerJoinEvent.class)
        .handler(e -> ...)
        .bindWith(lifecycle);
```

两种等价写法：

```java
task.bindWith(consumer);   // Terminable 侧
consumer.bind(task);       // consumer 侧
```

consumer 已关闭时，后绑定的对象会被立即关闭。

## 分组

`CompositeTerminable.create()` 创建组合注册表，把多个 `Terminable` 聚合为一个统一关闭（单个关闭失败不影响其余）。

`TerminableModule.create("模块名")` 创建具名模块，按功能分组注册，便于分类注销与诊断。

```java
CompositeTerminable group = CompositeTerminable.create();
group.bind(taskA);
group.bind(agentB);
// group.close() 关闭全部
```

## 实现 Terminable 的库类型

| 类型 | close 行为 |
|------|-----------|
| `ScheduledTask` | 取消任务 |
| `Events` 订阅句柄 | 注销监听 |
| `ConfigEngine.watch` 返回值 | 停止文件监听 |
| `MessageAgent` / 会话 agent | 退订频道 |
| `ScoreboardDisplay` | 恢复主计分板 |
| 命令注册 | 从命令表注销 |
| `Promise` | 取消底层 future |

自定义可注销资源同样实现 `Terminable` 并 `bindWith(lifecycle)` 即可纳入体系。

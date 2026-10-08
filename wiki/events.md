# Events

函数式事件订阅（仅 Bukkit 侧）：订阅即流——过滤 → 过期 → 处理 → 绑定生命周期，一行收口。入口 `ink.tuanzi.kelpLib.bukkit.listener.Events`。

实现基于 `HandlerList` + `RegisteredListener` 直接注册，无动态类生成。回调线程不做假设（Folia 下为区域线程）。

## subscribe

```java
Events.subscribe(PlayerJoinEvent.class)
        .filter(EventFilters.ignoreCancelled())
        .handler(e -> messages.send(e.getPlayer(), "welcome"))
        .bindWith(KelpBukkit.lifecycle(plugin));
```

构建器方法：

| 方法 | 说明 |
|------|------|
| `filter(Predicate<E>)` | 过滤，可叠加多个 |
| `ignoreCancelled(boolean)` | 忽略已取消事件 |
| `priority(EventPriority)` | 监听优先级，默认 `NORMAL` |
| `expireAfter(int)` | 执行 N 次后自动注销 |
| `expireAfter(Duration)` | 时限后自动注销 |
| `handler(Consumer<E>)` | 设置处理器，返回订阅句柄 |

订阅句柄 `EventSubscription` 实现 `Terminable`：`close()` / `unsubscribe()` 注销；`bindWith(consumer)` 以 consumer 所属插件为注册主体注册并纳入生命周期，也可 `register(plugin)` 直接注册。

## merge

多事件归一（共享父类型）：

```java
Events.merge(PlayerEvent.class, PlayerJoinEvent.class, PlayerQuitEvent.class)
        .filter(event -> event.getPlayer().hasPermission("demo.track"))
        .handler(event -> log(event.getPlayer().getName()))
        .bindWith(lifecycle);
```

`bindEvent(Class<E>, Function<E, B>)` 可追加事件类型并提取公共类型。

## EventFilters

预置过滤器：

| 方法 | 说明 |
|------|------|
| `ignoreCancelled()` | 忽略已取消事件（非 Cancellable 恒通过）|
| `playerHasPermission(String)` | 仅放行具有权限的玩家事件（无法判定玩家的事件恒拒绝）|
| `ignoreSameBlock(playerFn, blockFn)` | 忽略同一玩家对同一方块的连续重复事件 |

```java
Events.subscribe(BlockBreakEvent.class)
        .filter(EventFilters.playerHasPermission("demo.mine"))
        .handler(e -> ...)
        .bindWith(lifecycle);
```

## 传统监听器

已有 `@EventHandler` 监听器类经 `KelpBukkit.listeners()` 注册，返回 `Terminable`（close 即 `unregisterAll`）：

```java
KelpBukkit.listeners().register(plugin, new MyListener())
        .bindWith(KelpBukkit.lifecycle(plugin));
```

## 一次性订阅

```java
Events.subscribe(PlayerJoinEvent.class)
        .expireAfter(1)
        .handler(e -> e.getPlayer().sendMessage("一次性欢迎"))
        .bindWith(lifecycle);
```

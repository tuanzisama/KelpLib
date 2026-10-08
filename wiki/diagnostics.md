# Diagnostics

KelpLib 自带 `/kelp` 命令（权限 `kelplib.command`），作为各模块的最小演示与诊断工具。安装后直接执行即可看运行效果。

## /kelp info

输出平台与模块信息。Bukkit 端的 `describe()` 会标明调度实现：

```
platform: bukkit/folia (regionized)   # Folia
platform: bukkit/paper                # Paper
platform: velocity (proxy)            # Velocity
```

## /kelp demo \<module\>

| module | 内容 | 平台 |
|--------|------|------|
| `scheduler` | runLater / supply 演示 | 双端 |
| `promise` | async → delay → sync 链 + 超时 | 双端 |
| `event` | 一次性 PlayerJoinEvent 订阅（`expireAfter(1)`）| Bukkit |
| `listener` | 传统 `@EventHandler` + Terminable 绑定 | Bukkit |
| `item` | ItemBuilder 构建演示物品发放 | Bukkit |
| `nbt` | NBT 读写演示（不可用时输出降级引导）| Bukkit |
| `command` | 自研命令层说明（本命令即注解树注册）| Bukkit |
| `config` | 配置加载 / 写回（注释与键顺序保留）| 双端 |
| `message` | Messages 渲染与缺失键回退 | Bukkit |
| `scoreboard` | 侧边栏显示 10 秒（实体线程路由）| Bukkit |
| `gui` | GUI 门控可用性 | Bukkit |
| `storage` | 文件仓库 存-读-删 闭环（异步输出结果）| 双端 |
| `messenger` | 已注册传输与频道收发 | 双端 |

Velocity 侧可用模块为 scheduler / promise / config / storage / messenger；Bukkit 侧为全部。

示例：

```
/kelp demo promise
```

输出会标注来源模块（如 `[demo/promise] OK: ...`）。

## 常见问题排查

**`KelpLib is not available` 异常**：业务插件未声明依赖或调用早于 KelpLib 加载。Bukkit 检查 `depend: [KelpLib]`，Velocity 检查 `@Dependency(id = "kelplib")`；加载期调用先用 `Kelp.isAvailable()`。

**Messenger 无传输**：`plugin-messaging` 传输随库启动自动注册；Redis 传输需显式 `registerTransport(new RedisTransport(spec))`，见 [Messenger](messenger.md)。注意 Bukkit→Velocity 方向的插件消息传输需至少一名在线玩家作为载体，无玩家时投递静默丢弃。

**Nbt 不可用**：`Nbt.available()` 为 false 表示服务器与本插件均未提供 ItemNBTAPI，按 [Items](items.md) 降级到 PDC。

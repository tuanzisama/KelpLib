# Commands

注解式命令注册，入口 `Kelp.commands()`（`ink.tuanzi.kelpLib.api.command.Commands`）。一个类一棵命令树，同一套注解在 Bukkit 与 Velocity 上语义一致：

- Bukkit：注解命令树注册到 Paper `CommandMap`
- Velocity：注册到原生 `CommandManager`

命令注册自动纳入 Terminable 生命周期，插件 disable 自动注销。

## 注册

```java
Kelp.commands().register(plugin, HomeCommand.class, WarpCommand.class);
```

命令类需可无参构造，可多次调用 register。

## 注解

| 注解 | 目标 | 说明 |
|------|------|------|
| `@Command(name, permission, description)` | 类 | 命令树根；缺省无权限要求 |
| `@Execute(name, permission, description)` | 方法 | 无 name 为根命令执行体，有 name 为子命令 |
| `@Sender` | 参数 | 平台原生发送者（Bukkit `CommandSender` / Velocity `CommandSource`）|
| `@Arg(name, suggestions, greedy, optional)` | 参数 | 位置参数 |
| `@Flag(name, permission)` | 参数 | 布尔标记，执行时以 `--名称` 出现，位置无关 |

`@Arg` 支持类型：`String`、`int` / `long` / `double` / `boolean`、`UUID`、经 `registerArgumentResolver` 注册的自定义类型。字符串参数可 `greedy = true` 吞并剩余全部参数；`optional = true` 缺参不报用法错误。

## 示例

```java
@Command(name = "home", description = "家传送", permission = "demo.home")
public final class HomeCommand {

    @Execute(name = "set")
    public void set(@Sender Player player, @Arg(name = "名称") String name) { /* ... */ }

    @Execute(name = "list")
    public void list(@Sender Player player, @Flag(name = "silent") boolean silent) { /* ... */ }

    @Execute                                    // 根命令执行体：/home <名称>
    public void teleport(@Sender Player player, @Arg(name = "名称", greedy = true) String name) { /* ... */ }
}
```

权限在节点级校验（根命令 / 子命令 / flag 各自独立），无权限的子命令在 Tab 补全侧隐藏。

## 自定义参数解析器

动态参数值（从配置 / 数据源读取）经 `ArgumentResolver` 注册，重复注册覆盖：

```java
public record Home(String name) {}

Kelp.commands().registerArgumentResolver(Home.class, new ArgumentResolver<Home>() {
    @Override
    public Home parse(String raw, CommandContext ctx) {
        return homes.get(raw);           // null 报参数无效
    }

    @Override
    public List<String> suggest(String current, CommandContext ctx) {
        return homes.keySet().stream()
                .filter(name -> name.startsWith(current))
                .toList();
    }
});
```

`CommandContext` 提供 `source()`（平台发送者）、`hasPermission(String)`、`arg(name)`、`rawArgs()`。

静态候选直接写在 `@Arg(suggestions = {...})` 上。

## 注册主体

Bukkit 端 `register(plugin, ...)` 的第一参数为注册主体（业务 `JavaPlugin`）；传非 Plugin 实例时以 KelpLib 自身为主体。
